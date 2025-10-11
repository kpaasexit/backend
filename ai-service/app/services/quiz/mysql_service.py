import json
import asyncio
from datetime import datetime
from enum import Enum
from typing import List, Optional, Dict, Any
from openai import AsyncOpenAI

from app.models.quiz import (
    Quiz, QuizCreate, QuizUpdate, QuizType,
    QuizGenerationRequest, QuizGenerationPrompt
)
from app.db.database import get_engine
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker
from app.db.repositories import QuizRepository
from app.config import get_settings
from app.core.logger import LoggerSetup
from app.core.constants import CATEGORY_MAP

logger = LoggerSetup.get_logger(__name__)
settings = get_settings()


class MySQLQuizService:
    def __init__(self):
        self.openai_client = AsyncOpenAI(api_key=settings.openai.openai_api_key)
        self.categories = CATEGORY_MAP

    def _get_session(self):
        """Create a new session factory for current event loop"""
        engine = get_engine()
        return async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

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
    "explanation": "정답에 대한 상세한 설명과 추가 정보"
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
    "explanation": "정답에 대한 상세한 설명과 추가 정보"
}}

주의사항:
- quiz_correct_answer는 반드시 "1", "2", "3", "4" 중 하나여야 합니다
- 선택지는 명확하게 구분되어야 합니다
- 난이도는 일반 성인이 풀 수 있는 수준으로 합니다"""

    async def retrieve_recent_quizzes(self, category_id: int, limit: int = 20) -> List[Dict[str, Any]]:
        try:
            AsyncSessionLocal = self._get_session()
            async with AsyncSessionLocal() as session:
                repo = QuizRepository(session)
                quizzes = await repo.list_quizzes(category_id=category_id, limit=limit)

                recent_quizzes = []
                for quiz in quizzes:
                    recent_quizzes.append({
                        "title": quiz.quiz_title,
                        "content": quiz.quiz_content,
                        "type": quiz.quiz_type.value
                    })

                return recent_quizzes
        except Exception as e:
            logger.error(f"Error retrieving recent quizzes from MySQL: {e}")
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
    "explanation": "정답에 대한 상세한 설명과 추가 정보"
}}

주의사항:
- quiz_correct_answer는 반드시 "O" 또는 "X" 중 하나여야 합니다
- 문제는 명확하고 모호하지 않아야 합니다
- 난이도는 일반 성인이 풀 수 있는 수준으로 합니다
- 기존 퀴즈와 중복되지 않는 참신한 문제를 만들어주세요"""
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
    "explanation": "정답에 대한 상세한 설명과 추가 정보"
}}

주의사항:
- quiz_correct_answer는 반드시 "1", "2", "3", "4" 중 하나여야 합니다
- 선택지는 명확하게 구분되어야 합니다
- 난이도는 일반 성인이 풀 수 있는 수준으로 합니다
- 기존 퀴즈와 중복되지 않는 참신한 문제를 만들어주세요"""

    async def check_title_similarity(self, title: str, category_id: int) -> bool:
        """Check if similar title exists in MySQL"""
        try:
            AsyncSessionLocal = self._get_session()
            async with AsyncSessionLocal() as session:
                repo = QuizRepository(session)
                existing_quizzes = await repo.list_quizzes(category_id=category_id, limit=100)

                for quiz in existing_quizzes:
                    if quiz.quiz_title.lower() == title.lower():
                        return True

                return False
        except Exception as e:
            logger.error(f"Error checking title similarity in MySQL: {e}")
            return False

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

                    # Check for duplicate titles
                    is_duplicate = await self.check_title_similarity(
                        quiz_data["quiz_title"],
                        request.category_id
                    )

                    if is_duplicate:
                        logger.info(f"Similar quiz found: {quiz_data['quiz_title']}")
                        existing_quizzes.append({"title": quiz_data["quiz_title"]})
                        retry_count += 1
                        continue

                    quiz = QuizCreate(
                        quiz_category_id=request.category_id,
                        quiz_title=quiz_data["quiz_title"],
                        quiz_content=quiz_data["quiz_content"],
                        quiz_type=quiz_type,
                        quiz_correct_answer=quiz_data["quiz_correct_answer"],
                        explanation=quiz_data["explanation"]
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
        """Create a new quiz in MySQL database"""
        try:
            AsyncSessionLocal = self._get_session()
            async with AsyncSessionLocal() as session:
                repo = QuizRepository(session)
                created_quiz = await repo.create(quiz)
                logger.info(f"Quiz created in MySQL with ID: {created_quiz.quiz_id}")
                return created_quiz
        except Exception as e:
            logger.error(f"Error creating quiz in MySQL: {e}")
            raise

    async def update_quiz(self, quiz_id: int, quiz_update: QuizUpdate) -> Optional[Quiz]:
        """Update an existing quiz in MySQL database"""
        try:
            AsyncSessionLocal = self._get_session()
            async with AsyncSessionLocal() as session:
                repo = QuizRepository(session)
                updated_quiz = await repo.update(quiz_id, quiz_update)

                if updated_quiz:
                    logger.info(f"Quiz {quiz_id} updated in MySQL")
                else:
                    logger.warning(f"Quiz {quiz_id} not found in MySQL")

                return updated_quiz
        except Exception as e:
            logger.error(f"Error updating quiz in MySQL: {e}")
            raise

    async def get_quiz(self, quiz_id: int) -> Optional[Quiz]:
        """Get a quiz by ID from MySQL database"""
        try:
            AsyncSessionLocal = self._get_session()
            async with AsyncSessionLocal() as session:
                repo = QuizRepository(session)
                quiz = await repo.get_by_id(quiz_id)

                if not quiz:
                    logger.warning(f"Quiz {quiz_id} not found in MySQL")

                return quiz
        except Exception as e:
            logger.error(f"Error getting quiz from MySQL: {e}")
            raise

    async def list_quizzes(self, category_id: Optional[int] = None, limit: int = 100) -> List[Quiz]:
        """List quizzes from MySQL database"""
        try:
            AsyncSessionLocal = self._get_session()
            async with AsyncSessionLocal() as session:
                repo = QuizRepository(session)
                quizzes = await repo.list_quizzes(category_id=category_id, limit=limit)
                logger.info(f"Retrieved {len(quizzes)} quizzes from MySQL")
                return quizzes
        except Exception as e:
            logger.error(f"Error listing quizzes from MySQL: {e}")
            raise

    async def create_batch_quizzes(self, quizzes: List[QuizCreate]) -> List[Quiz]:
        """Create multiple quizzes at once in MySQL database"""
        try:
            AsyncSessionLocal = self._get_session()
            async with AsyncSessionLocal() as session:
                repo = QuizRepository(session)
                created_quizzes = await repo.create_batch(quizzes)
                logger.info(f"Created {len(created_quizzes)} quizzes in MySQL")
                return created_quizzes
        except Exception as e:
            logger.error(f"Error creating batch quizzes in MySQL: {e}")
            raise