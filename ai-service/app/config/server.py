"""Server configuration."""

from pydantic import Field
from .base import BaseConfig


class ServerConfig(BaseConfig):
    """Server configuration settings."""

    service_name: str = Field(default="ai-service")
    host: str = Field(default="0.0.0.0")
    http_port: int = Field(default=8000)
    grpc_port: int = Field(default=50051)  # Deprecated - use specific ports below
    question_service_port: int = Field(default=50051)
    quiz_service_port: int = Field(default=50052)
    environment: str = Field(default="development")
    log_level: str = Field(default="INFO")