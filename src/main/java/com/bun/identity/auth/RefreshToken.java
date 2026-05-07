package com.bun.identity.auth;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.bun.identity.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh Token entity for multi-device authentication.
 * Stores hashed refresh tokens with device tracking and rotation support.
 * 
 * Security:
 * - Only token hashes are stored (never plain tokens)
 * - Supports token rotation and revocation
 * - Tracks device information for security auditing
 * 
 * @author Mohammad
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_user", columnList = "app_user_id"),
        @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_token_device", columnList = "app_user_id, device_id"),
        @Index(name = "idx_refresh_token_expires", columnList = "expires_at"),
        @Index(name = "idx_refresh_token_created", columnList = "created_at")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who owns this refresh token
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_token_user"))
    private AppUser appUser;

    /**
     * SHA-256 hash of the refresh token.
     * NEVER store plain tokens in the database.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    /**
     * Unique device identifier (can be generated client-side or server-side)
     */
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    /**
     * Human-readable device name (e.g., "iPhone 13", "Chrome on Windows")
     */
    @Column(name = "device_name")
    private String deviceName;

    /**
     * Device type for categorization
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 50)
    private DeviceType deviceType;

    /**
     * IP address from which the token was created
     */
    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    /**
     * User agent string from the client
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * When the token was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * When the token expires
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Last time this token was used for refresh
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * When the token was revoked (null if still valid)
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Reason for revocation (for auditing)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason")
    private RevocationReason revokedReason;

    /**
     * Links to the new token hash when rotation occurs (audit trail)
     */
    @Column(name = "replaced_by_token_hash", length = 128)
    private String replacedByTokenHash;

    /**
     * Check if token is currently valid (not revoked and not expired)
     */
    @Transient
    public boolean isValid() {
        return revokedAt == null && Instant.now().isBefore(expiresAt);
    }

    /**
     * Check if token is expired
     */
    @Transient
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if token is revoked
     */
    @Transient
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Revoke this token with a reason
     */
    public void revoke(RevocationReason reason) {
        this.revokedAt = Instant.now();
        this.revokedReason = reason;
    }

    /**
     * Mark token as used (updates last_used_at)
     */
    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
    }

    public boolean isReplaced() {
        return replacedByTokenHash != null;
    }

    /**
     * Device types for categorization
     */
    public enum DeviceType {
        MOBILE,
        DESKTOP,
        TABLET,
        WEB,
        API,
        UNKNOWN
    }

    /**
     * Reasons for token revocation (for auditing and analytics)
     */
    public enum RevocationReason {
        /**
         * User explicitly logged out
         */
        LOGOUT,

        /**
         * Security breach detected
         */
        SECURITY_BREACH,

        /**
         * Token rotated (replaced with new token)
         */
        TOKEN_ROTATION,

        /**
         * System or support process revoked the token
         */
        SYSTEM_ACTION,

        /**
         * User changed password
         */
        PASSWORD_CHANGED,

        /**
         * User requested to revoke all sessions
         */
        REVOKE_ALL_SESSIONS,

        /**
         * Suspicious activity detected
         */
        SUSPICIOUS_ACTIVITY,

        /**
         * Token expired naturally
         */
        EXPIRED,

        /**
         * User deleted their account
         */
        ACCOUNT_DELETED
    }
}
