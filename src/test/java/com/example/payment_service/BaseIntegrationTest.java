package com.example.payment_service;

import com.example.payment_service.application.service.JwtService;
import com.example.payment_service.application.service.util.TestServiceDataFactory;
import com.example.payment_service.domain.user.User;
import com.example.payment_service.domain.user.UserRepository;
import com.example.payment_service.domain.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    protected static final String ADMIN_USERNAME = "admin-test";
    protected static final String ADMIN_PASSWORD = "admin-test-password";
    protected static final String ADMIN_EMAIL = "admin-test@example.com";

    protected static final String STRIPE_SECRET_KEY = "sk_test_dummy_key_for_integration_tests";
    protected static final String STRIPE_WEBHOOK_SECRET = "whsec_test_secret_for_integration_tests";

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("admin.username", () -> ADMIN_USERNAME);
        registry.add("admin.password", () -> ADMIN_PASSWORD);
        registry.add("admin.email", () -> ADMIN_EMAIL);
        registry.add("jwt.secret", () -> TestServiceDataFactory.SECRET_KEY);
        registry.add("stripe.secret-key", () -> STRIPE_SECRET_KEY);
        registry.add("stripe.webhook-secret", () -> STRIPE_WEBHOOK_SECRET);
    }

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    protected record AuthenticatedUser(User user, String token) {}

    protected AuthenticatedUser registerUserAndAuthenticate() {
        return registerUserAndAuthenticate(UserRole.MERCHANT);
    }

    protected AuthenticatedUser registerUserAndAuthenticate(UserRole role) {
        UUID userId = UUID.randomUUID();

        String username = "test-user-" + userId;
        User user = new User(
                userId,
                username,
                passwordEncoder.encode("test-password"),
                username + "@example.com",
                role
        );
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return new AuthenticatedUser(user, token);
    }

    protected String authenticateAsAdmin() {
        User admin = userRepository.findByUsername(ADMIN_USERNAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Admin user not found - AdminInitializer should have created it on startup"
                ));
        return jwtService.generateToken(admin);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}