package com.healthcare.api.importdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 每日健康汇总：快捷指令或其他来源导入的步数、睡眠、活动、体重。
 */
@Entity
@Table(name = "daily_health_metrics")
public class DailyMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(nullable = false)
    private int steps;

    @Column(name = "sleep_minutes", nullable = false)
    private int sleepMinutes;

    @Column(name = "active_minutes", nullable = false)
    private int activeMinutes;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "phone_battery")
    private Integer phoneBattery;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getMetricDate() {
        return metricDate;
    }

    public void setMetricDate(LocalDate metricDate) {
        this.metricDate = metricDate;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getSleepMinutes() {
        return sleepMinutes;
    }

    public void setSleepMinutes(int sleepMinutes) {
        this.sleepMinutes = sleepMinutes;
    }

    public int getActiveMinutes() {
        return activeMinutes;
    }

    public void setActiveMinutes(int activeMinutes) {
        this.activeMinutes = activeMinutes;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getPhoneBattery() {
        return phoneBattery;
    }

    public void setPhoneBattery(Integer phoneBattery) {
        this.phoneBattery = phoneBattery;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
