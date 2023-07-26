package cn.wildfirechat.app.avatar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(value = "/avatar")
public class AvatarController {
    @Autowired
    AvatarService avatarService;

    @CrossOrigin
    @GetMapping()
    public ResponseEntity<byte[]> avatar(@RequestParam("name") String name) throws IOException {
        return avatarService.avatar(name);
    }

    @GetMapping("/group")
    public CompletableFuture<ResponseEntity<byte[]>> groupAvatar(@RequestParam("request") String request) throws IOException, URISyntaxException {
        ObjectMapper mapper = new ObjectMapper();
        GroupAvatarRequest groupAvatarRequest = mapper.readValue(request, GroupAvatarRequest.class);
        return avatarService.groupAvatar(groupAvatarRequest);
    }
}
