"""Model loading utilities."""

from typing import Dict, Any
from transformers import AutoTokenizer

from app.config import get_settings
from app.core.constants import ModelType
from app.core.logger import LoggerSetup


logger = LoggerSetup.get_logger(__name__)


class ModelLoader:
    """Handles loading different types of models (tokenizers only)."""

    def __init__(self):
        self.settings = get_settings()

    def load_model(
        self,
        model_name: str,
        model_type: ModelType,
        use_onnx: bool = False
    ) -> Dict[str, Any]:
        """Load a model based on its type."""
        cache_dir = str(self.settings.model.model_cache_dir)

        if model_type == ModelType.TOKENIZER:
            return self._load_tokenizer(model_name, cache_dir)
        else:
            raise ValueError(f"Unsupported model type: {model_type}")

    def _load_tokenizer(
        self,
        model_name: str,
        cache_dir: str
    ) -> Dict[str, Any]:
        """Load tokenizer only."""
        logger.info(f"Loading tokenizer: {model_name}")

        tokenizer = AutoTokenizer.from_pretrained(
            model_name,
            cache_dir=cache_dir,
            trust_remote_code=True
        )

        return {
            "model": tokenizer,
            "tokenizer": tokenizer,
            "type": ModelType.TOKENIZER,
            "device": "cpu"
        }
