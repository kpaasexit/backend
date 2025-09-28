import asyncio
import random
from datetime import datetime, time
from typing import List, Dict
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger

from app.services.quiz.service import QuizService
from app.models.quiz import QuizType, Quiz
from app.core.logger import LoggerSetup
from app.config import get_settings

logger = LoggerSetup.get_logger(__name__)
settings = get_settings()


class QuizScheduler:
    def __init__(self):
        self.quiz_service = QuizService()
        self.scheduler = AsyncIOScheduler()
        self.categories = {
            1: "과학",
            2: "역사",
            3: "예술",
            4: "스포츠",
            5: "지리",
            6: "상식",
            7: "IT",
            8: "정치"
        }

        # Spring gRPC 서버 설정
        self.spring_grpc_host = "localhost"
        self.spring_grpc_port = 9081

    def start(self):
        """스케줄러 시작 (매일 새벽 2시 실행)"""
        if not self.scheduler.running:
            # 매일 새벽 2시에 실행
            self.scheduler.add_job(
                self._run_daily_job,
                CronTrigger(hour=2, minute=0),
                id='daily_quiz_generation',
                replace_existing=True
            )
            self.scheduler.start()
            logger.info("Quiz scheduler started - will run daily at 2:00 AM")

    def stop(self):
        """스케줄러 중지"""
        if self.scheduler.running:
            self.scheduler.shutdown()
            logger.info("Quiz scheduler stopped")

    async def generate_daily_quizzes(self) -> List[Quiz]:
        """하루 10문제 생성 (8종 카테고리 골고루 분배)"""
        logger.info("Starting daily quiz generation...")

        try:
            # 카테고리별 문제 개수 결정
            quiz_distribution = self._calculate_quiz_distribution(10)
            generated_quizzes = []

            for category_id, count in quiz_distribution.items():
                if count == 0:
                    continue

                logger.info(f"Generating {count} quizzes for category {self.categories[category_id]}")

                for _ in range(count):
                    # OX와 4지선다 랜덤 선택
                    quiz_type = random.choice([QuizType.OX, QuizType.FOUR_LIMBS])

                    try:
                        # 퀴즈 생성 (중복 방지 로직 포함)
                        quiz = await self._generate_unique_quiz(category_id, quiz_type)
                        if quiz:
                            generated_quizzes.append(quiz)
                            logger.info(f"Generated quiz: {quiz.quiz_title}")
                    except Exception as e:
                        logger.error(f"Failed to generate quiz for category {category_id}: {e}")

            # Spring 서버로 생성된 퀴즈 전송
            if generated_quizzes:
                await self._send_quizzes_to_spring(generated_quizzes)

            logger.info(f"Daily quiz generation completed. Generated {len(generated_quizzes)} quizzes")
            return generated_quizzes

        except Exception as e:
            logger.error(f"Error in daily quiz generation: {e}")
            return []

    def _calculate_quiz_distribution(self, total_count: int) -> Dict[int, int]:
        """카테고리별 문제 개수 균등 분배"""
        categories = list(range(1, 9))  # 1~8 카테고리
        base_count = total_count // len(categories)
        remainder = total_count % len(categories)

        distribution = {cat: base_count for cat in categories}

        # 나머지를 랜덤하게 분배
        if remainder > 0:
            random_categories = random.sample(categories, remainder)
            for cat in random_categories:
                distribution[cat] += 1

        return distribution

    async def _generate_unique_quiz(self, category_id: int, quiz_type: QuizType) -> Quiz:
        """중복 방지 로직이 포함된 퀴즈 생성"""
        max_retries = 3

        for attempt in range(max_retries):
            try:
                # 기존 퀴즈 제목들 가져오기 (최근 100개)
                recent_quizzes = await self.quiz_service.list_quizzes(
                    category_id=category_id,
                    limit=100
                )
                existing_titles = [q.quiz_title for q in recent_quizzes]

                # GPT를 통한 퀴즈 생성
                generated_quizzes = await self.quiz_service.generate_quiz(
                    category_id=category_id,
                    count=1,
                    quiz_type=quiz_type
                )

                if generated_quizzes:
                    quiz = generated_quizzes[0]

                    # 중복 체크
                    is_duplicate = await self.quiz_service.check_quiz_similarity(
                        title=quiz.quiz_title,
                        category_id=category_id,
                        threshold=0.8
                    )

                    if not is_duplicate["is_duplicate"]:
                        return quiz

                    logger.warning(f"Duplicate quiz detected, retrying... (attempt {attempt + 1})")

            except Exception as e:
                logger.error(f"Error generating unique quiz: {e}")

        return None

    async def _send_quizzes_to_spring(self, quizzes: List[Quiz]):
        """생성된 퀴즈를 Spring gRPC 서버로 전송"""
        try:
            # Spring 클라이언트 사용
            from app.grpc_service.spring_client import get_spring_quiz_client
            client = get_spring_quiz_client()

            await client.connect()

            success_count = 0
            for quiz in quizzes:
                try:
                    # Spring 서버로 전송
                    success = await client.send_quiz(quiz)

                    if success:
                        success_count += 1
                        logger.info(f"Successfully sent quiz {quiz.quiz_id} to Spring server")
                    else:
                        logger.error(f"Failed to send quiz {quiz.quiz_id} to Spring server")

                except Exception as e:
                    logger.error(f"Failed to send quiz {quiz.quiz_id} to Spring: {e}")

            await client.disconnect()
            logger.info(f"Sent {success_count}/{len(quizzes)} quizzes to Spring server successfully")

        except Exception as e:
            logger.error(f"Failed to connect to Spring gRPC server: {e}")

    def _run_daily_job(self):
        """스케줄러 작업 실행 (동기 래퍼)"""
        asyncio.create_task(self.generate_daily_quizzes())


# 싱글톤 인스턴스
_quiz_scheduler = None

def get_quiz_scheduler() -> QuizScheduler:
    """Quiz 스케줄러 싱글톤 인스턴스 반환"""
    global _quiz_scheduler
    if _quiz_scheduler is None:
        _quiz_scheduler = QuizScheduler()
    return _quiz_scheduler