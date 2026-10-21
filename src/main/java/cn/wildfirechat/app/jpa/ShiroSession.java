package cn.wildfirechat.app.jpa;

import org.hibernate.annotations.Type;

import javax.persistence.*;

@Entity
@Table(name = "shiro_session")
public class ShiroSession {
	@Id
	@Column(length = 128)
	private String sessionId;

	@Lob
	@Column(name="session_data", length = 2048)
	@Type(type="org.hibernate.type.BinaryType")
	private byte[] sessionData;

	/**
	 * 历史数据里 update_time 可能为 NULL（老版本没有这一列/手工写入），
	 * 必须用包装类型，不能用 primitive long，否则 Hibernate 加载时直接抛
	 * “Null value was assigned to a property of primitive type”。
	 */
	@Column(name = "update_time")
	private Long updateTime;

	public ShiroSession(String sessionId, byte[] sessionData) {
		this.sessionId = sessionId;
		this.sessionData = sessionData;
		this.updateTime = System.currentTimeMillis();
	}

	public ShiroSession() {
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public byte[] getSessionData() {
		return sessionData;
	}

	public void setSessionData(byte[] sessionData) {
		this.sessionData = sessionData;
	}

	/** NULL 按 0 处理，调用方不需要判空；清理 SQL 会排除 NULL/0 的历史数据 */
	public long getUpdateTime() {
		return updateTime == null ? 0L : updateTime;
	}

	public void setUpdateTime(Long updateTime) {
		this.updateTime = updateTime;
	}
}
