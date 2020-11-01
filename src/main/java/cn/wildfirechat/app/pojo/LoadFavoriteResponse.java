package cn.wildfirechat.app.pojo;

import cn.wildfirechat.app.jpa.FavoriteItem;

import java.util.List;

public class LoadFavoriteResponse {
    public List<FavoriteItem> items;
    public boolean hasMore;
}
