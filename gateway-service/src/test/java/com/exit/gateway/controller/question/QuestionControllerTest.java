package com.exit.gateway.controller.question;

import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.controller.question.dto.response.question.*;
import com.exit.gateway.global.resolver.UserIdArgumentResolver;
import com.exit.gateway.service.question.QuestionGrpcClient;
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
@Import(RestDocsConfiguration.class)
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionGrpcClient questionGrpcClient;

    @MockBean
    private com.exit.gateway.service.question.QuestionRequestMapper questionRequestMapper;

    @MockBean
    private UserIdArgumentResolver userIdArgumentResolver;

    @BeforeEach
    void setUp() {
        // Mock UserIdArgumentResolver
        given(userIdArgumentResolver.supportsParameter(any())).willAnswer(invocation -> {
            org.springframework.core.MethodParameter param = invocation.getArgument(0);
            return param.hasParameterAnnotation(com.exit.gateway.global.annotation.LoginUser.class);
        });
        given(userIdArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(1L);
    }

    @Test
    @DisplayName("질문 목록 조회 API")
    void getQuestionList() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        QuestionListQueryResponseDto question1 = QuestionListQueryResponseDto.builder()
                .questionId(1L)
                .questionCategoryId(1L)
                .questionWriterId(1L)
                .questionTitle("Spring Boot에서 JWT 인증 구현하는 방법")
                .questionContent("JWT 토큰을 사용한 인증을 구현하고 싶습니다...")
                .questionUrgency(true)
                .questionAnswerType("GENERAL")
                .questionAnswerAdopt(false)
                .answerCount(3)
                .createdAt(now)
                .build();

        QuestionListQueryResponseDto question2 = QuestionListQueryResponseDto.builder()
                .questionId(2L)
                .questionCategoryId(2L)
                .questionWriterId(2L)
                .questionTitle("Docker Compose 설정 문의")
                .questionContent("멀티 컨테이너 환경에서...")
                .questionUrgency(false)
                .questionAnswerType("GENERAL")
                .questionAnswerAdopt(true)
                .answerCount(5)
                .createdAt(now)
                .build();

        QuestionListResponseDto response = QuestionListResponseDto.builder()
                .questionList(List.of(question1, question2))
                .hasNext(true)
                .build();

        given(questionGrpcClient.getQuestionList(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/questions")
                        .param("categoryIds", "1", "2")
                        .param("keyword", "Spring")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.questionList[0].questionId").value(1L))
                .andExpect(jsonPath("$.result.questionList[0].questionTitle").value("Spring Boot에서 JWT 인증 구현하는 방법"))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andDo(document("question/list",
                        queryParameters(
                                parameterWithName("categoryIds").description("카테고리 ID 목록 (선택)").optional(),
                                parameterWithName("keyword").description("검색 키워드 (선택)").optional(),
                                parameterWithName("page").description("페이지 번호 (기본값: 0)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값: 10)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.questionList").description("질문 목록"),
                                fieldWithPath("result.questionList[].questionId").description("질문 ID"),
                                fieldWithPath("result.questionList[].questionCategoryId").description("질문 카테고리 ID"),
                                fieldWithPath("result.questionList[].questionWriterId").description("작성자 ID"),
                                fieldWithPath("result.questionList[].questionTitle").description("질문 제목"),
                                fieldWithPath("result.questionList[].questionContent").description("질문 내용"),
                                fieldWithPath("result.questionList[].questionUrgency").description("긴급 여부"),
                                fieldWithPath("result.questionList[].questionAnswerType").description("답변 타입"),
                                fieldWithPath("result.questionList[].questionAnswerAdopt").description("답변 채택 여부"),
                                fieldWithPath("result.questionList[].answerCount").description("답변 개수"),
                                fieldWithPath("result.questionList[].createdAt").description("작성일시"),
                                fieldWithPath("result.hasNext").description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    @DisplayName("질문 상세 조회 API")
    void getQuestionDetail() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        QuestionCreateResponseDto question = QuestionCreateResponseDto.builder()
                .questionId(1L)
                .questionTitle("Spring Boot에서 JWT 인증 구현하는 방법")
                .questionContent("JWT 토큰을 사용한 인증을 구현하고 싶습니다...")
                .questionCategory(1L)
                .questionUrgency(true)
                .questionAnswerType("GENERAL")
                .questionDisclosureType("PUBLIC")
                .questionWriterId(1L)
                .questionWriterName("김개발")
                .imageUrls(List.of("https://example.com/image1.jpg"))
                .createdAt(now)
                .build();

        ResponseDetailDto response1 = ResponseDetailDto.builder()
                .responseId(1L)
                .responseWriterId(2L)
                .responseWriterName("이답변")
                .responseContent("다음과 같이 구현하시면 됩니다...")
                .responseAdopt(true)
                .urls(List.of())
                .likeCount(5)
                .createdAt(now)
                .updatedAt(now)
                .build();

        QuestionDetailResponseDto response = new QuestionDetailResponseDto(
                question,
                List.of(response1),
                false
        );

        given(questionGrpcClient.getQuestionDetail(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/questions/{questionId}", 1L))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.question.questionId").value(1L))
                .andExpect(jsonPath("$.result.question.questionTitle").value("Spring Boot에서 JWT 인증 구현하는 방법"))
                .andExpect(jsonPath("$.result.responses[0].responseId").value(1L))
                .andDo(document("question/detail",
                        pathParameters(
                                parameterWithName("questionId").description("질문 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.question").description("질문 정보"),
                                fieldWithPath("result.question.questionId").description("질문 ID"),
                                fieldWithPath("result.question.questionTitle").description("질문 제목"),
                                fieldWithPath("result.question.questionContent").description("질문 내용"),
                                fieldWithPath("result.question.questionCategory").description("질문 카테고리 ID"),
                                fieldWithPath("result.question.questionUrgency").description("긴급 여부"),
                                fieldWithPath("result.question.questionAnswerType").description("답변 타입"),
                                fieldWithPath("result.question.questionDisclosureType").description("공개 타입"),
                                fieldWithPath("result.question.questionWriterId").description("작성자 ID"),
                                fieldWithPath("result.question.questionWriterName").description("작성자 이름"),
                                fieldWithPath("result.question.imageUrls").description("이미지 URL 목록"),
                                fieldWithPath("result.question.createdAt").description("작성일시"),
                                fieldWithPath("result.responses").description("답변 목록"),
                                fieldWithPath("result.responses[].responseId").description("답변 ID"),
                                fieldWithPath("result.responses[].responseWriterId").description("답변 작성자 ID"),
                                fieldWithPath("result.responses[].responseWriterName").description("답변 작성자 이름"),
                                fieldWithPath("result.responses[].responseContent").description("답변 내용"),
                                fieldWithPath("result.responses[].responseAdopt").description("채택 여부"),
                                fieldWithPath("result.responses[].urls").description("첨부 파일 URL 목록"),
                                fieldWithPath("result.responses[].likeCount").description("추천 수"),
                                fieldWithPath("result.responses[].createdAt").description("작성일시"),
                                fieldWithPath("result.responses[].updatedAt").description("수정일시"),
                                fieldWithPath("result.hasNext").description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    @DisplayName("카테고리 추천 API")
    void recommendCategory() throws Exception {
        // given
        CategoryRecommendationResponseDto response = CategoryRecommendationResponseDto.builder()
                .questionCategoryId(1L)
                .questionCategoryName("Spring Framework")
                .build();

        given(questionGrpcClient.recommendCategory(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/questions/categories/recommend")
                        .param("title", "Spring Boot JWT 인증"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.questionCategoryId").value(1L))
                .andExpect(jsonPath("$.result.questionCategoryName").value("Spring Framework"))
                .andDo(document("question/category-recommend",
                        queryParameters(
                                parameterWithName("title").description("질문 제목")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.questionCategoryId").description("추천 카테고리 ID"),
                                fieldWithPath("result.questionCategoryName").description("추천 카테고리 이름")
                        )
                ));
    }

    @Test
    @DisplayName("답변 채택 API")
    void adoptAnswer() throws Exception {
        // given
        AnswerAdoptResponseDto response = AnswerAdoptResponseDto.builder()
                .responseId(1L)
                .isAdopted(true)
                .build();

        given(questionGrpcClient.adoptAnswer(any())).willReturn(response);

        // when & then
        mockMvc.perform(org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post("/api/questions/answers/{responseId}/adopt", 1L))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseId").value(1L))
                .andExpect(jsonPath("$.result.isAdopted").value(true))
                .andDo(document("question/answer-adopt",
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

        given(questionGrpcClient.recommendAnswer(any())).willReturn(response);

        // when & then
        mockMvc.perform(org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post("/api/questions/answers/{responseId}/recommend", 1L))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseId").value(1L))
                .andExpect(jsonPath("$.result.isRecommended").value(true))
                .andExpect(jsonPath("$.result.recommendCount").value(10))
                .andDo(document("question/answer-recommend",
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
    @DisplayName("유사 질문 조회 API")
    void getSimilarQuestion() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        SimilarQuestionItemDto item1 = SimilarQuestionItemDto.builder()
                .questionId(1L)
                .questionTitle("Spring Boot JWT 인증 구현")
                .questionContent("JWT 인증 방법에 대해 질문합니다...")
                .questionCategory(1L)
                .questionUrgency(false)
                .questionAnswerType("GENERAL")
                .questionAnswerAdopt(true)
                .createdAt(now)
                .build();

        SimilarQuestionResponseDto response = SimilarQuestionResponseDto.builder()
                .similarQuestionResponseDto(List.of(item1))
                .build();

        given(questionGrpcClient.getSimilarQuestion(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/questions/similar")
                        .param("title", "Spring JWT")
                        .param("content", "인증 구현"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.similarQuestionResponseDto[0].questionId").value(1L))
                .andDo(document("question/similar",
                        queryParameters(
                                parameterWithName("title").description("질문 제목"),
                                parameterWithName("content").description("질문 내용")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.similarQuestionResponseDto").description("유사 질문 목록"),
                                fieldWithPath("result.similarQuestionResponseDto[].questionId").description("질문 ID"),
                                fieldWithPath("result.similarQuestionResponseDto[].questionTitle").description("질문 제목"),
                                fieldWithPath("result.similarQuestionResponseDto[].questionContent").description("질문 내용"),
                                fieldWithPath("result.similarQuestionResponseDto[].questionCategory").description("질문 카테고리 ID"),
                                fieldWithPath("result.similarQuestionResponseDto[].questionUrgency").description("긴급 여부"),
                                fieldWithPath("result.similarQuestionResponseDto[].questionAnswerType").description("답변 타입"),
                                fieldWithPath("result.similarQuestionResponseDto[].questionAnswerAdopt").description("답변 채택 여부"),
                                fieldWithPath("result.similarQuestionResponseDto[].createdAt").description("작성일시")
                        )
                ));
    }

    @Test
    @DisplayName("답변 수정 API")
    void updateAnswer() throws Exception {
        // given
        AnswerUpdateResponseDto response = new AnswerUpdateResponseDto(1L, "수정된 답변 내용입니다.");

        given(questionGrpcClient.updateResponse(any())).willReturn(response);

        // when & then
        mockMvc.perform(put("/api/questions/answers/{responseId}", 1L)
                        .contentType("application/json")
                        .content("{\"content\": \"수정된 답변 내용입니다.\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseId").value(1L))
                .andExpect(jsonPath("$.result.content").value("수정된 답변 내용입니다."))
                .andDo(document("question/answer-update",
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
        willDoNothing().given(questionGrpcClient).deleteResponse(any());

        // when & then
        mockMvc.perform(delete("/api/questions/answers/{responseId}", 1L))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result").value("성공적으로 삭제하였습니다."))
                .andDo(document("question/answer-delete",
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
    @DisplayName("질문 생성 API")
    void createQuestion() throws Exception {
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

        QuestionCreateResponseDto response = QuestionCreateResponseDto.builder()
                .questionId(1L)
                .questionTitle("Spring JWT 인증 구현")
                .questionContent("JWT 토큰 기반 인증을 구현하고 싶습니다.")
                .questionCategory(1L)
                .questionUrgency(true)
                .questionAnswerType("PUBLIC")
                .questionDisclosureType("PUBLIC")
                .questionWriterId(1L)
                .questionWriterName("김사용자")
                .imageUrls(List.of())
                .createdAt(now)
                .build();

        given(questionGrpcClient.createQuestion(any())).willReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/questions")
                        .file(image1)
                        .file(image2)
                        .param("questionTitle", "Spring JWT 인증 구현")
                        .param("questionContent", "JWT 토큰 기반 인증을 구현하고 싶습니다.")
                        .param("questionCategory", "1")
                        .param("questionUrgency", "true")
                        .param("questionAnswerType", "PUBLIC")
                        .param("questionDisclosureType", "PUBLIC")
                        .param("questionWriterId", "1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.questionId").value(1L))
                .andExpect(jsonPath("$.result.questionTitle").value("Spring JWT 인증 구현"))
                .andDo(document("question/create",
                        requestParts(                              // 파일 문서화
                                partWithName("images")
                                        .description("업로드할 이미지 파일 목록 (선택)")
                                        .optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.questionId").description("질문 ID"),
                                fieldWithPath("result.questionTitle").description("질문 제목"),
                                fieldWithPath("result.questionContent").description("질문 내용"),
                                fieldWithPath("result.questionCategory").description("질문 카테고리 ID"),
                                fieldWithPath("result.questionUrgency").description("긴급 여부"),
                                fieldWithPath("result.questionAnswerType").description("답변 타입"),
                                fieldWithPath("result.questionDisclosureType").description("공개 타입"),
                                fieldWithPath("result.questionWriterId").description("작성자 ID"),
                                fieldWithPath("result.questionWriterName").description("작성자 이름"),
                                fieldWithPath("result.imageUrls").description("이미지 URL 목록"),
                                fieldWithPath("result.createdAt").description("작성일시")
                        )
                ));
    }

    @Test
    @DisplayName("질문 신고 API")
    void reportQuestion() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        QuestionReportResponseDto response = QuestionReportResponseDto.builder()
                .questionId(1L)
                .questionReportId(1L)
                .questionReportTitle("부적절한 내용")
                .questionReportContent("질문 내용이 부적절합니다.")
                .questionReportWriterId(1L)
                .createdAt(now)
                .build();

        given(questionGrpcClient.reportQuestion(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/questions/{questionId}/report", 1L)
                        .contentType("application/json")
                        .content("{\"questionReportTitle\": \"부적절한 내용\", \"questionReportContent\": \"질문 내용이 부적절합니다.\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.questionReportId").value(1L))
                .andDo(document("question/report",
                        pathParameters(
                                parameterWithName("questionId").description("질문 ID")
                        ),
                        requestFields(
                                fieldWithPath("questionReportTitle").description("신고 제목"),
                                fieldWithPath("questionReportContent").description("신고 내용")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.questionId").description("질문 ID"),
                                fieldWithPath("result.questionReportId").description("질문 신고 ID"),
                                fieldWithPath("result.questionReportTitle").description("신고 제목"),
                                fieldWithPath("result.questionReportContent").description("신고 내용"),
                                fieldWithPath("result.questionReportWriterId").description("신고자 ID"),
                                fieldWithPath("result.createdAt").description("신고일시")
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

        given(questionGrpcClient.createAnswer(any())).willReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/questions/answers")
                        .file(image1)
                        .file(image2)
                        .param("questionId", "1")
                        .param("responseContent", "JWT 토큰은 다음과 같이 구현할 수 있습니다...")
                        .param("responseWriterId", "2"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseId").value(1L))
                .andExpect(jsonPath("$.result.responseContent").value("JWT 토큰은 다음과 같이 구현할 수 있습니다..."))
                .andDo(document("question/answer-create",
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

        given(questionGrpcClient.reportAnswer(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/questions/answers/{responseId}/report", 1L)
                        .contentType("application/json")
                        .content("{\"responseReportTitle\": \"부정확한 정보\", \"responseReportContent\": \"답변 내용이 부정확합니다.\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.responseReportId").value(1L))
                .andDo(document("question/answer-report",
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
