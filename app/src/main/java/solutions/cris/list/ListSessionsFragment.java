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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
// Build 200 Use the androidX Fragment class
//import android.app.Fragment;
//import android.app.FragmentManager;
//import android.app.FragmentTransaction;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.EditSession;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Client;
import solutions.cris.object.Document;
import solutions.cris.object.Group;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.Role;
import solutions.cris.object.Session;
import solutions.cris.object.User;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.utils.CRISExport;
import solutions.cris.utils.LocalSettings;
import solutions.cris.utils.PickList;
import solutions.cris.utils.PickListDialogFragment;
import solutions.cris.utils.SwipeDetector;


public class ListSessionsFragment extends Fragment {

    private ListView listView;
    private TextView footer;
    private View parent;
    private LocalDB localDB;
    private User currentUser;
    private SortMode sortMode = SortMode.DATE;
    private SearchView sv;
    private String searchText = "";
    private boolean isSearchIconified = true;
    // Build 200 Moved to ListSessions
    //private UUID selectedID = null;
    //private String selectedValue;
    private Date today;
    ArrayList<Session> sessionList;
    private SessionAdapter adapter;
    private String footerText;
    private Parcelable listViewState;
    private UUID oldClientRecordID;


    private enum SortMode {DATE, NAME}

    private ArrayList<Group> displayedGroups;
    private ArrayList<User> displayedSessionCoordinators;

    // Build 181 - Load Adapter on background thread due to large number of sessions
    private Date startTime;

    // Build 200 - This is called by the event listener in ListClients as a result of a OK
    // in the PickListDialogFragment
    public void pickListDialogFragmentOK() {
        new LoadAdapter().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.layout_list, container, false);
        footer = getActivity().findViewById(R.id.footer);
        // Build 200 Initialise selectMode to UNCANCELLED
        ((ListSessions) getActivity()).setSelectedIDs(new ArrayList<>());
        ((ListSessions) getActivity()).clearSelectedValues();
        ((ListActivity) getActivity()).setSelectMode(((ListActivity.SelectMode.FUTURE)));
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Today needs to be midnight
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        // Revert to midnight
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        today = now.getTime();

        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        toolbar.setTitle(getString(R.string.app_name) + " - My Sessions");

        localDB = LocalDB.getInstance();
        currentUser = User.getCurrentUser();

        // Initialise the list view
        this.listView = parent.findViewById(R.id.list_view);
        final SwipeDetector swipeDetector = new SwipeDetector();
        this.listView.setOnTouchListener(swipeDetector);
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (swipeDetector.swipeDetected()) {
                    displayDocumentHistory(position, swipeDetector.getAction());
                } else {
                    doReadSession(position);
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return doEditSession(position);
            }
        });


        // Swipe Left and Right
        //LinearLayout mainLayout = (LinearLayout) getActivity().findViewById(R.id.main_layout);


        User currentUser = User.getCurrentUser();
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_CREATE_SESSIONS) ||
                currentUser.getRole().hasPrivilege(Role.PRIVILEGE_EDIT_ALL_SESSIONS)) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doNewSession();
                }
            });
        } else {
            fab.setVisibility(View.GONE);
        }

        // Load the Adapter, on first run through
        if (adapter == null) {
            footerText = "";
            // Set a new empty array list
            ((ListActivity) getActivity()).setSessionAdapterList(new ArrayList<Session>());
            adapter = new SessionAdapter(getActivity(), ((ListActivity) getActivity()).getSessionAdapterList());
            new LoadAdapter().execute();
        }

        // Build 181 - Load the adapter in the background
        new LoadAdapter().execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Link the list view if first time or detached
        // Return from Cancel needs listView to be re-attached though adapter is re-used
        if (listView.getAdapter() == null) {
            this.listView.setAdapter(adapter);
        }
        if (listViewState != null) {
            listView.onRestoreInstanceState(listViewState);
        }
        if (footer.getText().toString().isEmpty()) {
            footer.setText(footerText);
        }
        // Only need to re-load if a session has been update/created
        Session editSession = ((ListActivity) getActivity()).getSession();
        if (editSession != null) {
            // Get the session record from the database
            Session updatedSession = (Session) localDB.getDocument(editSession.getDocumentID());
            if (((ListActivity) getActivity()).getMode() == Document.Mode.NEW) {
                if (updatedSession != null) {     // New client was saved
                    // Build 185 - Odd bug causing crash (sometimes) when the new session is
                    // added to the existing session list so, since the load time has been
                    // optimised and is practically negligible, reload from the database instead
                    // by nulling the session list
                    // Add to the client list
                    //sessionList.add(updatedSession);
                    sessionList = null;
                    // Clear the mode so that the session does not get added twice following subsequent read
                    ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                    // Load the Adapter since display of new session needs to be checked.
                    // Build 181 - Load adapter in the background
                    new LoadAdapter().execute();
                }
            } else if (!updatedSession.getRecordID().equals(oldClientRecordID)) {
                // Database record is different so update the client in the client list
                // Note: this may be direct edit, or indirect edit via ListClientHeader
                for (Session session : sessionList) {
                    if (session.getDocumentID().equals(editSession.getDocumentID())) {
                        int index = sessionList.indexOf(session);
                        sessionList.set(index, updatedSession);
                        break;
                    }
                }
                // Load the Adapter since display of updated client needs to be checked.
                // Build 181 - Load adapter in the background
                new LoadAdapter().execute();
            }
        }

    }

    private boolean mySession(Session session) {
        boolean selected = false;
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_EDIT_ALL_SESSIONS)) {
            selected = true;
        } else if (session.getKeyWorkerID().equals(currentUser.getUserID())) {
            selected = true;
        } else if (session.getSessionCoordinatorID().equals(currentUser.getUserID())) {
            selected = true;
        } else if (session.getOtherStaffIDList() != null &&
                session.getOtherStaffIDList().size() > 0) {
            for (UUID otherStaffID : session.getOtherStaffIDList()) {
                if (otherStaffID.equals(currentUser.getUserID())) {
                    selected = true;
                    break;
                }
            }
        }
        return selected;
    }

    private boolean selectSession(Session session) {
        boolean selected = session.search(searchText);
        // Test explicit select modes
        if (selected) {
            // Build 218 Some users have seen crash here due t0 getActivity() returning null
            // Replace with requireActivity which raises IllegalStateException which is
            // trapped in LoadAdapter background task which then exits
            //switch (((ListActivity) getActivity()).getSelectMode()) {
            switch (((ListActivity) requireActivity()).getSelectMode()) {
                case ALL:
                    selected = true;
                    break;

                case UNCANCELLED:
                    if (session.getCancelledFlag()) {
                        selected = false;
                    }
                    break;

                case FUTURE:
                    if (today.after(session.getReferenceDate())) {
                        selected = false;
                    }
                    break;

                case GROUPS:
                    if (!((ListSessions) requireActivity()).getSelectedIDs().contains(session.getGroupID())) {
                        //if (!session.getGroupID().equals(selectedID)) {
                        selected = false;
                    }
                    break;

                case SESSION_COORDINATORS:
                    if (!((ListSessions) requireActivity()).getSelectedIDs().contains(session.getSessionCoordinatorID())) {
                        //if (!session.getSessionCoordinatorID().equals(selectedID)) {
                        selected = false;
                    }
                    break;
                default:
                    // Awaiting Group, Keyworker etc.
                    selected = false;
            }
        }
        return selected;
    }

    // MENU BLOCK
    private static final int MENU_EXPORT = Menu.FIRST + 1;
    private static final int MENU_SELECT_ALL_SESSIONS = Menu.FIRST + 5;
    private static final int MENU_SELECT_UNCANCELLED_SESSIONS = Menu.FIRST + 6;
    private static final int MENU_SELECT_FUTURE_SESSIONS = Menu.FIRST + 7;
    private static final int MENU_SELECT_GROUPS = Menu.FIRST + 8;
    private static final int MENU_SELECT_SESSION_COORDINATORS = Menu.FIRST + 9;
    private static final int MENU_SORT_DATE = Menu.FIRST + 10;
    private static final int MENU_SORT_NAME = Menu.FIRST + 11;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance(getActivity());

        //SHARE
        // Build 126 - share is only relevant in Read fragments
        MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        shareOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        shareOption.setVisible(false);
        shareOption.setEnabled(false);

        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ALLOW_EXPORT)) {
            MenuItem selectExport = menu.add(0, MENU_EXPORT, 1, "Export to Google Sheets");
            selectExport.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        MenuItem selectAllOption = menu.add(0, MENU_SELECT_ALL_SESSIONS, 1, "Show All Sessions");
        selectAllOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem selectFutureOption = menu.add(0, MENU_SELECT_FUTURE_SESSIONS, 2, "Show Future Sessions");
        selectFutureOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);


        MenuItem selectUncancelledOption = menu.add(0, MENU_SELECT_UNCANCELLED_SESSIONS, 3, "Show Uncancelled Sessions");
        selectUncancelledOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);


        MenuItem selectGroupOption = menu.add(0, MENU_SELECT_GROUPS, 4, String.format("Select %ss ", localSettings.Group));
        selectGroupOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem selectOpenOption = menu.add(0, MENU_SELECT_SESSION_COORDINATORS, 5, String.format("Select %ss ", localSettings.SessionCoordinator));
        selectOpenOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem sortLastFirstOption = menu.add(0, MENU_SORT_DATE, 10, "Sort by Date");
        sortLastFirstOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem sortGroupOption = menu.add(0, MENU_SORT_NAME, 11, "Sort by Name");
        sortGroupOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        ActionBar supportActionBar = ((ListSessions) getActivity()).getSupportActionBar();
        if (supportActionBar != null) {
            sv = new SearchView(supportActionBar.getThemedContext());
            MenuItemCompat.setShowAsAction(searchItem,
                    MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            sv.setIconified(isSearchIconified);
            sv.setSubmitButtonEnabled(true);
            sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchText = query;
                    sv.clearFocus();
                    new LoadAdapter().execute();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
            MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    searchText = "";
                    isSearchIconified = true;
                    new LoadAdapter().execute();
                    return true;  // Return true to collapse action view
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    isSearchIconified = false;
                    return true;  // Return true to expand action view
                }
            });
            MenuItemCompat.setActionView(searchItem, sv);
            if (!isSearchIconified) {
                searchItem.expandActionView();
                sv.setQuery(searchText, false);
                sv.clearFocus();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final LocalSettings localSettings = LocalSettings.getInstance(getActivity());
        PickListDialogFragment dialog;
        switch (item.getItemId()) {
            case MENU_EXPORT:
                ((ListActivity) getActivity()).setExportListType("My Sessions");
                switch (((ListActivity) getActivity()).getSelectMode()) {
                    case ALL:
                        ((ListActivity) getActivity()).setExportSelection("All Sessions (inc. cancelled sessions)");
                        break;
                    case UNCANCELLED:
                        ((ListActivity) getActivity()).setExportSelection("All Sessions");
                        break;
                    case FUTURE:
                        ((ListActivity) getActivity()).setExportSelection("All Future Sessions");
                        break;
                    case GROUPS:
                        ((ListActivity) getActivity()).setExportSelection(String.format("%s: %s",
                                localSettings.Group,
                                ((ListSessions) getActivity()).getSelectedValues()));
                        break;
                    case SESSION_COORDINATORS:
                        ((ListActivity) getActivity()).setExportSelection(String.format("%s: %s",
                                localSettings.Keyworker,
                                ((ListSessions) getActivity()).getSelectedValues()));
                        break;
                }
                switch (sortMode) {
                    case DATE:
                        ((ListActivity) getActivity()).setExportSort("Session Date");
                        break;
                    case NAME:
                        ((ListActivity) getActivity()).setExportSort("Session Name");
                        break;
                }
                if (searchText.isEmpty()) {
                    ((ListActivity) getActivity()).setExportSearch("No Search Used");
                } else {
                    ((ListActivity) getActivity()).setExportSearch(searchText);
                }
                // Build 200 Use the androidX Fragment class
                //FragmentManager fragmentManager = getFragmentManager();
                //FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                //Fragment fragment = new CRISExport();
                //fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                //fragmentTransaction.commit();
                getParentFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .replace(R.id.content, CRISExport.class, null)
                        .commit();
                return true;

            case MENU_SELECT_ALL_SESSIONS:
                ((ListActivity) getActivity()).setSelectMode(ListActivity.SelectMode.ALL);
                // Build 181 - Load adapter in the background
                new LoadAdapter().execute();
                return true;

            case MENU_SELECT_UNCANCELLED_SESSIONS:
                ((ListActivity) getActivity()).setSelectMode(ListActivity.SelectMode.UNCANCELLED);
                // Build 181 - Load adapter in the background
                new LoadAdapter().execute();
                return true;

            case MENU_SELECT_FUTURE_SESSIONS:
                ((ListActivity) getActivity()).setSelectMode(ListActivity.SelectMode.FUTURE);
                // Build 181 - Load adapter in the background
                new LoadAdapter().execute();
                return true;

            case MENU_SELECT_GROUPS:
                //ArrayList<String> itemList = new ArrayList<>();
                //for (Group group : displayedGroups) {
                //    itemList.add(group.getItemValue());
                //}
                PickList groups = new PickList(displayedGroups);
                // Build 200 - Replaced single selection with checkbox selection for picklists
                dialog = new PickListDialogFragment(
                        String.format("Select one or more %ss", localSettings.Group),
                        groups, ListActivity.SelectMode.GROUPS);
                dialog.show(getParentFragmentManager(), null);
                return true;

            case MENU_SELECT_SESSION_COORDINATORS:
                //ArrayList<String> itemList = new ArrayList<>();
                //for (User sessionCoordinator : displayedSessionCoordinators) {
                //    itemList.add(sessionCoordinator.getFullName());
                //}
                PickList sessionCoordinators = new PickList(displayedSessionCoordinators);
                // Build 200 - Replaced single selection with checkbox selection for picklists
                dialog = new PickListDialogFragment(
                        String.format("Show sessions with one or more %ss", localSettings.SessionCoordinator),
                        sessionCoordinators, ListActivity.SelectMode.SESSION_COORDINATORS);
                dialog.show(getParentFragmentManager(), null);
                return true;

            case MENU_SORT_DATE:
                sortMode = SortMode.DATE;
                Collections.sort(((ListActivity) getActivity()).getSessionAdapterList(), Session.comparatorDate);
                adapter.notifyDataSetChanged();
                return true;

            case MENU_SORT_NAME:
                sortMode = SortMode.NAME;
                Collections.sort(((ListActivity) getActivity()).getSessionAdapterList(), Session.comparatorAZ);
                adapter.notifyDataSetChanged();
                return true;

            default:
                return false;
        }
    }
/*
    private void selectGroup() {
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance(getActivity());
        // Generate list group names to display
        ArrayList<String> itemList = new ArrayList<>();
        for (Group group : displayedGroups) {
            itemList.add(group.getItemValue());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(String.format("Show sessions for the following %s:", localSettings.Group));

        String[] items = itemList.toArray(new String[itemList.size()]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedID = displayedGroups.get(which).getListItemID();
                selectedValue = displayedGroups.get(which).getItemValue();
                selectMode = SelectMode.GROUP;
                // Build 181 - Load adapter in the background
                new LoadAdapter().execute();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void selectSessionCoordinator() {
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance(getActivity());
        // Generate list of  names to display
        ArrayList<String> itemList = new ArrayList<>();
        for (User sessionCoordinator : displayedSessionCoordinators) {
            itemList.add(sessionCoordinator.getFullName());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(String.format("Show clients for the following %s:", localSettings.SessionCoordinator));
        String[] items = itemList.toArray(new String[itemList.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedID = displayedSessionCoordinators.get(which).getUserID();
                selectedValue = displayedSessionCoordinators.get(which).getFullName();
                selectMode = SelectMode.SESSION_COORDINATOR;
                // Build 181 - Load adapter in the background
                new LoadAdapter().execute();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

 */

    private void doNoPrivilege() {
        new AlertDialog.Builder(getActivity())
                .setTitle("No Privilege")
                .setMessage("Unfortunately, this option is not available.")
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void doReadSession(int position) {
        Session session = adapter.getItem(position);
        listViewState = listView.onSaveInstanceState();
        // Save this recordID to enable check for change to client
        oldClientRecordID = session.getRecordID();
        // Client is serializable so can pass as extra to List Activity
        Intent intent = new Intent(getActivity(), ListSessionClients.class);
        intent.putExtra(Main.EXTRA_DOCUMENT, session);
        startActivity(intent);
    }

    private boolean doEditSession(int position) {
        Session session = adapter.getItem(position);
        listViewState = listView.onSaveInstanceState();
        // Save this recordID to enable check for change to client
        oldClientRecordID = session.getRecordID();
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_EDIT_ALL_SESSIONS) ||
                session.getSessionCoordinatorID().equals(currentUser.getUserID())) {
            ((ListActivity) getActivity()).setSession(session);
            // Build 200 Use the androidX Fragment class
            //FragmentManager fragmentManager = getFragmentManager();
            //FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            //Fragment fragment = new EditSession();
            //fragmentTransaction.replace(R.id.content, fragment);
            //fragmentTransaction.addToBackStack(null);
            //fragmentTransaction.commit();
            getParentFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .setReorderingAllowed(true)
                    .replace(R.id.content, EditSession.class, null)
                    .commit();
        } else {
            doNoPrivilege();
        }
        return true;
    }

    private boolean doNewSession() {
        ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
        listViewState = listView.onSaveInstanceState();
        ((ListActivity) getActivity()).setSession(new Session(currentUser));
        // Build 200 Use the androidX Fragment class
        //FragmentManager fragmentManager = getFragmentManager();
        //FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        //Fragment fragment = new EditSession();
        //fragmentTransaction.replace(R.id.content, fragment);
        //fragmentTransaction.addToBackStack(null);
        //fragmentTransaction.commit();
        Fragment editSessionFragment = new EditSession();
        getParentFragmentManager().beginTransaction()
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .replace(R.id.content, editSessionFragment, "EditSessionFragment")
                .commit();
        return true;
    }

    private void displayDocumentHistory(int position, SwipeDetector.Action action) {
        Session session = adapter.getItem(position);
        listViewState = listView.onSaveInstanceState();
        //Loop through all instances of the document gathering data
        String history = "";
        ArrayList<UUID> recordIDs = localDB.getRecordIDs(session);
        for (int i = 0; i < recordIDs.size(); i++) {
            boolean isEarliest = (i == recordIDs.size() - 1);
            history += localDB.getDocumentMetaData(recordIDs.get(i), isEarliest, action);
            if (!isEarliest) {
                history += Session.getChanges(localDB, recordIDs.get(i + 1), recordIDs.get(i), action);
            }
        }
        history += String.format("\nThe current document contents are:\n\n%s\n", session.textSummary());
        Intent intent = new Intent(getActivity(), AlertAndContinue.class);
        intent.putExtra("title", String.format("Change History - %s", session.getDocumentTypeString()));
        intent.putExtra("message", history);
        startActivity(intent);
    }

    private class SessionAdapter extends ArrayAdapter<Session> {

        // Constructor
        SessionAdapter(Context context, List<Session> objects) {
            super(context, 0, objects);
        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.layout_list_item, parent, false);
            }

            ImageView viewItemIcon = convertView.findViewById(R.id.item_icon);
            TextView viewItemDate = convertView.findViewById(R.id.item_date);
            TextView viewItemMainText = convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = convertView.findViewById(R.id.item_additional_text);

            final Session session = ((ListActivity) getActivity()).getSessionAdapterList().get(position);

            // Set the colour (for cancellations)
            int color;
            if (session.getCancelledFlag()) {
                color = ContextCompat.getColor(convertView.getContext(), R.color.red);

            } else {
                color = ContextCompat.getColor(convertView.getContext(), R.color.text_grey);
            }
            viewItemMainText.setTextColor(color);
            viewItemAdditionalText.setTextColor(color);
            viewItemDate.setTextColor(color);

            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_sessions));
            viewItemMainText.setText(session.getSessionName());
            SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK);
            viewItemDate.setText(sDate.format(session.getReferenceDate()));
            viewItemAdditionalText.setText(String.format("%s (%s)",
                    session.getSessionCoordinator().getFullName(),
                    session.getSessionCoordinator().getContactNumber()));


            return convertView;
        }
    }

    // Build 181 - Load Adapter in a background thread
    private class LoadAdapter extends AsyncTask<Void, String, String> {

        private ArrayList<Session> tempAdapterList;

        private int hidden;

        @Override
        protected String doInBackground(Void... params) {
            LocalDB localDB = LocalDB.getInstance();

            try {
                // Load the sessions from the database
                if (sessionList == null) {
                    // Load the clients from the database
                    sessionList = localDB.getAllSessions();
                }
                // Clear the lists of displayed Groups/Sessions
                displayedGroups = new ArrayList<>();
                displayedSessionCoordinators = new ArrayList<>();
                // Create the temporary adapter list
                tempAdapterList = new ArrayList<Session>();
                hidden = 0;
                for (Session session : sessionList) {
                    loadDisplayedGroups(session);
                    loadDisplayedSessionCoordinators(session);
                    if (mySession(session)) {
                        if (selectSession(session)) {
                            tempAdapterList.add(session);
                        } else {
                            hidden++;
                        }
                    }
                }
                // Sort the 'displayed' lists
                Collections.sort(displayedGroups, ListItem.comparatorAZ);
                Collections.sort(displayedSessionCoordinators, User.comparator);
                switch (sortMode) {
                    case DATE:
                        // Build 181
                        //Collections.sort(((ListActivity) getActivity()).getSessionAdapterList(), Session.comparatorDate);
                        Collections.sort(tempAdapterList, Session.comparatorDate);
                        break;
                    case NAME:
                        // Build 181
                        //Collections.sort(((ListActivity) getActivity()).getSessionAdapterList(), Session.comparatorAZ);
                        Collections.sort(tempAdapterList, Session.comparatorAZ);
                        break;
                }
            } catch (IllegalStateException ex) {
                //Build 218 If the user uses back arrow to abandon the fragment, calls to
                // requireActivity() can raise this exception. Load may simply be abandoned
                // since fragment doesn't exist.
            }
            return "";
        }

        @Override
        protected void onPreExecute() {
            // Runs on UI Thread
            startTime = new Date();     // Used to display execution time
            footer.setText("loading...");
            // Clear the adapter to show reload has started
            adapter.clear();
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(String output) {
            // Runs on UI Thread
            // Set the footer text
            if (hidden > 1) {
                footerText = String.format(Locale.UK, "%d sessions are not shown.", hidden);
            } else if (hidden == 1) {
                footerText = String.format(Locale.UK, "%d session is not shown.", hidden);
            } else {
                footerText = String.format("%s", getString(R.string.info_all_sessions_shown));
            }
            Date endTime = new Date();
            long elapsed = (endTime.getTime() - startTime.getTime()) / 1000;
            if (elapsed > 0) {
                footer.setText(String.format("%s (%d sec)", footerText, elapsed));
            } else {
                footer.setText(footerText);
            }
            // Reload the adapter list
            ((ListActivity) getActivity()).setSessionAdapterList(new ArrayList<Session>());
            for (Session session : tempAdapterList) {
                ((ListActivity) getActivity()).getSessionAdapterList().add(session);
            }
            adapter = new SessionAdapter(getActivity(), ((ListActivity) getActivity()).getSessionAdapterList());
            listView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }

        private void loadDisplayedGroups(Session session) {
            boolean found = false;
            for (Group group : displayedGroups) {
                if (group.getListItemID().equals(session.getGroupID())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Build 168 - Ignore 'Unknown Group'
                Group group = session.getGroup();
                if (group.getListItemID() != Group.unknownGroupID) {
                    displayedGroups.add(group);
                }
            }
        }

        private void loadDisplayedSessionCoordinators(Session session) {
            boolean found = false;
            for (User sessionCoordinator : displayedSessionCoordinators) {
                if (sessionCoordinator.getUserID().equals(session.getSessionCoordinatorID())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                displayedSessionCoordinators.add(session.getSessionCoordinator());
            }
        }
    }
}
