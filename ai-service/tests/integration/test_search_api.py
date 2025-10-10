import pytest
from fastapi.testclient import TestClient

from app.main import create_app


@pytest.fixture
def client():
    app = create_app()
    return TestClient(app)


class TestSearchAPI:

    def test_autocomplete_basic(self, client):
        response = client.get("/api/search/autocomplete?query=침실&limit=5")

        assert response.status_code == 200
        data = response.json()

        assert "query" in data
        assert "results" in data
        assert "total" in data
        assert "search_type" in data
        assert "took_ms" in data

        assert data["query"] == "침실"
        assert isinstance(data["results"], list)
        assert data["total"] >= 0

    def test_autocomplete_chosung(self, client):
        response = client.get("/api/search/autocomplete?query=ㅊㅅ&limit=5")

        assert response.status_code == 200
        data = response.json()

        assert data["query"] == "ㅊㅅ"
        # 초성 검색이 정상 작동하면 search_type이 'chosung' 또는 'error' (ES 미연결시)
        assert data["search_type"] in ['chosung', 'error']

    def test_autocomplete_with_category(self, client):
        response = client.get(
            "/api/search/autocomplete?query=침실&category=이사/인테리어&limit=5"
        )

        assert response.status_code == 200
        data = response.json()

        # 결과가 있으면 모두 해당 카테고리여야 함
        for result in data["results"]:
            assert result["category"] == "이사/인테리어"

    def test_autocomplete_invalid_query(self, client):
        # 빈 쿼리
        response = client.get("/api/search/autocomplete?query=")

        # 400 또는 422 에러 예상
        assert response.status_code in [400, 422]

    def test_autocomplete_limit_validation(self, client):
        # 최대 limit 초과
        response = client.get("/api/search/autocomplete?query=침실&limit=100")

        assert response.status_code in [200, 422]  # 서버 설정에 따라 다름

        # 음수 limit
        response = client.get("/api/search/autocomplete?query=침실&limit=-1")

        assert response.status_code == 422

    def test_suggest(self, client):
        response = client.get("/api/search/suggest?query=침실&limit=3")

        assert response.status_code == 200
        data = response.json()

        assert "query" in data
        assert "keywords" in data

        assert data["query"] == "침실"
        assert isinstance(data["keywords"], list)
        assert len(data["keywords"]) <= 3

    def test_trending_keywords(self, client):
        response = client.get("/api/search/trending?limit=10")

        assert response.status_code == 200
        data = response.json()

        assert "results" in data
        assert isinstance(data["results"], list)
        assert len(data["results"]) <= 10

        # 인기도 순 정렬 확인
        if len(data["results"]) >= 2:
            scores = [r["popularity_score"] for r in data["results"]]
            assert scores == sorted(scores, reverse=True)

    def test_trending_keywords_with_category(self, client):
        response = client.get("/api/search/trending?limit=5&category=이사/인테리어")

        assert response.status_code == 200
        data = response.json()

        # 결과가 있으면 모두 해당 카테고리여야 함
        for result in data["results"]:
            assert result["category"] == "이사/인테리어"

    def test_categories(self, client):
        response = client.get("/api/search/categories")

        assert response.status_code == 200
        data = response.json()

        assert "categories" in data
        assert isinstance(data["categories"], list)

        # 카테고리가 있으면 구조 검증
        if data["categories"]:
            category = data["categories"][0]
            assert "category" in category
            assert "count" in category
            assert isinstance(category["count"], int)
            assert category["count"] >= 0

    def test_fuzzy_matching(self, client):
        # 정상 쿼리
        response1 = client.get("/api/search/autocomplete?query=침실인테리어&limit=5")
        assert response1.status_code == 200

        # 오타가 있는 쿼리 (Fuzzy 활성화)
        response2 = client.get("/api/search/autocomplete?query=침실인데리어&enable_fuzzy=true&limit=5")
        assert response2.status_code == 200

        # Fuzzy 비활성화
        response3 = client.get("/api/search/autocomplete?query=침실인데리어&enable_fuzzy=false&limit=5")
        assert response3.status_code == 200

    def test_cache_functionality(self, client):
        # 첫 번째 요청 (캐시 미스)
        response1 = client.get("/api/search/autocomplete?query=침실&limit=5&enable_cache=true")
        assert response1.status_code == 200
        data1 = response1.json()

        # 두 번째 요청 (캐시 히트)
        response2 = client.get("/api/search/autocomplete?query=침실&limit=5&enable_cache=true")
        assert response2.status_code == 200
        data2 = response2.json()

        # 같은 결과여야 함
        assert data1["query"] == data2["query"]
        assert data1["total"] == data2["total"]

        # 캐시 비활성화
        response3 = client.get("/api/search/autocomplete?query=침실&limit=5&enable_cache=false")
        assert response3.status_code == 200
