import grpc
import asyncio
from typing import Optional, List, Dict, Any
from datetime import datetime

from app.core.logger import LoggerSetup
from app.config import get_settings
from app.models.quiz import Quiz

import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent))

logger = LoggerSetup.get_logger(__name__)
settings = get_settings()


class SpringQuizGrpcClient:

    def __init__(self, host: str = "localhost", port: int = 9081):
        self.host = host
        self.port = port
        self.channel = None
        self.stub = None
        self._connected = False

    async def connect(self):
        try:
            from protos.generated import quiz_service_pb2_grpc

            self.channel = grpc.aio.insecure_channel(f'{self.host}:{self.port}')
            self.stub = quiz_service_pb2_grpc.QuizServiceStub(self.channel)
            self._connected = True
            logger.info(f"Connected to Spring gRPC server at {self.host}:{self.port}")
        except Exception as e:
            logger.error(f"Failed to connect to Spring gRPC server: {e}")
            self._connected = False
            raise

    async def disconnect(self):
        if self.channel:
            await self.channel.close()
            self._connected = False
            logger.info("Disconnected from Spring gRPC server")

    async def send_quiz(self, quiz: Quiz) -> bool:
        """
        단일 퀴즈를 Spring 서버로 전송

        Args:
            quiz: 전송할 Quiz 객체

        Returns:
            성공 여부
        """
        if not self._connected:
            await self.connect()

        try:
            from protos.generated import quiz_service_pb2

            # Quiz를 Spring 서버로 전송 (SendQuizToSpring 사용 - 전체 데이터 전송)
            request = quiz_service_pb2.SendQuizToSpringRequest(
                quiz_id=quiz.quiz_id,
                quiz_category_id=quiz.quiz_category_id,
                quiz_title=quiz.quiz_title,
                quiz_content=quiz.quiz_content,
                quiz_type=str(quiz.quiz_type.value) if quiz.quiz_type else "OX",
                quiz_correct_answer=quiz.quiz_correct_answer,
                quiz_additional_information=quiz.quiz_additional_information or ""
            )
            response = await self.stub.SendQuizToSpring(request)

            if response.success:
                logger.info(f"Successfully sent quiz {quiz.quiz_id} to Spring server")
                return True
            else:
                logger.error(f"Failed to send quiz {quiz.quiz_id}: {response.message}")
                return False

        except grpc.RpcError as e:
            logger.error(f"gRPC error sending quiz {quiz.quiz_id}: {e.code()} - {e.details()}")
            return False
        except Exception as e:
            logger.error(f"Error sending quiz {quiz.quiz_id}: {e}")
            return False

    async def send_batch_quizzes(self, quizzes: List[Quiz]) -> Dict[int, bool]:
        """
        여러 퀴즈를 Spring 서버로 일괄 전송

        Args:
            quizzes: 전송할 Quiz 리스트

        Returns:
            {quiz_id: 성공 여부} 딕셔너리
        """
        results = {}

        for quiz in quizzes:
            success = await self.send_quiz(quiz)
            results[quiz.quiz_id] = success

        successful_count = sum(1 for success in results.values() if success)
        logger.info(f"Sent {successful_count}/{len(quizzes)} quizzes successfully")

        return results


# 싱글톤 인스턴스
_spring_client = None

def get_spring_quiz_client() -> SpringQuizGrpcClient:
    """Spring Quiz gRPC 클라이언트 싱글톤 인스턴스 반환"""
    global _spring_client
    if _spring_client is None:
        _spring_client = SpringQuizGrpcClient()
    return _spring_client