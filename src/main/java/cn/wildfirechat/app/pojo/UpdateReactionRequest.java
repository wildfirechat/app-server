package cn.wildfirechat.app.pojo;

public class UpdateReactionRequest {
    // 使用 String 接收，避免 JSON 解析 long 类型的问题
    private String messageUid;
    private String emoji;

    public long getMessageUid() {
        try {
            return Long.parseLong(messageUid);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setMessageUid(String messageUid) {
        this.messageUid = messageUid;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}
