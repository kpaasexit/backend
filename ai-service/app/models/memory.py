"""Memory management for models."""

import gc
from typing import Dict
import psutil

from app.config import get_settings
from app.core.logger import LoggerSetup
from app.core.exceptions import MemoryLimitExceeded


logger = LoggerSetup.get_logger(__name__)


class MemoryManager:
    """Handles memory monitoring and management for models."""

    def __init__(self):
        self.settings = get_settings()

    def get_memory_usage(self) -> Dict[str, float]:
        """Get current memory usage statistics."""
        process = psutil.Process()
        memory_info = process.memory_info()

        return {
            "rss_mb": memory_info.rss / 1024 / 1024,
            "vms_mb": memory_info.vms / 1024 / 1024,
            "percent": process.memory_percent(),
            "available_mb": psutil.virtual_memory().available / 1024 / 1024
        }

    def check_memory_threshold(self) -> bool:
        """Check if memory usage is below threshold."""
        memory_stats = self.get_memory_usage()
        used_percent = memory_stats["percent"]

        if used_percent >= self.settings.memory.gc_threshold:
            return False
        return True

    def validate_memory_availability(self, current_usage_mb: float) -> None:
        """Validate that memory is available for new models."""
        if not self.check_memory_threshold():
            raise MemoryLimitExceeded(
                current_usage=current_usage_mb,
                limit=self.settings.memory.max_memory_mb
            )

    def optimize_memory(self) -> Dict[str, float]:
        """Force memory cleanup and return before/after stats."""
        before = self.get_memory_usage()

        # Force garbage collection
        gc.collect()

        after = self.get_memory_usage()

        return {
            "before_mb": before["rss_mb"],
            "after_mb": after["rss_mb"],
            "freed_mb": before["rss_mb"] - after["rss_mb"]
        }