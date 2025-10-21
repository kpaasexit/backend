package com.exit.gateway.controller.question;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserDetailRequest;
import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.controller.question.dto.response.question.*;
import com.exit.gateway.controller.question.dto.response.response.GetAiBestResponseDto.GetAiBestResponseItem;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.exit.gateway.restdocs.MultipartFormParametersSnippet.multipartFormParameters;
import static com.exit.gateway.restdocs.MultipartFormParametersSnippet.multipartParameter;

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
                        .param("responseContent", "JWT 토큰은 다음과 같이 구현할 수 있습니다..."))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseId").value(1L))
                .andExpect(jsonPath("$.result.responseContent").value("JWT 토큰은 다음과 같이 구현할 수 있습니다..."))
                .andDo(document("response/answer-create",
                        relaxedRequestParts(
                                partWithName("images").description("업로드할 이미지 파일 목록 (선택)").optional()
                        ),
                        multipartFormParameters(
                                multipartParameter("questionId").description("질문 ID"),
                                multipartParameter("responseContent").description("답변 내용"),
                                multipartParameter("responseIsAnonymous").description("익명으로 등록할 것인지 (기본값 false)").optional()
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
                .responseReportReason(1)
                .responseReportContent("답변 내용이 부정확합니다.")
                .responseReportWriterId(1L)
                .createdAt(now)
                .build();

        given(responseGrpcClient.reportAnswer(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/responses/answers/{responseId}/report", 1L)
                        .contentType("application/json")
                        .content("{\"responseReportReason\": 1, \"responseReportContent\": \"답변 내용이 부정확합니다.\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseReportId").value(1L))
                .andDo(document("response/answer-report",
                        pathParameters(
                                parameterWithName("responseId").description("답변 ID")
                        ),
                        requestFields(
                                fieldWithPath("responseReportReason").description("신고 사유 ID"),
                                fieldWithPath("responseReportContent").description("신고 내용")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.responseReportId").description("답변 신고 ID"),
                                fieldWithPath("result.responseId").description("답변 ID"),
                                fieldWithPath("result.responseReportReason").description("신고 사유 ID"),
                                fieldWithPath("result.responseReportContent").description("신고 내용"),
                                fieldWithPath("result.responseReportWriterId").description("신고자 ID"),
                                fieldWithPath("result.createdAt").description("신고일시")
                        )
                ));
    }

    @Test
    @DisplayName("답변 리스트 상세 조회 API")
    void responseDetail() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        ResponseDetailDto response1 = ResponseDetailDto.builder()
                .responseId(1L)
                .responseWriterId(2L)
                .responseWriterName("김답변")
                .responseWriterProfile("https://example.com/profile/kim.jpg")
                .responseContent("JWT 토큰은 다음과 같이 구현할 수 있습니다...")
                .responseAdopt(true)
                .urls(List.of("https://example.com/image1.jpg", "https://example.com/image2.jpg"))
                .likeCount(15)
                .createdAt(now)
                .updatedAt(now)
                .isAi(false)
                .authority(new com.exit.gateway.controller.question.dto.response.authority.ResponseAuthority(true, true, true))
                .build();

        ResponseDetailDto response2 = ResponseDetailDto.builder()
                .responseId(2L)
                .responseWriterId(3L)
                .responseWriterName("이개발")
                .responseWriterProfile("https://example.com/profile/lee.jpg")
                .responseContent("다른 방법으로는 이렇게 할 수도 있습니다...")
                .responseAdopt(false)
                .urls(List.of())
                .likeCount(5)
                .createdAt(now)
                .updatedAt(now)
                .isAi(false)
                .authority(new com.exit.gateway.controller.question.dto.response.authority.ResponseAuthority(false, true, true))
                .build();

        GetDetailResponseResponseDto response = GetDetailResponseResponseDto.builder()
                .responses(List.of(response1, response2))
                .hasNext(true)
                .build();

        given(responseGrpcClient.getDetailResponse(any(), any(), any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/responses/{questionId}/responses", 1L)
                        .param("pageNum", "1")
                        .param("size", "5"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responses[0].responseId").value(1L))
                .andExpect(jsonPath("$.result.responses[0].responseAdopt").value(true))
                .andExpect(jsonPath("$.result.responses[0].likeCount").value(15))
                .andExpect(jsonPath("$.result.responses[1].responseId").value(2L))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andDo(document("response/detail-list",
                        pathParameters(
                                parameterWithName("questionId").description("질문 ID")
                        ),
                        queryParameters(
                                parameterWithName("pageNum").description("페이지 번호 (1부터 시작, 기본값: 1)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값: 5)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.responses").description("답변 목록"),
                                fieldWithPath("result.responses[].responseId").description("답변 ID"),
                                fieldWithPath("result.responses[].responseWriterId").description("답변 작성자 ID"),
                                fieldWithPath("result.responses[].responseWriterName").description("답변 작성자 이름"),
                                fieldWithPath("result.responses[].responseWriterProfile").description("답변 작성자 프로필 URL").optional(),
                                fieldWithPath("result.responses[].responseContent").description("답변 내용"),
                                fieldWithPath("result.responses[].responseAdopt").description("채택 여부"),
                                fieldWithPath("result.responses[].urls").description("답변 이미지 URL 목록"),
                                fieldWithPath("result.responses[].likeCount").description("좋아요 수"),
                                fieldWithPath("result.responses[].createdAt").description("작성일시"),
                                fieldWithPath("result.responses[].updatedAt").description("수정일시"),
                                fieldWithPath("result.responses[].isAi").description("AI 답변 여부"),
                                fieldWithPath("result.responses[].authority").description("권한 정보"),
                                fieldWithPath("result.responses[].authority.canAdopt").description("채택 권한 여부"),
                                fieldWithPath("result.responses[].authority.canModify").description("수정 권한 여부"),
                                fieldWithPath("result.responses[].authority.canDelete").description("삭제 권한 여부"),
                                fieldWithPath("result.hasNext").description("다음 페이지 존재 여부"),
                                fieldWithPath("result.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("result.totalPageNum").description("전체 페이지 개수")
                        )
                ));
    }

    @Test
    @DisplayName("AI 베스트 답변 조회 API")
    void aiBestResponse() throws Exception {
        // given
        GetAiBestResponseItem item1 =
                GetAiBestResponseItem.builder()
                        .questionId(1L)
                        .responseId(10L)
                        .title("JWT 토큰 인증 구현 방법")
                        .content("JWT 토큰은 다음과 같이 구현할 수 있습니다...")
                        .build();

        GetAiBestResponseItem item2 =
                GetAiBestResponseItem.builder()
                        .questionId(2L)
                        .responseId(20L)
                        .title("Spring Security 설정 방법")
                        .content("Spring Security는 다음과 같이 설정합니다...")
                        .build();

        GetAiBestResponseItem item3 =
                com.exit.gateway.controller.question.dto.response.response.GetAiBestResponseDto.GetAiBestResponseItem.builder()
                        .questionId(3L)
                        .responseId(30L)
                        .title("JPA 연관관계 매핑")
                        .content("JPA에서 연관관계는 이렇게 매핑합니다...")
                        .build();

        com.exit.gateway.controller.question.dto.response.response.GetAiBestResponseDto response =
                com.exit.gateway.controller.question.dto.response.response.GetAiBestResponseDto.builder()
                        .aiBestResponseItemList(List.of(item1, item2, item3))
                        .build();

        given(responseGrpcClient.getBestAiResponse()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/responses/ai"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.aiBestResponseItemList[0].questionId").value(1L))
                .andExpect(jsonPath("$.result.aiBestResponseItemList[0].responseId").value(10L))
                .andExpect(jsonPath("$.result.aiBestResponseItemList[0].title").value("JWT 토큰 인증 구현 방법"))
                .andExpect(jsonPath("$.result.aiBestResponseItemList[1].questionId").value(2L))
                .andExpect(jsonPath("$.result.aiBestResponseItemList[2].questionId").value(3L))
                .andDo(document("response/ai-best-response",
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.aiBestResponseItemList").description("AI 베스트 답변 목록"),
                                fieldWithPath("result.aiBestResponseItemList[].questionId").description("질문 ID"),
                                fieldWithPath("result.aiBestResponseItemList[].responseId").description("답변 ID"),
                                fieldWithPath("result.aiBestResponseItemList[].title").description("질문 제목"),
                                fieldWithPath("result.aiBestResponseItemList[].content").description("답변 내용")
                        )
                ));
    }
}
