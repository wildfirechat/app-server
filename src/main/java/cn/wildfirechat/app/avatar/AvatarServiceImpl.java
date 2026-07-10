package cn.wildfirechat.app.avatar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
public class AvatarServiceImpl implements AvatarService {
    private static final Logger LOG = LoggerFactory.getLogger(AvatarServiceImpl.class);

    private static final String DEFAULT_AVATAR_PATH = "/static/avatar/avatar_def.png";

    @Value("${avatar.bg.corlors}")
    String bgColors;

    public static final String AVATAR_DIR = "./avatar";

    @PostConstruct
    public void init() {
        File avatarDir = new File(AvatarServiceImpl.AVATAR_DIR);
        if (!avatarDir.exists()) {
            avatarDir.mkdirs();
        }
    }

    @Override
    public ResponseEntity<byte[]> avatar(String name) throws IOException {
        File file = nameAvatar(name);
        if (file != null && file.exists()) {
            try (InputStream is = Files.newInputStream(file.toPath())) {
                byte[] bytes = StreamUtils.copyToByteArray(is);
                return ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header("Cache-Control", "max-age=604800")
                    .body(bytes);
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Override
    public CompletableFuture<ResponseEntity<byte[]>> groupAvatar(GroupAvatarRequest request) {
        List<GroupAvatarRequest.GroupMemberInfo> infos = request.getMembers();
        List<URL> paths = new ArrayList<>();
        long hashCode = 0;
        URL defaultAvatarUrl = getClass().getResource(DEFAULT_AVATAR_PATH);
        for (int i = 0; i < infos.size() && i < 9; i++) {
            GroupAvatarRequest.GroupMemberInfo info = infos.get(i);
            URL url = null;
            long urlHashCode = 0;
            if (!StringUtils.isEmpty(info.getAvatarUrl())) {
                try {
                    url = new URL(info.getAvatarUrl());
                    urlHashCode = info.getAvatarUrl().hashCode();
                } catch (MalformedURLException e) {
                    // 头像链接非法，使用默认头像
                    url = defaultAvatarUrl;
                    urlHashCode = DEFAULT_AVATAR_PATH.hashCode();
                }
            } else {
                String memberName = info.getName();
                if (StringUtils.isEmpty(memberName)) {
                    // 没有头像且没有名字，使用默认头像
                    url = defaultAvatarUrl;
                    urlHashCode = DEFAULT_AVATAR_PATH.hashCode();
                } else {
                    File file = nameAvatar(memberName);
                    if (file != null && file.exists()) {
                        try {
                            url = file.toURI().toURL();
                            urlHashCode = memberName.hashCode();
                        } catch (MalformedURLException ignored) {
                        }
                    }
                }
            }
            if (url != null) {
                paths.add(url);
                hashCode += urlHashCode;
            }
        }
        File file = new File(AVATAR_DIR, hashCode + "-group.png");
        if (!file.exists()) {
            return CompletableFuture.supplyAsync(new Supplier<ResponseEntity<byte[]>>() {
                @Override
                public ResponseEntity<byte[]> get() {
                    InputStream inputStream = null;
                    try {
                        GroupAvatarUtil.getCombinationOfHead(paths, file);
                        if (file.exists()) {
                            inputStream = Files.newInputStream(file.toPath());
                            byte[] bytes = StreamUtils.copyToByteArray(inputStream);
                            return ResponseEntity.ok()
                                .contentType(MediaType.IMAGE_PNG)
                                .header("Cache-Control", "max-age=604800")
                                .body(bytes);
                        } else {
                            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                        }
                    } catch (IOException | URISyntaxException e) {
                        LOG.error("generate group avatar error", e);
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                // do nothing
                            }
                        }
                    }
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }

            });
        } else {
            try (
                InputStream inputStream = Files.newInputStream(file.toPath())
            ) {
                byte[] bytes = StreamUtils.copyToByteArray(inputStream);
                return CompletableFuture.completedFuture(ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header("Cache-Control", "max-age=604800")
                    .body(bytes));
            } catch (IOException e) {
                LOG.error("read cached group avatar error", e);
            }
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    private File nameAvatar(String name) {
        String displayName = name;
        if (StringUtils.isEmpty(displayName)) {
            displayName = "?";
        }
        String[] colors = bgColors.split(",");
        int len = colors.length;
        int hashCode = displayName.hashCode();
        File file = new File(AVATAR_DIR, hashCode + ".png");
        if (!file.exists()) {
            String color = colors[Math.abs(displayName.hashCode() % len)];
            // 最后一个字符
            String lastChar = displayName.substring(displayName.length() - 1).toUpperCase();
            file = new NameAvatarBuilder(color).name(lastChar, displayName).build();
        }
        return file;
    }
}
