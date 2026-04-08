package code.ticketreservationapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import code.ticketreservationapp.auth.AuthManager;
import code.ticketreservationapp.auth.AuthUserRecord;
import code.ticketreservationapp.auth.AuthUserStore;
import code.ticketreservationapp.auth.LoginRole;

public class AuthManagerTest {

    private FakeAuthUserStore userStore;
    private AuthManager manager;

    @Before
    public void setUp() {
        userStore = new FakeAuthUserStore();
        manager = new AuthManager(userStore);
    }

    // Prime Path Coverage Test Case 1 for method ensureDefaultAdminExists
    // Prime Path: fetchUser failure -> callback failure
    @Test
    @DisplayName("Default admin check reports fetch failures")
    public void ensureDefaultAdminExists_fetchFailure_reportsError() {
        userStore.fetchUserSuccess = false;
        userStore.fetchUserMessage = "database unavailable";

        CallbackResult result = capture(callback -> manager.ensureDefaultAdminExists(callback));

        assertFalse(result.success);
        assertEquals("Default admin check failed: database unavailable", result.message);
        assertEquals(1, userStore.fetchUserCalls);
        assertEquals(0, userStore.saveUserCalls);
    }

    // Prime Path Coverage Test Case 2 for method ensureDefaultAdminExists
    // Prime Path: fetchUser success -> valid admin exists -> callback success
    @Test
    @DisplayName("Existing default admin is accepted")
    public void ensureDefaultAdminExists_existingAdmin_isReady() {
        userStore.putUser(admin("123", "123"));

        CallbackResult result = capture(callback -> manager.ensureDefaultAdminExists(callback));

        assertTrue(result.success);
        assertEquals("Default admin is ready.", result.message);
        assertEquals(1, userStore.fetchUserCalls);
        assertEquals(0, userStore.saveUserCalls);
    }

    // Prime Path Coverage Test Case 3 for method ensureDefaultAdminExists
    // Prime Path: fetchUser success -> admin missing -> save success -> callback success
    @Test
    @DisplayName("Missing default admin is restored")
    public void ensureDefaultAdminExists_missingAdmin_restoresDefaultAdmin() {
        CallbackResult result = capture(callback -> manager.ensureDefaultAdminExists(callback));

        assertTrue(result.success);
        assertEquals("Default admin is ready.", result.message);
        assertEquals(1, userStore.fetchUserCalls);
        assertEquals(1, userStore.saveUserCalls);
        assertEquals("123", userStore.lastSavedUsername);
        assertEquals(LoginRole.ADMIN, userStore.lastSavedUser.getRole());
        assertEquals("123", userStore.lastSavedUser.getPassword());
    }

    // Implicant Coverage Test Case 1 for method registerCustomer
    // A = normalizedEmail.isEmpty() -> true
    @Test
    @DisplayName("Registration rejects a missing email")
    public void registerCustomer_missingEmail_rejected() {
        CallbackResult result = capture(
                callback -> manager.registerCustomer("", "5145550101", "secret", callback)
        );

        assertFalse(result.success);
        assertEquals("Email, phone number, and password are required.", result.message);
        assertEquals(0, userStore.fetchAllUsersCalls);
    }

    // Implicant Coverage Test Case 2 for method registerCustomer
    // B = normalizedPhone.isEmpty() -> true
    @Test
    @DisplayName("Registration rejects a missing phone number")
    public void registerCustomer_missingPhone_rejected() {
        CallbackResult result = capture(
                callback -> manager.registerCustomer("user@example.com", "   ", "secret", callback)
        );

        assertFalse(result.success);
        assertEquals("Email, phone number, and password are required.", result.message);
        assertEquals(0, userStore.fetchAllUsersCalls);
    }

    // Implicant Coverage Test Case 3 for method registerCustomer
    // C = normalizedPassword.isEmpty() -> true
    @Test
    @DisplayName("Registration rejects a missing password")
    public void registerCustomer_missingPassword_rejected() {
        CallbackResult result = capture(
                callback -> manager.registerCustomer("user@example.com", "5145550101", null, callback)
        );

        assertFalse(result.success);
        assertEquals("Email, phone number, and password are required.", result.message);
        assertEquals(0, userStore.fetchAllUsersCalls);
    }

    // All-DU-Paths Coverage Test Case 1 for method loginCustomer
    // DU path: matchedUser definition -> null check use
    @Test
    @DisplayName("Customer login reports unknown users")
    public void loginCustomer_userNotFound_reportsError() {
        userStore.putUser(admin("123", "123"));

        CallbackResult result = capture(
                callback -> manager.loginCustomer("customer@example.com", "secret", callback)
        );

        assertFalse(result.success);
        assertEquals("User not found.", result.message);
        assertFalse(manager.isLoggedIn());
    }

    // All-DU-Paths Coverage Test Case 2 for method loginCustomer
    // DU path: matchedUser definition -> password validation use
    @Test
    @DisplayName("Customer login detects corrupt user records")
    public void loginCustomer_corruptPassword_reportsError() {
        userStore.putUser(customer("customer-id", "", "customer@example.com", "5145550101"));

        CallbackResult result = capture(
                callback -> manager.loginCustomer("customer@example.com", "secret", callback)
        );

        assertFalse(result.success);
        assertEquals("Corrupt user record.", result.message);
        assertFalse(manager.isLoggedIn());
    }

    // All-DU-Paths Coverage Test Case 3 for method loginCustomer
    // DU path: matchedUser definition -> currentSession assignment
    @Test
    @DisplayName("Customer login succeeds with email credentials")
    public void loginCustomer_successByEmail_updatesSession() {
        userStore.putUser(customer("customer-id", "secret", "Customer@Example.com", "(514) 555-0101"));

        CallbackResult result = capture(
                callback -> manager.loginCustomer("customer@example.com", "secret", callback)
        );

        assertTrue(result.success);
        assertEquals("Login successful.", result.message);
        assertTrue(manager.isLoggedIn());
        assertEquals(LoginRole.USER, manager.currentRole());
        assertEquals("customer-id", manager.currentUsername());
        assertEquals("customer@example.com", manager.currentEmail());
        assertEquals("5145550101", manager.currentPhone());
    }

    private CallbackResult capture(Consumer<AuthManager.ResultCallback> action) {
        CallbackResult result = new CallbackResult();
        action.accept(result);
        assertEquals(1, result.callCount);
        return result;
    }

    private static AuthUserRecord customer(String username, String password, String email, String phone) {
        return new AuthUserRecord(username, password, LoginRole.USER, email, phone);
    }

    private static AuthUserRecord admin(String username, String password) {
        return new AuthUserRecord(username, password, LoginRole.ADMIN, "", "");
    }

    private static final class CallbackResult implements AuthManager.ResultCallback {
        private boolean success;
        private String message;
        private int callCount;

        @Override
        public void onResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.callCount++;
        }
    }

    private static final class FakeAuthUserStore implements AuthUserStore {
        private final Map<String, AuthUserRecord> usersByUsername = new LinkedHashMap<>();

        private boolean fetchUserSuccess = true;
        private String fetchUserMessage = "";

        private int fetchAllUsersCalls;
        private int fetchUserCalls;
        private int saveUserCalls;
        private String lastSavedUsername;
        private AuthUserRecord lastSavedUser;

        @Override
        public void fetchAllUsers(AuthUserStore.UsersCallback callback) {
            fetchAllUsersCalls++;
            List<AuthUserRecord> users = new ArrayList<>(usersByUsername.values());
            callback.onResult(true, users, "");
        }

        @Override
        public void fetchUser(String username, AuthUserStore.UserCallback callback) {
            fetchUserCalls++;
            callback.onResult(fetchUserSuccess, usersByUsername.get(username), fetchUserMessage);
        }

        @Override
        public void saveUser(String username, AuthUserRecord user, AuthUserStore.OperationCallback callback) {
            saveUserCalls++;
            lastSavedUsername = username;
            lastSavedUser = user;
            usersByUsername.put(username, user);
            callback.onResult(true, "");
        }

        @Override
        public String createUserId() {
            return "generated-user";
        }

        private void putUser(AuthUserRecord user) {
            usersByUsername.put(user.getUsername(), user);
        }
    }
}
