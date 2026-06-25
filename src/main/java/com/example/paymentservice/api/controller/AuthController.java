package com.example.paymentservice.api.controller;

import com.example.paymentservice.api.dto.AuthResponse;
import com.example.paymentservice.api.dto.LoginRequest;
import com.example.paymentservice.api.dto.RegisterRequest;
import com.example.paymentservice.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody @Valid RegisterRequest request) {
        String token = authService.register(
                request.username(),
                request.password(),
                request.email()
        );

        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid LoginRequest request) {
        String token = authService.login(
                request.username(),
                request.password()
        );

        return new AuthResponse(token);
    }
}
