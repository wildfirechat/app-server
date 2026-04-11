package cn.wildfirechat.app.jpa;

import javax.persistence.*;
import java.util.Date;

/**
 * 用户会议额度表
 * 存储用户的会议分钟数配额（不分月份，固定额度）
 */
@Entity
@Table(name = "user_conference_quota")
public class UserConferenceQuota {

    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "total_minutes")
    private int totalMinutes;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    public UserConferenceQuota() {
    }

    public UserConferenceQuota(String userId, int totalMinutes) {
        this.userId = userId;
        this.totalMinutes = totalMinutes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
