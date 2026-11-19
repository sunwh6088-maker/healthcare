package com.healthcare.api.push;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {

    List<PushSubscriptionEntity> findByUserId(Long userId);

    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    long deleteByUserIdAndEndpoint(Long userId, String endpoint);

    void deleteByUserId(Long userId);
}
