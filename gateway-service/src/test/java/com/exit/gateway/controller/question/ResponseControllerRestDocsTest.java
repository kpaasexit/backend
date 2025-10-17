package com.exit.gateway.controller.question;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserDetailRequest;
import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.controller.question.dto.response.question.*;
import com.exit.gateway.global.resolver.UserIdArgumentResolver;
import com.exit.gateway.service.question.QuestionRequestMapper;
import com.exit.gateway.service.question.ResponseGrpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuestionController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureRestDocs
@Import({
        RestDocsConfiguration.class,
        JwtTokenProvider.class
})
class ResponseControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ResponseGrpcClient responseGrpcClient;

    @MockBean
    private QuestionRequestMapper questionRequestMapper;

    @MockBean
    private UserIdArgumentResolver userIdArgumentResolver;

    private String validAccessToken;

    @BeforeEach
    void setUp() {
        UserDetailRequest userDetail = new UserDetailRequest(1L, "test-device-id");
        validAccessToken = jwtTokenProvider.generateAccessToken(userDetail);

        // Mock UserIdArgumentResolver
        given(userIdArgumentResolver.supportsParameter(any())).willAnswer(invocation -> {
            org.springframework.core.MethodParameter param = invocation.getArgument(0);
            return param.hasParameterAnnotation(com.exit.gateway.global.annotation.LoginUser.class);
        });
        given(userIdArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(1L);
    }

    @Test
    @DisplayName("답변 채택 API")
    void adoptAnswer() throws Exception {
        // given
        AnswerAdoptResponseDto response = AnswerAdoptResponseDto.builder()
                .responseId(1L)
                .isAdopted(true)
                .build();

        given(responseGrpcClient.adoptAnswer(any())).willReturn(response);

        // when & then
        mockMvc.perform(org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post("/api/responses/answers/{responseId}/adopt", 1L))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseId").value(1L))
                .andExpect(jsonPath("$.result.isAdopted").value(true))
                .andDo(document("response/answer-adopt",
                        pathParameters(
                                parameterWithName("responseId").description("답변 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.responseId").description("답변 ID"),
                                fieldWithPath("result.isAdopted").description("채택 여부")
                        )
                ));
    }

    @Test
    @DisplayName("답변 추천 API")
    void recommendAnswer() throws Exception {
        // given
        AnswerRecommendResponseDto response = AnswerRecommendResponseDto.builder()
                .responseId(1L)
                .isRecommended(true)
                .recommendCount(10)
                .build();

        given(responseGrpcClient.recommendAnswer(any())).willReturn(response);

        // when & then
        mockMvc.perform(org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post("/api/responses/answers/{responseId}/recommend", 1L))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseId").value(1L))
                .andExpect(jsonPath("$.result.isRecommended").value(true))
                .andExpect(jsonPath("$.result.recommendCount").value(10))
                .andDo(document("response/answer-recommend",
                        pathParameters(
                                parameterWithName("responseId").description("답변 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.responseId").description("답변 ID"),
                                fieldWithPath("result.isRecommended").description("추천 여부"),
                                fieldWithPath("result.recommendCount").description("추천 수")
                        )
                ));
    }

    @Test
    @DisplayName("답변 수정 API")
    void updateAnswer() throws Exception {
        // given
        AnswerUpdateResponseDto response = new AnswerUpdateResponseDto(1L, "수정된 답변 내용입니다.");

        given(responseGrpcClient.updateResponse(any())).willReturn(response);

        // when & then
        mockMvc.perform(put("/api/responses/answers/{responseId}", 1L)
                        .contentType("application/json")
                        .content("{\"content\": \"수정된 답변 내용입니다.\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseId").value(1L))
                .andExpect(jsonPath("$.result.content").value("수정된 답변 내용입니다."))
                .andDo(document("response/answer-update",
                        pathParameters(
                                parameterWithName("responseId").description("답변 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.responseId").description("답변 ID"),
                                fieldWithPath("result.content").description("수정된 답변 내용")
                        )
                ));
    }

    @Test
    @DisplayName("답변 삭제 API")
    void deleteAnswer() throws Exception {
        // given
        willDoNothing().given(responseGrpcClient).deleteResponse(any());

        // when & then
        mockMvc.perform(delete("/api/responses/answers/{responseId}", 1L))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result").value("성공적으로 삭제하였습니다."))
                .andDo(document("response/answer-delete",
                        pathParameters(
                                parameterWithName("responseId").description("답변 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터")
                        )
                ));
    }

    @Test
    @DisplayName("답변 생성 API")
    void createAnswer() throws Exception {
        // given

        MockMultipartFile image1 = new MockMultipartFile(
                "images",                           // DTO의 필드명과 일치
                "test-image1.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        MockMultipartFile image2 = new MockMultipartFile(
                "images",
                "test-image2.png",
                "image/png",
                "another image".getBytes()
        );
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        AnswerCreateResponseDto response = AnswerCreateResponseDto.builder()
                .responseId(1L)
                .responseContent("JWT 토큰은 다음과 같이 구현할 수 있습니다...")
                .questionId(1L)
                .responseWriterId(2L)
                .createdAt(now)
                .build();

        given(responseGrpcClient.createAnswer(any())).willReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/responses/answers")
                        .file(image1)
                        .file(image2)
                        .param("questionId", "1")
                        .param("responseContent", "JWT 토큰은 다음과 같이 구현할 수 있습니다...")
                        .param("responseWriterId", "2"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseId").value(1L))
                .andExpect(jsonPath("$.result.responseContent").value("JWT 토큰은 다음과 같이 구현할 수 있습니다..."))
                .andDo(document("response/answer-create",
                        requestParts(
                                partWithName("images")
                                        .description("업로드할 이미지 파일 목록 (선택)")
                                        .optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.responseId").description("답변 ID"),
                                fieldWithPath("result.responseContent").description("답변 내용"),
                                fieldWithPath("result.questionId").description("질문 ID"),
                                fieldWithPath("result.responseWriterId").description("답변 작성자 ID"),
                                fieldWithPath("result.createdAt").description("작성일시")
                        )
                ));
    }

    @Test
    @DisplayName("답변 신고 API")
    void reportAnswer() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        AnswerReportResponseDto response = AnswerReportResponseDto.builder()
                .responseReportId(1L)
                .responseId(1L)
                .responseReportTitle("부정확한 정보")
                .responseReportContent("답변 내용이 부정확합니다.")
                .responseReportWriterId(1L)
                .createdAt(now)
                .build();

        given(responseGrpcClient.reportAnswer(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/responses/answers/{responseId}/report", 1L)
                        .contentType("application/json")
                        .content("{\"responseReportTitle\": \"부정확한 정보\", \"responseReportContent\": \"답변 내용이 부정확합니다.\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseReportId").value(1L))
                .andDo(document("response/answer-report",
                        pathParameters(
                                parameterWithName("responseId").description("답변 ID")
                        ),
                        requestFields(
                                fieldWithPath("responseReportTitle").description("신고 제목"),
                                fieldWithPath("responseReportContent").description("신고 내용")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.responseReportId").description("답변 신고 ID"),
                                fieldWithPath("result.responseId").description("답변 ID"),
                                fieldWithPath("result.responseReportTitle").description("신고 제목"),
                                fieldWithPath("result.responseReportContent").description("신고 내용"),
                                fieldWithPath("result.responseReportWriterId").description("신고자 ID"),
                                fieldWithPath("result.createdAt").description("신고일시")
                        )
                ));
    }
}
