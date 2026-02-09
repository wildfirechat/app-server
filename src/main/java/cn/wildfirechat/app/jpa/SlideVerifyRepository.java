package cn.wildfirechat.app.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SlideVerifyRepository extends CrudRepository<SlideVerify, String> {

    Optional<SlideVerify> findByToken(String token);

    @Modifying
    @Query("DELETE FROM SlideVerify s WHERE s.timestamp < : cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
