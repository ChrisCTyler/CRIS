package solutions.cris.list;
//        CRIS - Client Record Information System
//        Copyright (C) 2018  Chris Tyler, CRIS.Solutions
//
//        This program is free software: you can redistribute it and/or modify
//        it under the terms of the GNU General Public License as published by
//        the Free Software Foundation, either version 3 of the License, or
//        (at your option) any later version.
//
//        This program is distributed in the hope that it will be useful,
//        but WITHOUT ANY WARRANTY; without even the implied warranty of
//        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//        GNU General Public License for more details.
//
//        You should have received a copy of the GNU General Public License
//        along with this program.  If not, see <https://www.gnu.org/licenses/>.
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import solutions.cris.CRISActivity;
import solutions.cris.Login;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.ChangePassword;
import solutions.cris.edit.EditUser;
import solutions.cris.object.Role;
import solutions.cris.object.User;
import solutions.cris.read.ReadUser;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.ExceptionHandler;

public class ListUsers extends CRISActivity {

    public static final String INCLUDE_PREVIOUS = "solutions.cris.IncludePrevious";
    public static final String CURRENT_POSITION = "solutions.cris.CurrentPosition";

    public static ArrayList<User> users;
    public static ArrayList<User> adapterList;
    public static int swipeValue;

    private ListView listView;
    private LocalDB localDB;
    private User currentUser;
    private boolean includePrevious = false;
    private int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CurrentUser always exists so if this check fails then exception in child
        // // has rendered system inconsistent so exit and let Main start from scratch()
        currentUser = User.getCurrentUser();
        if (currentUser == null) {
            finish();
        } else {
            // Add the global uncaught exception handler
            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
            setContentView(R.layout.activity_list_with_action);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // Check whether we're recreating a previously destroyed instance
            if (savedInstanceState != null) {
                // Restore value of members from saved state
                includePrevious = savedInstanceState.getBoolean(INCLUDE_PREVIOUS);
                currentPosition = savedInstanceState.getInt(CURRENT_POSITION);
            }

            this.listView = (ListView) findViewById(R.id.list_view);
            this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    doReadUser(position);
                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    return doEditUser(position);
                }
            });

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_SYSTEM_ADMINISTRATOR)) {
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        doNewUser();
                    }
                });
            } else {
                // Hide New Client option
                fab.setVisibility(View.GONE);
            }

            localDB = LocalDB.getInstance();
            // Load the users from the database
            users = localDB.getAllUsers();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Handle the swipe value potentially set by the child read activity
        if (swipeValue != 0) {
            int newPosition = currentPosition + swipeValue;
            swipeValue = 0;
            if (newPosition >= 0 && newPosition < adapterList.size()) {
                doReadUser(newPosition);
            }
        } else {
            // Sort the list by name
            Collections.sort(users, User.comparator);
            // Create the adapter
            adapterList = new ArrayList<>();
            UserAdapter adapter = new UserAdapter(this, adapterList);
            for (User user : users) {
                if (includePrevious || user.getEndDate().getTime() == Long.MIN_VALUE) {
                    adapterList.add(user);
                }
            }
            this.listView.setAdapter(adapter);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list_users, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_show_previous_users:
                includePrevious = true;
                onResume();
                return true;

            case R.id.action_change_password:
                Intent intent = new Intent(this, ChangePassword.class);
                intent.putExtra(Main.EXTRA_PASSWORD_EXPIRED, false);
                startActivity(intent);
                return true;

            case R.id.action_change_user:
                doChangeUser();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the state
        savedInstanceState.putBoolean(INCLUDE_PREVIOUS, includePrevious);
        savedInstanceState.putInt(CURRENT_POSITION, currentPosition);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private void doChangeUser() {
        final Context context = this;
        new AlertDialog.Builder(this)
                .setTitle("Change Device Owner")
                .setMessage("The CRIS application uses two-factor authentication, each device " +
                        "may only be used by a single user. " +
                        "Selecting this option will reset this device so " +
                        "that it may be used by a different user.\n\n" +
                        "ANY DATA NOT SYNCED MAY BE LOST.\n\n" +
                        "It is therefore very important that you manually sync the device " +
                        "before selecting this option.\n\n" +
                        "Once this option has been selected it will be necessary to " +
                        "login using the a email address/password and the device may " +
                        "then only be used by the new user.\n\n")
                .setPositiveButton("Change Device Owner", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String dbName = localDB.getDatabaseName();
                        CRISUtil.invalidateOrg(context, dbName);
                        User.setCurrentUser(null);
                        finish();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }

                })
                .show();
    }

    private void doReadUser(int position) {
        //if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_VIEW_USER_RECORD)) {
            // Save the current position in case the user 'swipes' in the read activity
            currentPosition = position;
            Intent intent = new Intent(this, ReadUser.class);
            intent.putExtra(Main.EXTRA_IS_NEW_MODE, false);
            intent.putExtra(Main.EXTRA_IS_FIRST_USE, false);
            intent.putExtra(Main.EXTRA_LIST_POSITION, position);
            startActivity(intent);
        //}
    }

    private boolean doEditUser(int position) {
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_SYSTEM_ADMINISTRATOR)) {
            Intent intent = new Intent(this, EditUser.class);
            intent.putExtra(Main.EXTRA_IS_NEW_MODE, false);
            intent.putExtra(Main.EXTRA_IS_FIRST_USE, false);
            intent.putExtra(Main.EXTRA_LIST_POSITION, position);
            intent.putExtra(Main.EXTRA_ORGANISATION, "");
            startActivity(intent);
        }
        return true;
    }

    private void doNewUser() {
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_SYSTEM_ADMINISTRATOR)) {
            User newUser = new User(currentUser);
            Intent intent = new Intent(this, EditUser.class);
            // User is serializable so can pass as extra to EditUser Activity
            intent.putExtra(Main.EXTRA_IS_NEW_MODE, true);
            intent.putExtra(Main.EXTRA_IS_FIRST_USE, false);
            intent.putExtra(Main.EXTRA_DOCUMENT, newUser);
            startActivity(intent);
        }
    }

    private class UserAdapter extends ArrayAdapter<User> {

        // Constructor
        UserAdapter(Context context, List<User> objects) {
            super(context, 0, objects);
        }

        @Override
        public @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_list_item, parent, false);
            }

            ImageView viewItemIcon = (ImageView) convertView.findViewById(R.id.item_icon);
            TextView viewItemDate = (TextView) convertView.findViewById(R.id.item_date);
            TextView viewItemMainText = (TextView) convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = (TextView) convertView.findViewById(R.id.item_additional_text);

            final User user = adapterList.get(position);

            SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
            if (user.getEndDate().getTime() != Long.MIN_VALUE) {
                viewItemDate.setText(String.format("End Date: %s", sDate.format(user.getEndDate())));
                viewItemIcon.setImageDrawable(getDrawable(R.drawable.ic_user_grey));
            } else {
                viewItemIcon.setImageDrawable(getDrawable(R.drawable.ic_user));
            }
            viewItemMainText.setText(String.format("%s, %s",
                    user.getFullName(),
                    user.getRole().getItemValue()
                    ));
            viewItemAdditionalText.setText(String.format("%s, %s",
                    user.getContactNumber(),
                    user.getEmailAddress()));

            return convertView;
        }
    }

}
