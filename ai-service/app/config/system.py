"""System configuration."""

from pydantic import Field
from .base import BaseConfig


class MemoryConfig(BaseConfig):
    """Memory management configuration."""

    max_memory_mb: int = Field(default=3500)
    memory_check_interval: int = Field(default=60)
    gc_threshold: int = Field(default=80)