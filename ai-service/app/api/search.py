from typing import List
from fastapi import APIRouter, Query, HTTPException
from pydantic import BaseModel, Field

from app.services.autocomplete import AutocompleteService
from app.core.logger import LoggerSetup


# 응답 모델
class AutocompleteResultSchema(BaseModel):
    keyword: str = Field(..., description="검색 키워드")
    answer_count: int = Field(..., description="답변 수")
    popularity_score: float = Field(..., description="인기도 점수")


class AutocompleteResponse(BaseModel):
    query: str = Field(..., description="검색 쿼리")
    results: List[AutocompleteResultSchema] = Field(..., description="검색 결과")
    total: int = Field(..., description="결과 개수")
    search_type: str = Field(..., description="검색 타입 (normal/chosung/fuzzy)")
    took_ms: float = Field(..., description="소요 시간 (밀리초)")


# 라우터 생성
router = APIRouter(prefix="/api/search", tags=["Search"])
logger = LoggerSetup.get_logger(__name__)


@router.get(
    "/autocomplete",
    response_model=AutocompleteResponse,
    summary="검색어 자동완성",
    description="검색어 자동완성 기능을 제공합니다. 일반 검색, 초성 검색(예: 'ㅊㅅ' → '침실'), Fuzzy 매칭(오타 허용), 인기도 기반 랭킹을 지원합니다.",
    responses={
        200: {
            "description": "자동완성 결과 반환",
            "content": {
                "application/json": {
                    "example": {
                        "query": "침실",
                        "results": [
                            {"keyword": "침실인테리어", "answer_count": 150, "popularity_score": 85.5},
                            {"keyword": "침실꾸미기", "answer_count": 120, "popularity_score": 78.2}
                        ],
                        "total": 2,
                        "search_type": "normal",
                        "took_ms": 45.3
                    }
                }
            }
        },
        500: {"description": "서버 오류"}
    }
)
async def autocomplete(
    query: str = Query(..., description="검색 쿼리 (최소 1자 이상)", min_length=1, example="침실"),
    limit: int = Query(10, description="반환할 최대 결과 개수 (1-50)", ge=1, le=50, example=10)
):
    try:
        service = AutocompleteService()

        result = await service.autocomplete(
            query=query,
            limit=limit,
            category=None,
            enable_fuzzy=True,
            enable_cache=True
        )

        await service.close()

        return AutocompleteResponse(
            query=result.query,
            results=[
                AutocompleteResultSchema(
                    keyword=r.keyword,
                    answer_count=r.answer_count,
                    popularity_score=r.popularity_score
                )
                for r in result.results
            ],
            total=result.total,
            search_type=result.search_type,
            took_ms=result.took_ms
        )

    except Exception as e:
        logger.error(f"자동완성 에러: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"자동완성 실패: {str(e)}")
