package code.ticketreservationapp.auth;

import java.util.List;

public interface AuthUserStore {

    public interface UsersCallback {
        void onResult(boolean success, List<AuthUserRecord> users, String message);
    }

    public interface UserCallback {
        void onResult(boolean success, AuthUserRecord user, String message);
    }

    public interface OperationCallback {
        void onResult(boolean success, String message);
    }

    void fetchAllUsers(UsersCallback callback);

    void fetchUser(String username, UserCallback callback);

    void saveUser(String username, AuthUserRecord user, OperationCallback callback);

    String createUserId();
}
