package code.ticketreservationapp.model;

import java.time.LocalDateTime;

public class Reservation {

    public enum Status {
        ACTIVE("Active"),
        CANCELLED_BY_USER("Cancelled by user"),
        CANCELLED_BY_ADMIN("Cancelled by admin");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final String id;
    private final String eventId;
    private final String eventTitle;
    private final String customerName;
    private final String contactValue;
    private final ContactMethod channel;
    private final int quantity;
    private final LocalDateTime createdAt;
    private Status status;

    public Reservation(
            String id,
            String eventId,
            String eventTitle,
            String customerName,
            String contactValue,
            ContactMethod channel,
            int quantity,
            LocalDateTime createdAt,
            Status status
    ) {
        this.id = id;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.customerName = customerName;
        this.contactValue = contactValue;
        this.channel = channel;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.status = status;
    }

    public Reservation copy() {
        return new Reservation(
                id,
                eventId,
                eventTitle,
                customerName,
                contactValue,
                channel,
                quantity,
                createdAt,
                status
        );
    }

    public String getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getContactValue() {
        return contactValue;
    }

    public ContactMethod getChannel() {
        return channel;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }
}
