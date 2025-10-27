package com.exit.gateway.controller.user;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserDetailRequest;
import com.exit.common.grpc.RefreshTokenResponse;
import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.global.resolver.DeviceIdArgumentResolver;
import com.exit.gateway.global.resolver.UserIdArgumentResolver;
import com.exit.gateway.service.user.AuthGrpcClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.operation.RequestCookie;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureRestDocs
@Import({
        RestDocsConfiguration.class,
        JwtTokenProvider.class
})
class AuthControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthGrpcClient authGrpcClient;

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
    @DisplayName("토큰 갱신 API")
    void refreshToken() throws Exception {
        // given
        UserDetailRequest userDetail = new UserDetailRequest(1L, "test-device-id");
        Cookie cookie = new Cookie("refreshToken", validAccessToken);

        RefreshTokenResponse grpcResponse = RefreshTokenResponse.newBuilder()
                .setAccessToken("new-access-token")
                .build();

        given(authGrpcClient.refreshToken(anyString(), anyString())).willReturn(grpcResponse);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("new-access-token"))
                .andDo(document("auth/refresh",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.accessToken").description("새로운 액세스 토큰")
                        )
                ));
    }

    @Test
    @DisplayName("로그아웃 API")
    void logout() throws Exception {
        // given
        com.exit.common.grpc.LogoutResponse grpcResponse = com.exit.common.grpc.LogoutResponse.newBuilder().build();
        given(authGrpcClient.logout(any(Long.class), anyString())).willReturn(grpcResponse);

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .header("X-Device-Id", "test-device-id")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk())
                .andDo(document("auth/logout",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("X-Device-Id").description("디바이스 ID"),
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터 (로그아웃 완료 메시지)")
                        )
                ));
    }

    @Test
    @DisplayName("회원 탈퇴 API")
    void withdraw() throws Exception {
        // given
        willDoNothing().given(authGrpcClient).withdraw(anyLong());

        // when & then
        mockMvc.perform(post("/api/auth/withdraw")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk())
                .andDo(document("auth/withdraw",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터 (회원탈퇴 완료 메시지)")
                        )
                ));
    }
}
