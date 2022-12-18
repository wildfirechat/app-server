package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PCSessionRepository extends CrudRepository<PCSession, String> {

    @Modifying
    @Transactional
    Long deleteByCreateDtBefore(long timestamp);
}
