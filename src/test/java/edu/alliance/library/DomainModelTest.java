package edu.alliance.library;

import edu.alliance.library.model.Book;
import edu.alliance.library.model.LibraryItem;
import edu.alliance.library.model.ReferenceBook;
import edu.alliance.library.model.Student;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {
    @Test
    void studentInheritsSharedUserDetails() {
        Student student = new Student(4, "student@example.edu", "Test Student", 8,
                "AU-8", "Engineering", "9000000000");

        assertEquals("STUDENT", student.getRole());
        assertEquals("Test Student", student.getFullName());
        assertEquals("AU-8", student.getStudentId());
    }

    @Test
    void libraryItemDispatchesReferenceDetailsAtRuntime() {
        LibraryItem item = new ReferenceBook(3, "9780199535569", "Dictionary", "Oxford", "Reference", 2, 2);

        assertTrue(item.getDetails().contains("library use only"));
    }

    @Test
    void bookRejectsInvalidAvailabilityAndMaintainsEncapsulation() {
        assertThrows(IllegalArgumentException.class,
                () -> new Book(1, "9780132350884", "Clean Code", "Robert Martin", "Technology", 2, 3, false));
        Book book = new Book(1, "9780132350884", "Clean Code", "Robert Martin", "Technology", 3, 2, false);
        assertThrows(IllegalArgumentException.class, () -> book.setCopyCounts(1, 2));
        assertEquals(2, book.getAvailableCopies());
    }
}
