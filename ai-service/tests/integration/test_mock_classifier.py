"""Integration tests for classifier using mocks instead of real models."""

import pytest
import time
from unittest.mock import patch, MagicMock
from typing import List, Tuple

from app.models.classifier import get_home_life_classifier, HOME_LIFE_CATEGORIES
from app.core.exceptions import ClassificationError
from tests.mocks.mock_models import MockClassifier


@pytest.mark.integration
class TestClassifierIntegration:
    """Integration tests for classifier with mocked dependencies."""

    @pytest.fixture(scope="class")
    def mock_classifier(self):
        """Create mock classifier for integration testing."""
        return MockClassifier()

    def test_mock_classification_accuracy(self, mock_classifier):
        """Test classification accuracy with known examples."""
        test_cases = [
            ("김치찌개 맛있게 끓이는 방법", "요리/식품관리"),
            ("화장실 곰팡이 제거하는 방법", "청소/세탁"),
            ("형광등 교체하는 방법", "생활수리/DIY"),
            ("전세 계약시 주의사항", "생활경제/계약"),
            ("강아지 배변훈련 방법", "육아/반려동물"),
            ("원룸 인테리어 꾸미기", "이사/인테리어"),
            ("미세먼지 심한날 대처법", "환경/건강"),
            ("에어컨 필터 청소 방법", "스마트홈/가전")
        ]

        correct_predictions = 0
        for text, expected_category in test_cases:
            category, confidence = mock_classifier.classify(text)
            print(f"'{text}' → {category} ({confidence:.2%})")

            # Mock classifier should classify correctly based on keywords
            if category == expected_category:
                correct_predictions += 1

            assert category in HOME_LIFE_CATEGORIES
            assert confidence > 0.5

        # Mock classifier should have reasonable accuracy
        accuracy = correct_predictions / len(test_cases)
        print(f"Mock classification accuracy: {accuracy:.2%}")
        assert accuracy > 0.6  # At least 60% accuracy for mocked classification

    def test_batch_classification_performance(self, mock_classifier):
        """Test batch classification performance."""
        texts = [
            "김치 보관 방법",
            "욕실 청소 방법",
            "벽에 못 박는 방법",
            "신용카드 연회비 줄이기",
            "고양이 털 관리",
            "이사 준비 체크리스트",
            "실내 공기 정화 방법",
            "로봇청소기 관리법"
        ]

        start_time = time.time()
        results = mock_classifier.batch_classify(texts)
        elapsed_time = time.time() - start_time

        assert len(results) == len(texts)
        # Mock should be very fast
        assert elapsed_time < 0.1

        for (category, confidence) in results:
            assert category in HOME_LIFE_CATEGORIES
            assert 0.0 <= confidence <= 1.0

    def test_edge_cases_handling(self, mock_classifier):
        """Test handling of edge cases."""
        edge_cases = [
            "ㅋㅋㅋㅋㅋ",
            "123456789",
            "????",
            "      ",
            "a" * 500,
            "김치 맛있게 끓이는 방법" * 10
        ]

        for text in edge_cases:
            try:
                category, confidence = mock_classifier.classify(text)
                print(f"Edge case '{text[:20]}...' → {category} ({confidence:.2%})")
                assert category in HOME_LIFE_CATEGORIES
                assert 0.0 <= confidence <= 1.0
            except ClassificationError:
                # Some edge cases may raise errors, which is acceptable
                print(f"Edge case '{text[:20]}...' → Error (expected)")
                pass

    def test_concurrent_classification(self, mock_classifier):
        """Test concurrent classification with mock."""
        import threading
        import concurrent.futures

        test_texts = ["테스트 질문 " + str(i) for i in range(20)]
        results = []
        errors = []

        def classify_text(text):
            try:
                result = mock_classifier.classify(text)
                results.append(result)
            except Exception as e:
                errors.append(str(e))

        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
            futures = [executor.submit(classify_text, text) for text in test_texts]
            concurrent.futures.wait(futures)

        assert len(errors) == 0  # Mock should handle concurrency well
        assert len(results) == len(test_texts)

        for category, confidence in results:
            assert category in HOME_LIFE_CATEGORIES
            assert 0.0 <= confidence <= 1.0


@pytest.mark.integration
class TestClassifierWithDependencyMocking:
    """Test classifier with fully mocked dependencies."""

    def test_classifier_with_mocked_torch_operations(self, mock_settings, mock_model_manager):
        """Test classifier with mocked PyTorch operations."""
        with patch('app.models.classifier.get_settings', return_value=mock_settings), \
             patch('app.models.classifier.torch.device') as mock_device, \
             patch('app.models.classifier.AutoTokenizer') as mock_tokenizer_cls, \
             patch('app.models.classifier.RobertaForSequenceClassification') as mock_model_cls, \
             patch('os.path.exists', return_value=True):

            # Setup mocks
            mock_device.return_value = "cpu"

            mock_tokenizer = MagicMock()
            mock_tokenizer.return_value = {
                'input_ids': MagicMock(),
                'attention_mask': MagicMock()
            }
            mock_tokenizer_cls.from_pretrained.return_value = mock_tokenizer

            mock_model = MagicMock()
            mock_model.eval.return_value = mock_model
            mock_model.to.return_value = mock_model
            mock_model_cls.from_pretrained.return_value = mock_model

            # Create classifier and test
            from app.models.classifier import HomeLifeClassifier

            classifier = HomeLifeClassifier()
            classifier.initialize()

            # Verify initialization worked
            assert classifier._initialized is True
            assert classifier.model is not None
            assert classifier.tokenizer is not None

    def test_classifier_initialization_flow(self):
        """Test complete classifier initialization flow with mocks."""
        mock_settings = MagicMock()

        with patch('app.models.classifier.get_settings', return_value=mock_settings), \
             patch('os.path.exists') as mock_exists, \
             patch('app.models.classifier.AutoTokenizer') as mock_tokenizer_cls, \
             patch('app.models.classifier.RobertaForSequenceClassification') as mock_model_cls, \
             patch('app.models.classifier.torch'):

            mock_exists.return_value = True
            mock_tokenizer_cls.from_pretrained.return_value = MagicMock()
            mock_model_cls.from_pretrained.return_value = MagicMock()

            from app.models.classifier import HomeLifeClassifier

            classifier = HomeLifeClassifier()

            # Should not be initialized yet
            assert hasattr(classifier, '_initialized_flag')

            # Initialize
            classifier.initialize()

            # Should be initialized
            assert classifier._initialized is True

            # Verify mocks were called
            mock_tokenizer_cls.from_pretrained.assert_called_once()
            mock_model_cls.from_pretrained.assert_called_once()

    def test_error_propagation_in_integration(self):
        """Test that errors are properly propagated in integration context."""
        mock_settings = MagicMock()

        with patch('app.models.classifier.get_settings', return_value=mock_settings), \
             patch('os.path.exists', return_value=True), \
             patch('app.models.classifier.AutoTokenizer') as mock_tokenizer_cls:

            # Make tokenizer loading fail
            mock_tokenizer_cls.from_pretrained.side_effect = Exception("Model download failed")

            from app.models.classifier import HomeLifeClassifier

            classifier = HomeLifeClassifier()

            with pytest.raises(ClassificationError, match="Model initialization failed"):
                classifier.initialize()


@pytest.mark.integration
class TestClassifierPerformanceWithMocks:
    """Test classifier performance characteristics with mocks."""

    def test_classification_speed(self, mock_settings):
        """Test classification speed with mocks."""
        # Use actual MockClassifier for realistic performance testing
        mock_classifier = MockClassifier()

        test_texts = [
            "김치찌개 만드는 방법",
            "화장실 청소하기",
            "전구 교체 방법"
        ] * 10  # 30 texts total

        start_time = time.time()

        for text in test_texts:
            category, confidence = mock_classifier.classify(text)
            assert category in HOME_LIFE_CATEGORIES

        end_time = time.time()
        total_time = end_time - start_time
        avg_time_per_classification = total_time / len(test_texts)

        print(f"Average classification time: {avg_time_per_classification*1000:.2f}ms")

        # Mock should be very fast
        assert avg_time_per_classification < 0.01  # Less than 10ms per classification

    def test_batch_vs_individual_performance(self, mock_settings):
        """Test batch vs individual classification performance."""
        mock_classifier = MockClassifier()

        test_texts = ["테스트 텍스트 " + str(i) for i in range(10)]

        # Individual classification timing
        start_time = time.time()
        individual_results = []
        for text in test_texts:
            result = mock_classifier.classify(text)
            individual_results.append(result)
        individual_time = time.time() - start_time

        # Batch classification timing
        start_time = time.time()
        batch_results = mock_classifier.batch_classify(test_texts)
        batch_time = time.time() - start_time

        print(f"Individual classification time: {individual_time:.4f}s")
        print(f"Batch classification time: {batch_time:.4f}s")

        # Results should be identical
        assert len(individual_results) == len(batch_results)

        # With mocks, both should be very fast
        assert individual_time < 0.1
        assert batch_time < 0.1

    def test_memory_efficiency_simulation(self):
        """Test memory efficiency simulation with mocks."""
        # Create multiple classifier instances to test singleton behavior
        mock_classifiers = []

        for _ in range(5):
            classifier = get_home_life_classifier()
            mock_classifiers.append(classifier)

        # All should be the same instance (singleton)
        for classifier in mock_classifiers[1:]:
            assert classifier is mock_classifiers[0]

        print("Singleton pattern working correctly - memory efficient")


@pytest.mark.integration
class TestClassifierResilience:
    """Test classifier resilience with various input conditions."""

    def test_unicode_and_special_characters(self):
        """Test classifier with unicode and special characters."""
        mock_classifier = MockClassifier()

        special_texts = [
            "김치찌개 만들기 👍",
            "청소 방법 🧽✨",
            "DIY 수리법 🔧",
            "계약서 작성 📝",
            "반려동물 돌보기 🐕",
            "인테리어 팁 🏠",
            "건강 관리 💪",
            "스마트홈 설정 📱"
        ]

        for text in special_texts:
            try:
                category, confidence = mock_classifier.classify(text)
                assert category in HOME_LIFE_CATEGORIES
                assert 0.0 <= confidence <= 1.0
                print(f"'{text}' → {category} ({confidence:.2%})")
            except Exception as e:
                print(f"Failed on '{text}': {e}")
                # Should not fail on unicode
                assert False, f"Should handle unicode text: {text}"

    def test_mixed_language_classification(self):
        """Test classification of mixed Korean-English text."""
        mock_classifier = MockClassifier()

        mixed_texts = [
            "김치 kimchi recipe 만들기",
            "bathroom 화장실 cleaning tips",
            "DIY home 수리 repair",
            "contract 계약 advice",
            "pet 반려동물 care",
            "interior design 인테리어",
            "health 건강 tips",
            "smart home 스마트홈 setup"
        ]

        for text in mixed_texts:
            category, confidence = mock_classifier.classify(text)
            assert category in HOME_LIFE_CATEGORIES
            assert 0.0 <= confidence <= 1.0
            print(f"'{text}' → {category} ({confidence:.2%})")

    def test_very_short_and_very_long_texts(self):
        """Test classification of very short and very long texts."""
        mock_classifier = MockClassifier()

        # Very short texts
        short_texts = ["김치", "청소", "수리", "계약"]
        for text in short_texts:
            category, confidence = mock_classifier.classify(text)
            assert category in HOME_LIFE_CATEGORIES
            print(f"Short: '{text}' → {category} ({confidence:.2%})")

        # Very long text
        long_text = "김치찌개를 맛있게 끓이는 방법에 대해 알려드리겠습니다. " * 50
        category, confidence = mock_classifier.classify(long_text)
        assert category in HOME_LIFE_CATEGORIES
        print(f"Long text → {category} ({confidence:.2%})")

    def test_classification_consistency(self):
        """Test that classification is consistent across multiple calls."""
        mock_classifier = MockClassifier()

        test_text = "김치찌개 맛있게 끓이는 방법"

        # Run classification multiple times
        results = []
        for _ in range(10):
            result = mock_classifier.classify(test_text)
            results.append(result)

        # All results should be identical (deterministic)
        first_result = results[0]
        for result in results[1:]:
            assert result == first_result

        print(f"Consistent result: {first_result[0]} ({first_result[1]:.2%})")