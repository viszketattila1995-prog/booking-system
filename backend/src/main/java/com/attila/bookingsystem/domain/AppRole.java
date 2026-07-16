package com.attila.bookingsystem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Lookup tábla: ROLE_GUEST / ROLE_PROVIDER / ROLE_ADMIN (seed adat a V1 migrációban).
 * A "ROLE_" prefix tudatosan benne van a DB-ben tárolt névben, mert Spring Security
 * hasRole("PROVIDER") automatikusan ezt a prefixet várja el az authority stringben -
 * így a UserDetailsService-ben simán role.getName()-et adhatunk át GrantedAuthority-ként.
 */
@Entity
@Table(name = "app_role")
@Getter
@Setter
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    protected AppRole() {
    }

    public AppRole(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppRole other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
