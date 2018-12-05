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
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
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
import solutions.cris.object.CrisObject;
import solutions.cris.object.User;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.object.SystemError;
import solutions.cris.utils.ExceptionHandler;

public class ListErrors extends CRISActivity {

    private ListView listView;
    public static List<SystemError> errors;
    public static List<SystemError> selectedErrors;

    private enum SelectMode {ALL, NOSYNC}

    private SelectMode selectMode = SelectMode.NOSYNC;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CurrentUser always exists so if this check fails then exception in child
        // // has rendered system inconsistent so exit and let Main start from scratch()
        User currentUser = User.getCurrentUser();
        if (currentUser == null) {
            finish();
        } else {
            // Add the global uncaught exception handler
            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
            setContentView(R.layout.activity_list);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            this.listView = (ListView) findViewById(R.id.list_view);

            this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    doReadError(position);
                }
            });

            LocalDB localDB = LocalDB.getInstance();
            // Load the documents for special client (ClientID=DOCUMENT) from the database
            errors = localDB.getAllSystemErrors(200);
            loadAdapter();
            // Sort the list by date
            //Collections.sort(errors, SystemError.dateComparator);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAdapter();

    }

    private void loadAdapter() {
        selectedErrors = new ArrayList<>();
        switch (selectMode) {
            case ALL:
                for (SystemError error : errors) {
                    selectedErrors.add(error);
                }
                break;
            case NOSYNC:
                for (SystemError error : errors) {
                    if (!error.getExceptionMessage().startsWith("Error in postJson") &&
                            !error.getExceptionMessage().startsWith("No database found") &&
                            !error.getExceptionMessage().startsWith("Unable to start receiver") &&
                            !error.getExceptionMessage().startsWith("java.net.SocketTimeout")) {
                        selectedErrors.add(error);

                    }
                }
        }
        ErrorAdapter adapter = new ErrorAdapter(this, selectedErrors);
        this.listView.setAdapter(adapter);
    }

    // MENU BLOCK
    private static final int MENU_SORT_AZ = Menu.FIRST + 1;
    private static final int MENU_SORT_DATE = Menu.FIRST + 2;
    private static final int MENU_SELECT_ALL = Menu.FIRST + 3;
    private static final int MENU_SELECT_NOSYNC = Menu.FIRST + 4;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_empty, menu);
        super.onCreateOptionsMenu(menu);
        // SORT A-Z
        MenuItem sortAZOption = menu.add(0, MENU_SORT_AZ, Menu.NONE, R.string.action_sort_az);
        sortAZOption.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_action_sort_az));
        sortAZOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        // SORT DATE
        MenuItem sortDateOption = menu.add(0, MENU_SORT_DATE, Menu.NONE, R.string.action_sort_date);
        sortDateOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // SELECT
        MenuItem selectAllOption = menu.add(0, MENU_SELECT_ALL, 4, "Show All Errors");
        selectAllOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem selectOpenOption = menu.add(0, MENU_SELECT_NOSYNC, 5, "Hide Sync Errors");
        selectOpenOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case MENU_SORT_AZ:
                Collections.sort(selectedErrors, SystemError.comparatorAZ);
                ((ErrorAdapter) listView.getAdapter()).notifyDataSetChanged();
                return true;

            case MENU_SORT_DATE:
                Collections.sort(selectedErrors, CrisObject.comparatorCreationDate);
                ((ErrorAdapter) listView.getAdapter()).notifyDataSetChanged();
                return true;

            case MENU_SELECT_ALL:
                selectMode = SelectMode.ALL;
                loadAdapter();
                return true;

            case MENU_SELECT_NOSYNC:
                selectMode = SelectMode.NOSYNC;
                loadAdapter();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void doReadError(int position) {
        SystemError error = selectedErrors.get(position);
        Intent intent = new Intent(this, AlertAndContinue.class);
        intent.putExtra("title", "System Error");
        intent.putExtra("message", error.getTextSummary());
        startActivity(intent);
    }

    private class ErrorAdapter extends ArrayAdapter<SystemError> {

        // Constructor
        ErrorAdapter(Context context, List<SystemError> objects) {
            super(context, 0, objects);
        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_list_item, parent, false);
            }

            ImageView viewItemIcon = (ImageView) convertView.findViewById(R.id.item_icon);
            TextView viewItemDate = (TextView) convertView.findViewById(R.id.item_date);
            TextView viewItemMainText = (TextView) convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = (TextView) convertView.findViewById(R.id.item_additional_text);

            final SystemError error = selectedErrors.get(position);

            viewItemIcon.setImageDrawable(getDrawable(R.drawable.ic_system_error));
            SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.UK);
            viewItemDate.setText(sDate.format(error.getCreationDate()));
            viewItemMainText.setText(error.getUserName());
            viewItemAdditionalText.setText(error.getExceptionMessage());
            return convertView;
        }
    }

}
