package com.okagaka.OkaGaka.domain.carrequest.dto;

import com.okagaka.OkaGaka.domain.user.entity.User;

public record CarpoolMember(
        Long userId,
        String userName
) {
    public static CarpoolMember from(User user) {
        return new CarpoolMember(user.getId(), user.getName());
    }
}
