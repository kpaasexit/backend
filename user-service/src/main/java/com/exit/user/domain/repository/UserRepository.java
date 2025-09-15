package com.exit.user.domain.repository;

import com.exit.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findBySocialIdAndProvider(String socialId, String provider);
}