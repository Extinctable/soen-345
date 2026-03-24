package code.ticketreservationapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import code.ticketreservationapp.model.ConfirmationRecord;
import code.ticketreservationapp.model.ContactMethod;
import code.ticketreservationapp.model.Event;
import code.ticketreservationapp.model.EventCategory;
import code.ticketreservationapp.model.EventFilter;
import code.ticketreservationapp.model.Reservation;
import code.ticketreservationapp.model.UserProfile;
import code.ticketreservationapp.service.TicketReservationService;

public class WelcomePage extends AppCompatActivity {

    private static final String ADMIN_PROMPT = "Select an event to edit or cancel it.";
    private static final String ADMIN_NONE = "No active event selected.";

    private final TicketReservationService service = new TicketReservationService();

    private View rootView;
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
    private android.widget.LinearLayout eventsContainer;
    private android.widget.LinearLayout reservationsContainer;
    private android.widget.LinearLayout confirmationsContainer;

    private Spinner adminEventSpinner;
    private TextView adminSelectionText;
    private TextInputEditText adminTitleInput;
    private TextInputEditText adminLocationInput;
    private TextInputEditText adminDateInput;
    private Spinner adminCategorySpinner;
    private TextInputEditText adminTicketsInput;
    private TextInputEditText adminPriceInput;

    private Event selectedAdminEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page);

        rootView = findViewById(R.id.main);
        inflater = LayoutInflater.from(this);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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

        adminEventSpinner = findViewById(R.id.adminEventSpinner);
        adminSelectionText = findViewById(R.id.adminSelectionText);
        adminTitleInput = findViewById(R.id.adminTitleInput);
        adminLocationInput = findViewById(R.id.adminLocationInput);
        adminDateInput = findViewById(R.id.adminDateInput);
        adminCategorySpinner = findViewById(R.id.adminCategorySpinner);
        adminTicketsInput = findViewById(R.id.adminTicketsInput);
        adminPriceInput = findViewById(R.id.adminPriceInput);
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

        ArrayAdapter<EventCategory> adminCategoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                EventCategory.editableValues()
        );
        adminCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adminCategorySpinner.setAdapter(adminCategoryAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.registerButton).setOnClickListener(v -> handleRegistration());
        findViewById(R.id.applyFiltersButton).setOnClickListener(v -> refreshEventList());
        findViewById(R.id.clearFiltersButton).setOnClickListener(v -> clearFilters());
        findViewById(R.id.adminAddButton).setOnClickListener(v -> handleAddEvent());
        findViewById(R.id.adminUpdateButton).setOnClickListener(v -> handleUpdateEvent());
        findViewById(R.id.adminCancelButton).setOnClickListener(v -> handleCancelEvent());
        findViewById(R.id.adminResetButton).setOnClickListener(v -> resetAdminForm());

        adminEventSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedAdminEvent = null;
                    adminSelectionText.setText(ADMIN_NONE);
                    return;
                }

                selectedAdminEvent = (Event) parent.getItemAtPosition(position);
                adminSelectionText.setText("Selected: " + selectedAdminEvent.getTitle());
                populateAdminForm(selectedAdminEvent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedAdminEvent = null;
                adminSelectionText.setText(ADMIN_NONE);
            }
        });
    }

    private void handleRegistration() {
        try {
            ContactMethod method = (ContactMethod) contactMethodSpinner.getSelectedItem();
            UserProfile user = service.registerUser(
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

    private void handleAddEvent() {
        try {
            Event createdEvent = service.addEvent(
                    valueOf(adminTitleInput),
                    valueOf(adminLocationInput),
                    parseDate(valueOf(adminDateInput), "Event date must use YYYY-MM-DD."),
                    (EventCategory) adminCategorySpinner.getSelectedItem(),
                    parsePositiveInt(valueOf(adminTicketsInput), "Ticket inventory must be a positive whole number."),
                    parsePositivePrice(valueOf(adminPriceInput))
            );
            showMessage(createdEvent.getTitle() + " added to the event catalog.");
            selectedAdminEvent = createdEvent;
            refreshAll();
            selectAdminEvent(createdEvent.getId());
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        }
    }

    private void handleUpdateEvent() {
        if (selectedAdminEvent == null) {
            showMessage(ADMIN_PROMPT);
            return;
        }

        try {
            Event updatedEvent = service.updateEvent(
                    selectedAdminEvent.getId(),
                    valueOf(adminTitleInput),
                    valueOf(adminLocationInput),
                    parseDate(valueOf(adminDateInput), "Event date must use YYYY-MM-DD."),
                    (EventCategory) adminCategorySpinner.getSelectedItem(),
                    parseNonNegativeInt(valueOf(adminTicketsInput), "Remaining tickets cannot be negative."),
                    parsePositivePrice(valueOf(adminPriceInput))
            );
            showMessage(updatedEvent.getTitle() + " updated.");
            refreshAll();
            selectAdminEvent(updatedEvent.getId());
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        }
    }

    private void handleCancelEvent() {
        if (selectedAdminEvent == null) {
            showMessage(ADMIN_PROMPT);
            return;
        }

        try {
            Event cancelledEvent = service.cancelEvent(selectedAdminEvent.getId());
            showMessage(cancelledEvent.getTitle() + " was cancelled.");
            selectedAdminEvent = null;
            refreshAll();
            resetAdminForm();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        }
    }

    private void resetAdminForm() {
        adminTitleInput.setText("");
        adminLocationInput.setText("");
        adminDateInput.setText("");
        adminTicketsInput.setText("");
        adminPriceInput.setText("");
        adminCategorySpinner.setSelection(0);
        adminEventSpinner.setSelection(0);
        selectedAdminEvent = null;
        adminSelectionText.setText(ADMIN_NONE);
    }

    private void populateAdminForm(Event event) {
        adminTitleInput.setText(event.getTitle());
        adminLocationInput.setText(event.getLocation());
        adminDateInput.setText(event.getDate().toString());
        adminTicketsInput.setText(String.valueOf(event.getAvailableTickets()));
        adminPriceInput.setText(String.valueOf(event.getPrice()));

        EventCategory[] categories = EventCategory.editableValues();
        for (int i = 0; i < categories.length; i++) {
            if (categories[i] == event.getCategory()) {
                adminCategorySpinner.setSelection(i);
                return;
            }
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
        refreshAdminEvents();
    }

    private void refreshUserState() {
        UserProfile registeredUser = service.getRegisteredUser();
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
        List<Event> events = service.getAvailableEvents(filter);

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
            reserveButton.setOnClickListener(v -> handleReserve(event, quantityInput));

            eventsContainer.addView(itemView);
        }
    }

    private void refreshReservations() {
        List<Reservation> reservations = service.getReservations();

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
            cancelButton.setOnClickListener(v -> handleReservationCancellation(reservation));

            reservationsContainer.addView(itemView);
        }
    }

    private void refreshConfirmations() {
        List<ConfirmationRecord> confirmations = service.getConfirmationHistory();

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

    private void refreshAdminEvents() {
        List<Event> activeEvents = service.getActiveEventsForAdmin();
        List<Object> spinnerItems = new ArrayList<>();
        spinnerItems.add(getString(R.string.admin_spinner_placeholder));
        spinnerItems.addAll(activeEvents);

        ArrayAdapter<Object> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                spinnerItems
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adminEventSpinner.setAdapter(adapter);

        if (selectedAdminEvent != null) {
            selectAdminEvent(selectedAdminEvent.getId());
        } else {
            adminSelectionText.setText(activeEvents.isEmpty() ? ADMIN_NONE : ADMIN_PROMPT);
        }
    }

    private void selectAdminEvent(String eventId) {
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) adminEventSpinner.getAdapter();
        if (adapter == null) {
            return;
        }

        for (int i = 1; i < adapter.getCount(); i++) {
            Event event = (Event) adapter.getItem(i);
            if (event != null && event.getId().equals(eventId)) {
                adminEventSpinner.setSelection(i);
                return;
            }
        }

        adminEventSpinner.setSelection(0);
    }

    private void handleReserve(Event event, TextInputEditText quantityInput) {
        try {
            int quantity = parsePositiveInt(valueOf(quantityInput), "Reservation quantity must be a positive whole number.");
            Reservation reservation = service.reserveTickets(event.getId(), quantity);
            showMessage("Reservation " + reservation.getId() + " confirmed.");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        }
    }

    private void handleReservationCancellation(Reservation reservation) {
        try {
            Reservation cancelledReservation = service.cancelReservation(reservation.getId());
            showMessage("Reservation " + cancelledReservation.getId() + " cancelled.");
            refreshAll();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
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

    private LocalDate parseDate(String value, String errorMessage) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private int parsePositiveInt(String value, String errorMessage) {
        int parsedValue = parseInt(value, errorMessage);
        if (parsedValue <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return parsedValue;
    }

    private int parseNonNegativeInt(String value, String errorMessage) {
        int parsedValue = parseInt(value, errorMessage);
        if (parsedValue < 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return parsedValue;
    }

    private int parseInt(String value, String errorMessage) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private double parsePositivePrice(String value) {
        try {
            double price = Double.parseDouble(value);
            if (price <= 0) {
                throw new IllegalArgumentException("Price must be greater than zero.");
            }
            return price;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Price must be a valid number.");
        }
    }

    private void showMessage(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }
}
