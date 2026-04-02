package code.ticketreservationapp.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

final class FirebaseTicketDatabase implements ReservationStateGateway {

    private static final String ROOT_PATH = "ticketReservationApp";

    private final DatabaseReference rootReference;

    private FirebaseTicketDatabase(DatabaseReference rootReference) {
        this.rootReference = rootReference;
    }

    static FirebaseTicketDatabase tryCreate() {
        try {
            FirebaseApp.getInstance();
            return new FirebaseTicketDatabase(FirebaseDatabase.getInstance().getReference(ROOT_PATH));
        } catch (IllegalStateException | DatabaseException ignored) {
            // If Firebase is present but Realtime Database is not configured yet,
            // keep the app running in local mode instead of crashing.
            return null;
        } catch (RuntimeException ignored) {
            // Defensive fallback for unexpected Firebase initialization failures.
            return null;
        }
    }

    @Override
    public void subscribe(SnapshotListener listener) {
        rootReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getValue() == null) {
                    listener.onSnapshot(new LinkedHashMap<>());
                    return;
                }

                Object value = snapshot.getValue();
                if (!(value instanceof Map)) {
                    listener.onSnapshot(new LinkedHashMap<>());
                    return;
                }

                listener.onSnapshot(normalizeMap((Map<?, ?>) value));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onSnapshot(null);
            }
        });
    }

    @Override
    public void writeSnapshot(Map<String, Object> snapshot) {
        // Preserve sibling branches like ticketReservationApp/users when the
        // reservation service syncs only its own event/reservation state.
        rootReference.updateChildren(snapshot);
    }

    private Map<String, Object> normalizeMap(Map<?, ?> source) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            normalized.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return normalized;
    }
}
