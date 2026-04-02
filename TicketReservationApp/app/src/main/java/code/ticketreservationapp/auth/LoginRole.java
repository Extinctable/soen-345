package code.ticketreservationapp.auth;

public enum LoginRole {
    USER("Customer"),
    ADMIN("Administrator");

    private final String displayName;

    LoginRole(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
