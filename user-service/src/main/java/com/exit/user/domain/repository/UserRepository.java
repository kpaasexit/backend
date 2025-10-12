package com.exit.user.domain.repository;

import com.exit.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findBySocialIdAndProviderAndUserDeletedFalse(String socialId, String provider);

    List<Users> findAllByUserIdIn(List<Long> userIds);

    Optional<Users> findByUserNickname(String nickname);
}