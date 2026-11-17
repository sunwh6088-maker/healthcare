package com.healthcare.api.state;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    List<AlarmEntity> findByUserIdOrderByTimeAsc(Long userId);

    void deleteByUserId(Long userId);
}
