package code.ticketreservationapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WelcomePageInstrumentedTest {

    @Before
    public void resetSharedState() {
        TicketReservationServiceStore.reset();
    }

    @Test
    public void welcomePageShowsRoleNavigation() {
        try (ActivityScenario<WelcomePage> ignored = ActivityScenario.launch(WelcomePage.class)) {
            onView(withId(R.id.appTitle)).check(matches(withText("Cloud Ticket Reservation")));
            onView(withId(R.id.openUserPageButton)).check(matches(isDisplayed()));
            onView(withId(R.id.openAdminPageButton)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void welcomePageOpensUserPortal() {
        try (ActivityScenario<WelcomePage> ignored = ActivityScenario.launch(WelcomePage.class)) {
            onView(withId(R.id.openUserPageButton)).perform(click());
            onView(withId(R.id.registerButton)).check(matches(isDisplayed()));
            onView(withId(R.id.eventsContainer)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminPageLoadsConsole() {
        try (ActivityScenario<AdminPage> ignored = ActivityScenario.launch(AdminPage.class)) {
            onView(withId(R.id.adminAddButton)).check(matches(isDisplayed()));
            onView(withId(R.id.adminEventSpinner)).check(matches(isDisplayed()));
            onView(withId(R.id.switchToUserButton)).check(matches(isDisplayed()));
        }
    }
}
