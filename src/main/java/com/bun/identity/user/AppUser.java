/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bun.identity.user;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.bun.identity.exception.IdentityException;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "app_users")
@SoftDelete(strategy = SoftDeleteType.DELETED, columnName = "deleted")
public class AppUser {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotEmpty
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String name;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private IdentityProvider provider = IdentityProvider.LOCAL;

    @Column(name = "provider_id", unique = true, length = 255)
    private String providerId;

    @Column(name = "guest_device_id", unique = true, length = 36)
    private String guestDeviceId;

    @Column(unique = true, length = 50)
    private String phone;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "app_user_id"))
    @Column(name = "role")
    private Set<Role> roles;

    // ============================================================
    // Account Status Fields (UserDetails)
    // ============================================================

    /**
     * Whether the account is enabled.
     * Disabled accounts cannot authenticate.
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * Whether the account is locked.
     * Locked accounts cannot authenticate (e.g., after multiple failed login
     * attempts).
     */
    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private boolean accountLocked = false;

    /**
     * When the account expires (null means never expires).
     * Expired accounts cannot authenticate.
     */
    @Column(name = "account_expires_at")
    private Instant accountExpiresAt;

    /**
     * When the password/credentials expire (null means never expires).
     * Users must change password after this date.
     */
    @Column(name = "credentials_expire_at")
    private Instant credentialsExpireAt;

    /**
     * Number of consecutive failed login attempts.
     * Used for account locking logic.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    /**
     * When the account was locked (null if not locked).
     */
    @Column(name = "locked_at")
    private Instant lockedAt;

    /**
     * Last successful login timestamp.
     */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public boolean isAccountNonExpired() {
        if (accountExpiresAt == null) {
            return true;
        }
        return Instant.now().isBefore(accountExpiresAt);
    }

    /**
     * Indicates whether the user is locked or unlocked.
     * A locked user cannot be authenticated.
     * 
     * @return true if the user is not locked, false otherwise
     */
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     * Expired credentials prevent authentication.
     * 
     * @return true if the user's credentials are valid (non-expired), false
     *         otherwise
     */
    public boolean isCredentialsNonExpired() {
        if (credentialsExpireAt == null) {
            return true;
        }
        return Instant.now().isBefore(credentialsExpireAt);
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * A disabled user cannot be authenticated.
     * 
     * @return true if the user is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Lock the account (e.g., after multiple failed login attempts).
     */
    public void lockAccount() {
        this.accountLocked = true;
        this.lockedAt = Instant.now();
    }

    /**
     * Unlock the account.
     */
    public void unlockAccount() {
        this.accountLocked = false;
        this.lockedAt = null;
        this.failedLoginAttempts = 0;
    }

    /**
     * Increment failed login attempts.
     * Optionally lock account after threshold.
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    /**
     * Reset failed login attempts (e.g., after successful login).
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    /**
     * Record successful login.
     */
    public void recordSuccessfulLogin() {
        this.lastLoginAt = Instant.now();
        this.failedLoginAttempts = 0;
    }

    public void assertCanAuthenticate() {
        if (!isEnabled()) {
            throw IdentityException.forbidden("User account is disabled");
        }
        if (!isAccountNonLocked()) {
            throw IdentityException.forbidden("User account is locked");
        }
        if (!isAccountNonExpired()) {
            throw IdentityException.forbidden("User account has expired");
        }
        if (!isCredentialsNonExpired()) {
            throw IdentityException.forbidden("User credentials have expired");
        }
    }

    public void assertGuestUpgradeCandidate() {
        if (!hasRole(Role.ROLE_GUEST)) {
            throw IdentityException.conflict("Only guest users can be upgraded in place");
        }
    }

    /**
     * Disable the account.
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * Enable the account.
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * Set account expiration date.
     */
    public void setAccountExpiration(Instant expiresAt) {
        this.accountExpiresAt = expiresAt;
    }

    /**
     * Set password expiration date.
     */
    public void setPasswordExpiration(Instant expiresAt) {
        this.credentialsExpireAt = expiresAt;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles == null ? null : new HashSet<>(roles);
    }

    public void addRole(Role role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(role);
        setRoles(this.roles);
    }

    public boolean hasRole(Role role) {
        return this.roles != null && this.roles.contains(role);
    }

}
