package com.attila.bookingsystem.dto.timeslot;

import com.attila.bookingsystem.domain.enums.TimeSlotStatus;

import java.time.Instant;
import java.util.UUID;

public record TimeSlotResponse(
        UUID id,
        UUID serviceOfferingId,
        Instant startTime,
        Instant endTime,
        TimeSlotStatus status
) {
}
