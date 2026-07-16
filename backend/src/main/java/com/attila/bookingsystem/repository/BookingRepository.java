package com.attila.bookingsystem.repository;

import com.attila.bookingsystem.domain.Booking;
import com.attila.bookingsystem.domain.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByGuestId(UUID guestId);

    Optional<Booking> findByTimeSlotIdAndStatus(UUID timeSlotId, BookingStatus status);

    // Ownership-check: a bejelentkezett vendég tényleg a foglalás tulajdonosa-e (lemondáshoz).
    boolean existsByIdAndGuestId(UUID bookingId, UUID guestId);

    // Ownership-check a szolgáltató oldalára: a foglalás a saját idősávjához tartozik-e
    // (pl. amikor a provider mondja le a foglalást).
    boolean existsByIdAndTimeSlot_ServiceOffering_Provider_UserId(UUID bookingId, UUID userId);
}
