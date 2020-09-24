package cn.wildfirechat.app.jpa;

import org.springframework.data.repository.CrudRepository;

public interface ShiroSessionRepository extends CrudRepository<ShiroSession, String> {
}
