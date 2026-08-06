package com.example.payment_service.infrastructure.initializer;

import com.example.payment_service.domain.user.User;
import com.example.payment_service.domain.user.UserRepository;
import com.example.payment_service.domain.user.UserRole;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.email}")
    private String adminEmail;

    @PostConstruct
    public void init() {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            userRepository.save(new User(
                    UUID.randomUUID(),
                    adminUsername,
                    passwordEncoder.encode(adminPassword),
                    adminEmail,
                    UserRole.ADMIN
            ));
        }
    }
}
