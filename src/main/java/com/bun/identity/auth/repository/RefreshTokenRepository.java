package com.bun.identity.auth.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bun.identity.auth.RefreshToken;
import com.bun.identity.user.AppUser;

import jakarta.persistence.LockModeType;

/**
 * Repository for RefreshToken operations.
 * Handles refresh token storage, validation, and revocation.
 *
 * @author Mohammad
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

        /**
         * Find refresh token by token hash
         *
         * @param tokenHash the SHA-256 hash of the token
         * @return Optional containing the refresh token if found
         */
        Optional<RefreshToken> findByTokenHash(String tokenHash);

        /**
         * Find refresh token by hash with pessimistic write lock (FOR UPDATE)
         * Prevents concurrent refresh attempts
         *
         * @param tokenHash the SHA-256 hash of the token
         * @return Optional containing the locked refresh token
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT rt FROM RefreshToken rt LEFT JOIN FETCH rt.appUser WHERE rt.tokenHash = :tokenHash")
        Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

        /**
         * Find latest active refresh token for specific user and device
         *
         * @param user     the AppUser entity
         * @param deviceId the device identifier
         * @param now      current timestamp
         * @return Optional containing the latest active token
         */
        @Query("SELECT rt FROM RefreshToken rt WHERE rt.appUser = :user " +
                        "AND rt.deviceId = :deviceId " +
                        "AND rt.revokedAt IS NULL " +
                        "AND rt.replacedByTokenHash IS NULL " +
                        "AND rt.expiresAt > :now " +
                        "ORDER BY rt.id DESC LIMIT 1")
        Optional<RefreshToken> findLatestActiveForDevice(@Param("user") AppUser user,
                        @Param("deviceId") String deviceId,
                        @Param("now") Instant now);

        /**
         * Check if token hash exists
         *
         * @param tokenHash the SHA-256 hash
         * @return true if exists
         */
        boolean existsByTokenHash(String tokenHash);

        @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash " +
                        "AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
        Optional<RefreshToken> findValidByTokenHash(@Param("tokenHash") String tokenHash,
                        @Param("now") Instant now);

        /**
         * Find all tokens for a user
         *
         * @param user the AppUser entity
         * @return List of refresh tokens
         */
        @Query("SELECT rt FROM RefreshToken rt WHERE rt.appUser = :user")
        List<RefreshToken> findByUser(@Param("user") AppUser user);

        /**
         * Find all valid tokens for a user
         *
         * @param user the AppUser entity
         * @param now  current timestamp for expiration check
         * @return List of valid refresh tokens
         */
        @Query("SELECT rt FROM RefreshToken rt WHERE rt.appUser = :user " +
                        "AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
        List<RefreshToken> findValidByUser(@Param("user") AppUser user,
                        @Param("now") Instant now);

        /**
         * Find all tokens for a specific device
         *
         * @param user     the AppUser entity
         * @param deviceId the device identifier
         * @return List of refresh tokens for the device
         */
        @Query("SELECT rt FROM RefreshToken rt WHERE rt.appUser = :user AND rt.deviceId = :deviceId")
        List<RefreshToken> findByUserAndDeviceId(@Param("user") AppUser user,
                        @Param("deviceId") String deviceId);

        /**
         * Find valid token for a specific device
         *
         * @param user     the AppUser entity
         * @param deviceId the device identifier
         * @param now      current timestamp for expiration check
         * @return Optional containing the valid refresh token if found
         */
        @Query("SELECT rt FROM RefreshToken rt WHERE rt.appUser = :user AND rt.deviceId = :deviceId " +
                        "AND rt.revokedAt IS NULL AND rt.expiresAt > :now " +
                        "ORDER BY rt.createdAt DESC LIMIT 1")
        Optional<RefreshToken> findValidByUserAndDeviceId(@Param("user") AppUser user,
                        @Param("deviceId") String deviceId,
                        @Param("now") Instant now);

        /**
         * Count valid tokens for a user
         *
         * @param user the AppUser entity
         * @param now  current timestamp for expiration check
         * @return number of valid tokens
         */
        @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.appUser = :user " +
                        "AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
        long countValidByUser(@Param("user") AppUser user, @Param("now") Instant now);

        /**
         * Count valid tokens for a user and device
         *
         * @param user     the AppUser entity
         * @param deviceId the device identifier
         * @param now      current timestamp for expiration check
         * @return number of valid tokens
         */
        @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.appUser = :user AND rt.deviceId = :deviceId " +
                        "AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
        long countValidByUserAndDeviceId(@Param("user") AppUser user,
                        @Param("deviceId") String deviceId,
                        @Param("now") Instant now);

        /**
         * Revoke a specific token
         *
         * @param tokenHash the SHA-256 hash of the token
         * @param reason    the revocation reason
         * @param now       current timestamp
         * @return number of records updated
         */
        @Modifying
        @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now, rt.revokedReason = :reason " +
                        "WHERE rt.tokenHash = :tokenHash AND rt.revokedAt IS NULL")
        int revokeByTokenHash(@Param("tokenHash") String tokenHash,
                        @Param("reason") RefreshToken.RevocationReason reason,
                        @Param("now") Instant now);

        /**
         * Revoke all tokens for a user
         *
         * @param user   the AppUser entity
         * @param reason the revocation reason
         * @param now    current timestamp
         * @return number of records updated
         */
        @Modifying
        @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now, rt.revokedReason = :reason " +
                        "WHERE rt.appUser = :user AND rt.revokedAt IS NULL")
        int revokeAllByUser(@Param("user") AppUser user,
                        @Param("reason") RefreshToken.RevocationReason reason,
                        @Param("now") Instant now);

        /**
         * Revoke all tokens for a user except the current one
         *
         * @param user             the AppUser entity
         * @param currentTokenHash the hash of the current token to keep
         * @param reason           the revocation reason
         * @param now              current timestamp
         * @return number of records updated
         */
        @Modifying
        @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now, rt.revokedReason = :reason " +
                        "WHERE rt.appUser = :user AND rt.tokenHash != :currentTokenHash AND rt.revokedAt IS NULL")
        int revokeAllByUserExcept(@Param("user") AppUser user,
                        @Param("currentTokenHash") String currentTokenHash,
                        @Param("reason") RefreshToken.RevocationReason reason,
                        @Param("now") Instant now);

        /**
         * Revoke all tokens for a specific device
         *
         * @param user     the AppUser entity
         * @param deviceId the device identifier
         * @param reason   the revocation reason
         * @param now      current timestamp
         * @return number of records updated
         */
        @Modifying
        @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now, rt.revokedReason = :reason " +
                        "WHERE rt.appUser = :user AND rt.deviceId = :deviceId AND rt.revokedAt IS NULL")
        int revokeAllByUserAndDeviceId(@Param("user") AppUser user,
                        @Param("deviceId") String deviceId,
                        @Param("reason") RefreshToken.RevocationReason reason,
                        @Param("now") Instant now);

        /**
         * Delete all expired tokens (cleanup job)
         *
         * @param now current timestamp
         * @return number of records deleted
         */
        @Modifying
        @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
        int deleteExpired(@Param("now") Instant now);

        /**
         * Delete all revoked tokens older than specified date (cleanup job)
         *
         * @param cutoffDate the date before which to delete
         * @return number of records deleted
         */
        @Modifying
        @Query("DELETE FROM RefreshToken rt WHERE rt.revokedAt IS NOT NULL AND rt.revokedAt < :cutoffDate")
        int deleteRevokedBefore(@Param("cutoffDate") Instant cutoffDate);

        /**
         * Find tokens that need rotation (old tokens that haven't been rotated)
         *
         * @param user              the AppUser entity
         * @param rotationThreshold the age threshold for rotation
         * @param now               current timestamp
         * @return List of tokens that should be rotated
         */
        @Query("SELECT rt FROM RefreshToken rt WHERE rt.appUser = :user " +
                        "AND rt.revokedAt IS NULL AND rt.expiresAt > :now " +
                        "AND rt.createdAt < :rotationThreshold AND rt.replacedByTokenHash IS NULL")
        List<RefreshToken> findTokensNeedingRotation(@Param("user") AppUser user,
                        @Param("rotationThreshold") Instant rotationThreshold,
                        @Param("now") Instant now);

        @Modifying
        @Query("""
                            DELETE FROM RefreshToken rt
                            WHERE
                                (rt.expiresAt IS NOT NULL AND rt.expiresAt < :cutoff)
                             OR (rt.revokedAt IS NOT NULL AND rt.revokedAt < :cutoff)
                        """)
        int deleteExpiredAndRevokedBefore(@Param("cutoff") Instant cutoff);
}
