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

	@Column(name = "update_time")
	private long updateTime;

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

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}
}
