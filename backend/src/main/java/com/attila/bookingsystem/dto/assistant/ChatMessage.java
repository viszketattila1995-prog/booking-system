package com.attila.bookingsystem.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChatMessage(
        @NotBlank @Pattern(regexp = "user|assistant", message = "role must be 'user' or 'assistant'") String role,
        @NotBlank String content
) {
}
