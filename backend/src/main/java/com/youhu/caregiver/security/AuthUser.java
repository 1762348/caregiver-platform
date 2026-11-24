package com.youhu.caregiver.security;

import com.youhu.caregiver.user.domain.UserRole;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 当前登录用户（由 JWT 解析得到），作为 Spring Security 的 principal。
 */
public record AuthUser(Long id, String phone, UserRole role, List<String> permissions) {

    public List<GrantedAuthority> authorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        if (permissions != null) {
            permissions.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
        }
        return authorities;
    }

    public boolean hasRole(UserRole expected) {
        return role == expected;
    }
}
