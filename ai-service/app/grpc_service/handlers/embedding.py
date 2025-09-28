"""Embedding handler for gRPC service."""

import time
from typing import List
import numpy as np

from app.models.embedder import get_embedder
from .base import BaseHandler

# Import generated protobuf classes
import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent.parent.parent))

from protos.generated import question_service_pb2


class EmbeddingHandler(BaseHandler):
    """Handles embedding requests."""

    def __init__(self):
        super().__init__()
        self.embedder = get_embedder()

    def get_embedding(self, request, context):
        """Generate embeddings for texts."""
        start_time = time.time()

        try:
            texts = list(request.texts)
            self.log_request(
                "GetEmbedding",
                num_texts=len(texts),
                model_type=request.model_type or "default"
            )

            # Generate embeddings for all texts
            embeddings = []
            dimension = 0

            if texts:
                # Use batch processing
                embedding_arrays = self.embedder.batch_embed(texts)

                for embedding_array in embedding_arrays:
                    # Convert numpy array to list of floats
                    values = embedding_array.tolist() if isinstance(embedding_array, np.ndarray) else list(embedding_array)
                    embeddings.append(question_service_pb2.EmbeddingVector(values=values))
                    dimension = len(values)

            processing_time = int((time.time() - start_time) * 1000)

            self.log_performance(
                "GetEmbedding",
                start_time,
                num_texts=len(texts),
                dimension=dimension
            )

            return question_service_pb2.EmbeddingResponse(
                embeddings=embeddings,
                dimension=dimension,
                processing_time=processing_time
            )

        except Exception as e:
            self.handle_error(context, "GetEmbedding", e)
            return question_service_pb2.EmbeddingResponse()