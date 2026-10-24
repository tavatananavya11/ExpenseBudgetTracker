package com.example.expensebudget.security;

import com.example.expensebudget.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class TrackerUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public TrackerUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(TrackerUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found."));
    }
}
