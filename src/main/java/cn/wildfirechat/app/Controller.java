package cn.wildfirechat.app;

import cn.wildfirechat.app.pojo.LoginRequest;
import cn.wildfirechat.app.pojo.SendCodeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
