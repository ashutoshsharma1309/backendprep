package com.example.library.grading;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GRADING TEST — Phase 6 (Business Logic & Reliability), idempotency module.
 *
 * Verifies: submitting the same borrow request twice with the same
 * Idempotency-Key returns the SAME loan, not a duplicate or a conflict.
 * See phase-6-business-logic-reliability/README.md Module 4.
 */
@Tag("grading")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Phase6IdempotencyGradingTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void borrowingSameBookTwiceWithSameIdempotencyKey_returnsSameLoan() throws Exception {
        String body = "{\"bookId\":1,\"patronEmail\":\"patron@example.com\"}";
        String key = "test-key-123";

        MvcResult first = mockMvc.perform(post("/loans")
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andReturn();

        MvcResult second = mockMvc.perform(post("/loans")
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andReturn();

        String firstBody = first.getResponse().getContentAsString();
        String secondBody = second.getResponse().getContentAsString();
        assertEquals(firstBody, secondBody, "Retried request with same Idempotency-Key must return the identical loan, not create a duplicate");
    }
}
