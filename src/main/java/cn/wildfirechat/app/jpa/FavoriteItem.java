package cn.wildfirechat.app.jpa;

import javax.annotation.Nullable;
import javax.persistence.*;

@Entity
@Table(name = "t_favorites", indexes = {@Index(columnList = "user_id, type")})
public class FavoriteItem {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id")
	public Long id;

    @Column(name = "messageUid")
    @Nullable
    public Long messageUid;

    @Column(name = "user_id", length = 64)
	public String userId;

    @Column(name = "type")
	public int type;

    @Column(name = "timestamp")
	public long timestamp;

	@Column(name = "conv_type")
	public int convType;

	@Column(name = "conv_line")
	public int convLine;

	@Column(name = "conv_target")
	public String convTarget;

	@Column(name = "origin")
	public String origin;

    @Column(name = "sender")
	public String sender;

	@Lob
	@Column(name="title")
	public String title;

	@Column(name="url",length = 1024)
	public String url;

	@Column(name = "thumb_url",length = 1024)
	public String thumbUrl;

	@Lob
	@Column(name="data")
	public String data;

}
