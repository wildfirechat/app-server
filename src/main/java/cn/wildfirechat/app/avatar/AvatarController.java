package cn.wildfirechat.app.avatar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjsse.net.ssl.OpenJSSE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;

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
    public ResponseEntity<byte[]> groupAvatar(@RequestParam("request") String request) throws IOException, URISyntaxException {
        ObjectMapper mapper = new ObjectMapper();
        GroupAvatarRequest groupAvatarRequest = mapper.readValue(request, GroupAvatarRequest.class);
        return avatarService.groupAvatar(groupAvatarRequest);
    }
}
