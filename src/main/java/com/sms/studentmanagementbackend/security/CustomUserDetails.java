package com.sms.studentmanagementbackend.security;

import com.sms.studentmanagementbackend.entity.AppUser;
import com.sms.studentmanagementbackend.entity.Role;
import com.sms.studentmanagementbackend.entity.enums.UserRole;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean active;
    private final UserRole role;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(AppUser user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.active = user.isActive();
        this.role = user.getRole();
        this.authorities = user.getRoles().stream()
                .map(Role::getName)
                .map(UserRole::name)
                .<GrantedAuthority>map(name -> new SimpleGrantedAuthority("ROLE_" + name))
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
