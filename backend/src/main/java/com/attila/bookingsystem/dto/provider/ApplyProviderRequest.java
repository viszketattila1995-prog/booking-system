package com.attila.bookingsystem.dto.provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplyProviderRequest(
        @NotBlank @Size(max = 255) String organizationName,
        @Size(max = 2000) String organizationDescription
) {
}
