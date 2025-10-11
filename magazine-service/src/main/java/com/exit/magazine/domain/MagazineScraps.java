package com.exit.magazine.domain;

import com.exit.common.domain.BaseEntity;
import com.exit.common.grpc.ScrapMagazineRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "magazine_scrap")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "magazine_scrap_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "magazine_scrap_updated_at"))
public class MagazineScraps extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "magazine_scrap_id")
    private Long magazineScrapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id", nullable = false)
    private Magazines magazine;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder
    public MagazineScraps(Magazines magazine, Long userId) {
        this.magazine = magazine;
        this.userId = userId;
    }

    public static MagazineScraps from(Magazines magazine, Long userId) {
        return MagazineScraps.builder()
                .magazine(magazine)
                .userId(userId)
                .build();
    }
}