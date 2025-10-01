from typing import Any, Dict, Optional

class NLPServiceError(Exception):
    def __init__(
        self,
        message: str,
        error_code: Optional[str] = None,
        details: Optional[Dict[str, Any]] = None
    ):
        self.message = message
        self.error_code = error_code
        self.details = details or {}
        super().__init__(self.message)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "error": self.__class__.__name__,
            "message": self.message,
            "error_code": self.error_code,
            "details": self.details
        }


class ModelLoadError(NLPServiceError):
    def __init__(self, model_name: str, reason: str):
        super().__init__(
            message=f"Failed to load model '{model_name}': {reason}",
            error_code="MODEL_LOAD_ERROR",
            details={"model_name": model_name, "reason": reason}
        )


class ModelNotFoundError(NLPServiceError):
    def __init__(self, model_name: str):
        super().__init__(
            message=f"Model '{model_name}' not found",
            error_code="MODEL_NOT_FOUND",
            details={"model_name": model_name}
        )


class MemoryLimitExceeded(NLPServiceError):
    def __init__(self, current_usage: float, limit: float):
        super().__init__(
            message=f"Memory limit exceeded: {current_usage}MB / {limit}MB",
            error_code="MEMORY_LIMIT_EXCEEDED",
            details={"current_usage": current_usage, "limit": limit}
        )


class InvalidInputError(NLPServiceError):
    def __init__(self, field: str, reason: str):
        super().__init__(
            message=f"Invalid input for field '{field}': {reason}",
            error_code="INVALID_INPUT",
            details={"field": field, "reason": reason}
        )


class VectorDBError(NLPServiceError):
    def __init__(self, operation: str, reason: str):
        super().__init__(
            message=f"Vector DB operation '{operation}' failed: {reason}",
            error_code="VECTOR_DB_ERROR",
            details={"operation": operation, "reason": reason}
        )


class GPTServiceError(NLPServiceError):
    def __init__(self, reason: str, status_code: Optional[int] = None):
        super().__init__(
            message=f"GPT service error: {reason}",
            error_code="GPT_SERVICE_ERROR",
            details={"reason": reason, "status_code": status_code}
        )


class CacheError(NLPServiceError):
    def __init__(self, operation: str, reason: str):
        super().__init__(
            message=f"Cache operation '{operation}' failed: {reason}",
            error_code="CACHE_ERROR",
            details={"operation": operation, "reason": reason}
        )


class ClassificationError(NLPServiceError):
    def __init__(self, text: str, reason: str):
        super().__init__(
            message=f"Classification failed: {reason}",
            error_code="CLASSIFICATION_ERROR",
            details={"text_sample": text[:100] if text else "", "reason": reason}
        )


class EmbeddingError(NLPServiceError):
    def __init__(self, reason: str):
        super().__init__(
            message=f"Embedding generation failed: {reason}",
            error_code="EMBEDDING_ERROR",
            details={"reason": reason}
        )


class GRPCError(NLPServiceError):
    def __init__(self, method: str, reason: str, status_code: Optional[str] = None):
        super().__init__(
            message=f"gRPC method '{method}' failed: {reason}",
            error_code="GRPC_ERROR",
            details={"method": method, "reason": reason, "status_code": status_code}
        )


class ConfigurationError(NLPServiceError):
    def __init__(self, config_name: str, reason: str):
        super().__init__(
            message=f"Configuration error for '{config_name}': {reason}",
            error_code="CONFIGURATION_ERROR",
            details={"config_name": config_name, "reason": reason}
        )