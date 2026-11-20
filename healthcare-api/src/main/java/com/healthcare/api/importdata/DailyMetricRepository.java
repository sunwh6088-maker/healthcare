package com.healthcare.api.importdata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyMetricRepository extends JpaRepository<DailyMetricEntity, Long> {

    Optional<DailyMetricEntity> findByUserIdAndMetricDate(Long userId, LocalDate metricDate);

    List<DailyMetricEntity> findByUserIdAndMetricDateBetweenOrderByMetricDateAsc(
            Long userId, LocalDate from, LocalDate to);

    void deleteByUserId(Long userId);
}
