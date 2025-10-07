package com.exit.user.util;

import java.util.Random;
import java.util.UUID;

public class NicknameGenerator {
    private static final String[] ADJECTIVES = {
            "행복한", "즐거운", "귀여운", "멋진", "용감한",
            "똑똑한", "활발한", "차분한", "재미있는", "신비로운",
            "빛나는", "따뜻한", "시원한", "상쾌한", "우아한",
            "친절한", "명랑한", "평화로운", "열정적인", "사랑스러운"
    };

    private static final String[] NOUNS = {
            "호랑이", "토끼", "곰", "여우", "사자",
            "펭귄", "코알라", "팬더", "강아지", "고양이",
            "햄스터", "다람쥐", "수달", "고래", "돌고래",
            "부엉이", "독수리", "앵무새", "기린", "코끼리"
    };

    private static final Random RANDOM = new Random();

    public static String generate() {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s_%s", adjective, noun, uuid);
    }
}
