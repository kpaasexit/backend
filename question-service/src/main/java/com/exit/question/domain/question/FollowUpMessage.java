package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

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
    public FollowUpMessage(FollowUpRoom followUpRoom, Long followUpMessageWriterId, String followUpMessageContent, List<FollowUpImage> followUpImages) {
        this.followUpRoom = followUpRoom;
        this.followUpMessageWriterId = followUpMessageWriterId;
        this.followUpMessageContent = followUpMessageContent;
        this.followUpImages = followUpImages;
    }

    public List<String> getImageUrlList() {
        return followUpImages.stream()
                .map(FollowUpImage::getFollowUpImageUrl)
                .toList();
    }
}
