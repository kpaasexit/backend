package com.exit.gateway.controller.user.dto.request.user;

import org.springframework.web.multipart.MultipartFile;

public record UpdateAdditionalUserInfoRequestDto(
        String nickname,
        MultipartFile image,
        Boolean isProfileImageDeleted
) {
}
