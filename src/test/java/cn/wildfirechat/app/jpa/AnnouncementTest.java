package cn.wildfirechat.app.jpa;

import org.junit.Test;

import static org.junit.Assert.*;

public class AnnouncementTest {

    @Test
    public void testConstructorAndGetters() {
        // Given
        String groupId = "group123";
        String author = "user456";
        String content = "Test announcement content";
        long timestamp = System.currentTimeMillis();

        // When
        Announcement announcement = new Announcement();
        announcement.setGroupId(groupId);
        announcement.setAuthor(author);
        announcement.setAnnouncement(content);
        announcement.setTimestamp(timestamp);

        // Then
        assertEquals(groupId, announcement.getGroupId());
        assertEquals(author, announcement.getAuthor());
        assertEquals(content, announcement.getAnnouncement());
        assertEquals(timestamp, announcement.getTimestamp());
    }

    @Test
    public void testEmptyAnnouncement() {
        // When
        Announcement announcement = new Announcement();

        // Then
        assertNull(announcement.getGroupId());
        assertNull(announcement.getAuthor());
        assertNull(announcement.getAnnouncement());
        assertEquals(0L, announcement.getTimestamp());
    }

    @Test
    public void testLongContent() {
        // Given
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longContent.append("This is a long announcement content. ");
        }

        // When
        Announcement announcement = new Announcement();
        announcement.setAnnouncement(longContent.toString());

        // Then
        assertEquals(longContent.toString(), announcement.getAnnouncement());
    }
}
