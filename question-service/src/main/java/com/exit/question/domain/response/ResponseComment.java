package com.exit.question.domain.response;

import com.exit.common.domain.BaseEntity;
import com.exit.question.domain.Comment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "response_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "response_comment_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "response_comment_updated_at"))
public class ResponseComment extends BaseEntity implements Comment {

    @Id
    @Column(name = "response_comment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id")
    private Response response;

    @Column(name = "response_comment_writer_id")
    private Long writerId;

    @Column(name = "response_comment_content", length = 200)
    private String content;

    @Builder
    public ResponseComment(Response response, Long writerId, String content) {
        this.response = response;
        this.writerId = writerId;
        this.content = content;
    }

    @Override
    public Long getCommentId() {
        return id;
    }

    @Override
    public Long getAuthorId() {
        return writerId;
    }

    @Override
    public String getContent() {
        return content;
    }
}
