package com.exit.gateway.controller.user;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserDetailRequest;
import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.global.resolver.DeviceIdArgumentResolver;
import com.exit.gateway.global.resolver.UserIdArgumentResolver;
import com.exit.gateway.service.user.UserGrpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureRestDocs
@Import({
        RestDocsConfiguration.class,
        JwtTokenProvider.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserGrpcClient userGrpcClient;

    @MockBean
    private DeviceIdArgumentResolver deviceIdArgumentResolver;

    @MockBean
    private UserIdArgumentResolver userIdArgumentResolver;

    private String validAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        UserDetailRequest userDetail = new UserDetailRequest(1L, "test-device-id");
        validAccessToken = jwtTokenProvider.generateAccessToken(userDetail);

        // Mock DeviceIdArgumentResolver
        given(deviceIdArgumentResolver.supportsParameter(any())).willAnswer(invocation -> {
            org.springframework.core.MethodParameter param = invocation.getArgument(0);
            return param.hasParameterAnnotation(com.exit.gateway.global.annotation.DeviceId.class);
        });
        given(deviceIdArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn("test-device-id");

        // Mock UserIdArgumentResolver
        given(userIdArgumentResolver.supportsParameter(any())).willAnswer(invocation -> {
            org.springframework.core.MethodParameter param = invocation.getArgument(0);
            return param.hasParameterAnnotation(com.exit.gateway.global.annotation.LoginUser.class);
        });
        given(userIdArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(1L);
    }

    @Test
    @DisplayName("사용자 추가 정보 수정 API")
    void updateAdditionalUserInfo() throws Exception {
        // given
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "profile.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        UpdateAdditionalUserInfoResponse grpcResponse = UpdateAdditionalUserInfoResponse.newBuilder()
                .setUserId(1L)
                .setUserName("테스트사용자")
                .setUserProfile("https://example.com/profile.jpg")
                .build();

        given(userGrpcClient.updateAdditionalUserInfo(anyLong(), any())).willReturn(grpcResponse);

        // when & then
        mockMvc.perform(multipart("/api/user/additional-info")
                        .file(imageFile)
                        .param("nickname", "테스트사용자")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userId").value(1L))
                .andExpect(jsonPath("$.result.nickName").value("테스트사용자"))
                .andExpect(jsonPath("$.result.profile").value("https://example.com/profile.jpg"))
                .andDo(document("user/update-additional-info",
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        requestParts(
                                partWithName("image").description("프로필 이미지 (optional)").optional(),
                                partWithName("nickname").description("닉네임 (optional)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.userId").description("사용자 ID"),
                                fieldWithPath("result.nickName").description("수정된 닉네임"),
                                fieldWithPath("result.profile").description("수정된 프로필 이미지 URL")
                        )
                ));
    }
}
