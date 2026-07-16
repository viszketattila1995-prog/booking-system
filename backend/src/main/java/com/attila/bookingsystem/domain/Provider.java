package com.attila.bookingsystem.domain;

import com.attila.bookingsystem.domain.enums.ProviderStatus;
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
 * Egy user szolgáltatói "tagsága" egy szervezetben, admin jóváhagyási állapottal.
 * Ez a tábla a forrása a method-level security ownership-chain-nek:
 * TimeSlot -> ServiceOffering -> Provider -> User. Az ownership-check ezért mindig
 * ezen keresztül jut el a bejelentkezett userig, nem közvetlenül a leaf entitásból.
 */
@Entity
@Table(name = "provider")
@Getter
@Setter
public class Provider extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProviderStatus status = ProviderStatus.PENDING;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private AppUser decidedBy;

    protected Provider() {
    }

    public Provider(AppUser user, Organization organization) {
        this.user = user;
        this.organization = organization;
        this.appliedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Provider other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
