package com.attila.bookingsystem.dto.provider;

import com.attila.bookingsystem.domain.enums.ProviderStatus;

import java.time.Instant;
import java.util.UUID;

public record ProviderResponse(
        UUID id,
        UUID organizationId,
        String organizationName,
        ProviderStatus status,
        Instant appliedAt,
        Instant decidedAt
) {
}
