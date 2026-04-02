package code.ticketreservationapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import code.ticketreservationapp.R;
import code.ticketreservationapp.auth.LoginRole;
import code.ticketreservationapp.model.Event;
import code.ticketreservationapp.model.EventCategory;

public class AdminPage extends BasePageActivity {

    private static final String ADMIN_PROMPT = "Select an event to edit or cancel it.";
    private static final String ADMIN_NONE = "No active event selected.";

    private Spinner adminEventSpinner;
    private TextView adminSelectionText;
    private TextView adminStatusText;
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
        setContentView(R.layout.admin_page);

        initializePage(R.id.main);

        bindViews();
        setupSpinners();
        setupListeners();
        refreshAdminEvents();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!ensureAuthorized(LoginRole.ADMIN)) {
            return;
        }
        refreshAdminEvents();
    }

    private void bindViews() {
        adminEventSpinner = findViewById(R.id.adminEventSpinner);
        adminSelectionText = findViewById(R.id.adminSelectionText);
        adminStatusText = findViewById(R.id.adminStatusText);
        adminTitleInput = findViewById(R.id.adminTitleInput);
        adminLocationInput = findViewById(R.id.adminLocationInput);
        adminDateInput = findViewById(R.id.adminDateInput);
        adminCategorySpinner = findViewById(R.id.adminCategorySpinner);
        adminTicketsInput = findViewById(R.id.adminTicketsInput);
        adminPriceInput = findViewById(R.id.adminPriceInput);
    }

    private void setupSpinners() {
        ArrayAdapter<EventCategory> adminCategoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                EventCategory.editableValues()
        );
        adminCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adminCategorySpinner.setAdapter(adminCategoryAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.backToWelcomeButton).setOnClickListener(view -> logoutAndReturnToWelcomePage());
        findViewById(R.id.switchToUserButton).setOnClickListener(view -> logoutAndReturnToWelcomePage());
        findViewById(R.id.adminAddButton).setOnClickListener(view -> handleAddEvent());
        findViewById(R.id.adminUpdateButton).setOnClickListener(view -> handleUpdateEvent());
        findViewById(R.id.adminCancelButton).setOnClickListener(view -> handleCancelEvent());
        findViewById(R.id.adminResetButton).setOnClickListener(view -> resetAdminForm());

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

    private void handleAddEvent() {
        try {
            Event createdEvent = service().addEvent(
                    valueOf(adminTitleInput),
                    valueOf(adminLocationInput),
                    parseDate(valueOf(adminDateInput), "Event date must use YYYY-MM-DD."),
                    (EventCategory) adminCategorySpinner.getSelectedItem(),
                    parsePositiveInt(valueOf(adminTicketsInput), "Ticket inventory must be a positive whole number."),
                    parsePositivePrice(valueOf(adminPriceInput))
            );
            showMessage(createdEvent.getTitle() + " added to the event catalog.");
            selectedAdminEvent = createdEvent;
            refreshAdminEvents();
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
            Event updatedEvent = service().updateEvent(
                    selectedAdminEvent.getId(),
                    valueOf(adminTitleInput),
                    valueOf(adminLocationInput),
                    parseDate(valueOf(adminDateInput), "Event date must use YYYY-MM-DD."),
                    (EventCategory) adminCategorySpinner.getSelectedItem(),
                    parseNonNegativeInt(valueOf(adminTicketsInput), "Remaining tickets cannot be negative."),
                    parsePositivePrice(valueOf(adminPriceInput))
            );
            showMessage(updatedEvent.getTitle() + " updated.");
            refreshAdminEvents();
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
            Event cancelledEvent = service().cancelEvent(selectedAdminEvent.getId());
            showMessage(cancelledEvent.getTitle() + " was cancelled.");
            selectedAdminEvent = null;
            refreshAdminEvents();
            resetAdminForm();
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        }
    }

    private void refreshAdminEvents() {
        List<Event> activeEvents = service().getActiveEventsForAdmin();
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
        adminStatusText.setText(getString(R.string.admin_status_format, activeEvents.size()));

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

        for (int index = 1; index < adapter.getCount(); index++) {
            Event event = (Event) adapter.getItem(index);
            if (event != null && event.getId().equals(eventId)) {
                adminEventSpinner.setSelection(index);
                return;
            }
        }

        adminEventSpinner.setSelection(0);
    }

    private void populateAdminForm(Event event) {
        adminTitleInput.setText(event.getTitle());
        adminLocationInput.setText(event.getLocation());
        adminDateInput.setText(event.getDate().toString());
        adminTicketsInput.setText(String.valueOf(event.getAvailableTickets()));
        adminPriceInput.setText(String.valueOf(event.getPrice()));

        EventCategory[] categories = EventCategory.editableValues();
        for (int index = 0; index < categories.length; index++) {
            if (categories[index] == event.getCategory()) {
                adminCategorySpinner.setSelection(index);
                return;
            }
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
}
