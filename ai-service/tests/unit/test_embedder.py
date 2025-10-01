"""Unit tests for text embedder with mocks."""

import pytest
import numpy as np
from unittest.mock import Mock, MagicMock, patch
from typing import List

from app.models.embedder import TextEmbedder, get_embedder
from app.core.exceptions import EmbeddingError
from app.core.constants import Language, ModelType
from tests.mocks.mock_models import MockEmbedder


@pytest.mark.unit
class TestTextEmbedder:
    """Test TextEmbedder functionality with mocks."""

    @pytest.fixture
    def mock_settings(self):
        """Mock settings for embedder."""
        settings = MagicMock()
        settings.model.ko_embedder_model = "jhgan/ko-sroberta-multitask"
        settings.model.en_embedder_model = "sentence-transformers/all-MiniLM-L6-v2"
        settings.model.max_sequence_length = 512
        settings.model.batch_size = 16
        return settings

    @pytest.fixture
    def mock_model_manager(self):
        """Mock model manager."""
        manager = MagicMock()

        def mock_load_model(model_name, model_type):
            mock_model = MagicMock()
            mock_model.encode.return_value = np.random.rand(384).astype(np.float32)
            return {"model": mock_model}

        manager.load_model = mock_load_model
        return manager

    @pytest.fixture
    def mock_dimension_reducer(self):
        """Mock dimension reducer."""
        reducer = MagicMock()
        reducer.reduce_dimension.return_value = np.random.rand(384).astype(np.float32)
        return reducer

    @pytest.fixture
    def embedder(self, mock_settings, mock_model_manager, mock_dimension_reducer):
        """Create embedder with mocked dependencies."""
        with patch('app.models.embedder.get_settings', return_value=mock_settings), \
             patch('app.models.embedder.get_model_manager', return_value=mock_model_manager), \
             patch('app.models.embedder.get_dimension_reducer', return_value=mock_dimension_reducer):
            return TextEmbedder()

    def test_embedder_initialization(self, embedder, mock_settings, mock_model_manager):
        """Test embedder initialization."""
        assert embedder.settings == mock_settings
        assert embedder.model_manager == mock_model_manager
        assert embedder.target_dim == 384
        assert embedder._cache == {}

    def test_embed_single_text(self, embedder):
        """Test embedding single text."""
        text = "Hello world"
        embedding = embedder.embed(text)

        assert isinstance(embedding, np.ndarray)
        assert embedding.shape[-1] == 384
        assert not np.isnan(embedding).any()
        assert np.isfinite(embedding).all()

    def test_embed_empty_text_raises_error(self, embedder):
        """Test that embedding empty text raises error."""
        with pytest.raises(EmbeddingError, match="Empty or invalid text"):
            embedder.embed("")

        with pytest.raises(EmbeddingError, match="Empty or invalid text"):
            embedder.embed("   ")

        with pytest.raises(EmbeddingError, match="Empty or invalid text"):
            embedder.embed(None)

    def test_embed_with_cache(self, embedder):
        """Test embedding with cache functionality."""
        text = "Test text for caching"

        # First call - should compute embedding
        embedding1 = embedder.embed(text, use_cache=True)

        # Second call - should return cached result
        embedding2 = embedder.embed(text, use_cache=True)

        # Should be identical (from cache)
        np.testing.assert_array_equal(embedding1, embedding2)

        # Cache should contain the embedding
        cache_key = embedder._get_cache_key(text)
        assert cache_key in embedder._cache

    def test_embed_without_cache(self, embedder):
        """Test embedding without using cache."""
        text = "Test text without cache"

        embedding = embedder.embed(text, use_cache=False)

        # Cache should be empty
        cache_key = embedder._get_cache_key(text)
        assert cache_key not in embedder._cache

    def test_embed_with_normalization(self, embedder):
        """Test embedding with normalization."""
        text = "Test normalization"

        # With normalization (default)
        normalized_embedding = embedder.embed(text, normalize=True)
        norm = np.linalg.norm(normalized_embedding)
        assert abs(norm - 1.0) < 1e-5  # Should be unit vector

        # Without normalization
        with patch.object(embedder, '_normalize_embedding', return_value=np.random.rand(384)):
            unnormalized_embedding = embedder.embed(text, normalize=False)
            # Should not be unit vector (in this mock case)

    @patch('app.models.embedder.LanguageDetector')
    @patch('app.models.embedder.get_dominant_language')
    def test_embed_single_language_korean(self, mock_get_lang, mock_lang_detector, embedder):
        """Test embedding Korean text."""
        mock_lang_detector.requires_multilingual_processing.return_value = False
        mock_get_lang.return_value = (Language.KOREAN, 0.95)

        text = "안녕하세요"
        embedding = embedder.embed(text)

        assert isinstance(embedding, np.ndarray)
        mock_get_lang.assert_called_once_with(text)

    @patch('app.models.embedder.LanguageDetector')
    @patch('app.models.embedder.get_dominant_language')
    def test_embed_single_language_english(self, mock_get_lang, mock_lang_detector, embedder):
        """Test embedding English text."""
        mock_lang_detector.requires_multilingual_processing.return_value = False
        mock_get_lang.return_value = (Language.ENGLISH, 0.95)

        text = "Hello world"
        embedding = embedder.embed(text)

        assert isinstance(embedding, np.ndarray)
        mock_get_lang.assert_called_once_with(text)

    @patch('app.models.embedder.LanguageDetector')
    @patch('app.models.embedder.get_language_weights')
    def test_embed_multilingual_text(self, mock_get_weights, mock_lang_detector, embedder):
        """Test embedding multilingual text."""
        mock_lang_detector.requires_multilingual_processing.return_value = True
        mock_get_weights.return_value = {"ko": 0.6, "en": 0.4}

        text = "Hello 안녕하세요"
        embedding = embedder.embed(text)

        assert isinstance(embedding, np.ndarray)
        mock_get_weights.assert_called_once_with(text)

    def test_embed_multilayer(self, embedder):
        """Test multilayer embedding."""
        title = "Test title"
        content = "This is test content for multilayer embedding"
        keywords = ["test", "embedding", "multilayer"]

        embedding = embedder.embed_multilayer(title, content, keywords)

        assert isinstance(embedding, np.ndarray)
        assert embedding.shape[-1] == 384

    def test_embed_multilayer_title_only(self, embedder):
        """Test multilayer embedding with title only."""
        title = "Test title only"

        embedding = embedder.embed_multilayer(title, None, None)

        assert isinstance(embedding, np.ndarray)

    def test_embed_multilayer_no_input_raises_error(self, embedder):
        """Test that multilayer embedding with no input raises error."""
        with pytest.raises(EmbeddingError, match="No valid input"):
            embedder.embed_multilayer("", None, None)

    def test_batch_embed(self, embedder):
        """Test batch embedding."""
        texts = ["First text", "Second text", "Third text"]

        embeddings = embedder.batch_embed(texts)

        assert isinstance(embeddings, list)
        assert len(embeddings) == len(texts)
        for embedding in embeddings:
            assert isinstance(embedding, np.ndarray)
            assert embedding.shape[-1] == 384

    def test_batch_embed_empty_list(self, embedder):
        """Test batch embedding with empty list."""
        embeddings = embedder.batch_embed([])
        assert embeddings == []

    @patch('app.models.embedder.LanguageDetector')
    @patch('app.models.embedder.get_dominant_language')
    def test_batch_embed_language_grouping(self, mock_get_lang, mock_lang_detector, embedder):
        """Test that batch embedding groups by language."""
        mock_lang_detector.requires_multilingual_processing.return_value = False

        # Mock language detection
        def mock_language_detection(text):
            if "안녕" in text:
                return (Language.KOREAN, 0.9)
            else:
                return (Language.ENGLISH, 0.9)

        mock_get_lang.side_effect = mock_language_detection

        texts = ["Hello world", "안녕하세요", "Good morning", "반갑습니다"]
        embeddings = embedder.batch_embed(texts)

        assert len(embeddings) == 4
        # Language detection should have been called for each text
        assert mock_get_lang.call_count == 4

    def test_batch_embed_with_cache(self, embedder):
        """Test batch embedding with cache."""
        texts = ["Cached text 1", "Cached text 2"]

        # First batch
        embeddings1 = embedder.batch_embed(texts, use_cache=True)

        # Second batch (should use cache)
        embeddings2 = embedder.batch_embed(texts, use_cache=True)

        # Results should be identical
        for emb1, emb2 in zip(embeddings1, embeddings2):
            np.testing.assert_array_equal(emb1, emb2)

    def test_compute_similarity(self, embedder):
        """Test computing similarity between embeddings."""
        # Create two similar embeddings
        embedding1 = np.array([1.0, 0.0, 0.0, 0.0] + [0.0] * 380, dtype=np.float32)
        embedding2 = np.array([0.9, 0.1, 0.0, 0.0] + [0.0] * 380, dtype=np.float32)

        similarity = embedder.compute_similarity(embedding1, embedding2)

        assert isinstance(similarity, float)
        assert 0.0 <= similarity <= 1.0

    def test_compute_similarity_identical(self, embedder):
        """Test similarity of identical embeddings."""
        embedding = np.random.rand(384).astype(np.float32)
        similarity = embedder.compute_similarity(embedding, embedding)

        assert abs(similarity - 1.0) < 1e-5

    def test_compute_similarity_orthogonal(self, embedder):
        """Test similarity of orthogonal embeddings."""
        embedding1 = np.zeros(384, dtype=np.float32)
        embedding1[0] = 1.0

        embedding2 = np.zeros(384, dtype=np.float32)
        embedding2[1] = 1.0

        similarity = embedder.compute_similarity(embedding1, embedding2)

        assert abs(similarity) < 1e-5  # Should be close to 0

    def test_clear_cache(self, embedder):
        """Test clearing embedding cache."""
        # Add something to cache
        text = "Test for cache clearing"
        embedder.embed(text, use_cache=True)

        # Verify cache has content
        assert len(embedder._cache) > 0

        # Clear cache
        embedder.clear_cache()

        # Verify cache is empty
        assert len(embedder._cache) == 0

    def test_preload_models(self, embedder):
        """Test preloading models."""
        # Should not raise any errors
        embedder.preload_models()

        # Verify that load_model was called for both Korean and English models
        calls = embedder.model_manager.load_model.call_args_list
        assert len(calls) >= 2

    def test_normalize_embedding_zero_vector(self, embedder):
        """Test normalizing zero vector."""
        zero_vector = np.zeros(384, dtype=np.float32)
        normalized = embedder._normalize_embedding(zero_vector)

        # Should return the same zero vector
        np.testing.assert_array_equal(normalized, zero_vector)

    def test_normalize_embedding_regular_vector(self, embedder):
        """Test normalizing regular vector."""
        vector = np.array([3.0, 4.0] + [0.0] * 382, dtype=np.float32)
        normalized = embedder._normalize_embedding(vector)

        norm = np.linalg.norm(normalized)
        assert abs(norm - 1.0) < 1e-5

    def test_resize_embedding(self, embedder):
        """Test resizing embedding."""
        # Mock dimension reducer behavior
        original_embedding = np.random.rand(768).astype(np.float32)

        resized = embedder._resize_embedding(original_embedding, 384)

        # Should have target dimension
        assert resized.shape[-1] == 384

    def test_get_cache_key_consistency(self, embedder):
        """Test that cache key generation is consistent."""
        text = "Test text for cache key"

        key1 = embedder._get_cache_key(text)
        key2 = embedder._get_cache_key(text)

        assert key1 == key2
        assert isinstance(key1, str)
        assert len(key1) > 0

    def test_get_cache_key_different_texts(self, embedder):
        """Test that different texts generate different cache keys."""
        text1 = "First text"
        text2 = "Second text"

        key1 = embedder._get_cache_key(text1)
        key2 = embedder._get_cache_key(text2)

        assert key1 != key2

    def test_error_handling_in_embed(self, embedder):
        """Test error handling in embed method."""
        # Mock model manager to raise an exception
        embedder.model_manager.load_model.side_effect = Exception("Model loading failed")

        with pytest.raises(EmbeddingError):
            embedder.embed("Test text")

    @patch('app.models.embedder.LanguageDetector')
    def test_group_by_language(self, mock_lang_detector, embedder):
        """Test _group_by_language method."""
        mock_lang_detector.requires_multilingual_processing.return_value = False

        with patch('app.models.embedder.get_dominant_language') as mock_get_lang:
            def mock_language_detection(text):
                if "안녕" in text:
                    return (Language.KOREAN, 0.9)
                else:
                    return (Language.ENGLISH, 0.9)

            mock_get_lang.side_effect = mock_language_detection

            texts = ["Hello", "안녕하세요", "World", "반갑습니다"]
            groups = embedder._group_by_language(texts)

            assert Language.ENGLISH in groups
            assert Language.KOREAN in groups
            assert len(groups[Language.ENGLISH]["texts"]) == 2
            assert len(groups[Language.KOREAN]["texts"]) == 2

    def test_batch_process_group_empty(self, embedder):
        """Test _batch_process_group with empty list."""
        result = embedder._batch_process_group([], Language.ENGLISH, normalize=True)
        assert result == []

    @patch('app.models.embedder.LanguageDetector')
    def test_batch_process_group_mixed_language(self, mock_lang_detector, embedder):
        """Test _batch_process_group with mixed language."""
        mock_lang_detector.requires_multilingual_processing.return_value = True

        texts = ["Hello 안녕하세요", "Mixed text"]

        with patch.object(embedder, 'embed') as mock_embed:
            mock_embed.return_value = np.random.rand(384).astype(np.float32)

            result = embedder._batch_process_group(texts, Language.MIXED, normalize=True)

            assert len(result) == 2
            # Should call individual embed for mixed language
            assert mock_embed.call_count == 2


@pytest.mark.unit
class TestGetEmbedder:
    """Test get_embedder global function."""

    def test_get_embedder_singleton(self):
        """Test that get_embedder returns singleton instance."""
        with patch('app.models.embedder.TextEmbedder') as mock_embedder_class:
            mock_instance = MagicMock()
            mock_embedder_class.return_value = mock_instance

            # Clear any existing instance
            import app.models.embedder
            app.models.embedder._embedder = None

            # First call
            embedder1 = get_embedder()
            assert embedder1 == mock_instance

            # Second call should return same instance
            embedder2 = get_embedder()
            assert embedder2 == mock_instance

            # TextEmbedder should only be instantiated once
            assert mock_embedder_class.call_count == 1


@pytest.mark.unit
class TestMockEmbedder:
    """Test the MockEmbedder implementation."""

    def test_mock_embedder_basic_functionality(self):
        """Test basic MockEmbedder functionality."""
        mock_embedder = MockEmbedder()

        text = "Test text"
        embedding = mock_embedder.embed(text)

        assert isinstance(embedding, np.ndarray)
        assert embedding.shape == (384,)
        assert embedding.dtype == np.float32

    def test_mock_embedder_deterministic(self):
        """Test that MockEmbedder produces deterministic results."""
        mock_embedder = MockEmbedder()

        text = "Same text"
        embedding1 = mock_embedder.embed(text)
        embedding2 = mock_embedder.embed(text)

        # Should be identical for same text
        np.testing.assert_array_equal(embedding1, embedding2)

    def test_mock_embedder_different_texts(self):
        """Test that MockEmbedder produces different results for different texts."""
        mock_embedder = MockEmbedder()

        embedding1 = mock_embedder.embed("Text 1")
        embedding2 = mock_embedder.embed("Text 2")

        # Should be different for different texts
        assert not np.array_equal(embedding1, embedding2)

    def test_mock_embedder_batch_functionality(self):
        """Test MockEmbedder batch functionality."""
        mock_embedder = MockEmbedder()

        texts = ["Text 1", "Text 2", "Text 3"]
        embeddings = mock_embedder.batch_embed(texts)

        assert len(embeddings) == 3
        for embedding in embeddings:
            assert isinstance(embedding, np.ndarray)
            assert embedding.shape == (384,)

    def test_mock_embedder_similarity(self):
        """Test MockEmbedder similarity computation."""
        mock_embedder = MockEmbedder()

        embedding1 = np.array([1.0, 0.0] + [0.0] * 382, dtype=np.float32)
        embedding2 = np.array([1.0, 0.0] + [0.0] * 382, dtype=np.float32)

        similarity = mock_embedder.compute_similarity(embedding1, embedding2)

        assert isinstance(similarity, float)
        assert 0.0 <= similarity <= 1.0

    def test_mock_embedder_empty_text_error(self):
        """Test that MockEmbedder raises error for empty text."""
        mock_embedder = MockEmbedder()

        with pytest.raises(EmbeddingError):
            mock_embedder.embed("")

        with pytest.raises(EmbeddingError):
            mock_embedder.embed("   ")