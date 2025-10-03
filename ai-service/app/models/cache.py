"""Model cache management."""

import gc
import threading
from typing import Dict, Any, Optional
from collections import OrderedDict

from app.core.logger import LoggerSetup
from .memory import MemoryManager


logger = LoggerSetup.get_logger(__name__)


class ModelCache:
    """LRU cache for loaded models."""

    def __init__(self, max_size: int):
        self.max_size = max_size
        self.cache: OrderedDict[str, Dict[str, Any]] = OrderedDict()
        self.lock = threading.Lock()
        self.memory_manager = MemoryManager()

    def get(self, model_name: str) -> Optional[Dict[str, Any]]:
        """Get model from cache, moving it to end (most recently used)."""
        with self.lock:
            if model_name not in self.cache:
                return None

            # Move to end (most recently used)
            self.cache.move_to_end(model_name)
            return self.cache[model_name]

    def put(self, model_name: str, model_data: Dict[str, Any]) -> None:
        """Add model to cache, evicting LRU if necessary."""
        with self.lock:
            # Remove if already exists
            if model_name in self.cache:
                self.cache.pop(model_name)

            # Evict LRU models if cache is full
            while len(self.cache) >= self.max_size:
                self._evict_lru_model()

            self.cache[model_name] = model_data

    def remove(self, model_name: str) -> bool:
        """Remove specific model from cache."""
        with self.lock:
            if model_name not in self.cache:
                return False

            model_data = self.cache.pop(model_name)
            self._cleanup_model_data(model_data)
            logger.info(f"Model removed from cache: {model_name}")
            return True

    def clear(self) -> None:
        """Clear all models from cache."""
        with self.lock:
            for model_name in list(self.cache.keys()):
                self.remove(model_name)
        logger.info("Model cache cleared")

    def list_cached_models(self) -> Dict[str, Dict[str, Any]]:
        """List all models currently in cache."""
        result = {}
        for model_name, model_data in self.cache.items():
            result[model_name] = {
                "type": model_data.get("type", "unknown"),
                "device": model_data.get("device", "unknown"),
                "has_model": "model" in model_data,
                "has_tokenizer": "tokenizer" in model_data
            }
        return result

    def _evict_lru_model(self) -> bool:
        """Evict the least recently used model."""
        if not self.cache:
            return False

        model_name, model_data = self.cache.popitem(last=False)
        self._cleanup_model_data(model_data)
        logger.info(f"Evicted LRU model: {model_name}")
        return True

    def _cleanup_model_data(self, model_data: Dict[str, Any]) -> None:
        """Clean up model data and free memory."""
        if "model" in model_data:
            del model_data["model"]
        if "tokenizer" in model_data:
            del model_data["tokenizer"]

        gc.collect()

    def get_cache_stats(self) -> Dict[str, Any]:
        """Get cache statistics."""
        memory_stats = self.memory_manager.get_memory_usage()
        return {
            "cached_models": len(self.cache),
            "max_size": self.max_size,
            "memory_usage_mb": memory_stats["rss_mb"],
            "memory_percent": memory_stats["percent"]
        }