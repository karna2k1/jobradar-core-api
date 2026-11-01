package com.jobradar.core.tracker;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByUserIdOrderByPositionAsc(Long userId);

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    Optional<Application> findByUserIdAndExternalJobId(Long userId, String externalJobId);

    @Query("select max(a.position) from Application a where a.userId = :userId and a.stage = :stage")
    Optional<Double> maxPosition(@Param("userId") Long userId, @Param("stage") Stage stage);

    List<Application> findTop100ByFollowUpSentFalseAndFollowUpAtLessThanEqualOrderByFollowUpAtAsc(Instant now);
}
