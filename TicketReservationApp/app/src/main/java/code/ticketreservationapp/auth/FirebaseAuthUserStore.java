package code.ticketreservationapp.auth;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class FirebaseAuthUserStore implements AuthUserStore {

    private static final String USERS_PATH = "ticketReservationApp/users";

    @Override
    public void fetchAllUsers(UsersCallback callback) {
        usersReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<AuthUserRecord> users = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    users.add(toUserRecord(userSnapshot));
                }
                callback.onResult(true, users, "");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onResult(false, new ArrayList<>(), error.getMessage());
            }
        });
    }

    @Override
    public void fetchUser(String username, UserCallback callback) {
        usersReference().child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getValue() == null) {
                    callback.onResult(true, null, "");
                    return;
                }
                callback.onResult(true, toUserRecord(snapshot), "");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onResult(false, null, error.getMessage());
            }
        });
    }

    @Override
    public void saveUser(String username, AuthUserRecord user, OperationCallback callback) {
        usersReference()
                .child(username)
                .setValue(new UserSeed(user.getPassword(), roleName(user.getRole()), user.getEmail(), user.getPhone()))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onResult(true, "");
                        return;
                    }
                    String message = task.getException() != null
                            ? task.getException().getMessage()
                            : "unknown error";
                    callback.onResult(false, message);
                });
    }

    @Override
    public String createUserId() {
        String key = usersReference().push().getKey();
        return key == null ? "" : key;
    }

    private DatabaseReference usersReference() {
        return FirebaseDatabase.getInstance().getReference(USERS_PATH);
    }

    private AuthUserRecord toUserRecord(DataSnapshot snapshot) {
        return new AuthUserRecord(
                snapshot.getKey(),
                childValue(snapshot, "password"),
                parseRole(childValue(snapshot, "role")),
                childValue(snapshot, "email"),
                childValue(snapshot, "phone")
        );
    }

    private LoginRole parseRole(String rawRole) {
        try {
            return LoginRole.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String roleName(LoginRole role) {
        return role == null ? "" : role.name();
    }

    private String childValue(DataSnapshot snapshot, String childKey) {
        Object value = snapshot.child(childKey).getValue();
        return value == null ? "" : String.valueOf(value);
    }

    private static final class UserSeed {
        public String password;
        public String role;
        public String email;
        public String phone;

        private UserSeed() {}

        private UserSeed(String password, String role, String email, String phone) {
            this.password = password;
            this.role = role;
            this.email = email;
            this.phone = phone;
        }
    }
}
