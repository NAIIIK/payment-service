package com.example.payment_service.application.service;

import com.example.payment_service.domain.exception.IncorrectPasswordException;
import com.example.payment_service.domain.exception.UserNotFoundException;
import com.example.payment_service.domain.user.User;
import com.example.payment_service.domain.user.UserRepository;
import com.example.payment_service.domain.user.UserRole;
import com.example.payment_service.infrastructure.logging.Sensitive;
import com.example.payment_service.infrastructure.logging.SensitiveResult;
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

    @SensitiveResult
    public String register(String username, @Sensitive String password, String email) {
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

    @SensitiveResult
    public String login(String username, @Sensitive String password) {
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        if (!passwordEncoder.matches(password, user.password()))
            throw new IncorrectPasswordException();

        return  jwtService.generateToken(user);
    }
}
