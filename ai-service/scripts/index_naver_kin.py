import asyncio
import os
from pathlib import Path
from typing import List, Dict, Any
from datetime import datetime

import pandas as pd
from elasticsearch import AsyncElasticsearch
from elasticsearch.helpers import async_bulk


# 초성 추출 함수
def extract_chosung(text: str) -> str:
    """한글 텍스트에서 초성만 추출"""
    if not isinstance(text, str):
        return ""

    CHOSUNG_LIST = ['ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ']

    result = []
    for char in text:
        if '가' <= char <= '힣':
            # 한글 유니코드: 0xAC00 ~ 0xD7A3
            char_code = ord(char) - 0xAC00
            chosung_index = char_code // (21 * 28)
            result.append(CHOSUNG_LIST[chosung_index])
        else:
            # 한글이 아닌 경우 그대로 추가
            result.append(char)

    return ''.join(result)


# Elasticsearch 인덱스 설정
INDEX_NAME = "naver_kin_autocomplete"

INDEX_SETTINGS = {
    "settings": {
        "analysis": {
            "analyzer": {
                "korean_standard": {
                    "type": "standard"
                },
                "ngram_analyzer": {
                    "type": "custom",
                    "tokenizer": "ngram_tokenizer",
                    "filter": ["lowercase"]
                },
                "edge_ngram_analyzer": {
                    "type": "custom",
                    "tokenizer": "edge_ngram_tokenizer",
                    "filter": ["lowercase"]
                },
                "keyword_analyzer": {
                    "type": "custom",
                    "tokenizer": "keyword",
                    "filter": ["lowercase"]
                }
            },
            "tokenizer": {
                "ngram_tokenizer": {
                    "type": "ngram",
                    "min_gram": 2,
                    "max_gram": 3,
                    "token_chars": ["letter", "digit"]
                },
                "edge_ngram_tokenizer": {
                    "type": "edge_ngram",
                    "min_gram": 1,
                    "max_gram": 20,
                    "token_chars": ["letter", "digit"]
                }
            }
        },
        "index": {
            "number_of_shards": 1,
            "number_of_replicas": 0,
            "max_ngram_diff": 20
        }
    },
    "mappings": {
        "properties": {
            "id": {"type": "integer"},
            "category": {
                "type": "text",
                "fields": {
                    "keyword": {"type": "keyword"}
                }
            },
            "keyword": {
                "type": "text",
                "analyzer": "korean_standard",
                "fields": {
                    "keyword": {"type": "keyword"},
                    "ngram": {
                        "type": "text",
                        "analyzer": "ngram_analyzer"
                    },
                    "edge_ngram": {
                        "type": "text",
                        "analyzer": "edge_ngram_analyzer"
                    },
                    "completion": {
                        "type": "completion",
                        "analyzer": "korean_standard"
                    }
                }
            },
            "keyword_chosung": {
                "type": "text",
                "analyzer": "keyword_analyzer"
            },
            "title": {
                "type": "text",
                "analyzer": "korean_standard",
                "fields": {
                    "ngram": {
                        "type": "text",
                        "analyzer": "ngram_analyzer"
                    }
                }
            },
            "title_chosung": {
                "type": "text",
                "analyzer": "keyword_analyzer"
            },
            "answer_count": {"type": "integer"},
            "date": {"type": "date", "format": "yyyy.MM.dd||yyyy-MM-dd||epoch_millis||strict_date_optional_time", "ignore_malformed": True},
            "crawled_at": {"type": "date", "format": "yyyy-MM-dd HH:mm:ss||strict_date_optional_time||epoch_millis", "ignore_malformed": True},
            "popularity_score": {"type": "float"},
            "suggest": {
                "type": "completion",
                "analyzer": "korean_standard",
                "contexts": [
                    {
                        "name": "category",
                        "type": "category"
                    }
                ]
            }
        }
    }
}


async def create_index(es: AsyncElasticsearch):
    """Elasticsearch 인덱스 생성"""
    if await es.indices.exists(index=INDEX_NAME):
        print(f"인덱스 '{INDEX_NAME}'가 이미 존재합니다. 삭제 후 재생성합니다.")
        await es.indices.delete(index=INDEX_NAME)

    await es.indices.create(index=INDEX_NAME, body=INDEX_SETTINGS)
    print(f"인덱스 '{INDEX_NAME}' 생성 완료!")


def calculate_popularity_score(row: pd.Series) -> float:
    """
    인기도 점수 계산
    - 답변 수: 가중치 70%
    - 최근성: 가중치 30%
    """
    answer_score = min(row['answer_count'] * 10, 100)  # 답변 수 (최대 100점)

    # 날짜 파싱 및 최근성 계산
    try:
        date_str = str(row['date'])
        if '.' in date_str:
            date_obj = datetime.strptime(date_str, '%Y.%m.%d')
        else:
            date_obj = datetime.strptime(date_str, '%Y-%m-%d')

        days_ago = (datetime.now() - date_obj).days
        recency_score = max(100 - (days_ago / 365) * 50, 0)  # 1년마다 50점 감소
    except:
        recency_score = 50  # 날짜 파싱 실패시 중간값

    return answer_score * 0.7 + recency_score * 0.3


def prepare_documents(df: pd.DataFrame) -> List[Dict[str, Any]]:
    """데이터프레임을 Elasticsearch 문서로 변환"""
    documents = []

    for _, row in df.iterrows():
        # NaN 값 처리
        keyword = str(row['keyword']) if pd.notna(row['keyword']) else ""
        title = str(row['title']) if pd.notna(row['title']) else ""
        category = str(row['category']) if pd.notna(row['category']) else ""

        # 빈 문서는 스킵
        if not keyword and not title:
            continue

        # 초성 추출
        keyword_chosung = extract_chosung(keyword)
        title_chosung = extract_chosung(title)

        # 인기도 점수 계산
        popularity_score = calculate_popularity_score(row)

        doc = {
            "_index": INDEX_NAME,
            "_id": int(row['id']),
            "_source": {
                "id": int(row['id']),
                "category": category,
                "keyword": keyword,
                "keyword_chosung": keyword_chosung,
                "title": title,
                "title_chosung": title_chosung,
                "answer_count": int(row['answer_count']) if pd.notna(row['answer_count']) else 0,
                "date": str(row['date']) if pd.notna(row['date']) else "",
                "crawled_at": str(row['crawled_at']) if pd.notna(row['crawled_at']) else "",
                "popularity_score": popularity_score,
                "suggest": {
                    "input": [keyword, title] if keyword and title else ([keyword] if keyword else [title]),
                    "weight": int(popularity_score),
                    "contexts": {
                        "category": [category] if category else []
                    }
                }
            }
        }

        documents.append(doc)

    return documents


async def bulk_index_documents(es: AsyncElasticsearch, documents: List[Dict[str, Any]], batch_size: int = 1000):
    """문서를 배치로 인덱싱"""
    print(f"총 {len(documents)}개 문서 인덱싱 시작...")

    success_count = 0
    fail_count = 0
    errors = []

    # 배치 단위로 인덱싱
    for i in range(0, len(documents), batch_size):
        batch = documents[i:i + batch_size]

        # bulk 요청 본문 생성
        bulk_body = []
        for doc in batch:
            # index action
            action = {"index": {"_index": doc["_index"], "_id": doc["_id"]}}
            bulk_body.append(action)
            # document source
            bulk_body.append(doc["_source"])

        try:
            response = await es.bulk(operations=bulk_body, refresh=False)

            # 결과 파싱
            if response.get("errors"):
                for item in response.get("items", []):
                    if "index" in item:
                        if item["index"].get("status") in [200, 201]:
                            success_count += 1
                        else:
                            fail_count += 1
                            errors.append(item["index"].get("error", "Unknown error"))
            else:
                success_count += len(batch)

            # 진행 상황 출력
            if (i + batch_size) % 10000 < batch_size:
                print(f"진행 상황: {min(i + batch_size, len(documents))}/{len(documents)}")

        except Exception as e:
            fail_count += len(batch)
            errors.append(str(e))
            print(f"배치 인덱싱 에러 (위치: {i}): {e}")

    print(f"\n인덱싱 완료!")
    print(f"성공: {success_count}, 실패: {fail_count}")

    if errors:
        print(f"\n에러 샘플 (최대 5개):")
        for error in errors[:5]:
            print(f"  - {error}")


async def verify_index(es: AsyncElasticsearch):
    """인덱스 검증"""
    count_response = await es.count(index=INDEX_NAME)
    total_docs = count_response['count']
    print(f"\n총 인덱싱된 문서 수: {total_docs}")

    # 샘플 검색 테스트
    print("\n=== 검색 테스트 ===")

    # 1. 일반 검색
    search_query = {
        "query": {
            "match": {
                "keyword": "침실인테리어"
            }
        },
        "size": 3
    }

    response = await es.search(index=INDEX_NAME, body=search_query)
    print(f"\n1. '침실인테리어' 검색 결과: {response['hits']['total']['value']}개")
    for hit in response['hits']['hits']:
        print(f"  - {hit['_source']['keyword']} (스코어: {hit['_score']:.2f})")

    # 2. 초성 검색
    chosung_query = {
        "query": {
            "match": {
                "keyword_chosung": "ㅊㅅㅇㅌㄹㅇ"
            }
        },
        "size": 3
    }

    response = await es.search(index=INDEX_NAME, body=chosung_query)
    print(f"\n2. 'ㅊㅅㅇㅌㄹㅇ' 초성 검색 결과: {response['hits']['total']['value']}개")
    for hit in response['hits']['hits']:
        print(f"  - {hit['_source']['keyword']} (초성: {hit['_source']['keyword_chosung']})")

    # 3. 자동완성 제안 (contexts 없이)
    suggest_query = {
        "suggest": {
            "keyword-suggest": {
                "prefix": "침실",
                "completion": {
                    "field": "keyword.completion",
                    "size": 5,
                    "skip_duplicates": True
                }
            }
        }
    }

    try:
        response = await es.search(index=INDEX_NAME, body=suggest_query)
        suggestions = response['suggest']['keyword-suggest'][0]['options']
        print(f"\n3. '침실' 자동완성 제안: {len(suggestions)}개")
        for suggestion in suggestions:
            print(f"  - {suggestion['text']}")
    except Exception as e:
        print(f"\n3. 자동완성 제안 테스트 실패: {e}")


async def main():
    """메인 실행 함수"""
    # Elasticsearch 연결
    es_host = os.getenv("ELASTICSEARCH_HOST", "localhost")
    es_port = int(os.getenv("ELASTICSEARCH_PORT", "9200"))

    es = AsyncElasticsearch(
        [f"http://{es_host}:{es_port}"],
        retry_on_timeout=True,
        max_retries=3
    )

    try:
        # 연결 확인
        if not await es.ping():
            raise Exception("Elasticsearch 연결 실패!")

        print("Elasticsearch 연결 성공!")

        # 데이터 로드
        data_path = Path(__file__).parent.parent / "app" / "models" / "home_life_finetuned" / "final_model" / "naver_kin_result.xlsx"

        if not data_path.exists():
            raise FileNotFoundError(f"데이터 파일을 찾을 수 없습니다: {data_path}")

        print(f"\n데이터 로드 중: {data_path}")
        df = pd.read_excel(data_path)
        print(f"총 {len(df)}개 레코드 로드됨")

        # 인덱스 생성
        print("\n인덱스 생성 중...")
        await create_index(es)

        # 문서 준비
        print("\n문서 준비 중...")
        documents = prepare_documents(df)

        # 인덱싱
        await bulk_index_documents(es, documents, batch_size=1000)

        # 인덱스 리프레시 (검색 가능하도록)
        await es.indices.refresh(index=INDEX_NAME)

        # 검증
        await verify_index(es)

        print("\n✓ 모든 작업이 완료되었습니다!")

    except Exception as e:
        print(f"\n에러 발생: {e}")
        raise
    finally:
        await es.close()


if __name__ == "__main__":
    asyncio.run(main())
