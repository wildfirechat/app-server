package cn.wildfirechat.app.pojo;

/**
 * POST /robot/provision 请求体。所有字段可选。
 */
public class RobotProvisionRequest {
    private String clientId;
    private Integer platform;
    private Boolean forceRefresh;
    private Integer configVersion;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public Integer getPlatform() { return platform; }
    public void setPlatform(Integer platform) { this.platform = platform; }

    public Boolean getForceRefresh() { return forceRefresh; }
    public void setForceRefresh(Boolean forceRefresh) { this.forceRefresh = forceRefresh; }

    public Integer getConfigVersion() { return configVersion; }
    public void setConfigVersion(Integer configVersion) { this.configVersion = configVersion; }
}
