package com.exit.question.domain.question;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionCategoryType {

    // 1. 요리/식품관리
    COOKING_FOOD("요리/식품관리", "cooking_food"),

    // 2. 청소/세탁
    CLEANING_LAUNDRY("청소/세탁", "cleaning_laundry"),

    // 3. 생활수리/DIY
    REPAIR_DIY("생활수리/DIY", "repair_diy"),

    // 4. 생활경제/계약
    FINANCE_CONTRACT("생활경제/계약", "finance_contract"),

    // 5. 이사/인테리어
    MOVING_INTERIOR("이사/인테리어", "moving_interior"),

    // 6. 육아/반려동물
    CHILDCARE_PETS("육아/반려동물", "childcare_pets"),

    // 7. 환경/건강
    ENVIRONMENT_HEALTH("환경/건강", "environment_health"),

    // 8. 스마트홈/가전
    SMART_HOME("스마트홈/가전", "smart_home");

    private final String displayName;
    private final String code;

    // 한글명으로 enum 찾기
    public static QuestionCategoryType fromDisplayName(String displayName) {
        for (QuestionCategoryType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown category: " + displayName);
    }

    // 코드로 enum 찾기
    public static QuestionCategoryType fromCode(String code) {
        for (QuestionCategoryType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
