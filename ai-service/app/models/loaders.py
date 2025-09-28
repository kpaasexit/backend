"""Model loading utilities."""

from typing import Dict, Any
import torch
from transformers import (
    AutoModel,
    AutoModelForSequenceClassification,
    AutoTokenizer,
)
from sentence_transformers import SentenceTransformer
from optimum.onnxruntime import ORTModelForSequenceClassification

from app.config import get_settings
from app.core.constants import ModelType
from app.core.logger import LoggerSetup


logger = LoggerSetup.get_logger(__name__)


class ModelLoader:
    """Handles loading different types of models."""

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
        device = "cuda" if torch.cuda.is_available() else "cpu"

        if model_type == ModelType.CLASSIFIER:
            return self._load_classifier(model_name, cache_dir, device, use_onnx)
        elif model_type == ModelType.EMBEDDER:
            return self._load_embedder(model_name, cache_dir, device)
        elif model_type == ModelType.TOKENIZER:
            return self._load_tokenizer(model_name, cache_dir)
        else:
            raise ValueError(f"Unsupported model type: {model_type}")

    def _load_classifier(
        self,
        model_name: str,
        cache_dir: str,
        device: str,
        use_onnx: bool
    ) -> Dict[str, Any]:
        """Load classification model."""
        logger.info(f"Loading classifier model: {model_name}")

        tokenizer = AutoTokenizer.from_pretrained(
            model_name,
            cache_dir=cache_dir,
            trust_remote_code=True
        )

        if use_onnx and self.settings.model.use_quantization:
            try:
                model = ORTModelForSequenceClassification.from_pretrained(
                    model_name,
                    cache_dir=cache_dir,
                    export=True,
                    provider="CPUExecutionProvider"
                )
                logger.info(f"Loaded ONNX classifier: {model_name}")
            except Exception as e:
                logger.warning(f"ONNX loading failed, falling back to PyTorch: {e}")
                model = self._load_pytorch_classifier(model_name, cache_dir, device)
        else:
            model = self._load_pytorch_classifier(model_name, cache_dir, device)

        model.eval()

        return {
            "model": model,
            "tokenizer": tokenizer,
            "type": ModelType.CLASSIFIER,
            "device": device
        }

    def _load_pytorch_classifier(self, model_name: str, cache_dir: str, device: str):
        """Load PyTorch classification model."""
        model = AutoModelForSequenceClassification.from_pretrained(
            model_name,
            cache_dir=cache_dir,
            torch_dtype=torch.float16 if device == "cuda" else torch.float32,
            trust_remote_code=True
        )
        if device == "cuda":
            model = model.to(device)
        return model

    def _load_embedder(
        self,
        model_name: str,
        cache_dir: str,
        device: str
    ) -> Dict[str, Any]:
        """Load embedding model."""
        logger.info(f"Loading embedder model: {model_name}")

        model = SentenceTransformer(
            model_name,
            cache_folder=cache_dir,
            device=device
        )

        return {
            "model": model,
            "tokenizer": None,
            "type": ModelType.EMBEDDER,
            "device": device
        }

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
            "model": None,
            "tokenizer": tokenizer,
            "type": ModelType.TOKENIZER,
            "device": "cpu"
        }