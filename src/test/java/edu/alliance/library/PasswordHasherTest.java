package edu.alliance.library;

import edu.alliance.library.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    @Test
    void usesSaltedHashesAndVerifiesPasswords() {
        char[] password = "correct-horse-battery-staple".toCharArray();
        String first = PasswordHasher.hash(password);
        String second = PasswordHasher.hash(password);

        assertNotEquals(first, second);
        assertTrue(PasswordHasher.verify(password, first));
        assertFalse(PasswordHasher.verify("incorrect-password".toCharArray(), first));
        assertFalse(PasswordHasher.verify(password, "not-a-supported-hash"));
        assertFalse(first.contains(new String(password)));
    }
}
