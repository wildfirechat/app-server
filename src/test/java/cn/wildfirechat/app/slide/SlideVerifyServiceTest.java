package cn.wildfirechat.app.slide;

import cn.wildfirechat.app.jpa.SlideVerify;
import cn.wildfirechat.app.jpa.SlideVerifyRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SlideVerifyServiceTest {

    @Mock
    private SlideVerifyRepository slideVerifyRepository;

    private SlideVerifyService slideVerifyService;

    @Before
    public void setUp() {
        slideVerifyService = new SlideVerifyService();
        // 通过反射注入 mock repository
        try {
            java.lang.reflect.Field field = SlideVerifyService.class.getDeclaredField("slideVerifyRepository");
            field.setAccessible(true);
            field.set(slideVerifyService, slideVerifyRepository);
        } catch (Exception e) {
            fail("Failed to inject mock repository: " + e.getMessage());
        }
    }

    @Test
    public void testGenerateSlideVerify() {
        // When
        Map<String, Object> result = slideVerifyService.generateSlideVerify();

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("token"));
        assertTrue(result.containsKey("backgroundImage"));
        assertTrue(result.containsKey("sliderImage"));
        assertTrue(result.containsKey("y"));

        String token = (String) result.get("token");
        assertNotNull(token);
        assertFalse(token.isEmpty());

        int y = (int) result.get("y");
        assertTrue(y >= 20 && y <= 100); // 20 + random(0, 80)

        // Verify repository save was called
        verify(slideVerifyRepository, times(1)).save(any(SlideVerify.class));
    }

    @Test
    public void testVerifySlideSuccess() {
        // Given
        String token = "test-token-success";
        int correctX = 150;
        int userX = 155; // within tolerance

        SlideVerify slideVerify = new SlideVerify(token, correctX, System.currentTimeMillis());
        when(slideVerifyRepository.findByToken(token)).thenReturn(Optional.of(slideVerify));

        // When
        boolean result = slideVerifyService.verifySlide(token, userX);

        // Then
        assertTrue(result);
        verify(slideVerifyRepository, times(1)).save(any(SlideVerify.class));
    }

    @Test
    public void testVerifySlideFailure() {
        // Given
        String token = "test-token-failure";
        int correctX = 150;
        int userX = 100; // outside tolerance

        SlideVerify slideVerify = new SlideVerify(token, correctX, System.currentTimeMillis());
        when(slideVerifyRepository.findByToken(token)).thenReturn(Optional.of(slideVerify));

        // When
        boolean result = slideVerifyService.verifySlide(token, userX);

        // Then
        assertFalse(result);
        verify(slideVerifyRepository, times(1)).delete(any(SlideVerify.class));
    }

    @Test
    public void testVerifySlideTokenNotFound() {
        // Given
        String token = "non-existent-token";
        when(slideVerifyRepository.findByToken(token)).thenReturn(Optional.empty());

        // When
        boolean result = slideVerifyService.verifySlide(token, 100);

        // Then
        assertFalse(result);
        verify(slideVerifyRepository, never()).save(any());
        verify(slideVerifyRepository, never()).delete(any());
    }

    @Test
    public void testVerifySlideAlreadyVerified() {
        // Given
        String token = "already-verified-token";
        SlideVerify slideVerify = new SlideVerify(token, 150, System.currentTimeMillis());
        slideVerify.setVerified(true); // Already verified

        when(slideVerifyRepository.findByToken(token)).thenReturn(Optional.of(slideVerify));

        // When
        boolean result = slideVerifyService.verifySlide(token, 150);

        // Then
        assertFalse(result);
        verify(slideVerifyRepository, never()).save(any());
        verify(slideVerifyRepository, never()).delete(any());
    }

    @Test
    public void testIsVerifiedWhenVerified() {
        // Given
        String token = "verified-token";
        SlideVerify slideVerify = new SlideVerify(token, 150, System.currentTimeMillis());
        slideVerify.setVerified(true);

        when(slideVerifyRepository.findByToken(token)).thenReturn(Optional.of(slideVerify));

        // When
        boolean result = slideVerifyService.isVerified(token);

        // Then
        assertTrue(result);
        verify(slideVerifyRepository, times(1)).delete(any(SlideVerify.class));
    }

    @Test
    public void testIsVerifiedWhenNotVerified() {
        // Given
        String token = "not-verified-token";
        SlideVerify slideVerify = new SlideVerify(token, 150, System.currentTimeMillis());
        slideVerify.setVerified(false);

        when(slideVerifyRepository.findByToken(token)).thenReturn(Optional.of(slideVerify));

        // When
        boolean result = slideVerifyService.isVerified(token);

        // Then
        assertFalse(result);
        verify(slideVerifyRepository, never()).delete(any());
    }

    @Test
    public void testIsVerifiedWhenNotFound() {
        // Given
        String token = "non-existent-token";
        when(slideVerifyRepository.findByToken(token)).thenReturn(Optional.empty());

        // When
        boolean result = slideVerifyService.isVerified(token);

        // Then
        assertFalse(result);
    }

    @Test
    public void testIsVerifiedWhenExpired() {
        // Given
        String token = "expired-token";
        SlideVerify slideVerify = new SlideVerify(token, 150, System.currentTimeMillis() - 400000); // 400秒前
        slideVerify.setVerified(false);

        when(slideVerifyRepository.findByToken(token)).thenReturn(Optional.of(slideVerify));

        // When
        boolean result = slideVerifyService.isVerified(token);

        // Then
        assertFalse(result);
    }
}
