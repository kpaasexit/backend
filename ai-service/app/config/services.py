"""External services configuration."""

from typing import Optional
from pydantic import Field
from .base import BaseConfig


class OpenAIConfig(BaseConfig):
    """OpenAI API configuration."""

    openai_api_key: Optional[str] = Field(default=None)
    openai_model: str = Field(default="gpt-4o-2024-08-06")
    openai_max_tokens: int = Field(default=800)
    openai_temperature: float = Field(default=0.7)
    openai_timeout: int = Field(default=30)


class EurekaConfig(BaseConfig):
    """Eureka service discovery configuration."""

    eureka_server_url: str = Field(default="http://localhost:8761/eureka")
    eureka_app_name: str = Field(default="NLP-SERVICE")
    eureka_instance_id: Optional[str] = Field(default=None)
    eureka_hostname: str = Field(default="localhost")
    eureka_health_check_interval: int = Field(default=30)
    eureka_renewal_interval: int = Field(default=30)
    eureka_duration: int = Field(default=90)
    eureka_enable: bool = Field(default=True)

