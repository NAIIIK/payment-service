package com.example.paymentservice.application.service;

import com.example.paymentservice.domain.exception.IncorrectPasswordException;
import com.example.paymentservice.domain.exception.UserNotFoundException;
import com.example.paymentservice.domain.user.User;
import com.example.paymentservice.domain.user.UserRepository;
import com.example.paymentservice.application.service.util.TestServiceDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestServiceDataFactory.createUser();
    }

    @Test
    void should_register_user_and_return_token() {
        when(passwordEncoder.encode(TestServiceDataFactory.RAW_TEST_PASSWORD)).thenReturn(TestServiceDataFactory.HASHED_TEST_PASSWORD);
        when(jwtService.generateToken(any(User.class))).thenReturn(TestServiceDataFactory.TEST_JWT_TOKEN);

        String token = authService.register(
                TestServiceDataFactory.TEST_USERNAME,
                TestServiceDataFactory.RAW_TEST_PASSWORD,
                TestServiceDataFactory.TEST_EMAIL
        );

        assertThat(token).isEqualTo(TestServiceDataFactory.TEST_JWT_TOKEN);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void should_login_and_return_token() {
        when(userRepository.findByUsername(TestServiceDataFactory.TEST_USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TestServiceDataFactory.RAW_TEST_PASSWORD, TestServiceDataFactory.HASHED_TEST_PASSWORD)).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn(TestServiceDataFactory.TEST_JWT_TOKEN);

        String token = authService.login(TestServiceDataFactory.TEST_USERNAME, TestServiceDataFactory.RAW_TEST_PASSWORD);

        assertThat(token).isEqualTo(TestServiceDataFactory.TEST_JWT_TOKEN);
    }

    @Test
    void should_throw_when_user_not_found() {
        when(userRepository.findByUsername(TestServiceDataFactory.TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(TestServiceDataFactory.TEST_USERNAME, TestServiceDataFactory.RAW_TEST_PASSWORD))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void should_throw_when_password_is_incorrect() {
        when(userRepository.findByUsername(TestServiceDataFactory.TEST_USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TestServiceDataFactory.RAW_TEST_PASSWORD, TestServiceDataFactory.HASHED_TEST_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(TestServiceDataFactory.TEST_USERNAME, TestServiceDataFactory.RAW_TEST_PASSWORD))
                .isInstanceOf(IncorrectPasswordException.class);
    }
}