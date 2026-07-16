package com.attila.bookingsystem.domain;

import com.attila.bookingsystem.domain.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Egy vendég foglalása egy time_slot-ra. Szándékosan nincs hard UNIQUE(time_slot_id):
 * a foglalási előzmény (lemondás után újrafoglalás) megmarad, csak egy adott
 * time_slot-hoz egyszerre max. egy CONFIRMED booking tartozhat - ezt a DB egy
 * parciális unique indexszel kényszeríti ki (lásd V1 migráció).
 */
@Entity
@Table(name = "booking")
@Getter
@Setter
public class Booking extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id")
    private TimeSlot timeSlot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_user_id")
    private AppUser guest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Booking() {
    }

    public Booking(TimeSlot timeSlot, AppUser guest) {
        this.timeSlot = timeSlot;
        this.guest = guest;
        this.bookedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
