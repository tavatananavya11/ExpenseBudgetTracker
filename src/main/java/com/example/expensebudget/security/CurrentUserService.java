package com.example.expensebudget.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public long requiredUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof TrackerUserPrincipal principal)) {
            throw new IllegalStateException("An authenticated account is required.");
        }
        return principal.id();
    }

    public String requiredEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof TrackerUserPrincipal principal)) {
            throw new IllegalStateException("An authenticated account is required.");
        }
        return principal.email();
    }
}
