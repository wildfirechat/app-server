package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ShiroSessionRepository extends CrudRepository<ShiroSession, String> {

    /**
     * 轻量更新 session 的 update_time，用于读缓存过期后仍被访问的场景，
     * 避免 60 秒 touch 节流导致活跃 session 的 update_time 长期不刷新而被误清理。
     */
    @Modifying
    @Transactional
    @Query("UPDATE ShiroSession s SET s.updateTime = :timestamp WHERE s.sessionId = :sessionId")
    int updateAccessTime(@Param("sessionId") String sessionId, @Param("timestamp") long timestamp);

    /**
     * 删除有明确更新时间且超过指定时间戳的 session。
     * 不清理 update_time 为 0 或 null 的历史数据。
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ShiroSession s WHERE s.updateTime > 0 AND s.updateTime < :timestamp")
    Long deleteExpiredSessions(@Param("timestamp") long timestamp);
}
