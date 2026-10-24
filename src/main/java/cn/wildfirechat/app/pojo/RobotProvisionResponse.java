package cn.wildfirechat.app.pojo;

/**
 * POST /robot/provision 响应。
 */
public class RobotProvisionResponse {
    private Robot robot;
    private Ai ai;
    private Features features;
    private int configVersion;
    private long updatedAt;

    public Robot getRobot() { return robot; }
    public void setRobot(Robot robot) { this.robot = robot; }

    public Ai getAi() { return ai; }
    public void setAi(Ai ai) { this.ai = ai; }

    public Features getFeatures() { return features; }
    public void setFeatures(Features features) { this.features = features; }

    public int getConfigVersion() { return configVersion; }
    public void setConfigVersion(int configVersion) { this.configVersion = configVersion; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public static class Robot {
        private String robotId;
        private String robotSecret;
        private String gatewayUrl;
        private String imServerUrl;
        private String owner;
        private String displayName;
        private String portrait;

        public String getRobotId() { return robotId; }
        public void setRobotId(String robotId) { this.robotId = robotId; }

        public String getRobotSecret() { return robotSecret; }
        public void setRobotSecret(String robotSecret) { this.robotSecret = robotSecret; }

        public String getGatewayUrl() { return gatewayUrl; }
        public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }

        public String getImServerUrl() { return imServerUrl; }
        public void setImServerUrl(String imServerUrl) { this.imServerUrl = imServerUrl; }

        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getPortrait() { return portrait; }
        public void setPortrait(String portrait) { this.portrait = portrait; }
    }

    public static class Ai {
        private String provider;
        private String baseUrl;
        private String apiKey;
        private String model;
        private String reasoningEffort;
        private Integer maxTokens;
        private Integer contextWindow;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getReasoningEffort() { return reasoningEffort; }
        public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }

        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

        public Integer getContextWindow() { return contextWindow; }
        public void setContextWindow(Integer contextWindow) { this.contextWindow = contextWindow; }
    }

    public static class Features {
        private int line = 2;
        private String workspaceMode = "multi-group-per-workspace";
        private String sessionMode = "one-session-per-group-per-robot";
        private boolean multiRobot = true;

        public int getLine() { return line; }
        public void setLine(int line) { this.line = line; }

        public String getWorkspaceMode() { return workspaceMode; }
        public void setWorkspaceMode(String workspaceMode) { this.workspaceMode = workspaceMode; }

        public String getSessionMode() { return sessionMode; }
        public void setSessionMode(String sessionMode) { this.sessionMode = sessionMode; }

        public boolean isMultiRobot() { return multiRobot; }
        public void setMultiRobot(boolean multiRobot) { this.multiRobot = multiRobot; }
    }
}
