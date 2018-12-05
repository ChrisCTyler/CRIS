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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
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

import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.Sheet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.EditClient;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.Contact;
import solutions.cris.object.Document;
import solutions.cris.object.ListType;
import solutions.cris.object.Role;
import solutions.cris.object.User;
import solutions.cris.utils.CRISExport;
import solutions.cris.utils.LocalSettings;
import solutions.cris.utils.PickList;
import solutions.cris.utils.SheetTest;

/**
 * Copyright CRIS.Solutions 13/12/2016.
 */

public class ListClientsFragment extends Fragment {

    private ListView listView;
    private TextView footer;
    private View parent;
    private LocalDB localDB;
    private User currentUser;
    private LocalSettings localSettings;
    private SelectMode selectMode = SelectMode.OPEN;
    private SortMode sortMode = SortMode.LAST_FIRST;
    private SearchView sv;
    private String searchText = "";
    private boolean isSearchIconified = true;
    private UUID selectedID = null;
    private String selectedValue = "";
    ArrayList<Client> clientList;
    private ClientAdapter adapter;
    private String footerText;

    private Parcelable listViewState;
    private UUID oldClientRecordID;

    // Build 110 - Added School, Agency
    private enum SelectMode {ALL, OPEN, FOLLOWED, GROUP, KEYWORKER, COMMISSIONER, OVERDUE, SCHOOL, AGENCY}

    private enum SortMode {FIRST_LAST, LAST_FIRST, GROUP, KEYWORKER, STATUS, CASE_START, AGE}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.layout_list, container, false);
        footer = (TextView) getActivity().findViewById(R.id.footer);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        if (((ListClients) getActivity()).isMyClients()) {
            toolbar.setTitle(getString(R.string.app_name) + " - My Clients");
        } else {
            toolbar.setTitle(getString(R.string.app_name) + " - All Clients");
        }

        // Use local settings for 'local' labels
        localSettings = LocalSettings.getInstance(getActivity());

        localDB = LocalDB.getInstance();
        currentUser = User.getCurrentUser();

        // Initialise the list view
        this.listView = (ListView) parent.findViewById(R.id.list_view);
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                doReadClient(position);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return doEditClient(position);
            }
        });

        User currentUser = User.getCurrentUser();
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        boolean myClients = ((ListClients) getActivity()).isMyClients();
        if (!myClients &&
                currentUser.getRole().hasPrivilege(Role.PRIVILEGE_CREATE_NEW_CLIENTS)) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doNewClient();
                }
            });
        } else {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doNotFromHere();
                }
            });
        }

        // Load the clients from the database
        if (clientList == null) {
            clientList = localDB.getAllClients();
            // Load the Adapter
            loadAdapter();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only need to re-load if a client has been update/created
        // Note: get/setDocument is used rather than get/setClient for consistency
        // since EditClient is possible from both list of clients and list of documents
        // and it used getDocument to establish the client document to be edited.
        Client editClient = (Client) ((ListActivity) getActivity()).getDocument();
        if (editClient != null) {
            // Get the client record from the database
            Client updatedClient = (Client) localDB.getDocument(editClient.getDocumentID());
            // New record needs to be added to the client list and adapter re-loaded
            if (((ListActivity) getActivity()).getMode() == Document.Mode.NEW) {
                if (updatedClient != null) {     // New client was saved
                    // Add to the client list
                    clientList.add(updatedClient);
                    // Load the Adapter since display of new client needs to be checked.
                    loadAdapter();
                }
            }
            // Otherwise, check if document has been updated (recordID will be different)
            else if (!updatedClient.getRecordID().equals(oldClientRecordID)) {
                // Database record is different so update the client in the client list
                // Note: this may be direct edit, or indirect edit via ListClientHeader
                for (Client client : clientList) {
                    if (client.getDocumentID().equals(editClient.getDocumentID())) {
                        int index = clientList.indexOf(client);
                        clientList.set(index, updatedClient);
                        break;
                    }
                }
                // Load the Adapter since display of updated client needs to be checked.
                loadAdapter();
            }
        }
        // Return from Cancel needs listView to be re-attached though adapter is re-used
        if (listView.getAdapter() == null) {
            listView.setAdapter(adapter);
        }
        if (listViewState != null){
            listView.onRestoreInstanceState(listViewState);
        }
        if (footer.getText().toString().isEmpty()){
            footer.setText(footerText);
        }

        /*
        Client editClient = (Client) ((ListActivity) getActivity()).getDocument();
        if (editClient != null) {
            // Get the client record from the database
            Client updatedClient = (Client) localDB.getDocument(editClient.getDocumentID());
            if (((ListActivity) getActivity()).getMode() == Document.Mode.NEW) {
                if (updatedClient != null) {     // New client was saved
                    // Add to the client list
                    clientList.add(updatedClient);
                    // Load the Adapter since display of new client needs to be checked.
                    loadAdapter();
                }
            } else if (!editClient.getRecordID().equals(updatedClient.getRecordID())) {
                // Database record is different so update the client in the client list
                // Note: this may be direct edit, or indirect edit via ListClientHeader
                for (Client client : clientList) {
                    if (client.getDocumentID().equals(editClient.getDocumentID())) {
                        int index = clientList.indexOf(client);
                        clientList.set(index, updatedClient);
                        break;
                    }
                }
                // Load the Adapter since display of updated client needs to be checked.
                loadAdapter();
            }
        }
        // Return from Cancel needs listView to be re-attached though adapter is re-used
        if (listView.getAdapter() == null) {
            listView.setAdapter(adapter);
            if (listViewState != null){
                listView.onRestoreInstanceState(listViewState);
            }
        }
        if (footer.getText().toString().isEmpty()){
            footer.setText(footerText);
        }
        */
    }

    private void loadAdapter(){
        int hidden = 0;
        ((ListActivity) getActivity()).setClientAdapterList(new ArrayList<Client>());
        adapter = new ClientAdapter(getActivity(),((ListActivity) getActivity()).getClientAdapterList() );

        for (Client client : clientList) {
            client.setLatestDocument(localDB.getLatestDocument(client));
            if (selectClient(client)) {
                ((ListActivity) getActivity()).getClientAdapterList().add(client);
            } else {
                hidden++;
            }
        }
        switch (sortMode) {
            case FIRST_LAST:
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorFirstLast);
                break;
            case LAST_FIRST:
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorLastFirst);
                break;
            case GROUP:
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorGroup);
                break;
            case KEYWORKER:
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorKeyworker);
                break;
            case STATUS:
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorStatus);
                break;
            case CASE_START:
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorCaseStart);
                break;
            case AGE:
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorAge);
                break;
        }
        this.listView.setAdapter(adapter);
        int displayed = ((ListActivity) getActivity()).getClientAdapterList().size();
        if (displayed > 1){
            footerText = String.format(Locale.UK, "%d clients shown, ", displayed);
        } else if (displayed == 1){
            footerText = "1 client shown, ";
        } else {
            footerText = "0 clients shown, ";
        }
        if (hidden > 0) {
            footerText += String.format(Locale.UK, "%d not shown.", hidden);
        } else {
            footerText = String.format(Locale.UK, "All clients shown (%d)", displayed);
        }
        footer.setText(footerText);
    }

    private boolean selectClient(Client client) {
        boolean selected = client.search(searchText);
        // Test MyClient
        if (selected) {
            boolean myClients = ((ListClients) getActivity()).isMyClients();
            if (myClients) {
                // Check for any reason why this is my client
                selected = false;
                if (client.getCurrentCase() != null) {
                    Case currentCase = client.getCurrentCase();
                    if ((currentCase.getKeyWorker() != null) &&
                            currentCase.getKeyWorkerID().equals(currentUser.getUserID())) {
                        // I am the keyworker
                        selected = true;
                    } else if ((currentCase.getCoWorker1ID() != null) &&
                            currentCase.getCoWorker1ID().equals(currentUser.getUserID())) {
                        // I am a co-worker
                        selected = true;
                    } else if ((currentCase.getCoWorker2ID() != null) &&
                            currentCase.getCoWorker2ID().equals(currentUser.getUserID())) {
                        selected = true;
                    } else if (localDB.isFollowing(currentUser.getUserID(), client.getClientID())) {
                        selected = true;
                    }
                }
            }
        }
        // Test explicit select modes
        if (selected) {
            switch (selectMode) {
                case ALL:
                    selected = true;
                    break;
                case OPEN:
                    // New clients (pre-case) are considered open
                    if (client.getCurrentCase() != null) {
                        Case currentCase = client.getCurrentCase();
                        if (currentCase.getCaseType().equals("Close")) {
                            selected = false;
                        }
                    }
                    break;
                case FOLLOWED:
                    selected = localDB.isFollowing(currentUser.getUserID(), client.getClientID());
                    break;
                case GROUP:
                    // New clients (pre-case) have no group
                    UUID clientID = null;
                    if (client.getCurrentCase() != null) {
                        Case currentCase = client.getCurrentCase();
                        if (currentCase.getGroupID() != null &&
                                !currentCase.getCaseType().equals("Close")) {
                            clientID = currentCase.getGroupID();
                        }
                    }
                    if (clientID == null || !clientID.equals(selectedID)) {
                        selected = false;
                    }
                    break;
                case KEYWORKER:
                    // New clients (pre-case) have no group
                    clientID = null;
                    if (client.getCurrentCase() != null) {
                        Case currentCase = client.getCurrentCase();
                        if (currentCase.getKeyWorkerID() != null&&
                                !currentCase.getCaseType().equals("Close")) {
                            clientID = currentCase.getKeyWorkerID();
                        }
                    }
                    if (clientID == null || !clientID.equals(selectedID)) {
                        selected = false;
                    }
                    break;
                case COMMISSIONER:
                    // New clients (pre-case) have no group
                    clientID = null;
                    if (client.getCurrentCase() != null) {
                        Case currentCase = client.getCurrentCase();
                        if (currentCase.getCommissionerID() != null&&
                                !currentCase.getCaseType().equals("Close")) {
                            clientID = currentCase.getCommissionerID();
                        }
                    }
                    if (clientID == null || !clientID.equals(selectedID)) {
                        selected = false;
                    }
                    break;
                case OVERDUE:
                    if (client.getCurrentCase() == null) {
                        selected = false;
                    } else {
                        Case currentCase = client.getCurrentCase();
                        if (currentCase.getOverdueThreshold() > 0) {
                            Date now = new Date();
                            long threshold = (now.getTime() / 84600000) - currentCase.getOverdueThreshold();
                            long overdue = threshold - (client.getLatestDocument().getTime() / 84600000);
                            if (overdue <= 0) {
                                selected = false;
                            }
                        } else selected = false;
                    }
                    break;
                //Build 110 Added School, Agency
                case SCHOOL:
                    if (client.getCurrentSchoolID() == null) {
                        selected = false;
                    }
                    else {
                        Contact contactDocument = client.getCurrentSchool();
                        if (contactDocument == null) {
                            selected = false;
                        }
                        else {
                            if (!contactDocument.getSchoolID().equals(selectedID)){
                                selected = false;
                            }
                        }
                    }
                    break;
                case AGENCY:
                    if (client.getCurrentAgencyID() == null) {
                        selected = false;
                    }
                    else {
                        Contact contactDocument = client.getCurrentAgency();
                        if (contactDocument == null) {
                            selected = false;
                        }
                        else {
                            if (!contactDocument.getAgencyID().equals(selectedID)){
                                selected = false;
                            }
                        }
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
    private static final int MENU_SELECT_ALL_CLIENTS = Menu.FIRST + 2;
    private static final int MENU_SELECT_OPEN_CLIENTS = Menu.FIRST + 3;
    private static final int MENU_SELECT_FOLLOWED_CLIENTS = Menu.FIRST + 4;
    private static final int MENU_SELECT_OVERDUE = Menu.FIRST + 5;
    private static final int MENU_SELECT_GROUP = Menu.FIRST + 6;
    private static final int MENU_SELECT_KEYWORKER = Menu.FIRST + 7;
    private static final int MENU_SELECT_COMMISSIONER = Menu.FIRST + 8;
    //Build 110 Added School, Agency
    private static final int MENU_SELECT_SCHOOL = Menu.FIRST + 9;
    private static final int MENU_SELECT_AGENCY = Menu.FIRST + 10;
    private static final int MENU_SORT_FIRST_LAST_NAME = Menu.FIRST + 11;
    private static final int MENU_SORT_LAST_FIRST_NAME = Menu.FIRST + 12;
    private static final int MENU_SORT_CASE_START = Menu.FIRST + 13;
    private static final int MENU_SORT_AGE = Menu.FIRST + 14;
    private static final int MENU_SORT_GROUP = Menu.FIRST + 15;
    private static final int MENU_SORT_KEYWORKER = Menu.FIRST + 16;
    private static final int MENU_SORT_STATUS = Menu.FIRST + 17;


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance(getActivity());

        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ALLOW_EXPORT)) {
            MenuItem selectExport = menu.add(0, MENU_EXPORT, 1, "Export to Google Sheets");
            selectExport.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            //selectExport.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_export));
        }

        MenuItem selectAllOption = menu.add(0, MENU_SELECT_ALL_CLIENTS, 4, "Show All Clients");
        selectAllOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem selectOpenOption = menu.add(0, MENU_SELECT_OPEN_CLIENTS, 5, "Show Open Clients");
        selectOpenOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem selectFollowedOption = menu.add(0, MENU_SELECT_FOLLOWED_CLIENTS, 6, "Show Clients I'm Following");
        selectFollowedOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem sortLastFirstOption = menu.add(0, MENU_SORT_LAST_FIRST_NAME, 20, "Sort by Last Name");
        sortLastFirstOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem sortFirstLastOption = menu.add(0, MENU_SORT_FIRST_LAST_NAME, 21, "Sort by First Name");
        sortFirstLastOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem sortCaseStartOption = menu.add(0, MENU_SORT_CASE_START, 22, "Sort by Case Start Date");
        sortCaseStartOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem sortAgeOption = menu.add(0, MENU_SORT_AGE, 23, "Sort by Date of Birth");
        sortAgeOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        boolean myClients = ((ListClients) getActivity()).isMyClients();
        if (myClients || currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_CLIENTS)) {
            MenuItem selectOverdueOption = menu.add(0, MENU_SELECT_OVERDUE, 7, "Show Clients Overdue for Update");
            selectOverdueOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            MenuItem selectGroupOption = menu.add(0, MENU_SELECT_GROUP, 8, "Show One " + localSettings.Group);
            selectGroupOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            MenuItem selectKeyworkerOption = menu.add(0, MENU_SELECT_KEYWORKER, 9, "Show One " + localSettings.Keyworker);
            selectKeyworkerOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            MenuItem selectCommissionerOption = menu.add(0, MENU_SELECT_COMMISSIONER, 10, "Show One " + localSettings.Commisioner);
            selectCommissionerOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            //Build 110 Added School, Agency
            MenuItem selectSchoolOption = menu.add(0, MENU_SELECT_SCHOOL, 11, "Show One School");
            selectSchoolOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            MenuItem selectAgencyOption = menu.add(0, MENU_SELECT_AGENCY, 12, "Show One Agency");
            selectAgencyOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            MenuItem sortGroupOption = menu.add(0, MENU_SORT_GROUP, 24, "Sort by " + localSettings.Group);
            sortGroupOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            MenuItem sortKeyworkerOption = menu.add(0, MENU_SORT_KEYWORKER, 25, "Sort by " + localSettings.Keyworker);
            sortKeyworkerOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            MenuItem sortStatusOption = menu.add(0, MENU_SORT_STATUS, 26, "Sort by Status");
            sortStatusOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        ActionBar supportActionBar = ((ListClients) getActivity()).getSupportActionBar();
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
                    loadAdapter();
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
                    loadAdapter();
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
        switch (item.getItemId()) {
            case MENU_EXPORT:
                if (((ListClients) getActivity()).isMyClients()) {
                    ((ListActivity) getActivity()).setExportListType("My Clients");
                } else {
                    ((ListActivity) getActivity()).setExportListType("All Clients");
                }
                switch (selectMode){
                    case ALL:
                        ((ListActivity) getActivity()).setExportSelection("All Clients (inc. closed cases)");
                        break;
                    case OPEN:
                        ((ListActivity) getActivity()).setExportSelection("All Open Cases");
                        break;
                    case FOLLOWED:
                        ((ListActivity) getActivity()).setExportSelection("Clients I'm Following");
                        break;
                    case OVERDUE:
                        ((ListActivity) getActivity()).setExportSelection("Clients Overdue for Update");
                        break;
                    case GROUP:
                        ((ListActivity) getActivity()).setExportSelection(String.format("%s: %s", localSettings.Group, selectedValue));
                        break;
                    case KEYWORKER:
                        ((ListActivity) getActivity()).setExportSelection(String.format("%s: %s", localSettings.Keyworker, selectedValue));
                        break;
                    case COMMISSIONER:
                        ((ListActivity) getActivity()).setExportSelection(String.format("%s: %s", localSettings.Commisioner, selectedValue));
                        break;
                    default:
                        ((ListActivity) getActivity()).setExportSelection(String.format("%s: %s", selectMode.toString(), selectedValue));
                }
                switch (sortMode){
                    case FIRST_LAST:
                        ((ListActivity) getActivity()).setExportSort("FirstNames/Lastname");
                        break;
                    case LAST_FIRST:
                        ((ListActivity) getActivity()).setExportSort("LastName/FirstNames");
                        break;
                    case CASE_START:
                        ((ListActivity) getActivity()).setExportSort("Case Start Date");
                        break;
                    case AGE:
                        ((ListActivity) getActivity()).setExportSort("Date of Birth");
                        break;
                    case GROUP:
                        ((ListActivity) getActivity()).setExportSort(localSettings.Group);
                        break;
                    case KEYWORKER:
                        ((ListActivity) getActivity()).setExportSort(localSettings.Keyworker);
                        break;
                    case STATUS:
                        ((ListActivity) getActivity()).setExportSort("Status");
                }
                if (searchText.isEmpty()){
                    ((ListActivity) getActivity()).setExportSearch("No Search Used");
                } else {
                    ((ListActivity) getActivity()).setExportSearch(searchText);
                }
                listViewState = listView.onSaveInstanceState();
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment fragment = new CRISExport();
                fragmentTransaction.replace(R.id.content, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                return true;

            case MENU_SELECT_ALL_CLIENTS:
                selectMode = SelectMode.ALL;
                loadAdapter();
                return true;

            case MENU_SELECT_OPEN_CLIENTS:
                selectMode = SelectMode.OPEN;
                loadAdapter();
                return true;

            case MENU_SELECT_FOLLOWED_CLIENTS:
                selectMode = SelectMode.FOLLOWED;
                loadAdapter();
                return true;

            case MENU_SELECT_OVERDUE:
                selectMode = SelectMode.OVERDUE;
                loadAdapter();
                return true;

            case MENU_SELECT_GROUP:
                selectGroup();
                return true;

            case MENU_SELECT_KEYWORKER:
                selectKeyWorker();
                return true;

            case MENU_SELECT_COMMISSIONER:
                selectCommissioner();
                return true;

            //Build 110 Added School, Agency
            case MENU_SELECT_SCHOOL:
                selectSchool();
                return true;

            case MENU_SELECT_AGENCY:
                selectAgency();
                return true;

            case MENU_SORT_FIRST_LAST_NAME:
                sortMode = SortMode.FIRST_LAST;
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorFirstLast);
                adapter.notifyDataSetChanged();
                return true;

            case MENU_SORT_LAST_FIRST_NAME:
                sortMode = SortMode.LAST_FIRST;
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorLastFirst);
                adapter.notifyDataSetChanged();
                return true;

            case MENU_SORT_CASE_START:
                sortMode = SortMode.CASE_START;
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorCaseStart);
                adapter.notifyDataSetChanged();
                return true;

            case MENU_SORT_AGE:
                sortMode = SortMode.AGE;
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorAge);
                adapter.notifyDataSetChanged();
                return true;

            case MENU_SORT_GROUP:
                sortMode = SortMode.GROUP;
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorGroup);
                adapter.notifyDataSetChanged();
                return true;

            case MENU_SORT_KEYWORKER:
                sortMode = SortMode.KEYWORKER;
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorKeyworker);
                adapter.notifyDataSetChanged();
                return true;

            case MENU_SORT_STATUS:
                sortMode = SortMode.STATUS;
                Collections.sort(((ListActivity) getActivity()).getClientAdapterList(), Client.comparatorStatus);
                adapter.notifyDataSetChanged();
                return true;

            default:
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

    private void doNoClient() {
        new AlertDialog.Builder(getActivity())
                .setTitle("How did you do it?")
                .setMessage("Hi, Chris here. You have managed to trigger an intermittent bug which has occurred " +
                        "a couple of times but I can't re-create. Can you email me with some details which might " +
                        "help, such as the client you were trying to access, any special menu options/search, " +
                        "is it repeatable and anything you did differently to normal which might help me re-create " +
                        "it and then fix it. Ta.")
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void selectGroup() {
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance(getActivity());
        final PickList groups = new PickList(localDB, ListType.GROUP, 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Show clients from the following :" + localSettings.Group);
        ArrayList<String> itemList = groups.getOptions();
        String[] items = itemList.toArray(new String[itemList.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedID = groups.getListItems().get(which).getListItemID();
                selectedValue = groups.getListItems().get(which).getItemValue();
                selectMode = SelectMode.GROUP;
                loadAdapter();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void selectCommissioner() {
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance(getActivity());
        final PickList groups = new PickList(localDB, ListType.COMMISSIONER, 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Show clients for the following :" + localSettings.Commisioner);
        ArrayList<String> itemList = groups.getOptions();
        String[] items = itemList.toArray(new String[itemList.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedID = groups.getListItems().get(which).getListItemID();
                selectedValue = groups.getListItems().get(which).getItemValue();
                selectMode = SelectMode.COMMISSIONER;
                loadAdapter();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Build 110 - Added Show One School
    private void selectSchool() {
        // Use local settings for 'local' labels
        final PickList schools = new PickList(localDB, ListType.SCHOOL, 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Show clients for the following School:");
        ArrayList<String> itemList = schools.getOptions();
        String[] items = itemList.toArray(new String[itemList.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedID = schools.getListItems().get(which).getListItemID();
                selectedValue = schools.getListItems().get(which).getItemValue();
                selectMode = SelectMode.SCHOOL;
                loadAdapter();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Build 110 - Added Show One Agency
    private void selectAgency() {
        // Use local settings for 'local' labels
        final PickList agencies = new PickList(localDB, ListType.AGENCY, 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Show clients for the following Agency:");
        ArrayList<String> itemList = agencies.getOptions();
        String[] items = itemList.toArray(new String[itemList.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedID = agencies.getListItems().get(which).getListItemID();
                selectedValue = agencies.getListItems().get(which).getItemValue();
                selectMode = SelectMode.AGENCY;
                loadAdapter();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void selectKeyWorker() {
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance(getActivity());

        // Get a list of keyworkers
        ArrayList<User> users = localDB.getAllUsers();
        ArrayList<User> keyworkers = new ArrayList<>();
        for (User user : users) {
            if (user.getRole().hasPrivilege(Role.PRIVILEGE_USER_IS_KEYWORKER)) {
                keyworkers.add(user);
            }
        }

        final PickList keyworkersPickList = new PickList(keyworkers);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Show clients for the following :" + localSettings.Keyworker);
        ArrayList<String> itemList = keyworkersPickList.getOptions();
        String[] items = itemList.toArray(new String[itemList.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedID = keyworkersPickList.getUsers().get(which).getUserID();
                selectedValue = keyworkersPickList.getUsers().get(which).getFullName();
                selectMode = SelectMode.KEYWORKER;
                loadAdapter();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void doReadClient(int position) {
        boolean myClients = ((ListClients) getActivity()).isMyClients();
        if ((myClients && currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_MY_CLIENTS)) ||
                (!myClients && currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_CLIENTS))) {
            // 3/3/2017 Odd crash (Graeme Edwards) seems to have returned null so test
            if (adapter.getItem(position) != null) {
                // V2.0 Set the 'document' in case the client is modified in the document view
                // Note: get/setDocument is used rather than get/setClient for consistency
                // since EditClient is possible from both list of clients and list of documents
                // and it used getDocument to establish the client document to be edited.
                Client client = adapter.getItem(position);
                ((ListActivity) getActivity()).setDocument(client);
                listViewState = listView.onSaveInstanceState();
                // Save this recordID to enable check for change to client
                oldClientRecordID = client.getRecordID();
                Intent intent = new Intent(getActivity(), ListClientHeader.class);
                // Client is serializable so can pass as extra to List Activity
                intent.putExtra(Main.EXTRA_DOCUMENT, client);
                startActivity(intent);
            }
            else {
                doNoClient();
            }
        } else {
            doNoPrivilege();
        }
    }

    private boolean doEditClient(int position) {
        boolean myClients = ((ListClients) getActivity()).isMyClients();
        if ((myClients && currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_MY_CLIENTS)) ||
                (!myClients && currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_ALL_CLIENTS))) {
            ((ListActivity) getActivity()).setMode(Document.Mode.EDIT);
            // 3/3/2017 Odd crash (Graeme Edwards) seems to have returned null so test
            if (adapter.getItem(position) != null) {
                // Note: get/setDocument is used rather than get/setClient for consistency
                // since EditClient is possible from both list of clients and list of documents
                // and it used getDocument to establish the client document to be edited.
                Client client = adapter.getItem(position);
                ((ListActivity) getActivity()).setDocument(client);
                listViewState = listView.onSaveInstanceState();
                // Save this recordID to enable check for change to client
                oldClientRecordID = client.getRecordID();
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment fragment = new EditClient();
                fragmentTransaction.replace(R.id.content, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
            else {
                doNoClient();
            }
        } else {
            doNoPrivilege();
        }

        return true;
    }

    private boolean doNewClient() {
        ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
        ((ListActivity) getActivity()).setDocument(new Client(currentUser));
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment fragment = new EditClient();
        fragmentTransaction.replace(R.id.content, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        return true;
    }

    private void doNotFromHere() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Create Clients From 'All Client'")
                .setMessage("New clients may only be created from the 'All Clients' " +
                        "view. \n\nThis helps to reduce the creation of duplicates " +
                        "by enabling you to check the full list of existing clients " +
                        "before creating a new record.")
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private class ClientAdapter extends ArrayAdapter<Client> {

        final private Drawable clientRed = ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red);
        final private Drawable clientAmber = ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber);
        final private Drawable clientGreen = ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green);
        final private Drawable clientGrey = ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_grey);
        final private Drawable clientAll = ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_all);
        final private int textGrey = ContextCompat.getColor(getActivity(), R.color.text_grey);
        final  private int textRed = ContextCompat.getColor(getActivity(), R.color.red);

        // Constructor
        ClientAdapter(Context context, List<Client> objects) {
            super(context, 0, objects);
        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final ImageView viewItemIcon;
            final TextView viewItemDate;
            final TextView viewItemMainText;
            final TextView viewItemAdditionalText;
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.layout_list_item, parent, false);
                viewItemIcon = (ImageView) convertView.findViewById(R.id.item_icon);
                convertView.setTag(R.id.tag_view_item_icon, viewItemIcon);
                viewItemDate = (TextView) convertView.findViewById(R.id.item_date);
                convertView.setTag(R.id.tag_view_item_date, viewItemDate);
                viewItemMainText = (TextView) convertView.findViewById(R.id.item_main_text);
                convertView.setTag(R.id.tag_main_text, viewItemMainText);
                viewItemAdditionalText = (TextView) convertView.findViewById(R.id.item_additional_text);
                convertView.setTag(R.id.tag_additional_text, viewItemAdditionalText);
            } else {
                viewItemIcon = (ImageView) convertView.getTag(R.id.tag_view_item_icon);
                viewItemDate = (TextView) convertView.getTag(R.id.tag_view_item_date);
                viewItemMainText = (TextView) convertView.getTag(R.id.tag_main_text);
                viewItemAdditionalText = (TextView) convertView.getTag(R.id.tag_additional_text);
            }
            final Client client = ((ListActivity) getActivity()).getClientAdapterList().get(position);

            // Display the client's name
            viewItemMainText.setText(client.getFullName());

            // Display the DOB and calculate/display age (and set the birthday flag as a by-product)
            SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
            viewItemDate.setText(String.format(Locale.UK,
                    "%s (%d)", sDate.format(client.getDateOfBirth()), client.getAge()));

            // Rest of information displayed depends on privilege and sort type
            // (privilege takes preference).
            boolean myClients = ((ListClients) getActivity()).isMyClients();
            int color = textGrey;
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
                if (currentCase.getKeyWorker() != null) {
                    keyworkerName = currentCase.getKeyWorker().getFullName();
                    keyworkerContact = currentCase.getKeyWorker().getContactNumber();
                }
            }
            if ((myClients && currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_MY_CLIENTS)) ||
                    (!myClients && currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_CLIENTS))) {
                if (currentCase == null) {
                    viewItemIcon.setImageDrawable(clientGrey);
                } else {
                    if (client.isBirthday()) {
                        viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_birthday_cake));
                    } else {
                        // Icon colour may be shown (from current Case record
                        switch (currentCase.getClientStatus()) {
                            case Case.RED:
                                viewItemIcon.setImageDrawable(clientRed);
                                break;
                            case Case.AMBER:
                                viewItemIcon.setImageDrawable(clientAmber);
                                break;
                            case Case.GREEN:
                                viewItemIcon.setImageDrawable(clientGreen);
                                break;
                        }
                    }
                }
                if (currentCase == null) {
                    additionalText = "New case";
                } else {
                    switch (sortMode) {
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
                        case CASE_START:
                            String startText = "New case.";
                            if (client.getStartCase() != null){
                                startText = "Case start: " + sDate.format(client.getStartCase().getReferenceDate());
                            }
                            additionalText = String.format("%s , %s",
                                    group,
                                    startText);
                            break;
                        default:
                            // Standard text
                            additionalText = String.format("%s - %s, Last entry: %s",
                                    tier,
                                    group,
                                    client.lastEntry());
                    }
                    switch (selectMode) {
                        case OVERDUE:
                            // Calculate overdue days
                            Date now = new Date();
                            long threshold = (now.getTime() / 84600000) - currentCase.getOverdueThreshold();
                            long overdue = threshold - (client.getLatestDocument().getTime() / 84600000);
                            if (overdue > 0) {
                                color = textRed;
                                additionalText = String.format(Locale.UK,
                                        "%s , Update overdue: %d days", keyworkerName, overdue);
                            }
                            // Else only overdue clients are displayed
                            break;
                        default:
                            // Use the Sort text
                    }
                }
            } else {
                viewItemIcon.setImageDrawable(clientAll);
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
}