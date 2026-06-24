package com.example.paymentservice.api;

import com.example.paymentservice.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends BaseIntegrationTest {

    private static final String URI_TEMPLATE = "/api/v1/payments";
    private static final MediaType CONTENT_TYPE = MediaType.APPLICATION_JSON;
    private static final String VALID_CONTENT = """
                            {
                                "senderId": 1,
                                "recipientId": 2,
                                "amount": 100.00,
                                "currency": "USD"
                            }
                            """;

    private final MockMvc mockMvc;

    PaymentControllerTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void should_create_payment() throws Exception {
        performPostWithHeader(VALID_CONTENT, UUID.randomUUID().toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void should_return_400_when_amount_is_negative() throws Exception {
        String invalidContent = """
                            {
                                "senderId": 1,
                                "recipientId": 2,
                                "amount": -100.00,
                                "currency": "USD"
                            }
                            """;

        performPostWithHeader(invalidContent, UUID.randomUUID().toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void should_return_404_when_payment_not_found() throws Exception {
        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_same_response_for_same_idempotency_key() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        String firstResponse = performPostWithHeader(VALID_CONTENT, idempotencyKey)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = performPostWithHeader(VALID_CONTENT, idempotencyKey)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(firstResponse).isEqualTo(secondResponse);
    }

    @Test
    void should_return_400_when_header_is_missing() throws Exception {
        mockMvc.perform(post(URI_TEMPLATE)
                        .contentType(CONTENT_TYPE)
                        .content(VALID_CONTENT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing required header"));
    }

    private ResultActions performPostWithHeader(String content, String idempotencyKey) throws Exception {
        return mockMvc.perform(post(URI_TEMPLATE)
                .contentType(CONTENT_TYPE)
                .content(content)
                .header("Idempotency-Key", idempotencyKey));
    }
}