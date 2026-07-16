package com.attila.bookingsystem.repository;

import com.attila.bookingsystem.domain.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, UUID> {

    List<TimeSlot> findByServiceOfferingId(UUID serviceOfferingId);

    // Ownership-check a láncon keresztül (TimeSlot -> ServiceOffering -> Provider -> User).
    boolean existsByIdAndServiceOffering_Provider_UserId(UUID timeSlotId, UUID userId);

    /**
     * Az átfedés-ellenőrzéshez: egy adott PROVIDER (nem csak egy service_offering!)
     * összes nem törölt idősávja, ami metszi a [startTime, endTime) intervallumot.
     * Providerre szűrünk, mert egy szolgáltató fizikailag nem tud egyszerre két
     * különböző saját szolgáltatásában (pl. "Hajvágás" és "Szakállvágás") is
     * időpontot adni - az ütközés a PROVIDER szintjén értelmezendő, nem a
     * service_offering szintjén.
     *
     * PESSIMISTIC_WRITE lock: a megtalált (meglévő) sorokat zárolja a tranzakció
     * végéig, hogy két egyidejű kérés ne tudjon ugyanazon meglévő slot(ok) mellett
     * egyszerre "szabadnak" látszó rést kihasználni. Ez FONTOS KORLÁTTAL jár: ha
     * épp NULLA meglévő átfedő sor van, nincs mit zárolni - két teljesen új,
     * egymást átfedő slot egyidejű beszúrását ez önmagában NEM zárja ki
     * (klasszikus "check-then-insert" race condition). A végleges védelem egy
     * későbbi lépésben DB-szintű Postgres EXCLUDE constraint lesz - lásd a V1
     * migráció kommentjét.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ts FROM TimeSlot ts
            WHERE ts.serviceOffering.provider.id = :providerId
              AND ts.status <> com.attila.bookingsystem.domain.enums.TimeSlotStatus.CANCELLED
              AND ts.startTime < :endTime
              AND ts.endTime > :startTime
            """)
    List<TimeSlot> findOverlappingForProvider(
            @Param("providerId") UUID providerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * Foglaláskor EZT kell használni sima findById helyett: itt VAN egy létező
     * sor (maga a time_slot), amit a tranzakció végéig zárolunk, mielőtt
     * ellenőriznénk/módosítanánk a status-t. Ez - a fenti findOverlappingForProvider-rel
     * ellentétben - tényleg kizárja a race condition-t, mert két egyidejű foglalási
     * kérés nem futhat át egyszerre ugyanazon a soron: a második a COMMIT-ig blokkol,
     * és utána már BOOKED státuszt lát.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.id = :id")
    Optional<TimeSlot> findByIdForUpdate(@Param("id") UUID id);
}
