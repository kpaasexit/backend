package com.exit.gateway.controller.question;

import com.exit.gateway.config.RestDocsConfiguration;
import com.exit.gateway.controller.question.dto.response.question.CreateAdditionalQuestionMessageResponseDto;
import com.exit.gateway.controller.question.dto.response.question.GetAdditionalQuestionResponseDto;
import com.exit.gateway.controller.question.dto.response.question.MessageItemDto;
import com.exit.gateway.global.resolver.UserIdArgumentResolver;
import com.exit.gateway.service.question.AdditionalQuestionGrpcClient;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdditionalQuestionController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
class AdditionalQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdditionalQuestionGrpcClient additionalQuestionGrpcClient;

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
    @DisplayName("추가 질문 조회 API")
    void getAdditionalQuestion() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        MessageItemDto message1 = MessageItemDto.builder()
                .isQuestioner(true)
                .messageId(1L)
                .content("추가로 질문드립니다. 이 부분이 잘 이해가 안 되는데...")
                .images(List.of("https://example.com/image1.jpg"))
                .createdAt(now)
                .build();

        MessageItemDto message2 = MessageItemDto.builder()
                .isQuestioner(false)
                .messageId(2L)
                .content("네, 그 부분은 이렇게 설명드릴 수 있습니다...")
                .images(List.of())
                .createdAt(now.plusMinutes(30))
                .build();

        GetAdditionalQuestionResponseDto response = new GetAdditionalQuestionResponseDto(
                1L,
                List.of(message1, message2)
        );

        given(additionalQuestionGrpcClient.getAdditionalQuestion(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/additional-question/{followUpRoomId}", 1L)
                        .param("questionId", "1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.followUpRoomId").value(1L))
                .andExpect(jsonPath("$.result.messageList[0].messageId").value(1L))
                .andExpect(jsonPath("$.result.messageList[1].messageId").value(2L))
                .andDo(document("additional-question/get",
                        pathParameters(
                                parameterWithName("followUpRoomId").description("추가 질문 방 ID")
                        ),
                        queryParameters(
                                parameterWithName("questionId").description("질문 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.followUpRoomId").description("추가 질문 방 ID"),
                                fieldWithPath("result.messageList").description("메시지 목록"),
                                fieldWithPath("result.messageList[].isQuestioner").description("질문자 여부"),
                                fieldWithPath("result.messageList[].messageId").description("메시지 ID"),
                                fieldWithPath("result.messageList[].content").description("메시지 내용"),
                                fieldWithPath("result.messageList[].images").description("이미지 URL 목록"),
                                fieldWithPath("result.messageList[].createdAt").description("작성일시")
                        )
                ));
    }

    @Test
    @DisplayName("추가 질문 메시지 생성 API")
    void createAdditionalQuestionMessage() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);

        MessageItemDto message = MessageItemDto.builder()
                .isQuestioner(true)
                .messageId(3L)
                .content("추가 질문드립니다.")
                .images(List.of())
                .createdAt(now)
                .build();

        CreateAdditionalQuestionMessageResponseDto response = new CreateAdditionalQuestionMessageResponseDto(
                1L,
                message
        );

        given(additionalQuestionGrpcClient.createAdditionalQuestionMessage(any())).willReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/additional-question/message")
                        .param("questionId", "1")
                        .param("responseId", "2")
                        .param("content", "추가 질문드립니다."))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.result.followUpRoomId").value(1L))
                .andExpect(jsonPath("$.result.message.messageId").value(3L))
                .andExpect(jsonPath("$.result.message.content").value("추가 질문드립니다."))
                .andDo(document("additional-question/create",
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("응답 데이터"),
                                fieldWithPath("result.followUpRoomId").description("추가 질문 방 ID"),
                                fieldWithPath("result.message").description("생성된 메시지"),
                                fieldWithPath("result.message.isQuestioner").description("질문자 여부"),
                                fieldWithPath("result.message.messageId").description("메시지 ID"),
                                fieldWithPath("result.message.content").description("메시지 내용"),
                                fieldWithPath("result.message.images").description("이미지 URL 목록"),
                                fieldWithPath("result.message.createdAt").description("작성일시")
                        )
                ));
    }
}
