import hashlib
import logging
from typing import Dict, List, Optional, Union
import numpy as np
import onnxruntime as ort
import os
import json

from app.config import get_settings
from app.core.exceptions import EmbeddingError
from app.core.constants import (
    ModelType,
    Language,
    DEFAULT_EMBEDDING_DIM
)
from app.models.manager import get_model_manager
from app.models.language import (
    get_dominant_language,
    get_language_weights,
    LanguageDetector
)
from app.models.dimension_reducer import get_dimension_reducer

logger = logging.getLogger(__name__)

class ONNXEmbedder:
    """ONNX-based embedding model wrapper."""

    def __init__(self, model_path: str, tokenizer):
        self.model_path = model_path
        self.tokenizer = tokenizer
        self.session = None
        self.config = None
        self._initialize()

    def _initialize(self):
        """Initialize ONNX session and load config."""
        onnx_file = os.path.join(self.model_path, "model.onnx")
        config_file = os.path.join(self.model_path, "config.json")

        if not os.path.exists(onnx_file):
            raise ValueError(f"ONNX model not found: {onnx_file}")

        # Load ONNX model
        self.session = ort.InferenceSession(
            onnx_file,
            providers=['CPUExecutionProvider']
        )

        # Load config
        if os.path.exists(config_file):
            with open(config_file, 'r') as f:
                self.config = json.load(f)

        logger.info(f"Loaded ONNX embedder from {self.model_path}")

    def encode(
        self,
        sentences: Union[str, List[str]],
        batch_size: int = 32,
        convert_to_numpy: bool = True,
        normalize_embeddings: bool = False
    ) -> np.ndarray:
        """
        Encode sentences to embeddings using ONNX model.

        Args:
            sentences: Single sentence or list of sentences
            batch_size: Batch size for processing
            convert_to_numpy: Always returns numpy (for compatibility)
            normalize_embeddings: Whether to normalize embeddings

        Returns:
            Embeddings as numpy array
        """
        is_single = isinstance(sentences, str)
        if is_single:
            sentences = [sentences]

        all_embeddings = []

        # Process in batches
        for i in range(0, len(sentences), batch_size):
            batch = sentences[i:i + batch_size]
            batch_embeddings = self._encode_batch(batch)
            all_embeddings.extend(batch_embeddings)

        embeddings = np.array(all_embeddings)

        if normalize_embeddings:
            embeddings = embeddings / np.linalg.norm(embeddings, axis=1, keepdims=True)

        return embeddings[0] if is_single else embeddings

    def _encode_batch(self, sentences: List[str]) -> List[np.ndarray]:
        """Encode a batch of sentences."""
        # Tokenize
        inputs = self.tokenizer(
            sentences,
            padding=True,
            truncation=True,
            max_length=self.config.get("max_seq_length", 128) if self.config else 128,
            return_tensors="np"
        )

        # Prepare ONNX inputs
        ort_inputs = {
            "input_ids": inputs["input_ids"].astype(np.int64),
            "attention_mask": inputs["attention_mask"].astype(np.int64)
        }

        # Run inference
        outputs = self.session.run(None, ort_inputs)
        last_hidden_state = outputs[0]  # (batch_size, seq_len, hidden_dim)

        # Mean pooling
        attention_mask = inputs["attention_mask"]
        attention_mask_expanded = np.expand_dims(attention_mask, -1).astype(np.float32)

        sum_embeddings = np.sum(last_hidden_state * attention_mask_expanded, axis=1)
        sum_mask = np.clip(np.sum(attention_mask_expanded, axis=1), a_min=1e-9, a_max=None)
        embeddings = sum_embeddings / sum_mask

        return list(embeddings)


class TextEmbedder:
    def __init__(self):
        self.settings = get_settings()
        self.model_manager = get_model_manager()
        self._cache = {}
        self.target_dim = DEFAULT_EMBEDDING_DIM
        self.dimension_reducer = get_dimension_reducer()
        self._onnx_models = {}  # Cache for ONNX models

    def _get_cache_key(self, text: str, prefix: str = "embed") -> str:
        return f"{prefix}:{hashlib.md5(text.encode()).hexdigest()}"

    def _normalize_embedding(self, embedding: np.ndarray) -> np.ndarray:
        norm = np.linalg.norm(embedding)
        if norm > 0:
            return embedding / norm
        return embedding

    def _resize_embedding(self, embedding: np.ndarray, target_dim: int) -> np.ndarray:
        return self.dimension_reducer.reduce_dimension(embedding)

    def _get_onnx_model(self, model_name: str) -> ONNXEmbedder:
        """Get or load ONNX model."""
        if model_name in self._onnx_models:
            return self._onnx_models[model_name]

        # Map model names to ONNX paths
        onnx_path_map = {
            "jhgan/ko-sroberta-multitask": "app/models/embeddings_onnx/ko-sroberta",
            "sentence-transformers/all-MiniLM-L6-v2": "app/models/embeddings_onnx/all-MiniLM-L6-v2"
        }

        if model_name not in onnx_path_map:
            raise ValueError(f"No ONNX model mapping for: {model_name}")

        onnx_path = onnx_path_map[model_name]

        # Load tokenizer
        tokenizer_data = self.model_manager.load_model(
            model_name=model_name,
            model_type=ModelType.TOKENIZER
        )
        tokenizer = tokenizer_data["model"]

        # Create ONNX embedder
        onnx_model = ONNXEmbedder(onnx_path, tokenizer)
        self._onnx_models[model_name] = onnx_model

        return onnx_model

    def embed(
        self,
        text: str,
        use_cache: bool = True,
        normalize: bool = True
    ) -> np.ndarray:
        if not text or not text.strip():
            raise EmbeddingError(reason="Empty or invalid text")

        cache_key = self._get_cache_key(text)
        if use_cache and cache_key in self._cache:
            return self._cache[cache_key]

        try:
            if LanguageDetector.requires_multilingual_processing(text):
                embedding = self._generate_multilingual_embedding(text)
            else:
                language, _ = get_dominant_language(text)
                embedding = self._generate_single_language_embedding(text, language)

            embedding = self._resize_embedding(embedding, self.target_dim)

            if normalize:
                embedding = self._normalize_embedding(embedding)

            if use_cache:
                self._cache[cache_key] = embedding

            return embedding

        except Exception as e:
            raise EmbeddingError(reason=str(e))

    def _generate_single_language_embedding(
        self,
        text: str,
        language: Language
    ) -> np.ndarray:
        if language == Language.KOREAN:
            model_name = self.settings.model.ko_embedder_model
        else:
            model_name = self.settings.model.en_embedder_model

        model = self._get_onnx_model(model_name)

        embedding = model.encode(
            text,
            convert_to_numpy=True,
            normalize_embeddings=False
        )

        return embedding

    def _generate_multilingual_embedding(self, text: str) -> np.ndarray:
        weights = get_language_weights(text)

        embeddings = {}
        total_weight = 0

        for lang_code, weight in weights.items():
            if weight > 0.1:
                if lang_code == "ko":
                    model_name = self.settings.model.ko_embedder_model
                    language = Language.KOREAN
                else:
                    model_name = self.settings.model.en_embedder_model
                    language = Language.ENGLISH

                embedding = self._generate_single_language_embedding(text, language)
                embeddings[lang_code] = embedding
                total_weight += weight

        if not embeddings:
            return self._generate_single_language_embedding(text, Language.KOREAN)

        weights = {k: v / total_weight for k, v in weights.items() if k in embeddings}

        # Ensure all embeddings are the same dimension before combining
        resized_embeddings = {}
        for lang_code, embedding in embeddings.items():
            resized_embeddings[lang_code] = self._resize_embedding(embedding, self.target_dim)

        combined = None
        for lang_code, embedding in resized_embeddings.items():
            weight = weights[lang_code]
            if combined is None:
                combined = embedding * weight
            else:
                combined += embedding * weight

        return combined

    def embed_multilayer(
        self,
        title: str,
        content: str,
        keywords: Optional[List[str]] = None
    ) -> np.ndarray:
        embeddings = []
        weights = []

        # Title embedding (highest weight)
        if title:
            title_emb = self.embed(title, normalize=False)
            embeddings.append(title_emb)
            weights.append(1.0)

        # Content embedding (first N characters)
        if content:
            content_sample = content[:self.settings.model.max_sequence_length]
            content_emb = self.embed(content_sample, normalize=False)
            embeddings.append(content_emb)
            weights.append(0.7)

        # Keywords embedding
        if keywords:
            keyword_text = " ".join(keywords[:5])  # Use top 5 keywords
            keyword_emb = self.embed(keyword_text, normalize=False)
            embeddings.append(keyword_emb)
            weights.append(0.3)

        if not embeddings:
            raise EmbeddingError(reason="No valid input for multilayer embedding")

        total_weight = sum(weights)
        weights = [w / total_weight for w in weights]

        # Combine embeddings
        combined = None
        for emb, weight in zip(embeddings, weights):
            if combined is None:
                combined = emb * weight
            else:
                # Resize if needed
                if combined.shape != emb.shape:
                    emb = self._resize_embedding(emb, combined.shape[0])
                combined += emb * weight

        # Final normalization
        return self._normalize_embedding(combined)

    def batch_embed(
        self,
        texts: List[str],
        use_cache: bool = True,
        normalize: bool = True
    ) -> List[np.ndarray]:
        """
        Generate embeddings for multiple texts.

        Args:
            texts: List of texts
            use_cache: Whether to use cache
            normalize: Whether to normalize embeddings

        Returns:
            List of embeddings
        """
        results = []
        uncached_indices = []
        uncached_texts = []
        cached_results = {}

        # Check cache first
        if use_cache:
            for i, text in enumerate(texts):
                cache_key = self._get_cache_key(text)
                if cache_key in self._cache:
                    cached_results[i] = self._cache[cache_key]
                else:
                    uncached_indices.append(i)
                    uncached_texts.append(text)
        else:
            uncached_texts = texts
            uncached_indices = list(range(len(texts)))

        # Process uncached texts
        if uncached_texts:
            # Group by language for efficiency
            language_groups = self._group_by_language(uncached_texts)

            batch_results = {}
            for language, group_data in language_groups.items():
                group_texts = group_data["texts"]
                group_indices = group_data["indices"]

                # Process group
                group_embeddings = self._batch_process_group(
                    texts=group_texts,
                    language=language,
                    normalize=normalize
                )

                # Map results back
                for idx, embedding in zip(group_indices, group_embeddings):
                    batch_results[uncached_indices[idx]] = embedding

                    if use_cache:
                        cache_key = self._get_cache_key(uncached_texts[idx])
                        self._cache[cache_key] = embedding

        # Combine results
        for i in range(len(texts)):
            if i in cached_results:
                results.append(cached_results[i])
            else:
                results.append(batch_results[i])

        return results

    def _group_by_language(self, texts: List[str]) -> Dict[Language, Dict]:
        """Group texts by detected language."""
        groups = {}

        for i, text in enumerate(texts):
            if LanguageDetector.requires_multilingual_processing(text):
                language = Language.MIXED
            else:
                language, _ = get_dominant_language(text)

            if language not in groups:
                groups[language] = {"texts": [], "indices": []}

            groups[language]["texts"].append(text)
            groups[language]["indices"].append(i)

        return groups

    def _batch_process_group(
        self,
        texts: List[str],
        language: Language,
        normalize: bool
    ) -> List[np.ndarray]:
        """
        Process a batch of texts with the same language.

        Args:
            texts: List of texts
            language: Language of the group
            normalize: Whether to normalize

        Returns:
            List of embeddings
        """
        if not texts:
            return []

        if language == Language.MIXED:
            # Process individually for mixed language
            return [self.embed(text, use_cache=False, normalize=normalize) for text in texts]

        # Select model
        if language == Language.KOREAN:
            model_name = self.settings.model.ko_embedder_model
        else:
            model_name = self.settings.model.en_embedder_model

        model = self._get_onnx_model(model_name)

        # Batch encode
        embeddings = model.encode(
            texts,
            batch_size=self.settings.model.batch_size,
            convert_to_numpy=True,
            normalize_embeddings=False
        )

        # Resize and normalize
        results = []
        for embedding in embeddings:
            embedding = self._resize_embedding(embedding, self.target_dim)

            if normalize:
                embedding = self._normalize_embedding(embedding)

            results.append(embedding)

        return results

    def compute_similarity(
        self,
        embedding1: np.ndarray,
        embedding2: np.ndarray
    ) -> float:
        """
        Compute cosine similarity between two embeddings.

        Args:
            embedding1: First embedding
            embedding2: Second embedding

        Returns:
            Cosine similarity score (0-1)
        """
        # Normalize embeddings
        emb1_norm = self._normalize_embedding(embedding1)
        emb2_norm = self._normalize_embedding(embedding2)

        # Compute dot product (cosine similarity for normalized vectors)
        similarity = np.dot(emb1_norm, emb2_norm)

        return float(np.clip(similarity, 0, 1))

    def clear_cache(self) -> None:
        """Clear embedding cache."""
        self._cache.clear()
        logger.info("Embedding cache cleared")

    def preload_models(self) -> None:
        """Preload all embedding models."""
        models_to_load = [
            self.settings.model.ko_embedder_model,
            self.settings.model.en_embedder_model
        ]

        for model_name in models_to_load:
            try:
                self._get_onnx_model(model_name)
                logger.info(f"Preloaded ONNX model: {model_name}")
            except Exception as e:
                logger.warning(f"Failed to preload model {model_name}: {e}")


# Global embedder instance
_embedder: Optional[TextEmbedder] = None


def get_embedder() -> TextEmbedder:
    """Get or create global embedder instance."""
    global _embedder
    if _embedder is None:
        _embedder = TextEmbedder()
    return _embedder
