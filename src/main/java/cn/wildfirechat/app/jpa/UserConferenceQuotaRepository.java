package cn.wildfirechat.app.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户会议额度Repository
 */
@Repository
public interface UserConferenceQuotaRepository extends CrudRepository<UserConferenceQuota, String> {

    /**
     * 根据用户ID查询额度配置
     */
    Optional<UserConferenceQuota> findByUserId(String userId);
}
