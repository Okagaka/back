package com.okagaka.OkaGaka.common.security;

import com.okagaka.OkaGaka.domain.user.entity.User;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

import lombok.Getter;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String phoneNumber;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.username = user.getName();
        this.phoneNumber = user.getPhoneNumber();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }
    @Override
    public String getPassword() { return null; }  // 비밀번호가 없으므로 null 처리

    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }


}
