package com.healthcare.api.iot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IotDeviceEventRepository extends JpaRepository<IotDeviceEventEntity, Long> {

    void deleteByDeviceId(Long deviceId);
}
