package com.attila.bookingsystem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Minden bejelentkező fiók (vendég, szolgáltató, admin egyaránt ebben tárolódik).
 * Nem @Data-t használunk, mert az entitáson generált equals/hashCode minden mezőt
 * (így a roles kollekciót is) bevonna - ez Hibernate lazy loading mellett könnyen
 * StackOverflow-hoz vagy hibás equals-hez vezet. Helyette id-alapú equals/hashCode.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    // FetchType.LAZY explicit: open-in-view ki van kapcsolva (application.yaml),
    // tehát a roles kollekció csak a service réteg tranzakcióján belül érhető el -
    // ez szándékos, hogy a controller réteg ne tudjon "véletlenül" lazy proxy-t
    // triggerelni a tranzakción kívül.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<AppRole> roles = new HashSet<>();

    protected AppUser() {
    }

    public AppUser(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppUser other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
