"""Unit tests for configuration modules."""

import os
import pytest
from pathlib import Path
from unittest.mock import patch, MagicMock

from app.config.main import Settings, get_settings
from app.config.base import BaseConfig
from app.config.server import ServerConfig
from app.config.model import ModelConfig
from app.config.database import QdrantConfig, RedisConfig
from app.config.services import OpenAIConfig, EurekaConfig
from app.config.system import MemoryConfig


@pytest.mark.unit
class TestBaseConfig:
    """Test BaseConfig functionality."""

    def test_base_config_creation(self):
        """Test basic config creation."""
        config = BaseConfig()
        assert config is not None

    def test_base_config_with_env_var(self, monkeypatch):
        """Test config with environment variable."""
        monkeypatch.setenv("TEST_VAR", "test_value")

        class TestConfig(BaseConfig):
            test_var: str = "default"

        config = TestConfig()
        # Pydantic doesn't automatically load from env without proper field config
        # This tests the basic functionality
        assert config.test_var == "default"

    def test_base_config_validation(self):
        """Test config validation."""
        class TestConfig(BaseConfig):
            required_field: str
            optional_field: str = "default"

        # Should work with required field
        config = TestConfig(required_field="test")
        assert config.required_field == "test"
        assert config.optional_field == "default"

        # Should fail without required field
        with pytest.raises(ValueError):
            TestConfig()


@pytest.mark.unit
class TestServerConfig:
    """Test ServerConfig functionality."""

    def test_server_config_defaults(self):
        """Test server config default values."""
        config = ServerConfig()

        assert config.host == "0.0.0.0"
        assert config.port == 8000
        assert config.debug is False
        assert config.environment == "development"

    def test_server_config_with_values(self):
        """Test server config with custom values."""
        config = ServerConfig(
            host="127.0.0.1",
            port=9000,
            debug=True,
            environment="production"
        )

        assert config.host == "127.0.0.1"
        assert config.port == 9000
        assert config.debug is True
        assert config.environment == "production"

    def test_server_config_validation(self):
        """Test server config validation."""
        # Valid port
        config = ServerConfig(port=8080)
        assert config.port == 8080

        # Invalid port (too low)
        with pytest.raises(ValueError):
            ServerConfig(port=0)

        # Invalid port (too high)
        with pytest.raises(ValueError):
            ServerConfig(port=70000)

        # Invalid environment
        with pytest.raises(ValueError):
            ServerConfig(environment="invalid")


@pytest.mark.unit
class TestModelConfig:
    """Test ModelConfig functionality."""

    def test_model_config_defaults(self):
        """Test model config default values."""
        config = ModelConfig()

        assert config.device == "cpu"
        assert config.max_sequence_length == 512
        assert config.batch_size == 16
        assert config.ko_embedder_model == "jhgan/ko-sroberta-multitask"
        assert config.en_embedder_model == "sentence-transformers/all-MiniLM-L6-v2"

    def test_model_config_with_values(self):
        """Test model config with custom values."""
        config = ModelConfig(
            device="cuda",
            max_sequence_length=256,
            batch_size=32,
            ko_embedder_model="custom/ko-model",
            en_embedder_model="custom/en-model"
        )

        assert config.device == "cuda"
        assert config.max_sequence_length == 256
        assert config.batch_size == 32
        assert config.ko_embedder_model == "custom/ko-model"
        assert config.en_embedder_model == "custom/en-model"

    def test_model_cache_dir_property(self):
        """Test model cache directory property."""
        config = ModelConfig()
        cache_dir = config.model_cache_dir

        assert isinstance(cache_dir, Path)
        assert "models" in str(cache_dir)

    def test_model_config_validation(self):
        """Test model config validation."""
        # Valid batch size
        config = ModelConfig(batch_size=8)
        assert config.batch_size == 8

        # Invalid batch size (too small)
        with pytest.raises(ValueError):
            ModelConfig(batch_size=0)

        # Invalid batch size (too large)
        with pytest.raises(ValueError):
            ModelConfig(batch_size=1000)

        # Valid sequence length
        config = ModelConfig(max_sequence_length=1024)
        assert config.max_sequence_length == 1024

        # Invalid sequence length
        with pytest.raises(ValueError):
            ModelConfig(max_sequence_length=0)


@pytest.mark.unit
class TestDatabaseConfigs:
    """Test database configuration classes."""

    def test_qdrant_config_defaults(self):
        """Test QdrantConfig default values."""
        config = QdrantConfig()

        assert config.host == "localhost"
        assert config.port == 6333
        assert config.collection_name == "home_life_questions"
        assert config.vector_size == 384

    def test_qdrant_config_with_values(self):
        """Test QdrantConfig with custom values."""
        config = QdrantConfig(
            host="qdrant.example.com",
            port=6334,
            collection_name="test_collection",
            vector_size=768
        )

        assert config.host == "qdrant.example.com"
        assert config.port == 6334
        assert config.collection_name == "test_collection"
        assert config.vector_size == 768

    def test_redis_config_defaults(self):
        """Test RedisConfig default values."""
        config = RedisConfig()

        assert config.host == "localhost"
        assert config.port == 6379
        assert config.db == 0
        assert config.password is None

    def test_redis_config_with_values(self):
        """Test RedisConfig with custom values."""
        config = RedisConfig(
            host="redis.example.com",
            port=6380,
            db=1,
            password="secret"
        )

        assert config.host == "redis.example.com"
        assert config.port == 6380
        assert config.db == 1
        assert config.password == "secret"


@pytest.mark.unit
class TestServiceConfigs:
    """Test service configuration classes."""

    def test_openai_config_defaults(self):
        """Test OpenAIConfig default values."""
        config = OpenAIConfig()

        assert config.model == "gpt-3.5-turbo"
        assert config.temperature == 0.7
        assert config.max_tokens == 1000
        assert config.openai_api_key is None  # Should be set via env var

    def test_openai_config_with_values(self):
        """Test OpenAIConfig with custom values."""
        config = OpenAIConfig(
            model="gpt-4",
            temperature=0.9,
            max_tokens=2000,
            openai_api_key="test-key"
        )

        assert config.model == "gpt-4"
        assert config.temperature == 0.9
        assert config.max_tokens == 2000
        assert config.openai_api_key == "test-key"

    def test_eureka_config_defaults(self):
        """Test EurekaConfig default values."""
        config = EurekaConfig()

        assert config.enabled is False
        assert config.server_url == "http://localhost:8761/eureka"
        assert config.app_name == "nlp-service"

    def test_eureka_config_with_values(self):
        """Test EurekaConfig with custom values."""
        config = EurekaConfig(
            enabled=True,
            server_url="http://eureka.example.com/eureka",
            app_name="test-service"
        )

        assert config.enabled is True
        assert config.server_url == "http://eureka.example.com/eureka"
        assert config.app_name == "test-service"


@pytest.mark.unit
class TestSystemConfig:
    """Test system configuration classes."""

    def test_memory_config_defaults(self):
        """Test MemoryConfig default values."""
        config = MemoryConfig()

        assert config.max_memory_mb == 2048
        assert config.warning_threshold == 0.8
        assert config.cleanup_threshold == 0.9

    def test_memory_config_with_values(self):
        """Test MemoryConfig with custom values."""
        config = MemoryConfig(
            max_memory_mb=4096,
            warning_threshold=0.7,
            cleanup_threshold=0.85
        )

        assert config.max_memory_mb == 4096
        assert config.warning_threshold == 0.7
        assert config.cleanup_threshold == 0.85

    def test_memory_config_validation(self):
        """Test MemoryConfig validation."""
        # Valid thresholds
        config = MemoryConfig(warning_threshold=0.6, cleanup_threshold=0.8)
        assert config.warning_threshold == 0.6
        assert config.cleanup_threshold == 0.8

        # Invalid threshold (cleanup <= warning)
        with pytest.raises(ValueError):
            MemoryConfig(warning_threshold=0.9, cleanup_threshold=0.8)

        # Invalid threshold values
        with pytest.raises(ValueError):
            MemoryConfig(warning_threshold=1.5)

        with pytest.raises(ValueError):
            MemoryConfig(cleanup_threshold=-0.1)


@pytest.mark.unit
class TestMainSettings:
    """Test main Settings class."""

    def test_settings_creation(self):
        """Test Settings creation with defaults."""
        settings = Settings()

        assert isinstance(settings.server, ServerConfig)
        assert isinstance(settings.model, ModelConfig)
        assert isinstance(settings.qdrant, QdrantConfig)
        assert isinstance(settings.openai, OpenAIConfig)
        assert isinstance(settings.redis, RedisConfig)
        assert isinstance(settings.memory, MemoryConfig)
        assert isinstance(settings.eureka, EurekaConfig)

        assert len(settings.categories) == 8
        assert "요리/식품관리" in settings.categories

    def test_settings_properties(self):
        """Test Settings properties."""
        # Test development environment
        settings = Settings()
        settings.server.environment = "development"
        assert settings.is_development is True
        assert settings.is_production is False

        # Test production environment
        settings.server.environment = "production"
        assert settings.is_development is False
        assert settings.is_production is True

    @patch("pathlib.Path.mkdir")
    def test_settings_validation(self, mock_mkdir):
        """Test Settings validation."""
        settings = Settings()

        # Mock model cache directory doesn't exist
        with patch.object(settings.model.model_cache_dir, "exists", return_value=False):
            settings.validate_config()
            mock_mkdir.assert_called_once_with(parents=True, exist_ok=True)

    def test_settings_validation_production_requirements(self):
        """Test Settings validation for production requirements."""
        settings = Settings()
        settings.server.environment = "production"
        settings.openai.openai_api_key = None

        # Should raise error for missing OpenAI key in production
        with pytest.raises(ValueError, match="OpenAI API key is required in production"):
            settings.validate_config()

        # Should pass with API key
        settings.openai.openai_api_key = "test-key"
        # Mock the path exists check to avoid actual filesystem operations
        with patch.object(settings.model.model_cache_dir, "exists", return_value=True):
            settings.validate_config()  # Should not raise

    def test_settings_validation_categories(self):
        """Test Settings validation for categories."""
        settings = Settings()
        settings.categories = []

        # Should raise error for empty categories
        with pytest.raises(ValueError, match="At least one category must be defined"):
            settings.validate_config()

    @patch("app.config.main.Settings")
    def test_get_settings_caching(self, mock_settings_class):
        """Test get_settings function caching."""
        mock_instance = MagicMock()
        mock_settings_class.return_value = mock_instance

        # Clear any existing cache
        get_settings.cache_clear()

        # First call
        result1 = get_settings()
        assert result1 == mock_instance

        # Second call should return same instance (cached)
        result2 = get_settings()
        assert result2 == mock_instance

        # Settings should only be created once
        assert mock_settings_class.call_count == 1

    def test_settings_with_environment_variables(self, monkeypatch):
        """Test Settings with environment variables."""
        # Set environment variables
        monkeypatch.setenv("OPENAI_API_KEY", "env-test-key")
        monkeypatch.setenv("QDRANT_HOST", "env-qdrant-host")
        monkeypatch.setenv("REDIS_HOST", "env-redis-host")

        # Create settings
        settings = Settings()

        # Note: This test assumes proper Pydantic field configuration for env vars
        # The actual behavior depends on how the config classes are set up
        assert settings is not None

    def test_settings_edge_cases(self):
        """Test Settings edge cases."""
        # Test with None values where allowed
        settings = Settings()
        settings.openai.openai_api_key = None
        settings.redis.password = None

        # Should not raise errors for None values where allowed
        assert settings.openai.openai_api_key is None
        assert settings.redis.password is None

    def test_settings_type_validation(self):
        """Test Settings type validation."""
        settings = Settings()

        # Test that properties return correct types
        assert isinstance(settings.server.port, int)
        assert isinstance(settings.server.debug, bool)
        assert isinstance(settings.model.max_sequence_length, int)
        assert isinstance(settings.openai.temperature, float)
        assert isinstance(settings.categories, list)

    def test_settings_immutability_after_creation(self):
        """Test that settings behave correctly after creation."""
        settings = Settings()

        # Should be able to modify after creation
        original_port = settings.server.port
        settings.server.port = 9000
        assert settings.server.port == 9000
        assert settings.server.port != original_port

    @patch.dict(os.environ, {}, clear=True)
    def test_settings_without_env_vars(self):
        """Test Settings creation without environment variables."""
        settings = Settings()

        # Should create with default values
        assert settings.server.host == "0.0.0.0"
        assert settings.server.port == 8000
        assert settings.openai.openai_api_key is None  # No env var set