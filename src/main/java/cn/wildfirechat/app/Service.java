package cn.wildfirechat.app;


import cn.wildfirechat.app.pojo.CancelSessionRequest;
import cn.wildfirechat.app.pojo.ConfirmSessionRequest;
import cn.wildfirechat.app.pojo.CreateSessionRequest;
import cn.wildfirechat.app.pojo.GroupAnnouncementPojo;
import cn.wildfirechat.pojos.InputCreateDevice;
import org.springframework.web.multipart.MultipartFile;

public interface Service {
    RestResult sendCode(String mobile);
    RestResult login(String mobile, String code, String clientId, int platform);


    RestResult createPcSession(CreateSessionRequest request);
    RestResult loginWithSession(String token);

    RestResult scanPc(String token);
    RestResult confirmPc(ConfirmSessionRequest request);
    RestResult cancelPc(CancelSessionRequest request);

    RestResult changeName(String newName);

    RestResult putGroupAnnouncement(GroupAnnouncementPojo request);
    RestResult getGroupAnnouncement(String groupId);

    RestResult saveUserLogs(String userId, MultipartFile file);

    RestResult addDevice(InputCreateDevice createDevice);
    RestResult getDeviceList();
    RestResult delDevice(InputCreateDevice createDevice);
}
