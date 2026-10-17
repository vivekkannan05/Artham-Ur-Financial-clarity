package com.artha;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 *
 * This is the most basic test: if the context loads, it means:
 *   1. All @Bean definitions are valid
 *   2. All @Autowired dependencies are satisfied
 *   3. Database connection (H2 for tests) works
 *   4. No circular dependencies
 *
 * If THIS test fails, something is fundamentally broken.
 */
@SpringBootTest
@ActiveProfiles("test")
class ArthaApplicationTests {

    @Test
    void contextLoads() {
        // If we get here without exception, the application context is healthy
    }
}
