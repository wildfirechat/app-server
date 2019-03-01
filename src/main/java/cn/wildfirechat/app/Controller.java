package cn.wildfirechat.app;

import cn.wildfirechat.app.pojo.ConfirmSessionRequest;
import cn.wildfirechat.app.pojo.LoginRequest;
import cn.wildfirechat.app.pojo.CreateSessionRequest;
import cn.wildfirechat.app.pojo.SendCodeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class Controller {
    @Autowired
    private Service mService;

    @PostMapping(value = "/send_code", produces = "application/json;charset=UTF-8"   )
    public Object sendCode(@RequestBody SendCodeRequest request) {
        return mService.sendCode(request.getMobile());
    }

    @PostMapping(value = "/login", produces = "application/json;charset=UTF-8"   )
    public Object login(@RequestBody LoginRequest request) {
        return mService.login(request.getMobile(), request.getCode(), request.getClientId());
    }


    /* PC扫码操作
    1, PC -> App     创建会话
    2, PC -> App     轮询会话，检查状态 0 新建状态；1 已扫码；2 已确认
    3, PC -> App     如果状态变为已确认，调用session_login进行登陆。
     */
    @PostMapping(value = "/pc_session", produces = "application/json;charset=UTF-8"   )
    public Object createPcSession(@RequestBody CreateSessionRequest request) {
        return mService.createPcSession(request);
    }

    @GetMapping(value = "/pc_session/{token}", produces = "application/json;charset=UTF-8"   )
    public Object getPcSession(@PathVariable("token") String token) {
        return mService.getPcSession(token);
    }

    @PostMapping(value = "/session_login/{token}", produces = "application/json;charset=UTF-8"   )
    public Object loginWithSession(@PathVariable("token") String token) {
        return mService.loginWithSession(token);
    }

    /* 手机扫码操作
    1，扫码，调用/scan_pc接口。
    2，调用/confirm_pc 接口进行确认
     */
    @GetMapping(value = "/scan_pc/{token}", produces = "application/json;charset=UTF-8"   )
    public Object scanPc(@PathVariable("token") String token) {
        return mService.scanPc(token);
    }

    @PostMapping(value = "/confirm_pc", produces = "application/json;charset=UTF-8"   )
    public Object confirmPc(@RequestBody ConfirmSessionRequest request) {
        return mService.confirmPc(request);
    }
}
