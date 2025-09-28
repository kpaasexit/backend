import logging
from datetime import datetime
from typing import Optional

import grpc
from protos.generated import quiz_service_pb2, quiz_service_pb2_grpc

from app.services.quiz import QuizService
from app.models.quiz import QuizType as ModelQuizType

logger = logging.getLogger(__name__)


class QuizHandler:
    def __init__(self):
        self.quiz_service = QuizService()

    def _convert_quiz_type_to_model(self, quiz_type: quiz_service_pb2.QuizType) -> ModelQuizType:
        if quiz_type == quiz_service_pb2.OX:
            return ModelQuizType.OX
        elif quiz_type == quiz_service_pb2.FOUR_LIMBS:
            return ModelQuizType.FOUR_LIMBS
        else:
            return ModelQuizType.OX

    def _convert_quiz_type_to_proto(self, quiz_type: ModelQuizType) -> quiz_service_pb2.QuizType:
        if quiz_type == ModelQuizType.OX:
            return quiz_service_pb2.OX
        elif quiz_type == ModelQuizType.FOUR_LIMBS:
            return quiz_service_pb2.FOUR_LIMBS
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
            quiz_additional_information=quiz.quiz_additional_information or "",
            quiz_created_at=int(quiz.quiz_created_at.timestamp()),
            quiz_updated_at=int(quiz.quiz_updated_at.timestamp())
        )

    async def CreateQuiz(self, request: quiz_service_pb2.CreateQuizRequest, context) -> quiz_service_pb2.CreateQuizResponse:
        try:
            from app.models.quiz import QuizCreate

            quiz_create = QuizCreate(
                quiz_category_id=request.quiz_category_id,
                quiz_title=request.quiz_title,
                quiz_content=request.quiz_content,
                quiz_type=self._convert_quiz_type_to_model(request.quiz_type),
                quiz_correct_answer=request.quiz_correct_answer,
                quiz_additional_information=request.quiz_additional_information or None
            )

            quiz = await self.quiz_service.create_quiz(quiz_create)

            return quiz_service_pb2.CreateQuizResponse(
                quiz=self._quiz_to_proto(quiz),
                success=True,
                message="퀴즈가 성공적으로 생성되었습니다."
            )
        except Exception as e:
            logger.error(f"Error creating quiz: {e}")
            return quiz_service_pb2.CreateQuizResponse(
                success=False,
                message=f"퀴즈 생성 실패: {str(e)}"
            )

    async def UpdateQuiz(self, request: quiz_service_pb2.UpdateQuizRequest, context) -> quiz_service_pb2.UpdateQuizResponse:
        try:
            from app.models.quiz import QuizUpdate

            update_dict = {}
            if request.HasField("quiz_category_id"):
                update_dict["quiz_category_id"] = request.quiz_category_id
            if request.HasField("quiz_title"):
                update_dict["quiz_title"] = request.quiz_title
            if request.HasField("quiz_content"):
                update_dict["quiz_content"] = request.quiz_content
            if request.HasField("quiz_type"):
                update_dict["quiz_type"] = self._convert_quiz_type_to_model(request.quiz_type)
            if request.HasField("quiz_correct_answer"):
                update_dict["quiz_correct_answer"] = request.quiz_correct_answer
            if request.HasField("quiz_additional_information"):
                update_dict["quiz_additional_information"] = request.quiz_additional_information

            quiz_update = QuizUpdate(**update_dict)
            quiz = await self.quiz_service.update_quiz(request.quiz_id, quiz_update)

            if not quiz:
                return quiz_service_pb2.UpdateQuizResponse(
                    success=False,
                    message=f"퀴즈 ID {request.quiz_id}를 찾을 수 없습니다."
                )

            return quiz_service_pb2.UpdateQuizResponse(
                quiz=self._quiz_to_proto(quiz),
                success=True,
                message="퀴즈가 성공적으로 수정되었습니다."
            )
        except Exception as e:
            logger.error(f"Error updating quiz: {e}")
            return quiz_service_pb2.UpdateQuizResponse(
                success=False,
                message=f"퀴즈 수정 실패: {str(e)}"
            )

    async def GenerateQuiz(self, request: quiz_service_pb2.GenerateQuizRequest, context) -> quiz_service_pb2.GenerateQuizResponse:
        try:
            from app.models.quiz import QuizGenerationRequest

            generation_request = QuizGenerationRequest(
                category_id=request.category_id,
                count=request.count,
                quiz_type=self._convert_quiz_type_to_model(request.quiz_type) if request.HasField("quiz_type") else None
            )

            quizzes = await self.quiz_service.generate_quiz(generation_request)

            proto_quizzes = [self._quiz_to_proto(quiz) for quiz in quizzes]

            return quiz_service_pb2.GenerateQuizResponse(
                quizzes=proto_quizzes,
                success=True,
                message=f"{len(quizzes)}개의 퀴즈가 성공적으로 생성되었습니다."
            )
        except Exception as e:
            logger.error(f"Error generating quiz: {e}")
            return quiz_service_pb2.GenerateQuizResponse(
                success=False,
                message=f"퀴즈 생성 실패: {str(e)}"
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

    async def CheckQuizSimilarity(self, request: quiz_service_pb2.CheckQuizSimilarityRequest, context) -> quiz_service_pb2.CheckQuizSimilarityResponse:
        try:
            similar_titles = await self.quiz_service.check_title_similarity(
                request.title,
                request.category_id,
                request.threshold if request.threshold > 0 else 0.85
            )

            proto_similar = [
                quiz_service_pb2.SimilarQuiz(title=item["title"], score=item["score"])
                for item in similar_titles
            ]

            return quiz_service_pb2.CheckQuizSimilarityResponse(
                title=request.title,
                category_id=request.category_id,
                threshold=request.threshold,
                similar_quizzes=proto_similar,
                is_duplicate=len(similar_titles) > 0,
                success=True,
                message="유사도 검사가 완료되었습니다."
            )
        except Exception as e:
            logger.error(f"Error checking similarity: {e}")
            return quiz_service_pb2.CheckQuizSimilarityResponse(
                title=request.title,
                category_id=request.category_id,
                threshold=request.threshold,
                success=False,
                message=f"유사도 검사 실패: {str(e)}"
            )

    async def GenerateDailyQuizzes(self, request, context) -> quiz_service_pb2.GenerateDailyQuizzesResponse:
        """Generate daily quizzes (10 quizzes across 8 categories)."""
        try:
            from app.services.quiz import get_quiz_scheduler
            scheduler = get_quiz_scheduler()

            # Generate quizzes
            generated_quizzes = await scheduler.generate_daily_quizzes()

            # Extract quiz IDs
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

    async def CorrectQuiz(self, request, context) -> quiz_service_pb2.CorrectQuizResponse:
        """Correct quiz errors using GPT-4."""
        try:
            from app.services.quiz import QuizCorrectorService
            corrector = QuizCorrectorService()

            # Correct the quiz
            error_description = request.error_description if request.HasField('error_description') else None
            corrected_quiz = await corrector.correct_quiz(request.quiz_id, error_description)

            if not corrected_quiz:
                return quiz_service_pb2.CorrectQuizResponse(
                    success=False,
                    message=f"Quiz {request.quiz_id} not found or correction failed"
                )

            # Convert to protobuf Quiz message
            quiz_proto = quiz_service_pb2.Quiz(
                quiz_id=corrected_quiz.quiz_id,
                quiz_category_id=corrected_quiz.quiz_category_id,
                quiz_title=corrected_quiz.quiz_title,
                quiz_content=corrected_quiz.quiz_content,
                quiz_type=quiz_service_pb2.QuizType.Value(corrected_quiz.quiz_type.value),
                quiz_correct_answer=corrected_quiz.quiz_correct_answer,
                quiz_additional_information=corrected_quiz.quiz_additional_information or ""
            )

            return quiz_service_pb2.CorrectQuizResponse(
                quiz=quiz_proto,
                success=True,
                message=f"Successfully corrected quiz {request.quiz_id}"
            )
        except Exception as e:
            logger.error(f"Error correcting quiz: {e}")
            return quiz_service_pb2.CorrectQuizResponse(
                success=False,
                message=f"Failed to correct quiz: {str(e)}"
            )

    async def StartScheduler(self, request, context) -> quiz_service_pb2.SchedulerStatusResponse:
        """Start quiz generation scheduler."""
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
        """Stop quiz generation scheduler."""
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

    async def SendQuizToSpring(self, request, context) -> quiz_service_pb2.SendQuizToSpringResponse:
        """Send quiz to Spring gRPC server."""
        try:
            # Get the quiz
            quiz = await self.quiz_service.get_quiz(request.quiz_id)
            if not quiz:
                return quiz_service_pb2.SendQuizToSpringResponse(
                    success=False,
                    message=f"Quiz {request.quiz_id} not found"
                )

            # Send to Spring server
            from app.grpc_service.spring_client import get_spring_quiz_client
            client = get_spring_quiz_client()
            await client.connect()
            success = await client.send_quiz(quiz)
            await client.disconnect()

            if not success:
                return quiz_service_pb2.SendQuizToSpringResponse(
                    success=False,
                    message="Failed to send quiz to Spring server"
                )

            # Convert to protobuf Quiz message
            quiz_proto = quiz_service_pb2.Quiz(
                quiz_id=quiz.quiz_id,
                quiz_category_id=quiz.quiz_category_id,
                quiz_title=quiz.quiz_title,
                quiz_content=quiz.quiz_content,
                quiz_type=quiz_service_pb2.QuizType.Value(quiz.quiz_type.value),
                quiz_correct_answer=quiz.quiz_correct_answer,
                quiz_additional_information=quiz.quiz_additional_information or ""
            )

            return quiz_service_pb2.SendQuizToSpringResponse(
                success=True,
                message=f"Quiz {request.quiz_id} sent to Spring server",
                quiz=quiz_proto
            )
        except Exception as e:
            logger.error(f"Error sending quiz to Spring: {e}")
            return quiz_service_pb2.SendQuizToSpringResponse(
                success=False,
                message=f"Failed to send quiz to Spring: {str(e)}"
            )

