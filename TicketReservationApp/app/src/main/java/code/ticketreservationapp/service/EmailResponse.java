package code.ticketreservationapp.service;

final class EmailResponse {

    private final int statusCode;
    private final String body;

    EmailResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body == null ? "" : body;
    }

    int getStatusCode() {
        return statusCode;
    }

    String getBody() {
        return body;
    }
}
