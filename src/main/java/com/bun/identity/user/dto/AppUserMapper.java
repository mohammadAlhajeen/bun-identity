package com.bun.identity.user.dto;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.bun.identity.exception.IdentityException;
import com.bun.identity.user.AppUser;

@Component
public class AppUserMapper {
    public void updateDtoToAppUser(AppUser appUser, UpdateAppUserDto updateAppUserDto) {
        appUser.setName(Optional.ofNullable(updateAppUserDto.name()).orElse(appUser.getName()));
        appUser.setPhone(Optional.ofNullable(updateAppUserDto.phone()).orElse(appUser.getPhone()));
    }

    public AppUserInfoDto toUserInfo(AppUser appUser) {
        if (appUser == null) {
            throw IdentityException.notFound("User not found");
        }
        return new AppUserInfoDto(
                appUser.getName(),
                appUser.getPhone(),
                appUser.getUsername());
    }

    public AppUser createAppUser(String username, String password, String name, String phone) {
        AppUser appUser = new AppUser();
        appUser.setName(name);
        appUser.setPassword(password);
        appUser.setPhone(phone);
        appUser.setUsername(username);
        appUser.setEnabled(true);
        appUser.setAccountLocked(false);
        return appUser;
    }

}
