package com.exit.gateway.controller.question;

import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.controller.question.dto.response.question.CreateCommentResponseDto;
import com.exit.gateway.global.resolver.UserIdArgumentResolver;
import com.exit.gateway.service.question.CommentGrpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommentController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentGrpcClient commentGrpcClient;

    @MockBean
    private UserIdArgumentResolver userIdArgumentResolver;

    @BeforeEach
    void setUp() {
        given(userIdArgumentResolver.supportsParameter(any())).willAnswer(invocation -> {
            org.springframework.core.MethodParameter param = invocation.getArgument(0);
            return param.hasParameterAnnotation(com.exit.gateway.global.annotation.LoginUser.class);
        });
        given(userIdArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(1L);
    }

    @Test
    @DisplayName("댓글 생성 API")
    void createComment() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        CreateCommentResponseDto response = CreateCommentResponseDto.builder()
                .commentId(1L)
                .content("도움이 되는 답변 감사합니다!")
                .userName("김사용자")
                .createdAt(now)
                .build();

        given(commentGrpcClient.createComment(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/comments")
                        .contentType("application/json")
                        .content("{\"targetId\": 1, \"commentType\": \"RESPONSE\", \"content\": \"도움이 되는 답변 감사합니다!\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.commentId").value(1L))
                .andExpect(jsonPath("$.result.content").value("도움이 되는 답변 감사합니다!"))
                .andExpect(jsonPath("$.result.userName").value("김사용자"))
                .andDo(document("comment/create",
                        requestFields(
                                fieldWithPath("targetId").description("대상 ID (질문 또는 답변 ID)"),
                                fieldWithPath("commentType").description("댓글 타입 (QUESTION, RESPONSE)"),
                                fieldWithPath("content").description("댓글 내용")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.commentId").description("댓글 ID"),
                                fieldWithPath("result.content").description("댓글 내용"),
                                fieldWithPath("result.userName").description("작성자 이름"),
                                fieldWithPath("result.createdAt").description("작성일시")
                        )
                ));
    }

    @Test
    @DisplayName("댓글 삭제 API")
    void deleteComment() throws Exception {
        // given
        willDoNothing().given(commentGrpcClient).deleteComment(any());

        // when & then
        mockMvc.perform(delete("/api/comments/{commentId}", 1L)
                        .param("commentType", "RESPONSE"))
                .andExpect(status().is2xxSuccessful())
                .andDo(document("comment/delete",
                        pathParameters(
                                parameterWithName("commentId").description("댓글 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터 (null)")
                        )
                ));
    }
}
