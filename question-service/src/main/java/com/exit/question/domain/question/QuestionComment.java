package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import com.exit.question.domain.Comment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "question_comment_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "question_comment_updated_at"))
public class QuestionComment extends BaseEntity implements Comment {

    @Id
    @Column(name = "question_comment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "question_comment_writer_id")
    private Long writerId;

    @Column(name = "question_comment_content", length = 200)
    private String content;

    @Builder
    public QuestionComment(Question question, String content, Long writerId) {
        this.question = question;
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
