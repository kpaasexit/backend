package com.exit.gateway.controller.user;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserDetailRequest;
import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.controller.user.dto.request.auth.DeviceFcmTokenRequestDto;
import com.exit.gateway.controller.user.dto.response.user.CheckNicknameDuplicateResponseDto;
import com.exit.gateway.controller.user.dto.response.user.GetUserInfoResponseDto;
import com.exit.gateway.global.resolver.DeviceIdArgumentResolver;
import com.exit.gateway.global.resolver.UserIdArgumentResolver;
import com.exit.gateway.service.user.UserGrpcClient;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
class UserControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

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

        UpdateAdditionalUserInfoResponse grpcResponse =
                UpdateAdditionalUserInfoResponse.newBuilder()
                        .setUserId(1L)
                        .setUserName("테스트사용자")
                        .setUserProfile("https://example.com/profile.jpg")
                        .build();

        given(userGrpcClient.updateAdditionalUserInfo(anyLong(), any()))
                .willReturn(grpcResponse);

        // when & then
        mockMvc.perform(multipart("/api/user/additional-info")
                        .file(imageFile)
                        .param("nickname", "테스트사용자")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userId").value(1L))
                .andExpect(jsonPath("$.result.nickName").value("테스트사용자"))
                .andExpect(jsonPath("$.result.profile").value("https://example.com/profile.jpg"))
                .andDo(document("user/update-additional-info",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization")
                                        .description("액세스 토큰 (Bearer {token})")
                        ),
                        requestParts(
                                partWithName("image")
                                        .description("프로필 이미지 파일 (JPG, PNG)")
                                        .optional()
                        ),
                        formParameters(
                                parameterWithName("nickname")
                                        .description("사용자 닉네임 (2-20자)")
                                        .optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.userId").description("사용자 ID"),
                                fieldWithPath("result.nickName").description("수정된 닉네임"),
                                fieldWithPath("result.profile").description("프로필 이미지 URL")
                        )
                ));
    }

    @Test
    @DisplayName("사용자 정보 조회 API")
    void getUserInfo() throws Exception {
        // given
        GetUserInfoResponseDto responseDto = GetUserInfoResponseDto.builder()
                .nickname("테스트사용자")
                .profileUrl("https://example.com/profile.jpg")
                .build();

        given(userGrpcClient.getUserInfo(anyLong()))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.nickname").value("테스트사용자"))
                .andExpect(jsonPath("$.result.profileUrl").value("https://example.com/profile.jpg"))
                .andDo(document("user/get-user-info",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization")
                                        .description("액세스 토큰 (Bearer {token})")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.nickname").description("사용자 닉네임"),
                                fieldWithPath("result.profileUrl").description("프로필 이미지 URL")
                        )
                ));
    }

    @Test
    @DisplayName("닉네임 중복 체크 API - 사용 가능")
    void checkNicknameDuplicate_Available() throws Exception {
        // given
        CheckNicknameDuplicateResponseDto responseDto = CheckNicknameDuplicateResponseDto.builder()
                .isAvailable(true)
                .build();

        given(userGrpcClient.checkNicknameDuplicate(anyString()))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/user/check-nickname")
                        .param("nickname", "사용가능닉네임")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isAvailable").value(true))
                .andDo(document("user/check-nickname-available",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("nickname")
                                        .description("중복 체크할 닉네임")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.isAvailable").description("사용 가능 여부 (true: 사용 가능, false: 중복)")
                        )
                ));
    }

    @Test
    @DisplayName("닉네임 중복 체크 API - 중복됨")
    void checkNicknameDuplicate_Duplicate() throws Exception {
        // given
        CheckNicknameDuplicateResponseDto responseDto = CheckNicknameDuplicateResponseDto.builder()
                .isAvailable(false)
                .build();

        given(userGrpcClient.checkNicknameDuplicate(anyString()))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/user/check-nickname")
                        .param("nickname", "중복된닉네임")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isAvailable").value(false))
                .andDo(document("user/check-nickname-duplicate",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("nickname")
                                        .description("중복 체크할 닉네임")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.isAvailable").description("사용 가능 여부 (true: 사용 가능, false: 중복)")
                        )
                ));
    }

    @Test
    @DisplayName("디바이스 정보 업데이트 API")
    void updateDevice() throws Exception {
        // given
        DeviceFcmTokenRequestDto request = new DeviceFcmTokenRequestDto("test-fcm-token");

        willDoNothing().given(userGrpcClient).updateDevice(anyLong(), anyString(), anyString());

        // when & then
        mockMvc.perform(post("/api/user/device")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .header("X-Device-Id", "test-device-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("user/update-device",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})"),
                                headerWithName("X-Device-Id").description("디바이스 ID")
                        ),
                        requestFields(
                                fieldWithPath("fcmToken").description("FCM 푸시 알림 토큰")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터 (업데이트 완료 메시지)")
                        )
                ));
    }
}
