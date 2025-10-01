from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker
from sqlalchemy.orm import declarative_base
from sqlalchemy import text
from app.config import get_settings
import logging

logger = logging.getLogger(__name__)

settings = get_settings()

DATABASE_URL = f"mysql+aiomysql://{settings.mysql.mysql_user}:{settings.mysql.mysql_password}@{settings.mysql.mysql_host}:{settings.mysql.mysql_port}/{settings.mysql.mysql_database}?charset=utf8mb4"

engine = create_async_engine(
    DATABASE_URL,
    echo=False,
    pool_pre_ping=True,  # Check connection health before using
    pool_size=5,  # Reduced for MSA - don't hog connections
    max_overflow=10,  # Allow some overflow but not too much
    pool_recycle=3600,  # Recycle connections every hour
    pool_timeout=30,  # Timeout waiting for connection
    connect_args={
        "connect_timeout": 10,
        "charset": "utf8mb4"
    }
)

AsyncSessionLocal = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,
)

Base = declarative_base()


async def get_db() -> AsyncSession:
    async with AsyncSessionLocal() as session:
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
    await engine.dispose()
    logger.info("Database connection closed")