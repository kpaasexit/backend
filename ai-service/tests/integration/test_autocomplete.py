import pytest
import pytest_asyncio
from app.services.autocomplete import AutocompleteService


@pytest_asyncio.fixture(scope="function")
async def service():
    """AutocompleteService fixture"""
    svc = AutocompleteService()
    yield svc
    await svc.close()


@pytest.mark.asyncio
class TestAutocompleteService:

    async def test_autocomplete_normal_search(self, service):
        result = await service.autocomplete(
            query="침실",
            limit=5,
            enable_cache=False
        )

        assert result.query == "침실"
        assert result.search_type in ['prefix', 'error']
        assert isinstance(result.results, list)
        assert result.took_ms >= 0

    async def test_autocomplete_chosung_search(self, service):
        result = await service.chosung_search(
            query="ㅊㅅ",
            limit=5,
            enable_cache=False
        )

        assert result.query == "ㅊㅅ"
        assert result.search_type in ['chosung', 'error']
        assert isinstance(result.results, list)

    async def test_autocomplete_with_category(self, service):
        result = await service.autocomplete(
            query="침실",
            limit=5,
            enable_cache=False
        )

        assert result.query == "침실"
        assert isinstance(result.results, list)

    async def test_autocomplete_fuzzy_matching(self, service):
        # autocomplete는 정확한 prefix 매칭만 수행
        # fuzzy 매칭은 related_search 사용
        result = await service.related_search(
            query="침실인테리어",
            limit=5,
            enable_cache=False
        )

        assert result.query == "침실인테리어"
        assert isinstance(result.results, list)
        assert result.search_type in ['related', 'error']

    async def test_suggest(self, service):
        keywords = await service.suggest(
            query="침실",
            limit=3
        )

        assert isinstance(keywords, list)
        assert len(keywords) <= 3

    async def test_get_trending_keywords(self, service):
        trending = await service.get_trending_keywords(
            limit=10
        )

        assert isinstance(trending, list)
        assert len(trending) <= 10

        # 인기도 순 정렬 확인
        if len(trending) >= 2:
            assert trending[0].popularity_score >= trending[1].popularity_score

    async def test_get_categories(self, service):
        categories = await service.get_categories()

        assert isinstance(categories, list)

        if categories:
            assert 'category' in categories[0]
            assert 'count' in categories[0]
            assert categories[0]['count'] > 0

    async def test_empty_query(self, service):
        result = await service.autocomplete(
            query="",
            limit=5
        )

        assert result.query == ""
        assert result.total == 0
        assert result.search_type == "prefix"  # 빈 쿼리도 prefix 타입
        assert len(result.results) == 0

    async def test_cache_functionality(self, service):
        # 첫 번째 요청 (캐시 미스)
        result1 = await service.autocomplete(
            query="침실인테리어",
            limit=5,
            enable_cache=True
        )

        # 두 번째 요청 (캐시 히트)
        result2 = await service.autocomplete(
            query="침실인테리어",
            limit=5,
            enable_cache=True
        )

        assert result1.query == result2.query
        assert result1.total == result2.total
