"""PCA-based dimension reduction for embeddings."""

import pickle
import os
from typing import Optional
import numpy as np
from sklearn.decomposition import PCA
import logging

logger = logging.getLogger(__name__)


class DimensionReducer:
    """
    PCA-based dimension reducer for Korean embeddings.
    Reduces 768-dim Korean vectors to 384-dim for consistency with English vectors.
    """

    def __init__(self, target_dim: int = 384, cache_path: str = "/tmp/pca_model.pkl"):
        self.target_dim = target_dim
        self.cache_path = cache_path
        self.pca_model: Optional[PCA] = None
        self._load_or_create_pca()

    def _load_or_create_pca(self):
        """Load existing PCA model or create new one."""
        if os.path.exists(self.cache_path):
            try:
                with open(self.cache_path, 'rb') as f:
                    self.pca_model = pickle.load(f)
                logger.info(f"Loaded PCA model from {self.cache_path}")
            except Exception as e:
                logger.warning(f"Failed to load PCA model: {e}")
                self._create_new_pca()
        else:
            self._create_new_pca()

    def _create_new_pca(self):
        """Create new PCA model."""
        self.pca_model = PCA(n_components=self.target_dim)
        logger.info(f"Created new PCA model with target dim {self.target_dim}")

    def fit(self, embeddings: np.ndarray):
        """
        Fit PCA model on Korean embeddings.

        Args:
            embeddings: Array of 768-dim Korean embeddings
        """
        if embeddings.shape[1] != 768:
            raise ValueError(f"Expected 768-dim embeddings, got {embeddings.shape[1]}")

        self.pca_model.fit(embeddings)
        self._save_pca()

        # Log explained variance
        variance_ratio = self.pca_model.explained_variance_ratio_.sum()
        logger.info(f"PCA fitted. Explained variance: {variance_ratio:.2%}")

    def _save_pca(self):
        """Save PCA model to cache."""
        try:
            with open(self.cache_path, 'wb') as f:
                pickle.dump(self.pca_model, f)
            logger.info(f"Saved PCA model to {self.cache_path}")
        except Exception as e:
            logger.warning(f"Failed to save PCA model: {e}")

    def reduce_dimension(self, embedding: np.ndarray) -> np.ndarray:
        """
        Reduce dimension of embedding.

        Args:
            embedding: Input embedding (can be 384 or 768 dim)

        Returns:
            384-dim embedding
        """
        # If already 384-dim (English), return as is
        if embedding.shape[0] == self.target_dim:
            return embedding

        # If 768-dim (Korean), reduce with PCA or truncation
        if embedding.shape[0] == 768:
            if self.pca_model is not None and hasattr(self.pca_model, 'components_'):
                try:
                    # Use PCA if fitted
                    reduced = self.pca_model.transform(embedding.reshape(1, -1))[0]
                    return reduced / np.linalg.norm(reduced)  # Normalize
                except:
                    pass

            # Fallback to truncation if PCA not available
            truncated = embedding[:self.target_dim]
            return truncated / np.linalg.norm(truncated)  # Normalize

        # Handle unexpected dimensions
        if embedding.shape[0] < self.target_dim:
            # Pad if smaller
            padded = np.zeros(self.target_dim)
            padded[:embedding.shape[0]] = embedding
            return padded / np.linalg.norm(padded)
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