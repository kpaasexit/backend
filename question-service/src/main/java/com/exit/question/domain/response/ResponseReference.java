package com.exit.question.domain.response;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "response_references")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "response_reference_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "response_reference_updated_at"))
public class ResponseReference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_reference_id")
    private Long responseReferenceId;

    @Column(name = "response_id", nullable = false)
    private Long responseId;

    @Column(name = "response_reference_url", length = 512)
    private String responseReferenceUrl;
}