import json
import asyncio
from datetime import datetime
from enum import Enum
from typing import List, Optional, Dict, Any
from openai import AsyncOpenAI
from qdrant_client.models import PointStruct, Filter, FieldCondition, MatchValue

from app.models.quiz import (
    Quiz, QuizCreate, QuizUpdate, QuizType,
    QuizGenerationRequest, QuizGenerationPrompt
)
from app.models.embedder import get_embedder
from app.vectordb.client import get_qdrant_client
from app.config import get_settings
from app.core.logger import LoggerSetup
from app.core.constants import CATEGORY_MAP

logger = LoggerSetup.get_logger(__name__)
settings = get_settings()


class QuizService:
    def __init__(self):
        self.openai_client = AsyncOpenAI(api_key=settings.openai.openai_api_key)
        qdrant_wrapper = get_qdrant_client()
        self.qdrant_client = qdrant_wrapper.get_client()
        self.embedder = get_embedder()
        self.collection_name = "quiz"
        self.categories = CATEGORY_MAP

    async def generate_quiz_prompt(self, category_id: int, quiz_type: QuizType) -> str:
        category_name = self.categories.get(category_id, "일반")

        if quiz_type == QuizType.OX:
            return f"""다음 조건에 맞는 OX 퀴즈를 1개 생성해주세요:

카테고리: {category_name}
형식: OX 퀴즈 (참/거짓을 판단하는 문제)

다음 JSON 형식으로 정확히 응답해주세요:
{{
    "quiz_title": "퀴즈의 간단한 제목 (50자 이내)",
    "quiz_content": "퀴즈 문제 내용 (명제 형태로 작성)",
    "quiz_correct_answer": "O 또는 X",
    "explanation": "정답이 왜 O(또는 X)인지에 대한 상세한 설명. 문제의 배경 지식, 정답의 근거, 오답일 경우의 이유 등을 포함하여 작성"
}}

주의사항:
- quiz_correct_answer는 반드시 "O" 또는 "X" 중 하나여야 합니다
- 문제는 명확하고 모호하지 않아야 합니다
- 난이도는 일반 성인이 풀 수 있는 수준으로 합니다
- explanation은 단순히 정답만 언급하지 말고, 왜 그것이 정답인지, 어떤 배경 지식이 필요한지 상세히 설명해야 합니다"""
        else:
            return f"""다음 조건에 맞는 4지선다 퀴즈를 1개 생성해주세요:

카테고리: {category_name}
형식: 4지선다 (1, 2, 3, 4 중 택1)

다음 JSON 형식으로 정확히 응답해주세요:
{{
    "quiz_title": "퀴즈의 간단한 제목 (50자 이내)",
    "quiz_content": "퀴즈 문제 내용\\n\\n1. 선택지 1\\n2. 선택지 2\\n3. 선택지 3\\n4. 선택지 4",
    "quiz_correct_answer": "1",
    "explanation": "정답이 왜 해당 번호인지에 대한 상세한 설명. 정답의 근거와 배경 지식, 다른 선택지가 오답인 이유 등을 포함하여 작성"
}}

주의사항:
- quiz_content에는 반드시 문제 내용과 함께 4개의 보기가 "1. ", "2. ", "3. ", "4. " 형식으로 포함되어야 합니다
- quiz_correct_answer는 반드시 "1", "2", "3", "4" 중 정확히 하나의 숫자만 입력해야 합니다 (예: "1")
- 선택지는 명확하게 구분되어야 합니다
- 난이도는 일반 성인이 풀 수 있는 수준으로 합니다
- explanation은 단순히 정답만 언급하지 말고, 왜 그것이 정답인지, 관련 배경 지식과 오답 분석을 포함하여 상세히 설명해야 합니다"""

    async def retrieve_recent_quizzes(self, category_id: int, limit: int = 20) -> List[Dict[str, Any]]:
        try:
            from qdrant_client.models import ScrollRequest, models
            results, _ = self.qdrant_client.scroll(
                collection_name=self.collection_name,
                scroll_filter=Filter(
                    must=[
                        FieldCondition(
                            key="category_id",
                            match=MatchValue(value=category_id)
                        )
                    ]
                ),
                limit=limit,
                with_payload=True,
                with_vectors=False
            )

            recent_quizzes = []
            for point in results:
                recent_quizzes.append({
                    "title": point.payload.get("quiz_title", ""),
                    "content": point.payload.get("quiz_content", ""),
                    "type": point.payload.get("quiz_type", "")
                })

            return recent_quizzes
        except Exception as e:
            logger.error(f"Error retrieving recent quizzes: {e}")
            return []

    async def generate_quiz_prompt_with_context(self, category_id: int, quiz_type: QuizType, existing_quizzes: List[Dict] = None) -> str:
        category_name = self.categories.get(category_id, "일반")

        existing_context = ""
        if existing_quizzes:
            existing_titles = [q["title"] for q in existing_quizzes[:10]]
            if existing_titles:
                existing_context = "\n\n이미 존재하는 퀴즈 제목들 (중복되지 않도록 주의):\n"
                existing_context += "\n".join([f"- {title}" for title in existing_titles])
                existing_context += "\n\n위 퀴즈들과 완전히 다른 주제와 내용으로 새로운 퀴즈를 생성해주세요."

        if quiz_type == QuizType.OX:
            return f"""# Role and Objective
당신은 독립 생활을 하는 성인을 위한 실용적인 퀴즈를 생성하는 전문가입니다.
목표: '{category_name}' 카테고리와 직접 연관된, 실생활에서 활용 가능한 구체적 지식을 테스트하는 고품질 OX 퀴즈 1개를 생성합니다.
{existing_context}

# Critical Quality Standards (모든 기준을 반드시 충족해야 함)

## 1. 난이도 기준 - STRICTLY ENFORCED
**절대 생성하지 말아야 할 퀴즈 유형:**
- ❌ 너무 당연한 상식: "손을 씻으면 위생에 도움이 된다", "규칙적인 운동은 건강에 좋다"
- ❌ 주관적이거나 애매모호한 진술: "자취생은 ~하는 것이 좋다", "~하면 편리하다"
- ❌ 일반적인 조언: "절약하려면 계획을 세워야 한다", "건강을 위해 영양소를 골고루 섭취해야 한다"
- ❌ 검증 불가능한 진술: "대부분의 사람들은 ~", "일반적으로 ~"
- ❌ 논란의 여지가 있는 내용: 개인차가 큰 내용, 상황에 따라 달라지는 내용

**반드시 생성해야 할 퀴즈 유형:**
- ✓ 구체적인 숫자/기준이 포함된 사실: "전입신고는 계약일로부터 14일 이내에 해야 한다"
- ✓ 검증 가능한 방법/절차: "계란을 물에 넣었을 때 가라앉으면 신선한 것이다"
- ✓ 법적/제도적 사실: "월세 세액공제는 연말정산 시 신청할 수 있다"
- ✓ 과학적으로 검증된 방법: "화상을 입었을 때 얼음물에 직접 대는 것은 피부 손상을 악화시킬 수 있다"
- ✓ 실무적으로 활용 가능한 팁: "세탁 기호에서 삼각형은 표백 가능 여부를 나타낸다"

## 2. 카테고리 준수 - MANDATORY
- 반드시 '{category_name}' 카테고리와 **직접적으로** 연관되어야 합니다
- 간접적인 연관성만으로는 불충분합니다
- 카테고리에서 벗어난 일반적인 상식 문제는 절대 생성하지 마세요

## 3. 명확성과 정확성 - NON-NEGOTIABLE
- 논란의 여지가 전혀 없어야 합니다
- 정답이 명확하게 O 또는 X로 판단 가능해야 합니다
- 예외 상황이 있다면 문제에 명시하거나 다른 주제를 선택하세요

## 4. 실용성 - ESSENTIAL
- 알고 있으면 실생활에서 직접 활용할 수 있어야 합니다
- "알면 좋은" 정도가 아닌, "알아야 하는" 수준의 정보여야 합니다

# Reasoning Steps (퀴즈 생성 전 반드시 따라야 할 단계)

1. **주제 선택**: '{category_name}' 카테고리에서 구체적이고 실용적인 주제를 선택합니다
2. **난이도 검증**: 선택한 주제가 위의 "절대 생성하지 말아야 할 유형"에 해당하는지 확인합니다
   - 해당한다면 다른 주제를 선택합니다
   - "반드시 생성해야 할 유형"에 해당하는지 재확인합니다
3. **명제 작성**: 검증 가능한 구체적 사실을 명제 형태로 작성합니다
4. **정답 확인**: 명제가 명확하게 O 또는 X로 판단 가능한지 확인합니다
5. **최종 검증**: 이 퀴즈가 성인에게 너무 쉽거나 당연하지 않은지 확인합니다

# Output Format
다음 JSON 형식으로 **정확히** 응답하세요 (다른 텍스트 포함 금지):
{{
    "quiz_content": "명확하고 구체적인 사실을 담은 명제 (예: '전월세 계약 시 전입신고는 계약일로부터 14일 이내에 해야 한다')",
    "quiz_correct_answer": 0,
    "explanation": "정답의 근거를 구체적으로 설명 (최소 2-3문장, 관련 법규/과학적 원리/실무 기준 등 포함)"
}}

# Technical Requirements
- quiz_correct_answer: 0(정답) 또는 1(오답) **숫자만** 입력
- quiz_content: '젊은층', '청년', '사회초년생', 'MZ세대' 등의 키워드 **절대 사용 금지**
- explanation: 단순 "정답은 O입니다" 수준이 아닌, 왜 그런지 배경 지식과 근거를 포함한 상세 설명

# Final Instruction
위의 모든 기준을 충족하는지 스스로 검증한 후, 확신이 있을 때만 퀴즈를 생성하세요.
기준을 충족하지 못하는 퀴즈는 절대 생성하지 마세요.

문제 예시 방향 (20대 사회초년생, 독립 생활자 중심):

[요리/식품관리] 자취생 필수
- 계란 신선도 확인법 (물에 띄우기), 밥 냉동/해동 방법, 냉장고 정리법 (위/아래 칸 용도)
- 간장:설탕:물 황금비율, 파스타 삶을 때 소금 넣는 이유, 고기 해동 방법
- 제철 식재료 구분, 유통기한 vs 소비기한, 마트 할인 시간대, 배달비 절약 꿀팁

[청소/세탁] 혼자 사는 집 관리
- 세탁 기호 의미 (삼각형, 사각형), 얼룩 종류별 제거법 (김치, 커피, 볼펜)
- 청소 루틴 (매일/주간/월간), 배수구 관리, 곰팡이 예방법
- 이불 세탁 주기, 베개 케어, 옷 개는 법, 다림질 온도 조절

[생활수리/DIY] 셀프 수리 가능한 것들
- 화장실 막힘 (뚫어뻥 사용법), 수도꼭지 누수 응급처치, 전구 교체
- 드라이버 종류와 사용법, 가구 조립 순서, 못 박는 법
- 에어컨 필터 청소, 싱크대 트랩 청소, 문고리 조이기

[생활경제/계약] 돈 관리의 기초
- 전월세 계약 시 필수 확인사항 (등기부등본, 전입신고), 중개수수료 계산법
- 체크카드 vs 신용카드 (혜택, 신용점수), 카드 할인 활용법
- 4대보험 종류와 혜택, 급여명세서 읽는 법, 실수령액 계산
- 연말정산 공제 항목, 월세 세액공제, 청약통장 유지 조건
- 소액 대출 주의사항, 신용등급 관리, 저축 vs 투자 비율

[이사/인테리어] 첫 집 구하기
- 이사 체크리스트 (전입신고, 인터넷, 가스), 집 보러 갈 때 확인사항
- 방향/층수 고려사항 (남향, 저층 습기), 옵션 종류 (풀옵션, 반전세)
- 원룸 공간 분리 아이디어, 조명 색온도 선택, 수납 꿀팁
- 보증금 반환 보증보험, 임대차 보호법, 계약 갱신 청구권

[육아/반려동물] 반려동물 기초 지식
- 강아지/고양이 예방접종 시기, 중성화 적정 시기와 이유
- 사료 선택법 (연령별, 알러지), 간식 급여량, 금지 음식
- 펫티켓 (산책, 대소변), 반려동물 보험, 동물병원 선택법
- 분리불안 대처법, 여행 시 펫시터 vs 호텔, 응급상황 대처

[환경/건강] 기본 건강 관리
- 플라스틱 분류 (1~7번), 재활용 마크 의미, 음식물 쓰레기 vs 일반 쓰레기
- 영양제 복용 시간 (공복/식후), 비타민 종류와 효능, 약 보관법
- 화상 처치 (얼음물 X), 지혈법, 응급처치 우선순위, 구급함 필수 품목
- 수면 주기 (90분 배수), 스트레칭 루틴, 자세 교정, 눈 건강 관리

[스마트홈/가전] 전자기기 관리
- 에어컨/공기청정기 필터 청소 주기, 세탁기 통세척
- 대기전력 차단 (멀티탭), 에너지 등급 읽는 법, 전기요금 누진제
- 와이파이 보안 (비밀번호 변경), 공유기 위치, 인터넷 속도 측정
- 노트북 배터리 관리, 스마트폰 수명 연장법, 백업의 중요성

[사회생활 예절] 직장 생활 기초
- 명함 교환법 (받을 때 두 손), 회식 매너 (자리 배치, 술 따르기)
- 업무 이메일 작성법 (제목, 참조/숨은참조), 직급 호칭법
- 경조사 예절 (부조금액, 조문 순서), 휴가 신청 예의
- 보고서 작성 기본 (육하원칙), 회의 참여 태도, 점심 시간 에티켓

[대중교통/이동] 교통 이용 꿀팁
- 환승 할인 시간 (30분), 교통카드 종류 (후불/선불), 마일리지 적립
- 분실물 찾기 (지하철, 버스, 택시), KTX 할인 (이코노미, 토크할인)
- 카카오T/우버/타다 비교, 전동킥보드 규정, 따릉이 이용법

[병원/약국/보험] 건강 관리 시스템
- 처방전 유효기간 (3일), 제네릭 의약품 vs 오리지널, 약 복용법
- 실비보험 청구 절차, 보험 가입 시기, 국민건강보험 환급
- 건강검진 시기 (2년마다), 진료과 선택법, 응급실 vs 당직의료기관
- 치과 스케일링 보험 적용, 안과 검진 주기, 한의원 건강보험

[관공서/서류] 행정 업무 기초
- 주민등록등본/초본 차이, 인감증명서 vs 본인서명사실확인서
- 전입신고 (14일 이내), 해외여행 시 재외국민 신고
- 병역 관련 신고 (전입/해외여행), 운전면허 갱신 (10년)
- 민원24 활용법, 정부24 공인인증서, 모바일 신분증

[은행/금융] 금융 기초 지식
- 통장 개설 서류, 입출금/적금/예금 차이, 자동이체 vs 자동납부
- 공동인증서 vs 간편인증, 금융인증서 차이
- 이체 한도 설정, 해킹 대응법 (OTP, 보안카드), 파밍/피싱 구분
- CMA 통장 활용, 비상금 통장 만들기, 청년우대형 상품

[일상 생활 꿀팁] 실용적인 라이프 해킹
- 신발 냄새 제거 (베이킹소다, 신문지), 구두 관리법, 운동화 세탁
- 옷에 묻은 껌 제거 (얼음 활용), 정전기 방지, 옷 보풀 제거
- 스마트폰 용량 확보, 배터리 절약 모드, 사진 백업 방법
- 택배 빠르게 받는 법, 반품/교환 절차, 직구 관세 계산
- 얼굴형별 안경 선택, 렌즈 관리법, 안경 김서림 방지
- 우산 관리법 (녹 방지), 장화/레인부츠 선택, 젖은 옷 빨리 말리기
- 모기 물린 곳 가려움 해소, 벌레 퇴치법, 바퀴벌레 퇴치
- 문서 스캔 앱 활용, PDF 편집, 압축파일 해제
- 음식 배달 할인 꿀팁, 멤버십 활용, 포인트 적립 전략
- 셀프 헤어컷 (앞머리, 뒷머리), 염색약 선택, 두피 관리

[알아두면 쓸데있는 신기한 잡학지식] 재미있고 유용한 상식
- 119 vs 112 차이, 긴급 상황별 연락처 (가스 누출, 수도 고장)
- 소화기 사용법 (PASS 원칙), 화재 대피 요령, 완강기 사용법
- 심폐소생술 CPR 방법, 하임리히법 (기도 막힘), AED 사용법
- 명절 선물 추천 (상황별), 세뱃돈 평균 금액, 부의금 봉투 작성법
- 향수 뿌리는 위치와 시간, 보관법, 향수 종류 (EDT, EDP, 퍼퓸)
- 생리 주기 계산, 생리통 완화법, 생리대 vs 탐폰 vs 생리컵
- 숙취 해소 음식과 방법, 음주 전후 관리, 적정 음주량
- 불면증 대처법 (수면 위생), 낮잠 시간 (20분), 시차 적응
- 운동 전후 식사 타이밍, 스트레칭 순서, 근육통 완화
- 안전한 비밀번호 만들기, 2단계 인증, 개인정보 유출 대응
- 명함 정리 방법, 영수증 보관 기간, 중요 서류 스캔 보관
- 피부 타입별 세안법, 선크림 양과 덧바르기, 피부과 시술 종류
- 재난 대비 비상 가방 구성, 지진 대피 요령, 정전 대처법
- 반려동물 응급상황 (초콜릿 섭취, 이물질 삼킴), 동물 병원 찾기
- 명절 스트레스 대처법, 가족 갈등 해결, 친척 호칭 정리

[계절별 생활 팁] 봄/여름/가을/겨울
- 봄: 황사/미세먼지 대처, 꽃가루 알레르기, 환절기 건강 관리
- 여름: 에어컨 청소와 온도 설정, 식중독 예방, 냉방병 대처, 여름휴가 준비
- 가을: 환절기 감기 예방, 가을 타는 이유, 난방기 점검, 김장 시기
- 겨울: 동파 방지, 히터 안전 수칙, 가습기 관리, 겨울철 정전기 방지

[자기계발/학습] 성장을 위한 팁
- 효과적인 목표 설정 (SMART 기법), 습관 만들기 (21일 법칙)
- 독서 습관 들이기, 속독법, 독서 노트 작성법
- 외국어 학습 방법, 영어 공부 순서, 언어 교환 앱 활용
- 자격증 선택 가이드, 공부 루틴, 온라인 강의 플랫폼 비교
- 시간 관리 기법 (포모도로, 타임블로킹), 우선순위 매트릭스
- 멘탈 관리, 스트레스 해소법, 번아웃 증후군 예방

[취업/이직] 커리어 관리
- 이력서 작성 팁, 자기소개서 구조, 포트폴리오 만들기
- 면접 준비 (예상 질문, 복장, 태도), 면접 후 후속 조치
- 연봉 협상 타이밍과 방법, 퇴사 절차, 경력 증명서
- 실업급여 신청 조건과 절차, 내일배움카드, 직업훈련
- 프리랜서 시작하기, 3.3% 세금, 종합소득세 신고

[디지털 라이프] 온라인 필수 스킬
- 구글 드라이브/네이버 클라우드 활용, 파일 공유 방법
- 화상회의 에티켓 (줌, 구글미트), 배경 설정, 음소거 예절
- 유튜브 프리미엄 vs 무료, 넷플릭스 공유 규정, OTT 비교
- 배달앱 쿠폰 활용, 구독 서비스 관리, 멤버십 혜택 비교
- SNS 개인정보 보호 설정, 스팸 차단, 사이버 불링 대응"""
        else:
            return f"""# Role and Objective
당신은 독립 생활을 하는 성인을 위한 실용적인 퀴즈를 생성하는 전문가입니다.
목표: '{category_name}' 카테고리와 직접 연관된, 실생활에서 활용 가능한 구체적 지식을 테스트하는 고품질 4지선다 퀴즈 1개를 생성합니다.
{existing_context}

# Critical Quality Standards (모든 기준을 반드시 충족해야 함)

## 1. 난이도 기준 - STRICTLY ENFORCED
**절대 생성하지 말아야 할 퀴즈 유형:**
- ❌ 너무 당연하거나 추상적인 질문: "건강한 식생활을 위해 필요한 것은?", "청소를 잘하려면?"
- ❌ 주관적이거나 정답이 여러 개인 질문: "가장 좋은 방법은?", "가장 중요한 것은?"
- ❌ 일반적인 상식으로 풀 수 있는 문제: "손을 씻어야 하는 이유는?", "운동이 건강에 좋은 이유는?"
- ❌ 구체성이 없는 애매한 질문: "절약하는 방법은?", "효율적으로 하는 법은?"
- ❌ 검색 없이 추측으로 풀 수 있는 문제

**반드시 생성해야 할 퀴즈 유형:**
- ✓ 구체적인 숫자/절차/방법을 묻는 질문: "전월세 계약 시 중개수수료 상한선은?", "냉동 밥을 해동할 때 가장 좋은 방법은?"
- ✓ 법적/제도적 지식: "전입신고 기한은?", "연말정산 공제 항목 중 틀린 것은?"
- ✓ 실무적 방법/절차: "세탁 기호 중 삼각형의 의미는?", "화상 응급처치 방법은?"
- ✓ 과학적 원리/검증된 팁: "계란 신선도 확인법은?", "냉장고 야채칸이 아래에 있는 이유는?"

## 2. 선택지 품질 기준 - MANDATORY
**오답 선택지 작성 규칙:**
- 그럴듯하게 들려야 합니다 (명백히 틀린 것은 안됨)
- 실제로 혼동할 수 있는 내용이어야 합니다
- 모든 선택지가 비슷한 길이와 구체성을 가져야 합니다
- "모두 해당", "없음" 같은 선택지는 피하세요

**절대 하지 말아야 할 것:**
- ❌ 정답이 너무 명확하게 드러나는 선택지 구성
- ❌ 오답이 말도 안 되게 이상한 경우
- ❌ 선택지 간 길이나 구체성이 너무 다른 경우
- ❌ "1번과 2번", "모두 정답" 같은 복합 선택지

## 3. 카테고리 준수 - MANDATORY
- 반드시 '{category_name}' 카테고리와 **직접적으로** 연관되어야 합니다
- 간접적인 연관성만으로는 불충분합니다
- 카테고리에서 벗어난 일반적인 상식 문제는 절대 생성하지 마세요

## 4. 실용성 - ESSENTIAL
- 알고 있으면 실생활에서 직접 활용할 수 있어야 합니다
- "알면 좋은" 정도가 아닌, "알아야 하는" 수준의 정보여야 합니다
- 성인이 독립 생활을 하면서 실제로 마주칠 수 있는 상황이어야 합니다

# Reasoning Steps (퀴즈 생성 전 반드시 따라야 할 단계)

1. **주제 선택**: '{category_name}' 카테고리에서 구체적이고 실용적인 주제를 선택합니다
2. **난이도 검증**: 선택한 주제가 위의 "절대 생성하지 말아야 할 유형"에 해당하는지 확인합니다
   - 해당한다면 다른 주제를 선택합니다
   - "반드시 생성해야 할 유형"에 해당하는지 재확인합니다
3. **질문 작성**: 명확하고 구체적인 질문을 작성합니다 (애매모호하지 않게)
4. **선택지 작성**:
   - 정답 1개를 먼저 작성합니다
   - 그럴듯한 오답 3개를 작성합니다 (추측으로 맞히기 어렵게)
   - 모든 선택지가 비슷한 수준의 구체성을 가지도록 조정합니다
5. **최종 검증**:
   - 이 퀴즈가 성인에게 너무 쉽거나 당연하지 않은지 확인합니다
   - 선택지만 보고도 정답이 명확하게 드러나지 않는지 확인합니다

# Output Format
다음 JSON 형식으로 **정확히** 응답하세요 (다른 텍스트 포함 금지):
{{
    "quiz_title": "명확하고 구체적인 질문 (예: '냉동 밥을 해동할 때 가장 영양소 손실이 적은 방법은?')",
    "quiz_content": "선택지1\\n선택지2\\n선택지3\\n선택지4",
    "quiz_correct_answer": 0,
    "explanation": "정답의 근거를 구체적으로 설명하고, 왜 다른 선택지들이 오답인지도 간략히 설명 (최소 3-4문장)"
}}

# Technical Requirements
- quiz_title: 구체적인 질문 (100자 이내, 애매모호한 표현 금지)
- quiz_content: 4개의 선택지를 \\n으로만 구분 (번호 없이 순수 텍스트만)
- quiz_correct_answer: 0, 1, 2, 3 중 하나 **숫자만** (0=첫번째, 1=두번째, 2=세번째, 3=네번째)
- 모든 선택지는 비슷한 길이와 구체성을 가져야 함
- '젊은층', '청년', '사회초년생', 'MZ세대' 등의 키워드 **절대 사용 금지**
- explanation: 정답의 근거 + 오답 분석을 포함한 상세 설명

# Final Instruction
위의 모든 기준을 충족하는지 스스로 검증한 후, 확신이 있을 때만 퀴즈를 생성하세요.
기준을 충족하지 못하는 퀴즈는 절대 생성하지 마세요.
특히 "너무 쉽거나 당연한 문제"는 절대 만들지 마세요.

문제 예시 방향 (20대 사회초년생, 독립 생활자 중심):

[요리/식품관리] 자취생 필수
- 계란 신선도 확인법 (물에 띄우기), 밥 냉동/해동 방법, 냉장고 정리법 (위/아래 칸 용도)
- 간장:설탕:물 황금비율, 파스타 삶을 때 소금 넣는 이유, 고기 해동 방법
- 제철 식재료 구분, 유통기한 vs 소비기한, 마트 할인 시간대, 배달비 절약 꿀팁

[청소/세탁] 혼자 사는 집 관리
- 세탁 기호 의미 (삼각형, 사각형), 얼룩 종류별 제거법 (김치, 커피, 볼펜)
- 청소 루틴 (매일/주간/월간), 배수구 관리, 곰팡이 예방법
- 이불 세탁 주기, 베개 케어, 옷 개는 법, 다림질 온도 조절

[생활수리/DIY] 셀프 수리 가능한 것들
- 화장실 막힘 (뚫어뻥 사용법), 수도꼭지 누수 응급처치, 전구 교체
- 드라이버 종류와 사용법, 가구 조립 순서, 못 박는 법
- 에어컨 필터 청소, 싱크대 트랩 청소, 문고리 조이기

[생활경제/계약] 돈 관리의 기초
- 전월세 계약 시 필수 확인사항 (등기부등본, 전입신고), 중개수수료 계산법
- 체크카드 vs 신용카드 (혜택, 신용점수), 카드 할인 활용법
- 4대보험 종류와 혜택, 급여명세서 읽는 법, 실수령액 계산
- 연말정산 공제 항목, 월세 세액공제, 청약통장 유지 조건
- 소액 대출 주의사항, 신용등급 관리, 저축 vs 투자 비율

[이사/인테리어] 첫 집 구하기
- 이사 체크리스트 (전입신고, 인터넷, 가스), 집 보러 갈 때 확인사항
- 방향/층수 고려사항 (남향, 저층 습기), 옵션 종류 (풀옵션, 반전세)
- 원룸 공간 분리 아이디어, 조명 색온도 선택, 수납 꿀팁
- 보증금 반환 보증보험, 임대차 보호법, 계약 갱신 청구권

[육아/반려동물] 반려동물 기초 지식
- 강아지/고양이 예방접종 시기, 중성화 적정 시기와 이유
- 사료 선택법 (연령별, 알러지), 간식 급여량, 금지 음식
- 펫티켓 (산책, 대소변), 반려동물 보험, 동물병원 선택법
- 분리불안 대처법, 여행 시 펫시터 vs 호텔, 응급상황 대처

[환경/건강] 기본 건강 관리
- 플라스틱 분류 (1~7번), 재활용 마크 의미, 음식물 쓰레기 vs 일반 쓰레기
- 영양제 복용 시간 (공복/식후), 비타민 종류와 효능, 약 보관법
- 화상 처치 (얼음물 X), 지혈법, 응급처치 우선순위, 구급함 필수 품목
- 수면 주기 (90분 배수), 스트레칭 루틴, 자세 교정, 눈 건강 관리

[스마트홈/가전] 전자기기 관리
- 에어컨/공기청정기 필터 청소 주기, 세탁기 통세척
- 대기전력 차단 (멀티탭), 에너지 등급 읽는 법, 전기요금 누진제
- 와이파이 보안 (비밀번호 변경), 공유기 위치, 인터넷 속도 측정
- 노트북 배터리 관리, 스마트폰 수명 연장법, 백업의 중요성

[사회생활 예절] 직장 생활 기초
- 명함 교환법 (받을 때 두 손), 회식 매너 (자리 배치, 술 따르기)
- 업무 이메일 작성법 (제목, 참조/숨은참조), 직급 호칭법
- 경조사 예절 (부조금액, 조문 순서), 휴가 신청 예의
- 보고서 작성 기본 (육하원칙), 회의 참여 태도, 점심 시간 에티켓

[대중교통/이동] 교통 이용 꿀팁
- 환승 할인 시간 (30분), 교통카드 종류 (후불/선불), 마일리지 적립
- 분실물 찾기 (지하철, 버스, 택시), KTX 할인 (이코노미, 토크할인)
- 카카오T/우버/타다 비교, 전동킥보드 규정, 따릉이 이용법

[병원/약국/보험] 건강 관리 시스템
- 처방전 유효기간 (3일), 제네릭 의약품 vs 오리지널, 약 복용법
- 실비보험 청구 절차, 보험 가입 시기, 국민건강보험 환급
- 건강검진 시기 (2년마다), 진료과 선택법, 응급실 vs 당직의료기관
- 치과 스케일링 보험 적용, 안과 검진 주기, 한의원 건강보험

[관공서/서류] 행정 업무 기초
- 주민등록등본/초본 차이, 인감증명서 vs 본인서명사실확인서
- 전입신고 (14일 이내), 해외여행 시 재외국민 신고
- 병역 관련 신고 (전입/해외여행), 운전면허 갱신 (10년)
- 민원24 활용법, 정부24 공인인증서, 모바일 신분증

[은행/금융] 금융 기초 지식
- 통장 개설 서류, 입출금/적금/예금 차이, 자동이체 vs 자동납부
- 공동인증서 vs 간편인증, 금융인증서 차이
- 이체 한도 설정, 해킹 대응법 (OTP, 보안카드), 파밍/피싱 구분
- CMA 통장 활용, 비상금 통장 만들기, 청년우대형 상품

[일상 생활 꿀팁] 실용적인 라이프 해킹
- 신발 냄새 제거 (베이킹소다, 신문지), 구두 관리법, 운동화 세탁
- 옷에 묻은 껌 제거 (얼음 활용), 정전기 방지, 옷 보풀 제거
- 스마트폰 용량 확보, 배터리 절약 모드, 사진 백업 방법
- 택배 빠르게 받는 법, 반품/교환 절차, 직구 관세 계산
- 얼굴형별 안경 선택, 렌즈 관리법, 안경 김서림 방지
- 우산 관리법 (녹 방지), 장화/레인부츠 선택, 젖은 옷 빨리 말리기
- 모기 물린 곳 가려움 해소, 벌레 퇴치법, 바퀴벌레 퇴치
- 문서 스캔 앱 활용, PDF 편집, 압축파일 해제
- 음식 배달 할인 꿀팁, 멤버십 활용, 포인트 적립 전략
- 셀프 헤어컷 (앞머리, 뒷머리), 염색약 선택, 두피 관리

[알아두면 쓸데있는 신기한 잡학지식] 재미있고 유용한 상식
- 119 vs 112 차이, 긴급 상황별 연락처 (가스 누출, 수도 고장)
- 소화기 사용법 (PASS 원칙), 화재 대피 요령, 완강기 사용법
- 심폐소생술 CPR 방법, 하임리히법 (기도 막힘), AED 사용법
- 명절 선물 추천 (상황별), 세뱃돈 평균 금액, 부의금 봉투 작성법
- 향수 뿌리는 위치와 시간, 보관법, 향수 종류 (EDT, EDP, 퍼퓸)
- 생리 주기 계산, 생리통 완화법, 생리대 vs 탐폰 vs 생리컵
- 숙취 해소 음식과 방법, 음주 전후 관리, 적정 음주량
- 불면증 대처법 (수면 위생), 낮잠 시간 (20분), 시차 적응
- 운동 전후 식사 타이밍, 스트레칭 순서, 근육통 완화
- 안전한 비밀번호 만들기, 2단계 인증, 개인정보 유출 대응
- 명함 정리 방법, 영수증 보관 기간, 중요 서류 스캔 보관
- 피부 타입별 세안법, 선크림 양과 덧바르기, 피부과 시술 종류
- 재난 대비 비상 가방 구성, 지진 대피 요령, 정전 대처법
- 반려동물 응급상황 (초콜릿 섭취, 이물질 삼킴), 동물 병원 찾기
- 명절 스트레스 대처법, 가족 갈등 해결, 친척 호칭 정리

[계절별 생활 팁] 봄/여름/가을/겨울
- 봄: 황사/미세먼지 대처, 꽃가루 알레르기, 환절기 건강 관리
- 여름: 에어컨 청소와 온도 설정, 식중독 예방, 냉방병 대처, 여름휴가 준비
- 가을: 환절기 감기 예방, 가을 타는 이유, 난방기 점검, 김장 시기
- 겨울: 동파 방지, 히터 안전 수칙, 가습기 관리, 겨울철 정전기 방지

[자기계발/학습] 성장을 위한 팁
- 효과적인 목표 설정 (SMART 기법), 습관 만들기 (21일 법칙)
- 독서 습관 들이기, 속독법, 독서 노트 작성법
- 외국어 학습 방법, 영어 공부 순서, 언어 교환 앱 활용
- 자격증 선택 가이드, 공부 루틴, 온라인 강의 플랫폼 비교
- 시간 관리 기법 (포모도로, 타임블로킹), 우선순위 매트릭스
- 멘탈 관리, 스트레스 해소법, 번아웃 증후군 예방

[취업/이직] 커리어 관리
- 이력서 작성 팁, 자기소개서 구조, 포트폴리오 만들기
- 면접 준비 (예상 질문, 복장, 태도), 면접 후 후속 조치
- 연봉 협상 타이밍과 방법, 퇴사 절차, 경력 증명서
- 실업급여 신청 조건과 절차, 내일배움카드, 직업훈련
- 프리랜서 시작하기, 3.3% 세금, 종합소득세 신고

[디지털 라이프] 온라인 필수 스킬
- 구글 드라이브/네이버 클라우드 활용, 파일 공유 방법
- 화상회의 에티켓 (줌, 구글미트), 배경 설정, 음소거 예절
- 유튜브 프리미엄 vs 무료, 넷플릭스 공유 규정, OTT 비교
- 배달앱 쿠폰 활용, 구독 서비스 관리, 멤버십 혜택 비교
- SNS 개인정보 보호 설정, 스팸 차단, 사이버 불링 대응"""

    async def check_title_similarity(self, title: str, category_id: int, threshold: float = 0.85) -> List[Dict]:
        try:
            embedding = self.embedder.embed(title)

            search_result = self.qdrant_client.search(
                collection_name=self.collection_name,
                query_vector=embedding.tolist(),
                query_filter=Filter(
                    must=[
                        FieldCondition(
                            key="category_id",
                            match=MatchValue(value=category_id)
                        )
                    ]
                ),
                limit=5,
                score_threshold=threshold
            )

            similar_titles = []
            for hit in search_result:
                similar_titles.append({
                    "title": hit.payload.get("quiz_title"),
                    "score": hit.score
                })

            return similar_titles
        except Exception as e:
            logger.error(f"Error checking title similarity: {e}")
            return []

    async def generate_quiz_with_rag(self, request: QuizGenerationRequest) -> List[Quiz]:
        generated_quizzes = []
        existing_quizzes = await self.retrieve_recent_quizzes(
            category_id=request.category_id,
            limit=20
        )

        for _ in range(request.count):
            max_retries = 3
            retry_count = 0

            while retry_count < max_retries:
                try:
                    quiz_type = request.quiz_type or QuizType.OX
                    prompt = await self.generate_quiz_prompt_with_context(
                        request.category_id,
                        quiz_type,
                        existing_quizzes
                    )

                    response = await self.openai_client.chat.completions.create(
                        model="gpt-4o-mini",
                        messages=[
                            {"role": "system", "content": """You are a professional quiz creator specialized in generating high-quality, practical quizzes for independent adults.

# Core Principles
- NEVER generate trivial or obvious questions that any adult would know
- ALWAYS ensure questions require specific, verifiable knowledge
- Questions must be directly applicable to real-life situations
- Every quiz must meet ALL quality standards specified in the user prompt

# Response Requirements
- Respond ONLY with valid JSON (no additional text or explanation)
- Follow the exact JSON schema provided in the user prompt
- Verify your quiz meets ALL criteria before responding
- If you cannot generate a quiz that meets ALL standards, try a different topic

# Quality Commitment
You are committed to excellence. Every quiz you generate must be:
1. Specific and concrete (not vague or general)
2. Practical and useful (applicable to real life)
3. Appropriately challenging (not too easy for adults)
4. Factually accurate and verifiable
5. Category-relevant (directly related to the specified category)

Remember: It is better to select a different topic than to compromise on quality standards."""},
                            {"role": "user", "content": prompt}
                        ],
                        response_format={"type": "json_object"},
                        temperature=0.7
                    )

                    quiz_data = json.loads(response.choices[0].message.content)

                    # OX 퀴즈의 경우 quiz_title에 quiz_content를 저장
                    if quiz_type == QuizType.OX:
                        quiz_title_for_storage = quiz_data["quiz_content"]
                    else:
                        quiz_title_for_storage = quiz_data.get("quiz_title", "")

                    similar_titles = await self.check_title_similarity(
                        quiz_title_for_storage,
                        request.category_id,
                        threshold=0.75
                    )

                    if similar_titles:
                        logger.info(f"Similar quiz found: {similar_titles[0]['title']} (score: {similar_titles[0]['score']})")
                        existing_quizzes.append({"title": quiz_title_for_storage})
                        retry_count += 1
                        continue

                    quiz = QuizCreate(
                        quiz_category_id=request.category_id,
                        quiz_title=quiz_title_for_storage,
                        quiz_content=quiz_data["quiz_content"],
                        quiz_type=quiz_type,
                        quiz_correct_answer=quiz_data["quiz_correct_answer"],
                        explanation=quiz_data["explanation"]
                    )

                    saved_quiz = await self.create_quiz(quiz)
                    generated_quizzes.append(saved_quiz)
                    existing_quizzes.append({"title": quiz_data["quiz_title"]})
                    break

                except Exception as e:
                    logger.error(f"Error generating quiz: {e}")
                    retry_count += 1
                    if retry_count >= max_retries:
                        raise Exception(f"Failed to generate quiz after {max_retries} retries")
                    await asyncio.sleep(1)

        return generated_quizzes

    async def generate_quiz(self, request: QuizGenerationRequest) -> List[Quiz]:
        return await self.generate_quiz_with_rag(request)

    async def create_quiz(self, quiz: QuizCreate) -> Quiz:
        try:
            import uuid
            from app.db.database import get_engine
            from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker
            from app.db.repositories import QuizRepository

            # 1. MySQL에 먼저 저장하여 AUTO_INCREMENT ID 획득
            engine = get_engine()
            async_session_local = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

            async with async_session_local() as session:
                repo = QuizRepository(session)
                saved_quiz = await repo.create(quiz)
                quiz_id = saved_quiz.quiz_id  # MySQL AUTO_INCREMENT로 생성된 ID

            logger.info(f"Quiz saved to MySQL with AUTO_INCREMENT ID: {quiz_id}")

            # 2. Qdrant에 동일한 quiz_id로 저장
            point_id = str(uuid.uuid4())  # Qdrant용 UUID (내부 ID)
            embedding = self.embedder.embed(quiz.quiz_title)

            point = PointStruct(
                id=point_id,
                vector=embedding.tolist(),
                payload={
                    "quiz_id": quiz_id,  # MySQL과 동일한 ID 사용
                    "category_id": quiz.quiz_category_id,
                    "quiz_title": quiz.quiz_title,
                    "quiz_content": quiz.quiz_content,
                    "quiz_type": quiz.quiz_type.value,
                    "quiz_correct_answer": quiz.quiz_correct_answer,
                    "explanation": quiz.explanation,
                    "created_at": int(datetime.now().timestamp()),
                    "updated_at": int(datetime.now().timestamp())
                }
            )

            self.qdrant_client.upsert(
                collection_name=self.collection_name,
                points=[point]
            )

            logger.info(f"Quiz saved to Qdrant with quiz_id: {quiz_id}")

            return saved_quiz
        except Exception as e:
            logger.error(f"Error creating quiz: {e}")
            raise

    async def update_quiz(self, quiz_id: int, quiz_update: QuizUpdate) -> Optional[Quiz]:
        try:
            # quiz_id로 먼저 검색
            search_filter = Filter(
                must=[
                    FieldCondition(
                        key="quiz_id",
                        match=MatchValue(value=quiz_id)
                    )
                ]
            )

            search_result = self.qdrant_client.scroll(
                collection_name=self.collection_name,
                scroll_filter=search_filter,
                limit=1
            )

            if not search_result[0]:
                return None

            point = search_result[0][0]

            existing_quiz = point.payload

            update_data = quiz_update.dict(exclude_unset=True)
            for key, value in update_data.items():
                if value is not None:
                    existing_quiz[key] = value if not isinstance(value, Enum) else value.value

            existing_quiz["updated_at"] = int(datetime.now().timestamp())

            if "quiz_title" in update_data:
                embedding = self.embedder.embed(update_data["quiz_title"])
                vector = embedding.tolist()
            else:
                # 기존 벡터를 유지 (point.vector가 None일 수 있음)
                if hasattr(point, 'vector') and point.vector is not None:
                    vector = point.vector
                else:
                    # 기존 제목으로 다시 임베딩 생성
                    embedding = self.embedder.embed(existing_quiz.get("quiz_title", ""))
                    vector = embedding.tolist()

            updated_point = PointStruct(
                id=point.id,
                vector=vector,
                payload=existing_quiz
            )

            self.qdrant_client.upsert(
                collection_name=self.collection_name,
                points=[updated_point]
            )

            return Quiz(
                quiz_id=quiz_id,
                quiz_category_id=existing_quiz["category_id"],
                quiz_title=existing_quiz["quiz_title"],
                quiz_content=existing_quiz["quiz_content"],
                quiz_type=QuizType(existing_quiz["quiz_type"]),
                quiz_correct_answer=existing_quiz["quiz_correct_answer"],
                explanation=existing_quiz.get("explanation"),
                quiz_created_at=datetime.fromtimestamp(existing_quiz["created_at"]),
                quiz_updated_at=datetime.fromtimestamp(existing_quiz["updated_at"])
            )
        except Exception as e:
            logger.error(f"Error updating quiz: {e}")
            raise

    async def get_quiz(self, quiz_id: int) -> Optional[Quiz]:
        try:
            search_filter = Filter(
                must=[
                    FieldCondition(
                        key="quiz_id",
                        match=MatchValue(value=quiz_id)
                    )
                ]
            )

            search_result = self.qdrant_client.scroll(
                collection_name=self.collection_name,
                scroll_filter=search_filter,
                limit=1
            )

            if not search_result[0]:
                return None

            payload = search_result[0][0].payload
            return Quiz(
                quiz_id=payload["quiz_id"],
                quiz_category_id=payload["category_id"],
                quiz_title=payload["quiz_title"],
                quiz_content=payload["quiz_content"],
                quiz_type=QuizType(payload["quiz_type"]),
                quiz_correct_answer=payload["quiz_correct_answer"],
                explanation=payload.get("explanation"),
                quiz_created_at=datetime.fromtimestamp(payload["created_at"]),
                quiz_updated_at=datetime.fromtimestamp(payload["updated_at"])
            )
        except Exception as e:
            logger.error(f"Error retrieving quiz: {e}")
            return None

    async def list_quizzes(self, category_id: Optional[int] = None, limit: int = 100) -> List[Quiz]:
        try:
            filter_conditions = []
            if category_id:
                filter_conditions.append(
                    FieldCondition(
                        key="category_id",
                        match=MatchValue(value=category_id)
                    )
                )

            query_filter = Filter(must=filter_conditions) if filter_conditions else None

            result = self.qdrant_client.scroll(
                collection_name=self.collection_name,
                scroll_filter=query_filter,
                limit=limit
            )

            quizzes = []
            for point in result[0]:
                payload = point.payload
                quiz = Quiz(
                    quiz_id=payload["quiz_id"],
                    quiz_category_id=payload["category_id"],
                    quiz_title=payload["quiz_title"],
                    quiz_content=payload["quiz_content"],
                    quiz_type=QuizType(payload["quiz_type"]),
                    quiz_correct_answer=payload["quiz_correct_answer"],
                    explanation=payload.get("explanation"),
                    quiz_created_at=datetime.fromtimestamp(payload["created_at"]),
                    quiz_updated_at=datetime.fromtimestamp(payload["updated_at"])
                )
                quizzes.append(quiz)

            return quizzes
        except Exception as e:
            logger.error(f"Error listing quizzes: {e}")
            return []