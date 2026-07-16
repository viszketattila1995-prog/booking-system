package com.attila.bookingsystem.exception;

import org.springframework.http.HttpStatus;

/**
 * Közös ős minden üzleti (nem validációs) kivételhez, hogy a GlobalExceptionHandler-ben
 * NE kelljen minden egyes exception típusnak külön @ExceptionHandler metódus - a
 * HTTP status maga az exception felelőssége, a handler csak kiolvassa.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
