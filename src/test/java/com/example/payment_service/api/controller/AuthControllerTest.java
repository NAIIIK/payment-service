package com.example.payment_service.api.controller;

import com.example.payment_service.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class AuthControllerTest extends BaseIntegrationTest {

    private static final String REGISTER_URI = "/api/v1/auth/register";
    private static final String LOGIN_URI = "/api/v1/auth/login";

    private static final String VALID_REGISTRATION_CONTENT =
                        """
                        {
                            "username": "%s",
                            "password": "test-password",
                            "email": "%s@example.com"
                        }
                        """;
    private static final String INVALID_REGISTRATION_CONTENT =
                    """
                    {
                        "username": "",
                        "password": "test-password",
                        "email": "not-an-email"
                    }
                    """;
    private static final String VALID_LOGIN_CONTENT =
                    """
                    {
                        "username": "%s",
                        "password": "test-password"
                    }
                    """;
    private static final String INCORRECT_PASSWORD_LOGIN_CONTENT =
                    """
                    {
                        "username": "%s",
                        "password": "wrong-password"
                    }
                    """;

    private String uniqueUsername;

    private final MockMvc mockMvc;

    AuthControllerTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
        uniqueUsername = "user-" + UUID.randomUUID();
    }

    @Test
    void should_return_token_after_successful_registration() throws Exception {
        performPostWithUniqueUsername(REGISTER_URI, VALID_REGISTRATION_CONTENT, uniqueUsername)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void should_return_token_after_successful_login() throws Exception {
        performPostWithUniqueUsername(REGISTER_URI, VALID_REGISTRATION_CONTENT, uniqueUsername)
                .andExpect(status().isOk());

        performPostWithUniqueUsername(LOGIN_URI, VALID_LOGIN_CONTENT, uniqueUsername)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void should_return_400_when_registration_data_is_invalid() throws Exception {
        performPostWithUniqueUsername(REGISTER_URI, INVALID_REGISTRATION_CONTENT, uniqueUsername)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void should_return_error_when_password_is_incorrect() throws Exception {
        performPostWithUniqueUsername(REGISTER_URI, VALID_REGISTRATION_CONTENT, uniqueUsername)
                .andExpect(status().isOk());

        performPostWithUniqueUsername(LOGIN_URI, INCORRECT_PASSWORD_LOGIN_CONTENT, uniqueUsername)
                .andExpect(status().isUnauthorized());
    }

    private ResultActions performPostWithUniqueUsername(String uri, String content, String uniqueUsername) throws Exception {
        return mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content.formatted(uniqueUsername, uniqueUsername)));
    }
}