"""Answer generation handler for gRPC service."""

import time
from typing import Optional, List, Dict, Any

from app.services.gpt import get_gpt_service
from app.vectordb.question_operations import QuestionVectorOperations
from app.models.embedder import get_embedder
from .base import BaseHandler

# Import generated protobuf classes
import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent.parent))

from protos.generated import question_service_pb2


class AnswerHandler(BaseHandler):
    """Handles answer generation requests."""

    def __init__(self):
        super().__init__()
        self.gpt_service = get_gpt_service()
        self.vector_ops = QuestionVectorOperations()
        self.embedder = get_embedder()

    def _get_related_questions_by_id(self, question_id: int) -> List[Dict[str, Any]]:
        """
        동일한 question_id를 가진 연계 질문들을 조회.

        Args:
            question_id: 질문 ID

        Returns:
            연계 질문 리스트
        """
        try:
            from qdrant_client.models import Filter, FieldCondition, MatchValue

            # question_id로 필터링하여 모든 연계 질문 검색
            result = self.vector_ops.client.scroll(
                collection_name=self.vector_ops.collection_name,
                scroll_filter=Filter(
                    must=[
                        FieldCondition(
                            key="question_id",
                            match=MatchValue(value=question_id)
                        )
                    ]
                ),
                limit=10,  # 최대 10개의 연계 질문
                with_payload=True,
                with_vectors=False
            )

            # 결과를 딕셔너리 형태로 변환
            related_questions = []
            if result and result[0]:  # (points, next_offset)
                points = result[0]
                for point in points:
                    if point.payload:
                        related_questions.append(dict(point.payload))

            return related_questions

        except Exception as e:
            self.logger.warning(f"Failed to get related questions: {e}")
            return []

    def _get_rag_context(self, question: str, question_id: int) -> Optional[str]:
        """
        RAG를 위한 컨텍스트 생성 (동일 question_id의 연계 질문과 답변 활용).

        Args:
            question: 사용자 질문
            question_id: 질문 ID (0이면 RAG 미사용)

        Returns:
            RAG 컨텍스트 문자열 또는 None
        """
        try:
            # question_id가 0이면 RAG 사용 안 함
            if not question_id or question_id <= 0:
                self.logger.debug("RAG skipped: question_id is 0 or invalid")
                return None

            # 동일한 question_id를 가진 연계 질문들 조회
            related_items = self._get_related_questions_by_id(question_id)

            if not related_items:
                self.logger.info(f"No related items found for question_id: {question_id}")
                return None

            # created_at 기준으로 정렬 (오래된 것부터)
            related_items.sort(
                key=lambda x: x.get("created_at", ""),
                reverse=False
            )

            # RAG 컨텍스트 구성 (질문과 답변을 모두 포함)
            context_parts = [f"이전 대화 기록 (총 {len(related_items)}개):"]

            conversation_count = 0
            for item in related_items:
                embedding_type = item.get("embedding_type", "mixed")

                if embedding_type == "answer":
                    # 답변인 경우
                    answer = item.get("answer", "")
                    if answer:
                        context_parts.append(f"[답변] {answer[:300]}...")  # 답변은 300자까지
                else:
                    # 질문인 경우
                    conversation_count += 1
                    title = item.get("title", "")
                    content = item.get("content_sample", "")

                    context_parts.append(f"\n[질문 {conversation_count}] {title}")
                    if content:
                        content_preview = content[:200] + "..." if len(content) > 200 else content
                        context_parts.append(f"내용: {content_preview}")

            rag_context = "\n".join(context_parts)
            self.logger.info(f"✓ RAG context created with {len(related_items)} items (questions + answers) for question_id: {question_id}")

            return rag_context

        except Exception as e:
            self.logger.warning(f"RAG context generation failed: {e}", exc_info=True)
            return None

    async def generate_ai_answer(self, request, context):
        """Generate AI answer for the latest question of a given question_id."""
        start_time = time.time()

        try:
            # Validate question_id
            if not request.question_id or request.question_id <= 0:
                self.handle_error(context, "GenerateAIAnswer", ValueError("question_id is required"))
                return question_service_pb2.AnswerResponse()

            # Get the latest question for this question_id
            latest_question = self.vector_ops.get_latest_question_by_id(request.question_id)

            if not latest_question:
                self.logger.error(f"No question found for question_id: {request.question_id}")
                self.handle_error(context, "GenerateAIAnswer", ValueError(f"No question found for question_id: {request.question_id}"))
                return question_service_pb2.AnswerResponse()

            # Extract question text
            question_text = latest_question.get("title", "")
            content_sample = latest_question.get("content_sample", "")

            # Combine title and content for full question
            full_question = question_text
            if content_sample:
                full_question = f"{question_text}\n{content_sample}"

            self.log_request(
                "GenerateAIAnswer",
                question_id=request.question_id,
                question_length=len(full_question)
            )

            self.logger.info(f"Processing question_id: {request.question_id}, question: {question_text[:50]}...")

            # RAG 컨텍스트 생성 (이전 대화 이력 포함)
            rag_context = self._get_rag_context(
                question=full_question,
                question_id=request.question_id
            )

            # RAG 컨텍스트가 있으면 질문에 추가
            enhanced_question = full_question
            if rag_context:
                enhanced_question = f"{rag_context}\n\n현재 질문: {full_question}"
                self.logger.info(f"✓ RAG enabled - Enhanced with conversation history (question_id: {request.question_id})")
            else:
                self.logger.info("✗ RAG disabled - No conversation history")

            # Generate answer (always uses cache)
            result = await self.gpt_service.agenerate_answer(
                question=enhanced_question
            )

            processing_time_ms = int((time.time() - start_time) * 1000)

            # Save answer to vector DB
            try:
                self.vector_ops.add_answer_to_question(
                    question_id=request.question_id,
                    title=question_text,  # Use the latest question title
                    answer=result["answer"]
                )
                self.logger.info(f"✓ Answer saved to vector DB for question_id: {request.question_id}")
            except Exception as e:
                self.logger.warning(f"Failed to save answer to vector DB: {e}")

            self.log_performance(
                "GenerateAIAnswer",
                start_time,
                tokens_used=result.get("tokens_used", 0),
                used_rag=bool(rag_context)
            )

            return question_service_pb2.AnswerResponse(
                answer=result["answer"],
                tokens_used=result.get("tokens_used", 0),
                processing_time_ms=processing_time_ms
            )

        except Exception as e:
            self.handle_error(context, "GenerateAIAnswer", e)
            return question_service_pb2.AnswerResponse()

