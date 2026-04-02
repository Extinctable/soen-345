package code.ticketreservationapp.service;

import java.util.Map;

interface ReservationStateGateway {

    interface SnapshotListener {
        void onSnapshot(Map<String, Object> snapshot);
    }

    void subscribe(SnapshotListener listener);

    void writeSnapshot(Map<String, Object> snapshot);
}
