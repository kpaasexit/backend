package com.exit.gateway.controller.user.dto.request.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdditionalUserInfoRequestDto{
    String nickname;
    Boolean isProfileImageDeleted;
}
