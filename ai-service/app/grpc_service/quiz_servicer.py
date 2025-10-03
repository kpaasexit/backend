"""Quiz gRPC service implementation."""

import asyncio
from app.config import get_settings
from app.core.logger import LoggerSetup
from .handlers.quiz import QuizHandler

import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent))

from protos.generated import quiz_service_pb2_grpc

logger = LoggerSetup.get_logger(__name__)


class QuizServicer(quiz_service_pb2_grpc.AIQuizServiceServicer):
    """퀴즈 관련 gRPC 서비스 구현."""

    def __init__(self):
        super().__init__()
        self.settings = get_settings()
        self.quiz_handler = QuizHandler()
        logger.info("Quiz gRPC servicer initialized")

    def UpdateQuiz(self, request, context):
        """퀴즈 수정."""
        return asyncio.run(self.quiz_handler.UpdateQuiz(request, context))

    def GetQuiz(self, request, context):
        """퀴즈 조회."""
        return asyncio.run(self.quiz_handler.GetQuiz(request, context))

    def ListQuizzes(self, request, context):
        """퀴즈 목록 조회."""
        return asyncio.run(self.quiz_handler.ListQuizzes(request, context))

    def GenerateDailyQuizzes(self, request, context):
        """일일 퀴즈 생성 (10문제, 8개 카테고리)."""
        return asyncio.run(self.quiz_handler.GenerateDailyQuizzes(request, context))

    def StartScheduler(self, request, context):
        """퀴즈 생성 스케줄러 시작."""
        return asyncio.run(self.quiz_handler.StartScheduler(request, context))

    def StopScheduler(self, request, context):
        """퀴즈 생성 스케줄러 중지."""
        return asyncio.run(self.quiz_handler.StopScheduler(request, context))

    def GetCategories(self, request, context):
        """카테고리 목록 조회."""
        return asyncio.run(self.quiz_handler.GetCategories(request, context))