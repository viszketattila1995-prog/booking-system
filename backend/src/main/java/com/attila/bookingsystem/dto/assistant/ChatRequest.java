package com.attila.bookingsystem.dto.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// A history-t a kliens tartja fenn és küldi el minden kéréssel (stateless,
// mint a JWT-s auth) - a szerver nem tárol beszélgetés-állapotot session-önként.
public record ChatRequest(
        @NotEmpty @Size(max = 40, message = "Conversation is too long") @Valid List<ChatMessage> messages
) {
}
