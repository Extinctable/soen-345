package code.ticketreservationapp.service;

public final class TicketReservationServiceStore {

    private static TicketReservationService service = new TicketReservationService(true);

    private TicketReservationServiceStore() {
    }

    public static synchronized TicketReservationService getService() {
        return service;
    }

    public static synchronized void setService(TicketReservationService replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("TicketReservationService cannot be null.");
        }
        service = replacement;
    }

    public static synchronized void reset() {
        service = new TicketReservationService(false);
    }
}
