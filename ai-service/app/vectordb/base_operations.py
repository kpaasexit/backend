"""Base vector operations implementation."""

import uuid
from typing import Dict, List, Optional, Any, Union
import numpy as np
from datetime import datetime

from qdrant_client.models import (
    PointStruct,
    Filter,
    FieldCondition,
    MatchValue,
    Range,
    SearchRequest,
    UpdateStatus,
    Record
)

from app.config import get_settings
from app.core.logger import LoggerSetup
from app.core.exceptions import VectorDBError
from app.vectordb.client import get_qdrant_client
from .interfaces import VectorOperationsInterface


logger = LoggerSetup.get_logger(__name__)


class BaseVectorOperations(VectorOperationsInterface):
    """Base implementation for vector database operations."""

    def __init__(self):
        self.settings = get_settings()
        self.client = get_qdrant_client()

    def _validate_vector(self, vector: Union[List[float], np.ndarray]) -> List[float]:
        """Validate and convert vector to list format."""
        if isinstance(vector, np.ndarray):
            vector = vector.tolist()

        if not isinstance(vector, list) or not all(isinstance(x, (int, float)) for x in vector):
            raise VectorDBError("Vector must be a list of numbers or numpy array")

        return [float(x) for x in vector]

    def _create_point(
        self,
        point_id: str,
        vector: List[float],
        payload: Dict[str, Any]
    ) -> PointStruct:
        """Create a Qdrant point structure."""
        return PointStruct(
            id=point_id,
            vector=vector,
            payload=payload
        )

    def _build_search_filter(
        self,
        category_filter: Optional[str] = None,
        language_filter: Optional[str] = None,
        tags_filter: Optional[List[str]] = None
    ) -> Optional[Filter]:
        """Build search filter based on criteria."""
        conditions = []

        if category_filter:
            conditions.append(
                FieldCondition(
                    key="category",
                    match=MatchValue(value=category_filter)
                )
            )

        if language_filter:
            conditions.append(
                FieldCondition(
                    key="language",
                    match=MatchValue(value=language_filter)
                )
            )

        if tags_filter:
            for tag in tags_filter:
                conditions.append(
                    FieldCondition(
                        key="tags",
                        match=MatchValue(value=tag)
                    )
                )

        return Filter(must=conditions) if conditions else None

    def _format_search_result(self, point: Record) -> Dict[str, Any]:
        """Format search result into standard format."""
        return {
            "question_id": str(point.id),
            "score": getattr(point, 'score', 0.0),
            "title": point.payload.get("title", ""),
            "category": point.payload.get("category", ""),
            "content_sample": point.payload.get("content_sample"),
            "language_ratio": point.payload.get("language_ratio", {}),
            "embedding_type": point.payload.get("embedding_type", "mixed"),
            "tags": point.payload.get("tags", []),
            "created_at": point.payload.get("created_at"),
            "updated_at": point.payload.get("updated_at")
        }

    def _handle_qdrant_error(self, operation: str, e: Exception) -> None:
        """Handle Qdrant errors consistently."""
        error_msg = f"Qdrant {operation} failed: {str(e)}"
        logger.error(error_msg)
        raise VectorDBError(error_msg)

    def get_collection_info(self, collection_name: str) -> Dict[str, Any]:
        """Get information about a collection."""
        try:
            collection_info = self.client.get_collection(collection_name)
            return {
                "status": collection_info.status,
                "vectors_count": collection_info.vectors_count,
                "indexed_vectors_count": collection_info.indexed_vectors_count,
                "points_count": collection_info.points_count,
                "segments_count": collection_info.segments_count,
                "config": {
                    "vector_size": collection_info.config.params.vectors.size,
                    "distance": collection_info.config.params.vectors.distance.name
                }
            }
        except Exception as e:
            self._handle_qdrant_error("get_collection_info", e)