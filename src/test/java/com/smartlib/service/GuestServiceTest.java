package com.smartlib.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuestServiceTest {
    @Test void clampTrimsAndCuts() {
        assertNull(GuestService.clamp(null, 10));
        assertEquals("abc", GuestService.clamp("  abc  ", 10));
        assertEquals(5, GuestService.clamp("x".repeat(500), 5).length());
    }
}
