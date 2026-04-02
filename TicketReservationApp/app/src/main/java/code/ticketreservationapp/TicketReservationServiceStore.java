package code.ticketreservationapp;

import code.ticketreservationapp.service.TicketReservationService;

final class TicketReservationServiceStore {

    private static TicketReservationService service = new TicketReservationService();

    private TicketReservationServiceStore() {
    }

    static synchronized TicketReservationService getService() {
        return service;
    }

    static synchronized void reset() {
        service = new TicketReservationService();
    }
}
