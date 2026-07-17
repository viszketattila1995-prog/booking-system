package com.attila.bookingsystem.controller;

import com.attila.bookingsystem.dto.assistant.ChatRequest;
import com.attila.bookingsystem.dto.assistant.ChatResponse;
import com.attila.bookingsystem.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Nincs role-restrikció: bármelyik bejelentkezett user (guest/provider/admin)
// használhatja az asszisztenst - a tool-ok maguk mindig a hívó SAJÁT
// erőforrásaira korlátozódnak (lásd AssistantToolExecutor).
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(assistantService.chat(request));
    }
}
