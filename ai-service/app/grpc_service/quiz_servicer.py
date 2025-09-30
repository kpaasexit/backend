"""Quiz gRPC service implementation."""

import time
import asyncio
from app.config import get_settings
from app.core.logger import LoggerSetup
from .handlers.quiz import QuizHandler

import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent))

from protos.generated import quiz_service_pb2_grpc

logger = LoggerSetup.get_logger(__name__)


class QuizServicer(quiz_service_pb2_grpc.QuizServiceServicer):
    """퀴즈 관련 gRPC 서비스 구현."""

    def __init__(self):
        super().__init__()
        self.settings = get_settings()
        self.quiz_handler = QuizHandler()
        logger.info("Quiz gRPC servicer initialized")

    def UpdateQuiz(self, request, context):
        """퀴즈 수정."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            return loop.run_until_complete(self.quiz_handler.UpdateQuiz(request, context))
        finally:
            loop.close()

    def GetQuiz(self, request, context):
        """퀴즈 조회."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            return loop.run_until_complete(self.quiz_handler.GetQuiz(request, context))
        finally:
            loop.close()

    def ListQuizzes(self, request, context):
        """퀴즈 목록 조회."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            return loop.run_until_complete(self.quiz_handler.ListQuizzes(request, context))
        finally:
            loop.close()

    def GenerateDailyQuizzes(self, request, context):
        """일일 퀴즈 생성 (10문제, 8개 카테고리)."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            return loop.run_until_complete(self.quiz_handler.GenerateDailyQuizzes(request, context))
        finally:
            loop.close()

    def StartScheduler(self, request, context):
        """퀴즈 생성 스케줄러 시작."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            return loop.run_until_complete(self.quiz_handler.StartScheduler(request, context))
        finally:
            loop.close()

    def StopScheduler(self, request, context):
        """퀴즈 생성 스케줄러 중지."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            return loop.run_until_complete(self.quiz_handler.StopScheduler(request, context))
        finally:
            loop.close()

    def SendQuizToSpring(self, request, context):
        """Spring 서버로 퀴즈 전송."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            return loop.run_until_complete(self.quiz_handler.SendQuizToSpring(request, context))
        finally:
            loop.close()

    def GetCategories(self, request, context):
        """카테고리 목록 조회."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            return loop.run_until_complete(self.quiz_handler.GetCategories(request, context))
        finally:
            loop.close()