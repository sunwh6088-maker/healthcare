package com.healthcare.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 用户实体：手机号、邮箱、昵称和密码哈希。
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String phone;

    @Column(length = 128)
    private String email;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    /** 头像版本号：每次上传/删除自增，用作图片 URL 的缓存版本。 */
    @Column(name = "avatar_version", nullable = false)
    private int avatarVersion;

    @Column(name = "has_avatar", nullable = false)
    private boolean hasAvatar;

    /** 健康数据导入令牌（只存 SHA-256）。 */
    @Column(name = "import_token_hash", length = 64)
    private String importTokenHash;

    @Column(name = "import_token_created_at")
    private Instant importTokenCreatedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getAvatarVersion() {
        return avatarVersion;
    }

    public void setAvatarVersion(int avatarVersion) {
        this.avatarVersion = avatarVersion;
    }

    public boolean isHasAvatar() {
        return hasAvatar;
    }

    public void setHasAvatar(boolean hasAvatar) {
        this.hasAvatar = hasAvatar;
    }

    public String getImportTokenHash() {
        return importTokenHash;
    }

    public void setImportTokenHash(String importTokenHash) {
        this.importTokenHash = importTokenHash;
    }

    public Instant getImportTokenCreatedAt() {
        return importTokenCreatedAt;
    }

    public void setImportTokenCreatedAt(Instant importTokenCreatedAt) {
        this.importTokenCreatedAt = importTokenCreatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
