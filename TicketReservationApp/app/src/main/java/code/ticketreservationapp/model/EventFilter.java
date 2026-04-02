package code.ticketreservationapp.model;

import java.time.LocalDate;

public class EventFilter {

    private final String query;
    private final String location;
    private final LocalDate date;
    private final EventCategory category;

    public EventFilter(String query, String location, LocalDate date, EventCategory category) {
        this.query = query == null ? "" : query.trim();
        this.location = location == null ? "" : location.trim();
        this.date = date;
        this.category = category == null ? EventCategory.ALL : category;
    }

    public String getQuery() {
        return query;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getDate() {
        return date;
    }

    public EventCategory getCategory() {
        return category;
    }
}
