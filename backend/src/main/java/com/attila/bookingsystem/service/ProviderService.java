package com.attila.bookingsystem.service;

import com.attila.bookingsystem.domain.AppRole;
import com.attila.bookingsystem.domain.AppUser;
import com.attila.bookingsystem.domain.Organization;
import com.attila.bookingsystem.domain.Provider;
import com.attila.bookingsystem.domain.enums.OrganizationStatus;
import com.attila.bookingsystem.domain.enums.ProviderStatus;
import com.attila.bookingsystem.dto.provider.ApplyProviderRequest;
import com.attila.bookingsystem.dto.provider.ProviderResponse;
import com.attila.bookingsystem.exception.InvalidStateException;
import com.attila.bookingsystem.exception.ResourceNotFoundException;
import com.attila.bookingsystem.repository.AppRoleRepository;
import com.attila.bookingsystem.repository.OrganizationRepository;
import com.attila.bookingsystem.repository.ProviderRepository;
import com.attila.bookingsystem.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProviderService {

    private static final String PROVIDER_ROLE = "ROLE_PROVIDER";

    private final ProviderRepository providerRepository;
    private final OrganizationRepository organizationRepository;
    private final AppRoleRepository appRoleRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    public ProviderService(ProviderRepository providerRepository, OrganizationRepository organizationRepository,
                            AppRoleRepository appRoleRepository, CurrentUserProvider currentUserProvider,
                            AuditService auditService) {
        this.providerRepository = providerRepository;
        this.organizationRepository = organizationRepository;
        this.appRoleRepository = appRoleRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    @Transactional
    public ProviderResponse apply(ApplyProviderRequest request) {
        AppUser currentUser = currentUserProvider.getCurrentUser();

        if (providerRepository.findByUserId(currentUser.getId()).isPresent()) {
            throw new InvalidStateException("User already has a provider application");
        }

        Organization organization = new Organization(request.organizationName(), request.organizationDescription(), currentUser);
        organizationRepository.save(organization);

        Provider provider = new Provider(currentUser, organization);
        providerRepository.save(provider);

        return toResponse(provider);
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> listPending() {
        return providerRepository.findByStatus(ProviderStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    // Publikus böngészéshez (guest a foglalás előtt providert választ) - csak
    // APPROVED állapotú providerek, PENDING/REJECTED/SUSPENDED nem jelenik meg itt.
    @Transactional(readOnly = true)
    public List<ProviderResponse> listApproved() {
        return providerRepository.findByStatus(ProviderStatus.APPROVED).stream()
                .map(this::toResponse)
                .toList();
    }

    // A bejelentkezett user saját provider-jelentkezésének állapota (ha van) -
    // ebből dönti el a kliens, hogy jelentkezési űrlapot, "elbírálás alatt"
    // üzenetet, vagy a szolgáltatás-kezelő felületet mutassa.
    @Transactional(readOnly = true)
    public Optional<ProviderResponse> findMine() {
        AppUser currentUser = currentUserProvider.getCurrentUser();
        return providerRepository.findByUserId(currentUser.getId()).map(this::toResponse);
    }

    @Transactional
    public ProviderResponse approve(UUID providerId) {
        AppUser admin = currentUserProvider.getCurrentUser();
        Provider provider = getPendingProvider(providerId);

        AppRole providerRole = appRoleRepository.findByName(PROVIDER_ROLE)
                .orElseThrow(() -> new IllegalStateException(PROVIDER_ROLE + " role missing - check V1 migration seed data"));

        provider.setStatus(ProviderStatus.APPROVED);
        provider.setDecidedAt(Instant.now());
        provider.setDecidedBy(admin);

        Organization organization = provider.getOrganization();
        organization.setStatus(OrganizationStatus.ACTIVE);

        AppUser providerUser = provider.getUser();
        var roles = new HashSet<>(providerUser.getRoles());
        roles.add(providerRole);
        providerUser.setRoles(roles);

        auditService.record(admin, "PROVIDER_APPROVED", "Provider", provider.getId(),
                Map.of("applicantUserId", providerUser.getId()));

        return toResponse(provider);
    }

    @Transactional
    public ProviderResponse reject(UUID providerId) {
        AppUser admin = currentUserProvider.getCurrentUser();
        Provider provider = getPendingProvider(providerId);

        provider.setStatus(ProviderStatus.REJECTED);
        provider.setDecidedAt(Instant.now());
        provider.setDecidedBy(admin);

        auditService.record(admin, "PROVIDER_REJECTED", "Provider", provider.getId(),
                Map.of("applicantUserId", provider.getUser().getId()));

        return toResponse(provider);
    }

    // ServiceOffering/TimeSlot service innen kéri le, hogy egy adott user tényleg
    // jóváhagyott (APPROVED) provider-e - PENDING/REJECTED/SUSPENDED alatt nem
    // hozhat létre új szolgáltatást/idősávot.
    @Transactional(readOnly = true)
    public Provider requireApprovedProvider(UUID userId) {
        Provider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No provider profile for user: " + userId));
        if (provider.getStatus() != ProviderStatus.APPROVED) {
            throw new InvalidStateException("Provider is not approved (status: " + provider.getStatus() + ")");
        }
        return provider;
    }

    private Provider getPendingProvider(UUID providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + providerId));
        if (provider.getStatus() != ProviderStatus.PENDING) {
            throw new InvalidStateException("Provider application is not pending: " + provider.getStatus());
        }
        return provider;
    }

    private ProviderResponse toResponse(Provider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getOrganization().getId(),
                provider.getOrganization().getName(),
                provider.getStatus(),
                provider.getAppliedAt(),
                provider.getDecidedAt()
        );
    }
}
