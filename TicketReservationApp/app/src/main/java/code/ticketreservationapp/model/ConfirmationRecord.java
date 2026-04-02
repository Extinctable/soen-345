package code.ticketreservationapp.model;

import java.time.LocalDateTime;

public class ConfirmationRecord {

    private final String id;
    private final ContactMethod channel;
    private final String recipient;
    private final String message;
    private final LocalDateTime createdAt;

    public ConfirmationRecord(
            String id,
            ContactMethod channel,
            String recipient,
            String message,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.channel = channel;
        this.recipient = recipient;
        this.message = message;
        this.createdAt = createdAt;
    }

    public ConfirmationRecord copy() {
        return new ConfirmationRecord(id, channel, recipient, message, createdAt);
    }

    public String getId() {
        return id;
    }

    public ContactMethod getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
