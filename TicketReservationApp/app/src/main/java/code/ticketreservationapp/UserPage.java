package code.ticketreservationapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import code.ticketreservationapp.model.ConfirmationRecord;
import code.ticketreservationapp.model.ContactMethod;
import code.ticketreservationapp.model.Event;
import code.ticketreservationapp.model.EventCategory;
import code.ticketreservationapp.model.EventFilter;
import code.ticketreservationapp.model.Reservation;
import code.ticketreservationapp.model.UserProfile;

public class UserPage extends BasePageActivity {

    private LayoutInflater inflater;

    private TextInputEditText nameInput;
    private TextInputEditText contactInput;
    private Spinner contactMethodSpinner;
    private TextView currentUserText;

    private TextInputEditText searchInput;
    private TextInputEditText filterDateInput;
    private TextInputEditText filterLocationInput;
    private Spinner filterCategorySpinner;

    private View eventsEmptyState;
    private View reservationsEmptyState;
    private View confirmationsEmptyState;
    private LinearLayout eventsContainer;
    private LinearLayout reservationsContainer;
    private LinearLayout confirmationsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_page);

        initializePage(R.id.main);
        inflater = LayoutInflater.from(this);

        bindViews();
        setupSpinners();
        setupListeners();
        refreshAll();
    }

    private void bindViews() {
        nameInput = findViewById(R.id.nameInput);
        contactInput = findViewById(R.id.contactInput);
        contactMethodSpinner = findViewById(R.id.contactMethodSpinner);
        currentUserText = findViewById(R.id.currentUserText);

        searchInput = findViewById(R.id.searchInput);
        filterDateInput = findViewById(R.id.filterDateInput);
        filterLocationInput = findViewById(R.id.filterLocationInput);
        filterCategorySpinner = findViewById(R.id.filterCategorySpinner);

        eventsContainer = findViewById(R.id.eventsContainer);
        reservationsContainer = findViewById(R.id.reservationsContainer);
        confirmationsContainer = findViewById(R.id.confirmationsContainer);
        eventsEmptyState = findViewById(R.id.eventsEmptyState);
        reservationsEmptyState = findViewById(R.id.reservationsEmptyState);
        confirmationsEmptyState = findViewById(R.id.confirmationsEmptyState);
    }

    private void setupSpinners() {
        ArrayAdapter<ContactMethod> contactAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                ContactMethod.values()
        );
        contactAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        contactMethodSpinner.setAdapter(contactAdapter);

        ArrayAdapter<EventCategory> filterCategoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                EventCategory.filterValues()
        );
        filterCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterCategorySpinner.setAdapter(filterCategoryAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.backToWelcomeButton).setOnClickListener(view -> returnToWelcomePage());
        findViewById(R.id.switchToAdminButton).setOnClickListener(view -> replacePage(AdminPage.class));
        findViewById(R.id.registerButton).setOnClickListener(view -> handleRegistration());
        findViewById(R.id.applyFiltersButton).setOnClickListener(view -> refreshEventList());
        findViewById(R.id.clearFiltersButton).setOnClickListener(view -> clearFilters());
    }

    private void handleRegistration() {
        try {
            ContactMethod method = (ContactMethod) contactMethodSpinner.getSelectedItem();
            UserProfile user = service().registerUser(
                    valueOf(nameInput),
                    valueOf(contactInput),
                    method
            );
            showMessage("Registration complete for " + user.getFullName() + ".");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        }
    }

    private void handleReserve(Event event, TextInputEditText quantityInput) {
        try {
            int quantity = parsePositiveInt(valueOf(quantityInput), "Reservation quantity must be a positive whole number.");
            Reservation reservation = service().reserveTickets(event.getId(), quantity);
            showMessage("Reservation " + reservation.getId() + " confirmed.");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        }
    }

    private void handleReservationCancellation(Reservation reservation) {
        try {
            Reservation cancelledReservation = service().cancelReservation(reservation.getId());
            showMessage("Reservation " + cancelledReservation.getId() + " cancelled.");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        }
    }

    private void clearFilters() {
        searchInput.setText("");
        filterDateInput.setText("");
        filterLocationInput.setText("");
        filterCategorySpinner.setSelection(0);
        refreshEventList();
    }

    private void refreshAll() {
        refreshUserState();
        refreshEventList();
        refreshReservations();
        refreshConfirmations();
    }

    private void refreshUserState() {
        UserProfile registeredUser = service().getRegisteredUser();
        if (registeredUser == null) {
            currentUserText.setText(getString(R.string.no_registered_user));
            return;
        }

        currentUserText.setText(
                getString(
                        R.string.current_user_format,
                        registeredUser.getFullName(),
                        registeredUser.getContactMethod().toString(),
                        registeredUser.getContactValue()
                )
        );
    }

    private void refreshEventList() {
        EventFilter filter = createFilterFromInputs();
        List<Event> events = service().getAvailableEvents(filter);

        eventsContainer.removeAllViews();
        eventsEmptyState.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);

        for (Event event : events) {
            View itemView = inflater.inflate(R.layout.item_event, eventsContainer, false);
            TextView titleView = itemView.findViewById(R.id.eventTitleText);
            TextView detailView = itemView.findViewById(R.id.eventDetailText);
            TextInputEditText quantityInput = itemView.findViewById(R.id.quantityInput);
            MaterialButton reserveButton = itemView.findViewById(R.id.reserveButton);

            titleView.setText(event.getTitle());
            detailView.setText(formatEventDetails(event));
            quantityInput.setText("1");
            reserveButton.setOnClickListener(view -> handleReserve(event, quantityInput));

            eventsContainer.addView(itemView);
        }
    }

    private void refreshReservations() {
        List<Reservation> reservations = service().getReservations();

        reservationsContainer.removeAllViews();
        reservationsEmptyState.setVisibility(reservations.isEmpty() ? View.VISIBLE : View.GONE);

        for (Reservation reservation : reservations) {
            View itemView = inflater.inflate(R.layout.item_reservation, reservationsContainer, false);
            TextView titleView = itemView.findViewById(R.id.reservationTitleText);
            TextView detailView = itemView.findViewById(R.id.reservationDetailText);
            MaterialButton cancelButton = itemView.findViewById(R.id.cancelReservationButton);

            titleView.setText(reservation.getEventTitle());
            detailView.setText(formatReservationDetails(reservation));
            cancelButton.setEnabled(reservation.isActive());
            cancelButton.setAlpha(reservation.isActive() ? 1.0f : 0.45f);
            cancelButton.setOnClickListener(view -> handleReservationCancellation(reservation));

            reservationsContainer.addView(itemView);
        }
    }

    private void refreshConfirmations() {
        List<ConfirmationRecord> confirmations = service().getConfirmationHistory();

        confirmationsContainer.removeAllViews();
        confirmationsEmptyState.setVisibility(confirmations.isEmpty() ? View.VISIBLE : View.GONE);

        for (ConfirmationRecord confirmation : confirmations) {
            View itemView = inflater.inflate(R.layout.item_confirmation, confirmationsContainer, false);
            TextView channelView = itemView.findViewById(R.id.confirmationChannelText);
            TextView messageView = itemView.findViewById(R.id.confirmationMessageText);

            channelView.setText(
                    getString(
                            R.string.confirmation_header_format,
                            confirmation.getChannel().toString(),
                            confirmation.getRecipient(),
                            confirmation.getCreatedAt().toString().replace('T', ' ')
                    )
            );
            messageView.setText(confirmation.getMessage());

            confirmationsContainer.addView(itemView);
        }
    }

    private EventFilter createFilterFromInputs() {
        LocalDate date = null;
        String rawDate = valueOf(filterDateInput);
        if (!TextUtils.isEmpty(rawDate)) {
            try {
                date = LocalDate.parse(rawDate);
            } catch (DateTimeParseException ex) {
                showMessage("Date filters must use YYYY-MM-DD. Showing all dates instead.");
                filterDateInput.setText("");
            }
        }

        return new EventFilter(
                valueOf(searchInput),
                valueOf(filterLocationInput),
                date,
                (EventCategory) filterCategorySpinner.getSelectedItem()
        );
    }

    private String formatEventDetails(Event event) {
        return getString(
                R.string.event_detail_format,
                event.getCategory().toString(),
                event.getDate().toString(),
                event.getLocation(),
                String.valueOf(event.getAvailableTickets()),
                String.format("$%.2f", event.getPrice())
        );
    }

    private String formatReservationDetails(Reservation reservation) {
        return getString(
                R.string.reservation_detail_format,
                reservation.getId(),
                String.valueOf(reservation.getQuantity()),
                reservation.getStatus().toString(),
                reservation.getChannel().toString()
        );
    }

    private String valueOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private int parsePositiveInt(String value, String errorMessage) {
        try {
            int parsedValue = Integer.parseInt(value);
            if (parsedValue <= 0) {
                throw new IllegalArgumentException(errorMessage);
            }
            return parsedValue;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
