package code.ticketreservationapp.auth;

public final class AuthSession {

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

    public static AuthSession loggedOut() {
        return LOGGED_OUT;
    }

    public static AuthSession forAdmin(String username) {
        return new AuthSession(LoginRole.ADMIN, username, null, null);
    }

    public static AuthSession forUser(String username, String email, String phone) {
        return new AuthSession(LoginRole.USER, username, email, phone);
    }

    public boolean isLoggedIn() {
        return role != null;
    }

    public boolean isLoggedInAs(LoginRole requiredRole) {
        return role == requiredRole;
    }

    public LoginRole getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
}
