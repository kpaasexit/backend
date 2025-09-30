package com.exit.quiz.domain;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "quiz_category_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "quiz_category_updated_at"))
public class QuizCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_category_id")
    private Short id;

    @Column(name = "quiz_category_name", length = 20)
    private String name;
}
