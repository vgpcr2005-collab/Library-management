package edu.alliance.library;

import edu.alliance.library.util.InputValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {
    @Test
    void normalizesTrimmedSearchTextConsistently() {
        assertEquals("clean code", InputValidator.normalize("  CLEAN CODE  "));
    }

    @Test
    void validatesEmailAndIsbn() {
        assertTrue(InputValidator.validEmail("student@alliance.edu.in"));
        assertFalse(InputValidator.validEmail("not-an-email"));
        assertTrue(InputValidator.validIsbn("978-0-13-235088-4"));
        assertFalse(InputValidator.validIsbn("bad-isbn"));
    }
}
