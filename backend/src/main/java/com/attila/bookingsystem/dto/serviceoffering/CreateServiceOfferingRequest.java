package com.attila.bookingsystem.dto.serviceoffering;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateServiceOfferingRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @Positive int durationMinutes,
        @NotNull @PositiveOrZero BigDecimal price
) {
}
