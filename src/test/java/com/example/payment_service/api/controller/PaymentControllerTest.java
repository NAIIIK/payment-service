package com.example.payment_service.api.controller;

import com.example.payment_service.BaseIntegrationTest;
import com.example.payment_service.application.service.PspClient;
import com.example.payment_service.application.service.dto.PspPaymentResult;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static com.example.payment_service.api.controller.util.TestControllerDataFactory.PAYMENT_INTENT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends BaseIntegrationTest {

    private static final String URI_TEMPLATE = "/api/v1/payments";
    private static final MediaType CONTENT_TYPE = MediaType.APPLICATION_JSON;
    private static final String VALID_CONTENT = """
                            {
                                "recipientId": "b2ec1587-aeb1-551f-bf1d-69101cb8bd1d",
                                "amount": 100.00,
                                "currency": "USD"
                            }
                            """;
    private static final String CLIENT_SECRET = "secret_test_123";

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final MockMvc mockMvc;

    @MockitoBean
    private PspClient pspClient;

    private String idempotencyKey;
    private String userToken;

    PaymentControllerTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
        idempotencyKey = UUID.randomUUID().toString();
        userToken = registerUserAndAuthenticate().token();
        when(pspClient.createPayment(any()))
                .thenReturn(new PspPaymentResult(PAYMENT_INTENT_ID, CLIENT_SECRET));
    }

    @Test
    void should_create_payment() throws Exception {
        performPostWithHeader(VALID_CONTENT, idempotencyKey, userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.clientSecret").value(CLIENT_SECRET))
                .andExpect(jsonPath("$.stripePaymentIntentId").value(PAYMENT_INTENT_ID));
    }

    @Test
    void should_return_401_when_unauthenticated() throws Exception {
        mockMvc.perform(post(URI_TEMPLATE)
                        .contentType(CONTENT_TYPE)
                        .content(VALID_CONTENT)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_400_when_amount_is_negative() throws Exception {
        String invalidContent = """
                            {
                                "recipientId": "b2ec1587-aeb1-551f-bf1d-69101cb8bd1d",
                                "amount": -100.00,
                                "currency": "USD"
                            }
                            """;

        performPostWithHeader(invalidContent, idempotencyKey, userToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void should_return_404_when_payment_not_found() throws Exception {
        mockMvc.perform(get(URI_TEMPLATE + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_same_response_for_same_idempotency_key() throws Exception {
        String firstResponse = performPostWithHeader(VALID_CONTENT, idempotencyKey, userToken)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = performPostWithHeader(VALID_CONTENT, idempotencyKey, userToken)
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
                        .content(VALID_CONTENT)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing required header"));
    }

    @Test
    void should_change_status_to_completed() throws Exception {
        String id = createPendingPaymentId();
        String adminToken = authenticateAsAdmin();

        mockMvc.perform(patch(URI_TEMPLATE + "/" + id + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void should_change_status_to_failed() throws Exception {
        String id = createPendingPaymentId();
        String adminToken = authenticateAsAdmin();

        mockMvc.perform(patch(URI_TEMPLATE + "/" + id + "/fail")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void should_return_409_when_status_transition_is_invalid() throws Exception {
        String id =  createPendingPaymentId();
        String adminToken = authenticateAsAdmin();

        mockMvc.perform(patch(URI_TEMPLATE + "/" + id + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(patch(URI_TEMPLATE + "/" + id + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void should_return_403_when_completing_without_admin_role() throws Exception {
        String id = createPendingPaymentId();

        mockMvc.perform(patch(URI_TEMPLATE + "/" + id + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    private ResultActions performPostWithHeader(String content, String idempotencyKey, String token) throws Exception {
        return mockMvc.perform(post(URI_TEMPLATE)
                .contentType(CONTENT_TYPE)
                .content(content)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private String createPendingPaymentId() throws Exception {
        String content = performPostWithHeader(VALID_CONTENT, idempotencyKey, userToken)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(content, "$.id");
    }
}