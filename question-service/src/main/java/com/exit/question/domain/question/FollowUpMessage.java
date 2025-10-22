package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import com.exit.common.grpc.ImageObject;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "follow_up_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AttributeOverride(name = "createdAt", column = @Column(name = "follow_up_message_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "follow_up_message_updated_at"))
public class FollowUpMessage extends BaseEntity {

    @Id
    @Column(name = "follow_up_message_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long followUpMessageId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "follow_up_room_id")
    private FollowUpRoom followUpRoom;

    @Column(name = "follow_up_message_writer_id")
    private Long followUpMessageWriterId;

    @Column(name = "follow_up_message_content", columnDefinition = "TEXT")
    private String followUpMessageContent;

    @OneToMany(mappedBy = "followUpMessage", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 50)
    private List<FollowUpImage> followUpImages = new ArrayList<>();

    @Builder
    public FollowUpMessage(FollowUpRoom followUpRoom, Long followUpMessageWriterId, String followUpMessageContent,
                           List<FollowUpImage> followUpImages) {
        this.followUpRoom = followUpRoom;
        this.followUpMessageWriterId = followUpMessageWriterId;
        this.followUpMessageContent = followUpMessageContent;
        this.followUpImages = followUpImages;
    }

    public List<ImageObject> getImageObjectList() {
        return followUpImages.stream()
                .sorted(Comparator.comparing(FollowUpImage::getCreatedAt))
                .map(image -> {
                    return ImageObject.newBuilder()
                            .setImageId(image.getFollowUpImageId())
                            .setImageUrl(image.getFollowUpImageUrl())
                            .build();
                })
                .toList();
    }
}
