package cn.wildfirechat.app.jpa;

import org.springframework.data.repository.CrudRepository;

public interface UserNameRepository extends CrudRepository<UserNameEntry, Long> {}
