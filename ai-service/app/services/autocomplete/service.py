import os
from typing import List, Optional, Dict, Any
from datetime import datetime

from elasticsearch import AsyncElasticsearch
from pydantic import BaseModel

from app.utils.chosung import is_chosung_query, extract_chosung, normalize_chosung_query
from app.services.cache import CacheService
from app.core.logger import LoggerSetup


# 응답 모델
class AutocompleteResult(BaseModel):
    keyword: str
    category: str
    title: str
    answer_count: int
    popularity_score: float
    matched_by: str  # 'keyword', 'title', 'chosung', 'fuzzy'


class AutocompleteResponse(BaseModel):
    query: str
    results: List[AutocompleteResult]
    total: int
    search_type: str  # 'normal', 'chosung', 'fuzzy'
    took_ms: float


class AutocompleteService:

    INDEX_NAME = "naver_kin_autocomplete"

    def __init__(self):
        self.logger = LoggerSetup.get_logger(__name__)
        self.cache = CacheService()

        # Elasticsearch 클라이언트 초기화
        es_host = os.getenv("ELASTICSEARCH_HOST", "localhost")
        es_port = int(os.getenv("ELASTICSEARCH_PORT", "9200"))

        self.es = AsyncElasticsearch(
            [f"http://{es_host}:{es_port}"],
            retry_on_timeout=True,
            max_retries=3
        )

        self.logger.info(f"Elasticsearch 연결: {es_host}:{es_port}")

    async def close(self):
        await self.es.close()

    async def _check_connection(self) -> bool:
        try:
            return await self.es.ping()
        except Exception as e:
            self.logger.error(f"Elasticsearch 연결 실패: {e}")
            return False

    async def autocomplete(
        self,
        query: str,
        limit: int = 10,
        category: Optional[str] = None,
        enable_fuzzy: bool = True,
        enable_cache: bool = True
    ) -> AutocompleteResponse:
        start_time = datetime.now()

        # 쿼리 정규화
        normalized_query = query.strip()
        if not normalized_query:
            return AutocompleteResponse(
                query=query,
                results=[],
                total=0,
                search_type='empty',
                took_ms=0.0
            )

        # 캐시 확인
        cache_key = f"autocomplete:{normalized_query}:{limit}:{category}"
        if enable_cache:
            cached_result = await self.cache.aget(cache_key)
            if cached_result:
                self.logger.debug(f"캐시 히트: {cache_key}")
                return AutocompleteResponse(**cached_result)

        # Elasticsearch 연결 확인
        if not await self._check_connection():
            self.logger.error("Elasticsearch 연결 불가")
            return AutocompleteResponse(
                query=query,
                results=[],
                total=0,
                search_type='error',
                took_ms=0.0
            )

        # 검색 타입 결정
        if is_chosung_query(normalized_query):
            search_type = 'chosung'
            results = await self._search_by_chosung(normalized_query, limit, category)
        else:
            search_type = 'normal'
            results = await self._search_normal(normalized_query, limit, category, enable_fuzzy)

        # 응답 생성
        took_ms = (datetime.now() - start_time).total_seconds() * 1000
        response = AutocompleteResponse(
            query=query,
            results=results,
            total=len(results),
            search_type=search_type,
            took_ms=took_ms
        )

        # 캐시 저장
        if enable_cache:
            await self.cache.aset(cache_key, response.model_dump(), ttl=300)  # 5분 캐싱

        return response

    async def _search_normal(
        self,
        query: str,
        limit: int,
        category: Optional[str],
        enable_fuzzy: bool
    ) -> List[AutocompleteResult]:
        # Multi-match 쿼리 구성
        must_queries = []

        # 1. 키워드 매칭 (가중치 높음)
        keyword_query = {
            "multi_match": {
                "query": query,
                "fields": [
                    "keyword^5",           # 정확한 키워드 매칭
                    "keyword.edge_ngram^3",  # 접두사 매칭
                    "keyword.ngram^2",       # 부분 매칭
                    "title.ngram"            # 제목 부분 매칭
                ],
                "type": "best_fields",
                "minimum_should_match": "70%"
            }
        }

        should_queries = [keyword_query]

        # 2. Fuzzy 매칭 (오타 허용)
        if enable_fuzzy:
            fuzzy_query = {
                "multi_match": {
                    "query": query,
                    "fields": ["keyword", "title"],
                    "fuzziness": "AUTO",
                    "prefix_length": 1,
                    "max_expansions": 50
                }
            }
            should_queries.append(fuzzy_query)

        # 3. 카테고리 필터
        if category:
            must_queries.append({
                "term": {
                    "category.keyword": category
                }
            })

        # 최종 쿼리 구성 - aggregation 사용하여 중복 제거
        search_body = {
            "size": 0,  # 상위 히트는 필요 없음
            "query": {
                "bool": {
                    "must": must_queries,
                    "should": should_queries,
                    "minimum_should_match": 1
                }
            },
            "aggs": {
                "unique_keywords": {
                    "terms": {
                        "field": "keyword.keyword",
                        "size": limit,
                        "order": {"max_popularity": "desc"}
                    },
                    "aggs": {
                        "max_popularity": {
                            "max": {
                                "field": "popularity_score"
                            }
                        },
                        "top_doc": {
                            "top_hits": {
                                "size": 1,
                                "sort": [
                                    {"popularity_score": {"order": "desc"}},
                                    {"answer_count": {"order": "desc"}}
                                ],
                                "_source": ["keyword", "category", "title", "answer_count", "popularity_score"]
                            }
                        }
                    }
                }
            }
        }

        try:
            response = await self.es.search(index=self.INDEX_NAME, body=search_body)
            results = []

            buckets = response.get('aggregations', {}).get('unique_keywords', {}).get('buckets', [])

            for bucket in buckets:
                top_hit = bucket['top_doc']['hits']['hits'][0]['_source']
                results.append(AutocompleteResult(
                    keyword=top_hit['keyword'],
                    category=top_hit['category'],
                    title=top_hit['title'],
                    answer_count=top_hit['answer_count'],
                    popularity_score=top_hit['popularity_score'],
                    matched_by='keyword'
                ))

            return results

        except Exception as e:
            self.logger.error(f"일반 검색 에러: {e}")
            return []

    async def _search_by_chosung(
        self,
        query: str,
        limit: int,
        category: Optional[str]
    ) -> List[AutocompleteResult]:
        # 쿼리 정규화 (초성만 추출)
        normalized_chosung = normalize_chosung_query(query)

        must_queries = []

        # 초성 매칭
        chosung_query = {
            "match": {
                "keyword_chosung": {
                    "query": normalized_chosung,
                    "fuzziness": "AUTO"
                }
            }
        }

        must_queries.append(chosung_query)

        # 카테고리 필터
        if category:
            must_queries.append({
                "term": {
                    "category.keyword": category
                }
            })

        # aggregation 사용하여 중복 제거
        search_body = {
            "size": 0,
            "query": {
                "bool": {
                    "must": must_queries
                }
            },
            "aggs": {
                "unique_keywords": {
                    "terms": {
                        "field": "keyword.keyword",
                        "size": limit,
                        "order": {"max_popularity": "desc"}
                    },
                    "aggs": {
                        "max_popularity": {
                            "max": {
                                "field": "popularity_score"
                            }
                        },
                        "top_doc": {
                            "top_hits": {
                                "size": 1,
                                "sort": [
                                    {"popularity_score": {"order": "desc"}},
                                    {"answer_count": {"order": "desc"}}
                                ],
                                "_source": ["keyword", "category", "title", "answer_count", "popularity_score"]
                            }
                        }
                    }
                }
            }
        }

        try:
            response = await self.es.search(index=self.INDEX_NAME, body=search_body)
            results = []

            buckets = response.get('aggregations', {}).get('unique_keywords', {}).get('buckets', [])

            for bucket in buckets:
                top_hit = bucket['top_doc']['hits']['hits'][0]['_source']
                results.append(AutocompleteResult(
                    keyword=top_hit['keyword'],
                    category=top_hit['category'],
                    title=top_hit['title'],
                    answer_count=top_hit['answer_count'],
                    popularity_score=top_hit['popularity_score'],
                    matched_by='chosung'
                ))

            return results

        except Exception as e:
            self.logger.error(f"초성 검색 에러: {e}")
            return []

    async def suggest(
        self,
        query: str,
        limit: int = 5,
        category: Optional[str] = None
    ) -> List[str]:
        # Completion Suggester 사용
        suggest_body = {
            "suggest": {
                "keyword-suggest": {
                    "prefix": query,
                    "completion": {
                        "field": "suggest",
                        "size": limit,
                        "skip_duplicates": True,
                        "contexts": {
                            "category": [category] if category else []
                        } if category else {}
                    }
                }
            }
        }

        try:
            response = await self.es.search(index=self.INDEX_NAME, body=suggest_body)
            suggestions = response['suggest']['keyword-suggest'][0]['options']

            keywords = []
            for suggestion in suggestions:
                keyword = suggestion['_source']['keyword']
                if keyword not in keywords:
                    keywords.append(keyword)

            return keywords[:limit]

        except Exception as e:
            self.logger.error(f"자동완성 제안 에러: {e}")
            return []

    async def get_trending_keywords(
        self,
        limit: int = 10,
        category: Optional[str] = None
    ) -> List[AutocompleteResult]:
        cache_key = f"trending:{limit}:{category}"

        # 캐시 확인
        cached_result = await self.cache.aget(cache_key)
        if cached_result:
            return [AutocompleteResult(**item) for item in cached_result]

        # Elasticsearch 쿼리
        query_body = {
            "query": {
                "match_all": {}
            } if not category else {
                "term": {
                    "category.keyword": category
                }
            },
            "sort": [
                {"popularity_score": {"order": "desc"}},
                {"answer_count": {"order": "desc"}}
            ],
            "size": limit,
            "_source": ["keyword", "category", "title", "answer_count", "popularity_score"]
        }

        try:
            response = await self.es.search(index=self.INDEX_NAME, body=query_body)
            results = []

            for hit in response['hits']['hits']:
                source = hit['_source']
                results.append(AutocompleteResult(
                    keyword=source['keyword'],
                    category=source['category'],
                    title=source['title'],
                    answer_count=source['answer_count'],
                    popularity_score=source['popularity_score'],
                    matched_by='trending'
                ))

            # 캐시 저장 (10분)
            await self.cache.aset(
                cache_key,
                [r.model_dump() for r in results],
                ttl=600
            )

            return results

        except Exception as e:
            self.logger.error(f"인기 검색어 조회 에러: {e}")
            return []

    async def get_categories(self) -> List[Dict[str, Any]]:
        cache_key = "categories:all"

        # 캐시 확인
        cached_result = await self.cache.aget(cache_key)
        if cached_result:
            return cached_result

        # Aggregation 쿼리
        agg_body = {
            "size": 0,
            "aggs": {
                "categories": {
                    "terms": {
                        "field": "category.keyword",
                        "size": 100
                    }
                }
            }
        }

        try:
            response = await self.es.search(index=self.INDEX_NAME, body=agg_body)
            buckets = response['aggregations']['categories']['buckets']

            categories = [
                {
                    "category": bucket['key'],
                    "count": bucket['doc_count']
                }
                for bucket in buckets
            ]

            # 캐시 저장 (1시간)
            await self.cache.aset(cache_key, categories, ttl=3600)

            return categories

        except Exception as e:
            self.logger.error(f"카테고리 조회 에러: {e}")
            return []
