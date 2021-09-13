package cn.wildfirechat.app.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_private_conference_id")
public class UserPrivateConferenceId {
	@Id
	@Column(length = 128)
	private String userId;

	private String conferenceId;

	public UserPrivateConferenceId() {
	}

	public UserPrivateConferenceId(String userId, String conferenceId) {
		this.userId = userId;
		this.conferenceId = conferenceId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getConferenceId() {
		return conferenceId;
	}

	public void setConferenceId(String conferenceId) {
		this.conferenceId = conferenceId;
	}
}
