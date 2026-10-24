package com.example.expensebudget.controller;

import com.example.expensebudget.model.CsrfTokenResponse;
import com.example.expensebudget.model.LoginRequest;
import com.example.expensebudget.model.RegisterRequest;
import com.example.expensebudget.model.UserProfile;
import com.example.expensebudget.security.CurrentUserService;
import com.example.expensebudget.security.TrackerUserPrincipal;
import com.example.expensebudget.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
public class AuthController {
    private final RegistrationService registrationService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CookieCsrfTokenRepository csrfTokenRepository;
    private final CurrentUserService currentUserService;

    public AuthController(RegistrationService registrationService,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          CookieCsrfTokenRepository csrfTokenRepository,
                          CurrentUserService currentUserService) {
        this.registrationService = registrationService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/auth/csrf")
    public ResponseEntity<CsrfTokenResponse> csrfToken(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<UserProfile> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<UserProfile> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest servletRequest,
                                             HttpServletResponse servletResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email().trim().toLowerCase(Locale.ROOT), request.password()));
        } catch (AuthenticationException exception) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);

        TrackerUserPrincipal principal = (TrackerUserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new UserProfile(principal.email()));
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<UserProfile> currentUser() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new UserProfile(currentUserService.requiredEmail()));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        csrfTokenRepository.saveToken(null, request, response);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
