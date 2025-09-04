package com.exit.user.domain;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table
@AttributeOverride(name = "createdAt", column = @Column(name = "notification_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "notification_updated_at"))
public class Notifications extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private Users user;

    @Column(name = "notification_title", length = 100)
    private String notificationTitle;

    @Column(name = "notification_content", length = 255)
    private String notificationContent;
}
