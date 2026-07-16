package com.attila.bookingsystem.controller;

import com.attila.bookingsystem.dto.timeslot.CreateTimeSlotRequest;
import com.attila.bookingsystem.dto.timeslot.TimeSlotResponse;
import com.attila.bookingsystem.service.TimeSlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    public TimeSlotController(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    @PostMapping("/api/providers/me/service-offerings/{serviceOfferingId}/time-slots")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<TimeSlotResponse> create(@PathVariable UUID serviceOfferingId,
                                                    @Valid @RequestBody CreateTimeSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeSlotService.create(serviceOfferingId, request));
    }

    @GetMapping("/api/service-offerings/{serviceOfferingId}/time-slots")
    public List<TimeSlotResponse> listForServiceOffering(@PathVariable UUID serviceOfferingId) {
        return timeSlotService.listForServiceOffering(serviceOfferingId);
    }

    @DeleteMapping("/api/providers/me/time-slots/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        timeSlotService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
