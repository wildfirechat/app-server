package cn.wildfirechat.app.avatar;

import java.util.List;

public class GroupAvatarRequest {
    private List<GroupMemberInfo> members;

    public List<GroupMemberInfo> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMemberInfo> members) {
        this.members = members;
    }

    public static class GroupMemberInfo {
        private String name;
        private String avatarUrl;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
    }
}
