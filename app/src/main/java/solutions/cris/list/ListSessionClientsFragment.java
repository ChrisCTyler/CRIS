package solutions.cris.list;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

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

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.EditMyWeek;
import solutions.cris.edit.EditNote;
import solutions.cris.edit.EditTransport;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.Contact;
import solutions.cris.object.Document;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.MyWeek;
import solutions.cris.object.Note;
import solutions.cris.object.NoteType;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.Role;
import solutions.cris.object.Session;
import solutions.cris.object.Status;
import solutions.cris.object.Transport;
import solutions.cris.object.User;
import solutions.cris.read.ReadMyWeek;
import solutions.cris.read.ReadTransport;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.utils.CRISExport;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.LocalSettings;
import solutions.cris.utils.PickList;
import solutions.cris.utils.PickListDialogFragment;
import solutions.cris.utils.SwipeDetector;

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

public class ListSessionClientsFragment extends Fragment {

    // Build 110 - Added School, Agency
    private enum AttendanceMode {
        INVITED, RESERVED
    }

    private ArrayList<RegisterEntry> registerAdapterList;
    private ArrayList<ClientEntry> clientAdapterList;
    //Build 186 - New variables Client view
    private ArrayList<ClientEntry> displayedClientList;
    private ArrayList<ClientEntry> hiddenClientList;


    private ListView listView;
    private View parent;
    private Session session;
    private LocalDB localDB;
    private LocalSettings localSettings;
    private User currentUser;
    private Date tomorrow = CRISUtil.midnightLater(new Date());
    private Date today = CRISUtil.midnightEarlier(new Date());

    private enum SortMode {FIRST_LAST, LAST_FIRST, ATTENDED, AGE,}

    private SortMode sortMode = SortMode.LAST_FIRST;

    // Used to switch between show existing invitees and get new invitees
    private boolean displayAllClients = false;

    // Build 186 Cannot show and hide search so don't try
    // Build 105 - Added Search
    //private MenuItem searchItem;
    private SearchView sv;
    private String searchText = "";
    private boolean isSearchIconified = true;

    // Build 181 - Load Adapter on background thread
    private TextView footer;
    private Date startTime;
    ClientEntryAdapter clientAdapter;
    private RegisterEntryAdapter registerAdapter;

    // Build 200 - This is called by the event listener in ListClients as a result of a OK
    // in the PickListDialogFragment
    public void pickListDialogFragmentOK() {
        onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.layout_list, container, false);
        footer = getActivity().findViewById(R.id.footer);
        // Build 200 Instantiate a new SelectdIDs array and set the defualt mode to OPEN
        ((ListSessionClients) getActivity()).setSelectedIDs(new ArrayList<>());
        ((ListSessionClients) getActivity()).clearSelectedValues();
        // Build 217 Restored the correct default
        ((ListActivity) getActivity()).setSelectMode(((ListActivity.SelectMode.UNCANCELLED)));
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        toolbar.setTitle(getString(R.string.app_name) + " - Session");

        session = ((ListSessionClients) getActivity()).getSession();
        localDB = LocalDB.getInstance();
        currentUser = User.getCurrentUser();
        // Use local settings for 'local' labels
        localSettings = LocalSettings.getInstance(getActivity());

        // Initialise the list view
        listView = parent.findViewById(R.id.list_view);
        final SwipeDetector swipeDetector = new SwipeDetector();
        listView.setOnTouchListener(swipeDetector);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (swipeDetector.swipeDetected()) {
                    if (displayAllClients) {
                        //displayDocumentHistory(position, swipeDetector.getAction());
                    } else {
                        displayClientSessionHistory(position, swipeDetector.getAction());
                    }
                }
            }
        });

        final FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_CREATE_SESSIONS) ||
                currentUser.getRole().hasPrivilege(Role.PRIVILEGE_EDIT_ALL_SESSIONS)) {
            fab.setVisibility(View.VISIBLE);
            fab.setImageResource(R.drawable.ic_fab_users);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (displayAllClients) {
                        fab.setImageResource(R.drawable.ic_fab_users);
                        displayAllClients = false;
                    } else {
                        fab.setImageResource(R.drawable.ic_fab_session);
                        displayAllClients = true;
                    }
                    // Build 186 - Clear the search
                    onResume();
                }
            });
        } else {
            fab.setVisibility(View.GONE);
        }

        // This is set true when editing myweek to prevent use of back button
        ((ListSessionClients) getActivity()).setEditMyWeek(false);

    }

    @Override
    public void onResume() {
        super.onResume();
        ((ListSessionClients) getActivity()).loadSessionHeader(session);
        if (displayAllClients) {
            onResumeAllClients();
        } else {
            onResumeRegister();
        }
    }

    private void onResumeRegister() {
        // Build 186 Cannot show and hide search so don't try
        // Build 105 - Search visibility
        //if (searchItem != null) {
        //    searchItem.setVisible(false);
        //}
        // Build 181 - Load Adapter on background thread
        new LoadRegisterAdapter().execute();

    }

    private void onResumeAllClients() {
        // Build 186 Cannot show and hide search so don't try
        // Build 105 - Search visibility
        //searchItem.setVisible(true);
        // Build 181 - Load Adapter on background thread
        new LoadAllClientsAdapter().execute();
    }


    // MENU BLOCK
    private static final int MENU_EXPORT = Menu.FIRST + 1;
    private static final int MENU_SELECT_ALL = Menu.FIRST + 2;
    private static final int MENU_SELECT_UNCANCELLED = Menu.FIRST + 3;
    // Build 200 - Added select options to menu
    private static final int MENU_SELECT_GROUPS = Menu.FIRST + 4;
    private static final int MENU_SELECT_KEYWORKERS = Menu.FIRST + 5;
    private static final int MENU_SELECT_COMMISSIONERS = Menu.FIRST + 6;
    private static final int MENU_SELECT_SCHOOLS = Menu.FIRST + 7;
    private static final int MENU_SELECT_AGENCIES = Menu.FIRST + 8;

    private static final int MENU_SORT_FIRST_LAST = Menu.FIRST + 10;
    private static final int MENU_SORT_LAST_FIRST = Menu.FIRST + 11;
    private static final int MENU_SORT_ATTENDED = Menu.FIRST + 12;
    private static final int MENU_SORT_AGE = Menu.FIRST + 13;
    private static final int MENU_BROADCAST = Menu.FIRST + 20;
    private static final int MENU_SEND_MYWEEK_LINK = Menu.FIRST + 30;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
        MenuItem selectAllOption = menu.add(0, MENU_SELECT_ALL, 2, "Show All Clients");
        selectAllOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        MenuItem selectUncancelledOption = menu.add(0, MENU_SELECT_UNCANCELLED, 3, "Show Uncancelled Clients");
        selectUncancelledOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // Build 200 - Added select options to menu
        MenuItem selectGroupOption = menu.add(0, MENU_SELECT_GROUPS, 4, String.format("Select %ss", localSettings.Group));
        selectGroupOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem selectKeyworkerOption = menu.add(0, MENU_SELECT_KEYWORKERS, 5, String.format("Select %ss", localSettings.Keyworker));
        selectKeyworkerOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem selectCommissionerOption = menu.add(0, MENU_SELECT_COMMISSIONERS, 6, String.format("Select %ss", localSettings.Commisioner));
        selectCommissionerOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        //Build 110 Added School, Agency
        MenuItem selectSchoolOption = menu.add(0, MENU_SELECT_SCHOOLS, 7, "Select Schools");
        selectSchoolOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem selectAgencyOption = menu.add(0, MENU_SELECT_AGENCIES, 8, "Select Agencies");
        selectAgencyOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);


        MenuItem sortFirstLastOption = menu.add(0, MENU_SORT_FIRST_LAST, 10, "Sort by FirstName");
        sortFirstLastOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        MenuItem sortLastFirstOption = menu.add(0, MENU_SORT_LAST_FIRST, 11, "Sort by Last Name");
        sortLastFirstOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        Date today = new Date();
        if (today.before(session.getReferenceDate())) {
            MenuItem sortAttendedOption = menu.add(0, MENU_SORT_ATTENDED, 12, "Sort by Attendance");
            sortAttendedOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        MenuItem sortAgeOption = menu.add(0, MENU_SORT_AGE, 13, "Sort by Age");
        sortAgeOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        MenuItem broadcastOption = menu.add(0, MENU_BROADCAST, 20, "Broadcast Message");
        broadcastOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        MenuItem sendMyWeekLinkOption = menu.add(0, MENU_SEND_MYWEEK_LINK, 21, "Send MyWeek Links");
        sendMyWeekLinkOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // Build 186 Revert to standard search
        /*
        // Build 105 Add Search at client level
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        // Initial visibility is false since register is the first list displayed
        // onResumeAllClients will display the search option
        // Build 186 Cannot show and hide search so don't try
        //searchItem.setVisible(false);
        ActionBar supportActionBar = ((ListSessionClients) getActivity()).getSupportActionBar();
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
                    onResumeAllClients();
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
                    onResumeAllClients();
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
         */
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        ActionBar supportActionBar = ((ListSessionClients) getActivity()).getSupportActionBar();
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
                    if (displayAllClients) {
                        new LoadAllClientsAdapter().execute();
                    } else {
                        new LoadRegisterAdapter().execute();
                    }
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
                    if (displayAllClients) {
                        new LoadAllClientsAdapter().execute();
                    } else {
                        new LoadRegisterAdapter().execute();
                    }
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
        SimpleDateFormat sDate = new SimpleDateFormat("WWW dd/MM/yyyy", Locale.UK);
        PickListDialogFragment dialog;
        switch (item.getItemId()) {
            case MENU_EXPORT:
                ((ListActivity) getActivity()).setExportListType("One Session");
                ((ListActivity) getActivity()).setExportSelection(String.format("%s - %s",
                        session.getSessionName(),
                        sDate.format(session.getReferenceDate())));
                ((ListActivity) getActivity()).setExportSort(" ");
                ((ListActivity) getActivity()).setExportSearch(" ");
                ((ListActivity) getActivity()).setSessionAdapterList(new ArrayList<Session>());
                ((ListActivity) getActivity()).getSessionAdapterList().add(session);
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
            case MENU_SELECT_ALL:
                ((ListActivity) getActivity()).setSelectMode(ListActivity.SelectMode.ALL);
                onResume();
                return true;
            case MENU_SELECT_UNCANCELLED:
                ((ListActivity) getActivity()).setSelectMode(ListActivity.SelectMode.UNCANCELLED);
                onResume();
                return true;

            // Build 200 - Added select options to menu
            case MENU_SELECT_GROUPS:
                final PickList groups = new PickList(localDB, ListType.GROUP, 0);
                // Build 200 - Replaced single selection with checkbox selection for picklists
                dialog = new PickListDialogFragment(
                        String.format("Select one or more %ss", localSettings.Group),
                        groups, ListActivity.SelectMode.GROUPS);
                dialog.show(getParentFragmentManager(), null);
                return true;

            case MENU_SELECT_KEYWORKERS:
                // Get a list of keyworkers
                ArrayList<User> users = localDB.getAllUsers();
                ArrayList<User> keyworkerList = new ArrayList<>();
                for (User user : users) {
                    if (user.getRole().hasPrivilege(Role.PRIVILEGE_USER_IS_KEYWORKER)) {
                        keyworkerList.add(user);
                    }
                }
                Collections.sort(keyworkerList, User.comparator);
                final PickList keyworkers = new PickList(keyworkerList, 0);
                // Build 200 - Replaced single selection with checkbox selection for picklists
                dialog = new PickListDialogFragment(
                        String.format("Select one or more %ss", localSettings.Keyworker),
                        keyworkers, ListActivity.SelectMode.KEYWORKERS);
                dialog.show(getParentFragmentManager(), null);
                return true;

            case MENU_SELECT_COMMISSIONERS:
                final PickList commissioners = new PickList(localDB, ListType.COMMISSIONER, 0);
                // Build 200 - Replaced single selection with checkbox selection for picklists
                dialog = new PickListDialogFragment(
                        String.format("Select one or more %ss", localSettings.Commisioner),
                        commissioners, ListActivity.SelectMode.COMMISSIONERS);
                dialog.show(getParentFragmentManager(), null);
                return true;

            case MENU_SELECT_SCHOOLS:
                final PickList schools = new PickList(localDB, ListType.SCHOOL, 0);
                // Build 200 - Replaced single selection with checkbox selection for picklists
                dialog = new PickListDialogFragment(
                        String.format("Select one or more %ss", "School"),
                        schools, ListActivity.SelectMode.SCHOOLS);
                dialog.show(getParentFragmentManager(), null);
                return true;

            case MENU_SELECT_AGENCIES:
                final PickList agencies = new PickList(localDB, ListType.AGENCY, 0);
                // Build 200 - Replaced single selection with checkbox selection for picklists
                dialog = new PickListDialogFragment(
                        "Select one or more Agencies",
                        agencies, ListActivity.SelectMode.AGENCIES);
                dialog.show(getParentFragmentManager(), null);
                return true;

            case MENU_SORT_FIRST_LAST:
                sortMode = SortMode.FIRST_LAST;
                onResume();
                return true;
            case MENU_SORT_LAST_FIRST:
                sortMode = SortMode.LAST_FIRST;
                onResume();
                return true;
            case MENU_SORT_ATTENDED:
                sortMode = SortMode.ATTENDED;
                onResume();
                return true;
            case MENU_SORT_AGE:
                sortMode = SortMode.AGE;
                onResume();
                return true;
            // Build 119 30 May 2019 Broadcast handler
            case MENU_BROADCAST:
                // Load Broadcast Client List from the selected clients
                ArrayList<Client> broadcastClientList = new ArrayList<>();
                for (RegisterEntry entry : registerAdapterList) {
                    broadcastClientList.add(entry.getClientSession().getClient());
                }
                ((ListActivity) getActivity()).setBroadcastClientList(broadcastClientList);
                // Start the Broadcast fragment
                // Build 200 Use the androidX Fragment class
                //fragmentManager = getFragmentManager();
                //fragmentTransaction = fragmentManager.beginTransaction();
                //fragment = new BroadcastMessageFragment();
                //fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                //fragmentTransaction.commit();
                getParentFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .replace(R.id.content, BroadcastMessageFragment.class, null)
                        .commit();
                return true;
            // Build 143 Send MyWeekLink handler
            case MENU_SEND_MYWEEK_LINK:
                // Load Client List from the selected clients
                ArrayList<Client> sendMyWeekClientList = new ArrayList<>();
                for (RegisterEntry entry : registerAdapterList) {
                    sendMyWeekClientList.add(entry.getClientSession().getClient());
                }
                ((ListActivity) getActivity()).setBroadcastClientList(sendMyWeekClientList);
                // Start the SendMyWeek fragment
                // Build 200 Use the androidX Fragment class
                //fragmentManager = getFragmentManager();
                //fragmentTransaction = fragmentManager.beginTransaction();
                //fragment = new SendMyWeekLinkFragment();
                //fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                //fragmentTransaction.commit();
                getParentFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .replace(R.id.content, SendMyWeekLinkFragment.class, null)
                        .commit();
                return true;
            default:
                // Build 105 - Added search so allow search option to return false
                //throw new CRISException("Unexpected menu option:" + item.getItemId());
                return false;
        }
    }

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

    private void displayClientSessionHistory(int position, SwipeDetector.Action action) {
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        RegisterEntry registerEntry = registerAdapter.getItem(position);
        ClientSession clientSession = registerEntry.getClientSession();
        Client client = clientSession.getClient();
        //Loop through all instances of the document gathering data
        String history = "";
        ArrayList<UUID> recordIDs = localDB.getRecordIDs(clientSession);
        for (int i = 0; i < recordIDs.size(); i++) {
            boolean isEarliest = (i == recordIDs.size() - 1);
            history += localDB.getDocumentMetaData(recordIDs.get(i), isEarliest, action);
            if (!isEarliest) {
                history += ClientSession.getChanges(localDB, recordIDs.get(i + 1), recordIDs.get(i), action);
            }

        }
        // Add some detail about the session
        if (action.equals(SwipeDetector.Action.RL)) {

            if (clientSession.getSession() != null) {
                history += String.format("\nAssociated Session:\n\nName: %s\nDate: %s\nID: %s\n",
                        clientSession.getSession().getSessionName(),
                        sDateTime.format(clientSession.getSession().getReferenceDate()),
                        clientSession.getSession().getDocumentID());
            }

        }
        history += String.format("\nThe current document contents are:\n\n%s\n%s\n",
                client.textSummary(),
                clientSession.textSummary());
        Intent intent = new Intent(getActivity(), AlertAndContinue.class);
        intent.putExtra("title", String.format("Change History - %s", "Client Session"));
        intent.putExtra("message", history);
        startActivity(intent);
    }

    private Comparator<RegisterEntry> comparatorRELastFirst = new Comparator<RegisterEntry>() {
        @Override
        public int compare(RegisterEntry r1, RegisterEntry r2) {
            ClientSession o1 = r1.getClientSession();
            ClientSession o2 = r2.getClientSession();
            // Build 110 - Added reserved option
            if (o1.isReserved() != o2.isReserved()) {
                if (o1.isReserved()) return 1;
                else return -1;
            } else if (o1.getClient().getLastName().equals(o2.getClient().getLastName())) {
                return o1.getClient().getFirstNames().compareTo(o2.getClient().getFirstNames());
            } else {
                return o1.getClient().getLastName().compareTo(o2.getClient().getLastName());
            }
        }
    };

    private Comparator<RegisterEntry> comparatorREAttended = new Comparator<RegisterEntry>() {
        @Override
        public int compare(RegisterEntry r1, RegisterEntry r2) {
            ClientSession o1 = r1.getClientSession();
            ClientSession o2 = r2.getClientSession();
            // Build 110 - Added reserved option
            if (o1.isReserved() != o2.isReserved()) {
                if (o1.isReserved()) return 1;
                else return -1;
            } else if (o1.isAttended() == o2.isAttended()) {
                if (o1.getClient().getLastName().equals(o2.getClient().getLastName())) {
                    return o1.getClient().getFirstNames().compareTo(o2.getClient().getFirstNames());
                } else {
                    return o1.getClient().getLastName().compareTo(o2.getClient().getLastName());
                }
            } else {
                if (o1.isAttended()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        }
    };

    private Comparator<RegisterEntry> comparatorREFirstLast = new Comparator<RegisterEntry>() {
        @Override
        public int compare(RegisterEntry r1, RegisterEntry r2) {
            ClientSession o1 = r1.getClientSession();
            ClientSession o2 = r2.getClientSession();
            // Build 110 - Added reserved option
            if (o1.isReserved() != o2.isReserved()) {
                if (o1.isReserved()) return 1;
                else return -1;
            } else if (o1.getClient().getFirstNames().equals(o2.getClient().getFirstNames())) {
                return o1.getClient().getLastName().compareTo(o2.getClient().getLastName());
            } else {
                return o1.getClient().getFirstNames().compareTo(o2.getClient().getFirstNames());
            }
        }
    };

    private Comparator<RegisterEntry> comparatorREAge = new Comparator<RegisterEntry>() {
        @Override
        public int compare(RegisterEntry r1, RegisterEntry r2) {
            ClientSession o1 = r1.getClientSession();
            ClientSession o2 = r2.getClientSession();
            // Build 110 - Added reserved option
            if (o1.isReserved() != o2.isReserved()) {
                if (o1.isReserved()) return 1;
                else return -1;
            } else if (o1.getClient().getDateOfBirth().equals(o2.getClient().getDateOfBirth())) {
                // Probably twins so compare on first name
                return o1.getClient().getFirstNames().compareTo(o2.getClient().getFirstNames());
            } else {
                return o2.getClient().getDateOfBirth().compareTo(o1.getClient().getDateOfBirth());
            }
        }
    };


    private class RegisterEntry {

        RegisterEntry(ClientSession clientSession) {
            this.clientSession = clientSession;
            /*
            // Build 181 Use SQL to return only this SessionID
            // Find any associated note / pdf
            ArrayList<Document> notes = localDB.getAllDocumentsOfType(clientSession.getClientID(), Document.Note);
            for (Document document : notes) {
                Note note = (Note) document;
                if (note.getSessionID() != null && note.getSessionID().equals(clientSession.getSessionID())) {
                    this.note = note;
                    break;
                }
                // Build 110 If sticky note found
                if (note.isStickyFlag()) {
                    stickyNoteFlag = true;
                }
            }

            ArrayList<Document> pdfDocuments = localDB.getAllDocumentsOfType(clientSession.getClientID(), Document.PdfDocument);
            for (Document document : pdfDocuments) {
                PdfDocument pdf = (PdfDocument) document;
                if (pdf.getSessionID() != null && pdf.getSessionID().equals(clientSession.getSessionID())) {
                    this.pdfDocument = pdf;
                    break;
                }
            }
            ArrayList<Document> transportDocuments = localDB.getAllDocumentsOfType(clientSession.getClientID(), Document.Transport);
            for (Document document : transportDocuments) {
                Transport transport = (Transport) document;
                if (transport.getSessionID() != null && transport.getSessionID().equals(clientSession.getSessionID())) {
                    this.transport = transport;
                    break;
                }
            }
            // Repeat the following for each sub-class of Status
            ArrayList<Document> myWeekDocuments = localDB.getAllDocumentsOfType(clientSession.getClientID(), Document.MyWeek);
            for (Document document : myWeekDocuments) {
                MyWeek myWeek = (MyWeek) document;
                if (myWeek.getSessionID() != null && myWeek.getSessionID().equals(clientSession.getSessionID())) {
                    this.statusDocument = myWeek;
                    break;
                }
            }
*/

            ArrayList<Document> notes = localDB.getAllDocumentsOfType(clientSession.getClientID(),
                    Document.Note, clientSession.getSessionID());
            for (Document document : notes) {
                Note note = (Note) document;
                this.note = note;
                break;
            }
            // Build 239 - Problem using SessionID search for Notes since it only finds 'session' notes
            // and not the sticky notes but older method is too slow for large registers.
            // Fix is to set a dummy SessionID on sticky notes
            //if (note.isStickyFlag()) {
            //    stickyNoteFlag = true;
            //}
            notes = localDB.getAllDocumentsOfType(clientSession.getClientID(),
                    Document.Note, Session.stickyNoteSessionID);
            if (notes.size() > 0) {
                stickyNoteFlag = true;
            }

            // Build 181 - This mechanism is very fast but SessionID field is only set for ClientSession records
            // pre this build. Fixed by modification to document.Save() and re-save via Fix option in SysAdmin
            ArrayList<Document> pdfDocuments = localDB.getAllDocumentsOfType(clientSession.getClientID(),
                    Document.PdfDocument, clientSession.getSessionID());
            for (Document document : pdfDocuments) {
                PdfDocument pdf = (PdfDocument) document;
                this.pdfDocument = pdf;
                break;
            }
            ArrayList<Document> transportDocuments = localDB.getAllDocumentsOfType(clientSession.getClientID(),
                    Document.Transport, clientSession.getSessionID());
            for (Document document : transportDocuments) {
                Transport transport = (Transport) document;
                this.transport = transport;
                break;
            }
            // Repeat the following for each sub-class of Status
            ArrayList<Document> myWeekDocuments = localDB.getAllDocumentsOfType(clientSession.getClientID(),
                    Document.MyWeek, clientSession.getSessionID());
            for (Document document : myWeekDocuments) {
                MyWeek myWeek = (MyWeek) document;
                this.statusDocument = myWeek;
                break;
            }
        }

        private ClientSession clientSession;

        ClientSession getClientSession() {
            return clientSession;
        }

        private Note note;

        public Note getNote() {
            return note;
        }

        public void setNote(Note note) {
            this.note = note;
        }

        private Transport transport;

        Transport getTransport() {
            return transport;
        }

        void setTransport(Transport transport) {
            this.transport = transport;
        }

        private PdfDocument pdfDocument;

        PdfDocument getPdfDocument() {
            return pdfDocument;
        }

        void setPdfDocument(PdfDocument pdfDocument) {
            this.pdfDocument = pdfDocument;
        }

        private Status statusDocument;

        Status getStatusDocument() {
            return statusDocument;
        }

        void setStatusDocument(Status statusDocument) {
            this.statusDocument = statusDocument;
        }

        // Build 110 Stick Note Flag
        private boolean stickyNoteFlag;

        boolean getStickyNoteFlag() {
            return stickyNoteFlag;
        }

        void setStickyNoteFlag(boolean stickyNoteFlag) {
            this.stickyNoteFlag = stickyNoteFlag;
        }

    }

    private class RegisterEntryAdapter extends ArrayAdapter<RegisterEntry> {

        // Constructor
        RegisterEntryAdapter(Context context, List<RegisterEntry> objects) {
            super(context, 0, objects);

        }

        private void displayClientRecord(ClientSession clientSession) {
            // Display the client record
            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_MY_CLIENTS)) {
                Client client = clientSession.getClient();
                Intent intent = new Intent(getActivity(), ListClientHeader.class);
                // User is serializable so can pass as extra to EditUser Activity
                intent.putExtra(Main.EXTRA_DOCUMENT, client);
                startActivity(intent);
            } else {
                doNoPrivilege();
            }
        }

        private void toggleAttendance(final ClientSession clientSession, Transport transport) {
            if (session.getReferenceDate().before(tomorrow)) {       // PAST
                // Toggle attendance
                if (clientSession.isAttended()) {
                    clientSession.setAttended(false);
                } else {
                    clientSession.setAttended(true);
                    // Build 110 - If attended, cannot be reserved
                    if (clientSession.isReserved()) {
                        clientSession.setReserved(false);
                        // Re-sort since client will move out of the reserved list
                        switch (sortMode) {
                            case FIRST_LAST:
                                Collections.sort(registerAdapterList, comparatorREFirstLast);
                                break;
                            case LAST_FIRST:
                                Collections.sort(registerAdapterList, comparatorRELastFirst);
                                break;
                            case ATTENDED:
                                Collections.sort(registerAdapterList, comparatorREAttended);
                                break;
                            case AGE:
                                Collections.sort(registerAdapterList, comparatorREAge);
                                break;
                        }
                    }
                }
                clientSession.save(false);
                // Build 105 - Automatically update the associated taxi document
                if (transport != null) {
                    boolean transportChanged = false;
                    if (clientSession.isAttended()) {
                        if (transport.isRequiredOutbound() && !transport.isUsedOutbound()) {
                            transport.setUsedOutbound(true);
                            transportChanged = true;
                        }
                        if (transport.isRequiredReturn() && !transport.isUsedReturn()) {
                            transport.setUsedReturn(true);
                            transportChanged = true;
                        }
                    } else {
                        if (transport.isRequiredOutbound() && transport.isUsedOutbound()) {
                            transport.setUsedOutbound(false);
                            transportChanged = true;
                        }
                        if (transport.isRequiredReturn() && transport.isUsedReturn()) {
                            transport.setUsedReturn(false);
                            transportChanged = true;
                        }
                    }
                    if (transportChanged) {
                        transport.save(false);
                    }
                }
                // End of Build 105 change
                notifyDataSetChanged();
            } else {
                // Get the reason and then call the validate/save sequence.
                final EditText editText = new EditText(getActivity());
                editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                new AlertDialog.Builder(getActivity())
                        .setView(editText)
                        .setTitle("Cancel Invite")
                        .setMessage("Please specify a cancellation reason")
                        .setPositiveButton("CancelDocument", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (editText.getText().length() > 0) {
                                    clientSession.setCancellationDate(new Date());
                                    clientSession.setCancellationReason(editText.getText().toString());
                                    clientSession.setCancelledByID(((ListActivity) getActivity()).getCurrentUser().getUserID());
                                    clientSession.setCancelledFlag(true);
                                    clientSession.save(false);
                                    notifyDataSetChanged();
                                }
                            }
                        })
                        .setNegativeButton("DoNotCancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();

            }
        }

        private void doNote(ClientSession clientSession, Note noteDocument, RegisterEntry registerEntry) {
            Client client = clientSession.getClient();
            ((ListActivity) getActivity()).setClient(client);
            // Display the Client Header
            ((ListSessionClients) getActivity()).loadClientHeader(client);

            if (noteDocument == null) {
                // Create New
                ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
                registerEntry.setNote(new Note(currentUser, clientSession.getClientID()));
                registerEntry.getNote().setSessionID(clientSession.getSessionID());
                registerEntry.getNote().setSession(clientSession.getSession());
                ((ListActivity) getActivity()).setDocument(registerEntry.getNote());
                // Build 200 Use the androidX Fragment clas
                //FragmentManager fragmentManager = getFragmentManager();
                //FragmentTransaction fragmentTransaction;
                //Fragment fragment = new EditNote();
                //fragmentTransaction = fragmentManager.beginTransaction();
                //fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                //fragmentTransaction.commit();
                getParentFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .replace(R.id.content, EditNote.class, null)
                        .commit();
                notifyDataSetChanged();
            } else {
                // Read Note
                // Use plus sign for note in read mode (add response document)
                final FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
                fab.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_fab_plus));
                ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                ((ListActivity) getActivity()).setDocument(noteDocument);
                localDB.read(noteDocument, currentUser);
                // Build 200 Use the androidX Fragment class
                //FragmentManager fragmentManager = getFragmentManager();
                //FragmentTransaction fragmentTransaction;
                //Fragment fragment;
                //fragmentTransaction = fragmentManager.beginTransaction();
                //fragment = new EditNote();
                //fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                //fragmentTransaction.commit();
                getParentFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .replace(R.id.content, EditNote.class, null)
                        .commit();
            }
        }

        private void doTransport(ClientSession clientSession, Transport transport, RegisterEntry registerEntry, boolean longPress) {
            Client client = clientSession.getClient();
            ((ListActivity) getActivity()).setClient(client);
            // Display the Client Header
            ((ListSessionClients) getActivity()).loadClientHeader(client);
            if (transport == null) {

                ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
                Transport newTransport = new Transport(currentUser, clientSession.getClientID());
                // Load the current session details
                newTransport.setSessionID(clientSession.getSessionID());
                newTransport.setSession(clientSession.getSession());
                newTransport.setFromAddress(client.getAddress());
                newTransport.setFromPostcode(client.getPostcode());
                newTransport.setToAddress(session.getAddress());
                newTransport.setToPostcode(session.getPostcode());
                Date sessionDate = session.getReferenceDate();
                SimpleDateFormat sTime = new SimpleDateFormat("HH:mm", Locale.UK);
                newTransport.setAdditionalInformation(String.format("The session starts at %s",
                        sTime.format(sessionDate)));
                newTransport.setOutboundDate(CRISUtil.midnightEarlier(sessionDate));
                newTransport.setRequiredReturn(true);
                newTransport.setReturnDate(CRISUtil.midnightEarlier(sessionDate));
                registerEntry.setTransport(newTransport);
                ((ListActivity) getActivity()).setDocument(newTransport);
                // Build 200 Use the androidX Fragment class
                //FragmentManager fragmentManager = getFragmentManager();
                //FragmentTransaction fragmentTransaction;
                //Fragment fragment = new EditTransport();
                //fragmentTransaction = fragmentManager.beginTransaction();
                //fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                //fragmentTransaction.commit();
                getParentFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .replace(R.id.content, EditTransport.class, null)
                        .commit();
                notifyDataSetChanged();
            } else {
                // Longpress to edit
                if (longPress) {
                    ((ListActivity) getActivity()).setMode(Document.Mode.EDIT);
                    ((ListActivity) getActivity()).setDocument(transport);
                    // Build 200 Use the androidX Fragment class
                    //FragmentManager fragmentManager = getFragmentManager();
                    //FragmentTransaction fragmentTransaction;
                    //Fragment fragment = new EditTransport();
                    //fragmentTransaction = fragmentManager.beginTransaction();

                    //fragmentTransaction.replace(R.id.content, fragment);
                    //fragmentTransaction.addToBackStack(null);
                    //fragmentTransaction.commit();
                    getParentFragmentManager().beginTransaction()
                            .addToBackStack(null)
                            .setReorderingAllowed(true)
                            .replace(R.id.content, EditTransport.class, null)
                            .commit();
                    notifyDataSetChanged();
                } else {
                    // Read Transport document
                    ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                    ((ListActivity) getActivity()).setDocument(transport);
                    localDB.read(transport, currentUser);
                    // Build 200 Use the androidX Fragment class
                    //FragmentManager fragmentManager = getFragmentManager();
                    //FragmentTransaction fragmentTransaction;
                    //Fragment fragment;
                    //fragmentTransaction = fragmentManager.beginTransaction();
                    //fragment = new ReadTransport();
                    //fragmentTransaction.replace(R.id.content, fragment);
                    //fragmentTransaction.addToBackStack(null);
                    //fragmentTransaction.commit();
                    getParentFragmentManager().beginTransaction()
                            .addToBackStack(null)
                            .setReorderingAllowed(true)
                            .replace(R.id.content, ReadTransport.class, null)
                            .commit();
                }
            }
        }

        private void doPdfDoument(ClientSession clientSession, PdfDocument pdfDocument, RegisterEntry registerEntry, boolean longPress) {
            Client client = clientSession.getClient();
            ((ListActivity) getActivity()).setClient(client);
            // Display the Client Header
            ((ListSessionClients) getActivity()).loadClientHeader(client);
            if (pdfDocument == null) {
                registerEntry.setPdfDocument(new PdfDocument(currentUser, clientSession.getClientID()));
                registerEntry.getPdfDocument().setSessionID(clientSession.getSessionID());
                registerEntry.getPdfDocument().setSession(clientSession.getSession());
                ((ListActivity) getActivity()).setDocument(registerEntry.getPdfDocument());
                ((ListActivity) getActivity()).tryEditFileDocument(Document.Mode.NEW, Document.PdfDocument);
                notifyDataSetChanged();
            } else {
                if (longPress) {
                    ((ListActivity) getActivity()).setDocument(pdfDocument);
                    ((ListActivity) getActivity()).tryEditFileDocument(Document.Mode.EDIT, Document.PdfDocument);
                } else {
                    // Read Note
                    PdfDocument.displayPDFDocument(pdfDocument, getContext());
                }
            }
        }

        private void doFutureError() {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Unable to Create Document")
                    .setMessage("This session is still in the future. Documents may not be created until the day of the session.")
                    .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }

        private void doStatusDocument(ClientSession clientSession, Status statusDocument, RegisterEntry registerEntry, boolean longPress, Transport transport) {
            Client client = clientSession.getClient();
            ((ListActivity) getActivity()).setClient(client);
            // Display the Client Header
            ((ListSessionClients) getActivity()).loadClientHeader(client);
            if (statusDocument == null) {
                // MyWeek cannot be entered until the session date
                if (session.getReferenceDate().before(tomorrow)) {       // PAST
                    // Set attendance
                    if (!clientSession.isAttended()) {
                        clientSession.setAttended(true);
                        /// Build 110 - If attended, cannot be reserved
                        if (clientSession.isReserved()) {
                            clientSession.setReserved(false);
                            // Re-sort since client will move out of the reserved list
                            switch (sortMode) {
                                case FIRST_LAST:
                                    Collections.sort(registerAdapterList, comparatorREFirstLast);
                                    break;
                                case LAST_FIRST:
                                    Collections.sort(registerAdapterList, comparatorRELastFirst);
                                    break;
                                case ATTENDED:
                                    Collections.sort(registerAdapterList, comparatorREAttended);
                                    break;
                                case AGE:
                                    Collections.sort(registerAdapterList, comparatorREAge);
                                    break;
                            }
                        }
                        clientSession.save(false);
                    }
                    // Build 105 - Automatically update the associated taxi document
                    if (transport != null) {
                        boolean transportChanged = false;
                        if (transport.isRequiredOutbound() && !transport.isUsedOutbound()) {
                            transport.setUsedOutbound(true);
                            transportChanged = true;
                        }
                        if (transport.isRequiredReturn() && !transport.isUsedReturn()) {
                            transport.setUsedReturn(true);
                            transportChanged = true;
                        }

                        if (transportChanged) {
                            transport.save(false);
                        }
                    }
                    // End of Build 105 change
                    // Create New MyWeek
                    ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
                    registerEntry.setStatusDocument(new MyWeek(currentUser, clientSession.getClientID()));
                    registerEntry.getStatusDocument().setSessionID(clientSession.getSessionID());
                    registerEntry.getStatusDocument().setSession(clientSession.getSession());
                    // Status document ReferenceDate is the session date
                    registerEntry.getStatusDocument().setReferenceDate(clientSession.getReferenceDate());
                    ((ListActivity) getActivity()).setDocument(registerEntry.getStatusDocument());
                    // Build 200 Use the androidX Fragment class
                    //FragmentManager fragmentManager = getFragmentManager();
                    //FragmentTransaction fragmentTransaction;
                    //Fragment fragment = new EditMyWeek();
                    //fragmentTransaction = fragmentManager.beginTransaction();
                    //fragmentTransaction.replace(R.id.content, fragment);
                    //fragmentTransaction.addToBackStack(null);
                    //fragmentTransaction.commit();
                    getParentFragmentManager().beginTransaction()
                            .addToBackStack(null)
                            .setReorderingAllowed(true)
                            .replace(R.id.content, EditMyWeek.class, null)
                            .commit();
                    notifyDataSetChanged();
                } else {
                    doFutureError();
                }
            } else {
                if (longPress) {
                    // Edit MyWeek
                    // Set the flag to prevent back button
                    ((ListSessionClients) getActivity()).setEditMyWeek(true);
                    ((ListActivity) getActivity()).setMode(Document.Mode.EDIT);
                    ((ListActivity) getActivity()).setDocument(statusDocument);
                    // Build 200 Use the androidX Fragment class
                    //FragmentManager fragmentManager = getFragmentManager();
                    //FragmentTransaction fragmentTransaction;
                    //Fragment fragment = new EditMyWeek();
                    //fragmentTransaction = fragmentManager.beginTransaction();
                    //fragmentTransaction.replace(R.id.content, fragment);
                    //fragmentTransaction.addToBackStack(null);
                    //fragmentTransaction.commit();
                    getParentFragmentManager().beginTransaction()
                            .addToBackStack(null)
                            .setReorderingAllowed(true)
                            .replace(R.id.content, EditMyWeek.class, null)
                            .commit();
                    notifyDataSetChanged();
                } else {
                    // Read Note
                    MyWeek myWeekDocument = (MyWeek) statusDocument;
                    ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                    ((ListActivity) getActivity()).setDocument(myWeekDocument);
                    localDB.read(myWeekDocument, currentUser);
                    // Build 200 Use the androidX Fragment class
                    //FragmentManager fragmentManager = getFragmentManager();
                    //FragmentTransaction fragmentTransaction;
                    //Fragment fragment;
                    //fragmentTransaction = fragmentManager.beginTransaction();
                    //fragment = new ReadMyWeek();
                    //fragmentTransaction.replace(R.id.content, fragment);
                    //fragmentTransaction.addToBackStack(null);
                    //fragmentTransaction.commit();
                    getParentFragmentManager().beginTransaction()
                            .addToBackStack(null)
                            .setReorderingAllowed(true)
                            .replace(R.id.content, ReadMyWeek.class, null)
                            .commit();
                }
            }
        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {

            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.layout_session_register_item, parent, false);
            }
            final RegisterEntry registerEntry = registerAdapterList.get(position);
            final ClientSession clientSession = registerEntry.getClientSession();
            final Transport transportDocument = registerEntry.getTransport();
            final Note noteDocument = registerEntry.getNote();
            // Build 105 - Replaced PDF document with Camera Consent
            final PdfDocument pdfDocument = registerEntry.getPdfDocument();
            final Status statusDocument = registerEntry.getStatusDocument();
            // Build 110 Stick Note Flag
            final boolean stickyNoteFlag = registerEntry.getStickyNoteFlag();
            ImageView noteIcon = convertView.findViewById(R.id.note_icon);
            noteIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doNote(clientSession, noteDocument, registerEntry);
                }
            });

            ImageView viewItemIcon = convertView.findViewById(R.id.item_icon);
            viewItemIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayClientRecord(clientSession);
                }
            });

            TextView viewItemMainText = convertView.findViewById(R.id.item_main_text);
            viewItemMainText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayClientRecord(clientSession);
                }
            });

            ImageView attendanceIcon = convertView.findViewById(R.id.attendance_icon);
            attendanceIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleAttendance(clientSession, transportDocument);
                }
            });

            ImageView statusIcon = convertView.findViewById(R.id.status_icon);
            statusIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doStatusDocument(clientSession, statusDocument, registerEntry, false, transportDocument);
                }
            });
            statusIcon.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    doStatusDocument(clientSession, statusDocument, registerEntry, true, transportDocument);
                    return true;
                }
            });

            ImageView transportIcon = convertView.findViewById(R.id.transport_icon);
            transportIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doTransport(clientSession, transportDocument, registerEntry, false);
                }
            });
            transportIcon.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    doTransport(clientSession, transportDocument, registerEntry, true);
                    return true;
                }
            });

            // Build 105 - Removed PDF Icon, replaced by Consent Camera

            ImageView pdfDocumentIcon = convertView.findViewById(R.id.pdf_icon);
            pdfDocumentIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doPdfDoument(clientSession, pdfDocument, registerEntry, false);
                }
            });
            pdfDocumentIcon.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    doPdfDoument(clientSession, pdfDocument, registerEntry, true);
                    return true;
                }
            });

            // Build 110 - If sticky note flag, display client record
            ImageView flagIcon = convertView.findViewById(R.id.flag_icon);

            flagIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (stickyNoteFlag) {
                        displayClientRecord(clientSession);
                    }
                }
            });

            // Build 110 - Added reserve as an option
            if (clientSession.isReserved()) {
                int backgroundColor = ContextCompat.getColor(convertView.getContext(), R.color.session_background_reserved);
                convertView.setBackgroundColor(backgroundColor);
            } else {
                int backgroundColor = ContextCompat.getColor(convertView.getContext(), R.color.white);
                convertView.setBackgroundColor(backgroundColor);
            }

            // Set the colour (for cancellations)
            int color;
            if (clientSession.getCancelledFlag()) {
                color = ContextCompat.getColor(convertView.getContext(), R.color.red);

            } else {
                color = ContextCompat.getColor(convertView.getContext(), R.color.text_grey);
            }
            viewItemMainText.setTextColor(color);

            // Display the client's name
            viewItemMainText.setText(clientSession.getClient().getFullName());

            // Call client.getAge() which sets the birthday flag as a by-product)
            Client client = clientSession.getClient();
            client.getAge();

            // Rest of information displayed depends on privilege and sort type
            // (privilege takes preference).
            // Unpick the current case
            Case currentCase = null;
            if (client.getCurrentCase() != null) {
                currentCase = clientSession.getClient().getCurrentCase();
            }
            if ((currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_MY_CLIENTS)) ||
                    (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_CLIENTS))) {
                if (currentCase == null) {
                    viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_grey));
                } else {
                    if (client.isBirthday()) {
                        viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_birthday_cake));
                    } else {
                        // Icon colour may be shown (from current Case record
                        switch (currentCase.getClientStatus()) {
                            case Case.RED:
                                viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red));
                                break;
                            case Case.AMBER:
                                viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber));
                                break;
                            case Case.GREEN:
                                viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green));
                                break;
                        }
                    }
                }

            } else {
                viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_all));
            }

            // Note Icon
            if (noteDocument == null) {
                noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_grey));
            } else {
                NoteType noteType = (NoteType) noteDocument.getNoteType();
                if (noteType == null) {
                    noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_grey));
                } else {
                    switch (noteType.getNoteIcon()) {
                        case NoteType.ICON_COLOUR_RED:
                            noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_red));
                            break;
                        case NoteType.ICON_COLOUR_AMBER:
                            noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_amber));
                            break;
                        case NoteType.ICON_COLOUR_GREEN:
                            noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_green));
                            break;
                        case NoteType.ICON_COLOUR_BLUE:
                            noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_blue));
                            break;
                        case NoteType.ICON_COLOUR_RESPONSE_RED:
                            noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_red));
                            break;
                        case NoteType.ICON_COLOUR_RESPONSE_AMBER:
                            noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_amber));
                            break;
                        case NoteType.ICON_COLOUR_RESPONSE_GREEN:
                            noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_green));
                            break;
                        case NoteType.ICON_COLOUR_RESPONSE_BLUE:
                            noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_blue));
                            break;
                        // Build 151 - Added default for cases where the Note colour has not been set
                        // Not sure how this occurs but an instance was found whilst testing something else
                        //case NoteType.ICON_COLOUR_UNKNOWN:
                        //    // Pre-V1.1
                        //    if (noteDocument.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                        //        noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_blue));
                        //    } else {
                        //        noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_blue));
                        //    }
                        //    break;
                        default:
                            if (noteDocument.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                                noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_blue));
                            } else {
                                noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_blue));
                            }
                    }
                }
            }

            // Transport Icon
            if (transportDocument == null) {
                // No transport document so set icon colour according to requirement
                if (currentCase == null) {
                    transportIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_transport_white));
                } else {
                    switch (currentCase.getTransportRequired()) {
                        case "No":
                            transportIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_transport_white));
                            break;
                        case "Sometimes":
                            transportIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_transport_grey));
                            break;
                        case "Always":
                            transportIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_transport_red));
                            break;
                        default:
                            transportIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_transport_white));
                    }
                }
            } else {
                // Transport document exists so set icon colour according to content
                if ((!transportDocument.isRequiredOutbound() || (transportDocument.isRequiredOutbound() && transportDocument.isUsedOutbound())) &&
                        (!transportDocument.isRequiredReturn() || (transportDocument.isRequiredReturn() && transportDocument.isUsedReturn()))) {
                    transportIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_transport_green));
                } else if (transportDocument.isBooked()) {
                    transportIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_transport_amber));
                } else {
                    transportIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_transport_red));
                }

            }

            // Status Icon
            if (statusDocument == null) {
                statusIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_grey));
            } else {
                switch ((int) statusDocument.getScore()) {
                    case 1:
                        statusIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_red));
                        break;
                    case 2:
                        statusIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_orange));
                        break;
                    case 3:
                        statusIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_yellow));
                        break;
                    case 4:
                        statusIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_green));
                        break;
                    case 5:
                        statusIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_blue));
                        break;
                    case 6:
                        statusIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_purple));
                        break;
                    default:
                        statusIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_purple));
                }
            }

            // Attended Icon
            if (session.getReferenceDate().before(tomorrow)) {       // PAST
                if (clientSession.isAttended()) {
                    attendanceIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_tick));
                } else {
                    attendanceIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_cross));
                }
            } else {
                if (clientSession.getCancelledFlag()) {
                    attendanceIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_cross));
                } else {
                    attendanceIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_tick_grey));
                }
            }

            // Build 105 replaced PDF icon with Camera Consent icon

            // PdfDocument Icon
            if (pdfDocument == null) {
                pdfDocumentIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pdf_grey));
            } else {
                pdfDocumentIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pdf_document));
            }

            ImageView cameraIcon = convertView.findViewById(R.id.camera_icon);
            // Build 111 - Fix bug where current case is null
            if (currentCase == null) {
                cameraIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_grey));
            } else {
                if (currentCase.isPhotographyConsentFlag()) {
                    cameraIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_green));
                } else {
                    cameraIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_red));
                }
            }

            // Build 110 added 'sticky note' flag
            if (stickyNoteFlag) {
                flagIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_flag_red));
            } else {
                flagIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_flag_grey));
            }

            return convertView;
        }
    }

    private Comparator<ClientEntry> comparatorCELastFirst = new Comparator<ClientEntry>() {
        @Override
        public int compare(ClientEntry o1, ClientEntry o2) {
            if (o1.isInvited() != o2.isInvited()) {
                if (o1.isInvited()) return -1;
                else return 1;
            }
            // Build 110 - Added reserved option
            else if (o1.isReserved() != o2.isReserved()) {
                if (o1.isReserved()) return -1;
                else return 1;
            } else {
                if (o1.getClient().getLastName().equals(o2.getClient().getLastName())) {
                    return o1.getClient().getFirstNames().compareTo(o2.getClient().getFirstNames());
                } else {
                    return o1.getClient().getLastName().compareTo(o2.getClient().getLastName());
                }
            }
        }
    };

    private Comparator<ClientEntry> comparatorCEFirstLast = new Comparator<ClientEntry>() {
        @Override
        public int compare(ClientEntry o1, ClientEntry o2) {
            if (o1.isInvited() != o2.isInvited()) {
                if (o1.isInvited()) return -1;
                else return 1;
            }
            // Build 110 - Added reserved option
            else if (o1.isReserved() != o2.isReserved()) {
                if (o1.isReserved()) return -1;
                else return 1;
            } else {
                if (o1.getClient().getFirstNames().equals(o2.getClient().getFirstNames())) {
                    return o1.getClient().getLastName().compareTo(o2.getClient().getLastName());
                } else {
                    return o1.getClient().getFirstNames().compareTo(o2.getClient().getFirstNames());
                }
            }
        }
    };

    private Comparator<ClientEntry> comparatorCEAge = new Comparator<ClientEntry>() {
        @Override
        public int compare(ClientEntry o1, ClientEntry o2) {
            if (o1.isInvited() != o2.isInvited()) {
                if (o1.isInvited()) return -1;
                else return 1;
            }
            // Build 110 - Added reserved option
            else if (o1.isReserved() != o2.isReserved()) {
                if (o1.isReserved()) return -1;
                else return 1;
            } else {
                if (o1.getClient().getDateOfBirth().equals(o2.getClient().getDateOfBirth())) {
                    // Probably twins so compare on first name
                    return o1.getClient().getFirstNames().compareTo(o2.getClient().getFirstNames());
                } else {
                    return o2.getClient().getDateOfBirth().compareTo(o1.getClient().getDateOfBirth());
                }
            }
        }
    };

    private class ClientEntry {

        ClientEntry(Client client) {
            invited = false;
            reserved = false;
            this.client = client;
        }

        private boolean invited;

        boolean isInvited() {
            return invited;
        }

        void setInvited(boolean invited) {
            this.invited = invited;
        }

        // Build 110
        private boolean reserved;

        boolean isReserved() {
            return reserved;
        }

        void setReserved(boolean reserved) {
            this.reserved = reserved;
        }

        private Client client;

        public Client getClient() {
            return client;
        }

        public void setClient(Client client) {
            this.client = client;
        }

        private ClientSession clientSession;

        ClientSession getClientSession() {
            return clientSession;
        }

        void setClientSession(ClientSession clientSession) {
            this.clientSession = clientSession;
        }
    }

    private class ClientEntryAdapter extends ArrayAdapter<ClientEntry> {

        // Constructor
        // Build 186 - Force type on list
        //ClientEntryAdapter(Context context, List<ClientEntry> objects) {
        ClientEntryAdapter(Context context, ArrayList<ClientEntry> objects) {
            super(context, 0, objects);
        }

        private void displayClientRecord(Client client) {
            // Display the client record
            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_MY_CLIENTS)) {
                Intent intent = new Intent(getActivity(), ListClientHeader.class);
                // User is serializable so can pass as extra to EditUser Activity
                intent.putExtra(Main.EXTRA_DOCUMENT, client);
                startActivity(intent);
            } else {
                doNoPrivilege();
            }
        }

        // Build 110 - Replaced function to handle reserve functionality
        private void toggleAttendance(final ClientEntry clientEntry, final AttendanceMode mode) {
            Client client = clientEntry.getClient();
            // If this is a new ClientSession, create it here (final so that alert dialog can access
            final ClientSession clientSession;
            if (clientEntry.getClientSession() == null) {
                clientSession = new ClientSession(currentUser, client.getClientID());
                clientSession.setSessionID(session.getDocumentID());
                clientSession.setReferenceDate(session.getReferenceDate());
                clientSession.setSession(session);
                clientSession.setAttended(false);
                clientSession.setReserved(false);
                // Set cancelled to true so that invite/reserve has the correct effect
                clientSession.setCancelledFlag(true);
                // No need to set clientSessionChanged since Invite to Reserve will set it
                //clientSessionChanged = true;
            } else {
                clientSession = clientEntry.getClientSession();
            }
            // Action depends on current state of cancelled/reserved and the mode being toggled
            if (clientSession.getCancelledFlag()) {
                // This is either a cancelled invitation or a cancelled reservation
                // Remove the cancellation
                clientSession.setCancellationDate(null);
                clientSession.setCancellationReason("");
                clientSession.setCancelledByID(null);
                clientSession.setCancelledFlag(false);
                // Then set the reserved flag to set the required state
                if (mode.equals(AttendanceMode.RESERVED)) {
                    clientSession.setReserved(true);
                    clientEntry.setReserved(true);
                    clientEntry.setInvited(false);
                } else {
                    clientSession.setReserved(false);
                    clientEntry.setReserved(false);
                    clientEntry.setInvited(true);
                }
                // And save the change
                clientSession.save(false);
                switch (sortMode) {
                    case FIRST_LAST:
                        Collections.sort(clientAdapterList, comparatorCEFirstLast);
                        break;
                    case LAST_FIRST:
                        Collections.sort(clientAdapterList, comparatorCELastFirst);
                        break;
                    case AGE:
                        Collections.sort(clientAdapterList, comparatorCEAge);
                        break;
                    /*
                    case GROUP:
                        Collections.sort(clientAdapterList, Client.comparatorGroup);
                        break;
                    case KEYWORKER:
                        Collections.sort(clientAdapterList, Client.comparatorKeyworker);
                        break;
                    case STATUS:
                        Collections.sort(clientAdapterList, Client.comparatorStatus);
                        break;
                        */
                }
                notifyDataSetChanged();
            } else {
                // Not cancelled so currently either invited or reserved
                if ((clientSession.isReserved() && mode.equals(AttendanceMode.INVITED) ||
                        (!clientSession.isReserved() && mode.equals(AttendanceMode.RESERVED)))) {
                    // Switch to invited/reserved
                    if (mode.equals(AttendanceMode.RESERVED)) {
                        clientSession.setReserved(true);
                        clientEntry.setReserved(true);
                        clientEntry.setInvited(false);
                    } else {
                        clientSession.setReserved(false);
                        clientEntry.setReserved(false);
                        clientEntry.setInvited(true);
                    }
                    // And save the change
                    clientSession.save(false);
                    switch (sortMode) {
                        case FIRST_LAST:
                            Collections.sort(clientAdapterList, comparatorCEFirstLast);
                            break;
                        case LAST_FIRST:
                            Collections.sort(clientAdapterList, comparatorCELastFirst);
                            break;
                        case AGE:
                            Collections.sort(clientAdapterList, comparatorCEAge);
                            break;
                    /*
                    case GROUP:
                        Collections.sort(clientAdapterList, Client.comparatorGroup);
                        break;
                    case KEYWORKER:
                        Collections.sort(clientAdapterList, Client.comparatorKeyworker);
                        break;
                    case STATUS:
                        Collections.sort(clientAdapterList, Client.comparatorStatus);
                        break;
                        */
                    }
                    notifyDataSetChanged();

                } else {
                    // Do the cancellation
                    // Cancel - Get the reason and then call the validate/save sequence.
                    final EditText editText = new EditText(getActivity());
                    editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                    new AlertDialog.Builder(getActivity())
                            .setView(editText)
                            .setTitle("Cancel Invite/Reservation")
                            .setMessage("Please specify a cancellation reason. Note: if " +
                                    "the cancellation was received after the session date then " +
                                    "it should be recorded as a DNA (Did Not Attend) using the " +
                                    "Session Register, not a cancellation since any resources will " +
                                    "have been committed.")
                            .setPositiveButton("CancelDocument", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (editText.getText().length() > 0) {
                                        clientSession.setCancellationDate(new Date());
                                        clientSession.setCancellationReason(editText.getText().toString());
                                        clientSession.setCancelledByID(((ListActivity) getActivity()).getCurrentUser().getUserID());
                                        clientSession.setCancelledFlag(true);
                                        // Build 110 - If reserved then set flag
                                        if (mode == AttendanceMode.RESERVED) {
                                            clientSession.setReserved(true);
                                        } else {
                                            clientSession.setReserved(false);
                                        }
                                        clientSession.save(false);
                                        clientEntry.setInvited(false);
                                        clientEntry.setReserved(false);
                                        switch (sortMode) {
                                            case FIRST_LAST:
                                                Collections.sort(clientAdapterList, comparatorCEFirstLast);
                                                break;
                                            case LAST_FIRST:
                                                Collections.sort(clientAdapterList, comparatorCELastFirst);
                                                break;
                                            case AGE:
                                                Collections.sort(clientAdapterList, comparatorCEAge);
                                                break;
                                    /*
                                    case GROUP:
                                        Collections.sort(clientAdapterList, Client.comparatorGroup);
                                        break;
                                    case KEYWORKER:
                                        Collections.sort(clientAdapterList, Client.comparatorKeyworker);
                                        break;
                                    case STATUS:
                                        Collections.sort(clientAdapterList, Client.comparatorStatus);
                                        break;
                                        */
                                        }
                                        notifyDataSetChanged();
                                    }
                                }
                            })
                            .setNegativeButton("DoNotCancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                }

            }
            if (session.getReferenceDate().before(tomorrow) &&
                    !clientSession.isReserved() &&
                    !clientSession.getCancelledFlag()) {       // PAST
                // Assume client attended and this is late data entry
                clientSession.setAttended(true);
            } else {
                // All other cases are non-attendance
                clientSession.setAttended(false);
            }
        }

        private void toggleAttendanceOld(final ClientEntry clientEntry) {
            Client client = clientEntry.getClient();
            if (clientEntry.isInvited()) {
                // Build 110 - Reserve Option - Whichever option is ticked, cancel the
                // invitation and the move to reserved or uninvited.
                final ClientSession clientSession = clientEntry.getClientSession();
                // Cancel - Get the reason and then call the validate/save sequence.
                final EditText editText = new EditText(getActivity());
                editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                new AlertDialog.Builder(getActivity())
                        .setView(editText)
                        .setTitle("Cancel Invite")
                        .setMessage("Please specify a cancellation reason. Note: if " +
                                "the cancellation was received after the session date then " +
                                "it should be recorded as a DNA (Did Not Attend) using the " +
                                "Session Register, not a cancellation since any resources will " +
                                "have been committed.")
                        .setPositiveButton("CancelDocument", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (editText.getText().length() > 0) {
                                    clientSession.setCancellationDate(new Date());
                                    clientSession.setCancellationReason(editText.getText().toString());
                                    clientSession.setCancelledByID(((ListActivity) getActivity()).getCurrentUser().getUserID());
                                    clientSession.setCancelledFlag(true);
                                    clientSession.setAttended(false);
                                    clientSession.save(false);
                                    clientEntry.setInvited(false);
                                    switch (sortMode) {
                                        case FIRST_LAST:
                                            Collections.sort(clientAdapterList, comparatorCEFirstLast);
                                            break;
                                        case LAST_FIRST:
                                            Collections.sort(clientAdapterList, comparatorCELastFirst);
                                            break;
                                        case AGE:
                                            Collections.sort(clientAdapterList, comparatorCEAge);
                                            break;
            /*
            case GROUP:
                Collections.sort(clientAdapterList, Client.comparatorGroup);
                break;
            case KEYWORKER:
                Collections.sort(clientAdapterList, Client.comparatorKeyworker);
                break;
            case STATUS:
                Collections.sort(clientAdapterList, Client.comparatorStatus);
                break;
                */
                                    }
                                    notifyDataSetChanged();
                                }
                            }
                        })
                        .setNegativeButton("DoNotCancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();

            } else {
                // Not currently invited
                //Check for cancelled
                ClientSession clientSession;
                if (clientEntry.getClientSession() != null) {
                    // Uncancel
                    clientSession = clientEntry.getClientSession();
                    clientSession.setCancellationDate(null);
                    clientSession.setCancellationReason("");
                    clientSession.setCancelledByID(null);
                    clientSession.setCancelledFlag(false);
                    // No need to update Register since cancels are included
                } else {
                    // Not yet invited/reserved
                    clientSession = new ClientSession(currentUser, client.getClientID());
                    clientSession.setSessionID(session.getDocumentID());
                    clientSession.setReferenceDate(session.getReferenceDate());
                    clientSession.setSession(session);
                    // Add to the Register
                    // Unnecessary because FAB switch calls onResume which reloads registerAdapter
                    //registerAdapterList.add(new RegisterEntry(clientSession));
                }
                if (session.getReferenceDate().before(tomorrow)) {       // PAST
                    // Assume client attended and this is late data entry
                    clientSession.setAttended(true);
                } else {
                    // Session is in the future
                    clientSession.setAttended(false);
                }
                clientSession.save(false);
                clientEntry.setInvited(true);
                switch (sortMode) {
                    case FIRST_LAST:
                        Collections.sort(clientAdapterList, comparatorCEFirstLast);
                        break;
                    case LAST_FIRST:
                        Collections.sort(clientAdapterList, comparatorCELastFirst);
                        break;
                    case AGE:
                        Collections.sort(clientAdapterList, comparatorCEAge);
                        break;
            /*
            case GROUP:
                Collections.sort(clientAdapterList, Client.comparatorGroup);
                break;
            case KEYWORKER:
                Collections.sort(clientAdapterList, Client.comparatorKeyworker);
                break;
            case STATUS:
                Collections.sort(clientAdapterList, Client.comparatorStatus);
                break;
                */
                }
                notifyDataSetChanged();
            }

        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {

            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.layout_session_client_item, parent, false);
            }

            final ClientEntry clientEntry = clientAdapterList.get(position);
            Client client = clientEntry.getClient();

            ImageView viewItemIcon = convertView.findViewById(R.id.item_icon);
            viewItemIcon.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    displayClientRecord(clientEntry.getClient());
                    return false;
                }
            });
            ImageView invitedIcon = convertView.findViewById(R.id.attendance_icon);
            invitedIcon.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    toggleAttendance(clientEntry, AttendanceMode.INVITED);
                    return false;
                }
            });
            ImageView reserveIcon = convertView.findViewById(R.id.reserve_icon);
            reserveIcon.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    toggleAttendance(clientEntry, AttendanceMode.RESERVED);
                    return false;
                }
            });
            TextView viewItemMainText = convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = convertView.findViewById(R.id.item_additional_text);

            // Display the client's name
            viewItemMainText.setText(client.getFullName());

            // Attendance Icon
            // Build 110 - Added reserve as an option
            if (clientEntry.isInvited()) {
                invitedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_tick));
                reserveIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_session_reserve_grey));
                int backgroundColor = ContextCompat.getColor(convertView.getContext(), R.color.white);

                convertView.setBackgroundColor(backgroundColor);
            } else if (clientEntry.isReserved()) {
                invitedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_tick_grey));
                reserveIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_session_reserve));
                int backgroundColor = ContextCompat.getColor(convertView.getContext(), R.color.session_background_reserved);
                convertView.setBackgroundColor(backgroundColor);
            } else {
                invitedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_tick_grey));
                reserveIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_session_reserve_grey));
                int backgroundColor = ContextCompat.getColor(convertView.getContext(), R.color.session_background_unreserved);
                convertView.setBackgroundColor(backgroundColor);
            }

            // Call client.getAge() which sets the birthday flag as a by-product)
            client.getAge();

            // Rest of information displayed depends on privilege and sort type
            // (privilege takes preference).
            // Build 110 - Already set text colour as part of 'reserve' change
            int color = ContextCompat.getColor(convertView.getContext(), R.color.text_grey);
            // Additional text depends on sort/select
            String additionalText = "";
            // Unpick the current case
            String tier = "No Tier";
            String group = "No Group";
            String keyworkerName = "No Keyworker";
            String keyworkerContact = "";
            Case currentCase = null;
            if (client.getCurrentCase() != null) {
                currentCase = client.getCurrentCase();
                if (currentCase.getTier() != null) {
                    tier = currentCase.getTier().getItemValue();
                }
                if (currentCase.getGroup() != null) {
                    group = currentCase.getGroup().getItemValue();
                }
                // Build 139 - Second Group
                if (currentCase.getGroup2() != null) {
                    group += " +1";
                }
                if (currentCase.getKeyWorker() != null) {
                    keyworkerName = currentCase.getKeyWorker().getFullName();
                    keyworkerContact = currentCase.getKeyWorker().getContactNumber();
                }
            }
            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_CLIENTS)) {
                if (currentCase == null) {
                    viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_grey));
                } else {
                    if (client.isBirthday()) {
                        viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_birthday_cake));
                    } else {
                        // Icon colour may be shown (from current Case record
                        switch (currentCase.getClientStatus()) {
                            case Case.RED:
                                viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red));
                                break;
                            case Case.AMBER:
                                viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber));
                                break;
                            case Case.GREEN:
                                viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green));
                                break;
                        }
                    }
                }
                switch (sortMode) {
                    case FIRST_LAST:
                    case LAST_FIRST:

                        //case STATUS:
                        if (currentCase == null) {
                            additionalText = "New case";
                        } else {
                            additionalText = String.format("%s - %s",
                                    tier,
                                    group);
                        }
                        break;
                    case AGE:
                        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
                        additionalText = String.format(Locale.UK, "Date of Birth: %s (%d)",
                                sDate.format(client.getDateOfBirth()), client.getAge());
                        break;
                        /*
                        case GROUP:
                            additionalText = String.format("%s , Last entry: %s",
                                    group,
                                    client.lastEntry());
                            break;
                        case KEYWORKER:
                            additionalText = String.format("%s , Last entry: %s",
                                    keyworkerName,
                                    client.lastEntry());
                            break;
                            */
                }
                    /*
                    switch (selectMode) {
                        case OVERDUE:
                            // Calculate overdue days
                            Date now = new Date();
                            long threshold = (now.getTime() / 84600000) - currentCase.getOverdueThreshold();
                            long overdue = threshold - (client.getLatestDocument().getTime() / 84600000);
                            if (overdue > 0) {
                                color = ContextCompat.getColor(convertView.getContext(), R.color.red);
                                additionalText = String.format(Locale.UK,
                                        "%s , Update overdue: %d days", keyworkerName, overdue);
                            }
                            // Else only overdue clients are displayed
                            break;
                        default:
                            // Use the Sort text
                    }
                    */

            } else {
                viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_all));
                if (currentCase == null) {
                    additionalText = "New case";
                } else {
                    additionalText = String.format("%s: %s", localSettings.Keyworker, keyworkerName);
                    if (!keyworkerContact.isEmpty()) {
                        additionalText += String.format(" (%s)", keyworkerContact);
                    }
                }
            }
            viewItemAdditionalText.setText(additionalText);
            viewItemAdditionalText.setTextColor(color);

            return convertView;
        }
    }

    // Build 181 - Load Adapter in a backrgound thread
    // There are two load tasks one for the Register and one for All Clients
    private class LoadRegisterAdapter extends AsyncTask<Void, String, String> {

        private ArrayList<RegisterEntry> tempAdapterList;

        private int attendees;
        private int reserves;
        // Build 186 - Implement search in Register view
        private int hidden;

        @Override
        protected String doInBackground(Void... params) {
            LocalDB localDB = LocalDB.getInstance();
            attendees = 0;
            // Build 186 - Implement search in Register view
            hidden = 0;
            // Build 110 - Add Reserved option
            reserves = 0;
            try {
                tempAdapterList = new ArrayList<>();
                ArrayList<ClientSession> clientSessions = localDB.getAllClientSessions(session);

                for (ClientSession clientSession : clientSessions) {
                    // Build 240 add client to enable other 'selects'
                    Client client = clientSession.getClient();
                    // Build 186 Implement search in the register view
                    if (client.search(searchText)) {
                        // Build 218 Some users have seen crash here due t0 getActivity() returning null
                        // Replace with requireActivity which raises IllegalStateException which is
                        // trapped in LoadAdapter background task which then exits
                        //switch (((ListActivity) getActivity()).getSelectMode()) {

                        switch (((ListActivity) requireActivity()).getSelectMode()) {
                            case ALL:
                                tempAdapterList.add(new RegisterEntry(clientSession));
                                // Build 240 Move to after case statement
                                //if (clientSession.isAttended()) attendees++;
                                //if (clientSession.isReserved()) {
                                //    reserves++;
                                //}
                                break;
                            case UNCANCELLED:
                                if (!clientSession.getCancelledFlag()) {
                                    tempAdapterList.add(new RegisterEntry(clientSession));
                                    // Build 240 Move to after case statement
                                    //if (clientSession.isAttended()) attendees++;
                                    //if (clientSession.isReserved()) {
                                    //    reserves++;
                                    //}
                                }
                                break;
                            // Build 240 Add the 'select' cases which were available in the menu
                            // but it was assumed would not be used in the RegisterAdapter case
                            case GROUPS:
                                boolean hide = true;
                                UUID groupID = null;
                                if (client.getCurrentCase() != null) {
                                    Case currentCase = client.getCurrentCase();
                                    if (!currentCase.getCaseType().equals("Close")) {
                                        if (currentCase.getGroupID() != null) {
                                            groupID = currentCase.getGroupID();
                                            if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(groupID)) {
                                                hide = false;
                                            }
                                        }
                                        if (currentCase.getGroup2ID() != null) {
                                            groupID = currentCase.getGroup2ID();
                                            if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(groupID)) {
                                                hide = false;
                                            }
                                        }
                                    }
                                }
                                if (hide) {
                                    hidden++;
                                } else {
                                    tempAdapterList.add(new RegisterEntry(clientSession));
                                }
                                break;

                            case KEYWORKERS:
                                hide = true;
                                if (client.getCurrentCase() != null) {
                                    Case currentCase = client.getCurrentCase();
                                    if (!currentCase.getCaseType().equals("Close")) {
                                        if (currentCase.getKeyWorkerID() != null) {
                                            UUID keyworkerID = currentCase.getKeyWorkerID();
                                            if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(keyworkerID)) {
                                                hide = false;
                                            }
                                        }
                                    }
                                }
                                if (hide) {
                                    hidden++;
                                } else {
                                    tempAdapterList.add(new RegisterEntry(clientSession));
                                }
                                break;
                            case COMMISSIONERS:
                                hide = true;
                                if (client.getCurrentCase() != null) {
                                    Case currentCase = client.getCurrentCase();
                                    if (!currentCase.getCaseType().equals("Close")) {
                                        if (currentCase.getCommissionerID() != null) {
                                            UUID commissionerID = currentCase.getCommissionerID();
                                            if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(commissionerID)) {
                                                hide = false;
                                            }
                                        }
                                    }
                                }
                                if (hide) {
                                    hidden++;
                                } else {
                                    tempAdapterList.add(new RegisterEntry(clientSession));
                                }
                                break;
                            //Build 110 Added School, Agency
                            case SCHOOLS:
                                hide = true;
                                if (client.getCurrentSchoolID() != null) {
                                    Contact contactDocument = client.getCurrentSchool();
                                    if (contactDocument != null) {
                                        // Build 162 - This fixes a very odd bug where SchoolID was null in a
                                        // Contact Document attached via client.getCurrentSchoolID
                                        UUID schoolID = contactDocument.getSchoolID();
                                        if (schoolID != null) {
                                            // Build 136 - Only show school with end date later than today
                                            Date now = new Date();
                                            if (contactDocument.getEndDate() == null ||
                                                    contactDocument.getEndDate().getTime() == Long.MIN_VALUE ||
                                                    contactDocument.getEndDate().after(now)) {
                                                if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(schoolID)) {
                                                    hide = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (hide) {
                                    hidden++;
                                } else {
                                    tempAdapterList.add(new RegisterEntry(clientSession));
                                }
                                break;
                            case AGENCIES:
                                hide = true;
                                if (client.getCurrentAgencyID() != null) {
                                    Contact contactDocument = client.getCurrentAgency();
                                    if (contactDocument != null) {
                                        // Build 162 - This fixes a very odd bug where AgencyID was null in a
                                        // Contact Document attached via client.getCurrentAgencyID
                                        UUID agencyID = contactDocument.getAgencyID();
                                        if (agencyID != null) {
                                            // Build 136 - Only show agency with end date later than today
                                            Date now = new Date();
                                            if (contactDocument.getEndDate() == null ||
                                                    contactDocument.getEndDate().getTime() == Long.MIN_VALUE ||
                                                    contactDocument.getEndDate().after(now)) {
                                                if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(agencyID)) {
                                                    hide = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (hide) {
                                    hidden++;
                                } else {
                                    tempAdapterList.add(new RegisterEntry(clientSession));
                                }
                                break;
                        }
                    } else {
                        hidden++;
                    }
                    // Build 240 End of changes
                }
                // Build 240 Only count the displayed clients for attendance
                for (RegisterEntry registerEntry : tempAdapterList){
                    if (registerEntry.getClientSession().isAttended()) attendees++;
                    else {
                        if (registerEntry.getClientSession().isReserved()) {
                            reserves++;
                        }
                    }
                }

                // Sort the documents
                switch (sortMode) {
                    case FIRST_LAST:
                        Collections.sort(tempAdapterList, comparatorREFirstLast);
                        break;
                    case LAST_FIRST:
                        Collections.sort(tempAdapterList, comparatorRELastFirst);
                        break;
                    case ATTENDED:
                        Collections.sort(tempAdapterList, comparatorREAttended);
                        break;
                    case AGE:
                        Collections.sort(tempAdapterList, comparatorREAge);
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
            if (registerAdapter != null) {
                registerAdapter.clear();
                registerAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onPostExecute(String output) {
            // Runs on UI Thread
            // Reload the adapter list
            try {
                registerAdapterList = new ArrayList<>();
                // Build 184 A System Error is being caused by a bad clientAdapterList it is
                // possible that addAll() has a problem with empty lists
                if (tempAdapterList.size() > 0) {
                    registerAdapterList.addAll(tempAdapterList);
                }
                // Build 218 Some users have seen crash here due t0 getActivity() returning null
                // Replace with requireActivity which raises IllegalStateException which is
                // trapped in LoadAdapter background task which then exits
                //registerAdapter = new RegisterEntryAdapter(getActivity(), registerAdapterList);
                registerAdapter = new RegisterEntryAdapter(requireActivity(), registerAdapterList);
                listView.setAdapter(registerAdapter);
                registerAdapter.notifyDataSetChanged();
                // Report the number of documents in the footer.
                String footerText;
                if (session.getReferenceDate().before(new Date())) {
                    footerText = String.format(Locale.UK, "Attended: %d, DNA: %d, Reserves: %d", attendees, registerAdapterList.size() - attendees - reserves, reserves);
                } else {
                    footerText = String.format(Locale.UK, "Invited: %d, Reserves: %d", registerAdapterList.size() - reserves, reserves);
                }
                if (hidden > 0) {
                    footerText += String.format(Locale.UK, ", Hidden: %d", hidden);
                }
                Date endTime = new Date();
                long elapsed = (endTime.getTime() - startTime.getTime()) / 1000;
                //long elapsed = (endTime.getTime() - startTime.getTime()) / 100;
                if (elapsed > 0) {

                    footer.setText(String.format("%s (%d sec)", footerText, elapsed));
                } else {
                    footer.setText(footerText);
                }
            } catch (IllegalStateException ex) {
                //Build 218 If the user uses back arrow to abandon the fragment, calls to
                // requireActivity() can raise this exception. Load may simply be abandoned
                // since fragment doesn't exist.
            }
        }
    }

    // Build 186 - Optimised client search to make loading of ad-hoc groups easier. Entries are
    // removed from the clientAdapterList and stored temporarily then returned when the search
    // is closed (or repeated)
    private class LoadAllClientsAdapter extends AsyncTask<Void, String, String> {

        int hidden = 0;
        int displayed = 0;

        @Override
        protected String doInBackground(Void... params) {
            LocalDB localDB = LocalDB.getInstance();

            // Background thread works on two background lists and only loads the clientAdapterList
            // in  the post execute to prevent partial results being shown
            try {
                // Move the hidden clients back in case of a previous search/select
                displayedClientList.addAll(hiddenClientList);
                hiddenClientList.clear();

                // Load the client list if it is the first time
                if (displayedClientList.size() == 0) {
                    // Load the clients from the database
                    ArrayList<Client> clientList = localDB.getAllClients();
                    for (Client client : clientList) {
                        client.setLatestDocument(localDB.getLatestDocument(client));
                        displayedClientList.add(new ClientEntry(client));
                    }
                    setInvitedReservedFlags();
                }

                // Build 200 - Add the set of select options
                for (ClientEntry clientEntry : displayedClientList) {
                    // Always display the invited/reserved clients
                    if (!clientEntry.isInvited() && !clientEntry.isReserved()) {
                        Client client = clientEntry.getClient();
                        // Build 218 Some users have seen crash here due t0 getActivity() returning null
                        // Replace with requireActivity which raises IllegalStateException which is
                        // trapped in LoadAdapter background task which then exits
                        //switch (((ListActivity) getActivity()).getSelectMode()) {
                        switch (((ListActivity) requireActivity()).getSelectMode()) {
                            case ALL:
                                break;

                            case GROUPS:
                                boolean hide = true;
                                UUID groupID = null;
                                if (client.getCurrentCase() != null) {
                                    Case currentCase = client.getCurrentCase();
                                    if (!currentCase.getCaseType().equals("Close")) {
                                        if (currentCase.getGroupID() != null) {
                                            groupID = currentCase.getGroupID();
                                            if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(groupID)) {
                                                hide = false;
                                            }
                                        }
                                        if (currentCase.getGroup2ID() != null) {
                                            groupID = currentCase.getGroup2ID();
                                            if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(groupID)) {
                                                hide = false;
                                            }
                                        }
                                    }
                                }
                                if (hide) {
                                    hiddenClientList.add(clientEntry);
                                }
                                break;

                            case KEYWORKERS:
                                hide = true;
                                if (client.getCurrentCase() != null) {
                                    Case currentCase = client.getCurrentCase();
                                    if (!currentCase.getCaseType().equals("Close")) {
                                        if (currentCase.getKeyWorkerID() != null) {
                                            UUID keyworkerID = currentCase.getKeyWorkerID();
                                            if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(keyworkerID)) {
                                                hide = false;
                                            }
                                        }
                                    }
                                }
                                if (hide) {
                                    hiddenClientList.add(clientEntry);
                                }
                                break;
                            case COMMISSIONERS:
                                hide = true;
                                if (client.getCurrentCase() != null) {
                                    Case currentCase = client.getCurrentCase();
                                    if (!currentCase.getCaseType().equals("Close")) {
                                        if (currentCase.getCommissionerID() != null) {
                                            UUID commissionerID = currentCase.getCommissionerID();
                                            if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(commissionerID)) {
                                                hide = false;
                                            }
                                        }
                                    }
                                }
                                if (hide) {
                                    hiddenClientList.add(clientEntry);
                                }
                                break;
                            //Build 110 Added School, Agency
                            case SCHOOLS:
                                hide = true;
                                if (client.getCurrentSchoolID() != null) {
                                    Contact contactDocument = client.getCurrentSchool();
                                    if (contactDocument != null) {
                                        // Build 162 - This fixes a very odd bug where SchoolID was null in a
                                        // Contact Document attached via client.getCurrentSchoolID
                                        UUID schoolID = contactDocument.getSchoolID();
                                        if (schoolID != null) {
                                            // Build 136 - Only show school with end date later than today
                                            Date now = new Date();
                                            if (contactDocument.getEndDate() == null ||
                                                    contactDocument.getEndDate().getTime() == Long.MIN_VALUE ||
                                                    contactDocument.getEndDate().after(now)) {
                                                if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(schoolID)) {
                                                    hide = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (hide) {
                                    hiddenClientList.add(clientEntry);
                                }
                                break;
                            case AGENCIES:
                                hide = true;
                                if (client.getCurrentAgencyID() != null) {
                                    Contact contactDocument = client.getCurrentAgency();
                                    if (contactDocument != null) {
                                        // Build 162 - This fixes a very odd bug where AgencyID was null in a
                                        // Contact Document attached via client.getCurrentAgencyID
                                        UUID agencyID = contactDocument.getAgencyID();
                                        if (agencyID != null) {
                                            // Build 136 - Only show agency with end date later than today
                                            Date now = new Date();
                                            if (contactDocument.getEndDate() == null ||
                                                    contactDocument.getEndDate().getTime() == Long.MIN_VALUE ||
                                                    contactDocument.getEndDate().after(now)) {
                                                if (((ListSessionClients) requireActivity()).getSelectedIDs().contains(agencyID)) {
                                                    hide = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (hide) {
                                    hiddenClientList.add(clientEntry);
                                }
                                break;
                        }
                    }
                }
                // Now remove the hidden clients from the displayed list.
                // Note: Cannot do this in the above loop
                for (ClientEntry clientEntry : hiddenClientList) {
                    displayedClientList.remove(clientEntry);
                }

                // Move entries from displayedList to hiddenList if not in search criteria
                if (searchText.length() > 0) {
                    // Note: Can't modify clientAdapterList whilst looping through it so add to
                    // stored on this pass and remove from clientAdapterlist on next pass
                    for (ClientEntry clientEntry : displayedClientList) {
                        if (!clientEntry.getClient().search(searchText)) {
                            hiddenClientList.add(clientEntry);
                        }
                    }
                    for (ClientEntry clientEntry : hiddenClientList) {
                        displayedClientList.remove(clientEntry);
                    }
                }
                displayed = displayedClientList.size();
                hidden = hiddenClientList.size();
                // Now sort the clientAdapterList
                switch (sortMode) {
                    case FIRST_LAST:
                        Collections.sort(displayedClientList, comparatorCEFirstLast);
                        break;
                    case LAST_FIRST:
                        Collections.sort(displayedClientList, comparatorCELastFirst);
                        break;
                    case AGE:
                        Collections.sort(displayedClientList, comparatorCEAge);
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

            // Clear the adapter to show reload has started
            if (clientAdapterList == null) {
                clientAdapterList = new ArrayList<>();
                displayedClientList = new ArrayList<>();
                hiddenClientList = new ArrayList<>();
            }
            // Link the client adapter to handle switch to client view
            clientAdapterList.clear();
            clientAdapter = new ClientEntryAdapter(getActivity(), clientAdapterList);
            listView.setAdapter(clientAdapter);
            clientAdapter.notifyDataSetChanged();
            footer.setText("loading...");
        }

        @Override
        protected void onPostExecute(String output) {
            // Runs on UI Thread
            // Load the actual client list
            clientAdapterList.addAll(displayedClientList);
            // Display the footer
            Date endTime = new Date();
            long elapsed = (endTime.getTime() - startTime.getTime()) / 1000;
            if (hidden == 0) {
                footer.setText(String.format(Locale.UK, "All Clients Displayed. (%d sec)", elapsed));
            } else {
                footer.setText(String.format(Locale.UK, "Displayed: %d, Hidden: %d. (%d sec)", displayed, hidden, elapsed));
            }
            clientAdapter.notifyDataSetChanged();
        }

        private void setInvitedReservedFlags() {
            for (RegisterEntry registerEntry : registerAdapterList) {
                UUID clientID = registerEntry.getClientSession().getClientID();
                // Build 181
                //for (ClientEntry clientEntry : clientAdapterList) {
                for (ClientEntry clientEntry : displayedClientList) {
                    if (clientEntry.getClient().getClientID().equals(clientID)) {
                        clientEntry.setClientSession(registerEntry.getClientSession());
                        if (registerEntry.getClientSession().getCancelledFlag()) {
                            clientEntry.setInvited(false);
                            clientEntry.setReserved(false);
                        } else {
                            if (registerEntry.getClientSession().isReserved()) {
                                clientEntry.setInvited(false);
                                clientEntry.setReserved(true);
                            } else {
                                clientEntry.setInvited(true);
                                clientEntry.setReserved(false);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}
