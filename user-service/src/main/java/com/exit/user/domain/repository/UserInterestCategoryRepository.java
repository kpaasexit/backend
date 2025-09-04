package com.exit.user.domain.repository;

import com.exit.user.domain.UserInterestCategories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInterestCategoryRepository extends JpaRepository<UserInterestCategories, Long> {
}
