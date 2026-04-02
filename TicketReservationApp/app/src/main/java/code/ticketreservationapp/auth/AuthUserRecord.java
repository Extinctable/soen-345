package code.ticketreservationapp.auth;

final class AuthUserRecord {

    private final String username;
    private final String password;
    private final LoginRole role;
    private final String email;
    private final String phone;

    AuthUserRecord(String username, String password, LoginRole role, String email, String phone) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.phone = phone;
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    LoginRole getRole() {
        return role;
    }

    String getEmail() {
        return email;
    }

    String getPhone() {
        return phone;
    }
}
