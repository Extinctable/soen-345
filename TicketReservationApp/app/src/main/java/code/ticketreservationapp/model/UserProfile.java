package code.ticketreservationapp.model;

import java.time.LocalDateTime;

public class UserProfile {

    private final String fullName;
    private final String contactValue;
    private final ContactMethod contactMethod;
    private final LocalDateTime registeredAt;

    public UserProfile(
            String fullName,
            String contactValue,
            ContactMethod contactMethod,
            LocalDateTime registeredAt
    ) {
        this.fullName = fullName;
        this.contactValue = contactValue;
        this.contactMethod = contactMethod;
        this.registeredAt = registeredAt;
    }

    public UserProfile copy() {
        return new UserProfile(fullName, contactValue, contactMethod, registeredAt);
    }

    public String getFullName() {
        return fullName;
    }

    public String getContactValue() {
        return contactValue;
    }

    public ContactMethod getContactMethod() {
        return contactMethod;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
}
