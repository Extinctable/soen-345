package code.ticketreservationapp.auth;

import java.util.List;

public final class AuthManager {

    private static final String DEFAULT_ADMIN_USERNAME = "123";
    private static final String DEFAULT_ADMIN_PASSWORD = "123";

    public interface ResultCallback {
        void onResult(boolean success, String message);
    }

    private final AuthUserStore userStore;
    private volatile AuthSession currentSession = AuthSession.loggedOut();

    public AuthManager(AuthUserStore userStore) {
        if (userStore == null) {
            throw new IllegalArgumentException("AuthUserStore cannot be null.");
        }
        this.userStore = userStore;
    }

    public static AuthManager createDefault() {
        return new AuthManager(new FirebaseAuthUserStore());
    }

    public void ensureDefaultAdminExists(ResultCallback callback) {
        userStore.fetchUser(DEFAULT_ADMIN_USERNAME, (success, user, message) -> {
            if (!success) {
                callback.onResult(false, "Default admin check failed: " + message);
                return;
            }

            boolean shouldRestore = user == null
                    || user.getRole() != LoginRole.ADMIN
                    || !DEFAULT_ADMIN_PASSWORD.equals(user.getPassword());
            if (!shouldRestore) {
                callback.onResult(true, "Default admin is ready.");
                return;
            }

            userStore.saveUser(
                    DEFAULT_ADMIN_USERNAME,
                    new AuthUserRecord(DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD, LoginRole.ADMIN, "", ""),
                    (saveSuccess, saveMessage) -> {
                        if (saveSuccess) {
                            callback.onResult(true, "Default admin is ready.");
                            return;
                        }
                        callback.onResult(false, "Default admin restore failed: " + saveMessage);
                    }
            );
        });
    }

    public void registerCustomer(String email, String phone, String password, ResultCallback callback) {
        String normalizedEmail = AuthInputNormalizer.normalizeEmail(email);
        String normalizedPhone = AuthInputNormalizer.normalizePhone(phone);
        String normalizedPassword = password == null ? "" : password;
        if (normalizedEmail.isEmpty() || normalizedPhone.isEmpty() || normalizedPassword.isEmpty()) {
            callback.onResult(false, "Email, phone number, and password are required.");
            return;
        }
        if (!AuthInputNormalizer.isValidEmail(normalizedEmail)) {
            callback.onResult(false, "Please enter a valid email address.");
            return;
        }
        if (!AuthInputNormalizer.isValidPhone(normalizedPhone)) {
            callback.onResult(false, "Please enter a valid phone number.");
            return;
        }

        userStore.fetchAllUsers((success, users, message) -> {
            if (!success) {
                callback.onResult(false, "Registration failed: " + message);
                return;
            }

            for (AuthUserRecord user : users) {
                if (user.getRole() != LoginRole.USER) {
                    continue;
                }

                String existingEmail = AuthInputNormalizer.normalizeEmail(user.getEmail());
                String existingPhone = AuthInputNormalizer.normalizePhone(user.getPhone());
                if (existingEmail.equals(normalizedEmail)) {
                    callback.onResult(false, "That email is already registered.");
                    return;
                }
                if (existingPhone.equals(normalizedPhone)) {
                    callback.onResult(false, "That phone number is already registered.");
                    return;
                }
            }

            String userKey = userStore.createUserId();
            if (userKey == null || userKey.isEmpty()) {
                callback.onResult(false, "Registration failed: could not create a user id.");
                return;
            }

            userStore.saveUser(
                    userKey,
                    new AuthUserRecord(userKey, normalizedPassword, LoginRole.USER, normalizedEmail, normalizedPhone),
                    (saveSuccess, saveMessage) -> {
                        if (saveSuccess) {
                            callback.onResult(true, "Registration successful. You can now log in with your email or phone.");
                            return;
                        }
                        callback.onResult(false, "Registration failed: " + saveMessage);
                    }
            );
        });
    }

    public void loginCustomer(String identifier, String password, ResultCallback callback) {
        String normalizedIdentifier = identifier == null ? "" : identifier;
        String normalizedPassword = password == null ? "" : password;
        if (normalizedIdentifier.isEmpty() || normalizedPassword.isEmpty()) {
            callback.onResult(false, "Sign-in identifier and password are required.");
            return;
        }

        String normalizedEmail = AuthInputNormalizer.normalizeEmail(normalizedIdentifier);
        String normalizedPhone = AuthInputNormalizer.normalizePhone(normalizedIdentifier);
        boolean usingEmail = AuthInputNormalizer.isValidEmail(normalizedEmail);
        boolean usingPhone = AuthInputNormalizer.isValidPhone(normalizedPhone);
        if (!usingEmail && !usingPhone) {
            callback.onResult(false, "Enter a valid email address or phone number.");
            return;
        }

        userStore.fetchAllUsers((success, users, message) -> {
            if (!success) {
                callback.onResult(false, "Login failed: " + message);
                return;
            }

            AuthUserRecord matchedUser = findMatchingCustomer(users, normalizedEmail, normalizedPhone, usingEmail, usingPhone);
            if (matchedUser == null) {
                callback.onResult(false, "User not found.");
                return;
            }
            if (matchedUser.getPassword() == null || matchedUser.getPassword().isEmpty()) {
                callback.onResult(false, "Corrupt user record.");
                return;
            }
            if (!matchedUser.getPassword().equals(normalizedPassword)) {
                callback.onResult(false, "Incorrect password.");
                return;
            }

            String dbEmail = AuthInputNormalizer.normalizeEmail(matchedUser.getEmail());
            String dbPhone = AuthInputNormalizer.normalizePhone(matchedUser.getPhone());
            if (!AuthInputNormalizer.isValidEmail(dbEmail) || !AuthInputNormalizer.isValidPhone(dbPhone)) {
                callback.onResult(
                        false,
                        "This customer account is missing a valid email or phone number. Please register again."
                );
                return;
            }

            currentSession = AuthSession.forUser(matchedUser.getUsername(), dbEmail, dbPhone);
            callback.onResult(true, "Login successful.");
        });
    }

    public void loginAdmin(String username, String password, ResultCallback callback) {
        String normalizedUsername = username == null ? "" : username;
        String normalizedPassword = password == null ? "" : password;
        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            callback.onResult(false, "Username and password are required.");
            return;
        }

        userStore.fetchUser(normalizedUsername, (success, user, message) -> {
            if (!success) {
                callback.onResult(false, "Login failed: " + message);
                return;
            }
            if (user == null) {
                callback.onResult(false, "User not found.");
                return;
            }
            if (user.getPassword() == null || user.getPassword().isEmpty() || user.getRole() == null) {
                callback.onResult(false, "Corrupt user record.");
                return;
            }
            if (!user.getPassword().equals(normalizedPassword)) {
                callback.onResult(false, "Incorrect password.");
                return;
            }
            if (user.getRole() != LoginRole.ADMIN) {
                callback.onResult(false, "Role mismatch.");
                return;
            }

            currentSession = AuthSession.forAdmin(normalizedUsername);
            callback.onResult(true, "Login successful.");
        });
    }

    public boolean isLoggedIn() {
        return currentSession.isLoggedIn();
    }

    public boolean isLoggedInAs(LoginRole role) {
        return currentSession.isLoggedInAs(role);
    }

    public LoginRole currentRole() {
        return currentSession.getRole();
    }

    public String currentUsername() {
        return currentSession.getUsername();
    }

    public String currentEmail() {
        return currentSession.getEmail();
    }

    public String currentPhone() {
        return currentSession.getPhone();
    }

    public void logout() {
        currentSession = AuthSession.loggedOut();
    }

    private AuthUserRecord findMatchingCustomer(
            List<AuthUserRecord> users,
            String normalizedEmail,
            String normalizedPhone,
            boolean usingEmail,
            boolean usingPhone
    ) {
        for (AuthUserRecord user : users) {
            if (user.getRole() != LoginRole.USER) {
                continue;
            }

            String dbEmail = AuthInputNormalizer.normalizeEmail(user.getEmail());
            String dbPhone = AuthInputNormalizer.normalizePhone(user.getPhone());
            boolean matchedEmail = usingEmail && dbEmail.equals(normalizedEmail);
            boolean matchedPhone = usingPhone && dbPhone.equals(normalizedPhone);
            if (matchedEmail || matchedPhone) {
                return user;
            }
        }
        return null;
    }
}
