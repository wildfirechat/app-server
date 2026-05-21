package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FavoriteRepository extends CrudRepository<FavoriteItem, Long> {

    @Query(value = "select * from t_favorites where user_id = ?1 and id < ?2 order by id desc limit ?3", nativeQuery = true)
    List<FavoriteItem> loadFav(String userId, long startId, int count);
}
