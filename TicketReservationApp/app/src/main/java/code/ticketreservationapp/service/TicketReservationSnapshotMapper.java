package code.ticketreservationapp.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import code.ticketreservationapp.model.ConfirmationRecord;
import code.ticketreservationapp.model.ContactMethod;
import code.ticketreservationapp.model.Event;
import code.ticketreservationapp.model.EventCategory;
import code.ticketreservationapp.model.Reservation;
import code.ticketreservationapp.model.UserProfile;

final class TicketReservationSnapshotMapper {

    private TicketReservationSnapshotMapper() {}

    static Map<String, Object> serialize(
            Map<String, Event> events,
            Map<String, Reservation> reservations,
            List<ConfirmationRecord> confirmations,
            UserProfile registeredUser
    ) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("events", serializeEvents(events));
        root.put("reservations", serializeReservations(reservations));
        root.put("confirmations", serializeConfirmations(confirmations));
        if (registeredUser != null) {
            root.put("registeredUser", serializeUser(registeredUser));
        }
        return root;
    }

    static SnapshotState restore(Map<String, Object> root, Clock clock) {
        Clock effectiveClock = clock == null ? Clock.systemDefaultZone() : clock;
        if (root == null || root.isEmpty()) {
            return null;
        }

        Map<String, Object> eventsNode = asMap(root.get("events"));
        if (eventsNode.isEmpty()) {
            return null;
        }

        Map<String, Event> loadedEvents = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : eventsNode.entrySet()) {
            Event event = toEvent(entry.getKey(), asMap(entry.getValue()), effectiveClock);
            if (event != null) {
                loadedEvents.put(event.getId(), event);
            }
        }
        if (loadedEvents.isEmpty()) {
            return null;
        }

        Map<String, Reservation> loadedReservations = new LinkedHashMap<>();
        Map<String, Object> reservationsNode = asMap(root.get("reservations"));
        for (Map.Entry<String, Object> entry : reservationsNode.entrySet()) {
            Reservation reservation = toReservation(entry.getKey(), asMap(entry.getValue()), effectiveClock);
            if (reservation != null) {
                loadedReservations.put(reservation.getId(), reservation);
            }
        }

        List<ConfirmationRecord> loadedConfirmations = new ArrayList<>();
        Map<String, Object> confirmationsNode = asMap(root.get("confirmations"));
        for (Map.Entry<String, Object> entry : confirmationsNode.entrySet()) {
            ConfirmationRecord confirmation = toConfirmation(entry.getKey(), asMap(entry.getValue()), effectiveClock);
            if (confirmation != null) {
                loadedConfirmations.add(confirmation);
            }
        }
        loadedConfirmations.sort(Comparator.comparing(ConfirmationRecord::getCreatedAt));

        UserProfile loadedUser = toUserProfile(asMap(root.get("registeredUser")), effectiveClock);
        return new SnapshotState(loadedEvents, loadedReservations, loadedConfirmations, loadedUser);
    }

    private static Map<String, Object> serializeEvents(Map<String, Event> events) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (Event event : events.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("title", event.getTitle());
            entry.put("location", event.getLocation());
            entry.put("date", event.getDate().toString());
            entry.put("category", event.getCategory().name());
            entry.put("availableTickets", event.getAvailableTickets());
            entry.put("price", event.getPrice());
            entry.put("active", event.isActive());
            payload.put(event.getId(), entry);
        }
        return payload;
    }

    private static Map<String, Object> serializeReservations(Map<String, Reservation> reservations) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (Reservation reservation : reservations.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("eventId", reservation.getEventId());
            entry.put("eventTitle", reservation.getEventTitle());
            entry.put("customerName", reservation.getCustomerName());
            entry.put("contactValue", reservation.getContactValue());
            entry.put("channel", reservation.getChannel().name());
            entry.put("quantity", reservation.getQuantity());
            entry.put("createdAt", reservation.getCreatedAt().toString());
            entry.put("status", reservation.getStatus().name());
            payload.put(reservation.getId(), entry);
        }
        return payload;
    }

    private static Map<String, Object> serializeConfirmations(List<ConfirmationRecord> confirmations) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (ConfirmationRecord confirmation : confirmations) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("channel", confirmation.getChannel().name());
            entry.put("recipient", confirmation.getRecipient());
            entry.put("message", confirmation.getMessage());
            entry.put("createdAt", confirmation.getCreatedAt().toString());
            payload.put(confirmation.getId(), entry);
        }
        return payload;
    }

    private static Map<String, Object> serializeUser(UserProfile user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", user.getFullName());
        payload.put("contactValue", user.getContactValue());
        payload.put("contactMethod", user.getContactMethod().name());
        payload.put("registeredAt", user.getRegisteredAt().toString());
        return payload;
    }

    private static Event toEvent(String eventId, Map<String, Object> payload, Clock clock) {
        if (payload.isEmpty()) {
            return null;
        }

        String title = asString(payload.get("title"));
        String location = asString(payload.get("location"));
        if (title.isEmpty() || location.isEmpty()) {
            return null;
        }

        return new Event(
                eventId,
                title,
                location,
                parseDate(payload.get("date"), clock),
                parseEventCategory(payload.get("category")),
                asInt(payload.get("availableTickets"), 0),
                asDouble(payload.get("price"), 0.0),
                asBoolean(payload.get("active"), true)
        );
    }

    private static Reservation toReservation(String reservationId, Map<String, Object> payload, Clock clock) {
        if (payload.isEmpty()) {
            return null;
        }

        String eventId = asString(payload.get("eventId"));
        String eventTitle = asString(payload.get("eventTitle"));
        String customerName = asString(payload.get("customerName"));
        String contactValue = asString(payload.get("contactValue"));
        if (eventId.isEmpty() || eventTitle.isEmpty() || customerName.isEmpty() || contactValue.isEmpty()) {
            return null;
        }

        return new Reservation(
                reservationId,
                eventId,
                eventTitle,
                customerName,
                contactValue,
                parseContactMethod(payload.get("channel")),
                asInt(payload.get("quantity"), 1),
                parseDateTime(payload.get("createdAt"), clock),
                parseReservationStatus(payload.get("status"))
        );
    }

    private static ConfirmationRecord toConfirmation(String confirmationId, Map<String, Object> payload, Clock clock) {
        if (payload.isEmpty()) {
            return null;
        }

        String recipient = asString(payload.get("recipient"));
        String message = asString(payload.get("message"));
        if (recipient.isEmpty() || message.isEmpty()) {
            return null;
        }

        return new ConfirmationRecord(
                confirmationId,
                parseContactMethod(payload.get("channel")),
                recipient,
                message,
                parseDateTime(payload.get("createdAt"), clock)
        );
    }

    private static UserProfile toUserProfile(Map<String, Object> payload, Clock clock) {
        if (payload.isEmpty()) {
            return null;
        }

        String fullName = asString(payload.get("fullName"));
        String contactValue = asString(payload.get("contactValue"));
        if (fullName.isEmpty() || contactValue.isEmpty()) {
            return null;
        }

        return new UserProfile(
                fullName,
                contactValue,
                parseContactMethod(payload.get("contactMethod")),
                parseDateTime(payload.get("registeredAt"), clock)
        );
    }

    private static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map)) {
            return new LinkedHashMap<>();
        }

        Map<?, ?> raw = (Map<?, ?>) value;
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            normalized.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return normalized;
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(asString(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double asDouble(Object value, double fallback) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(asString(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        String normalized = asString(value).toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        return fallback;
    }

    private static LocalDate parseDate(Object value, Clock clock) {
        try {
            return LocalDate.parse(asString(value));
        } catch (Exception ignored) {
            return LocalDate.now(clock);
        }
    }

    private static LocalDateTime parseDateTime(Object value, Clock clock) {
        try {
            return LocalDateTime.parse(asString(value));
        } catch (Exception ignored) {
            return LocalDateTime.now(clock);
        }
    }

    private static EventCategory parseEventCategory(Object value) {
        try {
            EventCategory parsed = EventCategory.valueOf(asString(value));
            return parsed == EventCategory.ALL ? EventCategory.MOVIE : parsed;
        } catch (Exception ignored) {
            return EventCategory.MOVIE;
        }
    }

    private static ContactMethod parseContactMethod(Object value) {
        try {
            return ContactMethod.valueOf(asString(value));
        } catch (Exception ignored) {
            return ContactMethod.EMAIL;
        }
    }

    private static Reservation.Status parseReservationStatus(Object value) {
        try {
            return Reservation.Status.valueOf(asString(value));
        } catch (Exception ignored) {
            return Reservation.Status.ACTIVE;
        }
    }

    static final class SnapshotState {
        private final Map<String, Event> events;
        private final Map<String, Reservation> reservations;
        private final List<ConfirmationRecord> confirmations;
        private final UserProfile registeredUser;

        SnapshotState(
                Map<String, Event> events,
                Map<String, Reservation> reservations,
                List<ConfirmationRecord> confirmations,
                UserProfile registeredUser
        ) {
            this.events = events;
            this.reservations = reservations;
            this.confirmations = confirmations;
            this.registeredUser = registeredUser;
        }

        Map<String, Event> getEvents() {
            return events;
        }

        Map<String, Reservation> getReservations() {
            return reservations;
        }

        List<ConfirmationRecord> getConfirmations() {
            return confirmations;
        }

        UserProfile getRegisteredUser() {
            return registeredUser;
        }
    }
}
