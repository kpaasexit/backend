package com.exit.question.domain.response;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
}