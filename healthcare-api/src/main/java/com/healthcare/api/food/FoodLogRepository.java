package com.healthcare.api.food;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FoodLogRepository extends JpaRepository<FoodLogEntity, Long> {

    List<FoodLogEntity> findByUserIdAndLogDateOrderByIdAsc(Long userId, LocalDate logDate);

    List<FoodLogEntity> findByUserIdOrderByLogDateDescIdDesc(Long userId);

    Optional<FoodLogEntity> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(sum(f.calories), 0) from FoodLogEntity f "
            + "where f.userId = :userId and f.logDate = :logDate")
    int sumCalories(@Param("userId") Long userId, @Param("logDate") LocalDate logDate);
}
