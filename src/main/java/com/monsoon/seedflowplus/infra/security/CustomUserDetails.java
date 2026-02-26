package com.monsoon.seedflowplus.infra.security;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String loginId;
    private final String password;
    private final Role role;
    private final boolean enabled;
    private final Long employeeId;
    private final Long clientId;

    public CustomUserDetails(User user) {
        Objects.requireNonNull(user, "user must not be null");
        this.userId = user.getId();
        this.loginId = user.getLoginId();
        this.password = user.getLoginPw();
        this.role = Objects.requireNonNull(user.getRole(), "role must not be null");
        this.enabled = user.getStatus() == com.monsoon.seedflowplus.domain.account.entity.Status.ACTIVATE;
        this.employeeId = user.getEmployee() != null ? user.getEmployee().getId() : null;
        this.clientId = user.getClient() != null ? user.getClient().getId() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
