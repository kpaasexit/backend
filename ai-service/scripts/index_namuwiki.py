"""
나무위키 텍스트 데이터를 사용하여 Elasticsearch 인덱스를 생성하는 스크립트

Korpora의 namuwikitext 말뭉치를 사용하여 자동완성용 키워드를 추출합니다.
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
    예: '고양이에게' -> '고양이', '영화를' -> '영화', '축구로' -> '축구'
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


def clean_title(title: str) -> str:
    """
    나무위키 섹션 타이틀에서 키워드 추출
    예: ' = 아스날 FC/2010-11 시즌 =' -> '아스날 FC'
    """
    if not title:
        return ""

    # '=' 기호 제거
    cleaned = title.replace('=', '').strip()

    # '/' 기호로 분리된 경우 첫 번째 부분만 사용
    if '/' in cleaned:
        cleaned = cleaned.split('/')[0].strip()

    # '(' 기호로 분리된 경우 첫 번째 부분만 사용 (예: "소말리아(축구선수)")
    if '(' in cleaned:
        main_part = cleaned.split('(')[0].strip()
        if main_part:  # 괄호 앞에 내용이 있으면 그것만 사용
            cleaned = main_part

    # 조사 제거
    cleaned = remove_josa(cleaned)

    return cleaned


def is_valid_keyword(keyword: str) -> bool:
    """
    유효한 키워드인지 검증
    - 최소 2글자 이상
    - 한글, 영문, 숫자 포함
    - 특수문자만으로 구성되지 않음
    - 조사/불필요한 연결어 제외
    """
    if not keyword or len(keyword) < 2:
        return False

    # 한글, 영문, 숫자가 하나 이상 포함되어야 함
    if not re.search(r'[가-힣a-zA-Z0-9]', keyword):
        return False

    # 너무 긴 키워드 제외 (50자 이상)
    if len(keyword) > 50:
        return False

    # 조사 및 연결어로 끝나는 키워드 필터링
    invalid_endings = [
        '은', '는', '이', '가', '을', '를', '의', '에', '에서', '으로', '로',
        '와', '과', '도', '만', '까지', '부터', '보다', '처럼', '같이',
        '이다', '다', '하다', '되다', '있다', '없다', '이며', '며',
        '했다', '한다', '할', '하는', '했던', '했을', '했던', '됐다',
        '였다', '었다', '이고', '고', '지만', '만', '에도', '라도',
        '라는', '하면', '하자', '하여', '하니', '하기', '하게'
    ]

    for ending in invalid_endings:
        if keyword.endswith(ending):
            # 조사를 제외한 부분이 2글자 이상인지 확인
            base = keyword[:-len(ending)]
            if len(base) < 2:
                return False

    # 조사/연결어로만 구성된 경우 제외
    josa_only = ['은', '는', '이', '가', '을', '를', '의', '에', '와', '과', '도', '만', '도', '로']
    if keyword in josa_only:
        return False

    # 의미 없는 단독 단어 제외
    meaningless = ['것', '것이', '것을', '수', '등', '및', '또', '또는', '중', '때', '곳']
    if keyword in meaningless:
        return False

    return True


def extract_keywords_from_text(text: str) -> List[str]:
    """
    본문 텍스트에서 추가 키워드 추출
    - 명사구 추출 (간단한 패턴 매칭)
    - 조사 제거
    """
    if not text:
        return []

    keywords = []

    # 줄바꿈으로 분리
    lines = text.split('\n')

    for line in lines[:10]:  # 처음 10줄만 사용 (성능 최적화)
        # 2-10글자 사이의 한글 단어 추출
        matches = re.findall(r'[가-힣]{2,10}', line)
        # 조사 제거
        cleaned_matches = [remove_josa(match) for match in matches]
        keywords.extend(cleaned_matches)

    return keywords


def calculate_popularity_score(keyword: str, frequency: int) -> float:
    """
    키워드의 인기도 점수 계산
    - 출현 빈도가 높을수록 높은 점수
    - 길이가 적당한 키워드에 가산점
    """
    # 기본 점수: 빈도 기반
    base_score = min(frequency * 5, 100)

    # 길이 보정: 3-6글자가 가장 이상적
    length = len(keyword)
    if 3 <= length <= 6:
        length_bonus = 10
    elif length == 2 or length == 7:
        length_bonus = 5
    else:
        length_bonus = 0

    return min(base_score + length_bonus, 100)


async def load_namuwiki_corpus():
    """
    나무위키 텍스트 말뭉치 로드

    주의: 사용자는 프롬프트가 나타나면 원하는 샘플 수(예: 100000)를 입력해야 합니다.
    또는 'yes'를 입력하면 전체 데이터를 로드합니다 (메모리 5.3GB 필요).
    """
    try:
        logger.info("나무위키 텍스트 말뭉치 로드 중...")
        logger.info("프롬프트가 나타나면 숫자(예: 100000)를 입력하세요!")
        corpus = Korpora.load("namuwikitext")
        logger.info("나무위키 텍스트 말뭉치 로드 완료!")
        return corpus
    except Exception as e:
        logger.error(f"나무위키 텍스트 로드 실패: {e}")
        logger.info("말뭉치를 다운로드합니다...")
        Korpora.fetch("namuwikitext")
        logger.info("다시 로드를 시도합니다. 프롬프트가 나타나면 숫자(예: 100000)를 입력하세요!")
        corpus = Korpora.load("namuwikitext")
        logger.info("나무위키 텍스트 말뭉치 로드 완료!")
        return corpus


async def extract_keywords_from_corpus(corpus, max_samples: int = 100000) -> Dict[str, int]:
    """
    나무위키 말뭉치에서 키워드 추출

    Returns:
        Dict[키워드, 출현빈도]
    """
    keyword_counter = Counter()
    processed_count = 0

    logger.info(f"키워드 추출 시작 (최대 {max_samples}개 샘플)...")

    # train 데이터에서 키워드 추출
    for i, item in enumerate(corpus.train):
        if i >= max_samples:
            break

        # pair (섹션 타이틀)에서 키워드 추출
        if item.pair:
            keyword = clean_title(item.pair)
            if is_valid_keyword(keyword):
                keyword_counter[keyword] += 1

        # text (본문)에서 추가 키워드 추출 (샘플링)
        if i % 10 == 0 and item.text:  # 10개 중 1개만 처리 (성능 최적화)
            text_keywords = extract_keywords_from_text(item.text)
            for kw in text_keywords:
                if is_valid_keyword(kw):
                    keyword_counter[kw] += 1

        processed_count += 1

        if processed_count % 10000 == 0:
            logger.info(f"처리 진행: {processed_count}/{max_samples}")

    logger.info(f"키워드 추출 완료! 총 {len(keyword_counter)}개의 고유 키워드")
    return dict(keyword_counter)


async def prepare_documents(keyword_freq: Dict[str, int], min_frequency: int = 2) -> List[Dict[str, Any]]:
    """
    Elasticsearch 문서 준비

    Args:
        keyword_freq: 키워드와 빈도수 딕셔너리
        min_frequency: 최소 출현 빈도 (이 값보다 적게 나타난 키워드는 제외)
    """
    documents = []
    doc_id = 1

    # 빈도순으로 정렬
    sorted_keywords = sorted(keyword_freq.items(), key=lambda x: x[1], reverse=True)

    logger.info(f"문서 준비 중... (최소 빈도: {min_frequency})")

    for keyword, frequency in sorted_keywords:
        # 최소 빈도 필터링
        if frequency < min_frequency:
            continue

        keyword_chosung = extract_chosung(keyword)
        popularity_score = calculate_popularity_score(keyword, frequency)

        # 카테고리 추정 (간단한 규칙 기반)
        category = estimate_category(keyword)

        doc = {
            "_index": "naver_kin_autocomplete",
            "_id": doc_id,
            "_source": {
                "id": doc_id,
                "category": category,
                "keyword": keyword,
                "keyword_chosung": keyword_chosung,
                "title": f"{keyword}",
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

    logger.info(f"총 {len(documents)}개 문서 준비 완료")
    return documents


def estimate_category(keyword: str) -> str:
    """
    키워드에서 카테고리 추정 (간단한 규칙 기반)
    """
    # 카테고리 키워드 패턴
    category_patterns = {
        "스포츠": ["축구", "야구", "농구", "배구", "경기", "선수", "팀", "리그"],
        "음식/요리": ["요리", "음식", "식당", "맛집", "레시피", "국", "찌개", "밥"],
        "영화/드라마": ["영화", "드라마", "시즌", "에피소드", "배우", "감독"],
        "게임": ["게임", "플레이", "캐릭터", "스킬", "레벨"],
        "음악": ["음악", "노래", "앨범", "가수", "밴드", "곡"],
        "IT/기술": ["프로그래밍", "소프트웨어", "하드웨어", "앱", "웹", "시스템"],
        "인물": ["인물", "배우", "가수", "정치인"],
        "지역": ["시", "구", "동", "읍", "면"],
    }

    keyword_lower = keyword.lower()

    for category, patterns in category_patterns.items():
        for pattern in patterns:
            if pattern in keyword_lower:
                return category

    return "일반"


async def bulk_index_documents(es: AsyncElasticsearch, documents: List[Dict[str, Any]], batch_size: int = 1000):
    """문서를 배치로 인덱싱"""
    logger.info(f"총 {len(documents)}개 문서 인덱싱 시작...")

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
                logger.info(f"진행 상황: {min(i + batch_size, len(documents))}/{len(documents)}")

        except Exception as e:
            fail_count += len(batch)
            logger.error(f"배치 인덱싱 에러 (위치: {i}): {e}")

    logger.info(f"\n인덱싱 완료! 성공: {success_count}, 실패: {fail_count}")


async def test_search(es: AsyncElasticsearch):
    """검색 테스트"""
    logger.info("\n=== 검색 테스트 ===")

    test_queries = ["축구", "영화", "음식", "프로그래밍", "서울"]

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
            response = await es.search(index="naver_kin_autocomplete", body=search_query)
            results = response['hits']['hits']
            logger.info(f"\n'{query}' 검색 결과: {len(results)}개")
            for hit in results:
                keyword = hit['_source']['keyword']
                score = hit['_source']['popularity_score']
                freq = hit['_source']['answer_count']
                logger.info(f"  - {keyword} (인기도: {score:.1f}, 빈도: {freq})")
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
        index_name = "naver_kin_autocomplete"
        if not await es.indices.exists(index=index_name):
            logger.error(f"인덱스 '{index_name}'가 존재하지 않습니다.")
            logger.info("먼저 index_naver_kin.py를 실행하여 인덱스를 생성하세요.")
            return

        # 나무위키 말뭉치 로드
        # 프롬프트가 나타나면 100000을 입력하세요
        corpus = await load_namuwiki_corpus()

        logger.info(f"Train 데이터 크기: {len(corpus.train):,}개")
        logger.info(f"Dev 데이터 크기: {len(corpus.dev):,}개")
        logger.info(f"Test 데이터 크기: {len(corpus.test):,}개")

        # 키워드 추출 (train 데이터에서만 추출, 최대 10만개 샘플)
        keyword_freq = await extract_keywords_from_corpus(corpus, max_samples=100000)

        # 상위 키워드 출력
        logger.info("\n=== 상위 20개 키워드 ===")
        sorted_keywords = sorted(keyword_freq.items(), key=lambda x: x[1], reverse=True)
        for keyword, freq in sorted_keywords[:20]:
            logger.info(f"  {keyword}: {freq}회")

        # 문서 준비 (최소 빈도 2 이상)
        documents = await prepare_documents(keyword_freq, min_frequency=2)

        # 인덱싱
        await bulk_index_documents(es, documents, batch_size=1000)

        # 인덱스 리프레시
        await es.indices.refresh(index=index_name)
        logger.info("\n인덱스 리프레시 완료")

        # 전체 문서 수 확인
        count_response = await es.count(index=index_name)
        total_docs = count_response['count']
        logger.info(f"현재 총 문서 수: {total_docs:,}개")

        # 검색 테스트
        await test_search(es)

        logger.info("\n✓ 나무위키 데이터 인덱싱 완료!")

    except Exception as e:
        logger.error(f"\n에러 발생: {e}", exc_info=True)
        raise
    finally:
        await es.close()


if __name__ == "__main__":
    asyncio.run(main())
