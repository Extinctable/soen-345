package code.ticketreservationapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import code.ticketreservationapp.R;
import code.ticketreservationapp.auth.AuthSessionStore;
import code.ticketreservationapp.auth.LoginRole;
import code.ticketreservationapp.model.ContactMethod;

public class WelcomePage extends BasePageActivity {

    private View customerRegistrationToggle;
    private ImageView customerRegistrationArrow;
    private View customerRegistrationContainer;
    private TextInputEditText customerLoginInput;
    private TextInputEditText customerLoginPasswordInput;
    private TextInputEditText customerEmailInput;
    private TextInputEditText customerPhoneInput;
    private TextInputEditText customerRegisterPasswordInput;
    private TextInputEditText adminUsernameInput;
    private TextInputEditText adminPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page);

        AuthSessionStore.ensureDefaultAdminExists(this);
        initializePage(R.id.main);

        customerRegistrationToggle = findViewById(R.id.customerRegistrationToggle);
        customerRegistrationArrow = findViewById(R.id.customerRegistrationArrow);
        customerRegistrationContainer = findViewById(R.id.customerRegistrationContainer);
        customerLoginInput = findViewById(R.id.customerLoginInput);
        customerLoginPasswordInput = findViewById(R.id.customerLoginPasswordInput);
        customerEmailInput = findViewById(R.id.customerEmailInput);
        customerPhoneInput = findViewById(R.id.customerPhoneInput);
        customerRegisterPasswordInput = findViewById(R.id.customerRegisterPasswordInput);
        adminUsernameInput = findViewById(R.id.adminUsernameInput);
        adminPasswordInput = findViewById(R.id.adminPasswordInput);

        setupCustomerRegistrationToggle();
        findViewById(R.id.customerLoginButton).setOnClickListener(view -> handleCustomerLogin());
        findViewById(R.id.customerRegisterButton).setOnClickListener(view -> handleCustomerRegister());
        findViewById(R.id.adminLoginButton).setOnClickListener(view -> handleAdminLogin());
    }

    @Override
    protected void onStart() {
        super.onStart();
        routeIfSessionExists();
    }

    private void handleCustomerLogin() {
        String identifier = valueOf(customerLoginInput);
        String password = valueOf(customerLoginPasswordInput);
        AuthSessionStore.loginCustomer(identifier, password, (success, message) -> {
            runOnUiThread(() -> {
                if (!success) {
                    showMessage(message);
                    return;
                }
                try {
                    registerLoggedInCustomerProfile();
                } catch (IllegalArgumentException ex) {
                    AuthSessionStore.logout();
                    showMessage(ex.getMessage());
                    return;
                }
                openPageForRole(LoginRole.USER);
            });
        });
    }

    private void handleCustomerRegister() {
        String email = valueOf(customerEmailInput);
        String phone = valueOf(customerPhoneInput);
        String password = valueOf(customerRegisterPasswordInput);
        AuthSessionStore.registerCustomer(email, phone, password, (success, message) -> {
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
                if (!success) {
                    return;
                }
                customerEmailInput.setText("");
                customerPhoneInput.setText("");
                customerRegisterPasswordInput.setText("");
                setCustomerRegistrationExpanded(false);
            });
        });
    }

    private void handleAdminLogin() {
        String username = valueOf(adminUsernameInput);
        String password = valueOf(adminPasswordInput);
        AuthSessionStore.loginAdmin(username, password, (success, message) -> {
            runOnUiThread(() -> {
                if (!success) {
                    showMessage(message);
                    return;
                }
                openPageForRole(LoginRole.ADMIN);
            });
        });
    }

    private void registerLoggedInCustomerProfile() {
        String customerEmail = AuthSessionStore.currentEmail();
        if (customerEmail == null || customerEmail.isEmpty()) {
            throw new IllegalArgumentException("Customer accounts must have a valid email address.");
        }
        String customerName = displayNameFromEmail(customerEmail);
        service().registerUser(customerName, customerEmail, ContactMethod.EMAIL);
    }

    private void routeIfSessionExists() {
        if (!AuthSessionStore.isLoggedIn()) {
            return;
        }

        openPageForRole(AuthSessionStore.currentRole());
    }

    private void openPageForRole(LoginRole role) {
        if (role == LoginRole.ADMIN) {
            replacePage(AdminPage.class);
            return;
        }

        replacePage(UserPage.class);
    }

    private void setupCustomerRegistrationToggle() {
        customerRegistrationToggle.setOnClickListener(view ->
                setCustomerRegistrationExpanded(customerRegistrationContainer.getVisibility() != View.VISIBLE));
        setCustomerRegistrationExpanded(false);
    }

    private void setCustomerRegistrationExpanded(boolean expanded) {
        customerRegistrationContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
        customerRegistrationArrow.setImageResource(
                expanded ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float
        );
    }

    private String displayNameFromEmail(String email) {
        String localPart = email.split("@", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (localPart.isEmpty()) {
            return "Customer";
        }

        String cleaned = localPart.replace('.', ' ').replace('_', ' ').replace('-', ' ');
        StringBuilder displayName = new StringBuilder();
        for (String word : cleaned.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (displayName.length() > 0) {
                displayName.append(' ');
            }
            displayName.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                displayName.append(word.substring(1));
            }
        }

        return displayName.length() == 0 ? "Customer" : displayName.toString();
    }
}
