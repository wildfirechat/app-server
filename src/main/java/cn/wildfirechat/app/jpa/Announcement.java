package cn.wildfirechat.app.jpa;

import javax.persistence.*;

@Entity
@Table(name = "text")
public class Announcement {
	@Id
	@Column(length = 128)
	private String groupId;

	private String author;

	@Column(length = 2048)
	private String announcement;

	private long timestamp;

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getAnnouncement() {
		return announcement;
	}

	public void setAnnouncement(String announcement) {
		this.announcement = announcement;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
