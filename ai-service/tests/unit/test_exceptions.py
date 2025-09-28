"""Unit tests for custom exception classes."""

import pytest

from app.core.exceptions import (
    NLPServiceError,
    ModelLoadError,
    ModelNotFoundError,
    MemoryLimitExceeded,
    InvalidInputError,
    VectorDBError,
    GPTServiceError,
    CacheError,
    ClassificationError,
    EmbeddingError,
    GRPCError,
    ConfigurationError
)


@pytest.mark.unit
class TestNLPServiceError:
    """Test base NLPServiceError class."""

    def test_basic_creation(self):
        """Test basic exception creation."""
        error = NLPServiceError("Test error message")
        assert str(error) == "Test error message"
        assert error.message == "Test error message"
        assert error.error_code is None
        assert error.details == {}

    def test_creation_with_error_code(self):
        """Test exception creation with error code."""
        error = NLPServiceError("Test error", error_code="TEST_ERROR")
        assert error.error_code == "TEST_ERROR"

    def test_creation_with_details(self):
        """Test exception creation with details."""
        details = {"param1": "value1", "param2": 123}
        error = NLPServiceError("Test error", details=details)
        assert error.details == details

    def test_to_dict_method(self):
        """Test to_dict method."""
        error = NLPServiceError(
            "Test error",
            error_code="TEST_ERROR",
            details={"key": "value"}
        )

        result = error.to_dict()
        expected = {
            "error": "NLPServiceError",
            "message": "Test error",
            "error_code": "TEST_ERROR",
            "details": {"key": "value"}
        }
        assert result == expected

    def test_inheritance_from_exception(self):
        """Test that NLPServiceError inherits from Exception."""
        error = NLPServiceError("Test")
        assert isinstance(error, Exception)

    def test_empty_details_default(self):
        """Test that details defaults to empty dict."""
        error = NLPServiceError("Test", details=None)
        assert error.details == {}


@pytest.mark.unit
class TestModelLoadError:
    """Test ModelLoadError class."""

    def test_model_load_error_creation(self):
        """Test ModelLoadError creation."""
        error = ModelLoadError("test-model", "File not found")
        assert "Failed to load model 'test-model': File not found" in str(error)
        assert error.error_code == "MODEL_LOAD_ERROR"

    def test_model_load_error_details(self):
        """Test ModelLoadError details."""
        error = ModelLoadError("test-model", "Connection timeout")
        assert error.details["model_name"] == "test-model"
        assert error.details["reason"] == "Connection timeout"

    def test_model_load_error_inheritance(self):
        """Test ModelLoadError inherits from NLPServiceError."""
        error = ModelLoadError("model", "reason")
        assert isinstance(error, NLPServiceError)

    def test_model_load_error_to_dict(self):
        """Test ModelLoadError to_dict method."""
        error = ModelLoadError("my-model", "disk full")
        result = error.to_dict()
        assert result["error"] == "ModelLoadError"
        assert "my-model" in result["message"]
        assert result["error_code"] == "MODEL_LOAD_ERROR"


@pytest.mark.unit
class TestModelNotFoundError:
    """Test ModelNotFoundError class."""

    def test_model_not_found_error_creation(self):
        """Test ModelNotFoundError creation."""
        error = ModelNotFoundError("missing-model")
        assert "Model 'missing-model' not found" in str(error)
        assert error.error_code == "MODEL_NOT_FOUND"

    def test_model_not_found_error_details(self):
        """Test ModelNotFoundError details."""
        error = ModelNotFoundError("missing-model")
        assert error.details["model_name"] == "missing-model"

    def test_model_not_found_error_inheritance(self):
        """Test ModelNotFoundError inherits correctly."""
        error = ModelNotFoundError("model")
        assert isinstance(error, NLPServiceError)


@pytest.mark.unit
class TestMemoryLimitExceeded:
    """Test MemoryLimitExceeded class."""

    def test_memory_limit_exceeded_creation(self):
        """Test MemoryLimitExceeded creation."""
        error = MemoryLimitExceeded(1024.5, 2048.0)
        assert "Memory limit exceeded: 1024.5MB / 2048.0MB" in str(error)
        assert error.error_code == "MEMORY_LIMIT_EXCEEDED"

    def test_memory_limit_exceeded_details(self):
        """Test MemoryLimitExceeded details."""
        error = MemoryLimitExceeded(1500.0, 2000.0)
        assert error.details["current_usage"] == 1500.0
        assert error.details["limit"] == 2000.0

    def test_memory_limit_exceeded_with_floats(self):
        """Test MemoryLimitExceeded with float values."""
        error = MemoryLimitExceeded(999.99, 1000.01)
        assert isinstance(error.details["current_usage"], float)
        assert isinstance(error.details["limit"], float)


@pytest.mark.unit
class TestInvalidInputError:
    """Test InvalidInputError class."""

    def test_invalid_input_error_creation(self):
        """Test InvalidInputError creation."""
        error = InvalidInputError("username", "cannot be empty")
        assert "Invalid input for field 'username': cannot be empty" in str(error)
        assert error.error_code == "INVALID_INPUT"

    def test_invalid_input_error_details(self):
        """Test InvalidInputError details."""
        error = InvalidInputError("email", "invalid format")
        assert error.details["field"] == "email"
        assert error.details["reason"] == "invalid format"

    def test_invalid_input_error_various_fields(self):
        """Test InvalidInputError with various field names."""
        fields = ["text", "model_name", "confidence", "embedding_dim"]
        for field in fields:
            error = InvalidInputError(field, "test reason")
            assert field in str(error)
            assert error.details["field"] == field


@pytest.mark.unit
class TestVectorDBError:
    """Test VectorDBError class."""

    def test_vector_db_error_creation(self):
        """Test VectorDBError creation."""
        error = VectorDBError("search", "connection failed")
        assert "Vector DB operation 'search' failed: connection failed" in str(error)
        assert error.error_code == "VECTOR_DB_ERROR"

    def test_vector_db_error_details(self):
        """Test VectorDBError details."""
        error = VectorDBError("upsert", "timeout")
        assert error.details["operation"] == "upsert"
        assert error.details["reason"] == "timeout"

    def test_vector_db_error_operations(self):
        """Test VectorDBError with different operations."""
        operations = ["search", "upsert", "delete", "create_collection"]
        for operation in operations:
            error = VectorDBError(operation, "test error")
            assert operation in str(error)


@pytest.mark.unit
class TestGPTServiceError:
    """Test GPTServiceError class."""

    def test_gpt_service_error_creation(self):
        """Test GPTServiceError creation."""
        error = GPTServiceError("API rate limit exceeded")
        assert "GPT service error: API rate limit exceeded" in str(error)
        assert error.error_code == "GPT_SERVICE_ERROR"

    def test_gpt_service_error_with_status_code(self):
        """Test GPTServiceError with status code."""
        error = GPTServiceError("Authentication failed", status_code=401)
        assert error.details["reason"] == "Authentication failed"
        assert error.details["status_code"] == 401

    def test_gpt_service_error_without_status_code(self):
        """Test GPTServiceError without status code."""
        error = GPTServiceError("Network error")
        assert error.details["status_code"] is None


@pytest.mark.unit
class TestCacheError:
    """Test CacheError class."""

    def test_cache_error_creation(self):
        """Test CacheError creation."""
        error = CacheError("get", "Redis connection lost")
        assert "Cache operation 'get' failed: Redis connection lost" in str(error)
        assert error.error_code == "CACHE_ERROR"

    def test_cache_error_details(self):
        """Test CacheError details."""
        error = CacheError("set", "memory full")
        assert error.details["operation"] == "set"
        assert error.details["reason"] == "memory full"

    def test_cache_error_operations(self):
        """Test CacheError with different cache operations."""
        operations = ["get", "set", "delete", "clear", "exists"]
        for operation in operations:
            error = CacheError(operation, "test error")
            assert operation in str(error)


@pytest.mark.unit
class TestClassificationError:
    """Test ClassificationError class."""

    def test_classification_error_creation(self):
        """Test ClassificationError creation."""
        error = ClassificationError("test text", "model not loaded")
        assert "Classification failed: model not loaded" in str(error)
        assert error.error_code == "CLASSIFICATION_ERROR"

    def test_classification_error_details(self):
        """Test ClassificationError details."""
        long_text = "a" * 200
        error = ClassificationError(long_text, "invalid input")
        assert error.details["text_sample"] == "a" * 100  # Truncated
        assert error.details["reason"] == "invalid input"

    def test_classification_error_empty_text(self):
        """Test ClassificationError with empty text."""
        error = ClassificationError("", "empty input")
        assert error.details["text_sample"] == ""

    def test_classification_error_none_text(self):
        """Test ClassificationError with None text."""
        error = ClassificationError(None, "null input")
        assert error.details["text_sample"] == ""

    def test_classification_error_short_text(self):
        """Test ClassificationError with short text."""
        short_text = "short"
        error = ClassificationError(short_text, "too short")
        assert error.details["text_sample"] == short_text


@pytest.mark.unit
class TestEmbeddingError:
    """Test EmbeddingError class."""

    def test_embedding_error_creation(self):
        """Test EmbeddingError creation."""
        error = EmbeddingError("dimension mismatch")
        assert "Embedding generation failed: dimension mismatch" in str(error)
        assert error.error_code == "EMBEDDING_ERROR"

    def test_embedding_error_details(self):
        """Test EmbeddingError details."""
        error = EmbeddingError("tensor shape error")
        assert error.details["reason"] == "tensor shape error"

    def test_embedding_error_various_reasons(self):
        """Test EmbeddingError with various reasons."""
        reasons = [
            "model not initialized",
            "invalid input shape",
            "CUDA out of memory",
            "tokenization failed"
        ]
        for reason in reasons:
            error = EmbeddingError(reason)
            assert reason in str(error)


@pytest.mark.unit
class TestGRPCError:
    """Test GRPCError class."""

    def test_grpc_error_creation(self):
        """Test GRPCError creation."""
        error = GRPCError("ClassifyText", "timeout exceeded")
        assert "gRPC method 'ClassifyText' failed: timeout exceeded" in str(error)
        assert error.error_code == "GRPC_ERROR"

    def test_grpc_error_details(self):
        """Test GRPCError details."""
        error = GRPCError("GetEmbedding", "server unavailable", "UNAVAILABLE")
        assert error.details["method"] == "GetEmbedding"
        assert error.details["reason"] == "server unavailable"
        assert error.details["status_code"] == "UNAVAILABLE"

    def test_grpc_error_without_status_code(self):
        """Test GRPCError without status code."""
        error = GRPCError("ProcessQuestion", "internal error")
        assert error.details["status_code"] is None

    def test_grpc_error_methods(self):
        """Test GRPCError with different gRPC methods."""
        methods = [
            "ClassifyCategory",
            "GetEmbedding",
            "GenerateAnswer",
            "HealthCheck"
        ]
        for method in methods:
            error = GRPCError(method, "test error")
            assert method in str(error)


@pytest.mark.unit
class TestConfigurationError:
    """Test ConfigurationError class."""

    def test_configuration_error_creation(self):
        """Test ConfigurationError creation."""
        error = ConfigurationError("database", "connection string missing")
        assert "Configuration error for 'database': connection string missing" in str(error)
        assert error.error_code == "CONFIGURATION_ERROR"

    def test_configuration_error_details(self):
        """Test ConfigurationError details."""
        error = ConfigurationError("openai", "API key not set")
        assert error.details["config_name"] == "openai"
        assert error.details["reason"] == "API key not set"

    def test_configuration_error_various_configs(self):
        """Test ConfigurationError with various config names."""
        configs = ["server", "model", "redis", "qdrant", "logging"]
        for config in configs:
            error = ConfigurationError(config, "invalid setting")
            assert config in str(error)


@pytest.mark.unit
class TestExceptionChaining:
    """Test exception chaining and raising."""

    def test_raise_and_catch_nlp_service_error(self):
        """Test raising and catching NLPServiceError."""
        with pytest.raises(NLPServiceError) as exc_info:
            raise NLPServiceError("Test error")

        assert str(exc_info.value) == "Test error"

    def test_raise_and_catch_specific_errors(self):
        """Test raising and catching specific error types."""
        errors = [
            (ModelLoadError, ("model", "reason")),
            (ModelNotFoundError, ("model",)),
            (MemoryLimitExceeded, (1000.0, 2000.0)),
            (InvalidInputError, ("field", "reason")),
            (VectorDBError, ("operation", "reason")),
            (GPTServiceError, ("reason",)),
            (CacheError, ("operation", "reason")),
            (ClassificationError, ("text", "reason")),
            (EmbeddingError, ("reason",)),
            (GRPCError, ("method", "reason")),
            (ConfigurationError, ("config", "reason"))
        ]

        for error_class, args in errors:
            with pytest.raises(error_class):
                raise error_class(*args)

    def test_catch_as_base_exception(self):
        """Test catching specific errors as base NLPServiceError."""
        with pytest.raises(NLPServiceError):
            raise ModelLoadError("model", "error")

        with pytest.raises(NLPServiceError):
            raise ClassificationError("text", "error")

        with pytest.raises(NLPServiceError):
            raise EmbeddingError("error")

    def test_exception_chaining(self):
        """Test exception chaining with 'from' clause."""
        original_error = ValueError("Original error")

        with pytest.raises(NLPServiceError) as exc_info:
            try:
                raise original_error
            except ValueError as e:
                raise NLPServiceError("Wrapper error") from e

        # Check that the original exception is chained
        assert exc_info.value.__cause__ is original_error


@pytest.mark.unit
class TestExceptionEquality:
    """Test exception equality and comparison."""

    def test_same_exception_equality(self):
        """Test that same exceptions with same parameters are equal in content."""
        error1 = NLPServiceError("Test", error_code="CODE", details={"key": "value"})
        error2 = NLPServiceError("Test", error_code="CODE", details={"key": "value"})

        # Note: Exception instances are not equal by default, but content should match
        assert error1.message == error2.message
        assert error1.error_code == error2.error_code
        assert error1.details == error2.details

    def test_different_exception_inequality(self):
        """Test that different exceptions have different content."""
        error1 = NLPServiceError("Test1")
        error2 = NLPServiceError("Test2")

        assert error1.message != error2.message

    def test_to_dict_consistency(self):
        """Test that to_dict method is consistent."""
        error = ModelLoadError("model", "reason")
        dict1 = error.to_dict()
        dict2 = error.to_dict()

        assert dict1 == dict2