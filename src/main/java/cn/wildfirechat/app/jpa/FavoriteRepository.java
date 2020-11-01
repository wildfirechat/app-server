package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource()
public interface FavoriteRepository extends CrudRepository<FavoriteItem, Long> {

    @Query(value = "select * from t_favorites where id > ?1 order by id desc limit ?2", nativeQuery = true)
    List<FavoriteItem> loadFav(long startId, int count);
}
