#!/usr/bin/env python3
"""Deployment initialization script for AI Service."""

import asyncio
import sys
import os
from pathlib import Path

# Add app to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.core.logger import LoggerSetup
from app.vectordb.collections import Collections
from app.vectordb.client import get_qdrant_client
from app.config import get_settings

logger = LoggerSetup.get_logger("deployment_init")


async def check_model_cache_permissions():
    """Check and fix model cache directory permissions."""
    cache_dir = Path("/app/model_cache")

    try:
        # Create directory if not exists
        cache_dir.mkdir(parents=True, exist_ok=True)

        # Check write permissions
        test_file = cache_dir / ".write_test"
        try:
            test_file.touch()
            test_file.unlink()
            logger.info(f"✓ Model cache directory {cache_dir} is writable")
            return True
        except PermissionError:
            logger.error(f"✗ Model cache directory {cache_dir} is not writable")
            logger.info("  Fix: Run 'chown -R appuser:appuser /app/model_cache' as root")
            return False

    except Exception as e:
        logger.error(f"Error checking model cache permissions: {e}")
        return False


async def initialize_qdrant_collections():
    """Initialize Qdrant collections."""
    try:
        client = get_qdrant_client()

        # Check connection
        if not client.is_alive():
            logger.error("✗ Cannot connect to Qdrant")
            return False

        logger.info("✓ Connected to Qdrant")

        # Create collections
        results = Collections.create_all_collections(force=False)

        for collection, created in results.items():
            if created:
                logger.info(f"✓ Collection '{collection}' ready")
            else:
                logger.error(f"✗ Failed to create collection '{collection}'")

        return all(results.values())

    except Exception as e:
        logger.error(f"Error initializing Qdrant: {e}")
        return False


async def preload_embedding_model():
    """Preload the embedding model to cache."""
    try:
        from app.services.embedding import EmbeddingService

        logger.info("Preloading embedding model...")
        embedder = EmbeddingService()

        # Test embedding generation
        test_text = "테스트 문장입니다"
        embedding = await embedder.get_embedding(test_text)

        if embedding is not None:
            logger.info("✓ Embedding model loaded and cached")
            return True
        else:
            logger.error("✗ Failed to generate embedding")
            return False

    except Exception as e:
        logger.error(f"Error preloading embedding model: {e}")
        return False


async def check_redis_connection():
    """Check Redis connection."""
    try:
        from app.services.cache import CacheService

        cache = CacheService()

        # Test connection
        test_key = "deployment_test"
        await cache.set(test_key, "test_value", expire=10)
        value = await cache.get(test_key)

        if value == "test_value":
            logger.info("✓ Redis connection successful")
            await cache.delete(test_key)
            return True
        else:
            logger.error("✗ Redis connection failed")
            return False

    except Exception as e:
        logger.error(f"Error checking Redis: {e}")
        return False


async def main():
    """Run all initialization checks."""
    logger.info("=" * 60)
    logger.info("Starting deployment initialization...")
    logger.info("=" * 60)

    checks = [
        ("Model Cache Permissions", check_model_cache_permissions()),
        ("Qdrant Collections", initialize_qdrant_collections()),
        ("Redis Connection", check_redis_connection()),
        ("Embedding Model", preload_embedding_model()),
    ]

    results = []
    for name, check_coro in checks:
        logger.info(f"\nChecking {name}...")
        try:
            result = await check_coro
            results.append(result)
        except Exception as e:
            logger.error(f"Check failed: {e}")
            results.append(False)

    logger.info("\n" + "=" * 60)

    if all(results):
        logger.info("✓ All initialization checks passed!")
        logger.info("=" * 60)
        return 0
    else:
        logger.error("✗ Some initialization checks failed")
        logger.error("Please fix the issues above before starting the service")
        logger.info("=" * 60)
        return 1


if __name__ == "__main__":
    exit_code = asyncio.run(main())
    sys.exit(exit_code)