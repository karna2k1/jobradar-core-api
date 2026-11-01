package com.jobradar.core.tracker;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, Long> {
}
