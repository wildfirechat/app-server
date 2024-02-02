package cn.wildfirechat.app;

import cn.wildfirechat.pojos.*;
import com.google.gson.Gson;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
IM对应事件发生时，会回调到配置地址。需要注意IM服务单线程进行回调，如果接收方处理太慢会导致推送线程被阻塞，导致延迟发生，甚至导致IM系统异常。
建议异步处理快速返回，这里收到后转到异步线程处理，并且立即返回。另外两个服务器的ping值不能太大。
 */
@RestController()
public class IMCallbackController {
    /*
    用户在线状态回调
     */
    @PostMapping(value = "/im_event/user/online")
    public Object onUserOnlineEvent(@RequestBody UserOnlineStatus event) {
        System.out.println("User:" + event.userId + " on device:" + event.clientId + " online status:" + event.status);
        return "ok";
    }

    /*
    用户关系变更回调
     */
    @PostMapping(value = "/im_event/user/relation")
    public Object onUserRelationUpdated(@RequestBody RelationUpdateEvent event) {
        System.out.println("User relation updated:" + event.userId);
        return "ok";
    }

    /*
    用户信息更新回调
     */
    @PostMapping(value = "/im_event/user/info")
    public Object onUserInfoUpdated(@RequestBody InputOutputUserInfo event) {
        System.out.println("User info updated:" + event.getUserId());
        return "ok";
    }

    /*
    发送消息回调
     */
    @PostMapping(value = "/im_event/message")
    public Object onMessage(@RequestBody OutputMessageData event) {
        System.out.println("message:" +event.getMessageId());
        return "ok";
    }

    /*
      发送消息回调
    */
    @PostMapping(value = "/im_event/recall_message")
    public Object onRecallMessage(@RequestBody OutputRecallMessageData event) {
        System.out.println("recall message:" +event.getUserId());
        return "ok";
    }

    /*
    物联网消息回调
     */
    @PostMapping(value = "/im_event/things/message")
    public Object onThingsMessage(@RequestBody OutputMessageData event) {
        System.out.println("message:" + event.getMessageId());
        return "ok";
    }

    /*
    消息已读回调
    */
    @PostMapping(value = "/im_event/message_read")
    public Object onMessageRead(@RequestBody OutputReadData event) {
        System.out.println("message:" +event.user);
        return "ok";
    }

    /*
    群组信息更新回调
     */
    @PostMapping(value = "/im_event/group/info")
    public Object onGroupInfoUpdated(@RequestBody GroupUpdateEvent event) {
        System.out.println("group info updated:" + event.type);
        return "ok";
    }

    /*
    群组成员更新回调
     */
    @PostMapping(value = "/im_event/group/member")
    public Object onGroupMemberUpdated(@RequestBody GroupMemberUpdateEvent event) {
        System.out.println("group member updated:" + event.type);
        return "ok";
    }

    /*
    频道信息更新回调
     */
    @PostMapping(value = "/im_event/channel/info")
    public Object onChannelInfoUpdated(@RequestBody ChannelUpdateEvent event) {
        System.out.println("channel info updated:" + event.type);
        return "ok";
    }

    /*
    聊天室信息更新回调
     */
    @PostMapping(value = "/im_event/chatroom/info")
    public Object onChatroomInfoUpdated(@RequestBody ChatroomUpdateEvent event) {
        System.out.println("chatroom info updated:" + event.type);
        return "ok";
    }

    /*
    聊天室成员更新回调
     */
    @PostMapping(value = "/im_event/chatroom/member")
    public Object onChatroomMemberUpdated(@RequestBody ChatroomMemberUpdateEvent event) {
        System.out.println("chatroom member updated:" + event.type);
        return "ok";
    }

    /*
    消息审查示例。

    如果允许发送，返回状态码为200，内容为空；如果替换内容发送，返回状态码200，内容为替换过的payload内容。如果不允许发送，返回状态码403。
    */
    @PostMapping(value = "/message/censor")
    public Object censorMessage(@RequestBody OutputMessageData event) {
        System.out.println("message:" +event.getMessageId());
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
        System.out.println("conference created:" + event);
        return "ok";
    }

    @PostMapping(value = "/im_event/conference/destroy")
    public Object onConferenceDestroyed(@RequestBody ConferenceDestroyEvent event) {
        System.out.println("conference destroyed:" + event);
        return "ok";
    }

    @PostMapping(value = "/im_event/conference/member_join")
    public Object onConferenceMemberJoined(@RequestBody ConferenceJoinEvent event) {
        System.out.println("conference member joined:" + event);
        return "ok";
    }

    @PostMapping(value = "/im_event/conference/member_leave")
    public Object onConferenceMemberLeaved(@RequestBody ConferenceLeaveEvent event) {
        System.out.println("conference member leaved:" + event);
        return "ok";
    }

    @PostMapping(value = "/im_event/conference/member_publish")
    public Object onConferenceMemberPublished(@RequestBody ConferencePublishEvent event) {
        System.out.println("conference member published:" + event);
        return "ok";
    }

    @PostMapping(value = "/im_event/conference/member_unpublish")
    public Object onConferenceMemberUnpublished(@RequestBody ConferenceUnpublishEvent event) {
        System.out.println("conference member unpublished:" + event);
        return "ok";
    }
}
