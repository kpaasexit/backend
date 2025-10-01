"""Memory management utilities."""

import gc
import threading
import time
from typing import Dict, Any, Optional, Callable

import psutil
import torch

from app.config import get_settings
from app.core.logger import LoggerSetup


logger = LoggerSetup.get_logger(__name__)


class MemoryMonitor:
    """Monitor and manage memory usage."""

    def __init__(self):
        self.settings = get_settings()
        self._monitoring = False
        self._monitor_thread: Optional[threading.Thread] = None
        self._callbacks = []

    def get_memory_stats(self) -> Dict[str, Any]:
        """
        Get current memory statistics.

        Returns:
            Dictionary with memory statistics
        """
        process = psutil.Process()
        memory_info = process.memory_info()
        virtual_memory = psutil.virtual_memory()

        stats = {
            "process": {
                "rss_mb": memory_info.rss / 1024 / 1024,
                "vms_mb": memory_info.vms / 1024 / 1024,
                "percent": process.memory_percent(),
                "available_mb": virtual_memory.available / 1024 / 1024
            },
            "system": {
                "total_mb": virtual_memory.total / 1024 / 1024,
                "available_mb": virtual_memory.available / 1024 / 1024,
                "percent": virtual_memory.percent,
                "used_mb": virtual_memory.used / 1024 / 1024,
                "free_mb": virtual_memory.free / 1024 / 1024
            }
        }

        # Add GPU memory if available
        if torch.cuda.is_available():
            try:
                stats["gpu"] = {
                    "allocated_mb": torch.cuda.memory_allocated() / 1024 / 1024,
                    "reserved_mb": torch.cuda.memory_reserved() / 1024 / 1024,
                    "max_memory_mb": torch.cuda.max_memory_allocated() / 1024 / 1024
                }
            except Exception as e:
                logger.warning(f"Failed to get GPU memory stats: {e}")

        return stats

    def check_memory_threshold(self) -> bool:
        """
        Check if memory usage is below threshold.

        Returns:
            True if memory is within limits, False otherwise
        """
        stats = self.get_memory_stats()
        current_usage_mb = stats["process"]["rss_mb"]
        usage_percent = stats["process"]["percent"]

        # Check absolute limit
        if current_usage_mb > self.settings.memory.max_memory_mb:
            logger.warning(
                f"Memory usage exceeds limit: {current_usage_mb:.1f}MB / "
                f"{self.settings.memory.max_memory_mb}MB"
            )
            return False

        # Check percentage threshold
        if usage_percent > self.settings.memory.gc_threshold:
            logger.warning(f"Memory usage high: {usage_percent:.1f}%")
            return False

        return True

    def force_cleanup(self) -> Dict[str, Any]:
        """
        Force memory cleanup.

        Returns:
            Dictionary with cleanup results
        """
        logger.info("Forcing memory cleanup...")

        before_stats = self.get_memory_stats()

        # Python garbage collection
        gc.collect()
        gc.collect()  # Run twice to ensure cleanup

        # Clear PyTorch cache if available
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
            torch.cuda.synchronize()

        after_stats = self.get_memory_stats()

        freed_mb = before_stats["process"]["rss_mb"] - after_stats["process"]["rss_mb"]

        result = {
            "before_mb": before_stats["process"]["rss_mb"],
            "after_mb": after_stats["process"]["rss_mb"],
            "freed_mb": freed_mb,
            "gc_stats": gc.get_stats()
        }

        logger.info(f"Memory cleanup completed: freed {freed_mb:.1f}MB")
        return result

    def start_monitoring(self, callback: Optional[Callable] = None) -> None:
        """
        Start background memory monitoring.

        Args:
            callback: Optional callback function to call when threshold exceeded
        """
        if self._monitoring:
            logger.warning("Memory monitoring already started")
            return

        if callback:
            self._callbacks.append(callback)

        self._monitoring = True
        self._monitor_thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self._monitor_thread.start()
        logger.info("Memory monitoring started")

    def stop_monitoring(self) -> None:
        """Stop background memory monitoring."""
        if not self._monitoring:
            return

        self._monitoring = False
        if self._monitor_thread:
            self._monitor_thread.join(timeout=5)

        logger.info("Memory monitoring stopped")

    def _monitor_loop(self) -> None:
        """Background monitoring loop."""
        check_interval = self.settings.memory.memory_check_interval

        while self._monitoring:
            try:
                # Check memory threshold
                if not self.check_memory_threshold():
                    # Trigger callbacks
                    for callback in self._callbacks:
                        try:
                            callback(self.get_memory_stats())
                        except Exception as e:
                            logger.error(f"Memory monitor callback error: {e}")

                    # Auto cleanup if threshold exceeded
                    self.force_cleanup()

            except Exception as e:
                logger.error(f"Memory monitoring error: {e}")

            # Sleep for interval
            time.sleep(check_interval)

    def add_callback(self, callback: Callable) -> None:
        """
        Add a callback for memory threshold events.

        Args:
            callback: Function to call when threshold exceeded
        """
        self._callbacks.append(callback)

    def remove_callback(self, callback: Callable) -> None:
        """
        Remove a callback.

        Args:
            callback: Callback to remove
        """
        if callback in self._callbacks:
            self._callbacks.remove(callback)


class MemoryContextManager:
    """Context manager for memory-intensive operations."""

    def __init__(self, cleanup_on_exit: bool = True, log_stats: bool = True):
        self.cleanup_on_exit = cleanup_on_exit
        self.log_stats = log_stats
        self.monitor = MemoryMonitor()
        self.start_stats = None

    def __enter__(self):
        """Enter context."""
        self.start_stats = self.monitor.get_memory_stats()

        if self.log_stats:
            logger.info(
                f"Memory at start: {self.start_stats['process']['rss_mb']:.1f}MB "
                f"({self.start_stats['process']['percent']:.1f}%)"
            )

        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Exit context."""
        end_stats = self.monitor.get_memory_stats()

        if self.log_stats:
            memory_delta = end_stats["process"]["rss_mb"] - self.start_stats["process"]["rss_mb"]
            logger.info(
                f"Memory at exit: {end_stats['process']['rss_mb']:.1f}MB "
                f"({end_stats['process']['percent']:.1f}%), "
                f"Delta: {memory_delta:+.1f}MB"
            )

        if self.cleanup_on_exit:
            self.monitor.force_cleanup()


def estimate_model_memory(model_name: str) -> float:
    """
    Estimate memory requirements for a model.

    Args:
        model_name: Name of the model

    Returns:
        Estimated memory in MB
    """
    # Common model size estimates (rough approximations)
    model_sizes = {
        "klue/roberta-small": 200,
        "distilbert-base-uncased": 250,
        "jhgan/ko-sroberta-multitask": 400,
        "sentence-transformers/all-MiniLM-L6-v2": 90,
        "gpt-3.5-turbo": 0,  # API model, no local memory
        "gpt-4": 0,  # API model
    }

    # Check if model name contains known patterns
    for pattern, size in model_sizes.items():
        if pattern in model_name:
            return size

    # Default estimate for unknown models
    return 500  # Conservative estimate


def optimize_memory_settings() -> Dict[str, Any]:
    """
    Optimize memory settings based on available resources.

    Returns:
        Dictionary with optimized settings
    """
    virtual_memory = psutil.virtual_memory()
    available_mb = virtual_memory.available / 1024 / 1024
    total_mb = virtual_memory.total / 1024 / 1024

    # Calculate optimal settings
    max_memory = min(
        available_mb * 0.7,  # Use up to 70% of available memory
        total_mb * 0.5,      # But no more than 50% of total memory
        4000                 # Cap at 4GB
    )

    gc_threshold = 80 if max_memory > 2000 else 70

    batch_size = 4 if max_memory > 2000 else 2

    max_models = 3 if max_memory > 3000 else 2

    return {
        "max_memory_mb": int(max_memory),
        "gc_threshold": gc_threshold,
        "batch_size": batch_size,
        "max_models_in_memory": max_models,
        "available_mb": available_mb,
        "total_mb": total_mb
    }


# Global memory monitor instance
_memory_monitor: Optional[MemoryMonitor] = None


def get_memory_monitor() -> MemoryMonitor:
    """Get or create global memory monitor instance."""
    global _memory_monitor
    if _memory_monitor is None:
        _memory_monitor = MemoryMonitor()
    return _memory_monitor