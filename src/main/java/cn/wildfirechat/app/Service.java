package cn.wildfirechat.app;


import cn.wildfirechat.app.jpa.FavoriteItem;
import cn.wildfirechat.app.pojo.*;
import cn.wildfirechat.pojos.InputCreateDevice;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

public interface Service {
    RestResult sendLoginCode(String mobile);
    RestResult sendLoginCode(String mobile, String slideVerifyToken);
    RestResult sendResetCode(String mobile, String slideVerifyToken);
    RestResult loginWithMobileCode(HttpServletResponse response, String mobile, String code, String clientId, int platform, String slideVerifyToken);
    RestResult loginWithPassword(HttpServletResponse response, String mobile, String password, String clientId, int platform, String slideVerifyToken);
    RestResult changePassword(String oldPwd, String newPwd, String slideVerifyToken);
    RestResult resetPassword(String mobile, String resetCode, String newPwd);
    RestResult sendDestroyCode();
    RestResult sendDestroyCode(String slideVerifyToken);
    RestResult destroy(HttpServletResponse response, String code);

    RestResult createPcSession(CreateSessionRequest request);
    RestResult loginWithSession(String token);

    RestResult scanPc(String token);
    RestResult confirmPc(ConfirmSessionRequest request);
    RestResult cancelPc(CancelSessionRequest request);

    RestResult changeName(String newName);
    RestResult complain(String text);

    RestResult putGroupAnnouncement(GroupAnnouncementPojo request);
    RestResult getGroupAnnouncement(String groupId);

    RestResult saveUserLogs(String userId, MultipartFile file);

    RestResult generateSlideVerify();
    RestResult verifySlide(String token, int x);

    RestResult addDevice(InputCreateDevice createDevice);
    RestResult getDeviceList();
    RestResult delDevice(InputCreateDevice createDevice);

    RestResult sendUserMessage(SendMessageRequest request);
    RestResult uploadMedia(int mediaType, MultipartFile file);

    RestResult putFavoriteItem(FavoriteItem request);
    RestResult removeFavoriteItems(long id);
    RestResult getFavoriteItems(long id, int count);
    RestResult getGroupMembersForPortrait(String groupId);
}
