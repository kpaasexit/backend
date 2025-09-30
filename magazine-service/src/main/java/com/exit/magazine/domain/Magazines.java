package com.exit.magazine.domain;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Magazines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "magazine_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "magazine_updated_at"))
public class Magazines extends BaseEntity {
    @Id
    @Column(name = "magazine_id")
    private Long magazineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_category_id")
    private MagazineCategories magazineCategory;

    @Column(name = "magazine_author_id")
    private Long magazineAuthorId;

    @Column(name = "magazine_title", length = 200)
    private String magazineTitle;

    @Column(name = "magazine_subtitle", length = 200)
    private String magazineSubtitle;

    @Column(name = "magazine_content", columnDefinition = "TEXT")
    private String magazineContent;

    @Column(name = "magazine_thumbnail_url", length = 500)
    private String magazineThumbnailUrl;

    @Builder
    public Magazines(MagazineCategories category, Long authorId, String magazineTitle, String magazineSubtitle, String magazineContent, String magazineThumbnailUrl) {
        this.magazineCategory = category;
        this.magazineAuthorId = authorId;
        this.magazineTitle = magazineTitle;
        this.magazineSubtitle = magazineSubtitle;
        this.magazineContent = magazineContent;
        this.magazineThumbnailUrl = magazineThumbnailUrl;
    }
}