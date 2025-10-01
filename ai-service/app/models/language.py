import re
from typing import Dict, Tuple
from app.core.constants import Language, LANGUAGE_DETECTION_THRESHOLD


class LanguageDetector:
    KOREAN_PATTERN = re.compile(r'[가-힣ㄱ-ㅎㅏ-ㅣ]')
    ENGLISH_PATTERN = re.compile(r'[a-zA-Z]')
    NUMBER_PATTERN = re.compile(r'[0-9]')
    SPECIAL_PATTERN = re.compile(r'[^\w\s]')

    @classmethod
    def detect_language_ratio(cls, text: str) -> Dict[str, float]:
        if not text:
            return {"ko": 0.0, "en": 0.0, "other": 0.0}

        text_no_space = re.sub(r'\s+', '', text)
        total_chars = len(text_no_space)

        if total_chars == 0:
            return {"ko": 0.0, "en": 0.0, "other": 0.0}

        korean_chars = len(cls.KOREAN_PATTERN.findall(text_no_space))
        english_chars = len(cls.ENGLISH_PATTERN.findall(text_no_space))
        number_chars = len(cls.NUMBER_PATTERN.findall(text_no_space))

        meaningful_chars = korean_chars + english_chars
        if meaningful_chars == 0:
            return {"ko": 0.0, "en": 0.0, "other": 1.0}

        ko_ratio = korean_chars / meaningful_chars
        en_ratio = english_chars / meaningful_chars
        other_ratio = (total_chars - meaningful_chars) / total_chars if total_chars > 0 else 0.0

        return {
            "ko": round(ko_ratio, 3),
            "en": round(en_ratio, 3),
            "other": round(other_ratio, 3)
        }

    @classmethod
    def get_dominant_language(
        cls,
        text: str,
        threshold: float = LANGUAGE_DETECTION_THRESHOLD
    ) -> Tuple[Language, Dict[str, float]]:
        ratios = cls.detect_language_ratio(text)

        if ratios["ko"] >= threshold:
            dominant = Language.KOREAN
        elif ratios["en"] >= threshold:
            dominant = Language.ENGLISH
        else:
            dominant = Language.MIXED

        return dominant, ratios

    @classmethod
    def split_by_language(cls, text: str) -> Dict[str, str]:
        korean_parts = []
        english_parts = []
        current_korean = []
        current_english = []

        words = text.split()

        for word in words:
            ratios = cls.detect_language_ratio(word)

            if ratios["ko"] > ratios["en"]:
                if current_english:
                    english_parts.append(" ".join(current_english))
                    current_english = []
                current_korean.append(word)
            elif ratios["en"] > ratios["ko"]:
                if current_korean:
                    korean_parts.append(" ".join(current_korean))
                    current_korean = []
                current_english.append(word)
            else:
                if current_korean:
                    current_korean.append(word)
                if current_english:
                    current_english.append(word)

        if current_korean:
            korean_parts.append(" ".join(current_korean))
        if current_english:
            english_parts.append(" ".join(current_english))

        return {
            "korean": " ".join(korean_parts),
            "english": " ".join(english_parts)
        }

    @classmethod
    def normalize_for_language(cls, text: str, language: Language) -> str:
        text = text.strip()
        text = re.sub(r'\s+', ' ', text)

        if language == Language.KOREAN:
            text = cls.KOREAN_PATTERN.sub(lambda m: m.group(), text)
            text = re.sub(r'([가-힣])\s+([가-힣])', r'\1 \2', text)

        elif language == Language.ENGLISH:
            text = text.lower()
            text = re.sub(r'[^\w\s\.\!\?]', ' ', text)

        return text.strip()

    @classmethod
    def get_language_weights(cls, text: str) -> Dict[str, float]:
        dominant_lang, ratios = cls.get_dominant_language(text)

        if dominant_lang == Language.KOREAN:
            return {
                "ko": min(ratios["ko"] * 1.2, 1.0),
                "en": ratios["en"] * 0.8
            }
        elif dominant_lang == Language.ENGLISH:
            return {
                "ko": ratios["ko"] * 0.8,
                "en": min(ratios["en"] * 1.2, 1.0)
            }
        else:
            return {
                "ko": ratios["ko"],
                "en": ratios["en"]
            }

    @classmethod
    def requires_multilingual_processing(cls, text: str) -> bool:
        ratios = cls.detect_language_ratio(text)

        ko_significant = ratios["ko"] >= 0.2
        en_significant = ratios["en"] >= 0.2

        return ko_significant and en_significant


def detect_language_ratio(text: str) -> Dict[str, float]:
    return LanguageDetector.detect_language_ratio(text)


def get_dominant_language(
    text: str,
    threshold: float = LANGUAGE_DETECTION_THRESHOLD
) -> Tuple[Language, Dict[str, float]]:
    return LanguageDetector.get_dominant_language(text, threshold)


def get_language_weights(text: str) -> Dict[str, float]:
    return LanguageDetector.get_language_weights(text)