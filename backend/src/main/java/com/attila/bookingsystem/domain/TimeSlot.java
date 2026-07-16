package com.attila.bookingsystem.domain;

import com.attila.bookingsystem.domain.enums.TimeSlotStatus;
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
 * Egy konkrét, foglalható időpont egy adott service_offering-hez.
 * start/end Instant-ként (UTC): a foglalási ütközésvizsgálat és tárolás mindig
 * abszolút időpontok összehasonlítása, a helyi időzóna-megjelenítés a frontend/API
 * réteg dolga, nem a persistence rétegé.
 */
@Entity
@Table(name = "time_slot")
@Getter
@Setter
public class TimeSlot extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_offering_id")
    private ServiceOffering serviceOffering;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TimeSlotStatus status = TimeSlotStatus.AVAILABLE;

    protected TimeSlot() {
    }

    public TimeSlot(ServiceOffering serviceOffering, Instant startTime, Instant endTime) {
        this.serviceOffering = serviceOffering;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
