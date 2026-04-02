package code.ticketreservationapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import code.ticketreservationapp.model.ContactMethod;
import code.ticketreservationapp.model.Event;
import code.ticketreservationapp.model.EventCategory;
import code.ticketreservationapp.model.EventFilter;
import code.ticketreservationapp.model.Reservation;
import code.ticketreservationapp.model.UserProfile;
import code.ticketreservationapp.service.TicketReservationService;

class TicketReservationServiceTest {

    private TicketReservationService service;

    @BeforeEach
    void setUp() {
        service = new TicketReservationService();
    }

    @Test
    @DisplayName("Registration accepts an email address and stores the user profile")
    void registerUserWithEmail() {
        UserProfile user = service.registerUser("Alex Martin", "alex@example.com", ContactMethod.EMAIL);

        assertNotNull(user);
        assertEquals("Alex Martin", user.getFullName());
        assertEquals(ContactMethod.EMAIL, user.getContactMethod());
        assertEquals(1, service.getConfirmationHistory().size());
    }

    @Test
    @DisplayName("Filtering events supports category, date, and location")
    void filterEvents() {
        Event montrealMovie = service.getAvailableEvents(new EventFilter("Downtown", "Montreal", null, EventCategory.MOVIE)).get(0);
        EventFilter exactDateFilter = new EventFilter(
                "",
                "Montreal",
                montrealMovie.getDate(),
                EventCategory.MOVIE
        );

        List<Event> filteredEvents = service.getAvailableEvents(exactDateFilter);

        assertEquals(1, filteredEvents.size());
        assertEquals("Downtown Cinema Premiere", filteredEvents.get(0).getTitle());
    }

    @Test
    @DisplayName("Reservations reduce inventory and cancellations restore it")
    void reserveAndCancelTickets() {
        service.registerUser("Priya Shah", "5145551234", ContactMethod.SMS);
        Event targetEvent = firstEvent();
        int startingInventory = targetEvent.getAvailableTickets();

        Reservation reservation = service.reserveTickets(targetEvent.getId(), 2);
        Reservation cancelledReservation = service.cancelReservation(reservation.getId());

        Event refreshedEvent = findEventById(targetEvent.getId());

        assertEquals(Reservation.Status.CANCELLED_BY_USER, cancelledReservation.getStatus());
        assertEquals(startingInventory, refreshedEvent.getAvailableTickets());
        assertEquals(3, service.getConfirmationHistory().size());
    }

    @Test
    @DisplayName("Administrators can add, update, and cancel an event")
    void adminLifecycle() {
        Event createdEvent = service.addEvent(
                "Summer Stadium Show",
                "Montreal",
                LocalDate.now().plusDays(45),
                EventCategory.CONCERT,
                120,
                75.50
        );

        Event updatedEvent = service.updateEvent(
                createdEvent.getId(),
                "Summer Stadium Show Reloaded",
                "Montreal",
                LocalDate.now().plusDays(50),
                EventCategory.CONCERT,
                90,
                85.00
        );
        Event cancelledEvent = service.cancelEvent(updatedEvent.getId());

        assertEquals("Summer Stadium Show Reloaded", updatedEvent.getTitle());
        assertEquals(90, updatedEvent.getAvailableTickets());
        assertTrue(cancelledEvent.getId().startsWith("EV-"));
        assertTrue(
                service.getAvailableEvents(new EventFilter("Summer Stadium Show Reloaded", "", null, EventCategory.ALL)).isEmpty()
        );
    }

    @Test
    @DisplayName("Service rejects invalid contact information")
    void rejectInvalidContact() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.registerUser("Chris Doe", "invalid-phone", ContactMethod.SMS)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.registerUser("Chris Doe", "invalid-email", ContactMethod.EMAIL)
        );
    }

    @Test
    @DisplayName("Concurrent reservations do not oversell inventory")
    void concurrentReservationsAreSafe() throws Exception {
        service.registerUser("Jamie Lee", "jamie@example.com", ContactMethod.EMAIL);
        Event event = firstEvent();
        int ticketCount = event.getAvailableTickets();

        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < ticketCount + 12; i++) {
            tasks.add(() -> {
                startSignal.await();
                try {
                    service.reserveTickets(event.getId(), 1);
                    return true;
                } catch (IllegalArgumentException ex) {
                    return false;
                }
            });
        }

        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            futures.add(executor.submit(task));
        }

        startSignal.countDown();

        int successfulReservations = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successfulReservations++;
            }
        }

        executor.shutdownNow();

        Event refreshedEvent = findEventById(event.getId());
        assertEquals(ticketCount, successfulReservations);
        assertEquals(0, refreshedEvent.getAvailableTickets());
    }

    private Event firstEvent() {
        return service.getAvailableEvents(new EventFilter("", "", null, EventCategory.ALL)).get(0);
    }

    private Event findEventById(String eventId) {
        return service.getAvailableEvents(new EventFilter("", "", null, EventCategory.ALL))
                .stream()
                .filter(event -> event.getId().equals(eventId))
                .findFirst()
                .orElseThrow();
    }
}
