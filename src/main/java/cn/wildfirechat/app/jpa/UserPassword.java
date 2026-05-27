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

	private long resetCodeTime;

	private int tryCount;

	private long lastTryTime;

	/** 密码最后一次更新时间（毫秒时间戳），0 或 null 表示从未设置过密码或历史数据无记录 */
	private Long passwordUpdateTime;

	public UserPassword() {
		this.passwordUpdateTime = 0L;
	}

	public UserPassword(String userId) {
		this.userId = userId;
		this.passwordUpdateTime = 0L;
	}

	public UserPassword(String userId, String password, String salt) {
		this.userId = userId;
		this.password = password;
		this.salt = salt;
		this.resetCodeTime = 0;
		this.tryCount = 0;
		this.lastTryTime = 0;
		this.passwordUpdateTime = 0L;
	}

	public UserPassword(String userId, String password, String salt, String resetCode, long resetCodeTime) {
		this.userId = userId;
		this.password = password;
		this.salt = salt;
		this.resetCode = resetCode;
		this.resetCodeTime = resetCodeTime;
		this.tryCount = 0;
		this.lastTryTime = 0;
		this.passwordUpdateTime = 0L;
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

	public long getResetCodeTime() {
		return resetCodeTime;
	}

	public void setResetCodeTime(long resetCodeTime) {
		this.resetCodeTime = resetCodeTime;
	}

	public int getTryCount() {
		return tryCount;
	}

	public void setTryCount(int tryCount) {
		this.tryCount = tryCount;
	}

	public long getLastTryTime() {
		return lastTryTime;
	}

	public void setLastTryTime(long lastTryTime) {
		this.lastTryTime = lastTryTime;
	}

	public long getPasswordUpdateTime() {
		return passwordUpdateTime == null ? 0L : passwordUpdateTime;
	}

	public void setPasswordUpdateTime(long passwordUpdateTime) {
		this.passwordUpdateTime = passwordUpdateTime;
	}
}
