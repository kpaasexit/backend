"""Answer generation utilities."""

import time
import asyncio
from typing import Dict, List, Any, Optional

from app.config import get_settings
from app.core.logger import LoggerSetup
from app.core.exceptions import GPTServiceError

from .client import OpenAIClient
from .prompts import PromptBuilder


logger = LoggerSetup.get_logger(__name__)


class AnswerGenerator:
    """Handles GPT answer generation."""

    def __init__(self, client: OpenAIClient):
        self.client = client
        self.settings = get_settings()
        self.prompt_builder = PromptBuilder()

    def generate_answer(
        self,
        question: str,
        context: Optional[str] = None,
        max_retries: int = 3
    ) -> Dict[str, Any]:
        """Generate answer using GPT."""
        prompt = self.prompt_builder.create_answer_prompt(question, context)

        for attempt in range(max_retries):
            try:
                start_time = time.time()

                response = self.client.sync_client.chat.completions.create(
                    model=self.settings.openai.openai_model,
                    messages=[
                        {
                            "role": "system",
                            "content": "You are a helpful assistant that provides practical life advice in Korean. Provide direct, concise answers in plain text without any markdown formatting."
                        },
                        {
                            "role": "user",
                            "content": prompt
                        }
                    ],
                    max_tokens=self.settings.openai.openai_max_tokens,
                    temperature=self.settings.openai.openai_temperature,
                    top_p=0.9,
                    frequency_penalty=0.2,
                    presence_penalty=0.1
                )

                # Extract answer
                answer = response.choices[0].message.content.strip()
                tokens_used = response.usage.total_tokens if response.usage else 0
                response_time = time.time() - start_time

                result = {
                    "answer": answer,
                    "tokens_used": tokens_used,
                    "model": self.settings.openai.openai_model,
                    "response_time": response_time
                }

                logger.info(f"Generated answer: {tokens_used} tokens, {response_time:.2f}s")

                # Log answer content at debug level
                answer_preview = answer[:300] + "..." if len(answer) > 300 else answer
                logger.debug(f"[GPT 답변]\n  질문: {question[:100]}{'...' if len(question) > 100 else ''}\n  답변: {answer_preview}")

                return result

            except Exception as e:
                logger.error(f"GPT generation attempt {attempt + 1} failed: {e}")
                if attempt == max_retries - 1:
                    raise GPTServiceError(
                        reason=f"Failed to generate answer after {max_retries} attempts: {str(e)}"
                    )
                # Exponential backoff
                time.sleep(2 ** attempt)

    async def agenerate_answer(
        self,
        question: str,
        context: Optional[str] = None,
        images: Optional[List[Dict[str, Any]]] = None,
        max_retries: int = 3
    ) -> Dict[str, Any]:
        """Async version of generate_answer.

        Args:
            question: The question text
            context: Optional context
            images: Optional list of dicts with 'data' (bytes) and 'mime_type' (str)
            max_retries: Maximum retry attempts
        """
        prompt = self.prompt_builder.create_answer_prompt(question, context)

        for attempt in range(max_retries):
            try:
                start_time = time.time()

                # Prepare messages
                messages = [
                    {
                        "role": "system",
                        "content": "You are a helpful assistant that provides practical life advice in Korean. Provide direct, concise answers in plain text without any markdown formatting."
                    }
                ]

                # Determine which model to use
                use_vision = images and len(images) > 0
                model = self.settings.openai.openai_vision_model if use_vision else self.settings.openai.openai_model

                # If images are provided, use vision format
                if use_vision:
                    import base64

                    content = [{"type": "text", "text": prompt}]

                    # Add images to content with proper mime type
                    for img_dict in images:
                        img_data = img_dict.get('data')
                        mime_type = img_dict.get('mime_type', 'image/jpeg')

                        # Encode image to base64
                        base64_image = base64.b64encode(img_data).decode('utf-8')

                        # Create data URL with correct mime type
                        content.append({
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:{mime_type};base64,{base64_image}"
                            }
                        })

                    messages.append({
                        "role": "user",
                        "content": content
                    })

                    logger.info(f"Using vision model ({model}) with {len(images)} image(s)")
                else:
                    # Text-only message
                    messages.append({
                        "role": "user",
                        "content": prompt
                    })

                response = await self.client.async_client.chat.completions.create(
                    model=model,
                    messages=messages,
                    max_tokens=self.settings.openai.openai_max_tokens,
                    temperature=self.settings.openai.openai_temperature,
                    top_p=0.9,
                    frequency_penalty=0.2,
                    presence_penalty=0.1
                )

                # Extract answer
                answer = response.choices[0].message.content.strip()
                tokens_used = response.usage.total_tokens if response.usage else 0
                response_time = time.time() - start_time

                result = {
                    "answer": answer,
                    "tokens_used": tokens_used,
                    "model": model,  # Use actual model used
                    "response_time": response_time
                }

                logger.info(f"Generated answer using {model}: {tokens_used} tokens, {response_time:.2f}s")
                return result

            except Exception as e:
                logger.error(f"Async GPT generation attempt {attempt + 1} failed: {e}")
                if attempt == max_retries - 1:
                    raise GPTServiceError(
                        reason=f"Failed to generate answer after {max_retries} attempts: {str(e)}"
                    )
                # Exponential backoff
                await asyncio.sleep(2 ** attempt)

    def generate_batch_answers(
        self,
        questions: List[Dict[str, str]]
    ) -> List[Dict[str, Any]]:
        """Generate answers for multiple questions."""
        results = []

        for item in questions:
            try:
                result = self.generate_answer(
                    question=item["question"]
                )
                results.append(result)
            except Exception as e:
                logger.error(f"Failed to generate answer for question: {e}")
                results.append({
                    "answer": "죄송합니다. 답변 생성 중 오류가 발생했습니다.",
                    "error": str(e)
                })

        return results

    def create_summary(
        self,
        text: str,
        max_length: int = 100
    ) -> str:
        """Create a summary of text using GPT."""
        try:
            prompt = self.prompt_builder.create_summary_prompt(text, max_length)

            response = self.client.sync_client.chat.completions.create(
                model=self.settings.openai.openai_model,
                messages=[
                    {
                        "role": "system",
                        "content": f"Summarize the following text in Korean. Maximum {max_length} characters."
                    },
                    {
                        "role": "user",
                        "content": text
                    }
                ],
                max_tokens=max_length * 2,  # Rough estimate for tokens
                temperature=0.3  # Lower temperature for more consistent summaries
            )

            summary = response.choices[0].message.content.strip()
            return summary[:max_length]  # Ensure length limit

        except Exception as e:
            logger.error(f"Summary generation failed: {e}")
            # Return truncated original text as fallback
            return text[:max_length] + "..." if len(text) > max_length else text

    def extract_keywords(
        self,
        text: str,
        num_keywords: int = 5
    ) -> List[str]:
        """Extract keywords from text using GPT."""
        try:
            prompt = self.prompt_builder.create_keyword_extraction_prompt(text, num_keywords)

            response = self.client.sync_client.chat.completions.create(
                model=self.settings.openai.openai_model,
                messages=[
                    {
                        "role": "system",
                        "content": f"Extract {num_keywords} key terms from the following Korean text. Return only the keywords separated by commas."
                    },
                    {
                        "role": "user",
                        "content": text
                    }
                ],
                max_tokens=100,
                temperature=0.3
            )

            keywords_str = response.choices[0].message.content.strip()
            keywords = [k.strip() for k in keywords_str.split(",")][:num_keywords]

            return keywords

        except Exception as e:
            logger.error(f"Keyword extraction failed: {e}")
            return []