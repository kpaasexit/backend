"""Model configuration."""

from pathlib import Path
from pydantic import Field
from .base import BaseConfig


class ModelConfig(BaseConfig):
    """Model configuration settings."""

    model_cache_dir: Path = Field(default=Path("./model_cache"))
    max_models_in_memory: int = Field(default=2)
    model_load_timeout: int = Field(default=30)
    use_quantization: bool = Field(default=True)
    onnx_optimization_level: str = Field(default="ALL")

    # Model specifications
    ko_classifier_model: str = Field(default="klue/roberta-small")
    en_classifier_model: str = Field(default="distilbert-base-uncased")
    ko_embedder_model: str = Field(default="jhgan/ko-sroberta-multitask")
    en_embedder_model: str = Field(default="sentence-transformers/all-MiniLM-L6-v2")

    # Processing parameters
    max_sequence_length: int = Field(default=128)
    classifier_max_length: int = Field(default=64)
    batch_size: int = Field(default=4)
    num_threads: int = Field(default=2)