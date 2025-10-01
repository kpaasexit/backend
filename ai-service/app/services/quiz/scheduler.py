import asyncio
import random
from datetime import datetime, time
from typing import List, Dict
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger

from app.services.quiz.mysql_service import MySQLQuizService as QuizService
from app.models.quiz import QuizType, Quiz
from app.core.logger import LoggerSetup
from app.config import get_settings
from app.core.constants import CATEGORY_MAP

logger = LoggerSetup.get_logger(__name__)
settings = get_settings()


class QuizScheduler:
    def __init__(self):
        self.scheduler = AsyncIOScheduler()
        self.categories = CATEGORY_MAP


    def start(self):
        if not self.scheduler.running:
            self.scheduler.add_job(
                self._run_daily_job,
                CronTrigger(hour=2, minute=0),
                id='daily_quiz_generation',
                replace_existing=True
            )
            self.scheduler.start()
            logger.info("Quiz scheduler started - will run daily at 2:00 AM")

    def stop(self):
        if self.scheduler.running:
            self.scheduler.shutdown()
            logger.info("Quiz scheduler stopped")

    async def generate_daily_quizzes(self) -> List[Quiz]:
        logger.info("Starting daily quiz generation...")

        # Create new QuizService instance for this request
        quiz_service = QuizService()

        try:
            quiz_distribution = self._calculate_quiz_distribution(10)
            generated_quizzes = []

            for category_id, count in quiz_distribution.items():
                if count == 0:
                    continue

                logger.info(f"Generating {count} quizzes for category {self.categories[category_id]}")

                for _ in range(count):
                    quiz_type = random.choice([QuizType.OX, QuizType.FOUR_LIMBS])

                    try:
                        quiz = await self._generate_unique_quiz(quiz_service, category_id, quiz_type)
                        if quiz:
                            generated_quizzes.append(quiz)
                            logger.info(f"Generated quiz: {quiz.quiz_title}")
                    except Exception as e:
                        logger.error(f"Failed to generate quiz for category {category_id}: {e}")

            logger.info(f"Daily quiz generation completed. Generated {len(generated_quizzes)} quizzes")
            return generated_quizzes

        except Exception as e:
            logger.error(f"Error in daily quiz generation: {e}")
            return []

    def _calculate_quiz_distribution(self, total_count: int) -> Dict[int, int]:
        categories = list(self.categories.keys())
        num_categories = len(categories)

        if total_count < num_categories:
            logger.warning(f"Quiz count ({total_count}) is less than categories ({num_categories}). Some categories will not receive quizzes.")
            selected_categories = random.sample(categories, total_count)
            distribution = {cat: 1 if cat in selected_categories else 0 for cat in categories}
        else:
            distribution = {cat: 1 for cat in categories}

            remaining = total_count - num_categories

            if remaining > 0:
                extra_distribution = [0] * num_categories
                for i in range(remaining):
                    extra_distribution[i % num_categories] += 1

                random.shuffle(extra_distribution)

                for cat, extra in zip(categories, extra_distribution):
                    distribution[cat] += extra

        return distribution

    async def _generate_unique_quiz(self, quiz_service: QuizService, category_id: int, quiz_type: QuizType) -> Quiz:
        from app.models.quiz import QuizGenerationRequest

        try:
            request = QuizGenerationRequest(
                category_id=category_id,
                count=1,
                quiz_type=quiz_type
            )

            generated_quizzes = await quiz_service.generate_quiz(request)

            if generated_quizzes:
                return generated_quizzes[0]
            else:
                logger.warning(f"No quiz generated for category {category_id}")
                return None

        except Exception as e:
            logger.error(f"Error generating unique quiz: {e}")
            return None

        return None


    def _run_daily_job(self):
        asyncio.create_task(self.generate_daily_quizzes())


_quiz_scheduler = None

def get_quiz_scheduler() -> QuizScheduler:
    global _quiz_scheduler
    if _quiz_scheduler is None:
        _quiz_scheduler = QuizScheduler()
    return _quiz_scheduler