package com.smartlib.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TranslatorTest {
    private Translator t;
    @BeforeEach void init() throws Exception { t = new Translator(); t.load(); }

    @Test void translatesKnownTextAndLeavesUnknown() {
        String out = t.html("<html lang=\"en\"><body><h2>Seats</h2><p>Totally unknown sentence</p></body></html>");
        assertTrue(out.contains("<html lang=\"mr\""));
        assertTrue(out.contains("आसने"));
        assertTrue(out.contains("Totally unknown sentence"));
    }
    @Test void neverTouchesFormValuesOrScripts() {
        String out = t.html("<option value=\"PENDING\">PENDING</option><script>var s='>Seats<';</script>");
        assertTrue(out.contains("value=\"PENDING\""));
        assertTrue(out.contains(">Seats<';"));
    }
    @Test void patternsAndDates() {
        assertTrue(t.html("<b>Valid until 12 Dec 2026 (30 days left)</b>").contains("डिसें"));
        assertTrue(t.html("<b>2h 5m</b>").contains("तास"));
    }
    @Test void translatedOutputIsEscaped() {
        // translated text is HTML-escaped, so a dictionary entry can never inject markup
        assertFalse(Translator.escape("<b>").contains("<"));
    }
}
