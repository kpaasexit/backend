package com.exit.user.domain;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_fcm_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "user_fcm_token_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "user_fcm_token_updated_at"))
public class UserFcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_fcm_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "token", length = 255)
    private String token;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "active")
    private Boolean active;

    @Builder
    public UserFcmToken(Users user, String token, String deviceId, Boolean active) {
        this.user = user;
        this.token = token;
        this.deviceId = deviceId;
        this.active = active != null ? active : true;
    }
}
