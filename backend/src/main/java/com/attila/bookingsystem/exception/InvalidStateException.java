package com.attila.bookingsystem.exception;

import org.springframework.http.HttpStatus;

/**
 * Az entitás jelenlegi állapota nem engedi meg a kért műveletet
 * (pl. már lemondott foglalás lemondása, nem APPROVED provider szolgáltatás-létrehozása).
 */
public class InvalidStateException extends ApiException {

    public InvalidStateException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
