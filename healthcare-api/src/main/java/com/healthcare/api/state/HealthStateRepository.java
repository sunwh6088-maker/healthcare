package com.healthcare.api.state;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthStateRepository extends JpaRepository<HealthStateEntity, Long> {
}
