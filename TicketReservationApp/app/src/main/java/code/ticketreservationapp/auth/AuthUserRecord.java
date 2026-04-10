package code.ticketreservationapp.auth;

public record AuthUserRecord(String username, String password, LoginRole role, String email, String phone) {
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public LoginRole getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
}
