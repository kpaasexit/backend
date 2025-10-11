package com.exit.magazine.domain;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MagazineCategories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "magzine_category_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "magzine_category_updated_at"))
public class MagazineCategories extends BaseEntity {
    @Id
    @Column(name = "magazine_category_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long magazineCategoryId;

    @Column(name = "magazine_category_name", length = 20)
    private String magazineCategoryName;
}