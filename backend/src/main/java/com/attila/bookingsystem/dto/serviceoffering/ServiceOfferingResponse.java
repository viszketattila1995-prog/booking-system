package com.attila.bookingsystem.dto.serviceoffering;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceOfferingResponse(
        UUID id,
        UUID providerId,
        String name,
        String description,
        int durationMinutes,
        BigDecimal price,
        boolean active
) {
}
