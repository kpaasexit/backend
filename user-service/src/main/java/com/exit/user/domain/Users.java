package com.exit.user.domain;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table
@AttributeOverride(name = "createdAt", column = @Column(name = "user_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "user_updated_at"))
public class Users extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 20, nullable = false)
    private String userName;

    @Column(name = "user_nickname", length = 20, nullable = false)
    private String userNickname;

    @Column(name = "user_phone_number", length = 20)
    private String userPhoneNumber;

    @Column(name = "user_email", length = 50)
    private String userEmail;

    @Column(name = "user_password")
    private String userPassword;

    @Column(name = "user_profile_url", length = 512)
    private String userProfileUrl;

    @Column(name = "user_report_count")
    private Short userReportCount;

    @Builder
    public Users(String userName, String userNickname, String userPassword, String userPhoneNumber, String userEmail, String userProfileUrl, Short userReportCount) {
        this.userName = userName;
        this.userNickname = userNickname;
        this.userPassword = userPassword;
        this.userPhoneNumber = userPhoneNumber;
        this.userEmail = userEmail;
        this.userProfileUrl = userProfileUrl;
        this.userReportCount = userReportCount;
    }
}