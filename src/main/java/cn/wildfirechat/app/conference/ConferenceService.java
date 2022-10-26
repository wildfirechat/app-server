package cn.wildfirechat.app.conference;


import cn.wildfirechat.app.RestResult;
import cn.wildfirechat.app.jpa.FavoriteItem;
import cn.wildfirechat.app.pojo.*;
import cn.wildfirechat.pojos.InputCreateDevice;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

public interface ConferenceService {
    RestResult getUserConferenceId(String userId);
    RestResult getMyConferenceId();
    RestResult getConferenceInfo(String conferenceId, String password);
    RestResult putConferenceInfo(ConferenceInfo info);
    RestResult createConference(ConferenceInfo info);
    RestResult destroyConference(String conferenceId);
    RestResult recordingConference(String conferenceId, boolean recording);
    RestResult focusConference(String conferenceId, String userId);
    RestResult favConference(String conferenceId);
    RestResult unfavConference(String conferenceId);
    RestResult getFavConferences();
    RestResult isFavConference(String conferenceId);
}
