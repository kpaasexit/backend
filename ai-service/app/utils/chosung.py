from typing import List


# 한글 상수
CHOSUNG_LIST = ['ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ']
JUNGSUNG_LIST = ['ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ']
JONGSUNG_LIST = ['', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ']

# 한글 유니코드 범위
HANGUL_START = 0xAC00  # '가'
HANGUL_END = 0xD7A3    # '힣'


def is_hangul(char: str) -> bool:
    if not char or len(char) != 1:
        return False
    return HANGUL_START <= ord(char) <= HANGUL_END


def is_chosung(char: str) -> bool:
    return char in CHOSUNG_LIST


def decompose_hangul(char: str) -> tuple[str, str, str]:
    if not is_hangul(char):
        return (char, '', '')

    char_code = ord(char) - HANGUL_START
    chosung_idx = char_code // (21 * 28)
    jungsung_idx = (char_code % (21 * 28)) // 28
    jongsung_idx = char_code % 28

    return (
        CHOSUNG_LIST[chosung_idx],
        JUNGSUNG_LIST[jungsung_idx],
        JONGSUNG_LIST[jongsung_idx]
    )


def extract_chosung(text: str) -> str:
    result = []
    for char in text:
        if is_hangul(char):
            chosung, _, _ = decompose_hangul(char)
            result.append(chosung)
        else:
            # 한글이 아닌 경우 그대로 추가
            result.append(char)

    return ''.join(result)


def compose_hangul(chosung: str, jungsung: str, jongsung: str = '') -> str:
    if chosung not in CHOSUNG_LIST:
        raise ValueError(f"유효하지 않은 초성: {chosung}")
    if jungsung not in JUNGSUNG_LIST:
        raise ValueError(f"유효하지 않은 중성: {jungsung}")
    if jongsung not in JONGSUNG_LIST:
        raise ValueError(f"유효하지 않은 종성: {jongsung}")

    chosung_idx = CHOSUNG_LIST.index(chosung)
    jungsung_idx = JUNGSUNG_LIST.index(jungsung)
    jongsung_idx = JONGSUNG_LIST.index(jongsung)

    char_code = HANGUL_START + (chosung_idx * 21 * 28) + (jungsung_idx * 28) + jongsung_idx

    return chr(char_code)


def match_chosung(text: str, chosung_pattern: str) -> bool:
    extracted = extract_chosung(text)
    return extracted == chosung_pattern


def find_chosung_matches(text: str, chosung_pattern: str) -> List[int]:
    matches = []
    pattern_len = len(chosung_pattern)

    for i in range(len(text) - pattern_len + 1):
        substring = text[i:i + pattern_len]
        if match_chosung(substring, chosung_pattern):
            matches.append(i)

    return matches


def is_chosung_query(text: str) -> bool:
    if not text:
        return False

    # 초성이 하나라도 포함되어 있으면 초성 검색으로 간주
    return any(is_chosung(char) for char in text)


def normalize_chosung_query(text: str) -> str:
    # 공백 제거
    text = text.replace(' ', '')

    # 영문 소문자 변환
    text = text.lower()

    return text
