package com.attila.bookingsystem.exception;

import org.springframework.http.HttpStatus;

/**
 * Hiányzó, lejárt, visszavont vagy - reuse-detektálás esetén - kompromittáltnak
 * tekintett refresh tokenre. Szándékosan 401, ugyanúgy, mint egy sima hibás
 * bejelentkezésnél - a kliens teendője ugyanaz: újra be kell jelentkezni.
 */
public class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
