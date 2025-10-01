"""Text processing utilities."""

import re
import unicodedata
from typing import List, Optional, Tuple
import hashlib


def normalize_text(text: str, lowercase: bool = False) -> str:
    """
    Normalize text for processing.

    Args:
        text: Input text
        lowercase: Whether to convert to lowercase

    Returns:
        Normalized text
    """
    # Remove control characters
    text = ''.join(char for char in text if not unicodedata.category(char).startswith('C'))

    # Normalize whitespace
    text = ' '.join(text.split())

    # Normalize unicode
    text = unicodedata.normalize('NFKC', text)

    # Optional lowercase
    if lowercase:
        text = text.lower()

    return text.strip()


def truncate_text(text: str, max_length: int, add_ellipsis: bool = True) -> str:
    """
    Truncate text to maximum length.

    Args:
        text: Input text
        max_length: Maximum length
        add_ellipsis: Whether to add ellipsis

    Returns:
        Truncated text
    """
    if len(text) <= max_length:
        return text

    if add_ellipsis and max_length > 3:
        return text[:max_length - 3] + "..."
    else:
        return text[:max_length]


def split_into_sentences(text: str, language: str = "mixed") -> List[str]:
    """
    Split text into sentences.

    Args:
        text: Input text
        language: Language hint (ko, en, mixed)

    Returns:
        List of sentences
    """
    # Korean sentence endings
    korean_endings = r'[.!?。！？]+'

    # English sentence endings
    english_endings = r'[.!?]+'

    # Mixed pattern
    if language == "ko" or language == "mixed":
        pattern = korean_endings
    else:
        pattern = english_endings

    # Split by pattern
    sentences = re.split(f'({pattern})', text)

    # Combine sentences with their endings
    result = []
    for i in range(0, len(sentences), 2):
        if i + 1 < len(sentences):
            sentence = sentences[i] + sentences[i + 1]
        else:
            sentence = sentences[i]

        sentence = sentence.strip()
        if sentence:
            result.append(sentence)

    return result


def extract_keywords_simple(text: str, num_keywords: int = 5) -> List[str]:
    """
    Extract keywords using simple frequency-based method.

    Args:
        text: Input text
        num_keywords: Number of keywords to extract

    Returns:
        List of keywords
    """
    # Remove punctuation
    clean_text = re.sub(r'[^\w\s가-힣]', ' ', text)

    # Split into words
    words = clean_text.split()

    # Filter stop words (simplified)
    stop_words = {
        '은', '는', '이', '가', '을', '를', '에', '에서', '으로', '와', '과',
        'the', 'a', 'an', 'is', 'are', 'was', 'were', 'in', 'on', 'at', 'to'
    }

    filtered_words = [
        word for word in words
        if len(word) > 1 and word.lower() not in stop_words
    ]

    # Count frequency
    word_freq = {}
    for word in filtered_words:
        word_lower = word.lower()
        word_freq[word_lower] = word_freq.get(word_lower, 0) + 1

    # Sort by frequency
    sorted_words = sorted(word_freq.items(), key=lambda x: x[1], reverse=True)

    # Return top keywords
    return [word for word, _ in sorted_words[:num_keywords]]


def clean_html(text: str) -> str:
    """
    Remove HTML tags from text.

    Args:
        text: Input text with potential HTML

    Returns:
        Cleaned text
    """
    # Remove HTML tags
    clean = re.sub(r'<[^>]+>', '', text)

    # Decode HTML entities
    html_entities = {
        '&amp;': '&',
        '&lt;': '<',
        '&gt;': '>',
        '&quot;': '"',
        '&#39;': "'",
        '&nbsp;': ' '
    }

    for entity, char in html_entities.items():
        clean = clean.replace(entity, char)

    return clean


def remove_urls(text: str, replacement: str = "") -> str:
    """
    Remove URLs from text.

    Args:
        text: Input text
        replacement: Replacement string for URLs

    Returns:
        Text with URLs removed
    """
    # URL pattern
    url_pattern = r'https?://\S+|www\.\S+'

    return re.sub(url_pattern, replacement, text).strip()


def remove_emails(text: str, replacement: str = "") -> str:
    """
    Remove email addresses from text.

    Args:
        text: Input text
        replacement: Replacement string for emails

    Returns:
        Text with emails removed
    """
    # Email pattern
    email_pattern = r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b'

    return re.sub(email_pattern, replacement, text).strip()


def mask_personal_info(text: str) -> str:
    """
    Mask personal information in text.

    Args:
        text: Input text

    Returns:
        Text with masked personal information
    """
    # Phone number patterns (Korean)
    phone_pattern = r'\b\d{2,4}[-.\s]?\d{3,4}[-.\s]?\d{4}\b'
    text = re.sub(phone_pattern, '[PHONE]', text)

    # Email addresses
    text = remove_emails(text, '[EMAIL]')

    # Korean ID patterns (simplified)
    id_pattern = r'\b\d{6}[-\s]?[1-4]\d{6}\b'
    text = re.sub(id_pattern, '[ID]', text)

    return text


def calculate_text_similarity(text1: str, text2: str) -> float:
    """
    Calculate simple text similarity using character overlap.

    Args:
        text1: First text
        text2: Second text

    Returns:
        Similarity score (0-1)
    """
    if not text1 or not text2:
        return 0.0

    # Convert to sets of characters
    set1 = set(text1.lower())
    set2 = set(text2.lower())

    # Calculate Jaccard similarity
    intersection = len(set1.intersection(set2))
    union = len(set1.union(set2))

    if union == 0:
        return 0.0

    return intersection / union


def generate_text_hash(text: str) -> str:
    """
    Generate a hash for text.

    Args:
        text: Input text

    Returns:
        Hash string
    """
    return hashlib.md5(text.encode('utf-8')).hexdigest()


def split_into_chunks(
    text: str,
    chunk_size: int = 1000,
    overlap: int = 100
) -> List[str]:
    """
    Split text into overlapping chunks.

    Args:
        text: Input text
        chunk_size: Size of each chunk
        overlap: Overlap between chunks

    Returns:
        List of text chunks
    """
    if not text or chunk_size <= 0:
        return []

    chunks = []
    start = 0

    while start < len(text):
        end = start + chunk_size
        chunk = text[start:end]
        chunks.append(chunk)

        # Move start position
        start += chunk_size - overlap

        # Break if we've reached the end
        if end >= len(text):
            break

    return chunks


def count_tokens_approximate(text: str) -> int:
    """
    Approximate token count for text.

    Args:
        text: Input text

    Returns:
        Approximate token count
    """
    # Rough approximation:
    # - English: ~4 characters per token
    # - Korean: ~2 characters per token

    # Count characters
    korean_chars = len(re.findall(r'[가-힣]', text))
    english_chars = len(re.findall(r'[a-zA-Z]', text))
    other_chars = len(text) - korean_chars - english_chars

    # Approximate tokens
    korean_tokens = korean_chars / 2
    english_tokens = english_chars / 4
    other_tokens = other_chars / 3

    return int(korean_tokens + english_tokens + other_tokens)


def validate_question_text(
    title: str,
    content: Optional[str] = None
) -> Tuple[bool, Optional[str]]:
    """
    Validate question text.

    Args:
        title: Question title
        content: Optional question content

    Returns:
        Tuple of (is_valid, error_message)
    """
    # Check title
    if not title or not title.strip():
        return False, "제목이 필요합니다"

    if len(title.strip()) < 5:
        return False, "제목이 너무 짧습니다 (최소 5자)"

    if len(title) > 500:
        return False, "제목이 너무 깁니다 (최대 500자)"

    # Check for spam patterns
    spam_patterns = [
        r'(.)\1{10,}',  # Repeated characters
        r'https?://\S+.*https?://\S+.*https?://\S+',  # Multiple URLs
        r'[0-9]{10,}',  # Long numbers
    ]

    for pattern in spam_patterns:
        if re.search(pattern, title):
            return False, "스팸으로 의심되는 내용이 포함되어 있습니다"

    # Check content if provided
    if content:
        if len(content) > 10000:
            return False, "내용이 너무 깁니다 (최대 10000자)"

        for pattern in spam_patterns:
            if re.search(pattern, content):
                return False, "스팸으로 의심되는 내용이 포함되어 있습니다"

    return True, None