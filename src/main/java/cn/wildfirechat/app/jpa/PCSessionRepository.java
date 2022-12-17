package cn.wildfirechat.app.jpa;

import org.springframework.data.repository.CrudRepository;

public interface PCSessionRepository extends CrudRepository<PCSession, String> {

    Long deleteByCreateDtBefore(long timestamp);
}
