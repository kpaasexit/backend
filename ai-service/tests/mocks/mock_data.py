"""Mock data generators and constants for testing."""

import numpy as np
from typing import Dict, List, Any, Tuple
from datetime import datetime, timedelta
import random


# Sample test data constants
SAMPLE_QUESTIONS = {
    "요리/식품관리": [
        "김치찌개 맛있게 끓이는 방법",
        "계란 신선도 확인하는 법",
        "돼지고기 누린내 제거 방법",
        "밥이 너무 질었을 때 해결법",
        "양파 자르면서 눈물 안나는 방법",
    ],
    "청소/세탁": [
        "화장실 곰팡이 제거하는 방법",
        "흰 옷 얼룩 빼는 법",
        "카펫 청소 방법",
        "유리창 깨끗하게 닦는 법",
        "세탁기 청소하는 방법",
    ],
    "생활수리/DIY": [
        "형광등 교체하는 방법",
        "수도꼭지 고치는 법",
        "문짝 소리 없애기",
        "벽에 못 박는 방법",
        "변기 막혔을 때 뚫는 법",
    ],
    "생활경제/계약": [
        "전세 계약 주의사항",
        "신용카드 연회비 절약법",
        "대출 이자 계산법",
        "부동산 중개수수료 협상법",
        "생활비 절약 노하우",
    ],
    "육아/반려동물": [
        "강아지 훈련 방법",
        "아기 이유식 시작 시기",
        "고양이 털 관리법",
        "신생아 목욕시키는 방법",
        "강아지 산책 시간",
    ],
    "이사/인테리어": [
        "원룸 인테리어 팁",
        "이사 업체 선택법",
        "작은 방 꾸미기",
        "가구 배치하는 방법",
        "벽지 고르는 요령",
    ],
    "환경/건강": [
        "미세먼지 대처법",
        "실내 공기 정화 방법",
        "수면 질 개선법",
        "목 결림 해결법",
        "스트레스 관리 방법",
    ],
    "스마트홈/가전": [
        "스마트홈 구축 방법",
        "에어컨 필터 청소",
        "로봇청소기 관리법",
        "냉장고 정리하는 법",
        "세탁기 사용법",
    ]
}

SAMPLE_EMBEDDINGS = {
    "dim_384": np.random.rand(384).astype(np.float32),
    "dim_768": np.random.rand(768).astype(np.float32),
    "dim_1536": np.random.rand(1536).astype(np.float32),
    "batch_5x384": np.random.rand(5, 384).astype(np.float32),
}

SAMPLE_CLASSIFICATIONS = [
    ("요리/식품관리", 0.95),
    ("청소/세탁", 0.87),
    ("생활수리/DIY", 0.92),
    ("생활경제/계약", 0.78),
    ("육아/반려동물", 0.88),
    ("이사/인테리어", 0.85),
    ("환경/건강", 0.91),
    ("스마트홈/가전", 0.89),
]


class MockDataGenerator:
    """Generator for creating realistic mock test data."""

    def __init__(self, seed: int = 42):
        """Initialize with random seed for reproducible data."""
        self.seed = seed
        random.seed(seed)
        np.random.seed(seed)

    def generate_question_data(
        self,
        category: str = "요리/식품관리",
        include_content: bool = True,
        include_tags: bool = True
    ) -> Dict[str, Any]:
        """Generate realistic question data."""
        questions = SAMPLE_QUESTIONS.get(category, SAMPLE_QUESTIONS["요리/식품관리"])
        title = random.choice(questions)

        data = {
            "question_id": f"q_{random.randint(1000, 9999)}",
            "title": title,
            "category": category,
            "created_at": self._random_timestamp(),
            "updated_at": self._random_timestamp(),
        }

        if include_content:
            data["content"] = self._generate_content(title, category)

        if include_tags:
            data["tags"] = self._generate_tags(title, category)

        return data

    def generate_embedding(
        self,
        dimension: int = 384,
        text: str = None,
        normalize: bool = True
    ) -> np.ndarray:
        """Generate deterministic embedding based on text or random."""
        if text:
            # Generate deterministic embedding based on text hash
            text_hash = hash(text) % (2**31)
            np.random.seed(text_hash)

        embedding = np.random.rand(dimension).astype(np.float32)

        if normalize:
            norm = np.linalg.norm(embedding)
            if norm > 0:
                embedding = embedding / norm

        # Reset seed
        np.random.seed(self.seed)
        return embedding

    def generate_batch_embeddings(
        self,
        texts: List[str],
        dimension: int = 384,
        normalize: bool = True
    ) -> np.ndarray:
        """Generate batch embeddings."""
        embeddings = []
        for text in texts:
            embedding = self.generate_embedding(dimension, text, normalize)
            embeddings.append(embedding)

        return np.array(embeddings)

    def generate_classification_result(
        self,
        text: str,
        target_category: str = None
    ) -> Tuple[str, float]:
        """Generate realistic classification result."""
        if target_category and target_category in SAMPLE_QUESTIONS:
            category = target_category
            # High confidence for specified target
            confidence = random.uniform(0.85, 0.98)
        else:
            # Classify based on text content (simplified)
            category = self._classify_text_simple(text)
            confidence = random.uniform(0.6, 0.95)

        return category, confidence

    def generate_similar_questions(
        self,
        query_text: str,
        category: str = None,
        count: int = 5
    ) -> List[Dict[str, Any]]:
        """Generate list of similar question results."""
        results = []

        # Use category-specific questions if provided
        if category and category in SAMPLE_QUESTIONS:
            source_questions = SAMPLE_QUESTIONS[category]
        else:
            # Mix questions from all categories
            source_questions = []
            for q_list in SAMPLE_QUESTIONS.values():
                source_questions.extend(q_list)

        # Select random questions and assign similarity scores
        selected_questions = random.sample(
            source_questions,
            min(count, len(source_questions))
        )

        for i, question in enumerate(selected_questions):
            # Assign decreasing similarity scores
            similarity = random.uniform(0.7 - i * 0.1, 0.9 - i * 0.05)
            similarity = max(0.3, similarity)  # Minimum threshold

            results.append({
                "question_id": f"similar_{random.randint(1000, 9999)}",
                "title": question,
                "category": self._classify_text_simple(question)[0],
                "similarity": similarity,
                "content_sample": self._generate_content_sample(question),
                "created_at": self._random_timestamp()
            })

        # Sort by similarity score (descending)
        results.sort(key=lambda x: x["similarity"], reverse=True)
        return results

    def generate_answer_response(
        self,
        question: str,
        category: str = None
    ) -> str:
        """Generate contextually appropriate answer."""
        if category:
            question_category = category
        else:
            question_category = self._classify_text_simple(question)[0]

        # Category-specific answer templates
        answer_templates = {
            "요리/식품관리": [
                "요리할 때는 신선한 재료를 사용하는 것이 중요합니다.",
                "조리 전 충분한 준비를 하고 단계별로 진행하세요.",
                "적절한 온도와 시간을 유지하는 것이 맛의 핵심입니다."
            ],
            "청소/세탁": [
                "청소할 때는 안전한 세제를 사용하고 환기를 잘 시켜주세요.",
                "얼룩은 빨리 처리할수록 제거하기 쉽습니다.",
                "정기적인 청소로 쾌적한 환경을 유지하세요."
            ],
            "생활수리/DIY": [
                "수리하기 전에 안전장비를 착용하고 전원을 차단하세요.",
                "적절한 도구를 사용하면 작업이 훨씬 쉬워집니다.",
                "복잡한 수리는 전문가에게 의뢰하는 것이 안전합니다."
            ],
            "생활경제/계약": [
                "계약서는 꼼꼼히 읽어보고 불명확한 부분은 질문하세요.",
                "금융상품 선택 시 수수료와 조건을 비교해보세요.",
                "장기적인 관점에서 경제계획을 세우는 것이 중요합니다."
            ],
            "육아/반려동물": [
                "인내심을 갖고 꾸준히 교육하는 것이 중요합니다.",
                "안전을 최우선으로 생각하고 전문가의 조언을 구하세요.",
                "사랑과 관심으로 꾸준히 돌봐주시면 좋은 결과를 얻을 수 있습니다."
            ],
            "이사/인테리어": [
                "공간의 특성을 파악하고 용도에 맞게 계획하세요.",
                "예산을 미리 정하고 우선순위를 정해서 진행하세요.",
                "전문가의 조언을 구하면 더 나은 결과를 얻을 수 있습니다."
            ],
            "환경/건강": [
                "꾸준한 관리와 좋은 습관이 건강의 기본입니다.",
                "환경 변화에 적극적으로 대응하는 것이 중요합니다.",
                "전문가의 조언을 구하고 정기적인 검진을 받으세요."
            ],
            "스마트홈/가전": [
                "사용설명서를 숙지하고 정기적인 관리를 해주세요.",
                "기술의 발전을 활용하되 안전을 우선으로 생각하세요.",
                "문제가 생기면 A/S센터에 문의하는 것이 좋습니다."
            ]
        }

        templates = answer_templates.get(
            question_category,
            answer_templates["요리/식품관리"]
        )

        return random.choice(templates)

    def _generate_content(self, title: str, category: str) -> str:
        """Generate realistic content for a question."""
        base_content = f"'{title}'에 대해 자세히 알고 싶습니다."

        category_specific = {
            "요리/식품관리": "맛있게 만드는 비법이나 주의사항이 있다면 알려주세요.",
            "청소/세탁": "효과적인 방법과 사용해야 할 도구나 세제를 추천해주세요.",
            "생활수리/DIY": "필요한 도구와 안전한 작업 방법을 알려주세요.",
            "생활경제/계약": "주의해야 할 점과 절약할 수 있는 방법을 알려주세요.",
            "육아/반려동물": "올바른 방법과 주의사항을 자세히 설명해주세요.",
            "이사/인테리어": "실용적인 팁과 예산 내에서 할 수 있는 방법을 알려주세요.",
            "환경/건강": "건강에 도움이 되는 방법을 구체적으로 알려주세요.",
            "스마트홈/가전": "효율적인 사용법과 관리 방법을 알려주세요."
        }

        additional = category_specific.get(category, "도움이 될 만한 정보를 알려주세요.")
        return f"{base_content} {additional}"

    def _generate_content_sample(self, title: str) -> str:
        """Generate brief content sample for similar questions."""
        return f"{title}에 대한 질문입니다. 구체적인 방법과 주의사항을 알고 싶어서 문의드립니다..."

    def _generate_tags(self, title: str, category: str) -> List[str]:
        """Generate relevant tags for a question."""
        # Category-based base tags
        category_tags = {
            "요리/식품관리": ["요리", "음식", "레시피"],
            "청소/세탁": ["청소", "세탁", "정리"],
            "생활수리/DIY": ["수리", "diy", "공구"],
            "생활경제/계약": ["절약", "계약", "금융"],
            "육아/반려동물": ["육아", "반려동물", "돌봄"],
            "이사/인테리어": ["인테리어", "이사", "정리"],
            "환경/건강": ["건강", "환경", "관리"],
            "스마트홈/가전": ["가전", "기술", "스마트홈"]
        }

        base_tags = category_tags.get(category, ["생활", "팁"])

        # Extract keywords from title
        common_words = ["방법", "법", "하는", "있는", "때", "시", "의", "을", "를", "이", "가"]
        title_words = [word for word in title.split() if word not in common_words and len(word) > 1]

        # Combine and limit
        all_tags = list(set(base_tags + title_words[:2]))
        return all_tags[:5]  # Limit to 5 tags

    def _classify_text_simple(self, text: str) -> Tuple[str, float]:
        """Simple text classification based on keywords."""
        text_lower = text.lower()

        category_keywords = {
            "요리/식품관리": ["김치", "요리", "음식", "식품", "레시피", "조리", "계란", "고기", "밥"],
            "청소/세탁": ["청소", "세탁", "빨래", "얼룩", "곰팡이", "화장실", "카펫"],
            "생활수리/DIY": ["수리", "diy", "형광등", "교체", "고장", "못", "수도꼭지", "변기"],
            "생활경제/계약": ["전세", "계약", "대출", "이자", "신용카드", "연회비", "절약"],
            "육아/반려동물": ["강아지", "고양이", "반려동물", "아기", "이유식", "육아"],
            "이사/인테리어": ["이사", "인테리어", "원룸", "꾸미기", "가구", "벽지"],
            "환경/건강": ["미세먼지", "환경", "건강", "공기", "수면", "스트레스"],
            "스마트홈/가전": ["에어컨", "가전", "스마트홈", "로봇청소기", "냉장고", "세탁기"]
        }

        best_category = "요리/식품관리"
        best_score = 0

        for category, keywords in category_keywords.items():
            score = sum(1 for keyword in keywords if keyword in text_lower)
            if score > best_score:
                best_category = category
                best_score = score

        confidence = min(0.95, 0.6 + best_score * 0.1)
        return best_category, confidence

    def _random_timestamp(self) -> int:
        """Generate random timestamp within last 30 days."""
        now = datetime.now()
        past = now - timedelta(days=30)
        random_time = past + (now - past) * random.random()
        return int(random_time.timestamp() * 1000)  # Milliseconds