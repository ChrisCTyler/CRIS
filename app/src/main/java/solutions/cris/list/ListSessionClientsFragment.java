package solutions.cris.list;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
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
import solutions.cris.object.Document;
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

    private enum SelectMode {ALL, UNCANCELLED}

    private SelectMode selectMode = SelectMode.UNCANCELLED;

    // Used to switch between show existing invitees and get new invitees
    private boolean displayAllClients = false;

    // Build 105 - Added Search
    private SearchView sv;
    private String searchText = "";
    private boolean isSearchIconified = true;

    private RegisterEntryAdapter registerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.layout_list, container, false);
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
        listView = (ListView) parent.findViewById(R.id.list_view);
        final SwipeDetector swipeDetector = new SwipeDetector();
        this.listView.setOnTouchListener(swipeDetector);
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
        // Build 105 - Search visibility
        if (searchItem != null) {
            searchItem.setVisible(false);
        }
        int attendees = 0;
        // Build 110 - Add Reserved option
        int reserves = 0;
        // Create the adapter
        registerAdapterList = new ArrayList<>();
        ArrayList<ClientSession> clientSessions = localDB.getAllClientSessions(session);
        for (ClientSession clientSession : clientSessions) {
            switch (selectMode) {
                case ALL:
                    registerAdapterList.add(new RegisterEntry(clientSession));
                    if (clientSession.isAttended()) attendees++;
                    if (clientSession.isReserved()) {
                        reserves++;
                    }
                    break;
                case UNCANCELLED:
                    if (!clientSession.getCancelledFlag()) {
                        registerAdapterList.add(new RegisterEntry(clientSession));
                        if (clientSession.isAttended()) attendees++;
                        if (clientSession.isReserved()) {
                            reserves++;
                        }
                    }
            }

        }
        // Sort the documents
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
        //RegisterEntryAdapter adapter = new RegisterEntryAdapter(getActivity(), registerAdapterList);
        registerAdapter = new RegisterEntryAdapter(getActivity(), registerAdapterList);
        this.listView.setAdapter(registerAdapter);
        // Report the number of hidden documents in the footer.
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        if (session.getReferenceDate().before(new Date())) {
            footer.setText(String.format(Locale.UK, "Attended: %d, DNA: %d, Reserves: %d", attendees, registerAdapterList.size() - attendees - reserves, reserves));
        } else {
            footer.setText(String.format(Locale.UK, "%d clients invited, %d reserves.", registerAdapterList.size() - reserves, reserves));
        }
    }

    private void onResumeAllClients() {
        // Build 105 - Search visibility
        searchItem.setVisible(true);

        int hidden = 0;
        // Create the adapter here because EditClient may have altered the eligibility for the
        // current list
        clientAdapterList = new ArrayList<>();

        // Load the clients from the database
        ArrayList<Client> clientList = localDB.getAllClients();
        for (Client client : clientList) {
            client.setLatestDocument(localDB.getLatestDocument(client));
            if (client.search(searchText)) {
                clientAdapterList.add(new ClientEntry(client));
            } else {
                hidden++;
            }
        }
        setInvitedReservedFlags();
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
        }
        ClientEntryAdapter adapter = new ClientEntryAdapter(getActivity(), clientAdapterList);
        this.listView.setAdapter(adapter);
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        int displayed = clientAdapterList.size();
        String footerText = "";
        if (displayed > 1) {
            footerText += String.format(Locale.UK, "%d clients shown, ", displayed);
        } else if (displayed == 1) {
            footerText += "1 client shown, ";
        } else {
            footerText += "0 clients shown, ";
        }
        if (hidden > 0) {
            footerText += String.format(Locale.UK, "%d not shown.", hidden);
        } else {
            footerText = String.format(Locale.UK, "All clients shown (%d)", displayed);
        }
        footer.setText(footerText);
    }

    private void setInvitedReservedFlags() {
        for (RegisterEntry registerEntry : registerAdapterList) {
            UUID clientID = registerEntry.getClientSession().getClientID();
            for (ClientEntry clientEntry : clientAdapterList) {
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

    // MENU BLOCK
    private static final int MENU_EXPORT = Menu.FIRST + 1;
    private static final int MENU_SELECT_ALL = Menu.FIRST + 2;
    private static final int MENU_SELECT_UNCANCELLED = Menu.FIRST + 3;
    private static final int MENU_SORT_FIRST_LAST = Menu.FIRST + 10;
    private static final int MENU_SORT_LAST_FIRST = Menu.FIRST + 11;
    private static final int MENU_SORT_ATTENDED = Menu.FIRST + 12;
    private static final int MENU_SORT_AGE = Menu.FIRST + 13;
    private static final int MENU_BROADCAST = Menu.FIRST + 20;

    private MenuItem searchItem;

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
        MenuItem selectAllOption = menu.add(0, MENU_SELECT_ALL, 1, "Show All Clients");
        selectAllOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        MenuItem selectUncancelledOption = menu.add(0, MENU_SELECT_UNCANCELLED, 10, "Show Uncancelled Clients");
        selectUncancelledOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

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

        // Build 105 Add Search at client level
        searchItem = menu.findItem(R.id.action_search);
        // Initial visibility is false since register is the first list displayed
        // onResumeAllClients will display the search option
        searchItem.setVisible(false);
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SimpleDateFormat sDate = new SimpleDateFormat("WWW dd/MM/yyyy", Locale.UK);
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
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment fragment = new CRISExport();
                fragmentTransaction.replace(R.id.content, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                return true;
            case MENU_SELECT_ALL:
                selectMode = SelectMode.ALL;
                onResume();
                return true;
            case MENU_SELECT_UNCANCELLED:
                selectMode = SelectMode.UNCANCELLED;
                onResume();
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
                for (RegisterEntry entry : registerAdapterList){
                    broadcastClientList.add(entry.getClientSession().getClient());
                }
                ((ListActivity)getActivity()).setBroadcastClientList(broadcastClientList);
                // Start the Broadcast fragment
                fragmentManager = getFragmentManager();
                fragmentTransaction = fragmentManager.beginTransaction();
                fragment = new BroadcastMessageFragment();
                fragmentTransaction.replace(R.id.content, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                /*
                new AlertDialog.Builder(getActivity())
                        .setTitle("Not Implemented Yet")
                        .setMessage("Unfortunately, this option is not yet available.")
                        .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                 */
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
            note = null;
            pdfDocument = null;
            statusDocument = null;
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
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction;
                Fragment fragment = new EditNote();
                fragmentTransaction = fragmentManager.beginTransaction();
                ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
                registerEntry.setNote(new Note(currentUser, clientSession.getClientID()));
                registerEntry.getNote().setSessionID(clientSession.getSessionID());
                registerEntry.getNote().setSession(clientSession.getSession());
                ((ListActivity) getActivity()).setDocument(registerEntry.getNote());
                fragmentTransaction.replace(R.id.content, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                notifyDataSetChanged();
            } else {
                // Read Note
                // Use plus sign for note in read mode (add response document)
                final FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
                fab.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_fab_plus));
                ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                ((ListActivity) getActivity()).setDocument(noteDocument);
                localDB.read(noteDocument, currentUser);
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction;
                Fragment fragment;
                fragmentTransaction = fragmentManager.beginTransaction();
                fragment = new EditNote();
                fragmentTransaction.replace(R.id.content, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        }

        private void doTransport(ClientSession clientSession, Transport transport, RegisterEntry registerEntry, boolean longPress) {
            Client client = clientSession.getClient();
            ((ListActivity) getActivity()).setClient(client);
            // Display the Client Header
            ((ListSessionClients) getActivity()).loadClientHeader(client);
            if (transport == null) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction;
                Fragment fragment = new EditTransport();
                fragmentTransaction = fragmentManager.beginTransaction();
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
                fragmentTransaction.replace(R.id.content, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                notifyDataSetChanged();
            } else {
                // Longpress to edit
                if (longPress) {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction;
                    Fragment fragment = new EditTransport();
                    fragmentTransaction = fragmentManager.beginTransaction();
                    ((ListActivity) getActivity()).setMode(Document.Mode.EDIT);
                    ((ListActivity) getActivity()).setDocument(transport);
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    notifyDataSetChanged();
                } else {
                    // Read Transport document
                    ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                    ((ListActivity) getActivity()).setDocument(transport);
                    localDB.read(transport, currentUser);
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction;
                    Fragment fragment;
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragment = new ReadTransport();
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
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
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction;
                    Fragment fragment = new EditMyWeek();
                    fragmentTransaction = fragmentManager.beginTransaction();
                    ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
                    registerEntry.setStatusDocument(new MyWeek(currentUser, clientSession.getClientID()));
                    registerEntry.getStatusDocument().setSessionID(clientSession.getSessionID());
                    registerEntry.getStatusDocument().setSession(clientSession.getSession());
                    // Status document ReferenceDate is the session date
                    registerEntry.getStatusDocument().setReferenceDate(clientSession.getReferenceDate());
                    ((ListActivity) getActivity()).setDocument(registerEntry.getStatusDocument());
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    notifyDataSetChanged();
                } else {
                    doFutureError();
                }
            } else {
                if (longPress) {
                    // Edit MyWeek
                    // Set the flag to prevent back button
                    ((ListSessionClients) getActivity()).setEditMyWeek(true);
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction;
                    Fragment fragment = new EditMyWeek();
                    fragmentTransaction = fragmentManager.beginTransaction();
                    ((ListActivity) getActivity()).setMode(Document.Mode.EDIT);
                    ((ListActivity) getActivity()).setDocument(statusDocument);
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    notifyDataSetChanged();
                } else {
                    // Read Note
                    MyWeek myWeekDocument = (MyWeek) statusDocument;
                    ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                    ((ListActivity) getActivity()).setDocument(myWeekDocument);
                    localDB.read(myWeekDocument, currentUser);
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction;
                    Fragment fragment;
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragment = new ReadMyWeek();
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
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
            ImageView noteIcon = (ImageView) convertView.findViewById(R.id.note_icon);
            noteIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doNote(clientSession, noteDocument, registerEntry);
                }
            });

            ImageView viewItemIcon = (ImageView) convertView.findViewById(R.id.item_icon);
            viewItemIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayClientRecord(clientSession);
                }
            });

            TextView viewItemMainText = (TextView) convertView.findViewById(R.id.item_main_text);
            viewItemMainText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayClientRecord(clientSession);
                }
            });

            ImageView attendanceIcon = (ImageView) convertView.findViewById(R.id.attendance_icon);
            attendanceIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleAttendance(clientSession, transportDocument);
                }
            });

            ImageView statusIcon = (ImageView) convertView.findViewById(R.id.status_icon);
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

            ImageView transportIcon = (ImageView) convertView.findViewById(R.id.transport_icon);
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

            ImageView pdfDocumentIcon = (ImageView) convertView.findViewById(R.id.pdf_icon);
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
            ImageView flagIcon = (ImageView) convertView.findViewById(R.id.flag_icon);
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
                        case NoteType.ICON_COLOUR_UNKNOWN:
                            // Pre-V1.1
                            if (noteDocument.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                                noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_blue));
                            } else {
                                noteIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_blue));
                            }
                            break;
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

            ImageView cameraIcon = (ImageView) convertView.findViewById(R.id.camera_icon);
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
        ClientEntryAdapter(Context context, List<ClientEntry> objects) {
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

            ImageView viewItemIcon = (ImageView) convertView.findViewById(R.id.item_icon);
            viewItemIcon.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    displayClientRecord(clientEntry.getClient());
                    return false;
                }
            });
            ImageView invitedIcon = (ImageView) convertView.findViewById(R.id.attendance_icon);
            invitedIcon.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    toggleAttendance(clientEntry, AttendanceMode.INVITED);
                    return false;
                }
            });
            ImageView reserveIcon = (ImageView) convertView.findViewById(R.id.reserve_icon);
            reserveIcon.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    toggleAttendance(clientEntry, AttendanceMode.RESERVED);
                    return false;
                }
            });
            TextView viewItemMainText = (TextView) convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = (TextView) convertView.findViewById(R.id.item_additional_text);

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
}
