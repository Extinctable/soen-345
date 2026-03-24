package code.ticketreservationapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WelcomePageInstrumentedTest {

    @Test
    public void dashboardLoadsKeySections() {
        try (ActivityScenario<WelcomePage> ignored = ActivityScenario.launch(WelcomePage.class)) {
            onView(withId(R.id.appTitle)).check(matches(withText("Cloud Ticket Reservation")));
            onView(withId(R.id.registerButton)).check(matches(isDisplayed()));
            onView(withId(R.id.eventsContainer)).check(matches(isDisplayed()));
            onView(withId(R.id.adminAddButton)).check(matches(isDisplayed()));
        }
    }
}
