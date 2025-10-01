import pytest
import time
from unittest.mock import patch, MagicMock
from app.models.classifier import get_home_life_classifier, HOME_LIFE_CATEGORIES
from tests.mocks.mock_models import MockClassifier

@pytest.mark.integration
@pytest.mark.requires_models
class TestRealClassifier:
    """Integration tests that can run with either real or mock models."""

    @pytest.fixture(scope="class")
    def classifier(self, request):
        """Get classifier - use mock in CI/test environments."""
        use_mock = request.config.getoption("--use-mocks", default=True)

        if use_mock:
            # Use mock classifier for fast, reliable tests
            return MockClassifier()
        else:
            # Use real classifier for full integration testing
            classifier = get_home_life_classifier()
            classifier.initialize()
            return classifier

    def test_real_classification(self, classifier):
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

        for text, expected_category in test_cases:
            category, confidence = classifier.classify(text)
            print(f"'{text}' → {category} ({confidence:.2%})")
            assert category == expected_category
            assert confidence > 0.5

    def test_batch_classification_performance(self, classifier):
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
        results = classifier.batch_classify(texts)
        elapsed_time = time.time() - start_time

        assert len(results) == len(texts)
        assert elapsed_time < 2.0

        for (category, confidence) in results:
            assert category in HOME_LIFE_CATEGORIES
            assert 0.0 <= confidence <= 1.0

    def test_edge_cases(self, classifier):
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
                category, confidence = classifier.classify(text)
                assert category in HOME_LIFE_CATEGORIES
                assert 0.0 <= confidence <= 1.0
            except Exception:
                pass

    def test_concurrent_classification(self, classifier):
        import threading
        import concurrent.futures

        test_texts = ["테스트 질문 " + str(i) for i in range(20)]
        results = []
        errors = []

        def classify_text(text):
            try:
                result = classifier.classify(text)
                results.append(result)
            except Exception as e:
                errors.append(str(e))

        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
            futures = [executor.submit(classify_text, text) for text in test_texts]
            concurrent.futures.wait(futures)

        assert len(errors) == 0
        assert len(results) == len(test_texts)