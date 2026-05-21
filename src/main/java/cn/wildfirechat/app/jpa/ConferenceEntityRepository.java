package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import java.util.List;

public interface ConferenceEntityRepository extends PagingAndSortingRepository<ConferenceEntity, String> {

    /**
     * 查询已过期的会议（endTime > 0 且 endTime < 当前时间）
     * @param currentTime 当前时间（秒）
     * @return 过期会议列表
     */
    @Query("SELECT c FROM ConferenceEntity c WHERE c.endTime > 0 AND c.endTime < ?1")
    List<ConferenceEntity> findExpiredConferences(long currentTime);

}
