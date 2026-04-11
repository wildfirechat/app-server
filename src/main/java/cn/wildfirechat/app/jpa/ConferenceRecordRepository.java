package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 会议记录Repository
 */
@Repository
public interface ConferenceRecordRepository extends CrudRepository<ConferenceRecord, String> {

    /**
     * 根据会议ID查询记录
     */
    Optional<ConferenceRecord> findByConferenceId(String conferenceId);

    /**
     * 更新会议结束信息
     */
    @Modifying
    @Query("UPDATE ConferenceRecord r SET r.endTime = ?2, r.actualDuration = ?3, r.status = 1, r.updatedAt = CURRENT_TIMESTAMP WHERE r.conferenceId = ?1")
    int endConference(String conferenceId, long endTime, int actualDuration);
}
