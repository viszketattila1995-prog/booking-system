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

    // Az ownership-checket a controller végzi @PreAuthorize("@ownership.isServiceOfferingOwner(#id)")
    // formában (lásd ServiceOfferingController) - itt már csak a jogos módosítás fut le.
    @Transactional
    public ServiceOfferingResponse update(UUID id, UpdateServiceOfferingRequest request) {
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
