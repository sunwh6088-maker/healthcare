package com.healthcare.api.importdata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyMetricRepository extends JpaRepository<DailyMetricEntity, Long> {

    Optional<DailyMetricEntity> findByUserIdAndMetricDate(Long userId, LocalDate metricDate);

    void deleteByUserId(Long userId);
}
