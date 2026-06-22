package com.example.paymentservice.api;

import com.example.paymentservice.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PaymentControllerTest extends BaseIntegrationTest {

    private final MockMvc mockMvc;

    PaymentControllerTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void should_create_payment() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content("""
                            {
                                "senderId": 1,
                                "recipientId": 2,
                                "amount": 100.00,
                                "currency": "USD"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void should_return_400_when_amount_is_negative() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content("""
                            {
                                "senderId": 1,
                                "recipientId": 2,
                                "amount": -100.00,
                                "currency": "USD"
                            }
                            """))
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

        String requestBody = """
                {
                    "senderId": 1,
                    "recipientId": 2,
                    "amount": 100.00,
                    "currency": "USD"
                }
                """;

        String firstResponse = mockMvc.perform(post("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", idempotencyKey)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(firstResponse).isEqualTo(secondResponse);
    }

    @Test
    void should_return_400_when_header_is_missing() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "senderId": 1,
                                "recipientId": 2,
                                "amount": 100.00,
                                "currency": "USD"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing required header"));
    }
}