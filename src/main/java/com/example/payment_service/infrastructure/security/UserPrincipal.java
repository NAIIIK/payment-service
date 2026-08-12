package com.example.payment_service.infrastructure.security;

import com.example.payment_service.domain.user.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.id();
        this.username = user.username();
        this.password = user.password();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
    }
}
