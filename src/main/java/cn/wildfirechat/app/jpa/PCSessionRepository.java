package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface PCSessionRepository extends CrudRepository<PCSession, String> {

    List<PCSession> findByTokenIn(Collection<String> tokens);

    @Modifying
    @Transactional
    Long deleteByCreateDtBefore(long timestamp);
}
