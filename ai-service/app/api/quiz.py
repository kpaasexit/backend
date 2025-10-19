import random
from typing import List
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field

from app.services.quiz import QuizService
from app.models.quiz import Quiz, QuizGenerationRequest, QuizType
from app.core.logger import LoggerSetup


# 응답 모델
class QuizSchema(BaseModel):
    quiz_id: int = Field(..., description="퀴즈 ID")
    quiz_category_id: int = Field(..., description="카테고리 ID (1-8)")
    quiz_title: str | None = Field(None, description="퀴즈 제목 (OX 퀴즈의 경우 null)")
    quiz_content: str = Field(..., description="퀴즈 내용")
    quiz_type: QuizType = Field(..., description="퀴즈 타입 (OX/MULTIPLE)")
    quiz_correct_answer: int = Field(..., description="정답 (OX: 0(정답)/1(오답), MULTIPLE: 0-3)")
    explanation: str = Field(None, description="정답 설명")

    class Config:
        from_attributes = True


class QuizGenerationResponse(BaseModel):
    success: bool = Field(..., description="생성 성공 여부")
    count: int = Field(..., description="생성된 퀴즈 개수")
    quizzes: List[QuizSchema] = Field(..., description="생성된 퀴즈 목록")
    message: str = Field(..., description="응답 메시지")


# 라우터 생성
router = APIRouter(prefix="/api/quiz", tags=["Quiz"])
logger = LoggerSetup.get_logger(__name__)


@router.post(
    "/generate",
    response_model=QuizGenerationResponse,
    summary="퀴즈 랜덤 생성 (최대 10개)",
    description="""
지정한 개수만큼 퀴즈를 랜덤으로 생성합니다.

### 특징:
- **카테고리**: 1-8 중 랜덤 선택
- **퀴즈 타입**: OX 또는 4지선다 랜덤 선택
- **대상**: 20대 사회초년생, 독립 생활자를 위한 실용적인 문제
- **중복 방지**: RAG 기반으로 기존 퀴즈와 중복 방지
- **상세한 설명**: 정답 근거와 배경 지식 포함
    """,
    responses={
        200: {
            "description": "퀴즈 생성 성공",
            "content": {
                "application/json": {
                    "example": {
                        "success": True,
                        "count": 10,
                        "quizzes": [
                            {
                                "quiz_id": 1234567890,
                                "quiz_category_id": 3,
                                "quiz_title": "계란의 신선도를 확인하는 가장 정확한 방법은?",
                                "quiz_content": "물에 띄워본다\n냄새를 맡는다\n깨서 확인한다\n흔들어본다",
                                "quiz_type": "MULTIPLE",
                                "quiz_correct_answer": 0,
                                "explanation": "계란을 물에 넣었을 때 가라앉으면 신선하고..."
                            }
                        ],
                        "message": "10개의 퀴즈가 성공적으로 생성되었습니다."
                    }
                }
            }
        },
        400: {"description": "잘못된 요청 (개수 범위 초과 등)"},
        500: {"description": "서버 오류"}
    }
)
async def generate_quizzes(
    count: int = Query(10, ge=1, le=10, description="생성할 퀴즈 개수 (1-10)", example=10)
):
    """
    지정한 개수만큼 퀴즈를 랜덤으로 생성합니다.
    카테고리(1-8)와 퀴즈 타입(OX/MULTIPLE)은 자동으로 랜덤 선택됩니다.

    - **count**: 생성할 퀴즈 개수 (기본값: 10, 최대 10개)
    """
    try:
        logger.info(f"랜덤 퀴즈 생성 요청: count={count}")

        service = QuizService()
        all_quizzes = []

        # 각 퀴즈마다 랜덤 카테고리와 타입 선택
        for i in range(count):
            random_category = random.randint(1, 8)
            random_quiz_type = random.choice([QuizType.OX, QuizType.MULTIPLE])

            logger.info(f"퀴즈 {i+1}/{count} 생성 중 - category: {random_category}, type: {random_quiz_type}")

            request = QuizGenerationRequest(
                category_id=random_category,
                count=1,
                quiz_type=random_quiz_type
            )

            quizzes = await service.generate_quiz(request)
            all_quizzes.extend(quizzes)

        logger.info(f"랜덤 퀴즈 생성 완료: {len(all_quizzes)}개 생성됨")

        return QuizGenerationResponse(
            success=True,
            count=len(all_quizzes),
            quizzes=[
                QuizSchema(
                    quiz_id=q.quiz_id,
                    quiz_category_id=q.quiz_category_id,
                    quiz_title=None if q.quiz_type == QuizType.OX else q.quiz_title,
                    quiz_content=q.quiz_content,
                    quiz_type=q.quiz_type,
                    quiz_correct_answer=q.quiz_correct_answer,
                    explanation=q.explanation
                )
                for q in all_quizzes
            ],
            message=f"{len(all_quizzes)}개의 퀴즈가 성공적으로 생성되었습니다."
        )

    except ValueError as e:
        logger.error(f"잘못된 요청: {e}")
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error(f"퀴즈 생성 에러: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"퀴즈 생성 실패: {str(e)}")
