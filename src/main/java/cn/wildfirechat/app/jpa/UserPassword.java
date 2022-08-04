package cn.wildfirechat.app.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_password")
public class UserPassword {
	@Id
	@Column(length = 128)
	private String userId;

	private String password;

	private String salt;

	private String resetCode;

	public UserPassword() {
	}

	public UserPassword(String userId, String password, String salt) {
		this.userId = userId;
		this.password = password;
		this.salt = salt;
	}

	public UserPassword(String userId, String password, String salt, String resetCode) {
		this.userId = userId;
		this.password = password;
		this.salt = salt;
		this.resetCode = resetCode;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public String getResetCode() {
		return resetCode;
	}

	public void setResetCode(String resetCode) {
		this.resetCode = resetCode;
	}
}
