package com.attila.bookingsystem.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyInUseException extends ApiException {

    public EmailAlreadyInUseException(String email) {
        super(HttpStatus.CONFLICT, "Email already in use: " + email);
    }
}
