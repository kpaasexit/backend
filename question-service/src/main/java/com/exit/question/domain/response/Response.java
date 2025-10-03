package com.exit.question.domain.response;

import com.exit.common.domain.BaseEntity;
import com.exit.common.grpc.AnswerCreateRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "response_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "response_updated_at"))
public class Response extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Long responseId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "response_writer_id")
    private Long responseWriterId;

    @Column(name = "response_content", columnDefinition = "TEXT")
    private String responseContent;

    @Column(name = "response_adopt")
    private Boolean responseAdopt;

    @Column(name = "response_is_anonymous")
    private Boolean responseIsAnonymous;

    @Builder
    public Response(Long questionId, Long responseWriterId, String responseContent, Boolean responseAdopt, Boolean responseIsAnonymous) {
        this.questionId = questionId;
        this.responseWriterId = responseWriterId;
        this.responseContent = responseContent;
        this.responseAdopt = responseAdopt;
        this.responseIsAnonymous = responseIsAnonymous;
    }

    public static Response createResponse(AnswerCreateRequest request) {
        return Response.builder()
                .questionId(request.getQuestionId())
                .responseWriterId(request.getResponseWriterId())
                .responseContent(request.getResponseContent())
                .responseAdopt(false)
                .responseIsAnonymous(request.getResponseIsAnonymous())
                .build();
    }

    public void updateResponseAdopt() {
        this.responseAdopt = true;
    }

    public void updateContent(String content) {
        responseContent = content;
    }
}