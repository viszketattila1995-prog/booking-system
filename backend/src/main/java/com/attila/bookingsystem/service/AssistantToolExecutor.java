package com.attila.bookingsystem.service;

import com.attila.bookingsystem.domain.ServiceOffering;
import com.attila.bookingsystem.domain.TimeSlot;
import com.attila.bookingsystem.domain.enums.ProviderStatus;
import com.attila.bookingsystem.domain.enums.TimeSlotStatus;
import com.attila.bookingsystem.dto.booking.BookingResponse;
import com.attila.bookingsystem.exception.BadRequestException;
import com.attila.bookingsystem.repository.ServiceOfferingRepository;
import com.attila.bookingsystem.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Az AI asszisztens tooljainak tényleges végrehajtása. Szándékosan a MEGLÉVŐ
 * service-réteget hívja (BookingService.book/listMine/cancel) ahelyett, hogy
 * a repository-kat közvetlenül piszkálná - így az asszisztens semmit sem tud
 * megtenni, amit a bejelentkezett user a sima REST API-n keresztül ne tudna
 * megtenni (ugyanazok az ownership/state-ellenőrzések, ugyanaz az audit log).
 * A CurrentUserProvider-en keresztül mindig az AKTUÁLIS, bejelentkezett userre
 * korlátozódik minden hívás - az asszisztens sosem járhat el más user nevében.
 */
@Service
public class AssistantToolExecutor {

    private static final int MAX_SEARCH_RESULTS = 15;

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    public AssistantToolExecutor(ServiceOfferingRepository serviceOfferingRepository, TimeSlotRepository timeSlotRepository,
                                  BookingService bookingService, ObjectMapper objectMapper) {
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.bookingService = bookingService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public String searchTimeSlots(Map<String, Object> input) {
        String serviceKeyword = lower((String) input.get("serviceKeyword"));
        String providerKeyword = lower((String) input.get("providerKeyword"));
        Instant afterTime = parseInstant(input.get("afterTime"));
        Instant beforeTime = parseInstant(input.get("beforeTime"));

        List<ServiceOffering> offerings = serviceOfferingRepository.findAllActiveForProviderStatus(ProviderStatus.APPROVED).stream()
                .filter(o -> serviceKeyword == null
                        || lower(o.getName()).contains(serviceKeyword)
                        || (o.getDescription() != null && lower(o.getDescription()).contains(serviceKeyword)))
                .filter(o -> providerKeyword == null
                        || lower(o.getProvider().getOrganization().getName()).contains(providerKeyword))
                .toList();

        List<Map<String, Object>> results = new ArrayList<>();
        outer:
        for (ServiceOffering offering : offerings) {
            for (TimeSlot slot : timeSlotRepository.findByServiceOfferingId(offering.getId())) {
                if (slot.getStatus() != TimeSlotStatus.AVAILABLE) {
                    continue;
                }
                if (afterTime != null && slot.getStartTime().isBefore(afterTime)) {
                    continue;
                }
                if (beforeTime != null && !slot.getStartTime().isBefore(beforeTime)) {
                    continue;
                }

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("timeSlotId", slot.getId().toString());
                row.put("serviceOfferingName", offering.getName());
                row.put("organizationName", offering.getProvider().getOrganization().getName());
                row.put("startTime", slot.getStartTime().toString());
                row.put("endTime", slot.getEndTime().toString());
                row.put("price", offering.getPrice());
                results.add(row);
                if (results.size() >= MAX_SEARCH_RESULTS) {
                    break outer;
                }
            }
        }

        return toJson(results.isEmpty() ? Map.of("message", "No matching available time slots found.") : results);
    }

    @Transactional
    public String bookTimeSlot(Map<String, Object> input) {
        UUID timeSlotId = requireUuid(input, "timeSlotId");
        BookingResponse booking = bookingService.book(timeSlotId);
        return toJson(booking);
    }

    @Transactional(readOnly = true)
    public String listMyBookings() {
        return toJson(bookingService.listMine());
    }

    @Transactional
    public String cancelBooking(Map<String, Object> input) {
        UUID bookingId = requireUuid(input, "bookingId");
        bookingService.cancel(bookingId);
        return toJson(Map.of("cancelled", true, "bookingId", bookingId.toString()));
    }

    private UUID requireUuid(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (!(value instanceof String raw) || raw.isBlank()) {
            throw new BadRequestException("Missing required parameter: " + key);
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid " + key + ": " + raw);
        }
    }

    private Instant parseInstant(Object value) {
        if (!(value instanceof String raw) || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid ISO-8601 instant: " + raw);
        }
    }

    private String lower(String value) {
        return value == null || value.isBlank() ? null : value.toLowerCase(Locale.ROOT);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize tool result", e);
        }
    }
}
