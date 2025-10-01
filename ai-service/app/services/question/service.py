"""Question processing service."""

from typing import Dict, List, Optional, Tuple
import asyncio
import numpy as np

from app.config import get_settings
from app.core.logger import LoggerSetup
from app.models.classifier import get_classifier
from app.models.embedder import get_embedder
from app.services.gpt import get_gpt_service
from app.services.cache import get_cache_service


logger = LoggerSetup.get_logger(__name__)


class QuestionService:
    """
    Main service for processing questions.

    Integrates:
    - Category classification
    - Embedding generation
    - AI answer generation
    - Caching
    """

    def __init__(self):
        self.settings = get_settings()
        self.classifier = get_classifier()
        self.embedder = get_embedder()
        self.gpt_service = get_gpt_service()
        self.cache_service = get_cache_service()

    def process_question(
        self,
        title: str,
        content: Optional[str] = None,
        generate_answer: bool = True
    ) -> Dict[str, any]:
        """
        Process a question through the complete pipeline.

        Args:
            title: Question title
            content: Optional question content
            generate_answer: Whether to generate AI answer

        Returns:
            Dictionary with processing results
        """
        result = {
            "title": title,
            "content": content
        }

        try:
            # Step 1: Classify category
            category, confidence = self.classifier.classify(title)
            result["category"] = category
            result["category_confidence"] = confidence
            logger.info(f"Classified as {category} (confidence: {confidence:.2f})")

            # Step 2: Generate embeddings
            if content:
                # Multi-layer embedding for title + content
                embedding = self.embedder.embed_multilayer(title, content)
            else:
                # Single embedding for title only
                embedding = self.embedder.embed(title)

            result["embedding"] = embedding.tolist()
            result["embedding_dim"] = len(embedding)

            # Step 3: Generate AI answer (optional)
            if generate_answer:
                answer_result = self.gpt_service.generate_answer(
                    category=category,
                    question=title + (f"\n\n{content}" if content else "")
                )
                result["answer"] = answer_result["answer"]
                result["tokens_used"] = answer_result["tokens_used"]

            result["success"] = True

        except Exception as e:
            logger.error(f"Question processing failed: {e}")
            result["success"] = False
            result["error"] = str(e)

        return result

    async def aprocess_question(
        self,
        title: str,
        content: Optional[str] = None,
        generate_answer: bool = True
    ) -> Dict[str, any]:
        """
        Async version of process_question.

        Args:
            title: Question title
            content: Optional question content
            generate_answer: Whether to generate AI answer

        Returns:
            Dictionary with processing results
        """
        result = {
            "title": title,
            "content": content
        }

        try:
            # Run classification and embedding in parallel
            tasks = [
                asyncio.create_task(
                    asyncio.get_event_loop().run_in_executor(
                        None,
                        self.classifier.classify,
                        title
                    )
                ),
                asyncio.create_task(
                    asyncio.get_event_loop().run_in_executor(
                        None,
                        self.embedder.embed_multilayer if content else self.embedder.embed,
                        title, content if content else None
                    )
                )
            ]

            results = await asyncio.gather(*tasks)

            # Process classification result
            category, confidence = results[0]
            result["category"] = category
            result["category_confidence"] = confidence

            # Process embedding result
            embedding = results[1] if content else results[1]
            result["embedding"] = embedding.tolist()
            result["embedding_dim"] = len(embedding)

            # Generate AI answer if requested
            if generate_answer:
                answer_result = await self.gpt_service.agenerate_answer(
                    category=category,
                    question=title + (f"\n\n{content}" if content else "")
                )
                result["answer"] = answer_result["answer"]
                result["tokens_used"] = answer_result["tokens_used"]

            result["success"] = True

        except Exception as e:
            logger.error(f"Async question processing failed: {e}")
            result["success"] = False
            result["error"] = str(e)

        return result

    def batch_process_questions(
        self,
        questions: List[Dict[str, str]],
        generate_answers: bool = False
    ) -> List[Dict[str, any]]:
        """
        Process multiple questions in batch.

        Args:
            questions: List of question dictionaries
            generate_answers: Whether to generate AI answers

        Returns:
            List of processing results
        """
        results = []

        # Extract titles for batch processing
        titles = [q.get("title", "") for q in questions]

        # Batch classify
        try:
            classifications = self.classifier.batch_classify(titles)
        except Exception as e:
            logger.error(f"Batch classification failed: {e}")
            classifications = [(None, 0.0)] * len(titles)

        # Batch embed
        try:
            embeddings = self.embedder.batch_embed(titles)
        except Exception as e:
            logger.error(f"Batch embedding failed: {e}")
            embeddings = [None] * len(titles)

        # Process each question
        for i, question in enumerate(questions):
            result = {
                "title": question.get("title"),
                "content": question.get("content"),
                "category": classifications[i][0] if classifications[i][0] else "기타",
                "category_confidence": classifications[i][1],
            }

            if embeddings[i] is not None:
                result["embedding"] = embeddings[i].tolist()
                result["embedding_dim"] = len(embeddings[i])

            # Generate answer if requested
            if generate_answers and classifications[i][0]:
                try:
                    answer_result = self.gpt_service.generate_answer(
                        category=classifications[i][0],
                        question=question.get("title", "")
                    )
                    result["answer"] = answer_result["answer"]
                    result["tokens_used"] = answer_result["tokens_used"]
                except Exception as e:
                    logger.error(f"Answer generation failed for question {i}: {e}")
                    result["answer"] = None

            results.append(result)

        return results

    def find_similar_questions(
        self,
        query_embedding: np.ndarray,
        embeddings: List[np.ndarray],
        titles: List[str],
        categories: Optional[List[str]] = None,
        category_filter: Optional[str] = None,
        limit: int = 10
    ) -> List[Dict[str, any]]:
        """
        Find similar questions based on embeddings.

        Args:
            query_embedding: Query embedding
            embeddings: List of candidate embeddings
            titles: List of titles corresponding to embeddings
            categories: Optional list of categories
            category_filter: Optional category to filter by
            limit: Maximum number of results

        Returns:
            List of similar questions with similarity scores
        """
        similarities = []

        for i, embedding in enumerate(embeddings):
            # Apply category filter if specified
            if category_filter and categories and categories[i] != category_filter:
                continue

            # Calculate similarity
            similarity = self.embedder.compute_similarity(query_embedding, embedding)

            similarities.append({
                "index": i,
                "title": titles[i],
                "category": categories[i] if categories else None,
                "similarity": similarity
            })

        # Sort by similarity (descending)
        similarities.sort(key=lambda x: x["similarity"], reverse=True)

        return similarities[:limit]

    def extract_question_keywords(
        self,
        title: str,
        content: Optional[str] = None,
        num_keywords: int = 5
    ) -> List[str]:
        """
        Extract keywords from question.

        Args:
            title: Question title
            content: Optional question content
            num_keywords: Number of keywords to extract

        Returns:
            List of keywords
        """
        text = title
        if content:
            text += f" {content}"

        return self.gpt_service.extract_keywords(text, num_keywords)

    def summarize_question(
        self,
        title: str,
        content: str,
        max_length: int = 100
    ) -> str:
        """
        Create a summary of question content.

        Args:
            title: Question title
            content: Question content
            max_length: Maximum summary length

        Returns:
            Summary text
        """
        full_text = f"{title}\n\n{content}"
        return self.gpt_service.create_summary(full_text, max_length)

    def validate_question(
        self,
        title: str,
        content: Optional[str] = None
    ) -> Tuple[bool, Optional[str]]:
        """
        Validate question input.

        Args:
            title: Question title
            content: Optional question content

        Returns:
            Tuple of (is_valid, error_message)
        """
        # Check title
        if not title or not title.strip():
            return False, "Title is required"

        if len(title.strip()) < 5:
            return False, "Title too short (minimum 5 characters)"

        if len(title) > 500:
            return False, "Title too long (maximum 500 characters)"

        # Check content if provided
        if content:
            if len(content) > 10000:
                return False, "Content too long (maximum 10000 characters)"

        return True, None

    def preload_models(self) -> None:
        """Preload all models for faster processing."""
        logger.info("Preloading models...")

        try:
            self.classifier.preload_models()
            self.embedder.preload_models()
            logger.info("Models preloaded successfully")
        except Exception as e:
            logger.error(f"Model preloading failed: {e}")

    def clear_caches(self) -> None:
        """Clear all service caches."""
        self.classifier.clear_cache()
        self.embedder.clear_cache()
        self.cache_service.clear_pattern("nlp_service:*")
        logger.info("All caches cleared")


# Global question service instance
_question_service: Optional[QuestionService] = None


def get_question_service() -> QuestionService:
    """Get or create global question service instance."""
    global _question_service
    if _question_service is None:
        _question_service = QuestionService()
    return _question_service