"""
통합 인덱싱 스크립트
- 네이버 지식인 데이터 (엑셀)
- 나무위키 텍스트 (Korpora)

모든 데이터를 하나의 인덱스에 충돌 없이 인덱싱합니다.

ID 범위 할당:
- 네이버 지식인: 1 ~ 999,999 (현재: 270,093개)
- 나무위키: 1,000,000 ~ 10,000,000 (현재: 3,910,370개 → 필터링 후 예상 1백만개)
"""

import asyncio
import os
import sys
import re
import json
from pathlib import Path
from typing import List, Dict, Any, Set, Optional, Tuple
from datetime import datetime
from collections import Counter
from unittest.mock import patch
from io import StringIO

import pandas as pd
from elasticsearch import AsyncElasticsearch
from sklearn.feature_extraction.text import TfidfVectorizer
from huggingface_hub import hf_hub_download
from dotenv import load_dotenv

# 로깅 설정
import logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 프로젝트 루트를 Python 경로에 추가
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

# .env 파일 로드
env_path = project_root / ".env"
load_dotenv(env_path)


# Korpora의 input() 프롬프트를 자동으로 "yes"로 응답하도록 패치
def auto_yes_input(prompt=""):
    """Korpora의 input() 호출을 자동으로 'yes'로 응답"""
    if "please insert" in prompt.lower() or "INPUT" in prompt:
        logger.info(f"자동 응답: yes (프롬프트: {prompt.strip()[:100]}...)")
        return "yes"
    return input(prompt)


# 형태소 분석기 초기화 (지연 로딩)
_pos_tagger = None

def get_pos_tagger():
    """형태소 분석기 싱글톤"""
    global _pos_tagger
    if _pos_tagger is None:
        try:
            from konlpy.tag import Okt
            _pos_tagger = Okt()
            logger.info("형태소 분석기 (Okt) 초기화 완료")
        except Exception as e:
            logger.warning(f"형태소 분석기 초기화 실패: {e}")
            _pos_tagger = None
    return _pos_tagger


def extract_nouns_only(text: str) -> Optional[str]:
    """
    형태소 분석을 통해 명사만 추출
    예: "영화배우이자" -> "영화배우"
    """
    if not text or len(text) < 2:
        return None

    tagger = get_pos_tagger()
    if tagger is None:
        return text  # 형태소 분석기가 없으면 원본 반환

    try:
        # 형태소 분석
        pos_result = tagger.pos(text, norm=True, stem=False)

        # 명사만 추출 (NNG: 일반명사, NNP: 고유명사)
        nouns = []
        for word, pos in pos_result:
            if pos in ['Noun']:  # Okt는 'Noun'으로 명사를 태깅
                nouns.append(word)

        if nouns:
            # 연속된 명사를 합쳐서 반환
            result = ''.join(nouns)
            if len(result) >= 2:
                return result

        return None
    except Exception as e:
        logger.debug(f"형태소 분석 실패 '{text}': {e}")
        return text


def analyze_keyword_pos(keyword: str) -> Dict[str, Any]:
    """
    키워드의 품사를 분석하여 세부 정보 반환

    Returns:
        {
            'has_proper_noun': bool,  # 고유명사 포함 여부
            'has_compound': bool,     # 복합명사 여부 (2개 이상 명사)
            'noun_count': int,        # 명사 개수
            'pos_tags': List[str],    # 품사 태그들
            'is_specialized': bool    # 전문/특화 키워드 여부
        }
    """
    tagger = get_pos_tagger()

    # 기본값
    result = {
        'has_proper_noun': False,
        'has_compound': False,
        'noun_count': 0,
        'pos_tags': [],
        'is_specialized': False
    }

    if tagger is None:
        return result

    try:
        # Okt로 형태소 분석
        pos_result = tagger.pos(keyword, norm=False, stem=False)

        noun_count = 0
        has_proper_noun = False
        pos_tags = []

        for word, pos in pos_result:
            pos_tags.append(pos)

            if pos == 'Noun':
                noun_count += 1
                # 고유명사 판별: 첫 글자가 대문자이거나, 특정 패턴
                if word and (word[0].isupper() or len(word) >= 3):
                    # 3글자 이상 명사는 고유명사/전문용어일 가능성
                    has_proper_noun = True

        result['noun_count'] = noun_count
        result['has_proper_noun'] = has_proper_noun
        result['has_compound'] = noun_count >= 2  # 2개 이상 명사 = 복합명사
        result['pos_tags'] = pos_tags

        # 전문/특화 키워드 판별:
        # 1. 복합명사 (명사 2개 이상)
        # 2. 영어+한글 조합
        # 3. 3글자 이상의 단일 명사
        has_english = bool(re.search(r'[a-zA-Z]', keyword))
        has_korean = bool(re.search(r'[가-힣]', keyword))

        if noun_count >= 2 or (has_english and has_korean) or (noun_count == 1 and len(keyword) >= 3):
            result['is_specialized'] = True

        return result

    except Exception as e:
        logger.debug(f"품사 분석 실패 '{keyword}': {e}")
        return result


def calculate_pos_boost(keyword: str, pos_info: Dict[str, Any] = None) -> float:
    """
    품사 분석 결과를 기반으로 가중치 계산

    - 고유명사: 1.5배
    - 복합명사: 1.3배
    - 전문용어: 1.2배
    - 일반명사: 1.0배

    Args:
        keyword: 키워드
        pos_info: analyze_keyword_pos 결과 (None이면 자동 분석)

    Returns:
        가중치 배율 (1.0 ~ 2.0)
    """
    if pos_info is None:
        pos_info = analyze_keyword_pos(keyword)

    boost = 1.0

    # 고유명사 보너스
    if pos_info['has_proper_noun']:
        boost *= 1.5

    # 복합명사 보너스
    if pos_info['has_compound']:
        boost *= 1.3

    # 전문용어 보너스
    elif pos_info['is_specialized']:
        boost *= 1.2

    return min(boost, 2.0)  # 최대 2.0배


def process_keyword_with_pos(text: str) -> Optional[str]:
    """
    명사만 엄격하게 추출

    동사, 형용사, 부사, 조사 등을 모두 제거하고 명사만 추출
    예:
      - "나온다" -> "나" (X, 동사 제거 시 의미 없음) -> None
      - "나이" -> "나이" (O, 명사)
      - "자동차보험" -> "자동차보험" (O, 복합명사)
      - "영화배우이자" -> "영화배우" (O, 조사 제거)

    Args:
        text: 원본 텍스트

    Returns:
        명사만 추출한 결과 (명사가 없으면 None)
    """
    if not text or len(text) < 2:
        return None

    tagger = get_pos_tagger()
    if tagger is None:
        # 형태소 분석기가 없으면 기본 처리
        return extract_nouns_only(text)

    try:
        # Okt 형태소 분석
        pos_result = tagger.pos(text, norm=True, stem=False)

        # 명사만 추출
        nouns = []
        for word, pos in pos_result:
            # Okt 품사 태그:
            # - Noun: 명사
            # - Verb: 동사 (제외)
            # - Adjective: 형용사 (제외)
            # - Adverb: 부사 (제외)
            # - Josa: 조사 (제외)
            if pos == 'Noun':
                # 명사만 추가
                nouns.append(word)

        if not nouns:
            return None

        # 명사들을 연결
        result = ''.join(nouns)

        # 최소 길이 체크
        if len(result) < 2:
            return None

        # 불용어 체크
        if result in DOMAIN_STOPWORDS:
            return None

        return result

    except Exception as e:
        logger.debug(f"형태소 분석 실패 '{text}': {e}")
        # 실패 시 기본 처리
        return extract_nouns_only(text)


# ============================================================================
# 공통 함수
# ============================================================================

INDEX_NAME = "naver_kin_autocomplete"
BACKUP_DIR = Path(__file__).parent / "backups"

# ============================================================================
# 도메인 특화 필터링 설정
# ============================================================================

# 일반적인 한국어 불용어 (위키백과, 나무위키에서 자주 나오지만 지식인에서는 덜 중요한 단어들)
DOMAIN_STOPWORDS = {
    # 일반 명사 (너무 포괄적)
    '것', '수', '등', '및', '또', '중', '때', '곳', '점', '데', '면', '편', '측', '바',
    '자', '이', '그', '저', '개', '번', '차', '회', '부', '명', '건', '처',

    # 형태소 분석 누락 일반어 (상위 빈도에서 발견된 단어들)
    '때문', '경우', '정도', '사실', '기타', '하나', '대한', '위한', '위해',
    '있는', '없는', '있다', '없다', '한다', '된다', '하지', '되지',
    '자신', '다른', '같은', '모든', '여러', '각각', '서로',
    '이런', '저런', '그런', '이렇', '저렇', '그렇',
    '어떤', '어느', '누구', '무엇', '언제', '어디', '어떻',
    '가장', '좀', '조금', '많이', '적게', '또한', '그리고',
    '하지만', '그러나', '그래서', '따라서', '그러므로',
    '다시', '계속', '항상', '언제나', '가끔', '때때로', '자주',

    # 시간/공간 관련 일반어
    '시대', '시기', '당시', '이후', '이전', '초기', '후기', '말기', '전기', '중기',
    '지역', '지방', '장소', '위치', '주변', '인근', '근처', '부근',
    '세기', '연대', '시절', '시기', '기간', '동안',

    # 역사/문화 일반어 (위키에서 자주 나옴)
    '역사', '문화', '전통', '유산', '유적', '유물', '문명', '시대',
    '왕조', '국가', '나라', '제국', '왕국', '공화국',
    '민족', '종족', '부족', '씨족', '가문', '혈통',

    # 인물 관련 일반어
    '사람', '인물', '인사', '이름', '성명', '호', '자',
    '출신', '태생', '가족', '친척', '조상', '후손',

    # 추상적 개념어
    '의미', '개념', '정의', '이유', '원인', '결과', '영향', '효과',
    '방법', '방식', '형태', '모습', '양상', '상태', '상황',
    '문제', '과제', '현상', '경향', '특성', '성질',
    '목적', '목표', '이상', '가치', '기준', '원칙',

    # 설명/서술어
    '특징', '성격', '성향', '경향', '추세', '동향',
    '구조', '체계', '시스템', '제도', '구성', '조직',
    '종류', '유형', '형식', '양식', '부류', '분류',
    '기능', '역할', '임무', '직무', '책임',
    '내용', '요소', '성분', '구성', '부분', '전체',

    # 위치/방향
    '위', '아래', '앞', '뒤', '좌', '우', '옆', '상', '하', '전', '후', '내', '외',
    '동', '서', '남', '북', '중앙', '중심', '주변', '외곽',

    # 일반 동작/상태
    '사용', '이용', '활용', '적용', '도입', '채택',
    '시작', '개시', '착수', '출발', '발단',
    '종료', '완료', '마무리', '끝', '종결',
    '변화', '변경', '수정', '개선', '발전', '진화',

    # 위키 메타 키워드
    '분류', '문서', '외부', '링크', '각주', '참고', '자료', '출처', '같이', '보기',
    '더보기', '참조', '관련', '항목', '외부링크', '둘러보기', '틀', '파일', '그림',
    '사진', '이미지', '목차', '개요', '특징', '종류', '구조', '기능',
    '참고문헌', '같이보기',

    # 학문/분야 일반어
    '학문', '학술', '연구', '조사', '분석', '검토',
    '이론', '학설', '주장', '견해', '관점', '시각',
    '분야', '영역', '범위', '범주', '항목',

    # 평가/판단
    '평가', '판단', '비판', '분석', '검토', '고찰',
    '중요', '주요', '핵심', '본질', '근본', '기본',
    '일반', '보편', '통상', '통례', '관례',

    # 너무 일반적인 고유명사 유형
    '대학', '대학교', '학교', '고등학교', '중학교', '초등학교',
    '회사', '기업', '법인', '단체', '조직', '기관',
    '정부', '행정', '관청', '부처', '청', '국',

    # 단순 형용/부사
    '크다', '작다', '많다', '적다', '높다', '낮다',
    '길다', '짧다', '넓다', '좁다', '깊다', '얕다',
    '매우', '아주', '너무', '정말', '진짜', '참',

    # 일반 지리 용어
    '지리', '지형', '지세', '지질', '지대',
    '산', '강', '바다', '호수', '섬', '반도', '평야', '분지',

    # 일반 학문 분야명
    '과학', '기술', '공학', '예술', '문학', '철학',
    '경제', '정치', '사회', '법률', '의학', '교육',

    # 관계/연결어
    '관계', '관련', '연관', '연결', '접속', '결합',
    '비교', '대조', '차이', '구별', '구분',

    # 너무 포괄적인 단어들
    '일', '사', '업', '행', '동', '작', '활', '운',
    '고대', '고유', '고정', '고급', '고속', '고향', '고려',
    '대상', '대표', '대중', '대부분', '대다수',
    '전체', '부분', '일부', '대부', '소수',
    '세계', '국내', '국제', '해외', '외국',
}

# 문서 빈도 필터링 설정
MIN_DOC_FREQ = 5  # 최소 5번 이상 나온 키워드만 (노이즈 제거, 3 -> 5)
MAX_DOC_FREQ_RATIO = 0.2  # 전체 문서의 20% 이상에 나오면 너무 일반적 (0.3 -> 0.2)
# 나무위키 기준: 3,910,370 * 0.2 = 782,074개 문서 이상 -> 제외
# 네이버 기준: 270,093 * 0.2 = 54,019개 문서 이상 -> 제외

# TF-IDF 임계값
MIN_TFIDF_SCORE = 0.005  # 최소 TF-IDF 점수

# 데이터 크기 비율 (Naver: 270K vs Namuwiki: 3.9M = 1:14.5)
DATA_SIZE_RATIO = 14.5  # 나무위키/네이버 비율

# ============================================================================
# 생활 관련 키워드 자동 가중치 시스템 (데이터 기반)
# ============================================================================

# 생활 카테고리별 가중치 설정
LIFE_CATEGORY_WEIGHTS = {
    "요리/식품관리": 3.0,      # 가장 높은 비중 (15.61%)
    "청소/세탁": 3.0,          # 일상 생활 핵심
    "생활수리/DIY": 2.5,       # 실용성 높음
    "이사/인테리어": 2.5,      # 실용성 높음
    "육아/반려동물": 2.5,      # 실용성 높음
    "생활경제/계약": 2.0,      # 중요하지만 검색 빈도 낮음
    "환경/건강": 2.0,          # 중요하지만 검색 빈도 낮음
    "스마트홈/가전": 2.0,      # 중요하지만 검색 빈도 낮음
}

# 전역 변수: 카테고리별 키워드 맵 (데이터 로드 시 자동 생성)
_category_keyword_map = {}


def build_category_keyword_map(df: pd.DataFrame, top_n: int = 100) -> Dict[str, Set[str]]:
    """
    네이버 지식인 데이터에서 카테고리별 상위 키워드 추출

    Args:
        df: 네이버 지식인 데이터프레임
        top_n: 각 카테고리별로 추출할 상위 키워드 수

    Returns:
        {category: set(keywords)} 형태의 딕셔너리
    """
    logger.info("카테고리별 키워드 맵 생성 중...")
    category_keyword_map = {}

    for category in LIFE_CATEGORY_WEIGHTS.keys():
        # 해당 카테고리의 데이터만 필터링
        category_df = df[df['category'] == category]

        # 키워드 빈도 계산
        keyword_counter = Counter()
        for _, row in category_df.iterrows():
            keyword = str(row['keyword']) if pd.notna(row['keyword']) else ""
            if keyword and is_valid_keyword(keyword):
                keyword_counter[keyword] += 1

        # 상위 N개 키워드 추출
        top_keywords = {kw for kw, _ in keyword_counter.most_common(top_n)}
        category_keyword_map[category] = top_keywords

        logger.info(f"  {category}: {len(top_keywords)}개 키워드 추출")

    return category_keyword_map


def calculate_life_category_boost(keyword: str, category: str = None) -> float:
    """
    생활 카테고리 기반 키워드 가중치 계산

    Args:
        keyword: 검사할 키워드
        category: 키워드의 카테고리 (있으면 직접 가중치 부여)

    Returns:
        가중치 배율 (1.0 ~ 3.0)
    """
    # 카테고리가 명시되어 있으면 직접 가중치 반환
    if category and category in LIFE_CATEGORY_WEIGHTS:
        return LIFE_CATEGORY_WEIGHTS[category]

    # 카테고리가 없으면 키워드 맵에서 검색
    if not _category_keyword_map:
        return 1.0  # 맵이 초기화되지 않았으면 기본값

    max_boost = 1.0
    for cat, keywords in _category_keyword_map.items():
        if keyword in keywords:
            weight = LIFE_CATEGORY_WEIGHTS.get(cat, 1.0)
            max_boost = max(max_boost, weight)

    return max_boost


def calculate_domain_relevance_score(
    keyword: str,
    naver_kin_freq: int,
    namuwiki_freq: int,
    total_naver_docs: int,
    total_namuwiki_docs: int
) -> Tuple[float, bool]:
    """
    도메인 특화 점수 계산 (지식인 vs 나무위키)

    나무위키 데이터가 훨씬 많기 때문에 완화된 필터링 적용

    Returns:
        (score, should_keep): 점수와 유지 여부
    """
    # 도메인 불용어 체크 (가장 기본적인 필터링)
    if keyword in DOMAIN_STOPWORDS:
        return 0.0, False

    # 너무 드물게 나오면 제외 (노이즈)
    if namuwiki_freq < MIN_DOC_FREQ:
        return 0.0, False

    # 정규화된 빈도 (데이터 양 차이 고려)
    naver_normalized = naver_kin_freq / max(total_naver_docs, 1)
    namuwiki_normalized = namuwiki_freq / max(total_namuwiki_docs, 1)

    # 나무위키에서만 너무 자주 나오는 경우 제외
    # 나무위키 비율이 매우 높고 지식인 비율이 거의 없는 경우
    if namuwiki_normalized > MAX_DOC_FREQ_RATIO and naver_normalized < 0.0005:
        logger.debug(f"'{keyword}': 위키 전용 키워드 (나무위키: {namuwiki_normalized:.2%}, 지식인: {naver_normalized:.2%})")
        return 0.0, False

    # 점수 계산 (데이터 크기 비율 14.5배 고려)
    # 1. 기본 점수: 나무위키 빈도 기반 (제한)
    base_score = min(namuwiki_freq * 0.5, 50)  # 나무위키 빈도는 50점 상한

    # 2. 품사 태깅 보너스: 고유명사, 복합명사, 전문용어에 가중치
    pos_boost = calculate_pos_boost(keyword)
    base_score *= pos_boost

    # 3. 지식인 보너스: 데이터 크기 비율 고려하여 강력한 가중치 부여
    if naver_kin_freq > 0:
        # 지식인 빈도를 데이터 크기 비율로 보정 (14.5배)
        # 지식인 1회 = 나무위키 14.5회에 해당하는 중요도
        adjusted_naver_freq = naver_kin_freq * DATA_SIZE_RATIO

        # 보정된 빈도에 따라 가중치 계산 (최대 10배)
        # 예: 지식인 10회 = 나무위키 145회 상당 -> 매우 높은 가중치
        domain_boost = min(1 + (adjusted_naver_freq / 50), 10.0)
        domain_score = base_score * domain_boost

        logger.debug(f"'{keyword}': 지식인 보너스 (지식인:{naver_kin_freq}회 -> x{domain_boost:.1f})")
    else:
        # 지식인에 없는 키워드는 기본 점수만
        domain_score = base_score

    return domain_score, True


def calculate_tfidf_scores(
    keywords_naver: Dict[str, int],
    keywords_namuwiki: Dict[str, int]
) -> Dict[str, float]:
    """
    TF-IDF를 사용하여 키워드의 중요도 계산

    Args:
        keywords_naver: 네이버 지식인 키워드 빈도 {keyword: count}
        keywords_namuwiki: 나무위키 키워드 빈도 {keyword: count}

    Returns:
        각 키워드의 TF-IDF 점수 {keyword: score}
    """
    logger.info("TF-IDF 기반 키워드 중요도 계산 중...")

    # 전체 키워드 수집
    all_keywords = set(keywords_naver.keys()) | set(keywords_namuwiki.keys())

    # 문서 생성 (각 키워드를 문서로 간주, 빈도만큼 반복)
    naver_docs = []
    namuwiki_docs = []

    for keyword in all_keywords:
        naver_count = keywords_naver.get(keyword, 0)
        namuwiki_count = keywords_namuwiki.get(keyword, 0)

        if naver_count > 0:
            naver_docs.append(' '.join([keyword] * min(naver_count, 100)))  # 최대 100회로 제한
        if namuwiki_count > 0:
            namuwiki_docs.append(' '.join([keyword] * min(namuwiki_count, 100)))

    # TF-IDF 계산
    try:
        vectorizer = TfidfVectorizer(
            max_features=100000,
            min_df=MIN_DOC_FREQ,
            max_df=MAX_DOC_FREQ_RATIO,
            token_pattern=r'(?u)\b\w+\b'
        )

        # 네이버 지식인 문서에 대해 TF-IDF 계산
        if naver_docs:
            tfidf_matrix = vectorizer.fit_transform(naver_docs)
            feature_names = vectorizer.get_feature_names_out()

            # 각 키워드의 평균 TF-IDF 점수
            scores = {}
            for idx, keyword in enumerate(feature_names):
                col_data = tfidf_matrix.getcol(idx).toarray().flatten()
                avg_score = col_data.mean()
                if avg_score >= MIN_TFIDF_SCORE:
                    scores[keyword] = avg_score

            logger.info(f"TF-IDF 계산 완료: {len(scores):,}개 키워드")
            return scores
        else:
            logger.warning("TF-IDF 계산을 위한 문서가 없습니다")
            return {}

    except Exception as e:
        logger.error(f"TF-IDF 계산 중 오류: {e}")
        return {}


def filter_keywords_by_domain(
    keywords_namuwiki: Dict[str, int],
    keywords_naver: Dict[str, int] = None
) -> Dict[str, int]:
    """
    도메인 특화 필터링 적용하여 나무위키 키워드 정제

    Args:
        keywords_namuwiki: 나무위키 키워드 빈도
        keywords_naver: 네이버 지식인 키워드 빈도 (옵션)

    Returns:
        필터링된 나무위키 키워드
    """
    logger.info(f"도메인 특화 필터링 시작 (원본: {len(keywords_namuwiki):,}개)")

    if keywords_naver is None:
        keywords_naver = {}

    total_naver = sum(keywords_naver.values()) if keywords_naver else 1
    total_namuwiki = sum(keywords_namuwiki.values())

    filtered_keywords = {}
    excluded_by_stopwords = 0
    excluded_by_freq = 0
    excluded_by_ratio = 0

    for keyword, namuwiki_freq in keywords_namuwiki.items():
        naver_freq = keywords_naver.get(keyword, 0)

        score, should_keep = calculate_domain_relevance_score(
            keyword,
            naver_freq,
            namuwiki_freq,
            total_naver,
            total_namuwiki
        )

        if should_keep:
            filtered_keywords[keyword] = namuwiki_freq
        else:
            if keyword in DOMAIN_STOPWORDS:
                excluded_by_stopwords += 1
            elif naver_freq + namuwiki_freq < MIN_DOC_FREQ:
                excluded_by_freq += 1
            else:
                excluded_by_ratio += 1

    logger.info(f"필터링 완료: {len(filtered_keywords):,}개 유지")
    logger.info(f"  - 불용어 제외: {excluded_by_stopwords:,}개")
    logger.info(f"  - 빈도 부족: {excluded_by_freq:,}개")
    logger.info(f"  - 너무 일반적: {excluded_by_ratio:,}개")

    return filtered_keywords


INDEX_SETTINGS = {
    "settings": {
        "analysis": {
            "tokenizer": {
                "nori_user_dict": {
                    "type": "nori_tokenizer",
                    "decompound_mode": "mixed"
                },
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
            },
            "filter": {
                "nori_posfilter": {
                    "type": "nori_part_of_speech",
                    "stoptags": [
                        "E", "IC", "J", "MAG", "MAJ", "MM", "SP", "SSC", "SSO",
                        "SC", "SE", "XPN", "XSA", "XSN", "XSV", "UNA", "NA", "VSV"
                    ]
                },
                "edge_ngram_filter": {
                    "type": "edge_ngram",
                    "min_gram": 1,
                    "max_gram": 20
                }
            },
            "analyzer": {
                "nori_analyzer": {
                    "type": "custom",
                    "tokenizer": "nori_user_dict",
                    "filter": ["nori_posfilter", "lowercase"]
                },
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
            "source_type": {"type": "keyword"},
            "category": {
                "type": "text",
                "fields": {"keyword": {"type": "keyword"}}
            },
            "keyword": {
                "type": "text",
                "analyzer": "nori_analyzer",
                "fields": {
                    "keyword": {"type": "keyword"},
                    "nori": {"type": "text", "analyzer": "nori_analyzer"},
                    "ngram": {"type": "text", "analyzer": "ngram_analyzer"},
                    "edge_ngram": {"type": "text", "analyzer": "edge_ngram_analyzer"},
                    "completion": {"type": "completion", "analyzer": "nori_analyzer"}
                }
            },
            "keyword_chosung": {"type": "text", "analyzer": "keyword_analyzer"},
            "title": {
                "type": "text",
                "analyzer": "nori_analyzer",
                "fields": {
                    "nori": {"type": "text", "analyzer": "nori_analyzer"},
                    "ngram": {"type": "text", "analyzer": "ngram_analyzer"}
                }
            },
            "title_chosung": {"type": "text", "analyzer": "keyword_analyzer"},
            "answer_count": {"type": "integer"},
            "date": {"type": "date", "format": "yyyy.MM.dd||yyyy-MM-dd||epoch_millis||strict_date_optional_time", "ignore_malformed": True},
            "crawled_at": {"type": "date", "format": "yyyy-MM-dd HH:mm:ss||strict_date_optional_time||epoch_millis", "ignore_malformed": True},
            "popularity_score": {"type": "float"},
            "suggest": {
                "type": "completion",
                "analyzer": "nori_analyzer",
                "contexts": [{"name": "category", "type": "category"}]
            }
        }
    }
}


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
    키워드에서 모든 조사를 엄격하게 제거 (다중 조사 처리)
    예: '고양이에게' -> '고양이', '영화를' -> '영화', '고기육수나' -> '고기육수'
    """
    if not keyword or len(keyword) < 2:
        return keyword

    # 조사 패턴 (긴 것부터 정렬)
    josa_patterns = [
        # 4글자 이상
        '에서부터', '으로부터', '에게서는', '한테서는',
        # 3글자
        '에서는', '에서도', '에서의', '으로는', '으로도', '으로의', '에게는', '에게도', '한테는', '한테도',
        '이라는', '이라도', '이라고', '이지만', '이면서', '이거나', '라고는', '라면서', '이어서',
        # 2글자
        '에서', '에게', '한테', '으로', '로서', '로써', '까지', '부터', '조차', '마저', '밖에', '대로', '처럼', '같이',
        '이다', '이며', '이고', '이나', '이든', '이요', '이야', '이랑', '라도', '라고', '라며', '라면', '보다', '마다',
        '대신', '위해', '따라', '통해', '인해', '의해', '관해', '대해',
        # 1글자
        '은', '는', '이', '가', '을', '를', '의', '에', '와', '과', '도', '만', '로', '요', '야', '나', '든'
    ]

    # 반복적으로 조사 제거 (최대 3회)
    prev_keyword = ""
    iteration = 0
    max_iterations = 3

    while prev_keyword != keyword and iteration < max_iterations:
        prev_keyword = keyword

        for josa in josa_patterns:
            if keyword.endswith(josa):
                base = keyword[:-len(josa)]
                # 최소 2글자 이상 남아야 하고, 의미있는 글자가 있어야 함
                if len(base) >= 2 and re.search(r'[가-힣a-zA-Z0-9]', base):
                    keyword = base
                    break

        iteration += 1

    return keyword


def is_valid_keyword(keyword: str) -> bool:
    """
    유효한 키워드인지 검증 (명사 중심 필터링 강화)

    - 기본 검증: 길이, 문자 패턴
    - 품사 검증: 명사가 포함되어 있는지 확인
    - 동사/형용사/부사/접미사 제외
    """
    if not keyword or len(keyword) < 2:
        return False

    if not re.search(r'[가-힣a-zA-Z0-9]', keyword):
        return False

    if len(keyword) > 50:
        return False

    # 기본 위키 메타 키워드 제외
    basic_wiki_meta = {
        '분류', '문서', '외부', '링크', '각주', '참고', '자료', '출처',
        '더보기', '참조', '관련', '항목', '외부링크', '둘러보기', '틀', '파일', '그림',
        '사진', '이미지', '목차', '참고문헌', '같이보기'
    }

    if keyword in basic_wiki_meta:
        return False

    # 숫자만으로 구성된 키워드 제외
    if keyword.isdigit():
        return False

    # 복수형/접미사 패턴 제외 (노트들, 사람들, 것들 등)
    plural_suffix_patterns = [
        r'.*들$',  # 복수형: 노트들, 사람들, 것들
        r'.*씩$',  # 배분: 하나씩, 조금씩
        r'.*마다$',  # 전체: 날마다, 해마다
        r'.*대로$',  # 양태: 생각대로, 말대로
        r'.*처럼$',  # 비교: 나처럼, 너처럼
        r'.*같이$',  # 비교: 나같이, 너같이
        r'.*부터$',  # 시작점
        r'.*까지$',  # 종료점
    ]

    for pattern in plural_suffix_patterns:
        if re.match(pattern, keyword) and len(keyword) <= 5:
            # 5글자 이하에서만 접미사 패턴 적용 (긴 복합어는 제외)
            return False

    # 동사/형용사 어미 패턴 제외 (강화)
    verb_adjective_patterns = [
        # 서술형 어미
        r'.*이자$', r'.*이며$', r'.*이고$', r'.*이거나$', r'.*이면서$',
        r'.*하며$', r'.*하고$', r'.*하거나$', r'.*하면서$',
        r'.*였으며$', r'.*였고$', r'.*이었으며$', r'.*이었고$',
        r'.*되며$', r'.*되고$', r'.*되거나$', r'.*되면서$',
        # 동사 어미 (다, 는다, 른다, 한다 등)
        r'.*[가-힣][는른한]다$',
        r'.*있다$', r'.*없다$', r'.*이다$', r'.*아니다$',
        # 형용사 패턴
        r'.*스럽다$', r'.*롭다$', r'.*답다$',
    ]

    for pattern in verb_adjective_patterns:
        if re.match(pattern, keyword):
            return False

    # 특수문자가 과도하게 포함된 경우 제외 (50% 이상)
    special_char_count = len(re.findall(r'[^가-힣a-zA-Z0-9\s]', keyword))
    if special_char_count / len(keyword) > 0.5:
        return False

    # 품사 태깅으로 명사 여부 확인 (2~4글자 키워드)
    if 2 <= len(keyword) <= 4:
        pos_info = analyze_keyword_pos(keyword)
        if pos_info['noun_count'] == 0:
            # 명사가 하나도 없으면 제외
            return False

    return True


async def create_index(es: AsyncElasticsearch):
    """Elasticsearch 인덱스 생성"""
    if await es.indices.exists(index=INDEX_NAME):
        logger.info(f"인덱스 '{INDEX_NAME}'가 이미 존재합니다. 삭제 후 재생성합니다.")
        await es.indices.delete(index=INDEX_NAME)

    await es.indices.create(index=INDEX_NAME, body=INDEX_SETTINGS)
    logger.info(f"인덱스 '{INDEX_NAME}' 생성 완료!")


async def bulk_index_documents(es: AsyncElasticsearch, documents: List[Dict[str, Any]], batch_size: int = 1000):
    """문서를 배치로 인덱싱"""
    logger.info(f"총 {len(documents):,}개 문서 인덱싱 시작...")

    success_count = 0
    fail_count = 0
    errors = []

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
                            errors.append(item["index"].get("error", "Unknown error"))
            else:
                success_count += len(batch)

            if (i + batch_size) % 10000 < batch_size:
                logger.info(f"진행 상황: {min(i + batch_size, len(documents)):,}/{len(documents):,}")

        except Exception as e:
            fail_count += len(batch)
            errors.append(str(e))
            logger.error(f"배치 인덱싱 에러 (위치: {i}): {e}")

    logger.info(f"인덱싱 완료! 성공: {success_count:,}, 실패: {fail_count}")

    if errors:
        logger.info(f"에러 샘플 (최대 5개):")
        for error in errors[:5]:
            logger.info(f"  - {error}")


# ============================================================================
# 1. 네이버 지식인 데이터 처리
# ============================================================================

def calculate_popularity_score_naver(row: pd.Series) -> float:
    """
    네이버 지식인 인기도 점수 계산
    - 답변 수: 가중치 70%
    - 최근성: 가중치 30%
    """
    answer_score = min(row['answer_count'] * 10, 100)

    try:
        date_str = str(row['date'])
        if '.' in date_str:
            date_obj = datetime.strptime(date_str, '%Y.%m.%d')
        else:
            date_obj = datetime.strptime(date_str, '%Y-%m-%d')

        days_ago = (datetime.now() - date_obj).days
        recency_score = max(100 - (days_ago / 365) * 50, 0)
    except:
        recency_score = 50

    return answer_score * 0.7 + recency_score * 0.3


def extract_naver_kin_keywords(df: pd.DataFrame) -> Dict[str, int]:
    """
    네이버 지식인 데이터에서 키워드 추출 (빈도 계산)

    형태소 분석을 통해 명사만 추출하여 정제
    """
    keyword_counter = Counter()
    logger.info("네이버 지식인 키워드 추출 중 (형태소 분석 적용)...")

    for _, row in df.iterrows():
        keyword = str(row['keyword']) if pd.notna(row['keyword']) else ""

        if keyword:
            # 형태소 분석으로 명사만 추출
            cleaned_keyword = process_keyword_with_pos(keyword)
            if cleaned_keyword and is_valid_keyword(cleaned_keyword):
                keyword_counter[cleaned_keyword] += 1

        title = str(row['title']) if pd.notna(row['title']) else ""
        if title:
            # 제목도 형태소 분석
            cleaned_title = process_keyword_with_pos(title)
            if cleaned_title and is_valid_keyword(cleaned_title):
                keyword_counter[cleaned_title] += 1

    logger.info(f"네이버 지식인 키워드 {len(keyword_counter):,}개 추출 완료")
    return dict(keyword_counter)


def prepare_naver_kin_documents(df: pd.DataFrame) -> List[Dict[str, Any]]:
    """
    네이버 지식인 데이터를 Elasticsearch 문서로 변환

    형태소 분석을 적용하여 명사만 추출
    """
    documents = []
    logger.info("네이버 지식인 데이터 준비 중 (형태소 분석 적용)...")

    for _, row in df.iterrows():
        raw_keyword = str(row['keyword']) if pd.notna(row['keyword']) else ""
        raw_title = str(row['title']) if pd.notna(row['title']) else ""
        category = str(row['category']) if pd.notna(row['category']) else ""

        # 형태소 분석으로 명사만 추출
        keyword = process_keyword_with_pos(raw_keyword) if raw_keyword else ""
        title = process_keyword_with_pos(raw_title) if raw_title else ""

        if not keyword and not title:
            continue

        keyword_chosung = extract_chosung(keyword)
        title_chosung = extract_chosung(title)

        # 기본 인기도 점수 계산
        base_popularity = calculate_popularity_score_naver(row)

        # 생활 카테고리 가중치 적용
        life_boost = calculate_life_category_boost(keyword, category)
        popularity_score = min(base_popularity * life_boost, 100)

        doc = {
            "_index": INDEX_NAME,
            "_id": int(row['id']),
            "_source": {
                "id": int(row['id']),
                "source_type": "naver_kin",
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

    logger.info(f"네이버 지식인 문서 {len(documents):,}개 준비 완료")
    return documents


# ============================================================================
# 2. 나무위키 데이터 처리
# ============================================================================

def clean_namuwiki_title(title: str) -> str:
    """나무위키 섹션 타이틀에서 키워드 추출 (형태소 분석 적용)"""
    if not title:
        return ""

    cleaned = title.replace('=', '').strip()

    if '/' in cleaned:
        cleaned = cleaned.split('/')[0].strip()

    if '(' in cleaned:
        main_part = cleaned.split('(')[0].strip()
        if main_part:
            cleaned = main_part

    # 조사 제거
    cleaned = remove_josa(cleaned)

    # 형태소 분석으로 명사만 추출
    noun_only = extract_nouns_only(cleaned)
    if noun_only and len(noun_only) >= 2:
        return noun_only

    return cleaned


def calculate_popularity_score_namuwiki(keyword: str, frequency: int, category: str = None) -> float:
    """
    나무위키 키워드의 인기도 점수 계산

    - 빈도 기반 점수
    - 길이 보너스
    - 품사 보너스 (고유명사, 복합명사, 전문용어)
    - 생활 카테고리 보너스 (데이터 기반)
    """
    base_score = min(frequency * 5, 100)

    # 길이 보너스
    length = len(keyword)
    if 3 <= length <= 6:
        length_bonus = 10
    elif length == 2 or length == 7:
        length_bonus = 5
    else:
        length_bonus = 0

    # 품사 보너스
    pos_boost = calculate_pos_boost(keyword)
    pos_bonus = (pos_boost - 1.0) * 20  # 1.0배 -> 0점, 1.5배 -> 10점, 2.0배 -> 20점

    # 생활 카테고리 보너스
    life_boost = calculate_life_category_boost(keyword, category)
    life_bonus = (life_boost - 1.0) * 30  # 1.0배 -> 0점, 2.0배 -> 30점, 3.0배 -> 60점

    total_score = base_score + length_bonus + pos_bonus + life_bonus
    return min(total_score, 100)


def extract_keywords_from_text(text: str, max_lines: int = 10) -> List[str]:
    """텍스트에서 키워드 추출 (형태소 분석 적용)"""
    if not text:
        return []

    keywords = []
    lines = text.split('\n')

    for line in lines[:max_lines]:
        # 한글 패턴 매칭
        matches = re.findall(r'[가-힣]{2,20}', line)
        for match in matches:
            # 조사 제거
            cleaned = remove_josa(match)
            # 형태소 분석으로 명사만 추출
            noun_only = extract_nouns_only(cleaned)
            if noun_only and len(noun_only) >= 2:
                keywords.append(noun_only)

        # 혼합 패턴 (영어+한글+숫자)
        mixed_matches = re.findall(r'[a-zA-Z0-9가-힣]{2,20}', line)
        for match in mixed_matches:
            cleaned = remove_josa(match)
            if cleaned and len(cleaned) >= 2:
                keywords.append(cleaned)

    return keywords


async def extract_namuwiki_keywords(corpus) -> Dict[str, int]:
    """나무위키 말뭉치에서 키워드 추출 (전체 데이터 사용)"""
    keyword_counter = Counter()
    processed_count = 0
    total_count = len(corpus.train)

    logger.info(f"나무위키 키워드 추출 시작 (전체 데이터: {total_count:,}개)...")

    for i, item in enumerate(corpus.train):
        if item.pair:
            keyword = clean_namuwiki_title(item.pair)
            if is_valid_keyword(keyword):
                keyword_counter[keyword] += 1

        if i % 10 == 0 and item.text:
            text_keywords = extract_keywords_from_text(item.text, max_lines=10)
            for kw in text_keywords:
                if is_valid_keyword(kw):
                    keyword_counter[kw] += 1

        processed_count += 1

        if processed_count % 10000 == 0:
            logger.info(f"나무위키 처리 진행: {processed_count:,}/{total_count:,} ({processed_count/total_count*100:.1f}%)")

    logger.info(f"나무위키 키워드 추출 완료! 총 {len(keyword_counter):,}개의 고유 키워드")
    return dict(keyword_counter)


def estimate_category_namuwiki(keyword: str) -> str:
    """나무위키 키워드에서 카테고리 추정"""
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


def prepare_namuwiki_documents(keyword_freq: Dict[str, int], min_frequency: int = 2, start_id: int = 1000000) -> List[Dict[str, Any]]:
    """나무위키 Elasticsearch 문서 준비"""
    documents = []
    doc_id = start_id

    sorted_keywords = sorted(keyword_freq.items(), key=lambda x: x[1], reverse=True)
    logger.info(f"나무위키 문서 준비 중... (최소 빈도: {min_frequency})")

    for keyword, frequency in sorted_keywords:
        if frequency < min_frequency:
            continue

        keyword_chosung = extract_chosung(keyword)
        popularity_score = calculate_popularity_score_namuwiki(keyword, frequency)
        category = estimate_category_namuwiki(keyword)

        doc = {
            "_index": INDEX_NAME,
            "_id": doc_id,
            "_source": {
                "id": doc_id,
                "source_type": "namuwiki",
                "category": category,
                "keyword": keyword,
                "keyword_chosung": keyword_chosung,
                "title": keyword,
                "title_chosung": keyword_chosung,
                "answer_count": frequency,
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

    logger.info(f"나무위키 문서 {len(documents):,}개 준비 완료")
    return documents


# ============================================================================
# 백업/복원 기능
# ============================================================================

async def backup_index_data(es: AsyncElasticsearch, index_name: str) -> Optional[Path]:
    """인덱스 데이터 백업"""
    try:
        logger.info("\n" + "=" * 80)
        logger.info("인덱스 백업 시작")
        logger.info("=" * 80)

        # 백업 디렉토리 생성
        BACKUP_DIR.mkdir(parents=True, exist_ok=True)

        # 전체 문서 수 확인
        count_response = await es.count(index=index_name)
        total_docs = count_response['count']
        logger.info(f"총 {total_docs:,}개 문서를 백업합니다.")

        # 모든 문서 가져오기 (scroll API 사용)
        documents = []
        batch_size = 1000

        response = await es.search(
            index=index_name,
            scroll='5m',
            size=batch_size,
            body={"query": {"match_all": {}}, "_source": True}
        )

        scroll_id = response['_scroll_id']
        hits = response['hits']['hits']

        while hits:
            for hit in hits:
                documents.append({"_id": hit['_id'], "_source": hit['_source']})

            if len(documents) % 10000 == 0:
                logger.info(f"백업 진행: {len(documents):,}/{total_docs:,}")

            response = await es.scroll(scroll_id=scroll_id, scroll='5m')
            scroll_id = response['_scroll_id']
            hits = response['hits']['hits']

        await es.clear_scroll(scroll_id=scroll_id)

        # 백업 파일 저장 (고정된 파일명 사용)
        backup_file = BACKUP_DIR / "autocomplete_backup.json"

        backup_data = {
            "metadata": {
                "index_name": index_name,
                "backup_date": datetime.now().isoformat(),
                "total_documents": len(documents),
                "elasticsearch_version": (await es.info())['version']['number']
            },
            "documents": documents
        }

        with open(backup_file, 'w', encoding='utf-8') as f:
            json.dump(backup_data, f, ensure_ascii=False, indent=2)

        file_size_mb = backup_file.stat().st_size / (1024 * 1024)
        logger.info(f"✓ 백업 완료! {len(documents):,}개 문서 ({file_size_mb:.2f} MB)")
        logger.info(f"백업 파일: {backup_file}")

        return backup_file

    except Exception as e:
        logger.error(f"백업 실패: {e}", exc_info=True)
        return None


def download_backup_from_huggingface() -> Path:
    """
    Hugging Face Private Repository에서 백업 파일 다운로드

    고정된 파일명(autocomplete_backup.json)을 다운로드

    Returns:
        다운로드된 백업 파일 경로
    """
    HF_TOKEN = os.getenv("HUGGINGFACE_API_TOKEN")
    HF_REPO_NAME = "elasticsearch-backups"
    HF_FIXED_FILENAME = "autocomplete_backup.json"

    if not HF_TOKEN:
        raise ValueError("HUGGINGFACE_API_TOKEN이 .env 파일에 설정되지 않았습니다.")

    logger.info("\n" + "=" * 80)
    logger.info("Hugging Face에서 백업 다운로드")
    logger.info("=" * 80)

    try:
        from huggingface_hub import HfApi

        # Hugging Face API 초기화
        api = HfApi()
        user_info = api.whoami(token=HF_TOKEN)
        username = user_info['name']
        repo_id = f"{username}/{HF_REPO_NAME}"

        logger.info(f"Repository: {repo_id}")
        logger.info(f"파일명: {HF_FIXED_FILENAME}")

        # 파일 다운로드
        logger.info("다운로드 중...")

        downloaded_path = hf_hub_download(
            repo_id=repo_id,
            repo_type="dataset",
            filename=HF_FIXED_FILENAME,
            token=HF_TOKEN,
            cache_dir=str(BACKUP_DIR / ".hf_cache"),
            force_download=True
        )

        logger.info(f"✓ 다운로드 완료: {downloaded_path}")
        logger.info("=" * 80)

        return Path(downloaded_path)

    except Exception as e:
        logger.error(f"다운로드 실패: {e}", exc_info=True)
        raise


async def restore_from_backup(es: AsyncElasticsearch, backup_file: Path) -> bool:
    """백업 파일로부터 인덱스 복원"""
    try:
        logger.info("\n" + "=" * 80)
        logger.info("백업에서 복원 시작")
        logger.info("=" * 80)

        # 백업 파일 읽기
        with open(backup_file, 'r', encoding='utf-8') as f:
            backup_data = json.load(f)

        metadata = backup_data['metadata']
        documents = backup_data['documents']

        logger.info(f"백업 정보:")
        logger.info(f"  - 백업 날짜: {metadata['backup_date']}")
        logger.info(f"  - 문서 수: {metadata['total_documents']:,}")

        # 인덱스가 이미 존재하면 삭제
        if await es.indices.exists(index=INDEX_NAME):
            logger.info(f"기존 인덱스 '{INDEX_NAME}' 삭제 중...")
            await es.indices.delete(index=INDEX_NAME)

        # 인덱스 재생성
        await es.indices.create(index=INDEX_NAME, body=INDEX_SETTINGS)
        logger.info(f"인덱스 '{INDEX_NAME}' 생성 완료")

        # 문서 복원
        logger.info(f"{len(documents):,}개 문서 복원 시작...")

        success_count = 0
        batch_size = 1000

        for i in range(0, len(documents), batch_size):
            batch = documents[i:i + batch_size]

            bulk_body = []
            for doc in batch:
                action = {"index": {"_index": INDEX_NAME, "_id": doc["_id"]}}
                bulk_body.append(action)
                bulk_body.append(doc["_source"])

            response = await es.bulk(operations=bulk_body, refresh=False)

            if not response.get("errors"):
                success_count += len(batch)

            if (i + batch_size) % 10000 < batch_size:
                logger.info(f"복원 진행: {min(i + batch_size, len(documents)):,}/{len(documents):,}")

        await es.indices.refresh(index=INDEX_NAME)

        logger.info(f"✓ 복원 완료! {success_count:,}개 문서")
        return True

    except Exception as e:
        logger.error(f"복원 실패: {e}", exc_info=True)
        return False


def get_latest_backup() -> Optional[Path]:
    """가장 최근 백업 파일 찾기"""
    if not BACKUP_DIR.exists():
        return None

    backup_files = sorted(BACKUP_DIR.glob(f"{INDEX_NAME}_backup_*.json"), reverse=True)
    return backup_files[0] if backup_files else None


def list_available_backups():
    """사용 가능한 백업 파일 목록 출력"""
    if not BACKUP_DIR.exists():
        return []

    backup_files = sorted(BACKUP_DIR.glob(f"{INDEX_NAME}_backup_*.json"), reverse=True)

    if backup_files:
        logger.info("\n사용 가능한 백업 파일:")
        for i, backup_file in enumerate(backup_files, 1):
            file_size_mb = backup_file.stat().st_size / (1024 * 1024)
            logger.info(f"  {i}. {backup_file.name} ({file_size_mb:.2f} MB)")

    return backup_files


# ============================================================================
# 검색 테스트
# ============================================================================

async def verify_index(es: AsyncElasticsearch):
    """인덱스 검증 및 검색 테스트"""
    count_response = await es.count(index=INDEX_NAME)
    total_docs = count_response['count']
    logger.info(f"\n총 인덱싱된 문서 수: {total_docs:,}")

    # 소스별 문서 수 확인
    for source_type in ["naver_kin", "namuwiki", "kowiki"]:
        source_query = {
            "query": {
                "term": {
                    "source_type": source_type
                }
            }
        }
        try:
            response = await es.count(index=INDEX_NAME, body=source_query)
            count = response['count']
            logger.info(f"  - {source_type}: {count:,}개")
        except Exception as e:
            logger.error(f"  - {source_type} 카운트 실패: {e}")

    # 샘플 검색 테스트
    logger.info("\n=== 검색 테스트 ===")

    test_queries = ["침실", "축구", "영화", "한국"]

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
                source = hit['_source']['source_type']
                score = hit['_source']['popularity_score']
                logger.info(f"  - {keyword} (출처: {source}, 인기도: {score:.1f})")
        except Exception as e:
            logger.error(f"'{query}' 검색 실패: {e}")


# ============================================================================
# 메인 함수
# ============================================================================

async def main():
    """메인 실행 함수"""
    import argparse

    parser = argparse.ArgumentParser(description="Elasticsearch 인덱싱 (백업/복원 통합)")
    parser.add_argument("--restore", action="store_true", help="최근 백업에서 복원")
    parser.add_argument("--restore-file", type=str, help="특정 백업 파일에서 복원")
    parser.add_argument("--from-huggingface", action="store_true", help="Hugging Face에서 백업 다운로드 후 복원 (autocomplete_backup.json)")
    parser.add_argument("--no-backup", action="store_true", help="인덱싱 후 자동 백업 비활성화")
    parser.add_argument("--list-backups", action="store_true", help="사용 가능한 백업 파일 목록")

    args = parser.parse_args()

    # 백업 목록만 출력하고 종료
    if args.list_backups:
        backups = list_available_backups()
        if not backups:
            logger.info("백업 파일이 없습니다.")
        return

    es_host = os.getenv("ELASTICSEARCH_HOST", "localhost")
    es_port = int(os.getenv("ELASTICSEARCH_PORT", "9200"))

    es = AsyncElasticsearch(
        [f"http://{es_host}:{es_port}"],
        retry_on_timeout=True,
        max_retries=3
    )

    try:
        # Elasticsearch 연결 확인
        if not await es.ping():
            raise Exception("Elasticsearch 연결 실패!")

        logger.info("=" * 80)
        logger.info("통합 인덱싱 (백업/복원 통합)")
        logger.info("=" * 80)
        logger.info("Elasticsearch 연결 성공!")

        # Hugging Face에서 복원 모드
        if args.from_huggingface:
            try:
                backup_file = download_backup_from_huggingface()
                logger.info(f"Hugging Face에서 다운로드 완료: {backup_file.name}")

                success = await restore_from_backup(es, backup_file)

                if success:
                    await verify_index(es)
                    logger.info("\n" + "=" * 80)
                    logger.info("✓ Hugging Face 백업 복원이 완료되었습니다!")
                    logger.info("=" * 80)
                else:
                    logger.error("복원 실패!")

            except Exception as e:
                logger.error(f"Hugging Face 복원 중 에러: {e}", exc_info=True)

            return

        # 로컬 백업에서 복원 모드
        if args.restore or args.restore_file:
            backup_file = None

            if args.restore_file:
                backup_file = Path(args.restore_file)
                if not backup_file.exists():
                    logger.error(f"백업 파일을 찾을 수 없습니다: {backup_file}")
                    return
            elif args.restore:
                backup_file = get_latest_backup()
                if not backup_file:
                    logger.error("백업 파일이 없습니다. 먼저 인덱싱을 실행하세요.")
                    return

            logger.info(f"백업 파일에서 복원: {backup_file.name}")
            success = await restore_from_backup(es, backup_file)

            if success:
                await verify_index(es)
                logger.info("\n" + "=" * 80)
                logger.info("✓ 복원이 완료되었습니다!")
                logger.info("=" * 80)
            else:
                logger.error("복원 실패!")

            return

        # 일반 인덱싱 모드
        # 기존 백업 확인
        latest_backup = get_latest_backup()
        if latest_backup:
            logger.info(f"\n최근 백업 발견: {latest_backup.name}")
            logger.info("백업에서 복원하려면 --restore 옵션을 사용하세요.")
            logger.info("  예: python index_all_data.py --restore")
            logger.info("")

        # 인덱스 생성
        logger.info("\n인덱스 생성 중...")
        await create_index(es)

        all_documents = []
        naver_keywords = {}  # 도메인 필터링에 사용

        # ====================================================================
        # 1. 네이버 지식인 데이터 로드 및 준비
        # ====================================================================
        logger.info("\n" + "=" * 80)
        logger.info("1. 네이버 지식인 데이터 처리")
        logger.info("=" * 80)

        data_path = Path(__file__).parent / "crawling_data" / "naver_kin_result.xlsx"

        if data_path.exists():
            logger.info(f"데이터 로드 중: {data_path}")
            df = pd.read_excel(data_path)
            logger.info(f"총 {len(df):,}개 레코드 로드됨")

            # 카테고리별 키워드 맵 생성 (생활 관련 가중치용)
            global _category_keyword_map
            _category_keyword_map = build_category_keyword_map(df, top_n=100)
            logger.info("카테고리 키워드 맵 생성 완료")

            # 키워드 추출 (도메인 필터링용)
            naver_keywords = extract_naver_kin_keywords(df)
            logger.info(f"네이버 지식인 고유 키워드: {len(naver_keywords):,}개")

            naver_docs = prepare_naver_kin_documents(df)
            all_documents.extend(naver_docs)
        else:
            logger.warning(f"네이버 지식인 데이터 파일을 찾을 수 없습니다: {data_path}")
            logger.warning("네이버 지식인 데이터는 건너뜁니다.")

        # ====================================================================
        # 2. 나무위키 데이터 로드 및 준비
        # ====================================================================
        logger.info("\n" + "=" * 80)
        logger.info("2. 나무위키 데이터 처리")
        logger.info("=" * 80)

        try:
            from Korpora import Korpora

            logger.info("나무위키 텍스트 말뭉치 로드 중...")

            # input() 함수를 패치하여 자동으로 "yes" 응답
            with patch('builtins.input', auto_yes_input):
                try:
                    namuwiki_corpus = Korpora.load("namuwikitext", force_download=False)
                except Exception as e:
                    logger.info(f"말뭉치를 다운로드합니다... (자동으로 진행됩니다) - {e}")
                    namuwiki_corpus = Korpora.load("namuwikitext", force_download=True)

            logger.info(f"나무위키 Train 데이터 크기: {len(namuwiki_corpus.train):,}개")

            namuwiki_keywords = await extract_namuwiki_keywords(namuwiki_corpus)

            # 도메인 특화 필터링 적용
            logger.info("\n" + "-" * 80)
            logger.info("도메인 특화 필터링 적용 (지식인 관련 키워드만 유지)")
            logger.info("-" * 80)

            filtered_namuwiki_keywords = filter_keywords_by_domain(
                namuwiki_keywords,
                naver_keywords
            )

            # 필터링 전후 비교
            logger.info("\n나무위키 키워드 필터링 전후:")
            logger.info(f"  필터링 전: {len(namuwiki_keywords):,}개")
            logger.info(f"  필터링 후: {len(filtered_namuwiki_keywords):,}개")
            logger.info(f"  감소율: {(1 - len(filtered_namuwiki_keywords)/len(namuwiki_keywords))*100:.1f}%")

            # 상위 키워드 출력 (필터링 후)
            logger.info("\n필터링 후 상위 20개 키워드:")
            sorted_keywords = sorted(filtered_namuwiki_keywords.items(), key=lambda x: x[1], reverse=True)
            for keyword, freq in sorted_keywords[:20]:
                logger.info(f"  {keyword}: {freq}회")

            namuwiki_docs = prepare_namuwiki_documents(filtered_namuwiki_keywords, min_frequency=2, start_id=1000000)
            all_documents.extend(namuwiki_docs)

        except ImportError:
            logger.warning("Korpora 라이브러리가 설치되지 않았습니다. 나무위키 데이터는 건너뜁니다.")
        except Exception as e:
            logger.error(f"나무위키 데이터 처리 중 에러: {e}", exc_info=True)
            logger.warning("나무위키 데이터는 건너뜁니다.")

        # ====================================================================
        # 3. 전체 인덱싱
        # ====================================================================
        logger.info("\n" + "=" * 80)
        logger.info("3. 전체 데이터 인덱싱")
        logger.info("=" * 80)

        if all_documents:
            await bulk_index_documents(es, all_documents, batch_size=1000)

            # 인덱스 리프레시
            await es.indices.refresh(index=INDEX_NAME)
            logger.info("인덱스 리프레시 완료")

            # 검증
            await verify_index(es)

            # 자동 백업 (--no-backup 옵션이 없으면)
            if not args.no_backup:
                backup_file = await backup_index_data(es, INDEX_NAME)
                if backup_file:
                    logger.info(f"\n자동 백업 완료: {backup_file.name}")
                else:
                    logger.warning("자동 백업 실패")
        else:
            logger.warning("인덱싱할 문서가 없습니다.")

        logger.info("\n" + "=" * 80)
        logger.info("✓ 모든 작업이 완료되었습니다!")
        logger.info("=" * 80)

    except Exception as e:
        logger.error(f"\n에러 발생: {e}", exc_info=True)
        raise
    finally:
        await es.close()


if __name__ == "__main__":
    asyncio.run(main())
