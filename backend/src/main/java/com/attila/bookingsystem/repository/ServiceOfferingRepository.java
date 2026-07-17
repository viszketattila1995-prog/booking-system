package com.attila.bookingsystem.repository;

import com.attila.bookingsystem.domain.ServiceOffering;
import com.attila.bookingsystem.domain.enums.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    List<ServiceOffering> findByProviderId(UUID providerId);

    List<ServiceOffering> findByProviderIdAndActiveTrue(UUID providerId);

    // Ownership-check egy lépcsővel feljebb a láncban (ServiceOffering -> Provider -> User).
    boolean existsByIdAndProvider_UserId(UUID serviceOfferingId, UUID userId);

    // Az AI asszisztens keresési tooljához: a teljes böngészhető katalógus (ugyanaz
    // a szűrés, mint amit egy guest a UI-n keresztül is látna - aktív ajánlat,
    // jóváhagyott provider).
    @Query("""
            SELECT so FROM ServiceOffering so
            WHERE so.active = true AND so.provider.status = :providerStatus
            """)
    List<ServiceOffering> findAllActiveForProviderStatus(ProviderStatus providerStatus);
}
