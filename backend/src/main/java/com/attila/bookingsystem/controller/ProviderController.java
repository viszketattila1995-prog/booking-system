package com.attila.bookingsystem.controller;

import com.attila.bookingsystem.dto.provider.ApplyProviderRequest;
import com.attila.bookingsystem.dto.provider.ProviderResponse;
import com.attila.bookingsystem.service.ProviderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    // Bármelyik bejelentkezett user (alapból ROLE_GUEST) jelentkezhet szolgáltatónak.
    @PostMapping("/api/providers/apply")
    public ResponseEntity<ProviderResponse> apply(@Valid @RequestBody ApplyProviderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerService.apply(request));
    }

    // Publikus böngészés (jóváhagyott providerek listája) - bejelentkezett usernek
    // elérhető, lásd ServiceOfferingController.listActiveForProvider komment.
    @GetMapping("/api/providers")
    public List<ProviderResponse> listApproved() {
        return providerService.listApproved();
    }

    // A bejelentkezett user saját jelentkezésének állapota. 204, ha még nem
    // jelentkezett - ez NEM hibaállapot, csak jelzi, hogy az apply űrlapot kell mutatni.
    @GetMapping("/api/providers/me")
    public ResponseEntity<ProviderResponse> getMine() {
        return providerService.findMine()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/api/admin/providers/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProviderResponse> listPending() {
        return providerService.listPending();
    }

    @PostMapping("/api/admin/providers/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ProviderResponse approve(@PathVariable UUID id) {
        return providerService.approve(id);
    }

    @PostMapping("/api/admin/providers/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ProviderResponse reject(@PathVariable UUID id) {
        return providerService.reject(id);
    }
}
