"""Collection definitions for Qdrant vector database."""

from dataclasses import dataclass, field
from typing import Dict, List, Optional, Any
from enum import Enum

from qdrant_client.models import Distance, VectorParams, PayloadSchemaType

from app.config import get_settings
from app.core.logger import LoggerSetup
from app.vectordb.client import get_qdrant_client


logger = LoggerSetup.get_logger(__name__)


class CollectionName(Enum):
    """Collection names enum."""
    QUESTIONS = "questions"
    QUIZ = "quiz"


@dataclass
class CollectionSchema:
    """Collection schema definition."""

    name: str
    vector_size: int
    distance: Distance = Distance.COSINE
    payload_schema: Dict[str, PayloadSchemaType] = field(default_factory=dict)
    hnsw_config: Dict[str, Any] = field(default_factory=dict)
    optimizers_config: Dict[str, Any] = field(default_factory=dict)
    on_disk_payload: bool = True


class Collections:
    """Collection definitions and management."""

    @staticmethod
    def get_questions_schema() -> CollectionSchema:
        """
        Get schema for questions collection.

        Returns:
            CollectionSchema for questions
        """
        settings = get_settings()

        return CollectionSchema(
            name=settings.qdrant.qdrant_collection_questions,
            vector_size=384,  # Default unified dimension
            distance=Distance.COSINE,
            payload_schema={
                "question_id": PayloadSchemaType.KEYWORD,
                "category": PayloadSchemaType.KEYWORD,
                "title": PayloadSchemaType.TEXT,
                "created_at": PayloadSchemaType.INTEGER,  # Unix timestamp
                "language_ratio": PayloadSchemaType.TEXT,  # JSON string
                "embedding_type": PayloadSchemaType.KEYWORD,  # "ko", "en", "mixed"
                "tags": PayloadSchemaType.KEYWORD,  # Array of tags
                "view_count": PayloadSchemaType.INTEGER,
                "answer_count": PayloadSchemaType.INTEGER
            },
            hnsw_config={
                "m": 16,  # Number of connections
                "ef_construct": 100,  # Search breadth during indexing
                "full_scan_threshold": 10000  # Use HNSW for collections > 10k
            },
            optimizers_config={
                "deleted_threshold": 0.2,
                "vacuum_min_vector_number": 1000,
                "default_segment_number": 2,
                "max_segment_size": 200000,
                "memmap_threshold": 100000,
                "indexing_threshold": 20000,
                "flush_interval_sec": 5,
                "max_optimization_threads": 2
            },
            on_disk_payload=True
        )

    @staticmethod
    def get_quiz_schema() -> CollectionSchema:
        """
        Get schema for quiz collection.

        Returns:
            CollectionSchema for quiz
        """
        settings = get_settings()

        return CollectionSchema(
            name=settings.qdrant.qdrant_collection_quiz,
            vector_size=384,
            distance=Distance.COSINE,
            payload_schema={
                "quiz_id": PayloadSchemaType.INTEGER,
                "quiz_category_id": PayloadSchemaType.INTEGER,
                "quiz_title": PayloadSchemaType.TEXT,
                "quiz_content": PayloadSchemaType.TEXT,
                "quiz_type": PayloadSchemaType.KEYWORD,  # "OX", "MULTIPLE"
                "quiz_correct_answer": PayloadSchemaType.KEYWORD,  # "O", "X", "1", "2", "3", "4"
                "explanation": PayloadSchemaType.TEXT,
                "created_at": PayloadSchemaType.INTEGER,  # Unix timestamp
                "updated_at": PayloadSchemaType.INTEGER,  # Unix timestamp
                "category_id": PayloadSchemaType.INTEGER,  # For backward compatibility
            },
            hnsw_config={
                "m": 16,
                "ef_construct": 100,
                "full_scan_threshold": 5000
            },
            optimizers_config={
                "deleted_threshold": 0.2,
                "vacuum_min_vector_number": 500,
                "default_segment_number": 1,
                "max_segment_size": 100000,
                "memmap_threshold": 50000,
                "indexing_threshold": 10000,
                "flush_interval_sec": 5,
                "max_optimization_threads": 1
            },
            on_disk_payload=True
        )

    @staticmethod
    def create_all_collections(force: bool = False) -> Dict[str, bool]:
        """
        Create all defined collections.

        Args:
            force: If True, recreate existing collections

        Returns:
            Dictionary with creation status for each collection
        """
        client = get_qdrant_client()
        results = {}

        # Define all schemas
        schemas = [
            Collections.get_questions_schema(),
            Collections.get_quiz_schema()
        ]

        for schema in schemas:
            try:
                # Check if collection exists
                exists = client.collection_exists(schema.name)

                if exists and not force:
                    logger.info(f"Collection {schema.name} already exists, skipping")
                    results[schema.name] = True
                    continue

                if exists and force:
                    # Delete existing collection
                    logger.info(f"Deleting existing collection {schema.name}")
                    client.delete_collection(schema.name)

                # Create collection
                success = client.create_collection(
                    collection_name=schema.name,
                    vector_size=schema.vector_size,
                    distance=schema.distance,
                    on_disk_payload=schema.on_disk_payload,
                    hnsw_config=schema.hnsw_config,
                    optimizers_config=schema.optimizers_config
                )

                results[schema.name] = success
                logger.info(f"Collection {schema.name} created: {success}")

            except Exception as e:
                logger.error(f"Failed to create collection {schema.name}: {e}")
                results[schema.name] = False

        return results

    @staticmethod
    def validate_collections() -> Dict[str, Dict[str, Any]]:
        """
        Validate all collections exist and have correct configuration.

        Returns:
            Dictionary with validation results
        """
        client = get_qdrant_client()
        results = {}

        schemas = {
            Collections.get_questions_schema().name: Collections.get_questions_schema(),
            Collections.get_quiz_schema().name: Collections.get_quiz_schema()
        }

        for collection_name, expected_schema in schemas.items():
            try:
                # Check existence
                exists = client.collection_exists(collection_name)

                if not exists:
                    results[collection_name] = {
                        "valid": False,
                        "exists": False,
                        "error": "Collection does not exist"
                    }
                    continue

                # Get collection info
                info = client.get_collection_info(collection_name)

                # Validate configuration
                validation = {
                    "valid": True,
                    "exists": True,
                    "vector_size_match": info["vector_size"] == expected_schema.vector_size,
                    "distance_match": info["distance"] == expected_schema.distance.value,
                    "points_count": info["points_count"],
                    "status": info["status"]
                }

                # Check if configuration matches
                if not validation["vector_size_match"] or not validation["distance_match"]:
                    validation["valid"] = False
                    validation["error"] = "Configuration mismatch"

                results[collection_name] = validation

            except Exception as e:
                results[collection_name] = {
                    "valid": False,
                    "exists": False,
                    "error": str(e)
                }

        return results

    @staticmethod
    def optimize_all_collections() -> Dict[str, bool]:
        """
        Optimize all collections for better performance.

        Returns:
            Dictionary with optimization status
        """
        client = get_qdrant_client()
        results = {}

        collection_names = [
            get_settings().qdrant.qdrant_collection_questions,
            get_settings().qdrant.qdrant_collection_quiz
        ]

        for collection_name in collection_names:
            try:
                if not client.collection_exists(collection_name):
                    results[collection_name] = False
                    logger.warning(f"Collection {collection_name} does not exist")
                    continue

                success = client.optimize_collection(collection_name, wait=True)
                results[collection_name] = success
                logger.info(f"Optimized collection {collection_name}: {success}")

            except Exception as e:
                logger.error(f"Failed to optimize collection {collection_name}: {e}")
                results[collection_name] = False

        return results

    @staticmethod
    def get_collection_stats() -> Dict[str, Dict[str, Any]]:
        """
        Get statistics for all collections.

        Returns:
            Dictionary with collection statistics
        """
        client = get_qdrant_client()
        stats = {}

        collection_names = [
            get_settings().qdrant.qdrant_collection_questions,
            get_settings().qdrant.qdrant_collection_quiz
        ]

        for collection_name in collection_names:
            try:
                if not client.collection_exists(collection_name):
                    stats[collection_name] = {"exists": False}
                    continue

                info = client.get_collection_info(collection_name)
                stats[collection_name] = {
                    "exists": True,
                    **info
                }

            except Exception as e:
                logger.error(f"Failed to get stats for collection {collection_name}: {e}")
                stats[collection_name] = {"exists": False, "error": str(e)}

        return stats