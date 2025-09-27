package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import com.exit.question.domain.response.Response;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "follow_up_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AttributeOverride(name = "createdAt", column = @Column(name = "follow_up_room_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "follow_up_room_updated_at"))
public class FollowUpRoom extends BaseEntity {

    @Id
    @Column(name = "follow_up_room_id")
    private Long followUpRoomId;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "response_id")
    private Response response;
}
