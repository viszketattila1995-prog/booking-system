package com.attila.bookingsystem.security;

import com.attila.bookingsystem.domain.AppUser;
import com.attila.bookingsystem.repository.AppUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * A JwtAuthenticationFilter a principal-t az email (subject claim) stringjeként
 * állítja be, DB hit nélkül (lásd JwtService komment). Ez a helper az egyetlen
 * hely, ahol emiatt egy DB hívással visszafejtjük a teljes AppUser entitást,
 * amikor egy service metódusnak tényleg szüksége van rá (pl. tulajdonos userId
 * megállapításához egy ownership-checkhez).
 */
@Component
public class CurrentUserProvider {

    private final AppUserRepository appUserRepository;

    public CurrentUserProvider(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}
