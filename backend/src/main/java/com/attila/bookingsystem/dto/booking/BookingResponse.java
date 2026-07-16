package com.attila.bookingsystem.dto.booking;

import com.attila.bookingsystem.domain.enums.BookingStatus;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID timeSlotId,
        UUID guestId,
        BookingStatus status,
        Instant bookedAt,
        Instant cancelledAt
) {
}
