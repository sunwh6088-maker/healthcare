package com.healthcare.api.exercise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciseLogRepository extends JpaRepository<ExerciseLogEntity, Long> {

    List<ExerciseLogEntity> findByUserIdOrderByLogDateDescIdDesc(Long userId);

    Optional<ExerciseLogEntity> findByIdAndUserId(Long id, Long userId);
}
