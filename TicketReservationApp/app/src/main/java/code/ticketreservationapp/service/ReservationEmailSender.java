package code.ticketreservationapp.service;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.Executor;

import code.ticketreservationapp.model.Reservation;

final class ReservationEmailSender {

    interface Callback {
        void onComplete(boolean success, String message);
    }

    static final class DeliveryResult {
        private final boolean success;
        private final String message;

        DeliveryResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        boolean isSuccess() {
            return success;
        }

        String getMessage() {
            return message;
        }
    }

    private final EmailJsConfiguration configuration;
    private final Executor executor;
    private final EmailRequestExecutor requestExecutor;

    ReservationEmailSender(
            EmailJsConfiguration configuration,
            Executor executor,
            EmailRequestExecutor requestExecutor
    ) {
        if (configuration == null || executor == null || requestExecutor == null) {
            throw new IllegalArgumentException("ReservationEmailSender dependencies cannot be null.");
        }
        this.configuration = configuration;
        this.executor = executor;
        this.requestExecutor = requestExecutor;
    }

    static ReservationEmailSender createDefault() {
        return new ReservationEmailSender(
                EmailJsConfiguration.defaultConfig(),
                command -> new Thread(command).start(),
                new HttpUrlConnectionEmailRequestExecutor()
        );
    }

    void sendReservationConfirmation(String toEmail, Reservation reservation, Callback callback) {
        executor.execute(() -> {
            DeliveryResult result = sendReservationConfirmationNow(toEmail, reservation);
            callback.onComplete(result.isSuccess(), result.getMessage());
        });
    }

    DeliveryResult sendReservationConfirmationNow(String toEmail, Reservation reservation) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return new DeliveryResult(false, "No destination email found for confirmation.");
        }
        if (reservation == null) {
            return new DeliveryResult(false, "No reservation was provided for confirmation.");
        }
        if (configuration.getPublicKey().trim().isEmpty()) {
            return new DeliveryResult(false, "Email confirmation failed: EmailJS public key is missing.");
        }

        try {
            EmailResponse response = requestExecutor.postJson(
                    configuration.getEndpoint(),
                    buildPayload(toEmail.trim(), reservation)
            );
            return interpretResponse(response);
        } catch (Exception ex) {
            return new DeliveryResult(false, "Email confirmation failed: " + ex.getMessage());
        }
    }

    String buildPayload(String destinationEmail, Reservation reservation) throws Exception {
        JSONObject templateParams = new JSONObject();
        templateParams.put("to_email", destinationEmail);
        templateParams.put("email", destinationEmail);
        templateParams.put("recipient_email", destinationEmail);
        templateParams.put("customer_email", destinationEmail);
        templateParams.put("user_email", destinationEmail);
        templateParams.put("reply_to", destinationEmail);
        templateParams.put("customer_name", reservation.getCustomerName());
        templateParams.put("reservation_id", reservation.getId());
        templateParams.put("event_title", reservation.getEventTitle());
        templateParams.put("quantity", reservation.getQuantity());
        templateParams.put("contact_method", "Email");
        templateParams.put(
                "message",
                "Reservation " + reservation.getId() + " confirmed for " + reservation.getEventTitle()
        );

        JSONObject payload = new JSONObject();
        payload.put("service_id", configuration.getServiceId());
        payload.put("template_id", configuration.getTemplateId());
        payload.put("user_id", configuration.getPublicKey());
        if (!configuration.getAccessToken().trim().isEmpty()) {
            payload.put("accessToken", configuration.getAccessToken());
        }
        payload.put("template_params", templateParams);
        return payload.toString();
    }

    private DeliveryResult interpretResponse(EmailResponse response) {
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            return new DeliveryResult(true, "Email confirmation sent.");
        }

        String responseMessage = response.getBody().trim();
        if (responseMessage.isEmpty()) {
            return new DeliveryResult(false, "Email confirmation failed (HTTP " + response.getStatusCode() + ").");
        }

        String normalizedMessage = responseMessage.toLowerCase(Locale.ROOT);
        if (normalizedMessage.contains("recipient") && normalizedMessage.contains("empty")) {
            return new DeliveryResult(
                    false,
                    "EmailJS could not find a recipient address. Set the template's To Email field to {{to_email}}."
            );
        }

        return new DeliveryResult(
                false,
                "Email confirmation failed (HTTP " + response.getStatusCode() + "): " + responseMessage
        );
    }
}
