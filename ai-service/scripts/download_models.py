#!/usr/bin/env python

import sys
from pathlib import Path
import time

sys.path.append(str(Path(__file__).parent.parent))

from app.config import get_settings
from app.core.logger import LoggerSetup
from app.models.manager import get_model_manager
from app.core.constants import ModelType
from app.utils.memory import get_memory_monitor


logger = LoggerSetup.setup_logger("download_models")


def download_models():
    logger.info("Starting model download...")

    try:
        settings = get_settings()
        model_manager = get_model_manager()
        memory_monitor = get_memory_monitor()

        models_to_download = [
            {
                "name": settings.model.ko_classifier_model,
                "type": ModelType.CLASSIFIER,
                "description": "Korean classifier"
            },
            {
                "name": settings.model.en_classifier_model,
                "type": ModelType.CLASSIFIER,
                "description": "English classifier"
            },
            {
                "name": settings.model.ko_embedder_model,
                "type": ModelType.EMBEDDER,
                "description": "Korean embedder"
            },
            {
                "name": settings.model.en_embedder_model,
                "type": ModelType.EMBEDDER,
                "description": "English embedder"
            }
        ]

        memory_stats = memory_monitor.get_memory_stats()
        logger.info(
            f"Initial memory: {memory_stats['process']['rss_mb']:.1f}MB "
            f"({memory_stats['process']['percent']:.1f}%)"
        )

        successful = 0
        failed = 0

        for model_info in models_to_download:
            model_name = model_info["name"]
            model_type = model_info["type"]
            description = model_info["description"]

            logger.info(f"Downloading {description}: {model_name}")

            try:
                start_time = time.time()

                model_data = model_manager.load_model(
                    model_name=model_name,
                    model_type=model_type,
                    use_onnx=False,  # Download PyTorch version first
                    force_reload=False
                )

                elapsed_time = time.time() - start_time

                if model_data:
                    logger.info(
                        f"✓ {description} loaded successfully "
                        f"(time: {elapsed_time:.1f}s)"
                    )
                    successful += 1

                    model_manager.unload_model(model_name)

                    memory_stats = memory_monitor.get_memory_stats()
                    logger.info(
                        f"Memory after unload: {memory_stats['process']['rss_mb']:.1f}MB"
                    )
                else:
                    logger.error(f"✗ Failed to load {description}")
                    failed += 1

            except Exception as e:
                logger.error(f"✗ Error downloading {description}: {e}")
                failed += 1

            memory_monitor.force_cleanup()

        logger.info("=" * 50)
        logger.info("Download Summary:")
        logger.info(f"  Successful: {successful}")
        logger.info(f"  Failed: {failed}")
        logger.info(f"  Total: {len(models_to_download)}")

        memory_stats = memory_monitor.get_memory_stats()
        logger.info(
            f"Final memory: {memory_stats['process']['rss_mb']:.1f}MB "
            f"({memory_stats['process']['percent']:.1f}%)"
        )

        cached_models = model_manager.list_cached_models()
        if cached_models:
            logger.info("Currently cached models:")
            for model_name, info in cached_models.items():
                logger.info(f"  - {model_name}: {info}")

        return successful == len(models_to_download)

    except Exception as e:
        logger.error(f"Model download failed: {e}")
        return False


def verify_model_cache():
    settings = get_settings()
    cache_dir = settings.model.model_cache_dir

    if not cache_dir.exists():
        logger.warning(f"Cache directory does not exist: {cache_dir}")
        return False

    cached_files = list(cache_dir.rglob("*"))
    logger.info(f"Found {len(cached_files)} files in cache directory")

    expected_models = [
        settings.model.ko_classifier_model.split("/")[-1],
        settings.model.en_classifier_model.split("/")[-1],
        settings.model.ko_embedder_model.split("/")[-1],
        settings.model.en_embedder_model.split("/")[-1]
    ]

    found_models = []
    for model_name in expected_models:
        model_files = [f for f in cached_files if model_name in str(f)]
        if model_files:
            found_models.append(model_name)
            logger.info(f"✓ Found cached files for {model_name}")
        else:
            logger.warning(f"✗ No cached files found for {model_name}")

    return len(found_models) == len(expected_models)


def clean_model_cache():
    settings = get_settings()
    cache_dir = settings.model.model_cache_dir

    if not cache_dir.exists():
        logger.info("Cache directory does not exist, nothing to clean")
        return

    logger.info(f"Cleaning cache directory: {cache_dir}")

    import shutil
    try:
        shutil.rmtree(cache_dir)
        cache_dir.mkdir(parents=True, exist_ok=True)
        logger.info("Cache directory cleaned")
    except Exception as e:
        logger.error(f"Failed to clean cache directory: {e}")


def main():
    import argparse

    parser = argparse.ArgumentParser(description="Download and cache NLP models")
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Clean the cache before downloading"
    )
    parser.add_argument(
        "--verify",
        action="store_true",
        help="Verify cached models without downloading"
    )

    args = parser.parse_args()

    logger.info("=" * 50)
    logger.info("NLP Service - Model Download")
    logger.info("=" * 50)

    if args.clean:
        logger.info("Cleaning model cache...")
        clean_model_cache()

    if args.verify:
        logger.info("Verifying model cache...")
        if verify_model_cache():
            logger.info("✓ All models are cached")
            sys.exit(0)
        else:
            logger.warning("⚠ Some models are missing from cache")
            sys.exit(1)

    if download_models():
        logger.info("✓ All models downloaded successfully")

        if verify_model_cache():
            logger.info("✓ Model cache verified")
            sys.exit(0)
        else:
            logger.warning("⚠ Model cache verification failed")
            sys.exit(1)
    else:
        logger.error("✗ Model download failed")
        sys.exit(1)


if __name__ == "__main__":
    main()