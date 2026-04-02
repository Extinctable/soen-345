package code.ticketreservationapp.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import code.ticketreservationapp.model.ConfirmationRecord;
import code.ticketreservationapp.model.ContactMethod;
import code.ticketreservationapp.model.Event;
import code.ticketreservationapp.model.EventCategory;
import code.ticketreservationapp.model.EventFilter;
import code.ticketreservationapp.model.Reservation;
import code.ticketreservationapp.model.UserProfile;

public class TicketReservationService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9][0-9\\-\\s]{8,14}[0-9]$");

    private final Map<String, Event> events = new LinkedHashMap<>();
    private final Map<String, Reservation> reservations = new LinkedHashMap<>();
    private final List<ConfirmationRecord> confirmations = new ArrayList<>();

    private final AtomicInteger eventSequence = new AtomicInteger(100);
    private final AtomicInteger reservationSequence = new AtomicInteger(1000);
    private final AtomicInteger confirmationSequence = new AtomicInteger(5000);
    private final Clock clock;
    private final ReservationStateGateway reservationStateGateway;
    private boolean firebaseHydrated;

    private UserProfile registeredUser;

    public TicketReservationService() {
        this(Clock.systemDefaultZone(), null);
    }

    public TicketReservationService(boolean enableFirebaseSync) {
        this(Clock.systemDefaultZone(), enableFirebaseSync ? FirebaseTicketDatabase.tryCreate() : null);
    }

    public TicketReservationService(Clock clock) {
        this(clock, null);
    }

    public TicketReservationService(Clock clock, ReservationStateGateway reservationStateGateway) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.reservationStateGateway = reservationStateGateway;

        if (this.reservationStateGateway == null) {
            seedDemoEvents();
            return;
        }

        firebaseHydrated = false;
        this.reservationStateGateway.subscribe(this::handleFirebaseSnapshot);
    }

    public synchronized UserProfile registerUser(String fullName, String contactValue, ContactMethod method) {
        String normalizedName = requireText(fullName, "Full name is required.");
        String normalizedContact = requireText(contactValue, "A valid email or phone number is required.");
        validateContact(normalizedContact, method);

        registeredUser = new UserProfile(normalizedName, normalizedContact, method, LocalDateTime.now(clock));
        createConfirmation(
                method,
                normalizedContact,
                "Welcome " + normalizedName + ". Your ticket profile is active."
        );
        persistStateToFirebase();
        return registeredUser.copy();
    }

    public synchronized UserProfile getRegisteredUser() {
        return registeredUser == null ? null : registeredUser.copy();
    }

    public synchronized List<Event> getAvailableEvents(EventFilter filter) {
        List<Event> filteredEvents = new ArrayList<>();

        for (Event event : events.values()) {
            if (!event.isActive()) {
                continue;
            }
            if (!matchesFilter(event, filter)) {
                continue;
            }
            filteredEvents.add(event.copy());
        }

        filteredEvents.sort(Comparator.comparing(Event::getDate).thenComparing(Event::getTitle));
        return filteredEvents;
    }

    public synchronized List<Event> getActiveEventsForAdmin() {
        List<Event> activeEvents = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.isActive()) {
                activeEvents.add(event.copy());
            }
        }
        activeEvents.sort(Comparator.comparing(Event::getDate).thenComparing(Event::getTitle));
        return activeEvents;
    }

    public synchronized List<Reservation> getReservations() {
        List<Reservation> allReservations = new ArrayList<>();
        for (Reservation reservation : reservations.values()) {
            if (registeredUser == null || reservation.getContactValue().equals(registeredUser.getContactValue())) {
                allReservations.add(reservation.copy());
            }
        }
        allReservations.sort(Comparator.comparing(Reservation::getCreatedAt).reversed());
        return allReservations;
    }

    public synchronized List<ConfirmationRecord> getConfirmationHistory() {
        List<ConfirmationRecord> history = new ArrayList<>();
        for (int i = confirmations.size() - 1; i >= 0; i--) {
            ConfirmationRecord confirmation = confirmations.get(i);
            if (registeredUser == null || confirmation.getRecipient().equals(registeredUser.getContactValue())) {
                history.add(confirmation.copy());
            }
        }
        return history;
    }

    public synchronized Reservation reserveTickets(String eventId, int quantity) {
        if (registeredUser == null) {
            throw new IllegalArgumentException("Register with an email or phone number before reserving tickets.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be greater than zero.");
        }

        Event event = requireActiveEvent(eventId);
        if (event.getAvailableTickets() < quantity) {
            throw new IllegalArgumentException("Only " + event.getAvailableTickets() + " tickets remain for this event.");
        }

        event.setAvailableTickets(event.getAvailableTickets() - quantity);

        Reservation reservation = new Reservation(
                nextReservationId(),
                event.getId(),
                event.getTitle(),
                registeredUser.getFullName(),
                registeredUser.getContactValue(),
                registeredUser.getContactMethod(),
                quantity,
                LocalDateTime.now(clock),
                Reservation.Status.ACTIVE
        );
        reservations.put(reservation.getId(), reservation);

        createConfirmation(
                registeredUser.getContactMethod(),
                registeredUser.getContactValue(),
                "Reservation " + reservation.getId() + " confirmed for " + event.getTitle() + "."
        );

        persistStateToFirebase();

        return reservation.copy();
    }

    public synchronized Reservation cancelReservation(String reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found.");
        }
        if (!reservation.isActive()) {
            throw new IllegalArgumentException("This reservation is already cancelled.");
        }

        Event event = events.get(reservation.getEventId());
        if (event != null && event.isActive()) {
            event.setAvailableTickets(event.getAvailableTickets() + reservation.getQuantity());
        }

        reservation.setStatus(Reservation.Status.CANCELLED_BY_USER);
        createConfirmation(
                reservation.getChannel(),
                reservation.getContactValue(),
                "Reservation " + reservation.getId() + " cancelled for " + reservation.getEventTitle() + "."
        );
        persistStateToFirebase();
        return reservation.copy();
    }

    public synchronized Event addEvent(
            String title,
            String location,
            LocalDate date,
            EventCategory category,
            int availableTickets,
            double price
    ) {
        Event event = new Event(
                nextEventId(),
                requireText(title, "Event title is required."),
                requireText(location, "Event location is required."),
                requireFutureOrPresentDate(date),
                requireCategory(category),
                requirePositiveTickets(availableTickets),
                requirePositivePrice(price),
                true
        );
        events.put(event.getId(), event);
            persistStateToFirebase();
        return event.copy();
    }

    public synchronized Event updateEvent(
            String eventId,
            String title,
            String location,
            LocalDate date,
            EventCategory category,
            int availableTickets,
            double price
    ) {
        Event event = requireActiveEvent(eventId);
        event.setTitle(requireText(title, "Event title is required."));
        event.setLocation(requireText(location, "Event location is required."));
        event.setDate(requireFutureOrPresentDate(date));
        event.setCategory(requireCategory(category));
        event.setAvailableTickets(requireNonNegativeTickets(availableTickets));
        event.setPrice(requirePositivePrice(price));
        persistStateToFirebase();
        return event.copy();
    }

    public synchronized Event cancelEvent(String eventId) {
        Event event = requireActiveEvent(eventId);
        event.setActive(false);

        for (Reservation reservation : reservations.values()) {
            if (!reservation.isActive() || !reservation.getEventId().equals(eventId)) {
                continue;
            }

            reservation.setStatus(Reservation.Status.CANCELLED_BY_ADMIN);
            createConfirmation(
                    reservation.getChannel(),
                    reservation.getContactValue(),
                    "Event " + event.getTitle() + " was cancelled. Reservation " + reservation.getId() + " is closed."
            );
        }

        persistStateToFirebase();

        return event.copy();
    }

    private synchronized void handleFirebaseSnapshot(Map<String, Object> root) {
        if (root == null) {
            return;
        }

        TicketReservationSnapshotMapper.SnapshotState restoredState = TicketReservationSnapshotMapper.restore(root, clock);
        if (restoredState != null) {
            applySnapshotState(restoredState);
            firebaseHydrated = true;
            return;
        }

        if (!firebaseHydrated) {
            clearInMemoryState();
            seedDemoEvents();
            firebaseHydrated = true;
            persistStateToFirebase();
        }
    }

    private void applySnapshotState(TicketReservationSnapshotMapper.SnapshotState restoredState) {
        events.clear();
        events.putAll(restoredState.getEvents());
        reservations.clear();
        reservations.putAll(restoredState.getReservations());
        confirmations.clear();
        confirmations.addAll(restoredState.getConfirmations());
        registeredUser = restoredState.getRegisteredUser();
        synchronizeSequenceCounters();
    }

    private void clearInMemoryState() {
        events.clear();
        reservations.clear();
        confirmations.clear();
        registeredUser = null;
    }

    private void persistStateToFirebase() {
        if (reservationStateGateway == null) {
            return;
        }

        try {
            reservationStateGateway.writeSnapshot(
                    TicketReservationSnapshotMapper.serialize(events, reservations, confirmations, registeredUser)
            );
        } catch (RuntimeException ignored) {
            // Keep app workflows alive even if remote persistence is temporarily unavailable.
        }
    }

    private void synchronizeSequenceCounters() {
        eventSequence.set(Math.max(100, highestSequence(events.keySet(), "EV-")));
        reservationSequence.set(Math.max(1000, highestSequence(reservations.keySet(), "RSV-")));

        List<String> confirmationIds = new ArrayList<>();
        for (ConfirmationRecord confirmation : confirmations) {
            confirmationIds.add(confirmation.getId());
        }
        confirmationSequence.set(Math.max(5000, highestSequence(confirmationIds, "CNF-")));
    }

    private int highestSequence(Collection<String> ids, String prefix) {
        int highest = 0;
        for (String id : ids) {
            if (id == null || !id.startsWith(prefix)) {
                continue;
            }

            try {
                highest = Math.max(highest, Integer.parseInt(id.substring(prefix.length())));
            } catch (NumberFormatException ignored) {
                // Ignore malformed ids and keep scanning.
            }
        }
        return highest;
    }


    private boolean matchesFilter(Event event, EventFilter filter) {
        if (filter == null) {
            return true;
        }

        String query = filter.getQuery().toLowerCase(Locale.ROOT);
        String location = filter.getLocation().toLowerCase(Locale.ROOT);

        boolean matchesQuery = query.isEmpty() || event.getTitle().toLowerCase(Locale.ROOT).contains(query);
        boolean matchesLocation = location.isEmpty() || event.getLocation().toLowerCase(Locale.ROOT).contains(location);
        boolean matchesDate = filter.getDate() == null || event.getDate().equals(filter.getDate());
        boolean matchesCategory = filter.getCategory() == EventCategory.ALL || event.getCategory() == filter.getCategory();

        return matchesQuery && matchesLocation && matchesDate && matchesCategory;
    }

    private Event requireActiveEvent(String eventId) {
        Event event = events.get(eventId);
        if (event == null || !event.isActive()) {
            throw new IllegalArgumentException("The selected event is no longer available.");
        }
        return event;
    }

    private void validateContact(String contactValue, ContactMethod method) {
        if (method == ContactMethod.EMAIL && !EMAIL_PATTERN.matcher(contactValue).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (method == ContactMethod.SMS && !PHONE_PATTERN.matcher(contactValue).matches()) {
            throw new IllegalArgumentException("Enter a valid phone number.");
        }
    }

    private String requireText(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    private LocalDate requireFutureOrPresentDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Event date is required.");
        }
        if (date.isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Event date cannot be in the past.");
        }
        return date;
    }

    private EventCategory requireCategory(EventCategory category) {
        if (category == null || category == EventCategory.ALL) {
            throw new IllegalArgumentException("Select a valid event category.");
        }
        return category;
    }

    private int requirePositiveTickets(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Ticket inventory must be greater than zero.");
        }
        return value;
    }

    private int requireNonNegativeTickets(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Remaining tickets cannot be negative.");
        }
        return value;
    }

    private double requirePositivePrice(double value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }
        return value;
    }

    private void createConfirmation(ContactMethod channel, String recipient, String message) {
        confirmations.add(
                new ConfirmationRecord(
                        "CNF-" + confirmationSequence.incrementAndGet(),
                        channel,
                        recipient,
                        message,
                        LocalDateTime.now(clock)
                )
        );
    }

    private String nextEventId() {
        return "EV-" + eventSequence.incrementAndGet();
    }

    private String nextReservationId() {
        return "RSV-" + reservationSequence.incrementAndGet();
    }

    private void seedDemoEvents() {
        LocalDate today = LocalDate.now(clock);
        addSeedEvent("Downtown Cinema Premiere", "Montreal", today.plusDays(12), EventCategory.MOVIE, 28, 18.50);
        addSeedEvent("Jazz Under The Stars", "Quebec City", today.plusDays(18), EventCategory.CONCERT, 76, 54.99);
        addSeedEvent("Canadiens Rivalry Night", "Ottawa", today.plusDays(21), EventCategory.SPORTS, 44, 89.00);
        addSeedEvent("Niagara Weekend Rail Pass", "Toronto", today.plusDays(30), EventCategory.TRAVEL, 32, 39.99);
    }

    private void addSeedEvent(
            String title,
            String location,
            LocalDate date,
            EventCategory category,
            int availableTickets,
            double price
    ) {
        Event event = new Event(
                nextEventId(),
                title,
                location,
                date,
                category,
                availableTickets,
                price,
                true
        );
        events.put(event.getId(), event);
    }
}
