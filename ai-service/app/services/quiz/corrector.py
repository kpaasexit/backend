import json
from typing import Optional, Dict, Any
from openai import AsyncOpenAI

from app.models.quiz import Quiz, QuizUpdate, QuizType
from app.services.quiz.service import QuizService
from app.core.logger import LoggerSetup
from app.config import get_settings

logger = LoggerSetup.get_logger(__name__)
settings = get_settings()


class QuizCorrectorService:
    """GPT를 활용한 Quiz 오류 수정 서비스"""

    def __init__(self):
        self.openai_client = AsyncOpenAI(api_key=settings.openai.openai_api_key)
        self.quiz_service = QuizService()

    async def correct_quiz(self, quiz_id: int, error_description: Optional[str] = None) -> Optional[Quiz]:
        """
        퀴즈 오류 수정

        Args:
            quiz_id: 수정할 퀴즈 ID
            error_description: 오류 설명 (옵션)

        Returns:
            수정된 Quiz 객체 또는 None
        """
        try:
            # 기존 퀴즈 조회
            existing_quiz = await self.quiz_service.get_quiz(quiz_id)
            if not existing_quiz:
                logger.error(f"Quiz not found: {quiz_id}")
                return None

            # GPT에게 수정 요청
            corrected_data = await self._request_correction_to_gpt(existing_quiz, error_description)
            if not corrected_data:
                return None

            # 퀴즈 업데이트
            quiz_update = QuizUpdate(
                quiz_title=corrected_data.get("quiz_title"),
                quiz_content=corrected_data.get("quiz_content"),
                quiz_correct_answer=corrected_data.get("quiz_correct_answer"),
                quiz_additional_information=corrected_data.get("quiz_additional_information")
            )

            updated_quiz = await self.quiz_service.update_quiz(quiz_id, quiz_update)
            logger.info(f"Successfully corrected quiz {quiz_id}")
            return updated_quiz

        except Exception as e:
            logger.error(f"Error correcting quiz {quiz_id}: {e}")
            return None

    async def _request_correction_to_gpt(self, quiz: Quiz, error_description: Optional[str] = None) -> Optional[Dict]:
        """GPT에게 퀴즈 수정 요청"""
        try:
            category_name = self.quiz_service.categories.get(quiz.quiz_category_id, "일반")

            prompt = self._build_correction_prompt(quiz, category_name, error_description)

            response = await self.openai_client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {
                        "role": "system",
                        "content": "You are a quiz correction assistant. Your job is to fix errors in quizzes while maintaining the original intent and category. Always respond with valid JSON."
                    },
                    {"role": "user", "content": prompt}
                ],
                response_format={"type": "json_object"},
                temperature=0.3  # 낮은 temperature로 일관성 있는 수정
            )

            corrected_data = json.loads(response.choices[0].message.content)
            return corrected_data

        except Exception as e:
            logger.error(f"Error requesting correction to GPT: {e}")
            return None

    def _build_correction_prompt(self, quiz: Quiz, category_name: str, error_description: Optional[str]) -> str:
        """수정 프롬프트 생성"""
        if quiz.quiz_type == QuizType.OX:
            quiz_type_desc = "OX 퀴즈 (O 또는 X)"
        else:
            quiz_type_desc = "4지선다 (A, B, C, D 중 택1)"

        error_info = f"\n\n보고된 오류: {error_description}" if error_description else ""

        return f"""다음 퀴즈에 오류가 있어 수정이 필요합니다:

카테고리: {category_name}
퀴즈 타입: {quiz_type_desc}

현재 퀴즈:
- 제목: {quiz.quiz_title}
- 내용: {quiz.quiz_content}
- 정답: {quiz.quiz_correct_answer}
- 추가 정보: {quiz.quiz_additional_information or "없음"}
{error_info}

다음을 확인하고 수정해주세요:
1. 문제의 정확성 (사실 관계가 올바른지)
2. 답변의 정확성 (정답이 맞는지)
3. 문장의 명확성 (모호하지 않고 이해하기 쉬운지)
4. 카테고리 적합성 ({category_name} 카테고리에 맞는지)
5. 형식 준수 (OX는 O/X, 4지선다는 A/B/C/D)

수정된 퀴즈를 다음 JSON 형식으로 응답해주세요:
{{
    "quiz_title": "수정된 제목",
    "quiz_content": "수정된 내용",
    "quiz_correct_answer": "수정된 정답 (O/X 또는 A/B/C/D)",
    "quiz_additional_information": "수정된 추가 정보",
    "changes_made": "수정한 내용 요약"
}}

주의사항:
- 퀴즈의 본질적인 내용은 유지하되, 오류만 수정하세요
- quiz_correct_answer는 반드시 형식에 맞게 (OX면 O 또는 X, 4지선다면 A/B/C/D 중 하나)
- 카테고리를 벗어나지 않도록 주의하세요"""



# 싱글톤 인스턴스
_quiz_corrector = None

def get_quiz_corrector() -> QuizCorrectorService:
    """Quiz 수정 서비스 싱글톤 인스턴스 반환"""
    global _quiz_corrector
    if _quiz_corrector is None:
        _quiz_corrector = QuizCorrectorService()
    return _quiz_corrector