package code.ticketreservationapp.auth;

import android.content.Context;
import android.widget.Toast;

public final class AuthSessionStore {

    private static AuthManager manager = AuthManager.createDefault();

    private AuthSessionStore() {}

    public interface LoginCallback {
        void onResult(boolean success, String message);
    }

    public interface RegisterCallback {
        void onResult(boolean success, String message);
    }

    public static void ensureDefaultAdminExists(Context context) {
        manager().ensureDefaultAdminExists((success, message) -> {
            if (success) {
                return;
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        });
    }

    public static void registerCustomer(String email, String phone, String password, RegisterCallback callback) {
        manager().registerCustomer(email, phone, password, callback::onResult);
    }

    public static void loginCustomer(String identifier, String password, LoginCallback callback) {
        manager().loginCustomer(identifier, password, callback::onResult);
    }

    public static void loginAdmin(String username, String password, LoginCallback callback) {
        manager().loginAdmin(username, password, callback::onResult);
    }

    public static boolean isLoggedIn() {
        return manager().isLoggedIn();
    }

    public static boolean isLoggedInAs(LoginRole role) {
        return manager().isLoggedInAs(role);
    }

    public static LoginRole currentRole() {
        return manager().currentRole();
    }

    public static String currentUsername() {
        return manager().currentUsername();
    }

    public static String currentEmail() {
        return manager().currentEmail();
    }

    public static String currentPhone() {
        return manager().currentPhone();
    }

    public static void logout() {
        manager().logout();
    }

    static synchronized void setManager(AuthManager replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("AuthManager cannot be null.");
        }
        manager = replacement;
    }

    static synchronized void resetManager() {
        manager = AuthManager.createDefault();
    }

    private static synchronized AuthManager manager() {
        return manager;
    }
}
