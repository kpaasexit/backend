package com.exit.user.domain;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table
@AttributeOverride(name = "createdAt", column = @Column(name = "user_interest_category_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "user_interest_category_updated_at"))
public class UserInterestCategories extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_interest_category_id")
    private Long userInterestCategoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private Users user;


    @Column(name = "category_id")
    private Short categoryId;
}
