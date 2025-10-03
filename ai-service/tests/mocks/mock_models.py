"""Mock ML model implementations for testing."""

import numpy as np
import torch
from typing import List, Dict, Any, Tuple, Optional, Union
from unittest.mock import MagicMock

from app.core.constants import Language, ModelType, HOME_LIFE_CATEGORIES
from app.core.exceptions import EmbeddingError, ClassificationError, ModelLoadError


class MockEmbedder:
    """Mock text embedder for testing."""

    def __init__(self, dimension: int = 384):
        self.dimension = dimension
        self.cache = {}
        self.target_dim = dimension
        self._initialized = True

    def embed(
        self,
        text: str,
        use_cache: bool = True,
        normalize: bool = True
    ) -> np.ndarray:
        """Generate mock embedding for text."""
        if not text or not text.strip():
            raise EmbeddingError("Empty or invalid text")

        # Generate deterministic embedding based on text hash
        text_hash = hash(text) % (2**31)  # Ensure positive hash
        np.random.seed(text_hash)

        embedding = np.random.rand(self.dimension).astype(np.float32)

        if normalize:
            norm = np.linalg.norm(embedding)
            if norm > 0:
                embedding = embedding / norm

        return embedding

    def embed_multilayer(
        self,
        title: str,
        content: str,
        keywords: Optional[List[str]] = None
    ) -> np.ndarray:
        """Generate mock multilayer embedding."""
        # Combine all inputs for deterministic result
        combined_text = f"{title} {content or ''}"
        if keywords:
            combined_text += " " + " ".join(keywords[:5])

        return self.embed(combined_text, normalize=True)

    def batch_embed(
        self,
        texts: List[str],
        use_cache: bool = True,
        normalize: bool = True
    ) -> List[np.ndarray]:
        """Generate batch embeddings."""
        return [self.embed(text, use_cache, normalize) for text in texts]

    def compute_similarity(
        self,
        embedding1: np.ndarray,
        embedding2: np.ndarray
    ) -> float:
        """Compute cosine similarity between embeddings."""
        # Normalize embeddings
        norm1 = np.linalg.norm(embedding1)
        norm2 = np.linalg.norm(embedding2)

        if norm1 == 0 or norm2 == 0:
            return 0.0

        emb1_norm = embedding1 / norm1
        emb2_norm = embedding2 / norm2

        # Compute dot product (cosine similarity for normalized vectors)
        similarity = np.dot(emb1_norm, emb2_norm)

        return float(np.clip(similarity, 0, 1))

    def clear_cache(self) -> None:
        """Clear embedding cache."""
        self.cache.clear()

    def preload_models(self) -> None:
        """Mock preloading models."""
        pass


class MockClassifier:
    """Mock text classifier for testing."""

    def __init__(self):
        self._initialized = True
        self.categories = [
            "요리/식품관리", "청소/세탁", "생활수리/DIY", "생활경제/계약",
            "육아/반려동물", "이사/인테리어", "환경/건강", "스마트홈/가전"
        ]

    def initialize(self) -> None:
        """Mock initialization."""
        self._initialized = True

    def classify(self, text: str) -> Tuple[str, float]:
        """Mock classification of text."""
        if not text or not text.strip():
            raise ClassificationError(text=text, reason="Empty or invalid text")

        # Generate deterministic classification based on text content
        text_lower = text.lower().strip()

        # Simple keyword-based classification for realistic testing
        category_keywords = {
            "요리/식품관리": ["김치", "요리", "음식", "식품", "레시피", "조리", "식재료", "계란", "고기"],
            "청소/세탁": ["청소", "세탁", "빨래", "얼룩", "곰팡이", "화장실", "욕실"],
            "생활수리/DIY": ["수리", "diy", "형광등", "교체", "고장", "못", "나사", "수도꼭지"],
            "생활경제/계약": ["전세", "계약", "대출", "이자", "신용카드", "연회비", "금융", "경제"],
            "육아/반려동물": ["강아지", "고양이", "반려동물", "아기", "이유식", "육아", "애완동물"],
            "이사/인테리어": ["이사", "인테리어", "원룸", "꾸미기", "가구", "공간"],
            "환경/건강": ["미세먼지", "환경", "건강", "공기", "정화", "운동", "수면"],
            "스마트홈/가전": ["에어컨", "가전", "스마트홈", "로봇청소기", "필터", "tv", "냉장고"]
        }

        # Find best matching category
        best_category = "요리/식품관리"  # default
        best_score = 0.5

        for category, keywords in category_keywords.items():
            score = 0
            for keyword in keywords:
                if keyword in text_lower:
                    score += 1

            if score > 0:
                # Calculate confidence based on keyword matches
                confidence = min(0.95, 0.6 + (score * 0.1))
                if confidence > best_score:
                    best_category = category
                    best_score = confidence

        # Add some randomness based on text hash for variety
        text_hash = hash(text) % 100
        if text_hash < 10:  # 10% chance to slightly vary confidence
            best_score = max(0.5, best_score - 0.1)

        return best_category, best_score

    def batch_classify(self, texts: List[str]) -> List[Tuple[str, float]]:
        """Mock batch classification."""
        return [self.classify(text) for text in texts]

    def get_category_id(self, category_name: str) -> Optional[int]:
        """Get category ID by name."""
        try:
            return self.categories.index(category_name)
        except ValueError:
            return None


class MockModelManager:
    """Mock model manager for testing."""

    def __init__(self):
        self.loaded_models = {}
        self._embedder = MockEmbedder()
        self._classifier = MockClassifier()

    def load_model(
        self,
        model_name: str,
        model_type: ModelType,
        force_reload: bool = False
    ) -> Dict[str, Any]:
        """Mock model loading."""
        model_key = f"{model_name}:{model_type.name}"

        if model_key in self.loaded_models and not force_reload:
            return self.loaded_models[model_key]

        if model_type == ModelType.EMBEDDER:
            model_data = {
                "model": self._create_mock_embedder_model(),
                "tokenizer": None,
                "config": {"max_length": 512, "hidden_size": 384}
            }
        elif model_type == ModelType.CLASSIFIER:
            model_data = self._create_mock_classifier_model()
        else:
            model_data = {"model": MagicMock()}

        self.loaded_models[model_key] = model_data
        return model_data

    def _create_mock_embedder_model(self):
        """Create mock embedding model."""
        model = MagicMock()

        def mock_encode(texts, **kwargs):
            if isinstance(texts, str):
                return self._embedder.embed(texts, normalize=False)
            else:
                return np.array([
                    self._embedder.embed(text, normalize=False)
                    for text in texts
                ])

        model.encode = mock_encode
        model.device = "cpu"
        model.max_seq_length = 512
        return model

    def _create_mock_classifier_model(self):
        """Create mock classifier model."""
        model = MagicMock()
        tokenizer = MagicMock()

        # Mock tokenizer
        def mock_tokenize(text, **kwargs):
            return {
                'input_ids': torch.tensor([[1, 2, 3, 4, 5]]),
                'attention_mask': torch.tensor([[1, 1, 1, 1, 1]])
            }

        tokenizer.side_effect = mock_tokenize

        # Mock model forward pass
        def mock_forward(**inputs):
            # Generate realistic logits based on input
            # Use deterministic generation for consistent testing
            text_repr = str(inputs.get('input_ids', []))
            text_hash = hash(text_repr) % 8

            logits = torch.zeros(1, 8)
            logits[0, text_hash] = 2.0  # High confidence for selected category
            logits[0, :] += torch.randn(8) * 0.2  # Add some noise

            mock_outputs = MagicMock()
            mock_outputs.logits = logits
            return mock_outputs

        model.side_effect = mock_forward
        model.eval.return_value = model
        model.to.return_value = model

        return {"model": model, "tokenizer": tokenizer}

    def unload_model(self, model_name: str, model_type: ModelType) -> bool:
        """Mock model unloading."""
        model_key = f"{model_name}:{model_type.name}"
        if model_key in self.loaded_models:
            del self.loaded_models[model_key]
            return True
        return False

    def is_loaded(self, model_name: str, model_type: ModelType) -> bool:
        """Check if model is loaded."""
        model_key = f"{model_name}:{model_type.name}"
        return model_key in self.loaded_models

    def get_memory_usage(self) -> Dict[str, Any]:
        """Get mock memory usage information."""
        return {
            "total": 512,  # MB
            "models": {
                name: {"size_mb": 128, "type": name.split(":")[1]}
                for name in self.loaded_models.keys()
            }
        }

    def cleanup(self) -> None:
        """Clean up all loaded models."""
        self.loaded_models.clear()


class MockDimensionReducer:
    """Mock dimension reducer for testing."""

    def __init__(self, target_dimension: int = 384):
        self.target_dim = target_dimension
        self._fitted = True

    def reduce_dimension(
        self,
        embedding: np.ndarray,
        target_dim: Optional[int] = None
    ) -> np.ndarray:
        """Mock dimension reduction."""
        if target_dim is None:
            target_dim = self.target_dim

        if len(embedding.shape) == 1:
            # Single embedding
            if embedding.shape[0] == target_dim:
                return embedding
            elif embedding.shape[0] > target_dim:
                # Truncate
                return embedding[:target_dim]
            else:
                # Pad with zeros
                padded = np.zeros(target_dim, dtype=embedding.dtype)
                padded[:len(embedding)] = embedding
                return padded
        else:
            # Batch embeddings
            batch_size = embedding.shape[0]
            current_dim = embedding.shape[1]

            if current_dim == target_dim:
                return embedding
            elif current_dim > target_dim:
                # Truncate
                return embedding[:, :target_dim]
            else:
                # Pad with zeros
                padded = np.zeros((batch_size, target_dim), dtype=embedding.dtype)
                padded[:, :current_dim] = embedding
                return padded

    def fit(self, embeddings: np.ndarray) -> None:
        """Mock fitting (no-op)."""
        self._fitted = True

    def is_fitted(self) -> bool:
        """Check if reducer is fitted."""
        return self._fitted