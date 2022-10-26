package cn.wildfirechat.app.model;

public interface ConferenceDTO {
    String getId();
    String getConference_title();
    String getPassword();
    String getPin();
    String getOwner();
    public String getManages();
    long getStart_time();
    long getEnd_time();
    boolean isAudience();
    boolean isAdvance();
    boolean isAllow_switch_mode();
    boolean isNo_join_before_start();
    boolean isRecording();
    String getFocus();
}
