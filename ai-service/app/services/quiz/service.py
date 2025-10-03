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
from app.core.constants import CATEGORY_MAP

logger = LoggerSetup.get_logger(__name__)
settings = get_settings()


class QuizService:
    def __init__(self):
        self.openai_client = AsyncOpenAI(api_key=settings.openai.openai_api_key)
        qdrant_wrapper = get_qdrant_client()
        self.qdrant_client = qdrant_wrapper.get_client()
        self.embedder = get_embedder()
        self.collection_name = "quiz"
        self.categories = CATEGORY_MAP

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
형식: 4지선다 (1, 2, 3, 4 중 택1)

다음 JSON 형식으로 정확히 응답해주세요:
{{
    "quiz_title": "퀴즈의 간단한 제목 (50자 이내)",
    "quiz_content": "퀴즈 문제 내용\\n\\n1. 선택지 1\\n2. 선택지 2\\n3. 선택지 3\\n4. 선택지 4",
    "quiz_correct_answer": "1, 2, 3, 4 중 하나",
    "quiz_additional_information": "정답에 대한 상세한 설명과 추가 정보"
}}

주의사항:
- quiz_correct_answer는 반드시 "1", "2", "3", "4" 중 하나여야 합니다
- 선택지는 명확하게 구분되어야 합니다
- 난이도는 일반 성인이 풀 수 있는 수준으로 합니다"""

    async def retrieve_recent_quizzes(self, category_id: int, limit: int = 20) -> List[Dict[str, Any]]:
        try:
            from qdrant_client.models import ScrollRequest, models
            results, _ = self.qdrant_client.scroll(
                collection_name=self.collection_name,
                scroll_filter=Filter(
                    must=[
                        FieldCondition(
                            key="category_id",
                            match=MatchValue(value=category_id)
                        )
                    ]
                ),
                limit=limit,
                with_payload=True,
                with_vectors=False
            )

            recent_quizzes = []
            for point in results:
                recent_quizzes.append({
                    "title": point.payload.get("quiz_title", ""),
                    "content": point.payload.get("quiz_content", ""),
                    "type": point.payload.get("quiz_type", "")
                })

            return recent_quizzes
        except Exception as e:
            logger.error(f"Error retrieving recent quizzes: {e}")
            return []

    async def generate_quiz_prompt_with_context(self, category_id: int, quiz_type: QuizType, existing_quizzes: List[Dict] = None) -> str:
        category_name = self.categories.get(category_id, "일반")

        existing_context = ""
        if existing_quizzes:
            existing_titles = [q["title"] for q in existing_quizzes[:10]]
            if existing_titles:
                existing_context = "\n\n이미 존재하는 퀴즈 제목들 (중복되지 않도록 주의):\n"
                existing_context += "\n".join([f"- {title}" for title in existing_titles])
                existing_context += "\n\n위 퀴즈들과 완전히 다른 주제와 내용으로 새로운 퀴즈를 생성해주세요."

        if quiz_type == QuizType.OX:
            return f"""다음 조건에 맞는 OX 퀴즈를 1개 생성해주세요:

카테고리: {category_name}
형식: OX 퀴즈 (참/거짓을 판단하는 문제)
대상: MZ세대(20-30대) 독립생활을 시작하거나 준비하는 사람들
{existing_context}

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
- 난이도는 일반 성인이 풀 수 있는 수준으로 합니다
- 기존 퀴즈와 중복되지 않는 참신한 문제를 만들어주세요

문제 예시 방향:
- "요리/식품관리": 자취 필수 (계란 신선도, 밥 보관, 냉장고 정리), 요리 기초 (양념 비율, 칼질법), 장보기 (제철 식재료, 유통기한)
- "청소/세탁": 세탁 기호 의미, 얼룩별 제거법, 청소 루틴, 이불 세탁 주기, 옷 개는 법
- "생활수리/DIY": 화장실 막힘, 수도꼭지 누수, 드라이버 사용법, 가구 조립 순서
- "생활경제/계약": 전월세 필수 확인사항, 체크카드 vs 신용카드, 4대보험, 급여명세서 읽기, 연말정산
- "이사/인테리어": 이사 체크리스트, 방향/층수 고려사항, 조명 선택, 원룸 공간 분리
- "육아/반려동물": 예방접종 시기, 사료 선택, 중성화 시기, 펫티켓, 병원비 관리
- "환경/건강": 플라스틱 분류, 재활용 마크, 영양제 복용법, 화상 처치, 구급함 구성
- "스마트홈/가전": 필터 청소, 대기전력, 에너지 등급, 멀티탭 사용법, 와이파이 보안

추가 생활 상식:
- 사회생활 예절: 명함 교환법, 회식 매너, 이메일 작성법, 경조사 예절
- 대중교통: 환승 할인, 교통카드 종류, 분실물 찾기
- 병원/약국: 처방전 유효기간, 제네릭 의약품, 실비보험 청구
- 관공서: 주민등록등본, 인감증명서, 전입신고
- 은행 업무: 통장 개설, 공동인증서, 이체 한도"""
        else:
            return f"""다음 조건에 맞는 4지선다 퀴즈를 1개 생성해주세요:

카테고리: {category_name}
형식: 4지선다 (1, 2, 3, 4 중 택1)
대상: MZ세대(20-30대) 독립생활을 시작하거나 준비하는 사람들
{existing_context}

다음 JSON 형식으로 정확히 응답해주세요:
{{
    "quiz_title": "퀴즈의 간단한 제목 (50자 이내)",
    "quiz_content": "퀴즈 문제 내용\\n\\n1. 선택지 1\\n2. 선택지 2\\n3. 선택지 3\\n4. 선택지 4",
    "quiz_correct_answer": "1, 2, 3, 4 중 하나",
    "quiz_additional_information": "정답에 대한 상세한 설명과 추가 정보"
}}

주의사항:
- quiz_correct_answer는 반드시 "1", "2", "3", "4" 중 하나여야 합니다
- 선택지는 명확하게 구분되어야 합니다
- 난이도는 일반 성인이 풀 수 있는 수준으로 합니다
- 기존 퀴즈와 중복되지 않는 참신한 문제를 만들어주세요

문제 예시 방향:
- "요리/식품관리": 자취 필수 (계란 신선도, 밥 보관, 냉장고 정리), 요리 기초 (양념 비율, 칼질법), 장보기 (제철 식재료, 유통기한)
- "청소/세탁": 세탁 기호 의미, 얼룩별 제거법, 청소 루틴, 이불 세탁 주기, 옷 개는 법
- "생활수리/DIY": 화장실 막힘, 수도꼭지 누수, 드라이버 사용법, 가구 조립 순서
- "생활경제/계약": 전월세 필수 확인사항, 체크카드 vs 신용카드, 4대보험, 급여명세서 읽기, 연말정산
- "이사/인테리어": 이사 체크리스트, 방향/층수 고려사항, 조명 선택, 원룸 공간 분리
- "육아/반려동물": 예방접종 시기, 사료 선택, 중성화 시기, 펫티켓, 병원비 관리
- "환경/건강": 플라스틱 분류, 재활용 마크, 영양제 복용법, 화상 처치, 구급함 구성
- "스마트홈/가전": 필터 청소, 대기전력, 에너지 등급, 멀티탭 사용법, 와이파이 보안

추가 생활 상식:
- 사회생활 예절: 명함 교환법, 회식 매너, 이메일 작성법, 경조사 예절
- 대중교통: 환승 할인, 교통카드 종류, 분실물 찾기
- 병원/약국: 처방전 유효기간, 제네릭 의약품, 실비보험 청구
- 관공서: 주민등록등본, 인감증명서, 전입신고
- 은행 업무: 통장 개설, 공동인증서, 이체 한도"""

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

    async def generate_quiz_with_rag(self, request: QuizGenerationRequest) -> List[Quiz]:
        generated_quizzes = []
        existing_quizzes = await self.retrieve_recent_quizzes(
            category_id=request.category_id,
            limit=20
        )

        for _ in range(request.count):
            max_retries = 3
            retry_count = 0

            while retry_count < max_retries:
                try:
                    quiz_type = request.quiz_type or QuizType.OX
                    prompt = await self.generate_quiz_prompt_with_context(
                        request.category_id,
                        quiz_type,
                        existing_quizzes
                    )

                    response = await self.openai_client.chat.completions.create(
                        model="gpt-4o-mini",
                        messages=[
                            {"role": "system", "content": "You are a creative quiz generator. Generate unique and interesting quizzes that are different from existing ones. Always respond with valid JSON only."},
                            {"role": "user", "content": prompt}
                        ],
                        response_format={"type": "json_object"},
                        temperature=0.9
                    )

                    quiz_data = json.loads(response.choices[0].message.content)

                    similar_titles = await self.check_title_similarity(
                        quiz_data["quiz_title"],
                        request.category_id,
                        threshold=0.75
                    )

                    if similar_titles:
                        logger.info(f"Similar quiz found: {similar_titles[0]['title']} (score: {similar_titles[0]['score']})")
                        existing_quizzes.append({"title": quiz_data["quiz_title"]})
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
                    existing_quizzes.append({"title": quiz_data["quiz_title"]})
                    break

                except Exception as e:
                    logger.error(f"Error generating quiz: {e}")
                    retry_count += 1
                    if retry_count >= max_retries:
                        raise Exception(f"Failed to generate quiz after {max_retries} retries")
                    await asyncio.sleep(1)

        return generated_quizzes

    async def generate_quiz(self, request: QuizGenerationRequest) -> List[Quiz]:
        return await self.generate_quiz_with_rag(request)

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