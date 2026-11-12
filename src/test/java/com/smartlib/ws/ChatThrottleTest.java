package com.smartlib.ws;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Security: chat flooding. */
class ChatThrottleTest {
    private final RealtimeHandler h = new RealtimeHandler(null, null, null, null);

    @Test void blocksAfter20In10Seconds() {
        for (int i = 0; i < 20; i++) assertTrue(h.allow(1L, 1000 + i));
        assertFalse(h.allow(1L, 1100));
        assertTrue(h.allow(2L, 1100), "other users are unaffected");
        assertTrue(h.allow(1L, 20_000), "window slides");
    }
}
