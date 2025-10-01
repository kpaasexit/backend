"""Answer generation handler for gRPC service."""

import time

from app.services.gpt import get_gpt_service
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

    async def generate_ai_answer(self, request, context):
        """Generate AI answer for a question using AnswerRequest."""
        start_time = time.time()

        try:
            self.log_request(
                "GenerateAIAnswer",
                question_length=len(request.question) if request.question else 0
            )

            # Generate answer (always uses cache)
            result = await self.gpt_service.agenerate_answer(
                question=request.question
            )

            processing_time_ms = int((time.time() - start_time) * 1000)

            self.log_performance(
                "GenerateAIAnswer",
                start_time,
                tokens_used=result.get("tokens_used", 0)
            )

            return question_service_pb2.AnswerResponse(
                answer=result["answer"],
                tokens_used=result.get("tokens_used", 0),
                processing_time_ms=processing_time_ms
            )

        except Exception as e:
            self.handle_error(context, "GenerateAIAnswer", e)
            return question_service_pb2.AnswerResponse()

