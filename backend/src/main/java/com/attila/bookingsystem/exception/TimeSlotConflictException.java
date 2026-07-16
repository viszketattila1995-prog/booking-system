package com.attila.bookingsystem.exception;

import org.springframework.http.HttpStatus;

public class TimeSlotConflictException extends ApiException {

    public TimeSlotConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
