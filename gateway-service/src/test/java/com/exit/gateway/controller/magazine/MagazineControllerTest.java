package com.exit.gateway.controller.magazine;

import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.controller.magazine.dto.response.MagazineItemDto;
import com.exit.gateway.controller.magazine.dto.response.MagazineItemListDto;
import com.exit.gateway.service.magazine.MagazineGrpcClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MagazineController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
class MagazineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MagazineGrpcClient magazineGrpcClient;

    @Test
    @DisplayName("카테고리별 매거진 목록 조회 API")
    void getMagazinesByCategory() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        MagazineItemDto item1 = MagazineItemDto.builder()
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

        MagazineItemDto item2 = MagazineItemDto.builder()
                .magazineId(2L)
                .magazineCategoryId(1L)
                .magazineTitle("MSA 아키텍처 가이드")
                .magazineSubtitle("마이크로서비스 설계 원칙")
                .magazineContent("마이크로서비스 아키텍처(MSA)는...")
                .magazineAuthor("이아키")
                .authorProfileUrl("https://example.com/profile/lee.jpg")
                .magazineThumbnailUrl("https://example.com/thumbnail/msa.jpg")
                .createdAt(now)
                .build();

        MagazineItemListDto response = new MagazineItemListDto(List.of(item1, item2));

        given(magazineGrpcClient.getMagazinesByCategory(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/magazine/category/{categoryId}", 1L)
                        .param("pageNum", "1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.magazineItems[0].magazineId").value(1L))
                .andExpect(jsonPath("$.result.magazineItems[0].magazineTitle").value("K-Paas 플랫폼 소개"))
                .andExpect(jsonPath("$.result.magazineItems[1].magazineId").value(2L))
                .andDo(document("magazine/list",
                        pathParameters(
                                parameterWithName("categoryId").description("카테고리 ID")
                        ),
                        queryParameters(
                                parameterWithName("pageNum").description("페이지 번호 (기본값: 1)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.magazineItems").description("매거진 목록"),
                                fieldWithPath("result.magazineItems[].magazineId").description("매거진 ID"),
                                fieldWithPath("result.magazineItems[].magazineCategoryId").description("매거진 카테고리 ID"),
                                fieldWithPath("result.magazineItems[].magazineTitle").description("매거진 제목"),
                                fieldWithPath("result.magazineItems[].magazineSubtitle").description("매거진 부제목"),
                                fieldWithPath("result.magazineItems[].magazineContent").description("매거진 내용"),
                                fieldWithPath("result.magazineItems[].magazineAuthor").description("작성자"),
                                fieldWithPath("result.magazineItems[].authorProfileUrl").description("작성자 프로필 URL"),
                                fieldWithPath("result.magazineItems[].magazineThumbnailUrl").description("매거진 썸네일 URL"),
                                fieldWithPath("result.magazineItems[].createdAt").description("작성일시")
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
                                fieldWithPath("result.createdAt").description("작성일시")
                        )
                ));
    }
}
