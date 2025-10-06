package com.exit.gateway.controller.quiz;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserDetailRequest;
import com.exit.common.grpc.*;
import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.controller.quiz.dto.response.quiz.GetSolvedQuizResponseDto;
import com.exit.gateway.controller.quiz.dto.response.quiz.ReportQuizResponseDto;
import com.exit.gateway.global.resolver.DeviceIdArgumentResolver;
import com.exit.gateway.global.resolver.UserIdArgumentResolver;
import com.exit.gateway.service.quiz.QuizGrpcClient;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuizController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureRestDocs
@Import({
        RestDocsConfiguration.class,
        JwtTokenProvider.class
})
@TestPropertySource(properties = {
        "jwt.secret_key=dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tdGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4=",
        "jwt.access_token.valid_time=3600000",
        "jwt.refresh_token.valid_time=604800000"
})
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private QuizGrpcClient quizGrpcClient;

    @MockBean
    private DeviceIdArgumentResolver deviceIdArgumentResolver;

    @MockBean
    private UserIdArgumentResolver userIdArgumentResolver;

    private String validAccessToken;

    @BeforeEach
    void setUp() throws Exception {
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
    @DisplayName("카테고리별 통계 조회 API")
    void getCategoryStatistics() throws Exception {
        // given
        com.exit.common.grpc.CategoryStat stat1 = com.exit.common.grpc.CategoryStat.newBuilder()
                .setCategoryId(1L)
                .setCategoryQuizNum(50)
                .setCategorySolvedNum(10)
                .build();

        com.exit.common.grpc.CategoryStat stat2 = com.exit.common.grpc.CategoryStat.newBuilder()
                .setCategoryId(2L)
                .setCategoryQuizNum(30)
                .setCategorySolvedNum(5)
                .build();

        GetCategoryStatisticsResponse grpcResponse = GetCategoryStatisticsResponse.newBuilder()
                .addCategoryStat(stat1)
                .addCategoryStat(stat2)
                .build();

        given(quizGrpcClient.getCategoryStatistics(anyLong())).willReturn(grpcResponse);

        // when & then
        mockMvc.perform(get("/api/quiz/categories/statistics")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result[0].categoryId").value(1L))
                .andExpect(jsonPath("$.result[0].quizTotalNum").value(50))
                .andExpect(jsonPath("$.result[0].quizSolvedNum").value(10))
                .andDo(document("quiz/category-statistics",
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result[].categoryId").description("카테고리 ID"),
                                fieldWithPath("result[].quizTotalNum").description("전체 퀴즈 수"),
                                fieldWithPath("result[].quizSolvedNum").description("푼 퀴즈 수")
                        )
                ));
    }

    @Test
    @DisplayName("퀴즈 조회 API")
    void getQuiz() throws Exception {
        // given
        GetQuizResponse grpcResponse = GetQuizResponse.newBuilder()
                .setQuizId(1L)
                .setQuizCategoryId(1L)
                .setQuizTitle("Java 기본 문법")
                .setQuizContent("다음 중 Java의 특징이 아닌 것은?")
                .setQuizType("MULTIPLE_CHOICE")
                .setQuizCorrectAnswer("3")
                .setQuizAdditionalInformation("1. 객체지향\n2. 플랫폼 독립적\n3. 저수준 언어\n4. 가비지 컬렉션")
                .build();

        given(quizGrpcClient.getQuiz(anyLong(), anyLong())).willReturn(grpcResponse);

        // when & then
        mockMvc.perform(get("/api/quiz/{categoryId}", 1L)
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.quizId").value(1L))
                .andExpect(jsonPath("$.result.quizTitle").value("Java 기본 문법"))
                .andDo(document("quiz/get",
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        pathParameters(
                                parameterWithName("categoryId").description("카테고리 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.quizId").description("퀴즈 ID"),
                                fieldWithPath("result.quizCategoryId").description("퀴즈 카테고리 ID"),
                                fieldWithPath("result.quizTitle").description("퀴즈 제목"),
                                fieldWithPath("result.quizContent").description("퀴즈 내용 (문제)"),
                                fieldWithPath("result.quizType").description("퀴즈 타입 (MULTIPLE_CHOICE, TRUE_FALSE 등)"),
                                fieldWithPath("result.quizCorrectAnswer").description("정답"),
                                fieldWithPath("result.quizAdditionalInformation").description("추가 정보 (선택지 등)")
                        )
                ));
    }

    @Test
    @DisplayName("퀴즈 답안 제출 API")
    void submitAnswer() throws Exception {
        // given
        SubmitAnswerResponse grpcResponse = SubmitAnswerResponse.newBuilder()
                .setQuizId(1L)
                .setQuizTotalAttemptNum(10)
                .setQuizCorrectNum(7)
                .setQuizCorrectPercent(70.0)
                .build();

        given(quizGrpcClient.submitAnswer(anyLong(), anyString(), anyLong())).willReturn(grpcResponse);

        // when & then
        mockMvc.perform(post("/api/quiz/{quizId}/submit", 1L)
                        .header("Authorization", "Bearer " + validAccessToken)
                        .param("answer", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.quizId").value(1L))
                .andExpect(jsonPath("$.result.quizCorrectPercent").value(70.0))
                .andDo(document("quiz/submit-answer",
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        pathParameters(
                                parameterWithName("quizId").description("퀴즈 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.quizId").description("퀴즈 ID"),
                                fieldWithPath("result.quizTotalAttemptNum").description("총 시도 횟수"),
                                fieldWithPath("result.quizCorrectNum").description("정답 횟수"),
                                fieldWithPath("result.quizCorrectPercent").description("정답률 (%)")
                        )
                ));
    }

    @Test
    @DisplayName("퀴즈 신고 API")
    void reportQuiz() throws Exception {
        // given
        ReportQuizResponse grpcResponse = ReportQuizResponse.newBuilder()
                .setQuizReportId(1L)
                .build();

        given(quizGrpcClient.reportQuiz(anyLong(), anyLong(), any())).willReturn(grpcResponse);

        // when & then
        mockMvc.perform(post("/api/quiz/{quizId}/report", 1L)
                        .header("Authorization", "Bearer " + validAccessToken)
                        .contentType("application/json")
                        .content("{\"title\": \"오타가 있습니다\", \"content\": \"문제 내용에 오타가 있어 신고합니다.\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.quizReportId").value(1L))
                .andDo(document("quiz/report",
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        pathParameters(
                                parameterWithName("quizId").description("퀴즈 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").description("신고 제목"),
                                fieldWithPath("content").description("신고 내용")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.quizReportId").description("퀴즈 신고 ID")
                        )
                ));
    }

    @Test
    @DisplayName("풀었던 퀴즈 목록 조회 API")
    void getSolvedQuiz() throws Exception {
        // given
        AttemptQuiz attemptQuiz1 = AttemptQuiz.newBuilder()
                .setQuizId(1L)
                .setQuizTitle("연차 사용 시 승인 절차는?")
                .build();

        AttemptQuiz attemptQuiz2 = AttemptQuiz.newBuilder()
                .setQuizId(2L)
                .setQuizTitle("퇴직금 지급 기준은?")
                .build();

        GetSolvedQuizResponse grpcResponse = GetSolvedQuizResponse.newBuilder()
                .addAttemptQuiz(attemptQuiz1)
                .addAttemptQuiz(attemptQuiz2)
                .setHasNext(true)
                .build();

        given(quizGrpcClient.getSolvedQuiz(anyLong(), any(), anyInt())).willReturn(grpcResponse);

        // when & then
        mockMvc.perform(get("/api/quiz/solved")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .param("categoryIds", "1,2")
                        .param("pageNum", "0"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.attemptQuiz[0].quizId").value(1L))
                .andExpect(jsonPath("$.result.attemptQuiz[0].quizTitle").value("연차 사용 시 승인 절차는?"))
                .andExpect(jsonPath("$.result.attemptQuiz[1].quizId").value(2L))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andDo(document("quiz/solved",
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.attemptQuiz").description("풀었던 퀴즈 목록"),
                                fieldWithPath("result.attemptQuiz[].quizId").description("퀴즈 ID"),
                                fieldWithPath("result.attemptQuiz[].quizTitle").description("퀴즈 제목"),
                                fieldWithPath("result.hasNext").description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    @DisplayName("퀴즈 정답 해설 조회 API")
    void resolveQuiz() throws Exception {
        // given
        GetQuizResponse grpcResponse = GetQuizResponse.newBuilder()
                .setQuizId(1L)
                .setQuizCategoryId(1L)
                .setQuizTitle("연차 사용")
                .setQuizContent("연차 사용 시 승인 절차는?")
                .setQuizType("MULTIPLE_CHOICE")
                .setQuizCorrectAnswer("1")
                .setQuizAdditionalInformation("1. 사전 승인 필요\n2. 사후 신고만 필요\n3. 승인 불필요\n4. 부서장 승인만 필요")
                .build();

        given(quizGrpcClient.resolveQuiz(anyLong())).willReturn(grpcResponse);

        // when & then
        mockMvc.perform(get("/api/quiz/{quizId}/resolve", 1L)
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.quizId").value(1L))
                .andExpect(jsonPath("$.result.quizContent").value("연차 사용 시 승인 절차는?"))
                .andExpect(jsonPath("$.result.quizCorrectAnswer").value("1"))
                .andDo(document("quiz/resolve",
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        pathParameters(
                                parameterWithName("quizId").description("퀴즈 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.quizId").description("퀴즈 ID"),
                                fieldWithPath("result.quizCategoryId").description("퀴즈 카테고리 ID"),
                                fieldWithPath("result.quizTitle").description("퀴즈 제목"),
                                fieldWithPath("result.quizContent").description("퀴즈 내용 (문제)"),
                                fieldWithPath("result.quizType").description("퀴즈 타입"),
                                fieldWithPath("result.quizCorrectAnswer").description("정답"),
                                fieldWithPath("result.quizAdditionalInformation").description("추가 정보 (선택지)")
                        )
                ));
    }
}
