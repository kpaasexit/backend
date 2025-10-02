"""Classification handler for gRPC service."""

import time
from typing import List

from app.models.classifier import get_classifier
from .base import BaseHandler

# Import generated protobuf classes
import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent.parent))

from protos.generated import question_service_pb2


class ClassificationHandler(BaseHandler):
    """Handles classification requests."""

    def __init__(self):
        super().__init__()
        self.classifier = get_classifier()

    def classify_category(self, request, context):
        """Classify question category using fine-tuned model."""
        start_time = time.time()

        try:
            self.log_request(
                "ClassifyCategory",
                title_length=len(request.title) if request.title else 0
            )

            # Perform classification (returns category name and confidence)
            category_name, confidence = self.classifier.classify(request.title)

            # Convert category name to ID (1-based for API)
            category_id = self.classifier.get_category_id(category_name)

            if category_id is None:
                self.logger.warning(f"Unknown category: {category_name}, defaulting to 1")
                category_id = 1

            processing_time_ms = int((time.time() - start_time) * 1000)

            self.log_performance(
                "ClassifyCategory",
                start_time,
                category_name=category_name,
                category_id=category_id,
                confidence=f"{confidence:.3f}"
            )

            return question_service_pb2.CategoryResponse(
                category_id=category_id,
                confidence=confidence,
                processing_time_ms=processing_time_ms
            )

        except Exception as e:
            self.handle_error(context, "ClassifyCategory", e)
            # Return default response on error
            return question_service_pb2.CategoryResponse(
                category_id=1,
                confidence=0.0,
                processing_time_ms=int((time.time() - start_time) * 1000)
            )

