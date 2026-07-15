package cn.wildfirechat.app.tools;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 媒体文件类型校验工具。
 * 根据 mediaType 校验文件扩展名是否符合预期。
 */
public class MediaFileTypeValidator {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif"
    ));

    private static final Set<String> VOICE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "amr", "mp3", "aac", "m4a", "wav", "ogg"
    ));

    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "mov", "avi", "mkv", "flv", "3gp", "webm"
    ));

    private static final Set<String> DANGEROUS_EXTENSIONS = new HashSet<>(Arrays.asList(
            "exe", "bat", "cmd", "sh", "bash", "csh", "zsh",
            "js", "jsx", "ts", "html", "htm", "xhtml", "php", "jsp", "asp", "aspx",
            "py", "pyc", "pyo", "rb", "pl", "lua", "vbs", "ps1", "psm1",
            "jar", "war", "ear", "dll", "so", "dylib", "msi", "scr", "com"
    ));

    /**
     * 判断文件是否符合指定 mediaType 的类型要求。
     *
     * @param mediaType   媒体类型，参考 ProtoConstants.MediaType
     * @param fileName    原始文件名
     * @return true 表示允许上传
     */
    public static boolean isAllowedMediaFile(int mediaType, String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return false;
        }
        String ext = getFileExtension(fileName).toLowerCase();
        if (ext.isEmpty()) {
            return false;
        }
        switch (mediaType) {
            case 1: // IMAGE
            case 7: // STICKER
                return IMAGE_EXTENSIONS.contains(ext);
            case 2: // VOICE
                return VOICE_EXTENSIONS.contains(ext);
            case 3: // VIDEO
            case 8: // MOMENTS
                return VIDEO_EXTENSIONS.contains(ext);
            case 0: // GENERAL
            case 4: // FILE
            case 6: // FAVORITE
            default:
                // 通用文件：禁止可执行/脚本等危险后缀
                return !DANGEROUS_EXTENSIONS.contains(ext);
        }
    }

    /**
     * 提取文件扩展名（不含点号）。
     */
    public static String getFileExtension(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }
}
