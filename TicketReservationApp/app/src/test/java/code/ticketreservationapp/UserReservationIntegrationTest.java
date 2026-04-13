package code.ticketreservationapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import code.ticketreservationapp.model.ContactMethod;
import code.ticketreservationapp.model.Event;
import code.ticketreservationapp.model.Reservation;
import code.ticketreservationapp.service.TicketReservationService;

@RunWith(JUnit4.class)
public class UserReservationIntegrationTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-12T10:00:00Z"), ZoneId.of("UTC"));
    private TicketReservationService service;

    @Before
    public void setUp() {
        // No Firebase, no Android — seeds 4 demo events automatically
        service = new TicketReservationService(CLOCK);
    }

    // -------------------------------------------------------------------------
    // Test 1 — Reserve tickets: touches registerUser → reserveTickets →
    //           getReservations → getAvailableEvents (ticket count reduced)
    // -------------------------------------------------------------------------

    @Test
    public void testCreateReservation() {
        service.registerUser("Jane Doe", "jane@gmail.com", ContactMethod.EMAIL);

        List<Event> events = service.getAvailableEvents(null);
        assertFalse("Service should seed demo events on startup", events.isEmpty());
        Event target = events.get(0);
        int ticketsBefore = target.getAvailableTickets();

        Reservation reservation = service.reserveTickets(target.getId(), 2);

        assertNotNull(reservation);
        assertEquals(target.getId(), reservation.getEventId());
        assertEquals(2, reservation.getQuantity());
        assertEquals(Reservation.Status.ACTIVE, reservation.getStatus());
        assertEquals("jane@gmail.com", reservation.getContactValue());

        List<Event> eventsAfter = service.getAvailableEvents(null);
        Event updated = eventsAfter.stream()
                .filter(e -> e.getId().equals(target.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Event missing after reservation"));
        assertEquals(ticketsBefore - 2, updated.getAvailableTickets());

        List<Reservation> userReservations = service.getReservations();
        assertEquals(1, userReservations.size());
        assertEquals(reservation.getId(), userReservations.get(0).getId());
    }

    // -------------------------------------------------------------------------
    // Test 2 — Cancel reservation: touches reserveTickets → cancelReservation →
    //           getAvailableEvents (ticket count restored)
    // -------------------------------------------------------------------------

    @Test
    public void testReservationCancelled() {
        service.registerUser("John Doe", "doe@gmail.com", ContactMethod.EMAIL);
        List<Event> events = service.getAvailableEvents(null);
        Event target = events.get(0);
        int ticketsBefore = target.getAvailableTickets();

        Reservation reservation = service.reserveTickets(target.getId(), 3);

        Reservation cancelled = service.cancelReservation(reservation.getId());

        assertEquals(Reservation.Status.CANCELLED_BY_USER, cancelled.getStatus());

        List<Event> eventsAfter = service.getAvailableEvents(null);
        Event restored = eventsAfter.stream()
                .filter(e -> e.getId().equals(target.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Event missing after cancellation"));
        assertEquals(ticketsBefore, restored.getAvailableTickets());

        try {
            service.cancelReservation(reservation.getId());
            fail("Expected IllegalArgumentException on double-cancel");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().toLowerCase().contains("already cancelled"));
        }
    }
}
