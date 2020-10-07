package cn.wildfirechat.app.pojo;

import java.util.List;

public class SendMessageRequest {
    public int type;
    public String target;
    public int line;

    public int content_type;
    public String content_searchable;
    public String content_binary;
    public String content;
    public String content_push;
    public String content_push_data;
    public int content_media_type;
    public String content_remote_url;
    public String content_extra;
    public int content_mentioned_type;
    public List<String> content_mentioned_targets;
}
