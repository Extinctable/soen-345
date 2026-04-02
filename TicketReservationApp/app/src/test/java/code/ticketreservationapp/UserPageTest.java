package code.ticketreservationapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


import org.junit.Test;
import org.junit.Before;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.LocalDate;

import code.ticketreservationapp.model.EventFilter;
import code.ticketreservationapp.model.Reservation;
import code.ticketreservationapp.service.TicketReservationService;
import code.ticketreservationapp.ui.UserPage;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class UserPageTest {
    static class TestableUserPage extends UserPage {
        String mockEmail = "";
        String message  = null;

        String resolveConfirmationEmail(Reservation r) {
            return mockEmail;
        }
        protected void showMessage(String message) {
            this.message = message;
        }

        boolean callIsValidEmail(String value) {
            return isValidEmail(value);
        }

        EventFilter callCreateFilterFromInputs() {
            return createFilterFromInputs();
        }

    }
    private TicketReservationService service;
    private TestableUserPage userPage;
    private Reservation mockReservation;

    @Before
    public void setUp() {
        service = new TicketReservationService();
        userPage = Robolectric.buildActivity(TestableUserPage.class).create().get();
        mockReservation = mock(Reservation.class);
    }

    //All-DU-Paths Test Case 1 for method sendConfirmationEmail
    //Path Nodes: [ 1, 2, 3 ]
    @Test
    @DisplayName("Invalid email without sent confirmation")
    public void emptyEmail_ShowErrorMessage() {
        userPage.mockEmail = "";
        userPage.sendEmailConfirmation(mockReservation);
        assertEquals("No valid email is attached to this customer account.", userPage.message);
    }

    //All-DU-Paths Test Case 1 for method sendConfirmationEmail
    //Path Nodes: [ 1, 2, 4, 5 ]
    @Test
    @DisplayName("Valid email with sent confirmation")
    public void validEmail_proceedsToSend() {
        userPage.mockEmail = "user@example.com";
        userPage.sendEmailConfirmation(mock(Reservation.class));
        assertNotNull(userPage.message);
    }

    // Implicant Coverage Test Case 1 for method isValidEmail
    // A=T, B=T → true
    @Test
    @DisplayName("Valid email format")
    public void validEmail() {
        assertTrue(userPage.callIsValidEmail("user@example.com"));
    }

    // Implicant Coverage Test Case 2 for method isValidEmail
    // A=T, B=F → false
    @Test
    @DisplayName("Invalid string format for email")
    public void invalidEmailFormat() {
        assertFalse(userPage.callIsValidEmail("notanemail"));
    }

    // Implicant Coverage Test Case 3 for method isValidEmail
    // A=F → false since B will not even be checked
    @Test
    @DisplayName("Empty string for email is invalid")
    public void emptyEmailString() {
        assertFalse(userPage.callIsValidEmail(""));
    }

    //Prime Path Coverage Test Case 1 for method createFilterFromInputs
    //Prime Path: [ 1, 2, 3, 7 ]
    @Test
    @DisplayName("Empty Input for Date Filter")
    public void emptyDate_skipsParseAndDateIsNull() {
        // leave all inputs empty (default state after create)
        userPage.filterDateInput.setText("");
        userPage.searchInput.setText("");
        userPage.filterLocationInput.setText("");

        EventFilter result = userPage.callCreateFilterFromInputs();

        assertNotNull(result);
        assertNull(result.getDate());
        assertNull(userPage.message); // no error shown
    }

    //Prime Path Coverage Test Case 2 for method createFilterFromInputs
    //Prime Path: [ 1, 2, 3, 4, 7 ]
    @Test
    @DisplayName("Valid Date Format")
    public void validDate_parsesSuccessfullyAndDateAssigned() {
        userPage.filterDateInput.setText("2025-06-15");
        userPage.searchInput.setText("");
        userPage.filterLocationInput.setText("");

        EventFilter result = userPage.callCreateFilterFromInputs();

        assertNotNull(result);
        assertEquals(LocalDate.of(2025, 6, 15), result.getDate());
        assertNull(userPage.message); // no error shown
    }

    //Prime Path Coverage Test Case 3 for method createFilterFromInputs
    //Prime Path: [ 1, 2, 3, 4, 5, 6, 7 ]
    @Test
    @DisplayName("Invalid Date Format Exception")
    public void invalidDate_showsErrorClearsInputAndDateIsNull() {
        userPage.filterDateInput.setText("bad-date");
        userPage.searchInput.setText("");
        userPage.filterLocationInput.setText("");

        EventFilter result = userPage.callCreateFilterFromInputs();

        assertNotNull(result);
        assertNull(result.getDate());
        assertEquals(
                "Date filters must use YYYY-MM-DD. Showing all dates instead.",
                userPage.message
        );
        assertEquals("", userPage.filterDateInput.getText().toString());
    }
}
