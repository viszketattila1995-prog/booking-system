package com.attila.bookingsystem.repository;

import com.attila.bookingsystem.domain.Provider;
import com.attila.bookingsystem.domain.enums.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderRepository extends JpaRepository<Provider, UUID> {

    Optional<Provider> findByUserId(UUID userId);

    List<Provider> findByStatus(ProviderStatus status);

    List<Provider> findByOrganizationId(UUID organizationId);

    // Gyors ownership-check (pl. egy custom @PreAuthorize security expression-ben),
    // hogy a bejelentkezett user tényleg a megadott provider mögötti user-e -
    // anélkül, hogy a teljes Provider entitást be kellene tölteni.
    boolean existsByIdAndUserId(UUID providerId, UUID userId);
}
