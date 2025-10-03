"""Main cache service implementation."""

import json
import pickle
from typing import Any, Dict, Optional, Union

import aiocache
from aiocache.serializers import PickleSerializer
from redis.exceptions import RedisError

from app.config import get_settings
from app.core.logger import LoggerSetup
from app.core.exceptions import CacheError

from .redis_client import RedisClient


logger = LoggerSetup.get_logger(__name__)


class CacheService:
    """
    Hierarchical caching service with Redis and in-memory LRU cache.

    Features:
    - Redis for distributed caching
    - In-memory LRU cache for fast access
    - TTL support
    - Async and sync interfaces
    """

    def __init__(self):
        self.settings = get_settings()
        self.redis_client = RedisClient()
        self._memory_cache = aiocache.Cache(aiocache.SimpleMemoryCache)
        self._memory_cache.serializer = PickleSerializer()

    def get(self, key: str) -> Optional[Any]:
        """Get value from cache (memory first, then Redis)."""
        # Try memory cache first
        try:
            memory_result = self._memory_cache.get(key)
            if memory_result is not None:
                logger.debug(f"Memory cache hit: {key}")
                return memory_result
        except Exception as e:
            logger.warning(f"Memory cache get failed: {e}")

        # Try Redis cache
        redis_client = self.redis_client.get_client()
        if redis_client:
            try:
                redis_result = redis_client.get(key)
                if redis_result is not None:
                    # Deserialize and store in memory cache
                    value = pickle.loads(redis_result)

                    # Store in memory cache for faster future access
                    try:
                        self._memory_cache.set(key, value, ttl=300)  # 5 min in memory
                    except Exception as e:
                        logger.warning(f"Failed to store in memory cache: {e}")

                    logger.debug(f"Redis cache hit: {key}")
                    return value
            except (RedisError, pickle.PickleError) as e:
                logger.warning(f"Redis cache get failed: {e}")
            except Exception as e:
                logger.error(f"Unexpected cache get error: {e}")

        logger.debug(f"Cache miss: {key}")
        return None

    async def aget(self, key: str) -> Optional[Any]:
        """Async get value from cache."""
        # Try memory cache first
        try:
            memory_result = await self._memory_cache.get(key)
            if memory_result is not None:
                logger.debug(f"Memory cache hit: {key}")
                return memory_result
        except Exception as e:
            logger.warning(f"Memory cache aget failed: {e}")

        # Try Redis cache
        redis_client = self.redis_client.get_client()
        if redis_client:
            try:
                redis_result = redis_client.get(key)
                if redis_result is not None:
                    # Deserialize and store in memory cache
                    value = pickle.loads(redis_result)

                    # Store in memory cache for faster future access
                    try:
                        await self._memory_cache.set(key, value, ttl=300)  # 5 min in memory
                    except Exception as e:
                        logger.warning(f"Failed to store in memory cache: {e}")

                    logger.debug(f"Redis cache hit: {key}")
                    return value
            except (RedisError, pickle.PickleError) as e:
                logger.warning(f"Redis cache aget failed: {e}")
            except Exception as e:
                logger.error(f"Unexpected cache aget error: {e}")

        logger.debug(f"Cache miss: {key}")
        return None

    def set(self, key: str, value: Any, ttl: int = None) -> bool:
        """Set value in cache (both memory and Redis)."""
        if ttl is None:
            ttl = self.settings.redis.cache_ttl

        success = True

        # Set in memory cache
        try:
            self._memory_cache.set(key, value, ttl=min(ttl, 300))  # Max 5 min in memory
        except Exception as e:
            logger.warning(f"Memory cache set failed: {e}")
            success = False

        # Set in Redis cache
        redis_client = self.redis_client.get_client()
        if redis_client:
            try:
                serialized_value = pickle.dumps(value)
                redis_client.setex(key, ttl, serialized_value)
                logger.debug(f"Cache set: {key} (TTL: {ttl}s)")
            except (RedisError, pickle.PickleError) as e:
                logger.warning(f"Redis cache set failed: {e}")
                success = False
            except Exception as e:
                logger.error(f"Unexpected cache set error: {e}")
                success = False
        else:
            success = False

        return success

    async def aset(self, key: str, value: Any, ttl: int = None) -> bool:
        """Async set value in cache."""
        if ttl is None:
            ttl = self.settings.redis.cache_ttl

        success = True

        # Set in memory cache
        try:
            await self._memory_cache.set(key, value, ttl=min(ttl, 300))  # Max 5 min in memory
        except Exception as e:
            logger.warning(f"Memory cache aset failed: {e}")
            success = False

        # Set in Redis cache
        redis_client = self.redis_client.get_client()
        if redis_client:
            try:
                serialized_value = pickle.dumps(value)
                redis_client.setex(key, ttl, serialized_value)
                logger.debug(f"Cache set: {key} (TTL: {ttl}s)")
            except (RedisError, pickle.PickleError) as e:
                logger.warning(f"Redis cache aset failed: {e}")
                success = False
            except Exception as e:
                logger.error(f"Unexpected cache aset error: {e}")
                success = False
        else:
            success = False

        return success

    def delete(self, key: str) -> bool:
        """Delete key from cache."""
        success = True

        # Delete from memory cache
        try:
            self._memory_cache.delete(key)
        except Exception as e:
            logger.warning(f"Memory cache delete failed: {e}")

        # Delete from Redis cache
        redis_client = self.redis_client.get_client()
        if redis_client:
            try:
                result = redis_client.delete(key)
                success = success and bool(result)
                logger.debug(f"Cache delete: {key}")
            except RedisError as e:
                logger.warning(f"Redis cache delete failed: {e}")
                success = False
            except Exception as e:
                logger.error(f"Unexpected cache delete error: {e}")
                success = False

        return success

    async def adelete(self, key: str) -> bool:
        """Async delete key from cache."""
        success = True

        # Delete from memory cache
        try:
            await self._memory_cache.delete(key)
        except Exception as e:
            logger.warning(f"Memory cache adelete failed: {e}")

        # Delete from Redis cache
        redis_client = self.redis_client.get_client()
        if redis_client:
            try:
                result = redis_client.delete(key)
                success = success and bool(result)
                logger.debug(f"Cache delete: {key}")
            except RedisError as e:
                logger.warning(f"Redis cache adelete failed: {e}")
                success = False
            except Exception as e:
                logger.error(f"Unexpected cache adelete error: {e}")
                success = False

        return success

    def clear(self, pattern: str = "*") -> int:
        """Clear cache entries matching pattern."""
        cleared = 0

        # Clear memory cache (all entries)
        try:
            self._memory_cache.clear()
            logger.debug("Memory cache cleared")
        except Exception as e:
            logger.warning(f"Memory cache clear failed: {e}")

        # Clear Redis cache with pattern
        redis_client = self.redis_client.get_client()
        if redis_client:
            try:
                keys = redis_client.keys(pattern)
                if keys:
                    cleared = redis_client.delete(*keys)
                    logger.debug(f"Redis cache cleared: {cleared} keys")
            except RedisError as e:
                logger.warning(f"Redis cache clear failed: {e}")
            except Exception as e:
                logger.error(f"Unexpected cache clear error: {e}")

        return cleared

    def get_stats(self) -> Dict[str, Any]:
        """Get cache statistics."""
        stats = {
            "redis_connected": self.redis_client.is_connected(),
            "memory_cache_enabled": True
        }

        # Redis stats
        redis_client = self.redis_client.get_client()
        if redis_client and self.redis_client.is_connected():
            try:
                info = redis_client.info()
                stats.update({
                    "redis_used_memory": info.get("used_memory_human", "unknown"),
                    "redis_connected_clients": info.get("connected_clients", 0),
                    "redis_total_commands": info.get("total_commands_processed", 0),
                })
            except RedisError as e:
                logger.warning(f"Failed to get Redis stats: {e}")

        return stats


# Global cache service instance
_cache_service: Optional[CacheService] = None


def get_cache_service() -> CacheService:
    """Get or create global cache service instance."""
    global _cache_service
    if _cache_service is None:
        _cache_service = CacheService()
    return _cache_service