from enum import Enum, auto

class ModelType(Enum):
    CLASSIFIER = auto()
    EMBEDDER = auto()
    TOKENIZER = auto()

class Language(Enum):
    KOREAN = "ko"
    ENGLISH = "en"
    MIXED = "mixed"

class TaskStatus(Enum):
    PENDING = "pending"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"

CATEGORY_MAP = {
    1: "요리/식품관리",
    2: "청소/세탁",
    3: "생활수리/DIY",
    4: "생활경제/계약",
    5: "이사/인테리어",
    6: "육아/반려동물",
    7: "환경/건강",
    8: "스마트홈/가전"
}

REVERSE_CATEGORY_MAP = {v: k for k, v in CATEGORY_MAP.items()}

CATEGORY_PERSONAS = {
    "요리/식품관리": """당신은 전문 요리사이자 영양사입니다.
    요리 레시피, 조리법, 식재료 보관법, 영양 정보 등에 대한 실용적인 조언을 제공합니다.""",

    "청소/세탁": """당신은 청소 전문가이자 세탁 전문가입니다.
    얼룩 제거, 청소 방법, 세탁 요령, 정리정돈 노하우를 친절하게 설명합니다.""",

    "생활수리/DIY": """당신은 숙련된 수리 기술자이자 DIY 전문가입니다.
    가정에서 발생하는 각종 고장 수리, DIY 프로젝트 방법을 단계별로 설명합니다.""",

    "생활경제/계약": """당신은 재무 상담사이자 법률 전문가입니다.
    생활 경제, 금융 상품, 계약서 작성, 세금 절약 등에 대한 실용적인 조언을 제공합니다.""",

    "이사/인테리어": """당신은 인테리어 디자이너이자 이사 전문가입니다.
    공간 활용, 인테리어 팁, 이사 준비사항, 정리 방법 등을 조언합니다.""",

    "육아/반려동물": """당신은 육아 전문가이자 수의사입니다.
    아이 돌봄, 교육 방법, 반려동물 관리, 건강 관리 등에 대한 따뜻한 조언을 제공합니다.""",

    "환경/건강": """당신은 환경 전문가이자 건강 관리사입니다.
    친환경 생활, 건강 관리, 운동법, 질병 예방 등에 대한 과학적인 조언을 제공합니다.""",

    "스마트홈/가전": """당신은 IT 전문가이자 가전제품 전문가입니다.
    스마트홈 구축, 가전제품 사용법, 고장 해결, 제품 추천 등을 설명합니다.""",

    "기타": """당신은 다양한 분야의 생활 전문가입니다.
    일상생활에서 발생하는 다양한 문제에 대한 실용적이고 도움이 되는 조언을 제공합니다."""
}

MIN_TEXT_LENGTH = 5
MAX_TEXT_LENGTH = 10000
DEFAULT_EMBEDDING_DIM = 384  # Use common dimension for both Korean and English models with PCA reduction

LANGUAGE_DETECTION_THRESHOLD = 0.7
MIN_KOREAN_RATIO = 0.3
MIN_ENGLISH_RATIO = 0.3

CACHE_KEY_PREFIX = "nlp_service"
CACHE_KEY_CLASSIFY = f"{CACHE_KEY_PREFIX}:classify"
CACHE_KEY_EMBED = f"{CACHE_KEY_PREFIX}:embed"
CACHE_KEY_ANSWER = f"{CACHE_KEY_PREFIX}:answer"

ERROR_INVALID_TEXT = "입력된 텍스트가 유효하지 않습니다."
ERROR_MODEL_LOAD_FAILED = "모델 로딩에 실패했습니다."
ERROR_MEMORY_EXCEEDED = "메모리 한계를 초과했습니다."
ERROR_SERVICE_UNAVAILABLE = "서비스를 일시적으로 사용할 수 없습니다."

MAX_REQUESTS_PER_MINUTE = 100
MAX_BATCH_SIZE = 32

MODEL_CONFIGS = {
    "klue/roberta-small": {
        "max_length": 512,
        "hidden_size": 768,
        "num_labels": 9
    },
    "distilbert-base-uncased": {
        "max_length": 512,
        "hidden_size": 768,
        "num_labels": 9
    },
    "jhgan/ko-sroberta-multitask": {
        "max_length": 128,
        "hidden_size": 768
    },
    "sentence-transformers/all-MiniLM-L6-v2": {
        "max_length": 256,
        "hidden_size": 384
    }
}