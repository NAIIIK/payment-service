package com.example.paymentservice.application.service.util;

import com.example.paymentservice.domain.user.User;
import com.example.paymentservice.domain.user.UserRole;

import java.util.UUID;

public final class TestServiceDataFactory {

    private TestServiceDataFactory() {}

    public static final String TEST_USERNAME = "test-username";
    public static final String HASHED_TEST_PASSWORD = "hashed-test-password";
    public static final String RAW_TEST_PASSWORD = "test-password";
    public static final String TEST_EMAIL = "test-email@example.com";
    public static final String TEST_JWT_TOKEN = "test-jwt-token";

    public static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    public static User createUser() {
        return new User(
                UUID.randomUUID(),
                TEST_USERNAME,
                HASHED_TEST_PASSWORD,
                TEST_EMAIL,
                UserRole.MERCHANT
        );
    }
}
