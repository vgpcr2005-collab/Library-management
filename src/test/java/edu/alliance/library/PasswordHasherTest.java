package edu.alliance.library;

import edu.alliance.library.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    @Test
    void hashesPasswordsWithIndependentSaltsAndVerifiesCredentials() {
        char[] password = "a-correct-horse-battery".toCharArray();
        String firstHash = PasswordHasher.hash(password);
        String secondHash = PasswordHasher.hash(password);

        assertNotEquals(firstHash, secondHash);
        assertTrue(PasswordHasher.verify(password, firstHash));
        assertFalse(PasswordHasher.verify("different-password".toCharArray(), firstHash));
        assertFalse(PasswordHasher.verify(password, "not-a-password-hash"));
    }
}
