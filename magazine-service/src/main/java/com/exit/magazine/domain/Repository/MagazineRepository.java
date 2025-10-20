package com.exit.magazine.domain.Repository;

import com.exit.magazine.domain.Magazines;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MagazineRepository extends JpaRepository<Magazines, Long> {
    Slice<Magazines> findAllByMagazineCategoryMagazineCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT m FROM Magazines m WHERE " +
           "m.magazineTitle like lower(concat('%', :keyword, '%')) OR " +
           "m.magazineSubtitle like lower(concat('%', :keyword, '%')) OR " +
           "m.magazineContent like lower(concat('%', :keyword, '%'))")
    Slice<Magazines> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    List<Magazines> findAllByMagazineIdIn(List<Long> longs);
}
