package com.exit.question.domain;

public enum CommentType {
    QUESTION("QUESTION"),
    RESPONSE("RESPONSE");

    private final String value;

    CommentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
