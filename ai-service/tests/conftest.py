import os
import sys
import asyncio
from pathlib import Path
from typing import Generator, AsyncGenerator, Dict, Any, List
from unittest.mock import MagicMock, AsyncMock, Mock

import pytest
import pytest_asyncio
import numpy as np
from dotenv import load_dotenv

PROJECT_ROOT = Path(__file__).parent.parent
sys.path.insert(0, str(PROJECT_ROOT))
env_path = PROJECT_ROOT / '.env'
if env_path.exists():
    load_dotenv(env_path)
    print(f".env 파일 로드: {env_path}")
    if os.getenv('OPENAI_API_KEY'):
        print(f"OPENAI_API_KEY 설정됨: {os.getenv('OPENAI_API_KEY')[:10]}...")

@pytest.fixture(scope="session")
def event_loop():
    """Create an instance of the default event loop for the test session."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()

@pytest.fixture
def mock_redis_client():
    """Mock Redis client for testing."""
    client = MagicMock()
    client.get.return_value = None
    client.set.return_value = True
    client.delete.return_value = 1
    client.exists.return_value = False
    client.ping.return_value = True
    client.flushdb.return_value = True
    return client

@pytest.fixture
def mock_qdrant_client():
    """Mock Qdrant client for testing."""
    client = MagicMock()

    # Mock collection operations
    client.get_collections.return_value.collections = []
    client.collection_exists.return_value = False
    client.create_collection.return_value = True
    client.delete_collection.return_value = True

    # Mock search operations
    client.search.return_value = []
    client.scroll.return_value = ([], None)

    # Mock upsert operations
    client.upsert.return_value = MagicMock()
    client.delete.return_value = MagicMock()

    return client

@pytest.fixture
def mock_openai_client():
    """Mock OpenAI client for testing."""
    client = MagicMock()

    # Mock embeddings
    mock_embedding = MagicMock()
    mock_embedding.data = [MagicMock(embedding=np.random.rand(384).tolist())]
    mock_embedding.usage = MagicMock(total_tokens=50)

    client.embeddings.create.return_value = mock_embedding

    # Mock chat completions
    mock_completion = MagicMock()
    mock_completion.choices = [
        MagicMock(message=MagicMock(content="테스트 답변입니다."))
    ]
    mock_completion.usage = MagicMock(total_tokens=100)

    client.chat.completions.create.return_value = mock_completion

    return client

@pytest.fixture
def mock_embedder_model():
    """Mock embedding model for testing."""
    model = MagicMock()

    def mock_encode(texts, **kwargs):
        if isinstance(texts, str):
            return np.random.rand(384).astype(np.float32)
        else:
            return np.random.rand(len(texts), 384).astype(np.float32)

    model.encode = mock_encode
    model.device = "cpu"
    model.max_seq_length = 512

    return model

@pytest.fixture
def mock_classifier_model():
    """Mock classifier model for testing."""
    import torch

    model = MagicMock()
    tokenizer = MagicMock()

    # Mock tokenizer
    tokenizer.return_value = {
        'input_ids': torch.tensor([[1, 2, 3]]),
        'attention_mask': torch.tensor([[1, 1, 1]])
    }

    # Mock model forward pass
    mock_outputs = MagicMock()
    mock_logits = torch.tensor([[0.1, 0.9, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1]])
    mock_outputs.logits = mock_logits

    model.return_value = mock_outputs
    model.eval.return_value = model
    model.to.return_value = model

    return {"model": model, "tokenizer": tokenizer}

@pytest.fixture
def mock_model_manager():
    """Mock model manager for testing."""
    manager = MagicMock()

    def mock_load_model(model_name, model_type):
        if "embedder" in model_name.lower() or model_type.name == "EMBEDDER":
            return {"model": mock_embedder_model()}
        elif "classifier" in model_name.lower() or model_type.name == "CLASSIFIER":
            return mock_classifier_model()
        else:
            return {"model": MagicMock()}

    manager.load_model = mock_load_model
    manager.unload_model.return_value = True
    manager.is_loaded.return_value = True
    manager.get_memory_usage.return_value = {"total": 1024, "models": {}}

    return manager

@pytest.fixture
def mock_dimension_reducer():
    """Mock dimension reducer for testing."""
    reducer = MagicMock()

    def mock_reduce(embedding):
        # Always return 384-dimensional embedding for consistency
        if len(embedding.shape) == 1:
            return np.random.rand(384).astype(np.float32)
        else:
            return np.random.rand(embedding.shape[0], 384).astype(np.float32)

    reducer.reduce_dimension = mock_reduce
    return reducer

@pytest.fixture
def mock_settings():
    """Mock application settings for testing."""
    from app.config.main import Settings
    from app.config.model import ModelConfig
    from app.config.server import ServerConfig
    from app.config.database import QdrantConfig, RedisConfig
    from app.config.services import OpenAIConfig
    from app.config.system import MemoryConfig

    settings = MagicMock(spec=Settings)

    # Mock sub-configs
    settings.model = MagicMock(spec=ModelConfig)
    settings.model.ko_embedder_model = "jhgan/ko-sroberta-multitask"
    settings.model.en_embedder_model = "sentence-transformers/all-MiniLM-L6-v2"
    settings.model.classifier_model = "klue/roberta-small"
    settings.model.max_sequence_length = 512
    settings.model.batch_size = 16
    settings.model.model_cache_dir = Path("/tmp/models")
    settings.model.device = "cpu"

    settings.server = MagicMock(spec=ServerConfig)
    settings.server.environment = "test"
    settings.server.debug = True

    settings.qdrant = MagicMock(spec=QdrantConfig)
    settings.qdrant.host = "localhost"
    settings.qdrant.port = 6333
    settings.qdrant.collection_name = "test_questions"

    settings.redis = MagicMock(spec=RedisConfig)
    settings.redis.host = "localhost"
    settings.redis.port = 6379
    settings.redis.db = 0

    settings.openai = MagicMock(spec=OpenAIConfig)
    settings.openai.openai_api_key = "test-key"
    settings.openai.model = "gpt-3.5-turbo"

    settings.memory = MagicMock(spec=MemoryConfig)
    settings.memory.max_memory_mb = 2048

    settings.categories = [
        "요리/식품관리", "청소/세탁", "생활수리/DIY", "생활경제/계약",
        "육아/반려동물", "이사/인테리어", "환경/건강", "스마트홈/가전"
    ]

    settings.is_production = False
    settings.is_development = False

    return settings

@pytest.fixture
def sample_questions():
    """Sample questions for testing by category."""
    return {
        "요리/식품관리": [
            "김치찌개 맛있게 끓이는 방법",
            "계란 신선도 확인하는 법",
            "돼지고기 누린내 제거 방법"
        ],
        "청소/세탁": [
            "화장실 곰팡이 제거하는 방법",
            "흰 옷 얼룩 빼는 법",
            "카펫 청소 방법"
        ],
        "생활수리/DIY": [
            "형광등 교체하는 방법",
            "수도꼭지 고치는 법",
            "문짝 소리 없애기"
        ],
        "생활경제/계약": [
            "전세 계약 주의사항",
            "신용카드 연회비 절약법",
            "대출 이자 계산법"
        ],
        "육아/반려동물": [
            "강아지 훈련 방법",
            "아기 이유식 시작 시기",
            "고양이 털 관리법"
        ],
        "이사/인테리어": [
            "원룸 인테리어 팁",
            "이사 업체 선택법",
            "작은 방 꾸미기"
        ],
        "환경/건강": [
            "미세먼지 대처법",
            "실내 공기 정화 방법",
            "수면 질 개선법"
        ],
        "스마트홈/가전": [
            "스마트홈 구축 방법",
            "에어컨 필터 청소",
            "로봇청소기 관리법"
        ]
    }

@pytest.fixture
def sample_embeddings():
    """Sample embeddings for testing."""
    return {
        "dim_384": np.random.rand(384).astype(np.float32),
        "dim_768": np.random.rand(768).astype(np.float32),
        "batch_384": np.random.rand(5, 384).astype(np.float32),
    }

@pytest.fixture
def sample_classification_results():
    """Sample classification results for testing."""
    return [
        ("요리/식품관리", 0.95),
        ("청소/세탁", 0.87),
        ("생활수리/DIY", 0.92),
        ("생활경제/계약", 0.78),
        ("육아/반려동물", 0.88),
        ("이사/인테리어", 0.85),
        ("환경/건강", 0.91),
        ("스마트홈/가전", 0.89),
    ]

@pytest.fixture
def grpc_addr():
    """gRPC server address for testing."""
    return "localhost:50051"

# Validation helpers
def assert_valid_category(category: str):
    """Assert that category is valid."""
    valid_categories = [
        "요리/식품관리", "청소/세탁", "생활수리/DIY", "생활경제/계약",
        "육아/반려동물", "이사/인테리어", "환경/건강", "스마트홈/가전"
    ]
    assert category in valid_categories, f"Invalid category: {category}"

def assert_valid_confidence(confidence: float):
    """Assert that confidence score is valid."""
    assert 0.0 <= confidence <= 1.0, f"Invalid confidence: {confidence}"

def assert_valid_embedding(embedding: np.ndarray, expected_dim: int = 384):
    """Assert that embedding has correct properties."""
    assert isinstance(embedding, np.ndarray), "Embedding must be numpy array"
    assert embedding.shape[-1] == expected_dim, f"Expected dimension {expected_dim}, got {embedding.shape[-1]}"
    assert not np.isnan(embedding).any(), "Embedding contains NaN values"
    assert np.isfinite(embedding).all(), "Embedding contains infinite values"

# Pytest configuration
def pytest_configure(config):
    """Configure pytest markers."""
    config.addinivalue_line("markers", "unit: Unit tests")
    config.addinivalue_line("markers", "integration: Integration tests")
    config.addinivalue_line("markers", "e2e: End-to-end tests")
    config.addinivalue_line("markers", "grpc: gRPC related tests")
    config.addinivalue_line("markers", "slow: Slow running tests")
    config.addinivalue_line("markers", "requires_api: Tests requiring external API")
    config.addinivalue_line("markers", "requires_models: Tests requiring ML models")

# Test data factories
class TestDataFactory:
    """Factory for creating test data."""

    @staticmethod
    def create_question_data(category: str = "요리/식품관리") -> Dict[str, Any]:
        """Create sample question data."""
        return {
            "question_id": "test_123",
            "title": "테스트 질문입니다",
            "content": "이것은 테스트용 질문 내용입니다. 실제 질문을 시뮬레이션합니다.",
            "category": category,
            "tags": ["테스트", "샘플"],
            "created_at": 1640995200000,  # 2022-01-01T00:00:00Z
        }

    @staticmethod
    def create_embedding_data(dimension: int = 384) -> np.ndarray:
        """Create sample embedding data."""
        # Create reproducible embeddings for testing
        np.random.seed(42)
        embedding = np.random.rand(dimension).astype(np.float32)
        # Normalize for consistency
        return embedding / np.linalg.norm(embedding)

    @staticmethod
    def create_classification_result(category: str = "요리/식품관리", confidence: float = 0.95) -> tuple:
        """Create sample classification result."""
        return (category, confidence)

@pytest.fixture
def test_data_factory():
    """Provide test data factory."""
    return TestDataFactory

# Mock patches for common dependencies
@pytest.fixture(autouse=True)
def mock_external_dependencies(monkeypatch):
    """Automatically mock external dependencies for all tests."""
    # Mock environment variables
    monkeypatch.setenv("OPENAI_API_KEY", "test-key")
    monkeypatch.setenv("QDRANT_HOST", "localhost")
    monkeypatch.setenv("REDIS_HOST", "localhost")

    # This can be overridden in specific tests if needed
    return True

# Async fixtures for async tests
@pytest_asyncio.fixture
async def async_mock_redis_client():
    """Async Redis client mock."""
    client = AsyncMock()
    client.get.return_value = None
    client.set.return_value = True
    client.delete.return_value = 1
    client.exists.return_value = False
    client.ping.return_value = True
    return client

@pytest_asyncio.fixture
async def async_mock_openai_client():
    """Async OpenAI client mock."""
    client = AsyncMock()

    # Mock async embeddings
    mock_embedding = MagicMock()
    mock_embedding.data = [MagicMock(embedding=np.random.rand(384).tolist())]
    mock_embedding.usage = MagicMock(total_tokens=50)

    client.embeddings.create = AsyncMock(return_value=mock_embedding)

    # Mock async chat completions
    mock_completion = MagicMock()
    mock_completion.choices = [
        MagicMock(message=MagicMock(content="테스트 답변입니다."))
    ]
    mock_completion.usage = MagicMock(total_tokens=100)

    client.chat.completions.create = AsyncMock(return_value=mock_completion)

    return client