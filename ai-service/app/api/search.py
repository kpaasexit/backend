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
    summary="검색어 자동완성 (접두사 매칭)",
    description="입력한 문자열로 시작하는 키워드만 반환합니다. 예: '고기' 입력 시 '고기', '고기국', '고기구이' 등 반환",
    responses={
        200: {
            "description": "자동완성 결과 반환",
            "content": {
                "application/json": {
                    "example": {
                        "query": "고기",
                        "results": [
                            {"keyword": "고기", "answer_count": 150, "popularity_score": 85.5},
                            {"keyword": "고기국", "answer_count": 120, "popularity_score": 78.2},
                            {"keyword": "고기구이", "answer_count": 100, "popularity_score": 75.0}
                        ],
                        "total": 3,
                        "search_type": "prefix",
                        "took_ms": 15.3
                    }
                }
            }
        },
        500: {"description": "서버 오류"}
    }
)
async def autocomplete(
    query: str = Query(..., description="검색 쿼리 (최소 1자 이상)", min_length=1, example="고기"),
    limit: int = Query(10, description="반환할 최대 결과 개수 (1-50)", ge=1, le=50, example=10)
):
    try:
        service = AutocompleteService()

        result = await service.autocomplete(
            query=query,
            limit=limit,
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


@router.get(
    "/related",
    response_model=AutocompleteResponse,
    summary="연관 검색어 (Fuzzy 매칭)",
    description="입력한 문자열과 유사한 키워드를 반환합니다. 오타를 허용하고 부분 매칭도 수행합니다. 예: '고기' 입력 시 '곡기', '고기류', '육고기' 등 반환",
    responses={
        200: {
            "description": "연관 검색어 결과 반환",
            "content": {
                "application/json": {
                    "example": {
                        "query": "고기",
                        "results": [
                            {"keyword": "고기", "answer_count": 150, "popularity_score": 85.5},
                            {"keyword": "곡기", "answer_count": 50, "popularity_score": 60.0},
                            {"keyword": "육고기", "answer_count": 80, "popularity_score": 70.5}
                        ],
                        "total": 3,
                        "search_type": "related",
                        "took_ms": 25.7
                    }
                }
            }
        },
        500: {"description": "서버 오류"}
    }
)
async def related_search(
    query: str = Query(..., description="검색 쿼리 (최소 1자 이상)", min_length=1, example="고기"),
    limit: int = Query(10, description="반환할 최대 결과 개수 (1-50)", ge=1, le=50, example=10)
):
    try:
        service = AutocompleteService()

        result = await service.related_search(
            query=query,
            limit=limit,
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
        logger.error(f"연관 검색어 에러: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"연관 검색어 조회 실패: {str(e)}")


@router.get(
    "/chosung",
    response_model=AutocompleteResponse,
    summary="초성 검색",
    description="초성으로 키워드를 검색합니다. 예: 'ㄱㄱ' 입력 시 '고기', '가구' 등 반환",
    responses={
        200: {
            "description": "초성 검색 결과 반환",
            "content": {
                "application/json": {
                    "example": {
                        "query": "ㄱㄱ",
                        "results": [
                            {"keyword": "고기", "answer_count": 150, "popularity_score": 85.5},
                            {"keyword": "가구", "answer_count": 120, "popularity_score": 78.2}
                        ],
                        "total": 2,
                        "search_type": "chosung",
                        "took_ms": 18.5
                    }
                }
            }
        },
        500: {"description": "서버 오류"}
    }
)
async def chosung_search(
    query: str = Query(..., description="초성 쿼리 (최소 1자 이상)", min_length=1, example="ㄱㄱ"),
    limit: int = Query(10, description="반환할 최대 결과 개수 (1-50)", ge=1, le=50, example=10)
):
    try:
        service = AutocompleteService()

        result = await service.chosung_search(
            query=query,
            limit=limit,
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
        logger.error(f"초성 검색 에러: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"초성 검색 실패: {str(e)}")
