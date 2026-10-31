package com.example.jaimemaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class InputValidatorTest {

    @Test
    public void acceptsNormalNames() {
        assertEquals("Casa", InputValidator.sanitizeTitle("Casa"));
        assertEquals("Plaza 2", InputValidator.sanitizeTitle("  Plaza   2 "));
        assertEquals("Ñandú, sector-1", InputValidator.sanitizeTitle("Ñandú, sector-1"));
    }

    @Test
    public void rejectsSqlInjectionPayloads() {
        assertNull(InputValidator.sanitizeTitle("'; DROP TABLE markers;--"));
        assertNull(InputValidator.sanitizeTitle("' OR '1'='1"));
        assertNull(InputValidator.sanitizeTitle("a\" OR 1=1 --"));
    }

    @Test
    public void rejectsMarkupAndInvalidInput() {
        assertNull(InputValidator.sanitizeTitle("<script>alert(1)</script>"));
        assertNull(InputValidator.sanitizeTitle(""));
        assertNull(InputValidator.sanitizeTitle("   "));
        assertNull(InputValidator.sanitizeTitle(null));
    }

    @Test
    public void rejectsTooLongInput() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < InputValidator.MAX_TITLE_LENGTH + 1; i++) sb.append('a');
        assertNull(InputValidator.sanitizeTitle(sb.toString()));
    }
}
