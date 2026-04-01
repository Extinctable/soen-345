package code.ticketreservationapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

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
    public void welcomePageShowsLoginControls() {
        try (ActivityScenario<WelcomePage> ignored = ActivityScenario.launch(WelcomePage.class)) {
            onView(withId(R.id.appTitle)).check(matches(withText("Cloud Ticket Reservation")));
            onView(withId(R.id.usernameInput)).check(matches(isDisplayed()));
            onView(withId(R.id.passwordInput)).check(matches(isDisplayed()));
            onView(withId(R.id.loginRoleSpinner)).check(matches(isDisplayed()));
            onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void userLoginOpensUserPortal() {
        try (ActivityScenario<WelcomePage> ignored = ActivityScenario.launch(WelcomePage.class)) {
            onView(withId(R.id.usernameInput)).perform(typeText("user"), closeSoftKeyboard());
            onView(withId(R.id.passwordInput)).perform(typeText("user123"), closeSoftKeyboard());
            onView(withId(R.id.loginButton)).perform(click());
            onView(withId(R.id.currentUserText)).check(matches(isDisplayed()));
            onView(withId(R.id.eventsContainer)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminLoginOpensAdminConsole() {
        try (ActivityScenario<WelcomePage> ignored = ActivityScenario.launch(WelcomePage.class)) {
            onView(withId(R.id.loginRoleSpinner)).perform(click());
            onData(allOf(is(instanceOf(LoginRole.class)), is(LoginRole.ADMIN))).perform(click());
            onView(withId(R.id.usernameInput)).perform(typeText("admin"), closeSoftKeyboard());
            onView(withId(R.id.passwordInput)).perform(typeText("admin123"), closeSoftKeyboard());
            onView(withId(R.id.loginButton)).perform(click());

            onView(withId(R.id.adminAddButton)).check(matches(isDisplayed()));
            onView(withId(R.id.adminEventSpinner)).check(matches(isDisplayed()));
            onView(withId(R.id.switchToUserButton)).check(matches(isDisplayed()));
        }
    }
}
