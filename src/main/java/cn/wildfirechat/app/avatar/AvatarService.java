package cn.wildfirechat.app.avatar;

import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.URISyntaxException;

public interface AvatarService {
    ResponseEntity<byte[]> avatar(String name) throws IOException;

    ResponseEntity<byte[]> groupAvatar(GroupAvatarRequest requeset) throws IOException, URISyntaxException;
}
