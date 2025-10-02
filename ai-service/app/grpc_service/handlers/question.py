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

            query_text = request.title
            if request.content:
                query_text = f"{request.title} {request.content}"

            embedding = self.embedder.embed(query_text)

            similar_questions = self.vector_ops.search_similar_questions(
                query_vector=embedding,
                limit=5,
                score_threshold=0.5,
                category_filter=None
            )

            questions = []
            for sq in similar_questions:
                created_at = 0
                if "created_at" in sq:
                    try:
                        from datetime import datetime
                        created_at = int(datetime.fromisoformat(sq["created_at"].replace("Z", "+00:00")).timestamp())
                    except:
                        created_at = 0

                question_id = 0
                try:
                    qid = sq.get("question_id", 0)
                    question_id = int(qid) if qid else 0
                except (ValueError, TypeError):
                    self.logger.warning(f"Invalid question_id format: {sq.get('question_id')}")
                    question_id = 0

                question = question_service_pb2.SimilarQuestion(
                    question_id=question_id,
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
            if not request.question_id or request.question_id <= 0:
                return question_service_pb2.SaveQuestionResponse(
                    success=False,
                    message="유효하지 않은 question_id입니다. 양수의 BIGINT 값이 필요합니다."
                )

            question_id = request.question_id

            self.log_request(
                "SaveQuestion",
                question_id=question_id,
                title_length=len(request.title) if request.title else 0,
                category_id=request.category_id
            )

            text_to_embed = request.title
            if request.content:
                text_to_embed = f"{request.title} {request.content}"

            embedding = self.embedder.embed(text_to_embed)

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

    def get_questions(self, request, context):
        """질문 ID로 질문 목록 조회 (질문 + 답변 모두 포함)."""
        start_time = time.time()

        try:
            self.log_request(
                "GetQuestions",
                question_id=request.question_id
            )

            # 동일한 question_id를 가진 모든 아이템 조회
            from qdrant_client.models import Filter, FieldCondition, MatchValue

            result = self.vector_ops.client.scroll(
                collection_name=self.vector_ops.collection_name,
                scroll_filter=Filter(
                    must=[
                        FieldCondition(
                            key="question_id",
                            match=MatchValue(value=request.question_id)
                        )
                    ]
                ),
                limit=100,
                with_payload=True,
                with_vectors=False
            )

            items = []
            if result and result[0]:
                points = result[0]

                # 시간순 정렬을 위해 먼저 리스트로 변환
                all_items = []
                for point in points:
                    if point.payload:
                        payload_dict = dict(point.payload)
                        all_items.append(payload_dict)

                # created_at 기준으로 정렬 (오래된 것부터)
                all_items.sort(
                    key=lambda x: x.get("created_at", ""),
                    reverse=False
                )

                # protobuf 메시지로 변환
                for item in all_items:
                    # created_at 처리
                    created_at = 0
                    if "created_at" in item:
                        try:
                            from datetime import datetime
                            created_at = int(datetime.fromisoformat(item["created_at"].replace("Z", "+00:00")).timestamp())
                        except:
                            created_at = 0

                    # question_id를 int로 변환
                    qid = item.get("question_id", request.question_id)
                    try:
                        qid = int(qid) if qid else request.question_id
                    except (ValueError, TypeError):
                        qid = request.question_id

                    question_item = question_service_pb2.QuestionItem(
                        question_id=qid,
                        title=item.get("title", ""),
                        category_id=int(item.get("category_id", 0)),
                        content_sample=item.get("content_sample", ""),
                        answer=item.get("answer", ""),
                        embedding_type=item.get("embedding_type", ""),
                        created_at=created_at
                    )
                    items.append(question_item)

            self.log_performance(
                "GetQuestions",
                start_time,
                found_count=len(items)
            )

            return question_service_pb2.GetQuestionsResponse(
                items=items,
                total_count=len(items)
            )

        except Exception as e:
            self.handle_error(context, "GetQuestions", e)
            return question_service_pb2.GetQuestionsResponse(
                items=[],
                total_count=0
            )