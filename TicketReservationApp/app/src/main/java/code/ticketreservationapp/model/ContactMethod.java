package code.ticketreservationapp.model;

public enum ContactMethod {
    EMAIL("Email"),
    SMS("SMS");

    private final String label;

    ContactMethod(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
