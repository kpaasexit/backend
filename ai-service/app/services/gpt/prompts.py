"""Prompt building utilities."""

from typing import Optional

from app.core.constants import CATEGORY_PERSONAS


class PromptBuilder:
    """Builds prompts for GPT models."""

    @staticmethod
    def create_answer_prompt(
        category: str,
        question: str,
        context: Optional[str] = None
    ) -> str:
        """Create prompt for answer generation."""
        # Get category persona
        persona = CATEGORY_PERSONAS.get(category, CATEGORY_PERSONAS["기타"])

        # Zero-Shot Chain-of-Thought prompt structure
        prompt = f"""{persona}

당신은 논리적 추론을 통해 정확한 답변을 제공하는 전문가입니다.

사고 과정:
1. 질문의 핵심 요구사항을 파악합니다
2. 관련 지식과 논리적 연결고리를 찾습니다
3. 단계별로 추론하여 최적의 답을 도출합니다

질문: {question}"""

        # Add context if provided (including conversation context)
        if context:
            prompt += f"\n\n{context}\n\n위 대화 맥락과 참고 정보를 고려하여 연계된 질문에 적절히 답변하세요. 이전 답변과 일관성을 유지하며 추가 정보를 제공하세요."

        # Add Zero-Shot reasoning and response guidelines
        prompt += """

먼저 간단히 추론 과정을 거친 후 답변하세요:

답변 지침:
1. 정답을 먼저 제시하세요
2. 핵심 내용만 간단명료하게 전달하세요
3. 질문에 대한 직접적인 답변만 제공하세요
4. 불필요한 서론이나 설명을 하지 마세요
5. 안전 주의사항이 있다면 답변에 간단히 포함하세요
6. 마크다운 문법(*, **, #, -, ` 등)을 사용하지 마세요
7. 순수 텍스트로만 답변하세요
"""

        return prompt

    @staticmethod
    def create_summary_prompt(text: str, max_length: int = 100) -> str:
        """Create prompt for text summarization."""
        return f"Summarize the following text in Korean. Maximum {max_length} characters.\n\n{text}"

    @staticmethod
    def create_keyword_extraction_prompt(text: str, num_keywords: int = 5) -> str:
        """Create prompt for keyword extraction."""
        return f"Extract {num_keywords} key terms from the following Korean text. Return only the keywords separated by commas.\n\n{text}"