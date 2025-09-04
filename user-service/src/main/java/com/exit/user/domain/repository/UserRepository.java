package com.exit.user.domain.repository;

import com.exit.user.domain.Users;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    boolean existsByUserEmail(@NotBlank(message = "이메일은 필수입니다") @Email(message = "올바른 이메일 형식이 아닙니다") String email);
}