package cn.wildfirechat.app.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource()
public interface UserPasswordRepository extends CrudRepository<UserPassword, String> {


}
