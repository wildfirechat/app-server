package cn.wildfirechat.app.jpa;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource()
public interface ConferenceEntityRepository extends PagingAndSortingRepository<ConferenceEntity, String> {


}
