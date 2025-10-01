"""Main GPT service interface."""

from typing import Dict, List, Any, Optional

from app.services.cache import get_cache_service
from app.config import get_settings
from app.core.logger import LoggerSetup

from .client import OpenAIClient
from .generator import AnswerGenerator


logger = LoggerSetup.get_logger(__name__)


class GPTService:
    """
    Service for generating AI answers using GPT models.

    Features:
    - Category-specific personas
    - Dynamic prompt generation
    - Response caching
    - Retry logic
    - Token usage tracking
    """

    def __init__(self):
        self.settings = get_settings()
        self.cache_service = get_cache_service()
        self.client = OpenAIClient()
        self.generator = AnswerGenerator(self.client)

    def generate_answer(
        self,
        question: str,
        context: Optional[str] = None,
        max_retries: int = 3
    ) -> Dict[str, Any]:
        """Generate AI answer for a question."""
        # Always check cache
        cache_key = f"answer:{hash(question)}"
        cached_result = self.cache_service.get(cache_key)
        if cached_result:
            logger.debug(f"Answer retrieved from cache: {question[:50]}...")
            return cached_result

        # Generate new answer
        result = self.generator.generate_answer(question, context, max_retries)

        # Always cache result
        self.cache_service.set(
            cache_key,
            result,
            ttl=self.settings.redis.cache_ttl
        )

        return result

    async def agenerate_answer(
        self,
        question: str,
        context: Optional[str] = None,
        max_retries: int = 3
    ) -> Dict[str, Any]:
        """Async version of generate_answer."""
        # Always check cache
        cache_key = f"answer:{hash(question)}"
        cached_result = await self.cache_service.aget(cache_key)
        if cached_result:
            logger.debug(f"Answer retrieved from cache: {question[:50]}...")
            return cached_result

        # Generate new answer
        result = await self.generator.agenerate_answer(question, context, max_retries)

        # Always cache result
        await self.cache_service.aset(
            cache_key,
            result,
            ttl=self.settings.redis.cache_ttl
        )

        return result

    def generate_batch_answers(
        self,
        questions: List[Dict[str, str]]
    ) -> List[Dict[str, Any]]:
        """Generate answers for multiple questions."""
        return self.generator.generate_batch_answers(questions)

    def create_summary(
        self,
        text: str,
        max_length: int = 100
    ) -> str:
        """Create a summary of text using GPT."""
        return self.generator.create_summary(text, max_length)

    def extract_keywords(
        self,
        text: str,
        num_keywords: int = 5
    ) -> List[str]:
        """Extract keywords from text using GPT."""
        return self.generator.extract_keywords(text, num_keywords)

    def validate_api_key(self) -> bool:
        """Validate OpenAI API key."""
        return self.client.validate_api_key()

    def create_prompt(
        self,
        question: str,
        context: Optional[str] = None
    ) -> str:
        """Create prompt for GPT based on question."""
        return self.generator.prompt_builder.create_answer_prompt(question, context)


# Global GPT service instance
_gpt_service: Optional[GPTService] = None


def get_gpt_service() -> GPTService:
    """Get or create global GPT service instance."""
    global _gpt_service
    if _gpt_service is None:
        _gpt_service = GPTService()
    return _gpt_service