package com.jobradar.core.profile;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findFirstByUserIdOrderByUploadedAtDesc(Long userId);
}
