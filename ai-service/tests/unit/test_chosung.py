import pytest
from app.utils.chosung import (
    is_hangul,
    is_chosung,
    decompose_hangul,
    extract_chosung,
    compose_hangul,
    match_chosung,
    find_chosung_matches,
    is_chosung_query,
    normalize_chosung_query
)


class TestChosungUtils:

    def test_is_hangul(self):
        assert is_hangul('가') == True
        assert is_hangul('힣') == True
        assert is_hangul('a') == False
        assert is_hangul('1') == False
        assert is_hangul('ㄱ') == False

    def test_is_chosung(self):
        assert is_chosung('ㄱ') == True
        assert is_chosung('ㅎ') == True
        assert is_chosung('가') == False
        assert is_chosung('a') == False

    def test_decompose_hangul(self):
        # 받침 있는 경우
        cho, jung, jong = decompose_hangul('한')
        assert cho == 'ㅎ'
        assert jung == 'ㅏ'
        assert jong == 'ㄴ'

        # 받침 없는 경우
        cho, jung, jong = decompose_hangul('가')
        assert cho == 'ㄱ'
        assert jung == 'ㅏ'
        assert jong == ''

        # 한글이 아닌 경우
        cho, jung, jong = decompose_hangul('a')
        assert cho == 'a'
        assert jung == ''
        assert jong == ''

    def test_extract_chosung(self):
        assert extract_chosung('침실인테리어') == 'ㅊㅅㅇㅌㄹㅇ'
        assert extract_chosung('가나다라') == 'ㄱㄴㄷㄹ'
        assert extract_chosung('한글') == 'ㅎㄱ'
        assert extract_chosung('abc') == 'abc'  # 한글 아닌 경우 그대로
        assert extract_chosung('침실 인테리어') == 'ㅊㅅ ㅇㅌㄹㅇ'  # 공백 유지

    def test_compose_hangul(self):
        # 받침 있는 경우
        assert compose_hangul('ㅎ', 'ㅏ', 'ㄴ') == '한'

        # 받침 없는 경우
        assert compose_hangul('ㄱ', 'ㅏ', '') == '가'

        # 복잡한 경우
        assert compose_hangul('ㅊ', 'ㅣ', 'ㅁ') == '침'

    def test_compose_hangul_invalid(self):
        with pytest.raises(ValueError):
            compose_hangul('a', 'ㅏ', '')  # 잘못된 초성

        with pytest.raises(ValueError):
            compose_hangul('ㄱ', 'a', '')  # 잘못된 중성

        with pytest.raises(ValueError):
            compose_hangul('ㄱ', 'ㅏ', 'a')  # 잘못된 종성

    def test_match_chosung(self):
        assert match_chosung('침실인테리어', 'ㅊㅅㅇㅌㄹㅇ') == True
        assert match_chosung('침실', 'ㅊㅅ') == True
        assert match_chosung('침실', 'ㅊㅅㅇ') == False  # 길이 다름
        assert match_chosung('가나다', 'ㄱㄴㄹ') == False  # 초성 다름

    def test_find_chosung_matches(self):
        matches = find_chosung_matches('침실인테리어 침대', 'ㅊㅅ')
        assert matches == [0, 7]

        matches = find_chosung_matches('가나다가나', 'ㄱㄴ')
        assert matches == [0, 3]

        matches = find_chosung_matches('침실', 'ㅊㅅㅇ')
        assert matches == []  # 매칭 없음

    def test_is_chosung_query(self):
        assert is_chosung_query('ㅊㅅㅇㅌㄹㅇ') == True
        assert is_chosung_query('ㅊㅅ') == True
        assert is_chosung_query('침실인테리어') == False
        assert is_chosung_query('ㅊㅅ침실') == True  # 혼합된 경우도 초성으로 판별
        assert is_chosung_query('') == False
        assert is_chosung_query('abc') == False

    def test_normalize_chosung_query(self):
        assert normalize_chosung_query('ㅊ ㅅ') == 'ㅊㅅ'  # 공백 제거
        assert normalize_chosung_query('ABC') == 'abc'  # 소문자 변환
        assert normalize_chosung_query('ㅊㅅ ABC') == 'ㅊㅅabc'  # 복합
        assert normalize_chosung_query('  ㅊㅅ  ') == 'ㅊㅅ'  # 앞뒤 공백 제거
