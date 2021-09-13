package cn.wildfirechat.app.jpa;

import cn.wildfirechat.app.model.ConferenceDTO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource()
public interface UserConferenceRepository extends PagingAndSortingRepository<UserConference, Long> {
    @Transactional
    @Modifying
    @Query(value = "delete from user_conference where user_id = ?1 and conference_id = ?2", nativeQuery = true)
    void deleteByUserIdAndConferenceId(String userId, String conferenceId);

    @Query(value = "select c.* from user_conference uc, conference c where uc.user_id = ?1 and uc.conference_id = c.id and (c.end_time = 0 or c.end_time > ?2) order by id desc", nativeQuery = true)
    List<ConferenceDTO> findByUserId(String userId, long now);

    Optional<UserConference> findByUserIdAndConferenceId(String userId, String conferenceId);
}
