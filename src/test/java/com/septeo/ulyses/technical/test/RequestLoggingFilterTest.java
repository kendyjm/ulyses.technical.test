package com.septeo.ulyses.technical.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.septeo.ulyses.technical.test.filter.RequestLoggingFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.sql.init.mode=never")
class RequestLoggingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    private ListAppender<ILoggingEvent> listAppender;
    private ch.qos.logback.classic.Logger filterLogger;

    @BeforeEach
    void attachAppender() {
        filterLogger = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(RequestLoggingFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        filterLogger.addAppender(listAppender);
    }

    @AfterEach
    void detachAppender() {
        filterLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void anyRequest_logsOneEntry() throws Exception {
        mockMvc.perform(get("/api/brands")).andExpect(status().isOk());

        assertThat(listAppender.list).hasSize(1);
    }

    @Test
    void logEntry_containsAllFiveFieldsInOrder() throws Exception {
        mockMvc.perform(get("/api/brands")).andExpect(status().isOk());

        String message = listAppender.list.get(0).getFormattedMessage();
        // Full-line shape: <datetime> <METHOD> <url> <status> <duration>ms — anchored so a
        // stray substring (e.g. "200" inside "200ms" or a path) cannot satisfy the assertion.
        assertThat(message).matches(
                "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)? GET /api/brands 200 \\d+ms");
    }

    @Test
    void errorStatusFromController_isLoggedCorrectly() throws Exception {
        // BrandController returns 404 via ResponseEntity.notFound() (a normal return, not a
        // thrown exception), so the status is logged accurately and exactly once.
        mockMvc.perform(get("/api/brands/999999")).andExpect(status().isNotFound());

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getFormattedMessage()).contains(" 404 ");
    }

    @Test
    void uncaughtException_isLoggedAs500_exactlyOnce() {
        // GET /api/boom is permitAll (under /api/**) and throws — exercising the catch(Throwable)
        // path. MockMvc surfaces the exception after the filter has already logged in finally.
        try {
            mockMvc.perform(get("/api/boom"));
        } catch (Exception expected) {
            // MockMvc re-throws the unresolved controller exception; the filter has logged by now.
        }

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getFormattedMessage()).contains(" 500 ");
    }

    @Test
    void unauthenticatedRequest_blockedBySecurity_isNotLogged() throws Exception {
        // POST /api/brands requires authentication (SecurityConfig.anyRequest().authenticated()).
        // Spring Security's filter chain (order -100) runs before this unordered @Component filter
        // (LOWEST_PRECEDENCE) and short-circuits with 401, so the rejected request never reaches
        // RequestLoggingFilter. This documents the ACTUAL ordering behaviour.
        mockMvc.perform(post("/api/brands")).andExpect(status().isUnauthorized());

        assertThat(listAppender.list).isEmpty();
    }

    // Test-only controller that always throws, used to exercise the catch(Throwable) path.
    // Nested inside a @TestConfiguration so Spring registers it (and only it) as a handler,
    // scoped to this test. GET /api/boom is permitAll under SecurityConfig's "/api/**" rule.
    @TestConfiguration
    static class BoomConfig {
        @RestController
        static class BoomController {
            @GetMapping("/api/boom")
            public String boom() {
                throw new IllegalStateException("boom");
            }
        }
    }
}
