package code.ticketreservationapp.auth;

final class AuthSession {

    private static final AuthSession LOGGED_OUT = new AuthSession(null, null, null, null);

    private final LoginRole role;
    private final String username;
    private final String email;
    private final String phone;

    private AuthSession(LoginRole role, String username, String email, String phone) {
        this.role = role;
        this.username = username;
        this.email = email;
        this.phone = phone;
    }

    static AuthSession loggedOut() {
        return LOGGED_OUT;
    }

    static AuthSession forAdmin(String username) {
        return new AuthSession(LoginRole.ADMIN, username, null, null);
    }

    static AuthSession forUser(String username, String email, String phone) {
        return new AuthSession(LoginRole.USER, username, email, phone);
    }

    boolean isLoggedIn() {
        return role != null;
    }

    boolean isLoggedInAs(LoginRole requiredRole) {
        return role == requiredRole;
    }

    LoginRole getRole() {
        return role;
    }

    String getUsername() {
        return username;
    }

    String getEmail() {
        return email;
    }

    String getPhone() {
        return phone;
    }
}
