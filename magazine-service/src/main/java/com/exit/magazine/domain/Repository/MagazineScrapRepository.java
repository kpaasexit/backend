package com.exit.magazine.domain.Repository;

import com.exit.magazine.domain.MagazineScraps;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MagazineScrapRepository extends JpaRepository<MagazineScraps, Long> {
    Optional<MagazineScraps> findByMagazine_MagazineIdAndUserId(Long magazineId, Long userId);

    @EntityGraph(attributePaths = {"magazine"})
    Slice<MagazineScraps> findByUserId(Long userId, PageRequest pageRequest);

    List<MagazineScraps> findByUserIdAndMagazine_MagazineIdIn(Long userId, List<Long> magazineIds);

    Optional<MagazineScraps> findByUserIdAndMagazine_magazineId(Long userId, Long magazineMagazineId);
}