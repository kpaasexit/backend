from typing import Any, Dict, Optional

from app.config import get_settings
from app.core.exceptions import ModelLoadError
from app.core.constants import ModelType
from app.core.logger import LoggerSetup

from .cache import ModelCache
from .loaders import ModelLoader
from .memory import MemoryManager

logger = LoggerSetup.get_logger(__name__)


class ModelManager:
    """High-level model management interface."""

    def __init__(self):
        self.settings = get_settings()
        self.cache = ModelCache(max_size=self.settings.model.max_models_in_memory)
        self.loader = ModelLoader()
        self.memory_manager = MemoryManager()
        self._initialize_cache_dir()

    def _initialize_cache_dir(self) -> None:
        """Initialize model cache directory."""
        self.settings.model.model_cache_dir.mkdir(parents=True, exist_ok=True)

    def load_model(
        self,
        model_name: str,
        model_type: ModelType,
        use_onnx: bool = False,
        force_reload: bool = False
    ) -> Dict[str, Any]:
        """Load and cache a model."""
        # Check if model is already in cache
        if not force_reload:
            cached_model = self.cache.get(model_name)
            if cached_model:
                logger.debug(f"Model retrieved from cache: {model_name}")
                return cached_model

        # Check memory availability
        memory_stats = self.memory_manager.get_memory_usage()
        self.memory_manager.validate_memory_availability(memory_stats["rss_mb"])

        try:
            # Load the model
            model_data = self.loader.load_model(model_name, model_type, use_onnx)

            # Cache the model
            self.cache.put(model_name, model_data)

            logger.info(f"Model loaded and cached: {model_name}")
            return model_data

        except Exception as e:
            raise ModelLoadError(model_name=model_name, reason=str(e))

    def get_model(self, model_name: str) -> Optional[Dict[str, Any]]:
        """Get model from cache if available."""
        return self.cache.get(model_name)

    def unload_model(self, model_name: str) -> bool:
        """Unload a specific model from cache."""
        return self.cache.remove(model_name)

    def clear_cache(self) -> None:
        """Clear all models from cache."""
        self.cache.clear()

    def list_cached_models(self) -> Dict[str, Dict[str, Any]]:
        """List all models currently in cache."""
        return self.cache.list_cached_models()

    def get_memory_usage(self) -> Dict[str, float]:
        """Get current memory usage statistics."""
        return self.memory_manager.get_memory_usage()

    def optimize_memory(self) -> Dict[str, Any]:
        """Optimize memory usage and return statistics."""
        stats = self.memory_manager.optimize_memory()
        stats["models_cached"] = len(self.cache.cache)
        return stats

    def get_stats(self) -> Dict[str, Any]:
        """Get comprehensive manager statistics."""
        cache_stats = self.cache.get_cache_stats()
        memory_stats = self.memory_manager.get_memory_usage()

        return {
            **cache_stats,
            "memory_stats": memory_stats,
            "cache_dir": str(self.settings.model.model_cache_dir)
        }


# Global model manager instance
_model_manager: Optional[ModelManager] = None


def get_model_manager() -> ModelManager:
    global _model_manager
    if _model_manager is None:
        _model_manager = ModelManager()
    return _model_manager