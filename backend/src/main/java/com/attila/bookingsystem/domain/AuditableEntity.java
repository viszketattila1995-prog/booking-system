package com.attila.bookingsystem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

/**
 * created_at/updated_at kezelése Java oldalon (@PrePersist/@PreUpdate), NEM a DB
 * trigger(ek)re bízva egyedüli forrásként: így az entitás azonnal, DB round-trip
 * nélkül tartalmazza a helyes időbélyeget persist()/merge() után is (pl. amikor a
 * service réteg rögtön DTO-vá alakítja a választ). A migrációban lévő
 * set_updated_at() trigger emiatt csak védőháló (pl. direkt SQL írásokhoz), nem az
 * egyetlen igazságforrás.
 */
@MappedSuperclass
public abstract class AuditableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
