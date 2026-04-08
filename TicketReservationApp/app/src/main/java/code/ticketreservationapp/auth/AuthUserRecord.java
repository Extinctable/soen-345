package code.ticketreservationapp.auth;

public final class AuthUserRecord {

    private final String username;
    private final String password;
    private final LoginRole role;
    private final String email;
    private final String phone;

    public AuthUserRecord(String username, String password, LoginRole role, String email, String phone) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.phone = phone;
    }

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
