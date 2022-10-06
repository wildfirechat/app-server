package cn.wildfirechat.app.conference;

import cn.wildfirechat.app.Service;
import cn.wildfirechat.app.pojo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class ConferenceController {
    private static final Logger LOG = LoggerFactory.getLogger(ConferenceController.class);
    @Autowired
    private ConferenceService mService;

    @CrossOrigin
    @PostMapping(value = "/conference/get_id/{userId}")
    public Object getUserConferenceId(@PathVariable("userId") String userId) throws IOException {
        return mService.getUserConferenceId(userId);
    }

    @CrossOrigin
    @PostMapping(value = "/conference/get_my_id")
    public Object getMyConferenceId() throws IOException {
        return mService.getMyConferenceId();
    }

    @CrossOrigin
    @PostMapping(value = "/conference/info")
    public Object getConferenceInfo(@RequestBody ConferenceInfoRequest request) throws IOException {
        return mService.getConferenceInfo(request.conferenceId, request.password);
    }

    @CrossOrigin
    @PostMapping(value = "/conference/put_info")
    public Object putConferenceInfo(@RequestBody ConferenceInfo info) throws IOException {
        return mService.putConferenceInfo(info);
    }

    @CrossOrigin
    @PostMapping(value = "/conference/create")
    public Object createConference(@RequestBody ConferenceInfo info) throws IOException {
        return mService.createConference(info);
    }

    @CrossOrigin
    @PostMapping(value = "/conference/destroy/{conferenceId}")
    public Object destroyConference(@PathVariable("conferenceId") String conferenceId) throws IOException {
        return mService.destroyConference(conferenceId);
    }

    @CrossOrigin
    @PostMapping(value = "/conference/recording/{conferenceId}")
    public Object recordingConference(@PathVariable("conferenceId") String conferenceId, @RequestBody RecordingRequest recordingRequest) throws IOException {
        return mService.recordingConference(conferenceId, recordingRequest.recording);
    }

    @CrossOrigin
    @PostMapping(value = "/conference/fav/{conferenceId}")
    public Object favConference(@PathVariable("conferenceId") String conferenceId) throws IOException {
        return mService.favConference(conferenceId);
    }

    @CrossOrigin
    @PostMapping(value = "/conference/unfav/{conferenceId}")
    public Object unfavConference(@PathVariable("conferenceId") String conferenceId) throws IOException {
        return mService.unfavConference(conferenceId);
    }

    @CrossOrigin
    @PostMapping(value = "/conference/is_fav/{conferenceId}")
    public Object isFavConference(@PathVariable("conferenceId") String conferenceId) throws IOException {
        return mService.isFavConference(conferenceId);
    }

    @CrossOrigin
    @PostMapping(value = "/conference/fav_conferences")
    public Object getFavConferences() throws IOException {
        return mService.getFavConferences();
    }
}
