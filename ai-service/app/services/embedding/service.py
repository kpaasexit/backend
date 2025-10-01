"""Embedding service for text vectorization."""

import asyncio
from typing import Optional, List
import numpy as np

from app.models.embedder import TextEmbedder
from app.core.logger import LoggerSetup


logger = LoggerSetup.get_logger(__name__)


class EmbeddingService:
    """Service for generating text embeddings."""

    def __init__(self):
        """Initialize the embedding service."""
        self.embedder = TextEmbedder()
        logger.info("EmbeddingService initialized")

    async def get_embedding(self, text: str) -> Optional[np.ndarray]:
        """
        Generate embedding for a single text.

        Args:
            text: Text to embed

        Returns:
            Embedding vector or None if failed
        """
        try:
            # Run CPU-bound operation in thread pool
            loop = asyncio.get_event_loop()
            embedding = await loop.run_in_executor(
                None,
                self.embedder.embed,
                text
            )
            return embedding
        except Exception as e:
            logger.error(f"Failed to generate embedding: {e}")
            return None

    async def get_embeddings(self, texts: List[str]) -> List[Optional[np.ndarray]]:
        """
        Generate embeddings for multiple texts.

        Args:
            texts: List of texts to embed

        Returns:
            List of embedding vectors
        """
        try:
            # Process in batch for efficiency
            loop = asyncio.get_event_loop()
            # Process texts one by one since TextEmbedder doesn't have batch method
            embeddings = []
            for text in texts:
                embedding = await loop.run_in_executor(
                    None,
                    self.embedder.embed,
                    text
                )
                embeddings.append(embedding)
            return embeddings
        except Exception as e:
            logger.error(f"Failed to generate embeddings: {e}")
            return [None] * len(texts)

    def get_model_info(self) -> dict:
        """
        Get information about the embedding model.

        Returns:
            Model information dictionary
        """
        return {
            "dimension": self.embedder.target_dim,
            "models": {
                "ko": self.embedder.settings.model.ko_embedder_model,
                "en": self.embedder.settings.model.en_embedder_model
            }
        }