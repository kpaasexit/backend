package com.exit.magazine.domain.Repository;

import com.exit.magazine.domain.MagazineCategories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MagazineCategoryRepository extends JpaRepository<MagazineCategories, Long> {
}
