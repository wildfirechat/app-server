package cn.wildfirechat.app.ptt;


import cn.wildfirechat.app.RestResult;
import cn.wildfirechat.app.pojo.PttChannelInfo;

public interface PttService {
    RestResult getUserChannelId(String userId);
    RestResult getMyChannelId();
    RestResult getChannelInfo(String ChannelId, String password);
    RestResult putChannelInfo(PttChannelInfo info);
    RestResult createChannel(PttChannelInfo info);
    RestResult destroyChannel(String ChannelId);
    RestResult favChannel(String ChannelId);
    RestResult unfavChannel(String ChannelId);
    RestResult getFavChannels();
    RestResult isFavChannel(String ChannelId);
}
