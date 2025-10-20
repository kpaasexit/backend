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
        enable_cache: bool = True
    ) -> AutocompleteResponse:
        """
        자동완성 API - 접두사 100% 반영
        입력한 문자열로 시작하는 키워드만 반환
        """
        start_time = datetime.now()

        # 쿼리 정규화
        normalized_query = query.strip()
        if not normalized_query:
            return AutocompleteResponse(
                query=query,
                results=[],
                total=0,
                search_type='prefix',
                took_ms=0.0
            )

        # 캐시 확인 (v3: 모든 조사 엄격 필터링 + 중복 제거)
        cache_key = f"autocomplete_prefix_v3:{normalized_query}:{limit}"
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

        # 접두사 매칭만 수행
        results = await self._search_by_prefix(normalized_query, limit)

        # 응답 생성
        took_ms = (datetime.now() - start_time).total_seconds() * 1000
        response = AutocompleteResponse(
            query=query,
            results=results,
            total=len(results),
            search_type='prefix',
            took_ms=took_ms
        )

        # 캐시 저장
        if enable_cache:
            await self.cache.aset(cache_key, response.model_dump(), ttl=300)  # 5분 캐싱

        return response

    async def related_search(
        self,
        query: str,
        limit: int = 10,
        enable_cache: bool = True
    ) -> AutocompleteResponse:
        """
        연관 검색어 API - Fuzzy 매칭, 오타 허용
        입력한 문자열과 유사한 키워드 반환
        """
        start_time = datetime.now()

        # 쿼리 정규화
        normalized_query = query.strip()
        if not normalized_query:
            return AutocompleteResponse(
                query=query,
                results=[],
                total=0,
                search_type='related',
                took_ms=0.0
            )

        # 캐시 확인
        cache_key = f"related_search_v3:{normalized_query}:{limit}"
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

        # Fuzzy 매칭 수행
        results = await self._search_by_fuzzy(normalized_query, limit)

        # 응답 생성
        took_ms = (datetime.now() - start_time).total_seconds() * 1000
        response = AutocompleteResponse(
            query=query,
            results=results,
            total=len(results),
            search_type='related',
            took_ms=took_ms
        )

        # 캐시 저장
        if enable_cache:
            await self.cache.aset(cache_key, response.model_dump(), ttl=300)  # 5분 캐싱

        return response

    async def chosung_search(
        self,
        query: str,
        limit: int = 10,
        enable_cache: bool = True
    ) -> AutocompleteResponse:
        """
        초성 검색 API
        초성으로 키워드 검색
        """
        start_time = datetime.now()

        # 쿼리 정규화
        normalized_query = normalize_chosung_query(query.strip())
        if not normalized_query:
            return AutocompleteResponse(
                query=query,
                results=[],
                total=0,
                search_type='chosung',
                took_ms=0.0
            )

        # 캐시 확인
        cache_key = f"chosung_search_v3:{normalized_query}:{limit}"
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

        # 초성 검색 수행
        results = await self._search_by_chosung(normalized_query, limit)

        # 응답 생성
        took_ms = (datetime.now() - start_time).total_seconds() * 1000
        response = AutocompleteResponse(
            query=query,
            results=results,
            total=len(results),
            search_type='chosung',
            took_ms=took_ms
        )

        # 캐시 저장
        if enable_cache:
            await self.cache.aset(cache_key, response.model_dump(), ttl=300)  # 5분 캐싱

        return response

    def _has_josa_ending(self, keyword: str) -> bool:
        """
        키워드가 조사로 끝나는지 엄격하게 확인
        예: "고양이에게" -> True, "고기와" -> True, "고양이" -> False
        """
        # 모든 한국어 조사 목록 (긴 것부터 먼저 체크)
        josa_endings = [
            # 4글자 이상
            '에서부터', '으로부터', '에게서는', '한테서는',
            # 3글자
            '에서는', '에서도', '에서의', '으로는', '으로도', '으로의', '에게는', '에게도', '한테는', '한테도',
            '이라는', '이라도', '이라고', '이지만', '이면서', '이거나',
            # 2글자
            '에서', '에게', '한테', '으로', '로서', '로써', '까지', '부터', '조차', '마저', '밖에', '대로', '처럼', '같이',
            '이다', '이며', '이고', '이나', '이든', '이요', '이야', '이랑', '라도', '라고', '라며', '라면',
            # 1글자
            '은', '는', '이', '가', '을', '를', '의', '에', '와', '과', '도', '만', '로', '요', '야'
        ]

        for josa in josa_endings:
            if keyword.endswith(josa):
                # 조사를 제외한 부분의 길이 체크
                base_len = len(keyword) - len(josa)
                # 조사를 제외한 부분이 2글자 이상이면 조사로 간주
                if base_len >= 2:
                    return True

        return False

    async def _search_by_prefix(
        self,
        query: str,
        limit: int
    ) -> List[AutocompleteResult]:
        """
        접두사 매칭 검색 - Completion Suggester 방식
        정확히 입력한 문자열로 시작하는 키워드만 반환
        """
        # Completion Suggester를 사용한 빠른 접두사 검색
        suggest_body = {
            "suggest": {
                "autocomplete": {
                    "prefix": query,
                    "completion": {
                        "field": "keyword.completion",
                        "size": limit * 3,  # 조사 필터링을 위해 더 많이 가져옴
                        "skip_duplicates": True,
                        "fuzzy": {
                            "fuzziness": 0  # 접두사 매칭은 정확하게
                        }
                    }
                }
            },
            "_source": ["keyword", "category", "title", "answer_count", "popularity_score"]
        }

        try:
            response = await self.es.search(index=self.INDEX_NAME, body=suggest_body)
            suggestions = response.get('suggest', {}).get('autocomplete', [])

            if not suggestions or not suggestions[0].get('options'):
                # Suggester 실패 시 fallback: 기존 prefix 쿼리 사용
                self.logger.debug(f"Completion Suggester 결과 없음, prefix 쿼리로 fallback")
                return await self._search_by_prefix_fallback(query, limit)

            results = []
            seen_keywords = set()  # 중복 제거용

            for option in suggestions[0]['options']:
                source = option['_source']
                keyword = source['keyword']

                # 조사로 끝나는 키워드 필터링
                if self._has_josa_ending(keyword):
                    continue

                # 중복 제거
                if keyword in seen_keywords:
                    continue
                seen_keywords.add(keyword)

                results.append(AutocompleteResult(
                    keyword=keyword,
                    category=source['category'],
                    title=source['title'],
                    answer_count=source['answer_count'],
                    popularity_score=source['popularity_score'],
                    matched_by='completion'
                ))

                # limit에 도달하면 중단
                if len(results) >= limit:
                    break

            return results

        except Exception as e:
            self.logger.error(f"Completion Suggester 에러: {e}, fallback으로 전환")
            return await self._search_by_prefix_fallback(query, limit)

    async def _search_by_prefix_fallback(
        self,
        query: str,
        limit: int
    ) -> List[AutocompleteResult]:
        """
        접두사 매칭 검색 - Fallback 방식 (기존 prefix 쿼리)
        Completion Suggester 실패 시 사용
        """
        # 더 많은 결과를 가져와서 조사 필터링 후 limit만큼 반환
        fetch_size = limit * 3

        # 최종 쿼리 구성
        search_body = {
            "size": fetch_size,
            "query": {
                "prefix": {
                    "keyword.keyword": {
                        "value": query
                    }
                }
            },
            "sort": [
                {"popularity_score": {"order": "desc"}},
                {"answer_count": {"order": "desc"}},
                {"_score": {"order": "desc"}}
            ],
            "_source": ["keyword", "category", "title", "answer_count", "popularity_score"]
        }

        try:
            response = await self.es.search(index=self.INDEX_NAME, body=search_body)
            results = []
            seen_keywords = set()  # 중복 제거용

            for hit in response['hits']['hits']:
                source = hit['_source']
                keyword = source['keyword']

                # 조사로 끝나는 키워드 필터링
                if self._has_josa_ending(keyword):
                    continue

                # 중복 제거
                if keyword in seen_keywords:
                    continue
                seen_keywords.add(keyword)

                results.append(AutocompleteResult(
                    keyword=keyword,
                    category=source['category'],
                    title=source['title'],
                    answer_count=source['answer_count'],
                    popularity_score=source['popularity_score'],
                    matched_by='prefix'
                ))

                # limit에 도달하면 중단
                if len(results) >= limit:
                    break

            return results

        except Exception as e:
            self.logger.error(f"접두사 검색 에러: {e}")
            return []

    async def _search_by_fuzzy(
        self,
        query: str,
        limit: int
    ) -> List[AutocompleteResult]:
        """
        Fuzzy 매칭 검색 - 오타를 허용한 유사 키워드 검색 (Multi-field Boosting 최적화)
        """
        # Multi-field Boosting 최적화: 필드별 가중치 조정
        # - keyword.keyword^10: 정확 매칭 최우선
        # - keyword.edge_ngram^5: Edge N-gram (접두사 매칭)
        # - keyword.nori^3: Nori 형태소 분석
        # - keyword.ngram^2: N-gram (부분 매칭)
        # - title.nori^1.5: 제목 형태소 분석
        # - title^1: 제목 기본
        fuzzy_query = {
            "multi_match": {
                "query": query,
                "fields": [
                    "keyword.keyword^10",     # 정확 매칭 최우선
                    "keyword.edge_ngram^5",   # Edge N-gram (접두사)
                    "keyword.nori^3",         # Nori 형태소 분석
                    "keyword.ngram^2",        # N-gram (부분 매칭)
                    "title.nori^1.5",         # 제목 형태소 분석
                    "title^1"                 # 제목 기본
                ],
                "fuzziness": "AUTO",
                "prefix_length": 0,  # 접두사 제한 없음 (연관 검색어이므로)
                "max_expansions": 50,
                "type": "most_fields",  # most_fields로 변경하여 여러 필드 점수 합산
                "tie_breaker": 0.3      # 타이 브레이커로 다른 필드 점수도 반영
            }
        }

        # Nori 기반 부분 매칭 (AND 조건)
        partial_match = {
            "match": {
                "keyword.nori": {
                    "query": query,
                    "operator": "and",
                    "boost": 2.0  # 완전 매칭에 가중치
                }
            }
        }

        # Edge N-gram 접두사 매칭 추가
        edge_ngram_match = {
            "match": {
                "keyword.edge_ngram": {
                    "query": query,
                    "boost": 1.5
                }
            }
        }

        # 최종 쿼리 구성
        query_bool = {
            "should": [fuzzy_query, partial_match, edge_ngram_match],
            "minimum_should_match": 1
        }

        search_body = {
            "size": limit * 2,  # 중복 제거를 위해 더 많이 가져옴
            "query": {
                "bool": query_bool
            },
            "sort": [
                {"_score": {"order": "desc"}},
                {"popularity_score": {"order": "desc"}},
                {"answer_count": {"order": "desc"}}
            ],
            "_source": ["keyword", "category", "title", "answer_count", "popularity_score"]
        }

        try:
            response = await self.es.search(index=self.INDEX_NAME, body=search_body)
            results = []
            seen_keywords = set()

            for hit in response['hits']['hits']:
                source = hit['_source']
                keyword = source['keyword']

                # 조사로 끝나는 키워드 필터링
                if self._has_josa_ending(keyword):
                    continue

                # 중복 제거
                if keyword not in seen_keywords:
                    seen_keywords.add(keyword)
                    results.append(AutocompleteResult(
                        keyword=keyword,
                        category=source['category'],
                        title=source['title'],
                        answer_count=source['answer_count'],
                        popularity_score=source['popularity_score'],
                        matched_by='fuzzy'
                    ))

                # limit에 도달하면 중단
                if len(results) >= limit:
                    break

            return results

        except Exception as e:
            self.logger.error(f"Fuzzy 검색 에러: {e}")
            return []

    async def _search_by_chosung(
        self,
        query: str,
        limit: int
    ) -> List[AutocompleteResult]:
        """
        초성 검색 - 초성으로 키워드 매칭
        """
        # 쿼리 정규화 (초성만 추출)
        normalized_chosung = normalize_chosung_query(query)

        # 초성 매칭 쿼리
        search_body = {
            "size": limit,
            "query": {
                "match": {
                    "keyword_chosung": {
                        "query": normalized_chosung,
                        "fuzziness": "AUTO"
                    }
                }
            },
            "sort": [
                {"popularity_score": {"order": "desc"}},
                {"answer_count": {"order": "desc"}},
                {"_score": {"order": "desc"}}
            ],
            "_source": ["keyword", "category", "title", "answer_count", "popularity_score"]
        }

        try:
            response = await self.es.search(index=self.INDEX_NAME, body=search_body)
            results = []
            seen_keywords = set()  # 중복 제거용

            for hit in response['hits']['hits']:
                source = hit['_source']
                keyword = source['keyword']

                # 조사로 끝나는 키워드 필터링
                if self._has_josa_ending(keyword):
                    continue

                # 중복 제거
                if keyword in seen_keywords:
                    continue
                seen_keywords.add(keyword)

                results.append(AutocompleteResult(
                    keyword=keyword,
                    category=source['category'],
                    title=source['title'],
                    answer_count=source['answer_count'],
                    popularity_score=source['popularity_score'],
                    matched_by='chosung'
                ))

                # limit에 도달하면 중단
                if len(results) >= limit:
                    break

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

        #1 확인
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
