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

// Build 200 Use androidX fragment class
//import android.app.FragmentManager;
//import android.app.FragmentTransaction;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.UUID;

import solutions.cris.R;
import solutions.cris.edit.EditSession;
import solutions.cris.object.Group;
import solutions.cris.object.ListItem;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;
import solutions.cris.utils.PickList;
import solutions.cris.utils.PickListDialogFragment;

public class ListSessions extends ListActivity implements PickListDialogFragment.PickListDialogListener {

    // Build 200 Move selectedIDs to here (from ListClientsFragment) and set following
    // positive button click in PickListDialogFragment
    private ArrayList<UUID> selectedIDs;

    public ArrayList<UUID> getSelectedIDs() {
        return selectedIDs;
    }

    public void setSelectedIDs(ArrayList<UUID> selectedIDs) {
        this.selectedIDs = selectedIDs;
    }

    private String selectedValues = "";

    public String getSelectedValues() {
        return selectedValues;
    }

    public void clearSelectedValues() {
        selectedValues = "";
    }

    public void addToSelectedValues(String selectedValue) {
        if (selectedValue.length() > 0) {
            if (this.selectedValues.length() > 0) {
                this.selectedValues += ", ";
            }
            this.selectedValues += selectedValue;
        }
    }

    // The picklist dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
        setSelectMode(((PickListDialogFragment) dialog).getSelectMode());
        // Clear and the load the selectedIDs array
        CheckBox checkBoxes[] = ((PickListDialogFragment) dialog).getCheckBoxes();
        PickList pickList = ((PickListDialogFragment) dialog).getPickList();
        selectedIDs.clear();
        for (int i = 0; i < checkBoxes.length; i++) {
            if (checkBoxes[i].isChecked()) {
                // Keyworkers are a special case, list of users, not ListItems
                if (getSelectMode() == SelectMode.SESSION_COORDINATORS) {
                    User sessionCoordinator = ((User) pickList.getObjects().get(i));
                    selectedIDs.add(sessionCoordinator.getUserID());
                    addToSelectedValues(sessionCoordinator.getFullName());
                } else if (getSelectMode() == SelectMode.KEYWORKERS) {
                    User keyWorker = ((User) pickList.getObjects().get(i));
                    selectedIDs.add(keyWorker.getUserID());
                    addToSelectedValues(keyWorker.getFullName());
                } else {
                    ListItem listItem = pickList.getListItems().get(i);
                    selectedIDs.add(listItem.getListItemID());
                    addToSelectedValues(listItem.getItemValue());
                }
            }
        }
        // Call the pickListDialogFragmentOK() method of the visible fragment (the one that
        // has just displayed the pickListDialogFragment

        Fragment listSessionsFragment = getSupportFragmentManager().findFragmentByTag("ListSessionsFragment");
        if (listSessionsFragment != null && listSessionsFragment.isVisible()) {
            ((ListSessionsFragment) listSessionsFragment).pickListDialogFragmentOK();
        } else {
            Fragment editSessionFragment = getSupportFragmentManager().findFragmentByTag("EditSessionFragment");
            if (editSessionFragment != null && editSessionFragment.isVisible()) {
                ((EditSession) editSessionFragment).pickListDialogFragmentOK();
            }
        }
    }

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
                //FragmentManager fragmentManager = getFragmentManager();
                //FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                //ListSessionsFragment fragment = new ListSessionsFragment();
                //fragmentTransaction.add(R.id.content, fragment);
                //fragmentTransaction.commit();
                // Build 200 Use androidX fragment class
                Fragment fragment = new ListSessionsFragment();
                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.content, fragment, "ListSessionsFragment")
                        .commit();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // Build 126 - Add share option so that Read fragments can offer a share
        //inflater.inflate(R.menu.menu_search, menu);
        inflater.inflate(R.menu.menu_search_share, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }


}
