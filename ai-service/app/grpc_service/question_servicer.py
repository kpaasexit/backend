"""Question gRPC service implementation."""

import asyncio
import time
from app.config import get_settings
from app.core.logger import LoggerSetup
from .handlers import (
    ClassificationHandler,
    AnswerHandler
)
from .handlers.question import QuestionHandler

import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent))

from protos.generated import question_service_pb2_grpc
from protos.generated import question_service_pb2

logger = LoggerSetup.get_logger(__name__)


class QuestionServicer(question_service_pb2_grpc.AIQuestionServiceServicer):
    """질문 처리 관련 gRPC 서비스 구현."""

    def __init__(self):
        super().__init__()
        self.settings = get_settings()
        self.classification_handler = ClassificationHandler()
        self.answer_handler = AnswerHandler()
        self.question_handler = QuestionHandler()
        logger.info("Question gRPC servicer initialized")

    def ClassifyCategory(self, request, context):
        """질문 카테고리 분류."""
        return self.classification_handler.classify_category(request, context)

    def GenerateAIAnswer(self, request, context):
        """AI 답변 생성."""
        return asyncio.run(self.answer_handler.generate_ai_answer(request, context))

    def FindSimilarQuestions(self, request, context):
        """벡터 데이터베이스에서 유사한 질문 찾기."""
        return self.question_handler.find_similar_questions(request, context)

    def SaveQuestion(self, request, context):
        """벡터 데이터베이스에 질문 저장."""
        return self.question_handler.save_question(request, context)