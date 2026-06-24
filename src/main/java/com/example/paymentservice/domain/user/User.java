package com.example.paymentservice.domain.user;

import java.util.UUID;

public record User (
    UUID id,
    String username,
    String password,
    String email,
    UserRole role
) {}
