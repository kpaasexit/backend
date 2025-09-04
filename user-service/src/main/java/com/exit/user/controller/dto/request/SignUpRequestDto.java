package com.exit.user.controller.dto.request;

import com.exit.user.domain.Users;
import com.exit.user.grpc.SignUpRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Builder
public record SignUpRequestDto(
        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        String name,

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        String nickname,

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
        String password,

        @Pattern(regexp = "^[0-9]{10,11}$", message = "올바른 전화번호 형식이 아닙니다")
        String phone,

        byte[] profileImage,

        String userAgent,

        String ipAddress
) {
    public static SignUpRequestDto from(SignUpRequest request) {
        return SignUpRequestDto.builder()
                .name(request.getName())
                .nickname(request.getNickname())
                .email(request.getEmail())
                .password(request.getPassword())
                .phone(request.getPhone())
                .profileImage(request.getProfileImage().toByteArray())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .build();
    }

    public static Users toUser(SignUpRequestDto request, String profileImageUrl, String encryptedPassword) {
        return Users.builder()
                .userName(request.name)
                .userNickname(request.nickname())
                .userEmail(request.email())
                .userPassword(encryptedPassword)
                .userPhoneNumber(request.phone())
                .userProfileUrl(profileImageUrl)
                .build();
    }
}
