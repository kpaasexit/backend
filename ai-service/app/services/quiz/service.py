import json
import asyncio
from datetime import datetime
from enum import Enum
from typing import List, Optional, Dict, Any
from openai import AsyncOpenAI
from qdrant_client.models import PointStruct, Filter, FieldCondition, MatchValue

from app.models.quiz import (
    Quiz, QuizCreate, QuizUpdate, QuizType,
    QuizGenerationRequest, QuizGenerationPrompt
)
from app.models.embedder import get_embedder
from app.vectordb.client import get_qdrant_client
from app.config import get_settings
from app.core.logger import LoggerSetup

logger = LoggerSetup.get_logger(__name__)
settings = get_settings()


class QuizService:
    def __init__(self):
        self.openai_client = AsyncOpenAI(api_key=settings.openai.openai_api_key)
        qdrant_wrapper = get_qdrant_client()
        self.qdrant_client = qdrant_wrapper.get_client()
        self.embedder = get_embedder()
        self.collection_name = "quiz"
        self.categories = {
            1: "과학",
            2: "역사",
            3: "지리",
            4: "문학",
            5: "스포츠",
            6: "예술",
            7: "기술",
            8: "일반상식"
        }

    async def generate_quiz_prompt(self, category_id: int, quiz_type: QuizType) -> str:
        category_name = self.categories.get(category_id, "일반")

        if quiz_type == QuizType.OX:
            return f"""다음 조건에 맞는 OX 퀴즈를 1개 생성해주세요:

카테고리: {category_name}
형식: OX 퀴즈 (참/거짓을 판단하는 문제)

다음 JSON 형식으로 정확히 응답해주세요:
{{
    "quiz_title": "퀴즈의 간단한 제목 (50자 이내)",
    "quiz_content": "퀴즈 문제 내용 (명제 형태로 작성)",
    "quiz_correct_answer": "O 또는 X",
    "quiz_additional_information": "정답에 대한 상세한 설명과 추가 정보"
}}

주의사항:
- quiz_correct_answer는 반드시 "O" 또는 "X" 중 하나여야 합니다
- 문제는 명확하고 모호하지 않아야 합니다
- 난이도는 일반 성인이 풀 수 있는 수준으로 합니다"""
        else:
            return f"""다음 조건에 맞는 4지선다 퀴즈를 1개 생성해주세요:

카테고리: {category_name}
형식: 4지선다 (A, B, C, D 중 택1)

다음 JSON 형식으로 정확히 응답해주세요:
{{
    "quiz_title": "퀴즈의 간단한 제목 (50자 이내)",
    "quiz_content": "퀴즈 문제 내용\\n\\nA) 선택지 1\\nB) 선택지 2\\nC) 선택지 3\\nD) 선택지 4",
    "quiz_correct_answer": "A, B, C, D 중 하나",
    "quiz_additional_information": "정답에 대한 상세한 설명과 추가 정보"
}}

주의사항:
- quiz_correct_answer는 반드시 "A", "B", "C", "D" 중 하나여야 합니다
- 선택지는 명확하게 구분되어야 합니다
- 난이도는 일반 성인이 풀 수 있는 수준으로 합니다"""

    async def check_title_similarity(self, title: str, category_id: int, threshold: float = 0.85) -> List[Dict]:
        try:
            embedding = self.embedder.embed(title)

            search_result = self.qdrant_client.search(
                collection_name=self.collection_name,
                query_vector=embedding.tolist(),
                query_filter=Filter(
                    must=[
                        FieldCondition(
                            key="category_id",
                            match=MatchValue(value=category_id)
                        )
                    ]
                ),
                limit=5,
                score_threshold=threshold
            )

            similar_titles = []
            for hit in search_result:
                similar_titles.append({
                    "title": hit.payload.get("quiz_title"),
                    "score": hit.score
                })

            return similar_titles
        except Exception as e:
            logger.error(f"Error checking title similarity: {e}")
            return []

    async def generate_quiz(self, request: QuizGenerationRequest) -> List[Quiz]:
        generated_quizzes = []

        for _ in range(request.count):
            max_retries = 5
            retry_count = 0

            while retry_count < max_retries:
                try:
                    quiz_type = request.quiz_type or QuizType.OX
                    prompt = await self.generate_quiz_prompt(request.category_id, quiz_type)

                    response = await self.openai_client.chat.completions.create(
                        model="gpt-4o-mini",
                        messages=[
                            {"role": "system", "content": "You are a quiz generator. Always respond with valid JSON only."},
                            {"role": "user", "content": prompt}
                        ],
                        response_format={"type": "json_object"},
                        temperature=0.8
                    )

                    quiz_data = json.loads(response.choices[0].message.content)

                    similar_titles = await self.check_title_similarity(
                        quiz_data["quiz_title"],
                        request.category_id
                    )

                    if similar_titles:
                        logger.info(f"Similar quiz found: {similar_titles[0]['title']} (score: {similar_titles[0]['score']})")
                        retry_count += 1
                        continue

                    quiz = QuizCreate(
                        quiz_category_id=request.category_id,
                        quiz_title=quiz_data["quiz_title"],
                        quiz_content=quiz_data["quiz_content"],
                        quiz_type=quiz_type,
                        quiz_correct_answer=quiz_data["quiz_correct_answer"],
                        quiz_additional_information=quiz_data["quiz_additional_information"]
                    )

                    saved_quiz = await self.create_quiz(quiz)
                    generated_quizzes.append(saved_quiz)
                    break

                except Exception as e:
                    logger.error(f"Error generating quiz: {e}")
                    retry_count += 1
                    if retry_count >= max_retries:
                        raise Exception(f"Failed to generate quiz after {max_retries} retries")
                    await asyncio.sleep(1)

        return generated_quizzes

    async def create_quiz(self, quiz: QuizCreate) -> Quiz:
        try:
            import time
            import uuid
            quiz_id = int(time.time() * 1000000) % (2**31)  # 32비트 정수 범위 내로 제한
            point_id = str(uuid.uuid4())  # Qdrant용 UUID

            embedding = self.embedder.embed(quiz.quiz_title)

            point = PointStruct(
                id=point_id,
                vector=embedding.tolist(),
                payload={
                    "quiz_id": quiz_id,
                    "category_id": quiz.quiz_category_id,
                    "quiz_title": quiz.quiz_title,
                    "quiz_content": quiz.quiz_content,
                    "quiz_type": quiz.quiz_type.value,
                    "quiz_correct_answer": quiz.quiz_correct_answer,
                    "quiz_additional_information": quiz.quiz_additional_information,
                    "created_at": int(datetime.now().timestamp()),
                    "updated_at": int(datetime.now().timestamp())
                }
            )

            self.qdrant_client.upsert(
                collection_name=self.collection_name,
                points=[point]
            )

            return Quiz(
                quiz_id=quiz_id,
                quiz_category_id=quiz.quiz_category_id,
                quiz_title=quiz.quiz_title,
                quiz_content=quiz.quiz_content,
                quiz_type=quiz.quiz_type,
                quiz_correct_answer=quiz.quiz_correct_answer,
                quiz_additional_information=quiz.quiz_additional_information,
                quiz_created_at=datetime.now(),
                quiz_updated_at=datetime.now()
            )
        except Exception as e:
            logger.error(f"Error creating quiz: {e}")
            raise

    async def update_quiz(self, quiz_id: int, quiz_update: QuizUpdate) -> Optional[Quiz]:
        try:
            # quiz_id로 먼저 검색
            search_filter = Filter(
                must=[
                    FieldCondition(
                        key="quiz_id",
                        match=MatchValue(value=quiz_id)
                    )
                ]
            )

            search_result = self.qdrant_client.scroll(
                collection_name=self.collection_name,
                scroll_filter=search_filter,
                limit=1
            )

            if not search_result[0]:
                return None

            point = search_result[0][0]

            existing_quiz = point.payload

            update_data = quiz_update.dict(exclude_unset=True)
            for key, value in update_data.items():
                if value is not None:
                    existing_quiz[key] = value if not isinstance(value, Enum) else value.value

            existing_quiz["updated_at"] = int(datetime.now().timestamp())

            if "quiz_title" in update_data:
                embedding = self.embedder.embed(update_data["quiz_title"])
                vector = embedding.tolist()
            else:
                # 기존 벡터를 유지 (point.vector가 None일 수 있음)
                if hasattr(point, 'vector') and point.vector is not None:
                    vector = point.vector
                else:
                    # 기존 제목으로 다시 임베딩 생성
                    embedding = self.embedder.embed(existing_quiz.get("quiz_title", ""))
                    vector = embedding.tolist()

            updated_point = PointStruct(
                id=point.id,
                vector=vector,
                payload=existing_quiz
            )

            self.qdrant_client.upsert(
                collection_name=self.collection_name,
                points=[updated_point]
            )

            return Quiz(
                quiz_id=quiz_id,
                quiz_category_id=existing_quiz["category_id"],
                quiz_title=existing_quiz["quiz_title"],
                quiz_content=existing_quiz["quiz_content"],
                quiz_type=QuizType(existing_quiz["quiz_type"]),
                quiz_correct_answer=existing_quiz["quiz_correct_answer"],
                quiz_additional_information=existing_quiz.get("quiz_additional_information"),
                quiz_created_at=datetime.fromtimestamp(existing_quiz["created_at"]),
                quiz_updated_at=datetime.fromtimestamp(existing_quiz["updated_at"])
            )
        except Exception as e:
            logger.error(f"Error updating quiz: {e}")
            raise

    async def get_quiz(self, quiz_id: int) -> Optional[Quiz]:
        try:
            search_filter = Filter(
                must=[
                    FieldCondition(
                        key="quiz_id",
                        match=MatchValue(value=quiz_id)
                    )
                ]
            )

            search_result = self.qdrant_client.scroll(
                collection_name=self.collection_name,
                scroll_filter=search_filter,
                limit=1
            )

            if not search_result[0]:
                return None

            payload = search_result[0][0].payload
            return Quiz(
                quiz_id=payload["quiz_id"],
                quiz_category_id=payload["category_id"],
                quiz_title=payload["quiz_title"],
                quiz_content=payload["quiz_content"],
                quiz_type=QuizType(payload["quiz_type"]),
                quiz_correct_answer=payload["quiz_correct_answer"],
                quiz_additional_information=payload.get("quiz_additional_information"),
                quiz_created_at=datetime.fromtimestamp(payload["created_at"]),
                quiz_updated_at=datetime.fromtimestamp(payload["updated_at"])
            )
        except Exception as e:
            logger.error(f"Error retrieving quiz: {e}")
            return None

    async def list_quizzes(self, category_id: Optional[int] = None, limit: int = 100) -> List[Quiz]:
        try:
            filter_conditions = []
            if category_id:
                filter_conditions.append(
                    FieldCondition(
                        key="category_id",
                        match=MatchValue(value=category_id)
                    )
                )

            query_filter = Filter(must=filter_conditions) if filter_conditions else None

            result = self.qdrant_client.scroll(
                collection_name=self.collection_name,
                scroll_filter=query_filter,
                limit=limit
            )

            quizzes = []
            for point in result[0]:
                payload = point.payload
                quiz = Quiz(
                    quiz_id=payload["quiz_id"],
                    quiz_category_id=payload["category_id"],
                    quiz_title=payload["quiz_title"],
                    quiz_content=payload["quiz_content"],
                    quiz_type=QuizType(payload["quiz_type"]),
                    quiz_correct_answer=payload["quiz_correct_answer"],
                    quiz_additional_information=payload.get("quiz_additional_information"),
                    quiz_created_at=datetime.fromtimestamp(payload["created_at"]),
                    quiz_updated_at=datetime.fromtimestamp(payload["updated_at"])
                )
                quizzes.append(quiz)

            return quizzes
        except Exception as e:
            logger.error(f"Error listing quizzes: {e}")
            return []