package com.healthcare.api.iot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IotDeviceRepository extends JpaRepository<IotDeviceEntity, Long> {

    List<IotDeviceEntity> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<IotDeviceEntity> findByProductKeyAndDeviceName(String productKey, String deviceName);

    Optional<IotDeviceEntity> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);
}
