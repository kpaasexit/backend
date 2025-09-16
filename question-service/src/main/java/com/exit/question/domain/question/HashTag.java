package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hash_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "hash_tag_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "hash_tag_updated_at"))
public class HashTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hash_tag_id")
    private Long hashTagId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "hash_tag_title", length = 20)
    private String hashTagTitle;

    @Builder
    public HashTag(Long questionId, String hashTagTitle) {
        this.questionId = questionId;
        this.hashTagTitle = hashTagTitle;
    }
}