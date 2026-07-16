package com.attila.bookingsystem.service;

import com.attila.bookingsystem.domain.AppUser;
import com.attila.bookingsystem.domain.Provider;
import com.attila.bookingsystem.domain.ServiceOffering;
import com.attila.bookingsystem.dto.serviceoffering.CreateServiceOfferingRequest;
import com.attila.bookingsystem.dto.serviceoffering.ServiceOfferingResponse;
import com.attila.bookingsystem.dto.serviceoffering.UpdateServiceOfferingRequest;
import com.attila.bookingsystem.exception.ResourceNotFoundException;
import com.attila.bookingsystem.repository.ServiceOfferingRepository;
import com.attila.bookingsystem.security.CurrentUserProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final ProviderService providerService;
    private final CurrentUserProvider currentUserProvider;

    public ServiceOfferingService(ServiceOfferingRepository serviceOfferingRepository, ProviderService providerService,
                                   CurrentUserProvider currentUserProvider) {
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.providerService = providerService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public ServiceOfferingResponse create(CreateServiceOfferingRequest request) {
        AppUser currentUser = currentUserProvider.getCurrentUser();
        Provider provider = providerService.requireApprovedProvider(currentUser.getId());

        ServiceOffering offering = new ServiceOffering(provider, request.name(), request.description(),
                request.durationMinutes(), request.price());
        serviceOfferingRepository.save(offering);

        return toResponse(offering);
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponse> listMine() {
        AppUser currentUser = currentUserProvider.getCurrentUser();
        Provider provider = providerService.requireApprovedProvider(currentUser.getId());
        return serviceOfferingRepository.findByProviderId(provider.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponse> listActiveForProvider(UUID providerId) {
        return serviceOfferingRepository.findByProviderIdAndActiveTrue(providerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ServiceOfferingResponse update(UUID id, UpdateServiceOfferingRequest request) {
        AppUser currentUser = currentUserProvider.getCurrentUser();

        // Ownership-check: a DB-t kérdezzük meg, tényleg a bejelentkezett user
        // service_offering-je-e ez, mielőtt bármit módosítanánk rajta - nem elég,
        // hogy valaki APPROVED provider, a SAJÁT erőforrására kell korlátozódnia.
        if (!serviceOfferingRepository.existsByIdAndProvider_UserId(id, currentUser.getId())) {
            throw new AccessDeniedException("Not the owner of this service offering");
        }

        ServiceOffering offering = serviceOfferingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service offering not found: " + id));

        offering.setName(request.name());
        offering.setDescription(request.description());
        offering.setDurationMinutes(request.durationMinutes());
        offering.setPrice(request.price());
        offering.setActive(request.active());

        return toResponse(offering);
    }

    @Transactional
    public void delete(UUID id) {
        AppUser currentUser = currentUserProvider.getCurrentUser();

        if (!serviceOfferingRepository.existsByIdAndProvider_UserId(id, currentUser.getId())) {
            throw new AccessDeniedException("Not the owner of this service offering");
        }

        serviceOfferingRepository.deleteById(id);
    }

    private ServiceOfferingResponse toResponse(ServiceOffering offering) {
        return new ServiceOfferingResponse(
                offering.getId(),
                offering.getProvider().getId(),
                offering.getName(),
                offering.getDescription(),
                offering.getDurationMinutes(),
                offering.getPrice(),
                offering.isActive()
        );
    }
}
