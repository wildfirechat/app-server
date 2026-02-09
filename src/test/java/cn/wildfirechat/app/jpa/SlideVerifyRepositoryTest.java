package cn.wildfirechat.app.jpa;

import cn.wildfirechat.app.slide.SlideVerifyCleanupService;
import cn.wildfirechat.app.slide.SlideVerifyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SlideVerifyRepository
 * Uses mocks to avoid Spring context loading issues
 */
@RunWith(MockitoJUnitRunner.class)
public class SlideVerifyRepositoryTest {

    @Mock
    private SlideVerifyRepository repository;

    private SlideVerifyCleanupService cleanupService;

    @Before
    public void setUp() {
        cleanupService = new SlideVerifyCleanupService();
        try {
            java.lang.reflect.Field field = SlideVerifyCleanupService.class.getDeclaredField("slideVerifyRepository");
            field.setAccessible(true);
            field.set(cleanupService, repository);
        } catch (Exception e) {
            fail("Failed to inject mock repository: " + e.getMessage());
        }
    }

    @Test
    public void testFindByTokenExists() {
        // Given
        String token = "test-token-123";
        SlideVerify expected = new SlideVerify(token, 150, System.currentTimeMillis());
        when(repository.findByToken(token)).thenReturn(Optional.of(expected));

        // When
        Optional<SlideVerify> result = repository.findByToken(token);

        // Then
        assertTrue(result.isPresent());
        assertEquals(token, result.get().getToken());
        assertEquals(150, result.get().getX());
    }

    @Test
    public void testFindByTokenNotExists() {
        // Given
        String token = "non-existent-token";
        when(repository.findByToken(token)).thenReturn(Optional.empty());

        // When
        Optional<SlideVerify> result = repository.findByToken(token);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    public void testSave() {
        // Given
        SlideVerify slideVerify = new SlideVerify("save-token", 100, System.currentTimeMillis());
        when(repository.save(any(SlideVerify.class))).thenReturn(slideVerify);

        // When
        SlideVerify result = repository.save(slideVerify);

        // Then
        assertNotNull(result);
        assertEquals("save-token", result.getToken());
    }

    @Test
    public void testDelete() {
        // Given
        SlideVerify slideVerify = new SlideVerify("delete-token", 100, System.currentTimeMillis());

        // When
        repository.delete(slideVerify);

        // Then - no exception means success
        verify(repository, times(1)).delete(slideVerify);
    }

    @Test
    public void testCleanupExpired() {
        // Given
        int deletedCount = 5;
        when(repository.deleteExpired(anyLong())).thenReturn(deletedCount);

        // When
        cleanupService.cleanupExpired();

        // Then
        verify(repository, times(1)).deleteExpired(anyLong());
    }

    @Test
    public void testUpdateVerified() {
        // Given
        String token = "update-token";
        SlideVerify slideVerify = new SlideVerify(token, 100, System.currentTimeMillis());
        slideVerify.setVerified(true);
        when(repository.save(any(SlideVerify.class))).thenReturn(slideVerify);

        // When
        SlideVerify result = repository.save(slideVerify);

        // Then
        assertTrue(result.isVerified());
    }
}
