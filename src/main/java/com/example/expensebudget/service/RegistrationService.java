package com.example.expensebudget.service;

import com.example.expensebudget.model.RegisterRequest;
import com.example.expensebudget.model.UserProfile;
import com.example.expensebudget.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class RegistrationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserProfile register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must use no more than 72 UTF-8 bytes.");
        }
        String passwordHash = passwordEncoder.encode(request.password());
        if (!userRepository.insert(normalizedEmail, passwordHash)) {
            throw new EmailAlreadyRegisteredException();
        }
        return new UserProfile(normalizedEmail);
    }
}
