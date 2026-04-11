package cn.wildfirechat.app.pojo;

/**
 * 会议额度查询响应
 */
public class ConferenceQuotaResponse {
    
    // 用户月度额度（分钟）
    private int totalQuota;
    
    // 当月已使用额度（分钟）
    private int usedMinutes;
    
    // 当月剩余额度（分钟）
    private int remainingMinutes;
    
    // 是否不限制（true表示无额度限制）
    private boolean unlimited;
    
    // 当前年月（yyyyMM格式）
    private String yearMonth;

    public ConferenceQuotaResponse() {
    }

    public int getTotalQuota() {
        return totalQuota;
    }

    public void setTotalQuota(int totalQuota) {
        this.totalQuota = totalQuota;
    }

    public int getUsedMinutes() {
        return usedMinutes;
    }

    public void setUsedMinutes(int usedMinutes) {
        this.usedMinutes = usedMinutes;
    }

    public int getRemainingMinutes() {
        return remainingMinutes;
    }

    public void setRemainingMinutes(int remainingMinutes) {
        this.remainingMinutes = remainingMinutes;
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public void setUnlimited(boolean unlimited) {
        this.unlimited = unlimited;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }
}
