package com.attila.bookingsystem.service;

import com.attila.bookingsystem.config.JwtProperties;
import com.attila.bookingsystem.domain.AppUser;
import com.attila.bookingsystem.domain.RefreshToken;
import com.attila.bookingsystem.exception.InvalidRefreshTokenException;
import com.attila.bookingsystem.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * A nyers refresh tokent SOHA nem tároljuk, csak a SHA-256 hash-ét (lásd RefreshToken
 * entitás komment). Rotációnál a felhasznált token azonnal revoke-olódik, és egy új
 * token váltja - ha egy MÁR revoke-olt token hash-ére érkezik újabb rotációs kérés,
 * az azt jelzi, hogy valaki egy korábban felhasznált (feltehetően ellopott) refresh
 * tokent próbál újra beváltani. Ez a "refresh token reuse detection": ilyenkor a
 * user összes aktív refresh tokenjét visszavonjuk, kényszerítve egy új bejelentkezést
 * minden eszközön - még akkor is, ha a jelenlegi kérés magától a jogos usertől jön,
 * mert nem tudjuk megkülönböztetni ezt a támadó általi visszajátszástól.
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTE_LENGTH = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties,
                                PlatformTransactionManager transactionManager) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public String issue(AppUser user) {
        String rawToken = generateRawToken();
        RefreshToken token = new RefreshToken(user, hash(rawToken), newExpiry());
        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (current.isRevoked()) {
            // A hívó tranzakciója a lenti throw miatt úgyis rollback-elne - és azzal
            // MAGÁT a védekező revoke-ot is visszavonná. Ezért ezt egy önálló, azonnal
            // commit-olt (REQUIRES_NEW) tranzakcióban végezzük, hogy a revoke akkor is
            // megtörténjen és megmaradjon, ha ez a kérés maga csak egy visszajátszás volt.
            UUID userId = current.getUser().getId();
            requiresNewTransactionTemplate.executeWithoutResult(status -> revokeAllForUser(userId));
            throw new InvalidRefreshTokenException("Refresh token reuse detected - all sessions revoked");
        }

        if (current.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        AppUser user = current.getUser();
        String newRawToken = generateRawToken();
        RefreshToken next = new RefreshToken(user, hash(newRawToken), newExpiry());
        refreshTokenRepository.save(next);

        current.setRevoked(true);
        current.setRevokedAt(Instant.now());
        current.setReplacedByToken(next);

        return new RotationResult(user, newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    token.setRevokedAt(Instant.now());
                });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        Instant now = Instant.now();
        active.forEach(token -> {
            token.setRevoked(true);
            token.setRevokedAt(now);
        });
    }

    private Instant newExpiry() {
        return Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RotationResult(AppUser user, String rawToken) {
    }
}
