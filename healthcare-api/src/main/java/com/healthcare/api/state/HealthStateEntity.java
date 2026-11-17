package com.healthcare.api.state;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "health_states")
public class HealthStateEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    private String gender;
    private int age;
    private int heightCm;
    private double weightKg;
    private int foodKcal;
    private int exerciseCount;

    private int steps;
    private int stepsGoal;
    private int waterMl;
    private int waterGoalMl;
    private int activeMinutes;
    private int activeGoalMinutes;
    private int sleepMinutes;
    private int sleepStartMinutes;

    private boolean notifyEnabled;
    private boolean privacyEnabled;
    private boolean deviceConnected;
    private boolean deviceAdded;

    @Column(nullable = false)
    private int deviceBattery;

    @Column(nullable = false)
    private Instant deviceLastSyncAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public static HealthStateEntity defaults(Long userId) {
        HealthStateEntity state = new HealthStateEntity();
        state.userId = userId;
        state.gender = null;
        state.age = 22;
        state.heightCm = 160;
        state.weightKg = 60;
        state.foodKcal = 0;
        state.exerciseCount = 0;
        state.steps = 4204;
        state.stepsGoal = 6000;
        state.waterMl = 900;
        state.waterGoalMl = 2000;
        state.activeMinutes = 0;
        state.activeGoalMinutes = 60;
        state.sleepMinutes = 500;
        state.sleepStartMinutes = 23 * 60;
        state.notifyEnabled = true;
        state.privacyEnabled = true;
        state.deviceConnected = true;
        state.deviceAdded = false;
        state.deviceBattery = 86;
        state.deviceLastSyncAt = Instant.now();
        return state;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(int heightCm) {
        this.heightCm = heightCm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public int getFoodKcal() {
        return foodKcal;
    }

    public void setFoodKcal(int foodKcal) {
        this.foodKcal = foodKcal;
    }

    public int getExerciseCount() {
        return exerciseCount;
    }

    public void setExerciseCount(int exerciseCount) {
        this.exerciseCount = exerciseCount;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getStepsGoal() {
        return stepsGoal;
    }

    public void setStepsGoal(int stepsGoal) {
        this.stepsGoal = stepsGoal;
    }

    public int getWaterMl() {
        return waterMl;
    }

    public void setWaterMl(int waterMl) {
        this.waterMl = waterMl;
    }

    public int getWaterGoalMl() {
        return waterGoalMl;
    }

    public void setWaterGoalMl(int waterGoalMl) {
        this.waterGoalMl = waterGoalMl;
    }

    public int getActiveMinutes() {
        return activeMinutes;
    }

    public void setActiveMinutes(int activeMinutes) {
        this.activeMinutes = activeMinutes;
    }

    public int getActiveGoalMinutes() {
        return activeGoalMinutes;
    }

    public void setActiveGoalMinutes(int activeGoalMinutes) {
        this.activeGoalMinutes = activeGoalMinutes;
    }

    public int getSleepMinutes() {
        return sleepMinutes;
    }

    public void setSleepMinutes(int sleepMinutes) {
        this.sleepMinutes = sleepMinutes;
    }

    public int getSleepStartMinutes() {
        return sleepStartMinutes;
    }

    public void setSleepStartMinutes(int sleepStartMinutes) {
        this.sleepStartMinutes = sleepStartMinutes;
    }

    public boolean isNotifyEnabled() {
        return notifyEnabled;
    }

    public void setNotifyEnabled(boolean notifyEnabled) {
        this.notifyEnabled = notifyEnabled;
    }

    public boolean isPrivacyEnabled() {
        return privacyEnabled;
    }

    public void setPrivacyEnabled(boolean privacyEnabled) {
        this.privacyEnabled = privacyEnabled;
    }

    public boolean isDeviceConnected() {
        return deviceConnected;
    }

    public void setDeviceConnected(boolean deviceConnected) {
        this.deviceConnected = deviceConnected;
    }

    public boolean isDeviceAdded() {
        return deviceAdded;
    }

    public void setDeviceAdded(boolean deviceAdded) {
        this.deviceAdded = deviceAdded;
    }

    public int getDeviceBattery() {
        return deviceBattery;
    }

    public void setDeviceBattery(int deviceBattery) {
        this.deviceBattery = deviceBattery;
    }

    public Instant getDeviceLastSyncAt() {
        return deviceLastSyncAt;
    }

    public void setDeviceLastSyncAt(Instant deviceLastSyncAt) {
        this.deviceLastSyncAt = deviceLastSyncAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
