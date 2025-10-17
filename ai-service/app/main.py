import asyncio
import threading
from contextlib import asynccontextmanager
from datetime import datetime
from typing import AsyncGenerator

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import get_settings
from app.grpc_service.question_server import QuestionGRPCServer
from app.grpc_service.quiz_server import QuizGRPCServer
from app.core.eureka import get_eureka_client
from app.api import search_router, quiz_router
from app.services.quiz import get_quiz_scheduler
from app.core.logger import LoggerSetup
from app.vectordb.collections import Collections
from app.vectordb.client import get_qdrant_client
from pathlib import Path


question_server = None
quiz_server = None
question_thread = None
quiz_thread = None
eureka_client = None
quiz_scheduler = None


def run_question_server():
    global question_server
    settings = get_settings()

    print(f"Starting Question gRPC server on port {settings.server.question_service_port}...")
    question_server = QuestionGRPCServer()

    try:
        question_server.start()
        asyncio.run(asyncio.Event().wait())
    except Exception as e:
        print(f"Question gRPC server error: {e}")


def run_quiz_server():
    global quiz_server
    settings = get_settings()

    print(f"Starting Quiz gRPC server on port {settings.server.quiz_service_port}...")
    quiz_server = QuizGRPCServer()

    try:
        quiz_server.start()
        asyncio.run(asyncio.Event().wait())
    except Exception as e:
        print(f"Quiz gRPC server error: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator:
    global question_thread, quiz_thread, eureka_client, quiz_scheduler
    logger = LoggerSetup.get_logger("main")

    # 초기화 체크
    logger.info("Starting deployment initialization...")

    # 1. Model cache 디렉토리 권한 체크
    cache_dir = Path("/app/model_cache")
    if cache_dir.exists():
        try:
            test_file = cache_dir / ".write_test"
            test_file.touch()
            test_file.unlink()
            logger.info("✓ Model cache directory is writable")
        except PermissionError:
            logger.warning("Model cache directory is not writable, attempting to fix...")
            import subprocess
            try:
                subprocess.run(["chown", "-R", "appuser:appuser", str(cache_dir)], check=False)
                logger.info("✓ Fixed model cache permissions")
            except Exception as e:
                logger.error(f"Failed to fix cache permissions: {e}")
    else:
        cache_dir.mkdir(parents=True, exist_ok=True)
        logger.info("✓ Created model cache directory")

    # 2. MySQL Database 초기화
    try:
        from app.db.database import init_db
        await init_db()
        logger.info("✓ MySQL database initialized")
    except Exception as e:
        logger.error(f"MySQL initialization error: {e}")

    # 3. Qdrant collections 초기화
    try:
        client = get_qdrant_client()
        if client.is_alive():
            logger.info("✓ Connected to Qdrant")
            results = Collections.create_all_collections(force=False)
            for collection, created in results.items():
                if created:
                    logger.info(f"✓ Collection '{collection}' initialized")
        else:
            logger.error("Failed to connect to Qdrant")
    except Exception as e:
        logger.error(f"Qdrant initialization error: {e}")

    # 4. Embedding model 사전 로드
    try:
        from app.services.embedding import EmbeddingService
        logger.info("Preloading embedding model...")
        embedder = EmbeddingService()
        test_embedding = await embedder.get_embedding("test")
        if test_embedding is not None:
            logger.info("✓ Embedding model cached successfully")
    except Exception as e:
        logger.warning(f"Could not preload embedding model: {e}")

    question_thread = threading.Thread(target=run_question_server, daemon=True)
    question_thread.start()

    quiz_thread = threading.Thread(target=run_quiz_server, daemon=True)
    quiz_thread.start()

    eureka_client = get_eureka_client()
    eureka_client.register()

    # Quiz 스케줄러 시작
    quiz_scheduler = get_quiz_scheduler()
    quiz_scheduler.start()
    logger.info("✓ Quiz scheduler started")

    yield

    # Cleanup
    if quiz_scheduler:
        quiz_scheduler.stop()

    if eureka_client:
        eureka_client.deregister()

    if question_server:
        question_server.stop()

    if quiz_server:
        quiz_server.stop()

    # Close MySQL database connection
    try:
        from app.db.database import close_db
        await close_db()
        logger.info("MySQL database connection closed")
    except Exception as e:
        logger.error(f"Error closing MySQL database: {e}")


def create_app() -> FastAPI:
    settings = get_settings()

    app = FastAPI(
        title="AI Service API",
        description="""

Question Service gRPC: 50051
Quiz Service gRPC: 50052
Search Service REST: /api/search/*
Quiz Service REST: /api/quiz/*

        """.strip(),
        version="1.0.0",
        lifespan=lifespan,
        servers=[
            {"url": f"http://localhost:{settings.server.http_port}", "description": "Local HTTP Server"},
            {"url": f"grpc://localhost:{settings.server.question_service_port}", "description": "Question gRPC Server"},
            {"url": f"grpc://localhost:{settings.server.quiz_service_port}", "description": "Quiz gRPC Server"}
        ]
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # REST API 라우터 등록
    app.include_router(search_router)
    app.include_router(quiz_router)

    @app.get("/health", summary="상태 확인", tags=["Health"])
    async def health_check():
        """
        서비스 상태를 확인합니다.

        ### 체크 항목:
        - FastAPI 서버 상태
        - gRPC 서버 상태
        - Quiz 스케줄러 상태
        - Eureka 등록 상태
        - Qdrant 연결 상태
        - Redis 연결 상태
        - Model Cache 권한
        """
        health_status = {
            "status": "healthy",
            "services": {
                "fastapi": "running",
                "grpc_question": "running" if question_server and question_server.server else "starting",
                "grpc_quiz": "running" if quiz_server and quiz_server.server else "starting",
                "quiz_scheduler": "running" if quiz_scheduler and quiz_scheduler.scheduler.running else "stopped",
                "eureka": "registered" if eureka_client and eureka_client._initialized else "not_registered"
            },
            "dependencies": {},
            "timestamp": datetime.now().isoformat()
        }

        # Check Qdrant
        try:
            client = get_qdrant_client()
            if client.is_alive():
                collections = client._client.get_collections().collections
                health_status["dependencies"]["qdrant"] = {
                    "status": "connected",
                    "collections": len(collections)
                }
            else:
                health_status["dependencies"]["qdrant"] = {"status": "disconnected"}
                health_status["status"] = "degraded"
        except Exception as e:
            health_status["dependencies"]["qdrant"] = {"status": "error", "error": str(e)}
            health_status["status"] = "degraded"

        # Check Redis
        try:
            from app.services.cache import CacheService
            cache = CacheService()
            await cache.set("health_check", "ok", expire=5)
            value = await cache.get("health_check")
            if value == "ok":
                health_status["dependencies"]["redis"] = {"status": "connected"}
            else:
                health_status["dependencies"]["redis"] = {"status": "error"}
                health_status["status"] = "degraded"
        except Exception as e:
            health_status["dependencies"]["redis"] = {"status": "error", "error": str(e)}
            health_status["status"] = "degraded"

        # Check model cache
        cache_dir = Path("/app/model_cache")
        if cache_dir.exists():
            try:
                test_file = cache_dir / ".health_check"
                test_file.touch()
                test_file.unlink()
                health_status["dependencies"]["model_cache"] = {"status": "writable"}
            except PermissionError:
                health_status["dependencies"]["model_cache"] = {"status": "read_only"}
                health_status["status"] = "degraded"
        else:
            health_status["dependencies"]["model_cache"] = {"status": "not_found"}
            health_status["status"] = "degraded"

        return health_status



    return app


app = create_app()