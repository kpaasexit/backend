package com.exit.gateway.controller.question;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserDetailRequest;
import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.controller.question.dto.ImageObjectDto;
import com.exit.gateway.controller.question.dto.response.question.*;
import com.exit.gateway.global.resolver.UserIdArgumentResolver;
import com.exit.gateway.service.question.QuestionGrpcClient;
import com.exit.gateway.service.question.QuestionRequestMapper;
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

import static com.exit.gateway.restdocs.MultipartFormParametersSnippet.multipartFormParameters;
import static com.exit.gateway.restdocs.MultipartFormParametersSnippet.multipartParameter;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
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
class QuestionControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private QuestionGrpcClient questionGrpcClient;

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
    @DisplayName("질문 목록 조회 API")
    void getQuestionList() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        QuestionListQueryResponseDto question1 = QuestionListQueryResponseDto.builder()
                .questionId(1L)
                .questionCategoryId(1L)
                .questionWriterName("고구마")
                .questionWriterProfile("profile url")
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
                .questionWriterName("감자")
                .questionWriterProfile("profile url")
                .questionTitle("Docker Compose 설정 문의")
                .questionContent("멀티 컨테이너 환경에서...")
                .questionUrgency(false)
                .questionAnswerType("GENERAL")
                .questionAnswerAdopt(true)
                .answerCount(5)
                .createdAt(now)
                .build();

        QuestionListResponseDto response = QuestionListResponseDto.builder()
                .questionListItems(List.of(question1, question2))
                .hasNext(true)
                .build();

        given(questionGrpcClient.getQuestionList(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/questions")
                        .param("categoryIds", "1", "2")
                        .param("keyword", "Spring")
                        .param("page", "0")
                        .param("size", "5")
                        .param("isAdopted", "false"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.questionListItems[0].questionId").value(1L))
                .andExpect(jsonPath("$.result.questionListItems[0].questionTitle").value("Spring Boot에서 JWT 인증 구현하는 방법"))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andDo(document("question/list",
                        queryParameters(
                                parameterWithName("categoryIds").description("카테고리 ID 목록 (선택)").optional(),
                                parameterWithName("keyword").description("검색 키워드 (선택)").optional(),
                                parameterWithName("page").description("페이지 번호 (기본값: 1)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값: 5)").optional(),
                                parameterWithName("isAdopted").description("답변 채택 여부 (기본값: false)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.questionListItems").description("질문 목록"),
                                fieldWithPath("result.questionListItems[].questionId").description("질문 ID"),
                                fieldWithPath("result.questionListItems[].questionCategoryId").description("질문 카테고리 ID"),
                                fieldWithPath("result.questionListItems[].questionWriterName").description("작성자 닉네임"),
                                fieldWithPath("result.questionListItems[].questionWriterProfile").description("작성자 프로필 url"),
                                fieldWithPath("result.questionListItems[].questionTitle").description("질문 제목"),
                                fieldWithPath("result.questionListItems[].questionContent").description("질문 내용"),
                                fieldWithPath("result.questionListItems[].questionUrgency").description("긴급 여부"),
                                fieldWithPath("result.questionListItems[].questionAnswerType").description("답변 타입"),
                                fieldWithPath("result.questionListItems[].questionAnswerAdopt").description("답변 채택 여부"),
                                fieldWithPath("result.questionListItems[].answerCount").description("답변 개수"),
                                fieldWithPath("result.questionListItems[].createdAt").description("작성일시"),
                                fieldWithPath("result.hasNext").description("다음 페이지 존재 여부"),
                                fieldWithPath("result.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("result.totalPageNum").description("전체 페이지 개수")
                        )
                ));
    }

    @Test
    @DisplayName("질문 상세 조회 API")
    void getQuestionDetail() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        ImageObjectDto[] imageObjectDtos = {
                new ImageObjectDto(1L, "url"),
                new ImageObjectDto(2L, "url")
        };
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
                .images(List.of(imageObjectDtos))
                .createdAt(now)
                .build();


        QuestionDetailResponseDto response = new QuestionDetailResponseDto(
                question,
                new com.exit.gateway.controller.question.dto.response.authority.QuestionAuthority(true, true)
        );

        given(questionGrpcClient.getQuestionDetail(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/questions/{questionId}", 1L))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.question.questionId").value(1L))
                .andExpect(jsonPath("$.result.question.questionTitle").value("Spring Boot에서 JWT 인증 구현하는 방법"))
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
                                fieldWithPath("result.question.questionWriterProfile").description("작성자 프로필"),
                                fieldWithPath("result.question.images").description("이미지 객체 목록"),
                                fieldWithPath("result.question.images[].imageId").description("이미지 id"),
                                fieldWithPath("result.question.images[].imageUrl").description("이미지 URL"),
                                fieldWithPath("result.question.createdAt").description("작성일시"),
                                fieldWithPath("result.authority").description("권한 정보"),
                                fieldWithPath("result.authority.canModify").description("수정 권한 여부"),
                                fieldWithPath("result.authority.canDelete").description("삭제 권한 여부")
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
                .images(List.of())
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
                        .param("questionDisclosureType", "PUBLIC"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.questionId").value(1L))
                .andExpect(jsonPath("$.result.questionTitle").value("Spring JWT 인증 구현"))
                .andDo(document("question/create",
                        requestParts(                              // 파일 문서화
                                partWithName("images")
                                        .description("업로드할 이미지 파일 목록 (선택)")
                                        .optional()
                        ),
                        multipartFormParameters(
                                multipartParameter("questionTitle").description("질문 제목"),
                                multipartParameter("questionContent").description("질문 내용"),
                                multipartParameter("questionCategory").description("질문 카테고리 ID"),
                                multipartParameter("questionUrgency").description("질문 긴급도 (기본값 false)").optional(),
                                multipartParameter("questionAnswerType").description("원하는 답변 타입 (INSTANT or COMMUNITY)"),
                                multipartParameter("questionDisclosureType").description("질문 공개 타입 (PUBLIC or PRIVATE)"),
                                multipartParameter("questionIsAnonymous").description("익명 여부(기본값 false)").optional()
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
                                fieldWithPath("result.questionWriterProfile").description("작성자 프로필").optional(),
                                fieldWithPath("result.images").description("이미지 객체 목록").optional(),
                                fieldWithPath("result.images[].imageId").type("Number").description("이미지 id").optional(),
                                fieldWithPath("result.images[].imageUrl").type("String").description("이미지 URL").optional(),
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
                .questionReportReason(1)
                .questionReportContent("질문 내용이 부적절합니다.")
                .questionReportWriterId(1L)
                .createdAt(now)
                .build();

        given(questionGrpcClient.reportQuestion(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/questions/{questionId}/report", 1L)
                        .contentType("application/json")
                        .content("{\"questionReportReason\": 1, \"questionReportContent\": \"질문 내용이 부적절합니다.\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.questionReportId").value(1L))
                .andDo(document("question/report",
                        pathParameters(
                                parameterWithName("questionId").description("질문 ID")
                        ),
                        requestFields(
                                fieldWithPath("questionReportReason").description("신고 사유 id"),
                                fieldWithPath("questionReportContent").description("신고 내용")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.questionId").description("질문 ID"),
                                fieldWithPath("result.questionReportId").description("질문 신고 ID"),
                                fieldWithPath("result.questionReportReason").description("신고 사유 ID"),
                                fieldWithPath("result.questionReportContent").description("신고 내용"),
                                fieldWithPath("result.questionReportWriterId").description("신고자 ID"),
                                fieldWithPath("result.createdAt").description("신고일시")
                        )
                ));
    }

    @Test
    @DisplayName("인기 게시물 조회 API")
    void getPopularPost() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        GetPopularPostResponseDto.PopularPost item1 = GetPopularPostResponseDto.PopularPost.builder()
                .categoryId(1L)
                .nickname("김개발")
                .profileUrl("https://example.com/profile.jpg")
                .createdAt(now)
                .title("Spring Boot 성능 최적화 방법")
                .content("대용량 트래픽 처리를 위한 최적화 방법을 알고 싶습니다...")
                .answerAdopt(true)
                .answerCount(15)
                .build();

        GetPopularPostResponseDto.PopularPost item2 = GetPopularPostResponseDto.PopularPost.builder()
                .categoryId(2L)
                .nickname("이프론트")
                .createdAt(now)
                .title("React vs Vue 선택 기준")
                .content("프로젝트에 적합한 프레임워크 선택 기준이 궁금합니다...")
                .answerAdopt(true)
                .answerCount(12)
                .build();

        GetPopularPostResponseDto response = GetPopularPostResponseDto.builder()
                .popularPostList(List.of(item1, item2))
                .build();

        given(questionGrpcClient.getPopularPost()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/questions/popular-post"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.popularPostList[0].title").value("Spring Boot 성능 최적화 방법"))
                .andExpect(jsonPath("$.result.popularPostList[1].title").value("React vs Vue 선택 기준"))
                .andDo(document("question/popular-post",
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.popularPostList").description("인기 게시물 목록"),
                                fieldWithPath("result.popularPostList[].questionId").description("질문 ID"),
                                fieldWithPath("result.popularPostList[].categoryId").description("카테고리 ID"),
                                fieldWithPath("result.popularPostList[].profileUrl").description("작성자 프로필 URL").optional(),
                                fieldWithPath("result.popularPostList[].nickname").description("작성자 닉네임"),
                                fieldWithPath("result.popularPostList[].createdAt").description("작성일시"),
                                fieldWithPath("result.popularPostList[].title").description("질문 제목"),
                                fieldWithPath("result.popularPostList[].content").description("질문 내용"),
                                fieldWithPath("result.popularPostList[].answerAdopt").description("답변 채택 여부"),
                                fieldWithPath("result.popularPostList[].answerCount").description("답변 개수")
                        )
                ));
    }

    @Test
    @DisplayName("내 질문 조회 API")
    void getMyQuestion() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        GetPopularPostResponseDto.PopularPost item1 = GetPopularPostResponseDto.PopularPost.builder()
                .categoryId(1L)
                .nickname("김사용자")
                .createdAt(now)
                .title("내가 작성한 첫 번째 질문")
                .content("질문 내용입니다...")
                .answerAdopt(true)
                .answerCount(5)
                .build();

        GetMyQuestionResponseDto response = GetMyQuestionResponseDto.builder()
                .questions(List.of(item1))
                .hasNext(false)
                .build();

        given(questionGrpcClient.getMyQuestion(anyLong(), anyInt(), anyInt())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/questions/my")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .param("pageNum", "1")
                        .param("size", "5"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.questions[0].title").value("내가 작성한 첫 번째 질문"))
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andDo(document("question/my-question",
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        queryParameters(
                                parameterWithName("pageNum").description("페이지 번호 (1부터 시작)").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.questions").description("내 질문 목록"),
                                fieldWithPath("result.questions[].questionId").description("질문 ID").optional(),
                                fieldWithPath("result.questions[].categoryId").description("카테고리 ID"),
                                fieldWithPath("result.questions[].profileUrl").description("작성자 프로필 URL").optional(),
                                fieldWithPath("result.questions[].nickname").description("작성자 닉네임"),
                                fieldWithPath("result.questions[].createdAt").description("작성일시"),
                                fieldWithPath("result.questions[].title").description("질문 제목"),
                                fieldWithPath("result.questions[].content").description("질문 내용"),
                                fieldWithPath("result.questions[].answerAdopt").description("답변 채택 여부"),
                                fieldWithPath("result.questions[].answerCount").description("답변 개수"),
                                fieldWithPath("result.hasNext").description("다음 페이지 존재 여부"),
                                fieldWithPath("result.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("result.totalPageNum").description("전체 페이지 개수")
                        )
                ));
    }
}
