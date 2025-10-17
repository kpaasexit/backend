"""
모두의 말뭉치(신문) 데이터를 사용하여 Elasticsearch 인덱스를 생성하는 스크립트

국립국어원의 신문 말뭉치에서 키워드를 추출하여 자동완성용 인덱스를 구축합니다.
"""

import asyncio
import os
import sys
import re
from typing import List, Dict, Any, Set
from datetime import datetime
from collections import Counter
from pathlib import Path

# 프로젝트 루트를 Python 경로에 추가
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from elasticsearch import AsyncElasticsearch
from Korpora import Korpora

# 로깅 설정
import logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


INDEX_NAME = "naver_kin_autocomplete"


# 초성 추출 함수
def extract_chosung(text: str) -> str:
    """한글 텍스트에서 초성만 추출"""
    if not isinstance(text, str):
        return ""

    CHOSUNG_LIST = ['ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ']

    result = []
    for char in text:
        if '가' <= char <= '힣':
            char_code = ord(char) - 0xAC00
            chosung_index = char_code // (21 * 28)
            result.append(CHOSUNG_LIST[chosung_index])
        else:
            result.append(char)

    return ''.join(result)


def remove_josa(keyword: str) -> str:
    """
    키워드에서 모든 조사를 엄격하게 제거
    예: '고양이에게' -> '고양이', '영화를' -> '영화'
    """
    # 모든 한국어 조사 목록 (긴 것부터 먼저 확인 - 우선순위)
    josa_patterns = [
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

    for josa in josa_patterns:
        if keyword.endswith(josa):
            base = keyword[:-len(josa)]
            # 조사를 제거한 부분이 유효한지 확인
            if len(base) >= 2 and re.search(r'[가-힣a-zA-Z0-9]', base):
                return base

    return keyword


def is_valid_keyword(keyword: str) -> bool:
    """
    유효한 키워드인지 검증
    - 최소 2글자 이상
    - 한글, 영문, 숫자 포함
    - 특수문자만으로 구성되지 않음
    - 의미 없는 단어 제외
    """
    if not keyword or len(keyword) < 2:
        return False

    # 한글, 영문, 숫자가 하나 이상 포함되어야 함
    if not re.search(r'[가-힣a-zA-Z0-9]', keyword):
        return False

    # 너무 긴 키워드 제외 (50자 이상)
    if len(keyword) > 50:
        return False

    # 의미 없는 단독 단어 제외
    meaningless = ['것', '수', '등', '및', '또', '중', '때', '곳', '점', '데', '면', '편', '측', '바']
    if keyword in meaningless:
        return False

    # 숫자로만 구성된 경우 제외
    if keyword.isdigit():
        return False

    return True


def extract_keywords_from_text(text: str) -> List[str]:
    """
    텍스트에서 키워드 추출
    - 명사구 추출 (간단한 패턴 매칭)
    - 조사 제거
    """
    if not text:
        return []

    keywords = []

    # 문장 단위로 분리
    sentences = re.split(r'[.!?\n]', text)

    for sentence in sentences[:20]:  # 처음 20문장만 처리
        # 2-15글자 사이의 한글 단어 추출
        matches = re.findall(r'[가-힣]{2,15}', sentence)
        # 조사 제거
        cleaned_matches = [remove_josa(match) for match in matches]
        keywords.extend(cleaned_matches)

        # 영문 + 한글 조합 키워드도 추출 (예: IT기술, AI기술)
        mixed_matches = re.findall(r'[a-zA-Z0-9가-힣]{2,15}', sentence)
        cleaned_mixed = [remove_josa(match) for match in mixed_matches]
        keywords.extend(cleaned_mixed)

    return keywords


def calculate_popularity_score(keyword: str, frequency: int) -> float:
    """
    키워드의 인기도 점수 계산
    - 출현 빈도가 높을수록 높은 점수
    - 길이가 적당한 키워드에 가산점
    """
    # 기본 점수: 빈도 기반
    base_score = min(frequency * 3, 100)

    # 길이 보정: 2-8글자가 가장 이상적
    length = len(keyword)
    if 2 <= length <= 4:
        length_bonus = 15
    elif 5 <= length <= 8:
        length_bonus = 10
    elif length == 9 or length == 10:
        length_bonus = 5
    else:
        length_bonus = 0

    return min(base_score + length_bonus, 100)


def map_topic_to_category(topic: str) -> str:
    """
    신문 토픽을 카테고리로 매핑
    """
    topic_map = {
        '정치': '정치',
        '경제': '경제/비즈니스',
        '사회': '사회',
        '생활': '생활/인테리어',
        'IT/과학': 'IT/기술',
        'IT과학': 'IT/기술',
        '연예': '연예',
        '스포츠': '스포츠',
        '문화': '문화',
        '미용/건강': '건강/의학',
        '미용건강': '건강/의학',
    }

    return topic_map.get(topic, '일반')


async def load_modu_news():
    """모두의 말뭉치(신문) 로드"""
    from Korpora import ModuNewsKorpus

    try:
        logger.info("모두의 말뭉치(신문) 로드 중...")
        logger.info("~/Korpora/NIKL_NEWSPAPER 디렉토리에 데이터가 있어야 합니다.")
        corpus = ModuNewsKorpus(load_light=True)
        logger.info("모두의 말뭉치(신문) 로드 완료!")
        return corpus
    except Exception as e:
        logger.error(f"모두의 말뭉치(신문) 로드 실패: {e}")
        logger.error("국립국어원에서 데이터를 다운로드하여 ~/Korpora/NIKL_NEWSPAPER에 압축 해제하세요.")
        raise


async def extract_keywords_from_corpus(corpus, max_samples: int = 100000) -> Dict[str, Dict[str, Any]]:
    """
    신문 말뭉치에서 키워드 추출

    Returns:
        Dict[키워드, {빈도, 카테고리 목록}]
    """
    keyword_info = {}
    processed_count = 0

    logger.info(f"키워드 추출 시작 (최대 {max_samples:,}개 샘플)...")

    # train 데이터에서 키워드 추출
    for i, item in enumerate(corpus.train):
        if i >= max_samples:
            break

        # 기사 제목 추출 (paragraph의 첫 줄)
        title = ""
        if item.paragraph and len(item.paragraph) > 0:
            title = item.paragraph[0]

        # 제목에서 키워드 추출
        if title:
            title_cleaned = remove_josa(title.strip())
            if is_valid_keyword(title_cleaned):
                if title_cleaned not in keyword_info:
                    keyword_info[title_cleaned] = {'count': 0, 'categories': set()}
                keyword_info[title_cleaned]['count'] += 3  # 제목은 가중치 높게
                if item.topic:
                    keyword_info[title_cleaned]['categories'].add(item.topic)

        # 본문에서 키워드 추출 (샘플링)
        if i % 5 == 0 and item.paragraph:  # 5개 중 1개만 처리
            full_text = ' '.join(item.paragraph)
            text_keywords = extract_keywords_from_text(full_text)

            for kw in text_keywords:
                if is_valid_keyword(kw):
                    if kw not in keyword_info:
                        keyword_info[kw] = {'count': 0, 'categories': set()}
                    keyword_info[kw]['count'] += 1
                    if item.topic:
                        keyword_info[kw]['categories'].add(item.topic)

        processed_count += 1

        if processed_count % 10000 == 0:
            logger.info(f"처리 진행: {processed_count:,}/{max_samples:,}")

    logger.info(f"키워드 추출 완료! 총 {len(keyword_info):,}개의 고유 키워드")
    return keyword_info


async def prepare_documents(keyword_info: Dict[str, Dict[str, Any]], min_frequency: int = 3, start_id: int = 2000000) -> List[Dict[str, Any]]:
    """
    Elasticsearch 문서 준비

    Args:
        keyword_info: 키워드 정보 딕셔너리
        min_frequency: 최소 출현 빈도
        start_id: 시작 문서 ID (기존 나무위키 데이터와 겹치지 않도록)
    """
    documents = []
    doc_id = start_id

    # 빈도순으로 정렬
    sorted_keywords = sorted(keyword_info.items(), key=lambda x: x[1]['count'], reverse=True)

    logger.info(f"문서 준비 중... (최소 빈도: {min_frequency})")

    for keyword, info in sorted_keywords:
        frequency = info['count']

        # 최소 빈도 필터링
        if frequency < min_frequency:
            continue

        keyword_chosung = extract_chosung(keyword)
        popularity_score = calculate_popularity_score(keyword, frequency)

        # 카테고리 선택 (가장 자주 나타난 카테고리 또는 첫 번째)
        categories = list(info['categories'])
        category = map_topic_to_category(categories[0]) if categories else '일반'

        doc = {
            "_index": INDEX_NAME,
            "_id": doc_id,
            "_source": {
                "id": doc_id,
                "category": category,
                "keyword": keyword,
                "keyword_chosung": keyword_chosung,
                "title": keyword,
                "title_chosung": keyword_chosung,
                "answer_count": frequency,  # 빈도를 답변 수로 사용
                "date": datetime.now().strftime("%Y-%m-%d"),
                "crawled_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "popularity_score": popularity_score,
                "suggest": {
                    "input": [keyword],
                    "weight": int(popularity_score),
                    "contexts": {
                        "category": [category]
                    }
                }
            }
        }

        documents.append(doc)
        doc_id += 1

    logger.info(f"총 {len(documents):,}개 문서 준비 완료")
    return documents


async def bulk_index_documents(es: AsyncElasticsearch, documents: List[Dict[str, Any]], batch_size: int = 1000):
    """문서를 배치로 인덱싱"""
    logger.info(f"총 {len(documents):,}개 문서 인덱싱 시작...")

    success_count = 0
    fail_count = 0

    for i in range(0, len(documents), batch_size):
        batch = documents[i:i + batch_size]

        bulk_body = []
        for doc in batch:
            action = {"index": {"_index": doc["_index"], "_id": doc["_id"]}}
            bulk_body.append(action)
            bulk_body.append(doc["_source"])

        try:
            response = await es.bulk(operations=bulk_body, refresh=False)

            if response.get("errors"):
                for item in response.get("items", []):
                    if "index" in item:
                        if item["index"].get("status") in [200, 201]:
                            success_count += 1
                        else:
                            fail_count += 1
            else:
                success_count += len(batch)

            if (i + batch_size) % 10000 < batch_size:
                logger.info(f"진행 상황: {min(i + batch_size, len(documents)):,}/{len(documents):,}")

        except Exception as e:
            fail_count += len(batch)
            logger.error(f"배치 인덱싱 에러 (위치: {i}): {e}")

    logger.info(f"\n인덱싱 완료! 성공: {success_count:,}, 실패: {fail_count}")


async def test_search(es: AsyncElasticsearch):
    """검색 테스트"""
    logger.info("\n=== 검색 테스트 ===")

    test_queries = ["정치", "경제", "코로나", "기술", "대통령"]

    for query in test_queries:
        search_query = {
            "query": {
                "prefix": {
                    "keyword.keyword": {
                        "value": query
                    }
                }
            },
            "size": 5,
            "sort": [
                {"popularity_score": {"order": "desc"}}
            ]
        }

        try:
            response = await es.search(index=INDEX_NAME, body=search_query)
            results = response['hits']['hits']
            logger.info(f"\n'{query}' 검색 결과: {len(results)}개")
            for hit in results:
                keyword = hit['_source']['keyword']
                score = hit['_source']['popularity_score']
                freq = hit['_source']['answer_count']
                category = hit['_source']['category']
                logger.info(f"  - {keyword} (카테고리: {category}, 인기도: {score:.1f}, 빈도: {freq})")
        except Exception as e:
            logger.error(f"'{query}' 검색 실패: {e}")


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

        logger.info("Elasticsearch 연결 성공!")

        # 인덱스 존재 확인
        if not await es.indices.exists(index=INDEX_NAME):
            logger.error(f"인덱스 '{INDEX_NAME}'가 존재하지 않습니다.")
            logger.info("먼저 index_naver_kin.py를 실행하여 인덱스를 생성하세요.")
            return

        # 모두의 말뭉치(신문) 로드
        corpus = await load_modu_news()

        logger.info(f"Train 데이터 크기: {len(corpus.train):,}개")

        # 키워드 추출 (train 데이터에서, 최대 10만개 샘플)
        keyword_info = await extract_keywords_from_corpus(corpus, max_samples=100000)

        # 상위 키워드 출력
        logger.info("\n=== 상위 20개 키워드 ===")
        sorted_keywords = sorted(keyword_info.items(), key=lambda x: x[1]['count'], reverse=True)
        for keyword, info in sorted_keywords[:20]:
            categories = list(info['categories'])
            logger.info(f"  {keyword}: {info['count']}회 (카테고리: {', '.join(categories[:3])})")

        # 문서 준비 (최소 빈도 3 이상)
        documents = await prepare_documents(keyword_info, min_frequency=3, start_id=2000000)

        # 인덱싱
        await bulk_index_documents(es, documents, batch_size=1000)

        # 인덱스 리프레시
        await es.indices.refresh(index=INDEX_NAME)
        logger.info("\n인덱스 리프레시 완료")

        # 전체 문서 수 확인
        count_response = await es.count(index=INDEX_NAME)
        total_docs = count_response['count']
        logger.info(f"현재 총 문서 수: {total_docs:,}개")

        # 검색 테스트
        await test_search(es)

        logger.info("\n✓ 신문 데이터 인덱싱 완료!")

    except Exception as e:
        logger.error(f"\n에러 발생: {e}", exc_info=True)
        raise
    finally:
        await es.close()


if __name__ == "__main__":
    asyncio.run(main())
