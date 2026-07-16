package com.attila.bookingsystem.dto.timeslot;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateTimeSlotRequest(
        @NotNull @Future Instant startTime,
        @NotNull @Future Instant endTime
) {
}
