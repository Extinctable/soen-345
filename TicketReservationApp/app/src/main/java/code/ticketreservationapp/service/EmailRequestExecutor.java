package code.ticketreservationapp.service;

interface EmailRequestExecutor {
    EmailResponse postJson(String endpoint, String body) throws Exception;
}
