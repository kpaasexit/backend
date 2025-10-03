"""Dimension reduction for embeddings."""

import logging
import numpy as np
from typing import Optional

logger = logging.getLogger(__name__)


class DimensionReducer:
    """
    Simple dimension reducer for embeddings.
    Reduces 768-dim Korean vectors to 384-dim for consistency with English vectors.
    Uses truncation method (no sklearn dependency needed).
    """

    def __init__(self, target_dim: int = 384):
        self.target_dim = target_dim
        logger.info(f"Initialized DimensionReducer with target dim {self.target_dim}")

    def reduce_dimension(self, embedding: np.ndarray) -> np.ndarray:
        """
        Reduce dimension of embedding.

        Args:
            embedding: Input embedding (can be 384 or 768 dim)

        Returns:
            384-dim embedding
        """
        # If already target dimension, return as is
        if embedding.shape[0] == self.target_dim:
            return embedding

        # If 768-dim (Korean), truncate to target dimension
        if embedding.shape[0] == 768:
            truncated = embedding[:self.target_dim]
            return truncated / np.linalg.norm(truncated)  # Normalize

        # Handle unexpected dimensions
        if embedding.shape[0] < self.target_dim:
            # Pad if smaller
            padded = np.zeros(self.target_dim)
            padded[:embedding.shape[0]] = embedding
            norm = np.linalg.norm(padded)
            return padded / norm if norm > 0 else padded
        else:
            # Truncate if larger
            truncated = embedding[:self.target_dim]
            return truncated / np.linalg.norm(truncated)


# Global instance
_dimension_reducer: Optional[DimensionReducer] = None


def get_dimension_reducer() -> DimensionReducer:
    """Get global dimension reducer instance."""
    global _dimension_reducer
    if _dimension_reducer is None:
        _dimension_reducer = DimensionReducer()
    return _dimension_reducer
