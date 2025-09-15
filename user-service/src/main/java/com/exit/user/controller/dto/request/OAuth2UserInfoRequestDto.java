package com.exit.user.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2UserInfoRequestDto {
    private String socialId;
    private String email;
    private String name;
    private String profileImageUrl;
    private String provider;
}