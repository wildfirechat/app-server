package cn.wildfirechat.app.jpa;

import javax.persistence.*;

import java.util.List;

import static javax.persistence.CascadeType.ALL;

@Entity
@Table(name = "user_conference", uniqueConstraints = {@UniqueConstraint(columnNames = {"userId","conferenceId"})})
public class UserConference {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "id")
	public Long id;

	@Column(length = 128)
	private String userId;

	private String conferenceId;

	private long timestamp;

	public UserConference() {
	}

	public UserConference(String userId, String conferenceId) {
		this.userId = userId;
		this.conferenceId = conferenceId;
		this.timestamp = System.currentTimeMillis();
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
