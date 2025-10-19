package com.exit.gateway.controller.magazine;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserDetailRequest;
import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.controller.magazine.dto.response.GetRecommendedMagazineResponseDto;
import com.exit.gateway.controller.magazine.dto.response.GetScrapBoxResponseDto;
import com.exit.gateway.controller.magazine.dto.response.MagazineItemDto;
import com.exit.gateway.controller.magazine.dto.response.MagazineListDto;
import com.exit.gateway.controller.magazine.dto.response.MagazineListItemDto;
import com.exit.gateway.controller.magazine.dto.response.ScrapMagazineResponseDto;
import com.exit.gateway.global.resolver.UserIdArgumentResolver;
import com.exit.gateway.service.magazine.MagazineGrpcClient;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MagazineController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureRestDocs
@Import({
        RestDocsConfiguration.class,
        JwtTokenProvider.class
})
class MagazineControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private MagazineGrpcClient magazineGrpcClient;

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
    @DisplayName("카테고리별 매거진 목록 조회 API")
    void getMagazinesByCategory() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        MagazineListItemDto item1 = MagazineListItemDto.builder()
                .magazineId(1L)
                .magazineCategoryId(1L)
                .magazineTitle("K-Paas 플랫폼 소개")
                .magazineSubtitle("클라우드 네이티브 플랫폼의 모든 것")
                .magazineAuthor("김개발")
                .authorProfileUrl("https://example.com/profile/kim.jpg")
                .magazineThumbnailUrl("https://example.com/thumbnail/kpaas.jpg")
                .createdAt(now)
                .build();

        MagazineListItemDto item2 = MagazineListItemDto.builder()
                .magazineId(2L)
                .magazineCategoryId(1L)
                .magazineTitle("MSA 아키텍처 가이드")
                .magazineSubtitle("마이크로서비스 설계 원칙")
                .magazineAuthor("이아키")
                .authorProfileUrl("https://example.com/profile/lee.jpg")
                .magazineThumbnailUrl("https://example.com/thumbnail/msa.jpg")
                .createdAt(now)
                .build();

        MagazineListDto response = new MagazineListDto(List.of(item1, item2), 1, true);

        given(magazineGrpcClient.getMagazinesByCategory(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/magazine/category/{categoryId}", 1L)
                        .param("pageNum", "1")
                        .param("size", "2"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.magazineListItems[0].magazineId").value(1L))
                .andExpect(jsonPath("$.result.magazineListItems[0].magazineTitle").value("K-Paas 플랫폼 소개"))
                .andExpect(jsonPath("$.result.magazineListItems[1].magazineId").value(2L))
                .andDo(document("magazine/list",
                        pathParameters(
                                parameterWithName("categoryId").description("카테고리 ID")
                        ),
                        queryParameters(
                                parameterWithName("pageNum").description("페이지 번호 (기본값: 1)").optional(),
                                parameterWithName("size").description("페이지 번호 (기본값: 5)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.magazineListItems").description("매거진 목록"),
                                fieldWithPath("result.magazineListItems[].magazineId").description("매거진 ID"),
                                fieldWithPath("result.magazineListItems[].magazineCategoryId").description(
                                        "매거진 카테고리 ID"),
                                fieldWithPath("result.magazineListItems[].magazineTitle").description("매거진 제목"),
                                fieldWithPath("result.magazineListItems[].magazineSubtitle").description("매거진 부제목"),
                                fieldWithPath("result.magazineListItems[].magazineAuthor").description("작성자"),
                                fieldWithPath("result.magazineListItems[].authorProfileUrl").description("작성자 프로필 URL"),
                                fieldWithPath("result.magazineListItems[].magazineThumbnailUrl").description(
                                        "매거진 썸네일 URL"),
                                fieldWithPath("result.magazineListItems[].createdAt").description("작성일시"),
                                fieldWithPath("result.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("result.hasNext").description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    @DisplayName("매거진 상세 조회 API")
    void getMagazine() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        MagazineItemDto response = MagazineItemDto.builder()
                .magazineId(1L)
                .magazineCategoryId(1L)
                .magazineTitle("K-Paas 플랫폼 소개")
                .magazineSubtitle("클라우드 네이티브 플랫폼의 모든 것")
                .magazineContent("K-Paas는 혁신적인 클라우드 플랫폼입니다...")
                .magazineAuthor("김개발")
                .authorProfileUrl("https://example.com/profile/kim.jpg")
                .magazineThumbnailUrl("https://example.com/thumbnail/kpaas.jpg")
                .createdAt(now)
                .build();

        given(magazineGrpcClient.getMagazine(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/magazine/{magazineId}", 1L))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.magazineId").value(1L))
                .andExpect(jsonPath("$.result.magazineTitle").value("K-Paas 플랫폼 소개"))
                .andExpect(jsonPath("$.result.magazineAuthor").value("김개발"))
                .andDo(document("magazine/detail",
                        pathParameters(
                                parameterWithName("magazineId").description("매거진 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.magazineId").description("매거진 ID"),
                                fieldWithPath("result.magazineCategoryId").description("매거진 카테고리 ID"),
                                fieldWithPath("result.magazineTitle").description("매거진 제목"),
                                fieldWithPath("result.magazineSubtitle").description("매거진 부제목"),
                                fieldWithPath("result.magazineContent").description("매거진 내용"),
                                fieldWithPath("result.magazineAuthor").description("작성자"),
                                fieldWithPath("result.authorProfileUrl").description("작성자 프로필 URL"),
                                fieldWithPath("result.magazineThumbnailUrl").description("매거진 썸네일 URL"),
                                fieldWithPath("result.createdAt").description("작성일시"),
                                fieldWithPath("result.isScrap").description("스크랩 여부")
                        )
                ));
    }

    @Test
    @DisplayName("매거진 스크랩 API")
    void scrapMagazine() throws Exception {
        // given
        ScrapMagazineResponseDto response = ScrapMagazineResponseDto.builder()
                .magazineId(1L)
                .isScrapped(true)
                .build();

        given(magazineGrpcClient.scrapMagazine(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/magazine/{magazineId}/scrap", 1L)
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.magazineId").value(1L))
                .andExpect(jsonPath("$.result.isScrapped").value(true))
                .andDo(document("magazine/scrap",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        pathParameters(
                                parameterWithName("magazineId").description("매거진 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.magazineId").description("매거진 ID"),
                                fieldWithPath("result.isScrapped").description("스크랩 여부 (true: 스크랩됨, false: 스크랩 취소됨)")
                        )
                ));
    }

    @Test
    @DisplayName("스크랩 박스 조회 API")
    void getScrapBox() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        GetScrapBoxResponseDto.MagazineScrapBoxItem item1 = GetScrapBoxResponseDto.MagazineScrapBoxItem.builder()
                .magazineId(1L)
                .magazineTitle("스크랩한 매거진 1")
                .magazineSubtitle("유용한 정보")
                .magazineThumbnailUrl("https://example.com/thumb1.jpg")
                .createdAt(now)
                .build();

        GetScrapBoxResponseDto.MagazineScrapBoxItem item2 = GetScrapBoxResponseDto.MagazineScrapBoxItem.builder()
                .magazineId(2L)
                .magazineTitle("스크랩한 매거진 2")
                .magazineSubtitle("흥미로운 이야기")
                .magazineThumbnailUrl("https://example.com/thumb2.jpg")
                .createdAt(now)
                .build();

        GetScrapBoxResponseDto response = GetScrapBoxResponseDto.builder()
                .scrapBoxItems(List.of(item1, item2))
                .currentPage(1)
                .hasNext(false)
                .build();

        given(magazineGrpcClient.getScrapBox(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/magazine/scrap-box")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .param("pageNum", "1")
                        .param("size", "5"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.scrapBoxItems[0].magazineId").value(1L))
                .andExpect(jsonPath("$.result.scrapBoxItems[0].magazineTitle").value("스크랩한 매거진 1"))
                .andExpect(jsonPath("$.result.scrapBoxItems[1].magazineId").value(2L))
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andDo(document("magazine/scrap-box",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        queryParameters(
                                parameterWithName("pageNum").description("페이지 번호 (1부터 시작)").optional(),
                                parameterWithName("size").description("페이지 번호 (기본값: 5)").optional()

                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.scrapBoxItems").description("스크랩한 매거진 목록"),
                                fieldWithPath("result.scrapBoxItems[].magazineId").description("매거진 ID"),
                                fieldWithPath("result.scrapBoxItems[].magazineTitle").description("매거진 제목"),
                                fieldWithPath("result.scrapBoxItems[].magazineSubtitle").description("매거진 부제목"),
                                fieldWithPath("result.scrapBoxItems[].magazineThumbnailUrl").description("매거진 썸네일 URL"),
                                fieldWithPath("result.scrapBoxItems[].createdAt").description("작성일시"),
                                fieldWithPath("result.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("result.hasNext").description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    @DisplayName("추천 매거진 조회 API")
    void getRecommendedMagazine() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        GetRecommendedMagazineResponseDto.RecommendedMagazineItem item1 = GetRecommendedMagazineResponseDto.RecommendedMagazineItem.builder()
                .magazineId(1L)
                .magazineTitle("추천 매거진 1")
                .magazineSubtitle("유용한 정보")
                .magazineThumbnailUrl("https://example.com/thumb1.jpg")
                .createdAt(now)
                .build();

        GetRecommendedMagazineResponseDto.RecommendedMagazineItem item2 = GetRecommendedMagazineResponseDto.RecommendedMagazineItem.builder()
                .magazineId(2L)
                .magazineTitle("추천 매거진 2")
                .magazineSubtitle("흥미로운 이야기")
                .magazineThumbnailUrl("https://example.com/thumb2.jpg")
                .createdAt(now)
                .build();

        GetRecommendedMagazineResponseDto response = GetRecommendedMagazineResponseDto.builder()
                .recommendedMagazineItems(List.of(item1, item2))
                .build();

        given(magazineGrpcClient.getRecommendedMagazine(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/magazine/recommend")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.recommendedMagazineItems[0].magazineId").value(1L))
                .andExpect(jsonPath("$.result.recommendedMagazineItems[0].magazineTitle").value("추천 매거진 1"))
                .andExpect(jsonPath("$.result.recommendedMagazineItems[1].magazineId").value(2L))
                .andDo(document("magazine/recommend",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("액세스 토큰 (Bearer {token})")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.recommendedMagazineItems").description("추천 매거진 목록"),
                                fieldWithPath("result.recommendedMagazineItems[].magazineId").description("매거진 ID"),
                                fieldWithPath("result.recommendedMagazineItems[].magazineTitle").description("매거진 제목"),
                                fieldWithPath("result.recommendedMagazineItems[].magazineSubtitle").description(
                                        "매거진 부제목"),
                                fieldWithPath("result.recommendedMagazineItems[].magazineThumbnailUrl").description(
                                        "매거진 썸네일 URL"),
                                fieldWithPath("result.recommendedMagazineItems[].createdAt").description("작성일시")
                        )
                ));
    }
}
