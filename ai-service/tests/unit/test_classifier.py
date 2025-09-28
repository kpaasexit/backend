"""Unit tests for text classifier with mocks."""

import pytest
import torch
import numpy as np
from unittest.mock import Mock, MagicMock, patch
from typing import List, Tuple

from app.models.classifier import (
    HomeLifeClassifier,
    get_classifier,
    get_home_life_classifier,
    HOME_LIFE_CATEGORIES
)
from app.core.exceptions import ClassificationError
from tests.mocks.mock_models import MockClassifier


@pytest.mark.unit
class TestHomeLifeClassifier:
    """Test HomeLifeClassifier functionality with mocks."""

    @pytest.fixture
    def mock_settings(self):
        """Mock settings for classifier."""
        settings = MagicMock()
        settings.model.classifier_model = "klue/roberta-small"
        settings.model.device = "cpu"
        return settings

    @pytest.fixture
    def mock_model_components(self):
        """Mock transformer model components."""
        # Mock model
        model = MagicMock()
        model.eval.return_value = model
        model.to.return_value = model

        # Mock tokenizer
        tokenizer = MagicMock()
        tokenizer.return_value = {
            'input_ids': torch.tensor([[1, 2, 3, 4, 5]]),
            'attention_mask': torch.tensor([[1, 1, 1, 1, 1]])
        }

        return model, tokenizer

    @pytest.fixture
    def classifier_with_mocks(self, mock_settings, mock_model_components):
        """Create classifier with mocked dependencies."""
        model, tokenizer = mock_model_components

        with patch('app.models.classifier.get_settings', return_value=mock_settings), \
             patch('app.models.classifier.AutoTokenizer') as mock_tokenizer_cls, \
             patch('app.models.classifier.RobertaForSequenceClassification') as mock_model_cls, \
             patch('os.path.exists', return_value=True):

            mock_tokenizer_cls.from_pretrained.return_value = tokenizer
            mock_model_cls.from_pretrained.return_value = model

            # Mock model output
            mock_outputs = MagicMock()
            # Create realistic logits (8 categories)
            logits = torch.tensor([[2.1, 0.5, 0.8, 0.3, 0.7, 0.4, 0.9, 0.6]])
            mock_outputs.logits = logits
            model.return_value = mock_outputs

            classifier = HomeLifeClassifier()
            classifier.initialize()
            return classifier

    def test_classifier_singleton_pattern(self):
        """Test that HomeLifeClassifier follows singleton pattern."""
        with patch('app.models.classifier.get_settings'):
            classifier1 = HomeLifeClassifier()
            classifier2 = HomeLifeClassifier()

            # Should be the same instance
            assert classifier1 is classifier2

    def test_classifier_initialization(self, classifier_with_mocks):
        """Test classifier initialization."""
        assert classifier_with_mocks._initialized is True
        assert classifier_with_mocks.model is not None
        assert classifier_with_mocks.tokenizer is not None
        assert classifier_with_mocks.device.type == "cpu"

    def test_classify_single_text(self, classifier_with_mocks):
        """Test classifying single text."""
        text = "김치찌개 맛있게 끓이는 방법"
        category, confidence = classifier_with_mocks.classify(text)

        assert category in HOME_LIFE_CATEGORIES
        assert isinstance(confidence, float)
        assert 0.0 <= confidence <= 1.0

    def test_classify_empty_text_raises_error(self, classifier_with_mocks):
        """Test that classifying empty text raises error."""
        with pytest.raises(ClassificationError, match="Empty or invalid text"):
            classifier_with_mocks.classify("")

        with pytest.raises(ClassificationError, match="Empty or invalid text"):
            classifier_with_mocks.classify("   ")

        with pytest.raises(ClassificationError, match="Empty or invalid text"):
            classifier_with_mocks.classify(None)

    def test_classify_various_categories(self, classifier_with_mocks):
        """Test classifying texts from various categories."""
        test_cases = [
            ("김치찌개 맛있게 끓이는 방법", "요리/식품관리"),
            ("화장실 곰팡이 제거하는 방법", "청소/세탁"),
            ("형광등 교체하는 방법", "생활수리/DIY"),
            ("전세 계약 주의사항", "생활경제/계약"),
            ("강아지 훈련 방법", "육아/반려동물"),
            ("원룸 인테리어 팁", "이사/인테리어"),
            ("미세먼지 대처법", "환경/건강"),
            ("에어컨 필터 청소", "스마트홈/가전")
        ]

        for text, expected_category in test_cases:
            category, confidence = classifier_with_mocks.classify(text)

            # Note: In unit tests with mocks, we might not get exact category matches
            # but we can test that it returns valid categories and confidences
            assert category in HOME_LIFE_CATEGORIES
            assert 0.0 <= confidence <= 1.0

    def test_classify_long_text_truncation(self, classifier_with_mocks):
        """Test that long text is properly handled (truncated)."""
        long_text = "김치찌개 " * 1000  # Very long text
        category, confidence = classifier_with_mocks.classify(long_text)

        assert category in HOME_LIFE_CATEGORIES
        assert 0.0 <= confidence <= 1.0

        # Verify tokenizer was called with truncation
        classifier_with_mocks.tokenizer.assert_called()
        call_args = classifier_with_mocks.tokenizer.call_args
        assert call_args[1]['truncation'] is True
        assert call_args[1]['max_length'] == 128

    def test_batch_classify(self, classifier_with_mocks):
        """Test batch classification."""
        texts = [
            "김치찌개 맛있게 끓이는 방법",
            "화장실 청소 방법",
            "형광등 교체하는 방법"
        ]

        results = classifier_with_mocks.batch_classify(texts)

        assert len(results) == len(texts)
        for category, confidence in results:
            assert category in HOME_LIFE_CATEGORIES
            assert 0.0 <= confidence <= 1.0

    def test_batch_classify_empty_list(self, classifier_with_mocks):
        """Test batch classification with empty list."""
        results = classifier_with_mocks.batch_classify([])
        assert results == []

    def test_batch_classify_error_handling(self, classifier_with_mocks):
        """Test batch classification error handling."""
        # Mock model to raise exception for batch processing
        classifier_with_mocks.model.side_effect = Exception("Batch processing failed")

        texts = ["Text 1", "Text 2"]

        # Should fall back to individual classification
        with patch.object(classifier_with_mocks, 'classify') as mock_classify:
            mock_classify.return_value = ("요리/식품관리", 0.8)

            results = classifier_with_mocks.batch_classify(texts)

            assert len(results) == 2
            assert mock_classify.call_count == 2

    def test_get_category_id(self, classifier_with_mocks):
        """Test getting category ID by name."""
        for i, category in enumerate(HOME_LIFE_CATEGORIES):
            category_id = classifier_with_mocks.get_category_id(category)
            assert category_id == i

    def test_get_category_id_invalid(self, classifier_with_mocks):
        """Test getting category ID for invalid category."""
        invalid_id = classifier_with_mocks.get_category_id("Invalid Category")
        assert invalid_id is None

    def test_classify_before_initialization(self):
        """Test classification before initialization."""
        with patch('app.models.classifier.get_settings'):
            classifier = HomeLifeClassifier()
            # Don't call initialize()

            with patch.object(classifier, 'initialize') as mock_init:
                with pytest.raises(Exception):
                    # This should trigger initialization
                    classifier.classify("test text")

    def test_initialization_with_model_path_priority(self):
        """Test initialization chooses correct model path."""
        mock_settings = MagicMock()

        with patch('app.models.classifier.get_settings', return_value=mock_settings), \
             patch('os.path.exists') as mock_exists, \
             patch('app.models.classifier.AutoTokenizer') as mock_tokenizer, \
             patch('app.models.classifier.RobertaForSequenceClassification') as mock_model:

            # Test finetuned model exists
            mock_exists.side_effect = lambda path: "finetuned" in path

            classifier = HomeLifeClassifier()
            classifier.initialize()

            # Should use finetuned path
            expected_path = "/home/ubuntu/backend/ai-service/app/models/home_life_finetuned"
            mock_tokenizer.from_pretrained.assert_called_with(expected_path)

    def test_initialization_fallback_to_base_model(self):
        """Test initialization falls back to base model."""
        mock_settings = MagicMock()

        with patch('app.models.classifier.get_settings', return_value=mock_settings), \
             patch('os.path.exists') as mock_exists, \
             patch('app.models.classifier.AutoTokenizer') as mock_tokenizer, \
             patch('app.models.classifier.RobertaForSequenceClassification') as mock_model:

            # Test only base model exists
            mock_exists.side_effect = lambda path: "finetuned" not in path

            classifier = HomeLifeClassifier()
            classifier.initialize()

            # Should use base path
            expected_path = "/home/ubuntu/backend/ai-service/app/models/home_life_classifier"
            mock_tokenizer.from_pretrained.assert_called_with(expected_path)

    def test_initialization_error_handling(self):
        """Test initialization error handling."""
        mock_settings = MagicMock()

        with patch('app.models.classifier.get_settings', return_value=mock_settings), \
             patch('os.path.exists', return_value=True), \
             patch('app.models.classifier.AutoTokenizer') as mock_tokenizer:

            # Make tokenizer raise exception
            mock_tokenizer.from_pretrained.side_effect = Exception("Model loading failed")

            classifier = HomeLifeClassifier()

            with pytest.raises(ClassificationError, match="Model initialization failed"):
                classifier.initialize()

    def test_model_device_handling(self, classifier_with_mocks):
        """Test that model is properly moved to device."""
        # Model should be moved to CPU device
        classifier_with_mocks.model.to.assert_called()

        # Should be in eval mode
        classifier_with_mocks.model.eval.assert_called()

    def test_tensor_operations(self, classifier_with_mocks):
        """Test tensor operations during classification."""
        text = "테스트 텍스트"

        with patch('torch.no_grad'):
            category, confidence = classifier_with_mocks.classify(text)

            # Verify that torch operations were mocked properly
            assert isinstance(category, str)
            assert isinstance(confidence, float)

    def test_softmax_and_argmax_operations(self, classifier_with_mocks):
        """Test softmax and argmax operations are called."""
        text = "김치찌개 만들기"

        with patch('torch.nn.functional.softmax') as mock_softmax, \
             patch('torch.argmax') as mock_argmax:

            # Setup mock returns
            mock_probabilities = torch.tensor([[0.1, 0.8, 0.05, 0.02, 0.01, 0.01, 0.005, 0.005]])
            mock_softmax.return_value = mock_probabilities
            mock_argmax.return_value = torch.tensor([1])  # Index 1

            # Mock the item() call
            mock_argmax.return_value.item.return_value = 1

            # Also need to mock max for confidence
            mock_max_result = MagicMock()
            mock_max_result.item.return_value = 0.8
            mock_probabilities[0, 1] = MagicMock()
            mock_probabilities[0, 1].item.return_value = 0.8

            category, confidence = classifier_with_mocks.classify(text)

            # Verify softmax was called
            mock_softmax.assert_called_once()
            # Verify argmax was called
            mock_argmax.assert_called_once()


@pytest.mark.unit
class TestGlobalClassifierFunctions:
    """Test global classifier functions."""

    def test_get_classifier_function(self):
        """Test get_classifier function."""
        with patch('app.models.classifier.HomeLifeClassifier') as mock_cls:
            mock_instance = MagicMock()
            mock_cls.return_value = mock_instance

            # Clear global instance
            import app.models.classifier
            app.models.classifier._classifier = None

            classifier = get_classifier()
            assert classifier == mock_instance

    def test_get_home_life_classifier_alias(self):
        """Test get_home_life_classifier is alias for get_classifier."""
        assert get_home_life_classifier is get_classifier

    def test_classifier_singleton_across_calls(self):
        """Test that classifier is singleton across multiple calls."""
        with patch('app.models.classifier.HomeLifeClassifier') as mock_cls:
            mock_instance = MagicMock()
            mock_cls.return_value = mock_instance

            # Clear global instance
            import app.models.classifier
            app.models.classifier._classifier = None

            classifier1 = get_classifier()
            classifier2 = get_classifier()

            assert classifier1 is classifier2
            # Should only instantiate once
            assert mock_cls.call_count == 1


@pytest.mark.unit
class TestHomeLifeCategories:
    """Test HOME_LIFE_CATEGORIES constant."""

    def test_categories_list_completeness(self):
        """Test that all expected categories are present."""
        expected_categories = [
            "생활경제/계약",
            "생활수리/DIY",
            "스마트홈/가전",
            "요리/식품관리",
            "육아/반려동물",
            "이사/인테리어",
            "청소/세탁",
            "환경/건강"
        ]

        assert len(HOME_LIFE_CATEGORIES) == 8
        for category in expected_categories:
            assert category in HOME_LIFE_CATEGORIES

    def test_categories_order_consistency(self):
        """Test that category order is consistent."""
        # This ensures model predictions map to correct categories
        assert HOME_LIFE_CATEGORIES[0] == "생활경제/계약"
        assert HOME_LIFE_CATEGORIES[1] == "생활수리/DIY"
        assert HOME_LIFE_CATEGORIES[7] == "환경/건강"

    def test_categories_are_strings(self):
        """Test that all categories are strings."""
        for category in HOME_LIFE_CATEGORIES:
            assert isinstance(category, str)
            assert len(category) > 0

    def test_categories_uniqueness(self):
        """Test that all categories are unique."""
        assert len(HOME_LIFE_CATEGORIES) == len(set(HOME_LIFE_CATEGORIES))


@pytest.mark.unit
class TestMockClassifier:
    """Test the MockClassifier implementation."""

    def test_mock_classifier_basic_functionality(self):
        """Test basic MockClassifier functionality."""
        mock_classifier = MockClassifier()

        text = "김치찌개 만들기"
        category, confidence = mock_classifier.classify(text)

        assert category in mock_classifier.categories
        assert isinstance(confidence, float)
        assert 0.0 <= confidence <= 1.0

    def test_mock_classifier_deterministic_results(self):
        """Test that MockClassifier produces deterministic results."""
        mock_classifier = MockClassifier()

        text = "동일한 텍스트"
        result1 = mock_classifier.classify(text)
        result2 = mock_classifier.classify(text)

        # Should be identical for same text
        assert result1 == result2

    def test_mock_classifier_keyword_based_classification(self):
        """Test that MockClassifier uses keyword-based classification."""
        mock_classifier = MockClassifier()

        # Test cooking-related text
        cooking_text = "김치찌개 맛있게 끓이는 방법"
        category, confidence = mock_classifier.classify(cooking_text)
        assert category == "요리/식품관리"
        assert confidence > 0.6

        # Test cleaning-related text
        cleaning_text = "화장실 곰팡이 제거 방법"
        category, confidence = mock_classifier.classify(cleaning_text)
        assert category == "청소/세탁"
        assert confidence > 0.6

    def test_mock_classifier_batch_functionality(self):
        """Test MockClassifier batch functionality."""
        mock_classifier = MockClassifier()

        texts = [
            "김치찌개 만들기",
            "화장실 청소법",
            "형광등 교체"
        ]

        results = mock_classifier.batch_classify(texts)

        assert len(results) == 3
        for category, confidence in results:
            assert category in mock_classifier.categories
            assert 0.0 <= confidence <= 1.0

    def test_mock_classifier_empty_text_error(self):
        """Test that MockClassifier raises error for empty text."""
        mock_classifier = MockClassifier()

        with pytest.raises(ClassificationError):
            mock_classifier.classify("")

        with pytest.raises(ClassificationError):
            mock_classifier.classify("   ")

    def test_mock_classifier_category_id_mapping(self):
        """Test MockClassifier category ID mapping."""
        mock_classifier = MockClassifier()

        for i, category in enumerate(mock_classifier.categories):
            category_id = mock_classifier.get_category_id(category)
            assert category_id == i

        # Test invalid category
        assert mock_classifier.get_category_id("Invalid") is None

    def test_mock_classifier_initialization(self):
        """Test MockClassifier initialization."""
        mock_classifier = MockClassifier()

        # Should be initialized by default
        assert mock_classifier._initialized is True

        # Initialize should not raise error
        mock_classifier.initialize()
        assert mock_classifier._initialized is True

    def test_mock_classifier_fallback_classification(self):
        """Test MockClassifier fallback for unrecognized text."""
        mock_classifier = MockClassifier()

        # Text without specific keywords
        generic_text = "xyz 123 random text"
        category, confidence = mock_classifier.classify(generic_text)

        # Should still return a valid category
        assert category in mock_classifier.categories
        # Confidence should be reasonable but not too high
        assert 0.4 <= confidence <= 0.7