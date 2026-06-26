package com.jesuspartal.specforge.application.service;

import com.jesuspartal.specforge.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private final RateLimiterService service = new RateLimiterService();

    @BeforeEach
    void setUp() throws Exception {
        setField(service, "capacity", 5);
        setField(service, "refillTokens", 5);
        setField(service, "refillPeriodMinutes", 1);
    }

    @Test
    void shouldAllowConsumptionWithinLimit() {
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> service.checkAndConsume());
        }
    }

    @Test
    void shouldThrowWhenExceedingLimit() {
        for (int i = 0; i < 5; i++) service.checkAndConsume();
        assertThrows(RateLimitExceededException.class, () -> service.checkAndConsume());
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}