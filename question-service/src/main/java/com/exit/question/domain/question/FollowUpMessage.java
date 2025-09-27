package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "follow_up_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AttributeOverride(name = "createdAt", column = @Column(name = "follow_up_message_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "follow_up_message__updated_at"))
public class FollowUpMessage extends BaseEntity {

    @Id
    @Column(name = "follow_up_message_id")
    private Long followUpMessageId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "follow_up_room_id")
    private FollowUpRoom followUpRoom;

    @Column(name = "follow_up_message_writer_id")
    private Long followUpMessageWriterId;

    @Column(name = "follow_up_message_content", columnDefinition = "TEXT")
    private String followUpMessageContent;
}
