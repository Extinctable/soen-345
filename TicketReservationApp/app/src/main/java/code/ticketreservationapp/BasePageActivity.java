package code.ticketreservationapp;

import android.content.Intent;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

import code.ticketreservationapp.service.TicketReservationService;

abstract class BasePageActivity extends AppCompatActivity {

    private View rootView;

    protected void initializePage(@IdRes int rootViewId) {
        rootView = findViewById(rootViewId);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    protected TicketReservationService service() {
        return TicketReservationServiceStore.getService();
    }

    protected void openPage(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    protected void replacePage(Class<?> activityClass) {
        openPage(activityClass);
        finish();
    }

    protected void returnToWelcomePage() {
        Intent intent = new Intent(this, WelcomePage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    protected void showMessage(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }
}
