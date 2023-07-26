package cn.wildfirechat.app.avatar;

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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
public class AvatarServiceImpl implements AvatarService {
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
            byte[] bytes = StreamUtils.copyToByteArray(Files.newInputStream(file.toPath()));
            return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Override
    public CompletableFuture<ResponseEntity<byte[]>> groupAvatar(GroupAvatarRequest request) throws IOException, URISyntaxException {
        List<GroupAvatarRequest.GroupMemberInfo> infos = request.getMembers();
        List<URL> paths = new ArrayList<>();
        long hashCode = 0;
        for (int i = 0; i < infos.size() && i < 9; i++) {
            GroupAvatarRequest.GroupMemberInfo info = infos.get(i);
            if (!StringUtils.isEmpty(info.getAvatarUrl())) {
                paths.add(new URL(info.getAvatarUrl()));
                hashCode += info.getAvatarUrl().hashCode();
            } else {
                File file = nameAvatar(info.getName());
                if (file != null && file.exists()) {
                    paths.add(file.toURI().toURL());
                    hashCode += info.getName().hashCode();
                }
            }
        }
        File file = new File(AVATAR_DIR, hashCode + "-group.png");
        if (!file.exists()) {
            return CompletableFuture.supplyAsync(new Supplier<ResponseEntity<byte[]>>() {
                @Override
                public ResponseEntity<byte[]> get() {
                    try {
                        GroupAvatarUtil.getCombinationOfHead(paths, file);
                        if (file.exists()) {
                            byte[] bytes = StreamUtils.copyToByteArray(Files.newInputStream(file.toPath()));
                            return ResponseEntity.ok()
                                .contentType(MediaType.IMAGE_PNG)
                                .body(bytes);
                        } else {
                            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                        }
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }

            });
        } else {
            byte[] bytes = StreamUtils.copyToByteArray(Files.newInputStream(file.toPath()));
            return CompletableFuture.completedFuture(ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes));
        }
    }

    private File nameAvatar(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        String[] colors = bgColors.split(",");
        int len = colors.length;
        int hashCode = name.hashCode();
        File file = new File(AVATAR_DIR, hashCode + ".png");
        if (!file.exists()) {
            String color = colors[Math.abs(name.hashCode() % len)];
            // 最后一个字符
            String lastChar = name.substring(name.length() - 1).toUpperCase();
            file = new NameAvatarBuilder(color).name(lastChar, name).build();
        }
        return file;
    }
}
