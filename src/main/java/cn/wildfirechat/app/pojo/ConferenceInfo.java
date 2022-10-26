package cn.wildfirechat.app.pojo;

import java.util.List;

public class ConferenceInfo {
    public String conferenceId;
    public String conferenceTitle;
    public String password;
    public String pin;
    public String owner;
    public List<String> managers;
    public long startTime;
    public long endTime;
    public boolean audience;
    public boolean advance;
    public boolean allowSwitchMode;
    public boolean noJoinBeforeStart;
    public boolean recording;
    public String focus;
}
