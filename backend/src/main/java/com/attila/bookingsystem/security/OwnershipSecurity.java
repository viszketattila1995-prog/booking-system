package com.attila.bookingsystem.security;

import com.attila.bookingsystem.repository.ServiceOfferingRepository;
import com.attila.bookingsystem.repository.TimeSlotRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @PreAuthorize kifejezésekből hívható ownership-ellenőrzések ("@ownership.xyz(#id)"
 * SpEL bean-referencia). A role-alapú korlátozás (hasRole(...)) statikus, kiolvasható
 * magából a JWT-ből - az ownership viszont mindig egy DB lekérdezést igényel (tényleg
 * a bejelentkezett user erőforrása-e ez), ezért ezt itt, egy külön bean-ben tartjuk,
 * nem a JWT claim-ekből próbáljuk kifejezni.
 *
 * Csak azokra a végpontokra vezetjük be, ahol az ownership check ÖNMAGÁBAN elég -
 * a BookingService.cancel()-nél szándékosan NEM ezt használjuk, mert ott a
 * guest/provider eldöntése (isGuestOwner) magának az üzleti logikának is kell
 * (melyik CANCELLED_BY_* státuszt kapja a booking), tehát a lekérdezést úgyis el
 * kellene végezni a service-ben - egy külön @PreAuthorize ownership-check ott
 * csak egy felesleges, duplikált DB-hívás lenne.
 */
@Component("ownership")
public class OwnershipSecurity {

    private final CurrentUserProvider currentUserProvider;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final TimeSlotRepository timeSlotRepository;

    public OwnershipSecurity(CurrentUserProvider currentUserProvider, ServiceOfferingRepository serviceOfferingRepository,
                              TimeSlotRepository timeSlotRepository) {
        this.currentUserProvider = currentUserProvider;
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    public boolean isServiceOfferingOwner(UUID serviceOfferingId) {
        return serviceOfferingRepository.existsByIdAndProvider_UserId(serviceOfferingId, currentUserId());
    }

    public boolean isTimeSlotOwner(UUID timeSlotId) {
        return timeSlotRepository.existsByIdAndServiceOffering_Provider_UserId(timeSlotId, currentUserId());
    }

    private UUID currentUserId() {
        return currentUserProvider.getCurrentUser().getId();
    }
}
