"""Redis client management."""

from typing import Optional
import redis
from redis.exceptions import RedisError

from app.config import get_settings
from app.core.logger import LoggerSetup


logger = LoggerSetup.get_logger(__name__)


class RedisClient:
    """Manages Redis connection and operations."""

    def __init__(self):
        self.settings = get_settings()
        self._client: Optional[redis.Redis] = None
        self._initialized = False

    def get_client(self) -> Optional[redis.Redis]:
        """Get or create Redis client."""
        if not self._initialized:
            self._initialize_client()
        return self._client

    def _initialize_client(self) -> None:
        """Initialize Redis connection."""
        try:
            self._client = redis.Redis(
                host=self.settings.redis.redis_host,
                port=self.settings.redis.redis_port,
                password=self.settings.redis.redis_password,
                db=self.settings.redis.redis_db,
                decode_responses=False,  # We'll handle encoding ourselves
                socket_timeout=5,
                socket_connect_timeout=5,
                retry_on_timeout=True,
                health_check_interval=30
            )

            # Test connection
            self._client.ping()
            self._initialized = True
            logger.info("Redis client initialized successfully")

        except RedisError as e:
            logger.warning(f"Redis connection failed: {e}")
            self._client = None
        except Exception as e:
            logger.error(f"Unexpected error initializing Redis: {e}")
            self._client = None

        self._initialized = True

    def is_connected(self) -> bool:
        """Check if Redis is connected."""
        if not self._client:
            return False

        try:
            self._client.ping()
            return True
        except RedisError:
            return False