package com.attila.bookingsystem.controller;

import com.attila.bookingsystem.dto.serviceoffering.CreateServiceOfferingRequest;
import com.attila.bookingsystem.dto.serviceoffering.ServiceOfferingResponse;
import com.attila.bookingsystem.dto.serviceoffering.UpdateServiceOfferingRequest;
import com.attila.bookingsystem.service.ServiceOfferingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ServiceOfferingController {

    private final ServiceOfferingService serviceOfferingService;

    public ServiceOfferingController(ServiceOfferingService serviceOfferingService) {
        this.serviceOfferingService = serviceOfferingService;
    }

    @PostMapping("/api/providers/me/service-offerings")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ServiceOfferingResponse> create(@Valid @RequestBody CreateServiceOfferingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceOfferingService.create(request));
    }

    @GetMapping("/api/providers/me/service-offerings")
    @PreAuthorize("hasRole('PROVIDER')")
    public List<ServiceOfferingResponse> listMine() {
        return serviceOfferingService.listMine();
    }

    @PutMapping("/api/providers/me/service-offerings/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ServiceOfferingResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateServiceOfferingRequest request) {
        return serviceOfferingService.update(id, request);
    }

    @DeleteMapping("/api/providers/me/service-offerings/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        serviceOfferingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Publikus böngészés: vendégek (bejelentkezés nélkül is - ez authenticated()
    // mögött van most, mert a SecurityConfig anyRequest().authenticated()-et ír elő;
    // ha ezt tényleg publikussá akarjuk tenni bejelentkezés nélkül is, az a
    // SecurityConfig permitAll listáját bővítené - egyelőre bejelentkezett usernek
    // is elérhető, ez elég a CRUD kör teszteléséhez).
    @GetMapping("/api/providers/{providerId}/service-offerings")
    public List<ServiceOfferingResponse> listActiveForProvider(@PathVariable UUID providerId) {
        return serviceOfferingService.listActiveForProvider(providerId);
    }
}
