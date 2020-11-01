package cn.wildfirechat.app.jpa;

import javax.persistence.*;

@Entity
@Table(name = "t_favorites", indexes = {@Index(columnList = "userId, type")})
public class FavoriteItem {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id")
	public Long id;

	public String userId;

	public int type;

	public long timestamp;

	@Column(name = "conv_type")
	public int convType;

	@Column(name = "conv_line")
	public int convLine;

	@Column(name = "conv_target")
	public String convTarget;

	public String origin;

	public String sender;

	@Column(name="title",columnDefinition="LONGTEXT")
	public String title;

	@Column(name="url",length = 1024)
	public String url;

	@Column(name = "thumb_url",length = 1024)
	public String thumbUrl;

	@Column(name="data",columnDefinition="LONGTEXT")
	public String data;

}
