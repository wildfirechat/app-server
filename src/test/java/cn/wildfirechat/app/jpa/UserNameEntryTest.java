package cn.wildfirechat.app.jpa;

import org.junit.Test;

import static org.junit.Assert.*;

public class UserNameEntryTest {

    @Test
    public void testDefaultConstructor() {
        // When
        UserNameEntry entry = new UserNameEntry();

        // Then
        assertNull(entry.getId());
    }

    @Test
    public void testSetAndGetId() {
        // Given
        UserNameEntry entry = new UserNameEntry();

        // When
        entry.setId(1);

        // Then
        assertEquals(Integer.valueOf(1), entry.getId());
    }

    @Test
    public void testDifferentIds() {
        // Given
        UserNameEntry entry = new UserNameEntry();

        // When - assign different IDs
        for (int i = 0; i < 100; i++) {
            entry.setId(i);
            // Then
            assertEquals(Integer.valueOf(i), entry.getId());
        }
    }
}
