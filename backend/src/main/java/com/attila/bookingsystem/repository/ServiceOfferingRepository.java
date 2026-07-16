package com.attila.bookingsystem.repository;

import com.attila.bookingsystem.domain.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    List<ServiceOffering> findByProviderId(UUID providerId);

    List<ServiceOffering> findByProviderIdAndActiveTrue(UUID providerId);

    // Ownership-check egy lépcsővel feljebb a láncban (ServiceOffering -> Provider -> User).
    boolean existsByIdAndProvider_UserId(UUID serviceOfferingId, UUID userId);
}
