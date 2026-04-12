package code.ticketreservationapp;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import code.ticketreservationapp.model.Event;
import code.ticketreservationapp.service.TicketReservationService;
import code.ticketreservationapp.ui.AdminPage;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AdminPageTest {

    static class TestableAdminPage extends AdminPage {
        String message = null;
        TicketReservationService mockService = mock(TicketReservationService.class);

        @Override
        protected TicketReservationService service() {
            return mockService;
        }

        @Override
        protected void showMessage(String message) {
            this.message = message;
        }

        int callParsePositiveInt(String value, String errorMessage) {
            return parsePositiveInt(value, errorMessage);
        }

        void callHandleUpdateEvent() {
            handleUpdateEvent();
        }

        String adminPrompt() {
            return ADMIN_PROMPT;
        }
    }

    private TestableAdminPage adminPage;
    private Event mockEvent;

    @Before
    public void setUp() {
        adminPage = Robolectric.buildActivity(TestableAdminPage.class).create().get();
        when(adminPage.mockService.getActiveEventsForAdmin()).thenReturn(new ArrayList<>());
        mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("1");
    }

    // All-DU-Paths Test Case 1 for method handleCancelEvent
    // Path Nodes: [ 1, 3, 4, 6 ]
    @Test
    @DisplayName("Valid selected event is cancelled and confirmation message shown")
    public void selectedEvent_cancelSucceeds_showsConfirmationMessage() {
        Event cancelledEvent = mock(Event.class);
        when(cancelledEvent.getTitle()).thenReturn("Test Event");

        adminPage.setSelectedAdminEvent(mockEvent);
        when(adminPage.mockService.cancelEvent("1")).thenReturn(cancelledEvent);
        adminPage.handleCancelEvent();

        assertEquals("Test Event was cancelled.", adminPage.message);
    }

    // Implicant Coverage Test Case 1 for method parsePositiveInt
    // A=T, B=T → true
    @Test
    @DisplayName("Valid positive integer parses successfully")
    public void validPositiveInt() {
        assertEquals(5, adminPage.callParsePositiveInt("5", "error"));
    }

    // Implicant Coverage Test Case 2 for method parsePositiveInt
    // A=T, B=F → false
    @Test
    @DisplayName("Non-positive integer throws exception")
    public void nonPositiveInt_throwsException() {
        try {
            adminPage.callParsePositiveInt("0", "error");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("error", e.getMessage());
        }
    }

    // Implicant Coverage Test Case 3 for method parsePositiveInt
    // A=F → false
    @Test
    @DisplayName("Non-numeric string throws exception")
    public void nonNumericString_throwsException() {
        try {
            adminPage.callParsePositiveInt("abc", "error");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("error", e.getMessage());
        }
    }

    // Prime Path Coverage Test Case 1 for method handleUpdateEvent
    // Prime Path: [ 1, 2 ]
    @Test
    @DisplayName("No event selected shows prompt message")
    public void noEventSelected_showsPromptMessage() {
        adminPage.setSelectedAdminEvent(null);
        adminPage.callHandleUpdateEvent();
        assertEquals(adminPage.adminPrompt(), adminPage.message);
    }

    // Prime Path Coverage Test Case 2 for method handleUpdateEvent
    // Prime Path: [ 1, 3, 4, 7 ]
    @Test
    @DisplayName("Valid event update shows confirmation message")
    public void validEvent_updateSucceeds_showsConfirmationMessage() {
        Event updatedEvent = mock(Event.class);
        when(updatedEvent.getTitle()).thenReturn("Test Event");
        when(updatedEvent.getId()).thenReturn("1");
        when(adminPage.mockService.updateEvent(
                any(), any(), any(), any(), any(), anyInt(),
                anyDouble())).thenReturn(updatedEvent
        );

        adminPage.adminDateInput.setText("2023-01-01");
        adminPage.adminTicketsInput.setText("10");
        adminPage.adminPriceInput.setText("9.99");
        adminPage.setSelectedAdminEvent(mockEvent);
        adminPage.callHandleUpdateEvent();

        assertEquals("Test Event updated.", adminPage.message);
    }

    // Prime Path Coverage Test Case 3 for method handleUpdateEvent
    // Prime Path: [ 1, 3, 5, 6, 7 ]
    @Test
    @DisplayName("Update throws exception shows error message")
    public void updateThrowsException_showsErrorMessage() {
        when(adminPage.mockService.updateEvent(
                any(), any(), any(), any(), any(), anyInt(),
                anyDouble())
        ).thenThrow(new IllegalArgumentException("Update failed."));

        adminPage.adminDateInput.setText("2024-09-26");
        adminPage.adminTicketsInput.setText("10");
        adminPage.adminPriceInput.setText("9.99");
        adminPage.setSelectedAdminEvent(mockEvent);
        adminPage.callHandleUpdateEvent();

        assertEquals("Update failed.", adminPage.message);
    }

}