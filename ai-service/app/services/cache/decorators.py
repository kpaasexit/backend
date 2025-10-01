"""Cache decorators."""

import hashlib
from functools import wraps
from typing import Any, Callable, Optional

from app.core.logger import LoggerSetup

logger = LoggerSetup.get_logger(__name__)


def cached(
    ttl: int = 3600,
    key_prefix: str = "cache",
    key_builder: Optional[Callable] = None
):
    """
    Decorator for caching function results.

    Args:
        ttl: Time to live in seconds
        key_prefix: Prefix for cache keys
        key_builder: Custom key building function
    """
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            # Import here to avoid circular imports
            from .service import get_cache_service

            cache_service = get_cache_service()

            # Build cache key
            if key_builder:
                cache_key = key_builder(*args, **kwargs)
            else:
                # Default key building
                key_parts = [key_prefix, func.__name__]
                if args:
                    key_parts.extend(str(arg) for arg in args)
                if kwargs:
                    key_parts.extend(f"{k}={v}" for k, v in sorted(kwargs.items()))

                key_str = ":".join(key_parts)
                cache_key = hashlib.md5(key_str.encode()).hexdigest()

            # Try to get from cache
            cached_result = cache_service.get(cache_key)
            if cached_result is not None:
                logger.debug(f"Cache hit for {func.__name__}")
                return cached_result

            # Execute function and cache result
            result = func(*args, **kwargs)
            cache_service.set(cache_key, result, ttl=ttl)
            logger.debug(f"Cache miss for {func.__name__}, result cached")

            return result

        return wrapper
    return decorator


def async_cached(
    ttl: int = 3600,
    key_prefix: str = "cache",
    key_builder: Optional[Callable] = None
):
    """
    Async decorator for caching function results.

    Args:
        ttl: Time to live in seconds
        key_prefix: Prefix for cache keys
        key_builder: Custom key building function
    """
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        async def wrapper(*args, **kwargs):
            # Import here to avoid circular imports
            from .service import get_cache_service

            cache_service = get_cache_service()

            # Build cache key
            if key_builder:
                cache_key = key_builder(*args, **kwargs)
            else:
                # Default key building
                key_parts = [key_prefix, func.__name__]
                if args:
                    key_parts.extend(str(arg) for arg in args)
                if kwargs:
                    key_parts.extend(f"{k}={v}" for k, v in sorted(kwargs.items()))

                key_str = ":".join(key_parts)
                cache_key = hashlib.md5(key_str.encode()).hexdigest()

            # Try to get from cache
            cached_result = await cache_service.aget(cache_key)
            if cached_result is not None:
                logger.debug(f"Cache hit for {func.__name__}")
                return cached_result

            # Execute function and cache result
            result = await func(*args, **kwargs)
            await cache_service.aset(cache_key, result, ttl=ttl)
            logger.debug(f"Cache miss for {func.__name__}, result cached")

            return result

        return wrapper
    return decorator