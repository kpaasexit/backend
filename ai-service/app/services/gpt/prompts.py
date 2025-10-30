"""Prompt building utilities."""

from typing import Optional

from app.core.constants import CATEGORY_PERSONAS


class PromptBuilder:
    """Builds prompts for GPT models."""

    @staticmethod
    def create_answer_prompt(
        question: str,
        context: Optional[str] = None,
        has_images: bool = False
    ) -> str:
        """Create prompt for answer generation following GPT-4.1 best practices.

        Args:
            question: User's question
            context: Optional conversation context
            has_images: Whether images are attached to the question
        """
        # Use a general persona for all questions
        persona = CATEGORY_PERSONAS.get("기타", "당신은 친절하고 유용한 AI 어시스턴트입니다.")

        # GPT-4.1 optimized prompt structure
        # Section 1: Role and Objective (at the top for long context optimization)
        prompt = f"""# 역할 및 목표
{persona}

당신은 실생활 질문에 대해 정확하고 실용적인 답변을 제공하는 전문가입니다.

# 지침
## 답변 원칙
- 질문의 핵심 의도를 정확히 파악하여 직접적으로 답변합니다
- 사실에 기반하여 신뢰할 수 있는 정보를 제공합니다
- 불확실한 내용은 추측하지 않고 명확히 표현합니다
- 이전 대화 맥락이 있다면 일관성을 유지하며 연결된 답변을 제공합니다

## 답변 형식
- 순수 텍스트만 사용합니다 (마크다운 문법 사용 금지: *, **, #, -, `, [] 등)
- 불필요한 인사말이나 서론 없이 바로 핵심 답변을 시작합니다
- 자세하고 충분하게 설명합니다 (간결함보다 완전성과 이해도를 우선시)
- 안전/주의사항이 필요한 경우 답변에 자연스럽게 포함합니다
- 공손하고 친절한 어조를 유지합니다

## 답변 품질 기준
- 정확성: 검증된 정보만 제공
- 완전성: 질문에 완전히 답변하고 충분한 설명 제공
- 명확성: 이해하기 쉽게 자세히 설명
- 실용성: 실제로 적용 가능한 구체적인 조언

"""

        # Add image analysis requirement if images are present
        if has_images:
            prompt += """## 이미지 분석 필수 사항
- 제공된 이미지를 반드시 상세히 분석하세요
- 이미지에서 관찰되는 모든 중요한 세부사항을 설명하세요
- 이미지 내용을 바탕으로 질문에 답변하세요
- 이미지에서 확인할 수 있는 구체적인 요소들을 언급하세요
- 이미지 분석 없이 일반적인 답변만 제공하지 마세요

"""

        # Section 2: Context (if provided - placed in the middle for long context)
        if context:
            prompt += f"""# 이전 대화 맥락
{context}

위 대화 기록을 참고하여 현재 질문에 답변하세요. 이전 대화와 일관성을 유지하면서 추가 정보나 후속 답변을 제공해야 합니다.

"""

        # Section 3: User Question
        prompt += f"""# 사용자 질문
{question}

"""

        # Section 4: Reasoning Strategy (Chain-of-Thought)
        prompt += """# 추론 전략
답변하기 전에 다음 단계를 따라 생각하세요:

1. 질문 분석: 사용자가 실제로 무엇을 알고 싶어하는지 핵심 의도 파악
2. 맥락 고려: 제공된 대화 기록이나 추가 정보 검토
3. 지식 활용: 관련된 정확한 정보와 논리적 연결고리 찾기
4. 답변 구성: 핵심을 먼저 제시하고 필요한 세부사항 추가
5. 품질 확인: 정확성, 완전성, 명확성 검증

"""

        # Section 5: Final Instructions (at the bottom for long context optimization)
        prompt += """# 최종 지침
위 추론 전략을 따라 단계별로 생각한 후, 사용자 질문에 대한 답변을 작성하세요.

답변 시 반드시 지켜야 할 사항:
- 마크다운 문법을 절대 사용하지 마세요
- 순수 텍스트로만 답변하세요
- 불필요한 서론 없이 바로 핵심 답변을 시작하세요
- 공손하고 친절한 어조를 유지하세요
- 정확하고 검증된 정보만 제공하세요

이제 답변을 작성하세요."""

        return prompt

    @staticmethod
    def create_summary_prompt(text: str, max_length: int = 100) -> str:
        """Create prompt for text summarization."""
        return f"Summarize the following text in Korean. Maximum {max_length} characters.\n\n{text}"

    @staticmethod
    def create_keyword_extraction_prompt(text: str, num_keywords: int = 5) -> str:
        """Create prompt for keyword extraction."""
        return f"Extract {num_keywords} key terms from the following Korean text. Return only the keywords separated by commas.\n\n{text}"