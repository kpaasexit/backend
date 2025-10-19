"""Main settings configuration."""

from typing import List
from functools import lru_cache
from pydantic import Field

from .base import BaseConfig
from .server import ServerConfig
from .model import ModelConfig
from .database import QdrantConfig, RedisConfig, MySQLConfig
from .services import OpenAIConfig, EurekaConfig
from .system import MemoryConfig
from .scheduler import QuizSchedulerConfig


class Settings(BaseConfig):
    """Main application settings."""

    server: ServerConfig = Field(default_factory=ServerConfig)
    model: ModelConfig = Field(default_factory=ModelConfig)
    qdrant: QdrantConfig = Field(default_factory=QdrantConfig)
    openai: OpenAIConfig = Field(default_factory=OpenAIConfig)
    redis: RedisConfig = Field(default_factory=RedisConfig)
    mysql: MySQLConfig = Field(default_factory=MySQLConfig)
    memory: MemoryConfig = Field(default_factory=MemoryConfig)
    eureka: EurekaConfig = Field(default_factory=EurekaConfig)
    quiz_scheduler: QuizSchedulerConfig = Field(default_factory=QuizSchedulerConfig)

    categories: List[str] = Field(
        default=[
            "요리/식품관리",
            "청소/세탁",
            "생활수리/DIY",
            "생활경제/계약",
            "이사/인테리어",
            "육아/반려동물",
            "환경/건강",
            "스마트홈/가전"
        ]
    )

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.server = ServerConfig()
        self.model = ModelConfig()
        self.qdrant = QdrantConfig()
        self.openai = OpenAIConfig()
        self.redis = RedisConfig()
        self.mysql = MySQLConfig()
        self.memory = MemoryConfig()
        self.eureka = EurekaConfig()
        self.quiz_scheduler = QuizSchedulerConfig()

    @property
    def is_production(self) -> bool:
        return self.server.environment == "production"

    @property
    def is_development(self) -> bool:
        return self.server.environment == "development"

    def validate_config(self) -> None:
        if not self.model.model_cache_dir.exists():
            self.model.model_cache_dir.mkdir(parents=True, exist_ok=True)

        if self.is_production and not self.openai.openai_api_key:
            raise ValueError("OpenAI API key is required in production")

        if not self.categories:
            raise ValueError("At least one category must be defined")


@lru_cache()
def get_settings() -> Settings:
    settings = Settings()
    settings.validate_config()
    return settings