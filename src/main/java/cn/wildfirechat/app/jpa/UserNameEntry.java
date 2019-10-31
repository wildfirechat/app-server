package cn.wildfirechat.app.jpa;

import javax.persistence.*;

@Entity
@Table(name = "t_user_name")
public class UserNameEntry {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id")
	private Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
