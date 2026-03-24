package code.ticketreservationapp.model;

public enum EventCategory {
    ALL("All categories"),
    MOVIE("Movie"),
    CONCERT("Concert"),
    TRAVEL("Travel"),
    SPORTS("Sports");

    private final String label;

    EventCategory(String label) {
        this.label = label;
    }

    public static EventCategory[] filterValues() {
        return values();
    }

    public static EventCategory[] editableValues() {
        return new EventCategory[]{MOVIE, CONCERT, TRAVEL, SPORTS};
    }

    @Override
    public String toString() {
        return label;
    }
}
