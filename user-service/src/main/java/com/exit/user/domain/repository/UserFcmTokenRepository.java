package com.exit.user.domain.repository;

import com.exit.user.domain.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    @Query("select uf.token from UserFcmToken uf where uf.user.userId = :userId and uf.deviceId = :deviceId and uf.active = true")
    Optional<String> findFcmTokenByUserIdAndDeviceId(Long userId, String deviceId);
}
