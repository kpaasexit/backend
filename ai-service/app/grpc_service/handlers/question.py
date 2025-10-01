"""Question handler for gRPC service."""

import time
import uuid
from typing import List, Dict, Any

from app.vectordb.question_operations import QuestionVectorOperations
from app.models.embedder import get_embedder
from .base import BaseHandler

# Import generated protobuf classes
import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent.parent))

from protos.generated import question_service_pb2


class QuestionHandler(BaseHandler):
    """Handles question-related requests."""

    def __init__(self):
        super().__init__()
        self.vector_ops = QuestionVectorOperations()
        self.embedder = get_embedder()

    def find_similar_questions(self, request, context):
        """벡터 데이터베이스에서 유사한 질문 찾기."""
        start_time = time.time()

        try:
            self.log_request(
                "FindSimilarQuestions",
                title_length=len(request.title) if request.title else 0,
                content_length=len(request.content) if request.content else 0
            )

            # 쿼리 텍스트 준비
            query_text = request.title
            if request.content:
                query_text = f"{request.title} {request.content}"

            # 임베딩 생성
            embedding = self.embedder.embed(query_text)

            # 유사한 질문 검색 (limit=5, score_threshold=0.5 고정)
            similar_questions = self.vector_ops.search_similar_questions(
                query_vector=embedding,
                limit=5,
                score_threshold=0.5,
                category_filter=None
            )

            # 결과를 protobuf 형식으로 변환
            questions = []
            for sq in similar_questions:
                # created_at 처리 (ISO 형식 문자열을 타임스탬프로 변환)
                created_at = 0
                if "created_at" in sq:
                    try:
                        from datetime import datetime
                        created_at = int(datetime.fromisoformat(sq["created_at"].replace("Z", "+00:00")).timestamp())
                    except:
                        created_at = 0

                question = question_service_pb2.SimilarQuestion(
                    question_id=int(sq.get("question_id", 0)),
                    title=sq.get("title", ""),
                    similarity=sq.get("score", 0.0),
                    content_sample=sq.get("content_sample", ""),
                    created_at=created_at
                )
                questions.append(question)

            self.log_performance(
                "FindSimilarQuestions",
                start_time,
                found_count=len(questions),
                search_time_ms=int((time.time() - start_time) * 1000)
            )

            return question_service_pb2.SimilarResponse(
                questions=questions,
                total_found=len(questions)
            )

        except Exception as e:
            self.handle_error(context, "FindSimilarQuestions", e)
            return question_service_pb2.SimilarResponse()

    def save_question(self, request, context):
        """벡터 데이터베이스에 질문 저장."""
        start_time = time.time()

        try:
            # 질문 ID 생성 (제공되지 않은 경우)
            # 0이면 자동 생성
            import random
            question_id = request.question_id if request.question_id else random.randint(1000000, 9999999)

            self.log_request(
                "SaveQuestion",
                question_id=question_id,
                title_length=len(request.title) if request.title else 0,
                category_id=request.category_id
            )

            # 임베딩 생성 (제목과 내용 결합)
            text_to_embed = request.title
            if request.content:
                text_to_embed = f"{request.title} {request.content}"

            embedding = self.embedder.embed(text_to_embed)

            # 벡터 데이터베이스에 저장
            success = self.vector_ops.insert_question(
                vector=embedding,
                question_id=question_id,
                title=request.title,
                category_id=request.category_id,
                content_sample=request.content[:500] if request.content else None  # 처음 500자만 저장
            )

            if success:
                self.log_performance(
                    "SaveQuestion",
                    start_time,
                    question_id=question_id,
                    saved=True
                )

                return question_service_pb2.SaveQuestionResponse(
                    success=True,
                    message=f"질문이 성공적으로 저장되었습니다. ID: {question_id}"
                )
            else:
                return question_service_pb2.SaveQuestionResponse(
                    success=False,
                    message="질문 저장에 실패했습니다."
                )

        except Exception as e:
            self.handle_error(context, "SaveQuestion", e)
            return question_service_pb2.SaveQuestionResponse(
                success=False,
                message=f"오류가 발생했습니다: {str(e)}"
            )