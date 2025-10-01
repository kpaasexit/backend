"""OpenAI client management."""

from typing import Optional

from openai import OpenAI, AsyncOpenAI

from app.config import get_settings
from app.core.logger import LoggerSetup
from app.core.exceptions import GPTServiceError


logger = LoggerSetup.get_logger(__name__)


class OpenAIClient:
    """Manages OpenAI client instances."""

    def __init__(self):
        self.settings = get_settings()
        self._client: Optional[OpenAI] = None
        self._async_client: Optional[AsyncOpenAI] = None
        self._initialize_clients()

    def _initialize_clients(self) -> None:
        """Initialize OpenAI clients."""
        if self.settings.openai.openai_api_key:
            self._client = OpenAI(
                api_key=self.settings.openai.openai_api_key,
                timeout=self.settings.openai.openai_timeout
            )
            self._async_client = AsyncOpenAI(
                api_key=self.settings.openai.openai_api_key,
                timeout=self.settings.openai.openai_timeout
            )
            logger.info("OpenAI clients initialized")
        else:
            logger.warning("OpenAI API key not configured")

    @property
    def sync_client(self) -> OpenAI:
        """Get synchronous client."""
        if not self._client:
            raise GPTServiceError(
                reason="OpenAI client not initialized. Check API key configuration."
            )
        return self._client

    @property
    def async_client(self) -> AsyncOpenAI:
        """Get asynchronous client."""
        if not self._async_client:
            raise GPTServiceError(
                reason="Async OpenAI client not initialized. Check API key configuration."
            )
        return self._async_client

    def validate_api_key(self) -> bool:
        """Validate OpenAI API key."""
        try:
            if not self._client:
                return False

            # Test with a simple completion
            self._client.chat.completions.create(
                model="gpt-3.5-turbo",
                messages=[{"role": "user", "content": "test"}],
                max_tokens=1
            )
            return True

        except Exception as e:
            logger.error(f"API key validation failed: {e}")
            return False