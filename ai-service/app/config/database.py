"""Database configuration."""

from typing import Optional
from pydantic import Field
from .base import BaseConfig


class QdrantConfig(BaseConfig):
    """Qdrant vector database configuration."""

    qdrant_host: str = Field(default="localhost")
    qdrant_port: int = Field(default=6333)
    qdrant_api_key: Optional[str] = Field(default=None)
    qdrant_collection_questions: str = Field(default="questions")
    qdrant_collection_quiz: str = Field(default="quiz")
    vector_size_ko: int = Field(default=768)
    vector_size_en: int = Field(default=384)


class RedisConfig(BaseConfig):
    """Redis cache configuration."""

    redis_host: str = Field(default="localhost")
    redis_port: int = Field(default=6379)
    redis_password: Optional[str] = Field(default=None)
    redis_db: int = Field(default=0)
    cache_ttl: int = Field(default=3600)