package cn.wildfirechat.app.ptt;

import cn.wildfirechat.app.pojo.PttChannelInfo;
import cn.wildfirechat.app.pojo.PttChannelInfoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class PttController {
    private static final Logger LOG = LoggerFactory.getLogger(PttController.class);
    @Autowired
    private PttService mService;

    @CrossOrigin
    @PostMapping(value = "/ptt/get_id/{userId}")
    public Object getUserChannelId(@PathVariable("userId") String userId) throws IOException {
        return mService.getUserChannelId(userId);
    }

    @CrossOrigin
    @PostMapping(value = "/ptt/get_my_id")
    public Object getMyChannelId() throws IOException {
        return mService.getMyChannelId();
    }

    @CrossOrigin
    @PostMapping(value = "/ptt/info")
    public Object getChannelInfo(@RequestBody PttChannelInfoRequest request) throws IOException {
        return mService.getChannelInfo(request.channelId, request.password);
    }

    @CrossOrigin
    @PostMapping(value = "/ptt/put_info")
    public Object putChannelInfo(@RequestBody PttChannelInfo info) throws IOException {
        return mService.putChannelInfo(info);
    }

    @CrossOrigin
    @PostMapping(value = "/ptt/create")
    public Object createChannel(@RequestBody PttChannelInfo info) throws IOException {
        return mService.createChannel(info);
    }

    @CrossOrigin
    @PostMapping(value = "/ptt/destroy/{ChannelId}")
    public Object destroyChannel(@PathVariable("ChannelId") String ChannelId) throws IOException {
        return mService.destroyChannel(ChannelId);
    }

    @CrossOrigin
    @PostMapping(value = "/ptt/fav/{ChannelId}")
    public Object favChannel(@PathVariable("ChannelId") String ChannelId) throws IOException {
        return mService.favChannel(ChannelId);
    }

    @CrossOrigin
    @PostMapping(value = "/ptt/unfav/{ChannelId}")
    public Object unfavChannel(@PathVariable("ChannelId") String ChannelId) throws IOException {
        return mService.unfavChannel(ChannelId);
    }

    @CrossOrigin
    @PostMapping(value = "/ptt/is_fav/{ChannelId}")
    public Object isFavChannel(@PathVariable("ChannelId") String ChannelId) throws IOException {
        return mService.isFavChannel(ChannelId);
    }

    @CrossOrigin
    @PostMapping(value = "/ptt/fav_Channels")
    public Object getFavChannels() throws IOException {
        return mService.getFavChannels();
    }
}
