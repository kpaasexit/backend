"""Unit tests for text utility functions."""

import pytest
from typing import List

from app.utils.text import (
    normalize_text,
    truncate_text,
    split_into_sentences,
    extract_keywords_simple,
    clean_html,
    remove_urls,
    remove_emails,
    mask_personal_info,
    calculate_text_similarity,
    generate_text_hash,
    split_into_chunks,
    count_tokens_approximate,
    validate_question_text
)


@pytest.mark.unit
class TestNormalizeText:
    """Test normalize_text function."""

    def test_normalize_basic_text(self):
        """Test basic text normalization."""
        text = "  Hello   World  "
        result = normalize_text(text)
        assert result == "Hello World"

    def test_normalize_with_lowercase(self):
        """Test normalization with lowercase option."""
        text = "Hello World"
        result = normalize_text(text, lowercase=True)
        assert result == "hello world"

    def test_normalize_unicode_text(self):
        """Test normalization of Unicode text."""
        text = "안녕하세요　　세계"  # Contains wide spaces
        result = normalize_text(text)
        assert result == "안녕하세요 세계"

    def test_normalize_control_characters(self):
        """Test removal of control characters."""
        text = "Hello\x00World\x01"
        result = normalize_text(text)
        assert result == "HelloWorld"

    def test_normalize_empty_text(self):
        """Test normalization of empty text."""
        assert normalize_text("") == ""
        assert normalize_text("   ") == ""

    def test_normalize_special_characters(self):
        """Test normalization preserves valid special characters."""
        text = "Hello, World! 안녕하세요?"
        result = normalize_text(text)
        assert result == "Hello, World! 안녕하세요?"


@pytest.mark.unit
class TestTruncateText:
    """Test truncate_text function."""

    def test_truncate_short_text(self):
        """Test truncation of text shorter than limit."""
        text = "Short"
        result = truncate_text(text, 10)
        assert result == "Short"

    def test_truncate_long_text_with_ellipsis(self):
        """Test truncation with ellipsis."""
        text = "This is a very long text that should be truncated"
        result = truncate_text(text, 20, add_ellipsis=True)
        assert result == "This is a very lo..."
        assert len(result) == 20

    def test_truncate_long_text_without_ellipsis(self):
        """Test truncation without ellipsis."""
        text = "This is a very long text that should be truncated"
        result = truncate_text(text, 20, add_ellipsis=False)
        assert result == "This is a very long "
        assert len(result) == 20

    def test_truncate_exact_length(self):
        """Test truncation at exact length."""
        text = "Exactly twenty chars"
        result = truncate_text(text, 20)
        assert result == text
        assert len(result) == 20

    def test_truncate_very_short_limit(self):
        """Test truncation with very short limit."""
        text = "Hello"
        result = truncate_text(text, 3, add_ellipsis=True)
        assert result == "..."

    def test_truncate_korean_text(self):
        """Test truncation of Korean text."""
        text = "안녕하세요 반갑습니다 좋은 하루 되세요"
        result = truncate_text(text, 10, add_ellipsis=True)
        assert len(result) == 10
        assert result.endswith("...")


@pytest.mark.unit
class TestSplitIntoSentences:
    """Test split_into_sentences function."""

    def test_split_english_sentences(self):
        """Test splitting English sentences."""
        text = "Hello world. How are you? I am fine!"
        sentences = split_into_sentences(text, "en")
        assert len(sentences) == 3
        assert sentences[0] == "Hello world."
        assert sentences[1] == "How are you?"
        assert sentences[2] == "I am fine!"

    def test_split_korean_sentences(self):
        """Test splitting Korean sentences."""
        text = "안녕하세요. 어떻게 지내세요? 저는 잘 지냅니다！"
        sentences = split_into_sentences(text, "ko")
        assert len(sentences) == 3
        assert sentences[0] == "안녕하세요."
        assert sentences[1] == "어떻게 지내세요?"
        assert sentences[2] == "저는 잘 지냅니다！"

    def test_split_mixed_sentences(self):
        """Test splitting mixed language sentences."""
        text = "Hello 안녕하세요. How are you 어떻게 지내세요?"
        sentences = split_into_sentences(text, "mixed")
        assert len(sentences) == 2

    def test_split_no_sentences(self):
        """Test splitting text without sentence endings."""
        text = "This is just a phrase without endings"
        sentences = split_into_sentences(text)
        assert len(sentences) == 1
        assert sentences[0] == text

    def test_split_empty_text(self):
        """Test splitting empty text."""
        sentences = split_into_sentences("")
        assert sentences == []

    def test_split_multiple_punctuation(self):
        """Test splitting with multiple punctuation marks."""
        text = "Really?! Yes... Maybe."
        sentences = split_into_sentences(text)
        assert len(sentences) == 3


@pytest.mark.unit
class TestExtractKeywordsSimple:
    """Test extract_keywords_simple function."""

    def test_extract_keywords_english(self):
        """Test keyword extraction from English text."""
        text = "Machine learning and artificial intelligence are transforming technology"
        keywords = extract_keywords_simple(text, 3)
        assert len(keywords) <= 3
        assert "machine" in keywords or "learning" in keywords

    def test_extract_keywords_korean(self):
        """Test keyword extraction from Korean text."""
        text = "인공지능과 머신러닝이 기술을 변화시키고 있습니다"
        keywords = extract_keywords_simple(text, 3)
        assert len(keywords) <= 3
        assert "인공지능과" in keywords or "머신러닝이" in keywords

    def test_extract_keywords_with_stopwords(self):
        """Test keyword extraction filters stop words."""
        text = "The quick brown fox jumps over the lazy dog"
        keywords = extract_keywords_simple(text)
        assert "the" not in keywords
        assert "over" not in keywords

    def test_extract_keywords_empty_text(self):
        """Test keyword extraction from empty text."""
        keywords = extract_keywords_simple("")
        assert keywords == []

    def test_extract_keywords_punctuation(self):
        """Test keyword extraction removes punctuation."""
        text = "Hello, world! This is a test."
        keywords = extract_keywords_simple(text)
        for keyword in keywords:
            assert "," not in keyword
            assert "!" not in keyword
            assert "." not in keyword

    def test_extract_keywords_frequency_based(self):
        """Test keyword extraction is frequency-based."""
        text = "apple banana apple cherry apple banana"
        keywords = extract_keywords_simple(text, 2)
        assert "apple" in keywords  # Most frequent should be included
        assert len(keywords) <= 2


@pytest.mark.unit
class TestCleanHtml:
    """Test clean_html function."""

    def test_clean_basic_html(self):
        """Test cleaning basic HTML tags."""
        html = "<p>Hello <b>world</b></p>"
        result = clean_html(html)
        assert result == "Hello world"

    def test_clean_complex_html(self):
        """Test cleaning complex HTML."""
        html = "<div class='container'><h1>Title</h1><p>Content with <a href='#'>link</a></p></div>"
        result = clean_html(html)
        assert result == "TitleContent with link"

    def test_clean_html_entities(self):
        """Test cleaning HTML entities."""
        html = "&amp; &lt; &gt; &quot; &#39; &nbsp;"
        result = clean_html(html)
        assert result == "& < > \" '  "

    def test_clean_no_html(self):
        """Test cleaning text without HTML."""
        text = "Plain text without tags"
        result = clean_html(text)
        assert result == text

    def test_clean_empty_html(self):
        """Test cleaning empty HTML."""
        assert clean_html("") == ""
        assert clean_html("<p></p>") == ""

    def test_clean_nested_html(self):
        """Test cleaning nested HTML tags."""
        html = "<div><p><span><b>Deep</b> nesting</span></p></div>"
        result = clean_html(html)
        assert result == "Deep nesting"


@pytest.mark.unit
class TestRemoveUrls:
    """Test remove_urls function."""

    def test_remove_http_urls(self):
        """Test removing HTTP URLs."""
        text = "Visit http://example.com for more info"
        result = remove_urls(text)
        assert result == "Visit  for more info"

    def test_remove_https_urls(self):
        """Test removing HTTPS URLs."""
        text = "Check https://secure.example.com/page?param=value"
        result = remove_urls(text)
        assert result == "Check"

    def test_remove_www_urls(self):
        """Test removing www URLs."""
        text = "Go to www.example.com for details"
        result = remove_urls(text)
        assert result == "Go to  for details"

    def test_remove_multiple_urls(self):
        """Test removing multiple URLs."""
        text = "Visit http://first.com and https://second.com for info"
        result = remove_urls(text)
        assert result == "Visit  and  for info"

    def test_remove_urls_with_replacement(self):
        """Test removing URLs with replacement text."""
        text = "Visit http://example.com for more"
        result = remove_urls(text, replacement="[LINK]")
        assert result == "Visit [LINK] for more"

    def test_no_urls_to_remove(self):
        """Test text without URLs."""
        text = "This text has no URLs"
        result = remove_urls(text)
        assert result == text


@pytest.mark.unit
class TestRemoveEmails:
    """Test remove_emails function."""

    def test_remove_basic_email(self):
        """Test removing basic email address."""
        text = "Contact me at user@example.com"
        result = remove_emails(text)
        assert result == "Contact me at"

    def test_remove_multiple_emails(self):
        """Test removing multiple email addresses."""
        text = "Email admin@site.com or support@help.org"
        result = remove_emails(text)
        assert result == "Email  or"

    def test_remove_complex_emails(self):
        """Test removing complex email addresses."""
        text = "Contact first.last+tag@subdomain.example.co.uk"
        result = remove_emails(text)
        assert result == "Contact"

    def test_remove_emails_with_replacement(self):
        """Test removing emails with replacement text."""
        text = "Email me at user@example.com"
        result = remove_emails(text, replacement="[EMAIL]")
        assert result == "Email me at [EMAIL]"

    def test_no_emails_to_remove(self):
        """Test text without emails."""
        text = "This has no email addresses"
        result = remove_emails(text)
        assert result == text

    def test_invalid_email_formats(self):
        """Test that invalid email formats are not removed."""
        text = "Invalid: @example.com user@ incomplete.email"
        result = remove_emails(text)
        assert result == text  # Should remain unchanged


@pytest.mark.unit
class TestMaskPersonalInfo:
    """Test mask_personal_info function."""

    def test_mask_phone_numbers(self):
        """Test masking phone numbers."""
        text = "Call me at 010-1234-5678"
        result = mask_personal_info(text)
        assert "[PHONE]" in result
        assert "010-1234-5678" not in result

    def test_mask_various_phone_formats(self):
        """Test masking various phone number formats."""
        texts = [
            "010-1234-5678",
            "010.1234.5678",
            "010 1234 5678",
            "02-123-4567",
        ]
        for text in texts:
            result = mask_personal_info(text)
            assert "[PHONE]" in result

    def test_mask_email_addresses(self):
        """Test masking email addresses."""
        text = "Email me at user@example.com"
        result = mask_personal_info(text)
        assert "[EMAIL]" in result
        assert "user@example.com" not in result

    def test_mask_korean_id_numbers(self):
        """Test masking Korean ID numbers."""
        text = "My ID is 123456-1234567"
        result = mask_personal_info(text)
        assert "[ID]" in result
        assert "123456-1234567" not in result

    def test_mask_multiple_info_types(self):
        """Test masking multiple types of personal info."""
        text = "Contact: 010-1234-5678, email@test.com, ID: 890123-2345678"
        result = mask_personal_info(text)
        assert "[PHONE]" in result
        assert "[EMAIL]" in result
        assert "[ID]" in result

    def test_no_personal_info(self):
        """Test text without personal information."""
        text = "This is just regular text content"
        result = mask_personal_info(text)
        assert result == text


@pytest.mark.unit
class TestCalculateTextSimilarity:
    """Test calculate_text_similarity function."""

    def test_identical_texts(self):
        """Test similarity of identical texts."""
        text = "Hello world"
        similarity = calculate_text_similarity(text, text)
        assert similarity == 1.0

    def test_completely_different_texts(self):
        """Test similarity of completely different texts."""
        text1 = "abc"
        text2 = "xyz"
        similarity = calculate_text_similarity(text1, text2)
        assert similarity == 0.0

    def test_partially_similar_texts(self):
        """Test similarity of partially similar texts."""
        text1 = "hello world"
        text2 = "hello python"
        similarity = calculate_text_similarity(text1, text2)
        assert 0.0 < similarity < 1.0

    def test_case_insensitive(self):
        """Test that similarity is case-insensitive."""
        text1 = "Hello World"
        text2 = "hello world"
        similarity = calculate_text_similarity(text1, text2)
        assert similarity == 1.0

    def test_empty_texts(self):
        """Test similarity with empty texts."""
        assert calculate_text_similarity("", "") == 0.0
        assert calculate_text_similarity("hello", "") == 0.0
        assert calculate_text_similarity("", "world") == 0.0

    def test_korean_text_similarity(self):
        """Test similarity with Korean text."""
        text1 = "안녕하세요"
        text2 = "안녕하세요 반갑습니다"
        similarity = calculate_text_similarity(text1, text2)
        assert 0.0 < similarity < 1.0


@pytest.mark.unit
class TestGenerateTextHash:
    """Test generate_text_hash function."""

    def test_consistent_hash(self):
        """Test that same text produces same hash."""
        text = "Hello world"
        hash1 = generate_text_hash(text)
        hash2 = generate_text_hash(text)
        assert hash1 == hash2

    def test_different_texts_different_hashes(self):
        """Test that different texts produce different hashes."""
        text1 = "Hello world"
        text2 = "Goodbye world"
        hash1 = generate_text_hash(text1)
        hash2 = generate_text_hash(text2)
        assert hash1 != hash2

    def test_hash_format(self):
        """Test hash format."""
        text = "Test text"
        hash_value = generate_text_hash(text)
        assert isinstance(hash_value, str)
        assert len(hash_value) == 32  # MD5 hash length
        assert all(c in '0123456789abcdef' for c in hash_value)

    def test_unicode_text_hash(self):
        """Test hashing of Unicode text."""
        text = "안녕하세요 🌍"
        hash_value = generate_text_hash(text)
        assert isinstance(hash_value, str)
        assert len(hash_value) == 32

    def test_empty_text_hash(self):
        """Test hashing empty text."""
        hash_value = generate_text_hash("")
        assert isinstance(hash_value, str)
        assert len(hash_value) == 32


@pytest.mark.unit
class TestSplitIntoChunks:
    """Test split_into_chunks function."""

    def test_split_basic_chunks(self):
        """Test basic chunk splitting."""
        text = "a" * 1000
        chunks = split_into_chunks(text, chunk_size=100, overlap=10)
        assert len(chunks) > 1
        assert len(chunks[0]) == 100

    def test_split_with_overlap(self):
        """Test chunk splitting with overlap."""
        text = "abcdefghijklmnopqrstuvwxyz"
        chunks = split_into_chunks(text, chunk_size=10, overlap=2)
        assert len(chunks) > 1
        # Check overlap
        if len(chunks) > 1:
            assert chunks[0][-2:] == chunks[1][:2]

    def test_split_short_text(self):
        """Test splitting text shorter than chunk size."""
        text = "short"
        chunks = split_into_chunks(text, chunk_size=100)
        assert len(chunks) == 1
        assert chunks[0] == text

    def test_split_empty_text(self):
        """Test splitting empty text."""
        chunks = split_into_chunks("", chunk_size=100)
        assert chunks == []

    def test_split_invalid_chunk_size(self):
        """Test splitting with invalid chunk size."""
        text = "test text"
        chunks = split_into_chunks(text, chunk_size=0)
        assert chunks == []

    def test_split_large_overlap(self):
        """Test splitting with overlap larger than chunk size."""
        text = "a" * 100
        chunks = split_into_chunks(text, chunk_size=10, overlap=20)
        # Should still work, but overlap will be limited
        assert len(chunks) >= 1


@pytest.mark.unit
class TestCountTokensApproximate:
    """Test count_tokens_approximate function."""

    def test_count_english_tokens(self):
        """Test approximate token counting for English text."""
        text = "Hello world this is a test"
        token_count = count_tokens_approximate(text)
        assert token_count > 0
        assert isinstance(token_count, int)

    def test_count_korean_tokens(self):
        """Test approximate token counting for Korean text."""
        text = "안녕하세요 이것은 테스트입니다"
        token_count = count_tokens_approximate(text)
        assert token_count > 0

    def test_count_mixed_tokens(self):
        """Test approximate token counting for mixed text."""
        text = "Hello 안녕하세요 world 세계"
        token_count = count_tokens_approximate(text)
        assert token_count > 0

    def test_count_empty_text_tokens(self):
        """Test token counting for empty text."""
        token_count = count_tokens_approximate("")
        assert token_count == 0

    def test_count_special_characters(self):
        """Test token counting with special characters."""
        text = "Hello!@#$%^&*()World"
        token_count = count_tokens_approximate(text)
        assert token_count > 0

    def test_count_numbers(self):
        """Test token counting with numbers."""
        text = "123 456 789"
        token_count = count_tokens_approximate(text)
        assert token_count > 0


@pytest.mark.unit
class TestValidateQuestionText:
    """Test validate_question_text function."""

    def test_valid_title_only(self):
        """Test validation with valid title only."""
        is_valid, error = validate_question_text("이것은 유효한 질문 제목입니다")
        assert is_valid is True
        assert error is None

    def test_valid_title_and_content(self):
        """Test validation with valid title and content."""
        title = "유효한 질문 제목입니다"
        content = "이것은 질문의 내용입니다. 더 자세한 설명을 포함합니다."
        is_valid, error = validate_question_text(title, content)
        assert is_valid is True
        assert error is None

    def test_empty_title(self):
        """Test validation with empty title."""
        is_valid, error = validate_question_text("")
        assert is_valid is False
        assert "제목이 필요합니다" in error

    def test_whitespace_only_title(self):
        """Test validation with whitespace-only title."""
        is_valid, error = validate_question_text("   ")
        assert is_valid is False
        assert "제목이 필요합니다" in error

    def test_title_too_short(self):
        """Test validation with too short title."""
        is_valid, error = validate_question_text("짧음")
        assert is_valid is False
        assert "너무 짧습니다" in error

    def test_title_too_long(self):
        """Test validation with too long title."""
        long_title = "a" * 501
        is_valid, error = validate_question_text(long_title)
        assert is_valid is False
        assert "너무 깁니다" in error

    def test_content_too_long(self):
        """Test validation with too long content."""
        title = "유효한 제목입니다"
        long_content = "a" * 10001
        is_valid, error = validate_question_text(title, long_content)
        assert is_valid is False
        assert "너무 깁니다" in error

    def test_spam_patterns_in_title(self):
        """Test validation detects spam patterns in title."""
        # Repeated characters
        spam_title = "aaaaaaaaaaaaa 질문입니다"
        is_valid, error = validate_question_text(spam_title)
        assert is_valid is False
        assert "스팸으로 의심" in error

        # Multiple URLs
        spam_title = "http://spam1.com http://spam2.com http://spam3.com"
        is_valid, error = validate_question_text(spam_title)
        assert is_valid is False
        assert "스팸으로 의심" in error

        # Long numbers
        spam_title = "12345678901234567890 질문"
        is_valid, error = validate_question_text(spam_title)
        assert is_valid is False
        assert "스팸으로 의심" in error

    def test_spam_patterns_in_content(self):
        """Test validation detects spam patterns in content."""
        title = "정상적인 질문 제목입니다"
        spam_content = "aaaaaaaaaaaaa" * 100
        is_valid, error = validate_question_text(title, spam_content)
        assert is_valid is False
        assert "스팸으로 의심" in error

    def test_edge_case_lengths(self):
        """Test validation at edge case lengths."""
        # Minimum valid title length
        min_title = "12345"
        is_valid, error = validate_question_text(min_title)
        assert is_valid is True
        assert error is None

        # Maximum valid title length
        max_title = "a" * 500
        is_valid, error = validate_question_text(max_title)
        assert is_valid is True
        assert error is None

        # Maximum valid content length
        title = "유효한 제목입니다"
        max_content = "a" * 10000
        is_valid, error = validate_question_text(title, max_content)
        assert is_valid is True
        assert error is None