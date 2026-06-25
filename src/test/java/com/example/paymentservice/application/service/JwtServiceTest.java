package com.example.paymentservice.application.service;

import com.example.paymentservice.domain.user.User;
import com.example.paymentservice.application.service.util.TestServiceDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private String token;

    @BeforeEach
    void setUp() throws Exception{
        jwtService = new JwtService();

        Field secretField = JwtService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtService, TestServiceDataFactory.SECRET_KEY);

        User user = TestServiceDataFactory.createUser();

        token = jwtService.generateToken(user);
    }

    @Test
    void should_generate_token() {
        assertThat(token).isNotBlank();
    }

    @Test
    void should_extract_username_from_token() {
        assertThat(jwtService.extractUsername(token)).isEqualTo(TestServiceDataFactory.TEST_USERNAME);
    }

    @Test
    void should_validate_token() {
        assertThat(jwtService.isTokenValid(token, TestServiceDataFactory.TEST_USERNAME)).isTrue();
    }

    @Test
    void should_not_validate_token_for_wrong_name() {
        assertThat(jwtService.isTokenValid(token, "wrong-name")).isFalse();
    }
}