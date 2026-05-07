package com.bun.identity.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bun.identity.user.AppUser;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    @Query("SELECT u FROM AppUser u WHERE u.username = :username")
    Optional<AppUser> findByUsername(@Param("username") String username);

    List<AppUser> findAllByUsernameIgnoreCase(String username);

    Optional<AppUser> findByProviderId(String providerId);

    Optional<AppUser> findByGuestDeviceId(String guestDeviceId);

    boolean existsByUsername(String username);

}
