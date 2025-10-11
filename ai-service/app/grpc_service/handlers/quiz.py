import logging
from datetime import datetime
from typing import Optional

import grpc
from protos.generated import quiz_service_pb2, quiz_service_pb2_grpc

from app.services.quiz.mysql_service import MySQLQuizService as QuizService
from app.models.quiz import QuizType as ModelQuizType

logger = logging.getLogger(__name__)


class QuizHandler:
    def __init__(self):
        self.quiz_service = QuizService()

    def _convert_quiz_type_to_model(self, quiz_type: quiz_service_pb2.QuizType) -> ModelQuizType:
        if quiz_type == quiz_service_pb2.OX:
            return ModelQuizType.OX
        elif quiz_type == quiz_service_pb2.MULTIPLE:
            return ModelQuizType.MULTIPLE
        else:
            return ModelQuizType.OX

    def _convert_quiz_type_to_proto(self, quiz_type: ModelQuizType) -> quiz_service_pb2.QuizType:
        if quiz_type == ModelQuizType.OX:
            return quiz_service_pb2.OX
        elif quiz_type == ModelQuizType.MULTIPLE:
            return quiz_service_pb2.MULTIPLE
        else:
            return quiz_service_pb2.QUIZ_TYPE_UNSPECIFIED

    def _quiz_to_proto(self, quiz) -> quiz_service_pb2.Quiz:
        return quiz_service_pb2.Quiz(
            quiz_id=quiz.quiz_id,
            quiz_category_id=quiz.quiz_category_id,
            quiz_title=quiz.quiz_title,
            quiz_content=quiz.quiz_content,
            quiz_type=self._convert_quiz_type_to_proto(quiz.quiz_type),
            quiz_correct_answer=quiz.quiz_correct_answer,
            explanation=quiz.explanation or "",
            quiz_created_at=int(quiz.quiz_created_at.timestamp()),
            quiz_updated_at=int(quiz.quiz_updated_at.timestamp())
        )

    async def UpdateQuiz(self, request: quiz_service_pb2.UpdateQuizRequest, context) -> quiz_service_pb2.UpdateQuizResponse:
        try:
            from app.services.quiz import QuizCorrectorService
            corrector = QuizCorrectorService()

            corrected_quiz = await corrector.correct_quiz(request.quiz_id, request.error_description)

            if not corrected_quiz:
                return quiz_service_pb2.UpdateQuizResponse(
                    success=False,
                    message=f"퀴즈 ID {request.quiz_id}를 찾을 수 없거나 수정에 실패했습니다."
                )

            return quiz_service_pb2.UpdateQuizResponse(
                quiz=self._quiz_to_proto(corrected_quiz),
                success=True,
                message=f"AI를 사용하여 퀴즈가 성공적으로 수정되었습니다."
            )
        except Exception as e:
            logger.error(f"Error updating quiz: {e}")
            return quiz_service_pb2.UpdateQuizResponse(
                success=False,
                message=f"퀴즈 수정 실패: {str(e)}"
            )

    async def GetQuiz(self, request: quiz_service_pb2.GetQuizRequest, context) -> quiz_service_pb2.GetQuizResponse:
        try:
            quiz = await self.quiz_service.get_quiz(request.quiz_id)

            if not quiz:
                return quiz_service_pb2.GetQuizResponse(
                    success=False,
                    message=f"퀴즈 ID {request.quiz_id}를 찾을 수 없습니다."
                )

            return quiz_service_pb2.GetQuizResponse(
                quiz=self._quiz_to_proto(quiz),
                success=True,
                message="퀴즈를 성공적으로 조회했습니다."
            )
        except Exception as e:
            logger.error(f"Error getting quiz: {e}")
            return quiz_service_pb2.GetQuizResponse(
                success=False,
                message=f"퀴즈 조회 실패: {str(e)}"
            )

    async def ListQuizzes(self, request: quiz_service_pb2.ListQuizzesRequest, context) -> quiz_service_pb2.ListQuizzesResponse:
        try:
            category_id = request.category_id if request.HasField("category_id") else None
            limit = request.limit if request.limit > 0 else 100

            quizzes = await self.quiz_service.list_quizzes(category_id, limit)

            proto_quizzes = [self._quiz_to_proto(quiz) for quiz in quizzes]

            return quiz_service_pb2.ListQuizzesResponse(
                quizzes=proto_quizzes,
                success=True,
                message=f"{len(quizzes)}개의 퀴즈를 조회했습니다."
            )
        except Exception as e:
            logger.error(f"Error listing quizzes: {e}")
            return quiz_service_pb2.ListQuizzesResponse(
                success=False,
                message=f"퀴즈 목록 조회 실패: {str(e)}"
            )

    async def GenerateDailyQuizzes(self, request, context) -> quiz_service_pb2.GenerateDailyQuizzesResponse:
        """Generate daily quizzes (10 quizzes across 8 categories)."""
        try:
            from app.services.quiz import get_quiz_scheduler
            scheduler = get_quiz_scheduler()

            generated_quizzes = await scheduler.generate_daily_quizzes()

            quiz_ids = [quiz.quiz_id for quiz in generated_quizzes]

            return quiz_service_pb2.GenerateDailyQuizzesResponse(
                quiz_ids=quiz_ids,
                count=len(quiz_ids),
                success=True,
                message=f"Successfully generated {len(quiz_ids)} quizzes"
            )
        except Exception as e:
            logger.error(f"Error generating daily quizzes: {e}")
            return quiz_service_pb2.GenerateDailyQuizzesResponse(
                success=False,
                message=f"Failed to generate daily quizzes: {str(e)}"
            )

    async def StartScheduler(self, request, context) -> quiz_service_pb2.SchedulerStatusResponse:
        try:
            from app.services.quiz import get_quiz_scheduler
            scheduler = get_quiz_scheduler()
            scheduler.start()

            return quiz_service_pb2.SchedulerStatusResponse(
                is_running=scheduler.scheduler.running,
                success=True,
                message="Quiz scheduler started successfully"
            )
        except Exception as e:
            logger.error(f"Error starting scheduler: {e}")
            return quiz_service_pb2.SchedulerStatusResponse(
                is_running=False,
                success=False,
                message=f"Failed to start scheduler: {str(e)}"
            )

    async def StopScheduler(self, request, context) -> quiz_service_pb2.SchedulerStatusResponse:
        try:
            from app.services.quiz import get_quiz_scheduler
            scheduler = get_quiz_scheduler()
            scheduler.stop()

            return quiz_service_pb2.SchedulerStatusResponse(
                is_running=scheduler.scheduler.running,
                success=True,
                message="Quiz scheduler stopped successfully"
            )
        except Exception as e:
            logger.error(f"Error stopping scheduler: {e}")
            return quiz_service_pb2.SchedulerStatusResponse(
                is_running=False,
                success=False,
                message=f"Failed to stop scheduler: {str(e)}"
            )


    async def GetCategories(self, request, context) -> quiz_service_pb2.GetCategoriesResponse:
        try:
            from app.core.constants import CATEGORY_MAP

            categories = []
            for category_id in sorted(CATEGORY_MAP.keys()):
                category = quiz_service_pb2.Category(
                    category_id=category_id,
                    category_name=CATEGORY_MAP[category_id],
                    description=""
                )
                categories.append(category)

            return quiz_service_pb2.GetCategoriesResponse(
                categories=categories,
                success=True
            )
        except Exception as e:
            logger.error(f"Error getting categories: {e}")
            return quiz_service_pb2.GetCategoriesResponse(
                categories=[],
                success=False
            )

