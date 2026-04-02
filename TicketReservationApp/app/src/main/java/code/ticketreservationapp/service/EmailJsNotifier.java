package code.ticketreservationapp.service;

import code.ticketreservationapp.model.Reservation;

public final class EmailJsNotifier {

    private static ReservationEmailSender sender = ReservationEmailSender.createDefault();

    private EmailJsNotifier() {
    }

    public interface Callback {
        void onComplete(boolean success, String message);
    }

    public static void sendReservationConfirmation(String toEmail, Reservation reservation, Callback callback) {
        sender().sendReservationConfirmation(toEmail, reservation, callback::onComplete);
    }

    static synchronized void setSender(ReservationEmailSender replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("ReservationEmailSender cannot be null.");
        }
        sender = replacement;
    }

    static synchronized void resetSender() {
        sender = ReservationEmailSender.createDefault();
    }

    private static synchronized ReservationEmailSender sender() {
        return sender;
    }
}
