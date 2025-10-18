package com.exit.common.auth.jwt;

import lombok.Getter;

@Getter
public class Member {
    Long userId;
    String deviceId;

    public Member(Long userId, String deviceId) {
        this.userId = userId;
        this.deviceId = deviceId;
    }

}
