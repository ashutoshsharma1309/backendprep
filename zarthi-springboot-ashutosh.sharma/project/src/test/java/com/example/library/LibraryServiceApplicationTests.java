package com.example.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: confirms the Spring context loads. Keep this passing at all
 * times — a broken context load means something is misconfigured, and every
 * later phase's tests depend on this working first.
 */
@SpringBootTest
class LibraryServiceApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty — if the context fails to start, this test fails.
    }
}
