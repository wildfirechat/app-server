package cn.wildfirechat.app;

import cn.wildfirechat.app.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class Controller {
    @Autowired
    private Service mService;

    @GetMapping()
    public Object health() {
        return "Ok";
    }

    @PostMapping(value = "/send_code", produces = "application/json;charset=UTF-8")
    public Object sendCode(@RequestBody SendCodeRequest request) {
        return mService.sendCode(request.getMobile());
    }

    @PostMapping(value = "/login", produces = "application/json;charset=UTF-8")
    public Object login(@RequestBody LoginRequest request) {
        return mService.login(request.getMobile(), request.getCode(), request.getClientId(), request.getPlatform() == null ? 0 : request.getPlatform());
    }


    /* PC扫码操作
    1, PC -> App     创建会话
    2, PC -> App     轮询调用session_login进行登陆，如果已经扫码确认返回token，否则反正错误码9（已经扫码还没确认)或10(还没有被扫码)。
     */
    @CrossOrigin
    @PostMapping(value = "/pc_session", produces = "application/json;charset=UTF-8")
    public Object createPcSession(@RequestBody CreateSessionRequest request) {
        return mService.createPcSession(request);
    }

    @CrossOrigin
    @PostMapping(value = "/session_login/{token}", produces = "application/json;charset=UTF-8")
    public Object loginWithSession(@PathVariable("token") String token) {
        return mService.loginWithSession(token);
    }

    /* 手机扫码操作
    1，扫码，调用/scan_pc接口。
    2，调用/confirm_pc 接口进行确认
     */
    @PostMapping(value = "/scan_pc/{token}", produces = "application/json;charset=UTF-8")
    public Object scanPc(@PathVariable("token") String token) {
        return mService.scanPc(token);
    }

    @PostMapping(value = "/confirm_pc", produces = "application/json;charset=UTF-8")
    public Object confirmPc(@RequestBody ConfirmSessionRequest request) {
        return mService.confirmPc(request);
    }

    @PostMapping(value = "/put_group_announcement", produces = "application/json;charset=UTF-8")
    public Object putGroupAnnouncement(@RequestBody GroupAnnouncementPojo request) {
        return mService.putGroupAnnouncement(request);
    }

    @PostMapping(value = "/get_group_announcement", produces = "application/json;charset=UTF-8")
    public Object getGroupAnnouncement(@RequestBody GroupIdPojo request) {
        return mService.getGroupAnnouncement(request.groupId);
    }

    @PostMapping(value = "/user/online_event")
    public Object onUserOnlineEvent(@RequestBody String request) {
        return "hello";
    }

    @PostMapping(value = "/logs/{userId}/upload")
    public Object uploadFiles(@RequestParam("file") MultipartFile file, @PathVariable("userId") String userId) throws IOException {
        return mService.saveUserLogs(userId, file);
    }
}
