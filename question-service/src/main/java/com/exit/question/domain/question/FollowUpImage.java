package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "follow_up_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AttributeOverride(name = "createdAt", column = @Column(name = "follow_up_image_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "follow_up_image_updated_at"))
public class FollowUpImage extends BaseEntity {

    @Id
    @Column(name = "follow_up_image_id")
    private Long followUpImageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_message_id", insertable = false, updatable = false)
    private FollowUpMessage followUpMessage;

    @Column(name = "follow_up_image_url", length = 512)
    private String followUpImageUrl;
}