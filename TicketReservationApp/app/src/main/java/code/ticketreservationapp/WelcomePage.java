package code.ticketreservationapp;

import android.os.Bundle;

public class WelcomePage extends BasePageActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page);

        initializePage(R.id.main);

        findViewById(R.id.openUserPageButton).setOnClickListener(view -> openPage(UserPage.class));
        findViewById(R.id.openAdminPageButton).setOnClickListener(view -> openPage(AdminPage.class));
    }
}
