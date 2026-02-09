package cn.wildfirechat.app.slide;

import cn.wildfirechat.app.jpa.SlideVerify;
import cn.wildfirechat.app.jpa.SlideVerifyRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SlideVerifyCleanupServiceTest {

    @Mock
    private SlideVerifyRepository slideVerifyRepository;

    private SlideVerifyCleanupService cleanupService;

    @Before
    public void setUp() {
        cleanupService = new SlideVerifyCleanupService();
        try {
            java.lang.reflect.Field field = SlideVerifyCleanupService.class.getDeclaredField("slideVerifyRepository");
            field.setAccessible(true);
            field.set(cleanupService, slideVerifyRepository);
        } catch (Exception e) {
            fail("Failed to inject mock repository: " + e.getMessage());
        }
    }

    @Test
    public void testCleanupExpired() {
        // Given
        int expectedDeleted = 5;
        when(slideVerifyRepository.deleteExpired(anyLong())).thenReturn(expectedDeleted);

        // When
        cleanupService.cleanupExpired();

        // Then
        verify(slideVerifyRepository, times(1)).deleteExpired(anyLong());
    }

    @Test
    public void testCleanupExpiredNoData() {
        // Given
        when(slideVerifyRepository.deleteExpired(anyLong())).thenReturn(0);

        // When
        cleanupService.cleanupExpired();

        // Then
        verify(slideVerifyRepository, times(1)).deleteExpired(anyLong());
    }
}
