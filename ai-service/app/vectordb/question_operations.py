"""Question-specific vector operations."""

import uuid
from typing import Dict, List, Optional, Any, Union
import numpy as np
from datetime import datetime

from qdrant_client.models import PointStruct

from app.core.logger import LoggerSetup
from app.core.exceptions import VectorDBError
from .base_operations import BaseVectorOperations


logger = LoggerSetup.get_logger(__name__)


class QuestionVectorOperations(BaseVectorOperations):
    """Vector operations specifically for questions collection."""

    def __init__(self):
        super().__init__()
        self.collection_name = self.settings.qdrant.qdrant_collection_questions

    def insert_question(
        self,
        vector: Union[List[float], np.ndarray],
        question_id: int,
        title: str,
        category_id: int,
        content_sample: Optional[str] = None,
        language_ratio: Optional[Dict[str, float]] = None,
        embedding_type: str = "mixed",
        **additional_payload
    ) -> bool:
        """Insert a single question into the vector database."""
        try:
            # Validate vector
            vector = self._validate_vector(vector)

            # Create payload
            payload = {
                "question_id": question_id,
                "title": title,
                "category_id": category_id,
                "embedding_type": embedding_type,
                "created_at": datetime.utcnow().isoformat(),
                "updated_at": datetime.utcnow().isoformat(),
                **additional_payload
            }

            if content_sample:
                payload["content_sample"] = content_sample

            if language_ratio:
                payload["language_ratio"] = language_ratio

            # Create point
            # Use UUID for Qdrant ID to avoid format issues with small numeric IDs
            point_id = str(uuid.uuid4())
            point = self._create_point(point_id, vector, payload)

            # Insert into Qdrant
            operation_info = self.client.upsert(
                collection_name=self.collection_name,
                points=[point]
            )

            success = operation_info.status.name == "COMPLETED"
            if success:
                logger.info(f"Question inserted: {question_id} (category_id: {category_id})")
            else:
                logger.error(f"Question insertion failed: {question_id}")

            return success

        except Exception as e:
            self._handle_qdrant_error("insert_question", e)

    def batch_insert_questions(
        self,
        vectors: List[Union[List[float], np.ndarray]],
        questions: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """Insert multiple questions into the vector database."""
        if len(vectors) != len(questions):
            raise VectorDBError("Number of vectors and questions must match")

        try:
            points = []
            processed_ids = []

            for i, (vector, question_data) in enumerate(zip(vectors, questions)):
                # Validate vector
                vector = self._validate_vector(vector)

                # Generate ID if not provided
                import random
                question_id = question_data.get("question_id", random.randint(1000000, 9999999))
                processed_ids.append(question_id)

                # Create payload
                payload = {
                    "question_id": question_id,
                    "title": question_data.get("title", ""),
                    "category_id": question_data.get("category_id", 1),
                    "embedding_type": question_data.get("embedding_type", "mixed"),
                    "created_at": datetime.utcnow().isoformat(),
                    "updated_at": datetime.utcnow().isoformat(),
                }

                # Add optional fields
                optional_fields = ["content_sample", "language_ratio"]
                for field in optional_fields:
                    if field in question_data:
                        payload[field] = question_data[field]

                # Add any additional payload data
                additional_data = {k: v for k, v in question_data.items()
                                if k not in ["question_id", "vector"] + optional_fields + list(payload.keys())}
                payload.update(additional_data)

                # Create point
                # Use UUID for Qdrant ID to avoid format issues with small numeric IDs
                point_id = str(uuid.uuid4())
                point = self._create_point(point_id, vector, payload)
                points.append(point)

            # Batch insert into Qdrant
            operation_info = self.client.upsert(
                collection_name=self.collection_name,
                points=points
            )

            success = operation_info.status.name == "COMPLETED"
            result = {
                "success": success,
                "processed_count": len(processed_ids),
                "question_ids": processed_ids,
                "operation_id": getattr(operation_info, 'operation_id', None)
            }

            if success:
                logger.info(f"Batch inserted {len(processed_ids)} questions")
            else:
                logger.error(f"Batch insertion failed for {len(processed_ids)} questions")

            return result

        except Exception as e:
            self._handle_qdrant_error("batch_insert_questions", e)

    def search_similar_questions(
        self,
        query_vector: Union[List[float], np.ndarray],
        limit: int = 10,
        score_threshold: float = 0.7,
        category_filter: Optional[int] = None,
        language_filter: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """Search for similar questions in the vector database."""
        try:
            # Validate vector
            query_vector = self._validate_vector(query_vector)

            # Build search filter
            search_filter = self._build_search_filter(
                category_filter=category_filter,
                language_filter=language_filter
            )

            # Perform search
            search_result = self.client.search(
                collection_name=self.collection_name,
                query_vector=query_vector,
                query_filter=search_filter,
                limit=limit,
                score_threshold=score_threshold,
                with_payload=True,
                with_vectors=False
            )

            # Format results
            results = [self._format_search_result(point) for point in search_result]

            logger.debug(f"Found {len(results)} similar questions (threshold: {score_threshold})")
            return results

        except Exception as e:
            self._handle_qdrant_error("search_similar_questions", e)

    def delete_question(self, question_id: int) -> bool:
        """Delete a question from the vector database by question_id in payload."""
        try:
            from qdrant_client.models import Filter, FieldCondition, MatchValue

            # Delete by payload filter instead of ID
            operation_info = self.client.delete(
                collection_name=self.collection_name,
                points_selector=Filter(
                    must=[
                        FieldCondition(
                            key="question_id",
                            match=MatchValue(value=question_id)
                        )
                    ]
                )
            )

            success = operation_info.status.name == "COMPLETED"
            if success:
                logger.info(f"Question deleted: {question_id}")
            else:
                logger.error(f"Question deletion failed: {question_id}")

            return success

        except Exception as e:
            self._handle_qdrant_error("delete_question", e)

    def get_question(self, question_id: int) -> Optional[Dict[str, Any]]:
        """Get a specific question by question_id from payload."""
        try:
            from qdrant_client.models import Filter, FieldCondition, MatchValue

            # Search by payload filter instead of ID
            result = self.client.search(
                collection_name=self.collection_name,
                query_filter=Filter(
                    must=[
                        FieldCondition(
                            key="question_id",
                            match=MatchValue(value=question_id)
                        )
                    ]
                ),
                query_vector=[0.0] * 384,  # Dummy vector for filter-only search
                limit=1,
                with_payload=True,
                with_vectors=False
            )

            if result:
                return self._format_search_result(result[0])

            return None

        except Exception as e:
            self._handle_qdrant_error("get_question", e)

    def update_question(
        self,
        question_id: int,
        vector: Optional[Union[List[float], np.ndarray]] = None,
        payload: Optional[Dict[str, Any]] = None
    ) -> bool:
        """Update a question in the vector database by question_id."""
        try:
            from qdrant_client.models import Filter, FieldCondition, MatchValue

            # First, find the actual point ID by question_id
            search_result = self.client.search(
                collection_name=self.collection_name,
                query_filter=Filter(
                    must=[
                        FieldCondition(
                            key="question_id",
                            match=MatchValue(value=question_id)
                        )
                    ]
                ),
                query_vector=[0.0] * 384,  # Dummy vector for filter-only search
                limit=1,
                with_payload=True,
                with_vectors=False
            )

            if not search_result:
                raise VectorDBError(f"Question with ID {question_id} not found")

            point_id = search_result[0].id
            update_data = {}

            if vector is not None:
                vector = self._validate_vector(vector)
                update_data["vector"] = vector

            if payload is not None:
                # Add updated timestamp
                payload = payload.copy()
                payload["updated_at"] = datetime.utcnow().isoformat()
                # Also ensure question_id is preserved
                payload["question_id"] = question_id
                update_data["payload"] = payload

            if not update_data:
                raise VectorDBError("Either vector or payload must be provided for update")

            # Create updated point with the found point ID
            point = PointStruct(
                id=point_id,
                **update_data
            )

            operation_info = self.client.upsert(
                collection_name=self.collection_name,
                points=[point]
            )

            success = operation_info.status.name == "COMPLETED"
            if success:
                logger.info(f"Question updated: {question_id}")
            else:
                logger.error(f"Question update failed: {question_id}")

            return success

        except Exception as e:
            self._handle_qdrant_error("update_question", e)