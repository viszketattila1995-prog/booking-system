package com.attila.bookingsystem.exception;

import org.springframework.http.HttpStatus;

/**
 * Olyan érvénytelen bemenetre, amit egy @Valid annotáció önmagában nem tud kifejezni
 * (pl. mezők közötti összefüggés: endTime > startTime) - ezért ez service-szintű
 * validáció, nem a Bean Validation réteg.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
