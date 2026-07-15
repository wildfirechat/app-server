package cn.wildfirechat.app.tools;

import org.junit.Test;

import static org.junit.Assert.*;

public class MediaFileTypeValidatorTest {

    @Test
    public void testImageAllowed() {
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(1, "photo.jpg"));
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(1, "photo.JPEG"));
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(1, "image.png"));
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(7, "sticker.gif"));
    }

    @Test
    public void testImageRejected() {
        assertFalse(MediaFileTypeValidator.isAllowedMediaFile(1, "document.pdf"));
        assertFalse(MediaFileTypeValidator.isAllowedMediaFile(1, "script.exe"));
        assertFalse(MediaFileTypeValidator.isAllowedMediaFile(1, "no_extension"));
    }

    @Test
    public void testVoiceAllowed() {
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(2, "voice.amr"));
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(2, "voice.mp3"));
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(2, "voice.M4A"));
    }

    @Test
    public void testVideoAllowed() {
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(3, "video.mp4"));
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(8, "moment.mov"));
    }

    @Test
    public void testGeneralFileAllowed() {
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(0, "document.pdf"));
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(4, "archive.zip"));
        assertTrue(MediaFileTypeValidator.isAllowedMediaFile(6, "favorite.txt"));
    }

    @Test
    public void testDangerousExtensionRejected() {
        assertFalse(MediaFileTypeValidator.isAllowedMediaFile(0, "virus.exe"));
        assertFalse(MediaFileTypeValidator.isAllowedMediaFile(4, "run.sh"));
        assertFalse(MediaFileTypeValidator.isAllowedMediaFile(6, "page.html"));
        assertFalse(MediaFileTypeValidator.isAllowedMediaFile(0, "app.py"));
    }

    @Test
    public void testEmptyOrInvalidFileName() {
        assertFalse(MediaFileTypeValidator.isAllowedMediaFile(1, null));
        assertFalse(MediaFileTypeValidator.isAllowedMediaFile(1, ""));
        assertFalse(MediaFileTypeValidator.isAllowedMediaFile(1, "noext"));
        assertFalse(MediaFileTypeValidator.isAllowedMediaFile(1, "trailing."));
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("jpg", MediaFileTypeValidator.getFileExtension("photo.jpg"));
        assertEquals("png", MediaFileTypeValidator.getFileExtension("/path/to/image.png"));
        assertEquals("", MediaFileTypeValidator.getFileExtension("no_extension"));
        assertEquals("", MediaFileTypeValidator.getFileExtension(""));
        assertEquals("", MediaFileTypeValidator.getFileExtension(null));
    }
}
