package com.exit.gateway.controller.search;

import com.exit.common.grpc.QuestionListRequest;
import com.exit.common.grpc.QuestionListResponse;
import com.exit.common.grpc.SearchMagazinesResponse;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.controller.question.dto.response.question.QuestionListResponseDto;
import com.exit.gateway.service.magazine.MagazineGrpcClient;
import com.exit.gateway.service.question.QuestionGrpcClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SearchController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
class SearchControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionGrpcClient questionGrpcClient;
    @MockBean
    private MagazineGrpcClient magazineGrpcClient;

    @Test
    @DisplayName("통합 검색 API")
    void integratedSearch() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2025, 10, 16, 0, 0);

        // Question mock data
        com.exit.common.grpc.QuestionListItem question1 = com.exit.common.grpc.QuestionListItem.newBuilder()
                .setQuestionId(1L)
                .setQuestionCategory(1L)
                .setQuestionWriterName("김질문")
                .setQuestionWriterProfile("profile url")
                .setQuestionTitle("Spring Boot 질문입니다")
                .setQuestionContent("Spring Boot에서 JWT 인증은 어떻게 구현하나요?")
                .setQuestionUrgency(true)
                .setQuestionAnswerType("GENERAL")
                .setQuestionAnswerAdopt(false)
                .setAnswerCount(3)
                .setCreatedAt(TimeStampUtil.toGrpcTimestamp(now))
                .build();

        com.exit.common.grpc.QuestionListItem question2 = com.exit.common.grpc.QuestionListItem.newBuilder()
                .setQuestionId(2L)
                .setQuestionCategory(2L)
                .setQuestionWriterName("이개발")
                .setQuestionWriterProfile("profile url")
                .setQuestionTitle("MSA 구조 질문")
                .setQuestionContent("MSA에서 서비스간 통신은 어떻게 하나요?")
                .setQuestionUrgency(false)
                .setQuestionAnswerType("GENERAL")
                .setQuestionAnswerAdopt(true)
                .setAnswerCount(5)
                .setCreatedAt(TimeStampUtil.toGrpcTimestamp(now))
                .build();

        QuestionListResponse questionsResponse = QuestionListResponse.newBuilder()
                .addAllQuestions(List.of(question1, question2))
                .setHasNext(false)
                .build();

        // Magazine mock data
        com.exit.common.grpc.MagazineListItem magazine1 = com.exit.common.grpc.MagazineListItem.newBuilder()
                .setMagazineId(1L)
                .setMagazineCategoryId(1L)
                .setMagazineTitle("K-Paas 플랫폼 소개")
                .setMagazineSubtitle("클라우드 네이티브 플랫폼의 모든 것")
                .setMagazineAuthor("김개발")
                .setAuthorProfileUrl("https://example.com/profile/kim.jpg")
                .setMagazineThumbnailUrl("https://example.com/thumbnail/kpaas.jpg")
                .setCreatedAt(TimeStampUtil.toGrpcTimestamp(LocalDateTime.now()))
                .build();

        com.exit.common.grpc.MagazineListItem magazine2 = com.exit.common.grpc.MagazineListItem.newBuilder()
                .setMagazineId(2L)
                .setMagazineCategoryId(1L)
                .setMagazineTitle("MSA 아키텍처 가이드")
                .setMagazineSubtitle("마이크로서비스 설계 원칙")
                .setMagazineAuthor("이아키")
                .setAuthorProfileUrl("https://example.com/profile/lee.jpg")
                .setMagazineThumbnailUrl("https://example.com/thumbnail/msa.jpg")
                .setCreatedAt(TimeStampUtil.toGrpcTimestamp(LocalDateTime.now()))
                .build();

        SearchMagazinesResponse magazinesResponse = SearchMagazinesResponse.newBuilder()
                .addAllMagazines(List.of(magazine1, magazine2))
                .setHasNext(false)
                .build();

        given(questionGrpcClient.getQuestionList(any(QuestionListRequest.class))).willReturn(QuestionListResponseDto.from(questionsResponse));
        given(magazineGrpcClient.searchMagazines(anyString(), anyInt(), anyInt())).willReturn(magazinesResponse);

        // when & then
        mockMvc.perform(get("/api/search")
                        .param("keyword", "Spring")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.questions[0].questionId").value(1L))
                .andExpect(jsonPath("$.result.questions[0].questionTitle").value("Spring Boot 질문입니다"))
                .andExpect(jsonPath("$.result.magazines[0].magazineId").value(1L))
                .andExpect(jsonPath("$.result.magazines[0].magazineTitle").value("K-Paas 플랫폼 소개"))
                .andExpect(jsonPath("$.result.questionHasNext").value(false))
                .andExpect(jsonPath("$.result.magazineHasNext").value(false))
                .andDo(document("search/integrated",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("keyword").description("검색 키워드"),
                                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값: 10)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.questions").description("질문 검색 결과 목록"),
                                fieldWithPath("result.questions[].questionId").description("질문 ID"),
                                fieldWithPath("result.questions[].questionCategoryId").description("질문 카테고리 ID"),
                                fieldWithPath("result.questions[].questionWriterProfile").description("질문 작성자 프로필 url").optional(),
                                fieldWithPath("result.questions[].questionWriterName").description("질문 작성자 이름"),
                                fieldWithPath("result.questions[].questionTitle").description("질문 제목"),
                                fieldWithPath("result.questions[].questionContent").description("질문 내용"),
                                fieldWithPath("result.questions[].questionUrgency").description("긴급 여부"),
                                fieldWithPath("result.questions[].questionAnswerType").description("답변 타입"),
                                fieldWithPath("result.questions[].questionAnswerAdopt").description("답변 채택 여부"),
                                fieldWithPath("result.questions[].answerCount").description("답변 개수"),
                                fieldWithPath("result.questions[].createdAt").description("작성일시"),
                                fieldWithPath("result.magazines").description("매거진 검색 결과 목록"),
                                fieldWithPath("result.magazines[].magazineId").description("매거진 ID"),
                                fieldWithPath("result.magazines[].magazineCategoryId").description("매거진 카테고리 ID"),
                                fieldWithPath("result.magazines[].magazineTitle").description("매거진 제목"),
                                fieldWithPath("result.magazines[].magazineSubtitle").description("매거진 부제목"),
                                fieldWithPath("result.magazines[].magazineAuthor").description("매거진 작성자 닉네임"),
                                fieldWithPath("result.magazines[].authorProfileUrl").description("작성자 프로필 URL"),
                                fieldWithPath("result.magazines[].magazineThumbnailUrl").description("매거진 썸네일 URL"),
                                fieldWithPath("result.magazines[].createdAt").description("작성일시"),
                                fieldWithPath("result.questionHasNext").description("질문 다음 페이지 존재 여부"),
                                fieldWithPath("result.magazineHasNext").description("매거진 다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    @DisplayName("추천 검색어 조회 API")
    void getRecommendedSearchTerms() throws Exception {
        // when & then
        mockMvc.perform(get("/api/search/recommend"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.terms[0]").value("감자"))
                .andExpect(jsonPath("$.result.terms[1]").value("고구마"))
                .andExpect(jsonPath("$.result.terms[2]").value("강아지"))
                .andExpect(jsonPath("$.result.terms[3]").value("고양이"))
                .andDo(document("search/recommend",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.terms").type(JsonFieldType.ARRAY).description("추천 검색어 목록")
                        )
                ));
    }
}
