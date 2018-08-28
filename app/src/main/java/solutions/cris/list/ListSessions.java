package solutions.cris.list;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import solutions.cris.Login;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.object.Document;
import solutions.cris.object.Session;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;

public class ListSessions extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CurrentUser always exists so if this check fails then exception in child
        // // has rendered system inconsistent so exit and let Main start from scratch()
        setCurrentUser(User.getCurrentUser());
        if (getCurrentUser() == null) {
            finish();
        } else {
            // Add the global uncaught exception handler
            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
            setContentView(R.layout.activity_list_sessions);
            setToolbar((Toolbar) findViewById(R.id.toolbar));
            getToolbar().setTitle(getString(R.string.app_name));
            setSupportActionBar(getToolbar());

            setFab((FloatingActionButton) findViewById(R.id.fab));

            // Only display the fragment when not reloading destroyed instance
            if (savedInstanceState == null) {
                // Start the List Sessions fragment
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                ListSessionsFragment fragment = new ListSessionsFragment();
                fragmentTransaction.add(R.id.content, fragment);
                fragmentTransaction.commit();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

}