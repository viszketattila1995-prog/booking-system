package com.attila.bookingsystem.service;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;

import java.util.List;
import java.util.Map;

/**
 * A Claude-nak felkínált toolok JSON séma szerinti leírása. Csak a leírás
 * (mit hívjon, milyen paraméterekkel) - a tényleges végrehajtás az
 * AssistantToolExecutor-ban van, teljesen elkülönítve.
 */
final class AssistantTools {

    private AssistantTools() {
    }

    static List<Tool> definitions() {
        return List.of(
                tool(
                        "search_time_slots",
                        "Search for AVAILABLE time slots the current user can book. Filter by a service name/description "
                                + "keyword, a provider/organization name keyword, and/or a time range. Omit any filter you "
                                + "don't need. Returns a JSON list of matching slots, each with a timeSlotId.",
                        Map.of(
                                "serviceKeyword", schema("string", "Keyword to match against the service name or description, e.g. \"haircut\"."),
                                "providerKeyword", schema("string", "Keyword to match against the provider/organization name."),
                                "afterTime", schema("string", "Only return slots starting at or after this ISO-8601 instant, e.g. 2026-07-20T00:00:00Z."),
                                "beforeTime", schema("string", "Only return slots starting before this ISO-8601 instant.")
                        ),
                        List.of()
                ),
                tool(
                        "book_time_slot",
                        "Book a specific AVAILABLE time slot for the current user. Only call this once the user has "
                                + "confirmed exactly which slot (by timeSlotId) they want - never invent or guess a timeSlotId, "
                                + "always get it from a prior search_time_slots result.",
                        Map.of("timeSlotId", schema("string", "The id of the time slot to book, from search_time_slots.")),
                        List.of("timeSlotId")
                ),
                tool(
                        "list_my_bookings",
                        "List the current user's own bookings (past and present), with their status and appointment details.",
                        Map.of(),
                        List.of()
                ),
                tool(
                        "cancel_booking",
                        "Cancel one of the current user's own CONFIRMED bookings. Only call this once the user has "
                                + "confirmed exactly which booking (by bookingId) to cancel, from a prior list_my_bookings result.",
                        Map.of("bookingId", schema("string", "The id of the booking to cancel, from list_my_bookings.")),
                        List.of("bookingId")
                )
        );
    }

    private static Tool tool(String name, String description, Map<String, Map<String, String>> properties, List<String> required) {
        Tool.InputSchema.Properties.Builder propertiesBuilder = Tool.InputSchema.Properties.builder();
        properties.forEach((propertyName, propertySchema) ->
                propertiesBuilder.putAdditionalProperty(propertyName, JsonValue.from(propertySchema)));

        Tool.InputSchema inputSchema = Tool.InputSchema.builder()
                .properties(propertiesBuilder.build())
                .required(required)
                .build();

        return Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();
    }

    private static Map<String, String> schema(String type, String description) {
        return Map.of("type", type, "description", description);
    }
}
