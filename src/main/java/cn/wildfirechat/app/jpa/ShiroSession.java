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

	public ShiroSession(String sessionId, byte[] sessionData) {
		this.sessionId = sessionId;
		this.sessionData = sessionData;
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
}
