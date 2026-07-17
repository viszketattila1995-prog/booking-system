package com.attila.bookingsystem.service;

import com.attila.bookingsystem.domain.AppUser;
import com.attila.bookingsystem.domain.Booking;
import com.attila.bookingsystem.domain.TimeSlot;
import com.attila.bookingsystem.domain.enums.BookingStatus;
import com.attila.bookingsystem.domain.enums.TimeSlotStatus;
import com.attila.bookingsystem.dto.booking.BookingResponse;
import com.attila.bookingsystem.exception.InvalidStateException;
import com.attila.bookingsystem.exception.ResourceNotFoundException;
import com.attila.bookingsystem.repository.BookingRepository;
import com.attila.bookingsystem.repository.TimeSlotRepository;
import com.attila.bookingsystem.security.CurrentUserProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    public BookingService(BookingRepository bookingRepository, TimeSlotRepository timeSlotRepository,
                           CurrentUserProvider currentUserProvider, AuditService auditService) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    @Transactional
    public BookingResponse book(UUID timeSlotId) {
        AppUser guest = currentUserProvider.getCurrentUser();

        // findByIdForUpdate zárolja a sort a tranzakció végéig - lásd a repository
        // komment. Enélkül két egyidejű foglalási kérés mindkettő AVAILABLE-nek
        // látná a slotot, és mindkettő létrehozna egy CONFIRMED bookingot (a DB-beli
        // parciális unique index az egyiket úgyis elutasítaná, de csúnya, nem kezelt
        // constraint violation formájában - ez itt tisztán, üzleti hibaként kezeli).
        TimeSlot timeSlot = timeSlotRepository.findByIdForUpdate(timeSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found: " + timeSlotId));

        if (timeSlot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new InvalidStateException("Time slot is not available (status: " + timeSlot.getStatus() + ")");
        }

        timeSlot.setStatus(TimeSlotStatus.BOOKED);

        Booking booking = new Booking(timeSlot, guest);
        bookingRepository.save(booking);

        auditService.record(guest, "BOOKING_CREATED", "Booking", booking.getId(),
                Map.of("timeSlotId", timeSlotId));

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listMine() {
        AppUser guest = currentUserProvider.getCurrentUser();
        return bookingRepository.findByGuestId(guest.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void cancel(UUID bookingId) {
        AppUser currentUser = currentUserProvider.getCurrentUser();

        boolean isGuestOwner = bookingRepository.existsByIdAndGuestId(bookingId, currentUser.getId());
        boolean isProviderOwner = bookingRepository.existsByIdAndTimeSlot_ServiceOffering_Provider_UserId(bookingId, currentUser.getId());

        if (!isGuestOwner && !isProviderOwner) {
            throw new AccessDeniedException("Not a party to this booking");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidStateException("Booking is not in a cancellable state (status: " + booking.getStatus() + ")");
        }

        booking.setStatus(isGuestOwner ? BookingStatus.CANCELLED_BY_GUEST : BookingStatus.CANCELLED_BY_PROVIDER);
        booking.setCancelledAt(Instant.now());

        booking.getTimeSlot().setStatus(TimeSlotStatus.AVAILABLE);

        auditService.record(currentUser, "BOOKING_CANCELLED", "Booking", booking.getId(),
                Map.of("cancelledBy", isGuestOwner ? "GUEST" : "PROVIDER"));
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getTimeSlot().getId(),
                booking.getGuest().getId(),
                booking.getStatus(),
                booking.getBookedAt(),
                booking.getCancelledAt()
        );
    }
}
