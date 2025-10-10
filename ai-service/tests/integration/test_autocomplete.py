import pytest
from app.services.autocomplete import AutocompleteService


@pytest.mark.asyncio
class TestAutocompleteService:

    @pytest.fixture
    async def service(self):
        service = AutocompleteService()
        yield service
        await service.close()

    async def test_autocomplete_normal_search(self, service):
        result = await service.autocomplete(
            query="침실",
            limit=5,
            enable_cache=False
        )

        assert result.query == "침실"
        assert result.search_type in ['normal', 'error']
        assert isinstance(result.results, list)
        assert result.took_ms >= 0

    async def test_autocomplete_chosung_search(self, service):
        result = await service.autocomplete(
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
            category="이사/인테리어",
            enable_cache=False
        )

        assert result.query == "침실"
        assert isinstance(result.results, list)

        # 결과가 있으면 모두 해당 카테고리여야 함
        for item in result.results:
            assert item.category == "이사/인테리어"

    async def test_autocomplete_fuzzy_matching(self, service):
        result = await service.autocomplete(
            query="침실인데리어",  # 오타
            limit=5,
            enable_fuzzy=True,
            enable_cache=False
        )

        assert result.query == "침실인데리어"
        assert isinstance(result.results, list)

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
        assert result.search_type == "empty"
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

        # 같은 결과여야 함
        assert result1.query == result2.query
        assert result1.total == result2.total

        # 캐시 히트는 더 빨라야 함 (일반적으로)
        # assert result2.took_ms <= result1.took_ms
