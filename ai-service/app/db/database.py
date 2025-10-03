from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker
from sqlalchemy.orm import declarative_base
from sqlalchemy import text
from app.config import get_settings
import logging
import asyncio

logger = logging.getLogger(__name__)

settings = get_settings()

DATABASE_URL = f"mysql+aiomysql://{settings.mysql.mysql_user}:{settings.mysql.mysql_password}@{settings.mysql.mysql_host}:{settings.mysql.mysql_port}/{settings.mysql.mysql_database}?charset=utf8mb4"

_engine = None
_engine_loop = None

def get_engine():
    """Get or create engine for current event loop"""
    global _engine, _engine_loop

    try:
        current_loop = asyncio.get_running_loop()
    except RuntimeError:
        current_loop = None

    # Create new engine if no engine exists or if event loop changed
    if _engine is None or _engine_loop != current_loop:
        if _engine is not None:
            # Dispose old engine asynchronously would be better, but sync dispose is safe here
            asyncio.create_task(_engine.dispose())

        _engine = create_async_engine(
            DATABASE_URL,
            echo=False,
            pool_pre_ping=True,
            pool_size=5,
            max_overflow=10,
            pool_recycle=3600,
            pool_timeout=30,
            connect_args={
                "connect_timeout": 10,
                "charset": "utf8mb4"
            }
        )
        _engine_loop = current_loop
        logger.debug(f"Created new database engine for event loop: {id(current_loop)}")

    return _engine

# For backwards compatibility
engine = get_engine()

AsyncSessionLocal = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,
)

Base = declarative_base()


async def get_db() -> AsyncSession:
    engine = get_engine()
    SessionLocal = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with SessionLocal() as session:
        try:
            yield session
        finally:
            await session.close()


async def init_db():
    """Initialize database connection and verify table exists.
    In MSA environment, we don't create tables - they should be managed by migration tools.
    """
    try:
        # Test connection only
        engine = get_engine()
        async with engine.begin() as conn:
            # Check if we can connect and query the table
            result = await conn.execute(
                text("SELECT 1 FROM quizs LIMIT 1")
            )
            logger.info("✓ Connected to MySQL database and verified quizs table exists")
    except Exception as e:
        if "doesn't exist" in str(e) or "1146" in str(e):
            logger.error(f"Table 'quizs' does not exist in database. Please ensure Spring application has created the table.")
            raise Exception("Database table 'quizs' not found. In MSA environment, tables should be created by the primary service.")
        elif "Access denied" in str(e):
            logger.error(f"Access denied to database. Check user permissions.")
            raise
        else:
            # Connection test - might be empty table
            logger.info("✓ Connected to MySQL database")


async def close_db():
    global _engine
    if _engine is not None:
        await _engine.dispose()
        _engine = None
        logger.info("Database connection closed")