package code.ticketreservationapp.auth;

import java.util.List;

interface AuthUserStore {

    interface UsersCallback {
        void onResult(boolean success, List<AuthUserRecord> users, String message);
    }

    interface UserCallback {
        void onResult(boolean success, AuthUserRecord user, String message);
    }

    interface OperationCallback {
        void onResult(boolean success, String message);
    }

    void fetchAllUsers(UsersCallback callback);

    void fetchUser(String username, UserCallback callback);

    void saveUser(String username, AuthUserRecord user, OperationCallback callback);

    String createUserId();
}
