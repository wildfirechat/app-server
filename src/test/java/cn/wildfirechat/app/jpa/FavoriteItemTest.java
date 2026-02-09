package cn.wildfirechat.app.jpa;

import org.junit.Test;

import static org.junit.Assert.*;

public class FavoriteItemTest {

    @Test
    public void testDefaultConstructor() {
        // When
        FavoriteItem item = new FavoriteItem();

        // Then
        assertNull(item.id);
        assertNull(item.messageUid);
        assertNull(item.userId);
        assertEquals(0, item.type);
        assertEquals(0, item.timestamp);
        assertNull(item.title);
        assertNull(item.url);
    }

    @Test
    public void testSettersAndGetters() {
        // Given
        FavoriteItem item = new FavoriteItem();

        // When
        item.id = 1L;
        item.messageUid = 12345L;
        item.userId = "user123";
        item.type = 1;
        item.timestamp = System.currentTimeMillis();
        item.convType = 0;
        item.convLine = 0;
        item.convTarget = "target456";
        item.origin = "original data";
        item.sender = "sender789";
        item.title = "Favorite Title";
        item.url = "https://example.com/image.png";
        item.thumbUrl = "https://example.com/thumb.png";
        item.data = "{\"key\":\"value\"}";

        // Then
        assertEquals(Long.valueOf(1L), item.id);
        assertEquals(Long.valueOf(12345L), item.messageUid);
        assertEquals("user123", item.userId);
        assertEquals(1, item.type);
        assertTrue(item.timestamp > 0);
        assertEquals(0, item.convType);
        assertEquals("target456", item.convTarget);
        assertEquals("original data", item.origin);
        assertEquals("sender789", item.sender);
        assertEquals("Favorite Title", item.title);
        assertEquals("https://example.com/image.png", item.url);
        assertEquals("https://example.com/thumb.png", item.thumbUrl);
        assertEquals("{\"key\":\"value\"}", item.data);
    }

    @Test
    public void testDifferentTypes() {
        // Given
        FavoriteItem item = new FavoriteItem();

        // When - test different favorite types
        for (int type = 0; type < 10; type++) {
            item.type = type;
            // Then
            assertEquals(type, item.type);
        }
    }

    @Test
    public void testLongUrls() {
        // Given
        FavoriteItem item = new FavoriteItem();
        String longUrl = "https://example.com/very/long/path/to/image.png?param1=value1&param2=value2";

        // When
        item.url = longUrl;

        // Then
        assertEquals(longUrl, item.url);
    }
}
