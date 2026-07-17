package com.attila.bookingsystem.service;

import com.attila.bookingsystem.domain.ServiceOffering;
import com.attila.bookingsystem.domain.TimeSlot;
import com.attila.bookingsystem.domain.enums.TimeSlotStatus;
import com.attila.bookingsystem.dto.timeslot.CreateTimeSlotRequest;
import com.attila.bookingsystem.dto.timeslot.TimeSlotResponse;
import com.attila.bookingsystem.exception.BadRequestException;
import com.attila.bookingsystem.exception.InvalidStateException;
import com.attila.bookingsystem.exception.ResourceNotFoundException;
import com.attila.bookingsystem.exception.TimeSlotConflictException;
import com.attila.bookingsystem.repository.ServiceOfferingRepository;
import com.attila.bookingsystem.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;

    public TimeSlotService(TimeSlotRepository timeSlotRepository, ServiceOfferingRepository serviceOfferingRepository) {
        this.timeSlotRepository = timeSlotRepository;
        this.serviceOfferingRepository = serviceOfferingRepository;
    }

    // Az ownership-checket a controller végzi @PreAuthorize("@ownership.isServiceOfferingOwner(#serviceOfferingId)")
    // formában (lásd TimeSlotController) - itt már csak a jogos létrehozás fut le.
    @Transactional
    public TimeSlotResponse create(UUID serviceOfferingId, CreateTimeSlotRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }

        ServiceOffering offering = serviceOfferingRepository.findById(serviceOfferingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service offering not found: " + serviceOfferingId));

        // A providerId a service_offering-en keresztül jön, mert az ütközést a
        // PROVIDER szintjén nézzük (lásd TimeSlotRepository komment): egy szolgáltató
        // nem adhat időpontot két különböző saját szolgáltatásában egyszerre sem.
        UUID providerId = offering.getProvider().getId();
        List<TimeSlot> overlapping = timeSlotRepository.findOverlappingForProvider(
                providerId, request.startTime(), request.endTime());
        if (!overlapping.isEmpty()) {
            throw new TimeSlotConflictException("This time range overlaps with an existing time slot for this provider");
        }

        TimeSlot timeSlot = new TimeSlot(offering, request.startTime(), request.endTime());
        timeSlotRepository.save(timeSlot);

        return toResponse(timeSlot);
    }

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> listForServiceOffering(UUID serviceOfferingId) {
        return timeSlotRepository.findByServiceOfferingId(serviceOfferingId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Az ownership-checket a controller végzi @PreAuthorize("@ownership.isTimeSlotOwner(#id)")
    // formában (lásd TimeSlotController) - itt már csak a jogos lemondás fut le.
    @Transactional
    public void cancel(UUID timeSlotId) {
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found: " + timeSlotId));

        // Már lefoglalt slotot nem lehet itt közvetlenül törölni - azt a foglalás
        // lemondásán (BookingService.cancel) keresztül kell rendezni, hogy a guest
        // is értesüljön/konzisztens maradjon a booking rekord állapota is.
        if (timeSlot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new InvalidStateException("Only AVAILABLE time slots can be cancelled directly (status: " + timeSlot.getStatus() + ")");
        }

        timeSlot.setStatus(TimeSlotStatus.CANCELLED);
    }

    private TimeSlotResponse toResponse(TimeSlot timeSlot) {
        return new TimeSlotResponse(
                timeSlot.getId(),
                timeSlot.getServiceOffering().getId(),
                timeSlot.getStartTime(),
                timeSlot.getEndTime(),
                timeSlot.getStatus()
        );
    }
}
