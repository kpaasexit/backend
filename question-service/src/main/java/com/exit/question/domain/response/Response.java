package com.exit.question.domain.response;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    @Column(name = "response_title", length = 100)
    private String responseTitle;

    @Column(name = "response_content", columnDefinition = "TEXT")
    private String responseContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_disclosure")
    private ResponseDisclosureType responseDisclosure;

    @Column(name = "response_adopt")
    private Boolean responseAdopt;

}