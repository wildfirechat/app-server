package cn.wildfirechat.app;

import cn.wildfirechat.pojos.*;
import cn.wildfirechat.pojos.moments.CommentPojo;
import cn.wildfirechat.pojos.moments.FeedPojo;
import cn.wildfirechat.pojos.moments.IdPojo;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
IM对应事件发生时，会回调到配置地址。需要注意IM服务单线程进行回调，如果接收方处理太慢会导致推送线程被阻塞，导致延迟发生，甚至导致IM系统异常。
建议异步处理快速返回，这里收到后转到异步线程处理，并且立即返回。另外两个服务器的ping值不能太大。
 */
@RestController()
public class IMCallbackController {
    private static final Logger LOG = LoggerFactory.getLogger(IMCallbackController.class);
    /*
    用户在线状态回调
     */
    @PostMapping(value = "/im_event/user/online")
    public Object onUserOnlineEvent(@RequestBody UserOnlineStatus event) {
        LOG.info("User:{} on device:{} online status:{}", event.userId, event.clientId, event.status);
        return "ok";
    }

    /*
    用户关系变更回调
     */
    @PostMapping(value = "/im_event/user/relation")
    public Object onUserRelationUpdated(@RequestBody RelationUpdateEvent event) {
        LOG.info("User relation updated:{}", event.userId);
        return "ok";
    }

    /*
    用户信息更新回调
     */
    @PostMapping(value = "/im_event/user/info")
    public Object onUserInfoUpdated(@RequestBody InputOutputUserInfo event) {
        LOG.info("User info updated:{}", event.getUserId());
        return "ok";
    }

    /*
    发送消息回调
     */
    @PostMapping(value = "/im_event/message")
    public Object onMessage(@RequestBody OutputMessageData event) {
        LOG.info("message:{}", event.getMessageId());
        return "ok";
    }

    /*
      发送消息回调
    */
    @PostMapping(value = "/im_event/recall_message")
    public Object onRecallMessage(@RequestBody OutputRecallMessageData event) {
        LOG.info("recall message:{}", event.getUserId());
        return "ok";
    }

    /*
    物联网消息回调
     */
    @PostMapping(value = "/im_event/things/message")
    public Object onThingsMessage(@RequestBody OutputMessageData event) {
        LOG.info("message:{}", event.getMessageId());
        return "ok";
    }

    /*
    消息已读回调
    */
    @PostMapping(value = "/im_event/message_read")
    public Object onMessageRead(@RequestBody OutputReadData event) {
        LOG.info("message:{}", event.user);
        return "ok";
    }

    /*
    群组信息更新回调
     */
    @PostMapping(value = "/im_event/group/info")
    public Object onGroupInfoUpdated(@RequestBody GroupUpdateEvent event) {
        LOG.info("group info updated:{}", event.type);
        return "ok";
    }

    /*
    群组成员更新回调
     */
    @PostMapping(value = "/im_event/group/member")
    public Object onGroupMemberUpdated(@RequestBody GroupMemberUpdateEvent event) {
        LOG.info("group member updated:{}", event.type);
        return "ok";
    }

    /*
    频道信息更新回调
     */
    @PostMapping(value = "/im_event/channel/info")
    public Object onChannelInfoUpdated(@RequestBody ChannelUpdateEvent event) {
        LOG.info("channel info updated:{}", event.type);
        return "ok";
    }

    /*
    聊天室信息更新回调
     */
    @PostMapping(value = "/im_event/chatroom/info")
    public Object onChatroomInfoUpdated(@RequestBody ChatroomUpdateEvent event) {
        LOG.info("chatroom info updated:{}", event.type);
        return "ok";
    }

    /*
    聊天室成员更新回调
     */
    @PostMapping(value = "/im_event/chatroom/member")
    public Object onChatroomMemberUpdated(@RequestBody ChatroomMemberUpdateEvent event) {
        LOG.info("chatroom member updated:{}", event.type);
        return "ok";
    }

    /*
    消息审查示例。

    如果允许发送，返回状态码为200，内容为空；如果替换内容发送，返回状态码200，内容为替换过的payload内容。如果不允许发送，返回状态码403。
    注意如果没有替换内容运行原消息发送，要返回空内容，不要返回原消息！！！
    */
    @PostMapping(value = "/message/censor")
    public Object censorMessage(@RequestBody OutputMessageData event) {
        LOG.info("message:{}", event.getMessageId());
        if(event.getPayload().getSearchableContent() != null && event.getPayload().getSearchableContent().contains("testkongbufenzi")) {
            throw new ForbiddenException();
        }
        if(event.getPayload().getSearchableContent() != null && event.getPayload().getSearchableContent().contains("testzhaopian")) {
            event.getPayload().setSearchableContent(event.getPayload().getSearchableContent().replace("zhaopian", "照片"));
            return new Gson().toJson(event.getPayload());
        }
        return "";
    }

    @PostMapping(value = "/im_event/conference/create")
    public Object onConferenceCreated(@RequestBody ConferenceCreateEvent event) {
        LOG.info("conference created:{}", event);
        return "ok";
    }

    @PostMapping(value = "/im_event/conference/destroy")
    public Object onConferenceDestroyed(@RequestBody ConferenceDestroyEvent event) {
        LOG.info("conference destroyed:{}", event);
        return "ok";
    }

    @PostMapping(value = "/im_event/conference/member_join")
    public Object onConferenceMemberJoined(@RequestBody ConferenceJoinEvent event) {
        LOG.info("conference member joined:{}", event);
        return "ok";
    }

    @PostMapping(value = "/im_event/conference/member_leave")
    public Object onConferenceMemberLeaved(@RequestBody ConferenceLeaveEvent event) {
        LOG.info("conference member leaved:{}", event);
        return "ok";
    }

    @PostMapping(value = "/im_event/conference/member_publish")
    public Object onConferenceMemberPublished(@RequestBody ConferencePublishEvent event) {
        LOG.info("conference member published:{}", event);
        return "ok";
    }

    @PostMapping(value = "/im_event/conference/member_unpublish")
    public Object onConferenceMemberUnpublished(@RequestBody ConferenceUnpublishEvent event) {
        LOG.info("conference member unpublished:{}", event);
        return "ok";
    }

    @PostMapping(value = "/im_event/moments_feed")
    public Object onMomentsFeed(@RequestBody FeedPojo event) {
        LOG.info("feed posted:{}, {}, {}", event.sender, event.feedId, event.text);
        return "ok";
    }

    @PostMapping(value = "/im_event/moments_feed_recall")
    public Object onMomentsFeedRecall(@RequestBody IdPojo event) {
        LOG.info("recall feed:{}", event.id);
        return "ok";
    }

    @PostMapping(value = "/im_event/moments_comment")
    public Object onMomentsComment(@RequestBody CommentPojo event) {
        LOG.info("feed posted:{}, {}, {}, {}", event.sender, event.commentId, event.feedId, event.text);
        return "ok";
    }

    @PostMapping(value = "/im_event/moments_comment_recall")
    public Object onMomentsCommentRecall(@RequestBody IdPojo event) {
        LOG.info("recall comment:{},{}", event.id, event.id2);
        return "ok";
    }
}
