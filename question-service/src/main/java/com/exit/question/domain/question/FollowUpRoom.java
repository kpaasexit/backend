package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import com.exit.question.domain.response.Response;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "follow_up_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AttributeOverride(name = "createdAt", column = @Column(name = "follow_up_room_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "follow_up_room_updated_at"))
@NamedEntityGraph(
        name = "FollowUpRoom.full",
        attributeNodes = {
                @NamedAttributeNode(value = "followUpMessages", subgraph = "messages"),
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "messages",
                        attributeNodes = {
                                @NamedAttributeNode("followUpImages")
                        }
                )
        }
)
public class FollowUpRoom extends BaseEntity {

    @Id
    @Column(name = "follow_up_room_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long followUpRoomId;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "response_id")
    private Response response;

    @OneToMany(mappedBy = "followUpRoom", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 5)
    private List<FollowUpMessage> followUpMessages = new ArrayList<>();

    public FollowUpRoom(Response response) {
        this.response = response;
    }

    @Builder
    public FollowUpRoom(Response response, List<FollowUpMessage> followUpMessages) {
        this.response = response;
        this.followUpMessages = followUpMessages;
    }

    public FollowUpMessage getLastMessage() {
        if(followUpMessages.isEmpty()) return null;

        int size = followUpMessages.size();
        return followUpMessages.get(size - 1);
    }
}
