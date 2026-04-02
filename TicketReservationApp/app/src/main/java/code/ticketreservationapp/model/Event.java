package code.ticketreservationapp.model;

import java.time.LocalDate;

public class Event {

    private final String id;
    private String title;
    private String location;
    private LocalDate date;
    private EventCategory category;
    private int availableTickets;
    private double price;
    private boolean active;

    public Event(
            String id,
            String title,
            String location,
            LocalDate date,
            EventCategory category,
            int availableTickets,
            double price,
            boolean active
    ) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.date = date;
        this.category = category;
        this.availableTickets = availableTickets;
        this.price = price;
        this.active = active;
    }

    public Event copy() {
        return new Event(id, title, location, date, category, availableTickets, price, active);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public int getAvailableTickets() {
        return availableTickets;
    }

    public void setAvailableTickets(int availableTickets) {
        this.availableTickets = availableTickets;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return title + " (" + date + ")";
    }
}
