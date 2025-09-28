"""Vector database interfaces and protocols."""

from abc import ABC, abstractmethod
from typing import Dict, List, Optional, Any, Union
import numpy as np

from qdrant_client.models import Record


class VectorOperationsInterface(ABC):
    """Interface for vector database operations."""

    @abstractmethod
    def insert_question(
        self,
        vector: Union[List[float], np.ndarray],
        question_id: str,
        title: str,
        category: str,
        content_sample: Optional[str] = None,
        language_ratio: Optional[Dict[str, float]] = None,
        embedding_type: str = "mixed",
        tags: Optional[List[str]] = None,
        **additional_payload
    ) -> bool:
        """Insert a single question into the vector database."""
        pass

    @abstractmethod
    def batch_insert_questions(
        self,
        vectors: List[Union[List[float], np.ndarray]],
        questions: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """Insert multiple questions into the vector database."""
        pass

    @abstractmethod
    def search_similar_questions(
        self,
        query_vector: Union[List[float], np.ndarray],
        limit: int = 10,
        score_threshold: float = 0.7,
        category_filter: Optional[str] = None,
        language_filter: Optional[str] = None,
        tags_filter: Optional[List[str]] = None
    ) -> List[Dict[str, Any]]:
        """Search for similar questions in the vector database."""
        pass

    @abstractmethod
    def delete_question(self, question_id: str) -> bool:
        """Delete a question from the vector database."""
        pass

    @abstractmethod
    def get_question(self, question_id: str) -> Optional[Dict[str, Any]]:
        """Get a specific question by ID."""
        pass

    @abstractmethod
    def update_question(
        self,
        question_id: str,
        vector: Optional[Union[List[float], np.ndarray]] = None,
        payload: Optional[Dict[str, Any]] = None
    ) -> bool:
        """Update a question in the vector database."""
        pass