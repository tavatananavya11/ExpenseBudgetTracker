package com.example.expensebudget.security;

import com.example.expensebudget.model.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class TrackerUserPrincipal implements UserDetails {
    private final long id;
    private final String email;
    private final String passwordHash;

    public TrackerUserPrincipal(UserAccount account) {
        this.id = account.id();
        this.email = account.email();
        this.passwordHash = account.passwordHash();
    }

    public long id() {
        return id;
    }

    public String email() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
