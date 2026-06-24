package com.example.paymentservice.application.service;

import com.example.paymentservice.domain.user.User;
import com.example.paymentservice.domain.user.UserRepository;
import com.example.paymentservice.domain.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public String register(String username, String password, String email) {
        User user = new User(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode(password),
                email,
                UserRole.MERCHANT
        );

        repository.save(user);
        return jwtService.generateToken(user);
    }

    public String login(String username, String password) {
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Username not found: " + username));

        if (!passwordEncoder.matches(password, user.password()))
            throw new RuntimeException("Incorrect password");

        return  jwtService.generateToken(user);
    }
}
