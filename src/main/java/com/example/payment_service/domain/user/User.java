package com.example.payment_service.domain.user;

import java.util.UUID;

public record User (
    UUID id,
    String username,
    String password,
    String email,
    UserRole role
) {

    @Override
    public String toString() {
        return "User[" +
                "id=" + id +
                ", username=" + username +
                ", email=" + email +
                ", role=" + role +
                "]";
    }
}
