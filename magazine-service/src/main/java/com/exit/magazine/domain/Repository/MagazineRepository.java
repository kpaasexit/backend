package com.exit.magazine.domain.Repository;

import com.exit.magazine.domain.Magazines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MagazineRepository extends JpaRepository<Long, Magazines> {
}
