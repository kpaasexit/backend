package com.exit.question.domain.response;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "response_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "response_image_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "response_image_updated_at"))
public class ResponseImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_image_id")
    private Long responseImageId;

    @Column(name = "response_id", nullable = false)
    private Long responseId;

    @Column(name = "response_image_url", length = 512)
    private String responseImageUrl;
}