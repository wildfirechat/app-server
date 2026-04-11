package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户额度使用Repository
 */
@Repository
public interface UserQuotaUsageRepository extends CrudRepository<UserQuotaUsage, Long> {

    /**
     * 根据用户ID和年月查询使用记录
     */
    Optional<UserQuotaUsage> findByUserIdAndYearMonth(String userId, String yearMonth);

    /**
     * 增加用户使用分钟数
     */
    @Modifying
    @Query("UPDATE UserQuotaUsage u SET u.usedMinutes = u.usedMinutes + ?3 WHERE u.userId = ?1 AND u.yearMonth = ?2")
    int addUsedMinutes(String userId, String yearMonth, int minutes);
}
