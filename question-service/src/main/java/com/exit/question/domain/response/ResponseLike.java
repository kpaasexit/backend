package com.exit.question.domain.response;

import com.exit.common.domain.BaseEntity;
import com.exit.common.grpc.AnswerRecommendRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "response_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "response_like_created_at"))
public class ResponseLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_like_id")
    private Long responseLikeId;

    @Column(name = "response_id", nullable = false)
    private Long responseId;

    @Column(name = "user_id")
    private Long userId;

    @Builder
    public ResponseLike(Long responseId, Long userId) {
        this.responseId = responseId;
        this.userId = userId;
    }

    public static ResponseLike from(AnswerRecommendRequest request) {
        return ResponseLike.builder()
                .responseId(request.getResponseId())
                .userId(request.getUserId())
                .build();
    }
}