package solutions.cris.list;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.EditCAT;
import solutions.cris.edit.EditCase;
import solutions.cris.edit.EditClient;
import solutions.cris.edit.EditContact;
import solutions.cris.edit.EditMyWeek;
import solutions.cris.edit.EditNote;
import solutions.cris.edit.EditTransport;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.Contact;
import solutions.cris.object.CriteriaAssessmentTool;
import solutions.cris.object.Document;
import solutions.cris.object.Image;
import solutions.cris.object.ListType;
import solutions.cris.object.MyWeek;
import solutions.cris.object.Note;
import solutions.cris.object.NoteType;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.Role;
import solutions.cris.object.Transport;
import solutions.cris.object.User;
import solutions.cris.read.ReadCAT;
import solutions.cris.read.ReadCase;
import solutions.cris.read.ReadClient;
import solutions.cris.read.ReadContact;
import solutions.cris.read.ReadImage;
import solutions.cris.read.ReadMyWeek;
import solutions.cris.read.ReadTransport;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.utils.CRISExport;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.PickList;
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

public class ListClientDocumentsFragment extends Fragment {

    private static final SimpleDateFormat sDateTime = new SimpleDateFormat("dd MMM HH:mm", Locale.UK);
    // MENU BLOCK
    private static final int MENU_EXPORT = Menu.FIRST + 1;
    private static final int MENU_SELECT_ALL = Menu.FIRST + 2;
    private static final int MENU_SELECT_UNCANCELLED = Menu.FIRST + 3;
    private static final int MENU_SELECT_CONTACTS = Menu.FIRST + 4;
    private static final int MENU_SELECT_TYPE = Menu.FIRST + 5;
    private static final int MENU_SORT_DATE = Menu.FIRST + 6;
    private static final int MENU_SORT_TYPE = Menu.FIRST + 7;
    private static final int MENU_FOLLOW_CLIENT = Menu.FIRST + 8;
    private static final int MENU_UNFOLLOW_CLIENT = Menu.FIRST + 9;
    private static final int MENU_SEND_MYWEEK_LINK = Menu.FIRST + 10;

    private ArrayList<Document> adapterList;
    private ListView listView;
    TextView footer;
    private View parent;
    private LocalDB localDB;
    private User currentUser;
    private Client client;
    private Menu menu;
    private MenuItem followOption;
    private SearchView sv;
    private String searchText = "";
    private String selectedDocumentType;
    private ArrayList<String> documentTypes = new ArrayList<>();

    private boolean isSearchIconified = true;
    private String[] hashTags;
    private SimpleCursorAdapter mAdapter;
    private Parcelable listViewState;

    private enum SelectMode {ALL, UNCANCELLED, CONTACT, TYPE}

    private SelectMode selectMode = SelectMode.UNCANCELLED;
    private SelectMode previousSelectMode = SelectMode.UNCANCELLED;

    private enum SortMode {TYPE, DATE}

    private SortMode sortMode = SortMode.DATE;

    private boolean currentCaseFound;
    private boolean startCaseFound;
    private boolean currentToolFound;
    private boolean currentAgencyFound;
    private boolean currentSchoolFound;
    private boolean assocDocumentChanged;
    private int transportBooked;
    private int transportUsed;
    private Date firstSession;
    private Date firstAttendedSession;
    private int sessionsOffered;
    private int sessionsAttended;
    private int sessionsCancelled;
    private int sessionsDNA;
    private DocumentAdapter adapter;
    private String footerText;

    private MenuItem shareOption;

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
        toolbar.setTitle(getString(R.string.app_name));
        client = ((ListActivity) getActivity()).getClient();
        localDB = LocalDB.getInstance();
        currentUser = User.getCurrentUser();

        // Build 125 - Tablet re-orientation not working because read mode will be set and
        // adapter will not be refreshed in onResume. Set to Edit mode to force a reload
        if (savedInstanceState != null){
            ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
        }


        // Initialise the list view
        this.listView = (ListView) parent.findViewById(R.id.list_view);
        final SwipeDetector swipeDetector = new SwipeDetector();
        this.listView.setOnTouchListener(swipeDetector);
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (swipeDetector.swipeDetected()) {
                    displayDocumentHistory(position, swipeDetector.getAction());
                } else {
                    doReadDocument(position);
                }

            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return doEditDocument(position);
            }
        });

        User currentUser = User.getCurrentUser();
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_ALL_DOCUMENTS) ||
                currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_NOTES) ||
                currentUser.getRole().hasPrivilege(Role.PRIVILEGE_CREATE_NOTES)) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doNewDocument();
                }
            });
        } else {
            // Hide New Client option
            fab.setVisibility(View.GONE);
        }

        final PickList hashTagPL = new PickList(localDB, ListType.HASHTAG, 0);
        ArrayList<String> itemList = hashTagPL.getOptions();
        hashTags = itemList.toArray(new String[itemList.size()]);
        final String[] from = new String[]{"hashTag"};
        final int[] to = new int[]{android.R.id.text1};
        mAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1,
                null,
                from,
                to,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
    }

    @Override
    public void onResume() {
        super.onResume();
        // V2.0 Optimisation. Only reload if New or Edit (not read)
        if (!((ListActivity) getActivity()).getMode().equals(Document.Mode.READ)) {
            ((ListActivity) getActivity()).setMode(Document.Mode.READ);
            // Clear until next New/Edit
            ((ListActivity) getActivity()).setDocument(null);
            int hidden = 0;
            int clientSessionPos = 4;
            // Create the adapter
            adapterList = new ArrayList<>();
            // Load the documents for the client from the database.

            ArrayList<Document> documents = loadDocuments();
            // Sort the documents into reverse chronological order (with stickiness)
            Collections.sort(documents, Document.comparatorDate);
            // Clear the list of document types
            documentTypes.clear();
            // Get a pointer to the Fragment-wide variable and clear the contents
            ClientSession[] clientSessions = ((ListClientHeader) getActivity()).getClientSessions();
            for (int i = 0; i < 5; i++) {
                clientSessions[i] = null;
            }
            // Initialise the associated documents flags
            currentCaseFound = false;
            startCaseFound = false;
            currentToolFound = false;
            currentAgencyFound = false;
            currentSchoolFound = false;
            assocDocumentChanged = false;
            transportBooked = 0;
            transportUsed = 0;
            firstSession = new Date(Long.MIN_VALUE);
            firstAttendedSession = new Date(Long.MIN_VALUE);
            sessionsOffered = 0;
            sessionsAttended = 0;
            sessionsCancelled = 0;
            sessionsDNA = 0;
            // Loop through the documents (in reverse chronological order)
            for (Document document : documents) {
                // Add document to document type list if new type
                addToDocumentTypes(document);
                // Check whether the current document is an 'associated' document
                testForAssociatedDocument(document);
                clientSessionPos = testForClientSession(clientSessionPos, document);
                if (selectDocument(document)) {
                    adapterList.add(document);
                } else {
                    hidden++;
                }
            }
            // If there'e been a change, save the client record
            checkClientChanges();
            // Sort the Document Types array
            Collections.sort(documentTypes);
            // Sort the list into Type order if necessary
            switch (sortMode) {
                case DATE:
                    // AdapterList was loaded in date order so no sort necessary
                    Collections.sort(adapterList, Document.comparatorDate);
                    break;
                case TYPE:
                    Collections.sort(adapterList, Document.comparatorType);
                    break;
            }
            adapter = new DocumentAdapter(getActivity(), adapterList);
            this.listView.setAdapter(adapter);
            // Report the number of hidden documents in the footer.

            if (hidden > 1) {
                footerText = String.format(Locale.UK, "%d documents are not shown.", hidden);
            } else if (hidden == 1) {
                footerText = String.format(Locale.UK, "%d document is not shown.", hidden);
            } else {
                footerText = String.format("%s", getString(R.string.info_all_documents_shown));
            }
            footer.setText(footerText);
            // Refresh the client using the client record from the database to pick up changes
            client = (Client) localDB.getDocument(client.getDocumentID());
            // Refresh the header to reflect any changes
            ((ListClientHeader) getActivity()).loadHeader(client);
        } else {
            // Reload requires view contents to be re-loaded with saved content
            if (this.listView.getAdapter() == null) {
                this.listView.setAdapter(adapter);
            }
            if (listViewState != null) {
                listView.onRestoreInstanceState(listViewState);
            }
            if (footer.getText().toString().isEmpty()) {
                footer.setText(footerText);
            }
        }
        // Build 110 - Set the share action text based on the currently selected documents
        if (shareOption != null) {
            createShareActionProvider(shareOption);
        }
    }

    private boolean selectDocument(Document document) {
        boolean selected = false;
        // Check for privilege

        // Build 086 - Replaced by Demographic document check
        //if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ACCESS_ALL_DOCUMENTS)) {
        //    selected = true;
        //} else if (document.getDocumentType() == Document.Note &&
        //        currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ACCESS_NOTES)) {
        //    selected = true;
        //}
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ACCESS_ALL_DOCUMENTS)) {
            selected = true;
        } else {
            // From Build 086 READ_NOTES is used for READ_DEMOGRAPHICDOCUMENTS
            switch (document.getDocumentType()) {
                case Document.Client:
                case Document.Case:
                case Document.Contact:
                    if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ACCESS_NOTES)) {
                        selected = true;
                    }
                    break;
                case Document.Note:
                    if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ACCESS_NOTES)) {
                        // Build 086 - Special case for Notes. If READ_NOTES (demographic documents)
                        // only sticky notes are allowed
                        Note note = (Note) document;
                        if (note.isStickyFlag()) {
                            selected = true;
                        }
                    }
                    break;
                default:
                    selected = false;
            }
        }
        // Check for selection
        if (selected) {
            selected = false;
            switch (selectMode) {
                case ALL:
                    selected = true;
                    break;
                case UNCANCELLED:
                    if (!document.getCancelledFlag())
                        selected = true;
                    break;
                case CONTACT:
                    if (document.getDocumentType() == Document.Contact) {
                        selected = true;
                    }
                    break;
                case TYPE:
                    switch (document.getDocumentType()) {
                        case Document.Note:
                            Note note = (Note) document;
                            if (note.getNoteType().getItemValue().equals(selectedDocumentType)) {
                                selected = true;
                            }
                            break;
                        case Document.PdfDocument:
                            PdfDocument pdfDocument = (PdfDocument) document;
                            if (pdfDocument.getPdfType().getItemValue().equals(selectedDocumentType)) {
                                selected = true;
                            }
                            break;
                        default:
                            if (document.getDocumentTypeString().equals(selectedDocumentType)) {
                                selected = true;
                            }
                    }

            }

        }
        // Check for search
        if (selected) {
            selected = search(document);
        }
        return selected;
    }

    private boolean search(Document document) {
        if (searchText.isEmpty()) {
            return true;
        } else {
            switch (document.getDocumentType()) {
                case Document.Case:
                    return ((Case) document).search(searchText);
                case Document.Client:
                    return ((Client) document).search(searchText);
                case Document.Contact:
                    return ((Contact) document).search(searchText);
                case Document.Image:
                    return ((Image) document).search(searchText);
                case Document.Note:
                    Note note = (Note) document;
                    if (note.getInitialNote() != null) {
                        return (((Note) document).getInitialNote()).search(searchText);
                    } else {
                        return ((Note) document).search(searchText);
                    }
                case Document.PdfDocument:
                    return ((PdfDocument) document).search(searchText);
                default:
                    return false;
            }
        }
    }

    private int testForClientSession(int pos, Document document) {
        int newPos = pos;
        if (document.getReferenceDate().getTime() <= new Date().getTime()) {

            if (document.getDocumentType() == Document.ClientSession) {
                if (pos >= 0) {
                    ClientSession[] clientSessions = ((ListClientHeader) getActivity()).getClientSessions();
                    clientSessions[pos] = (ClientSession) document;
                    newPos--;
                }
            }
            if (document.getDocumentType() == Document.MyWeek) {
                MyWeek myWeek = (MyWeek) document;
                ClientSession[] clientSessions = ((ListClientHeader) getActivity()).getClientSessions();
                for (int i = 0; i < 5; i++) {
                    if (clientSessions[i] != null && clientSessions[i].getSessionID().equals(myWeek.getSessionID())) {
                        clientSessions[i].setStatus((int) myWeek.getScore());
                    }
                }

            }
        }
        return newPos;
    }

    private void checkClientChanges() {
        boolean transportRequired = false;
        if (client.getCurrentCase() != null) {
            if (!client.getCurrentCase().getTransportRequired().equals("No")) {
                transportRequired = true;
            }
            if ((client.isTransportRequired() && !transportRequired) ||
                    (!client.isTransportRequired() && transportRequired)) {
                assocDocumentChanged = true;
                client.setTransportRequired(transportRequired);
            }
            if (!client.getTransportRequiredType().equals(client.getCurrentCase().getTransportRequired())) {
                assocDocumentChanged = true;
                client.setTransportRequiredType(client.getCurrentCase().getTransportRequired());
            }
        }

        if (client.getTransportBooked() != transportBooked) {
            assocDocumentChanged = true;
            client.setTransportBooked(transportBooked);
        }
        if (client.getTransportUsed() != transportUsed) {
            assocDocumentChanged = true;
            client.setTransportUsed(transportUsed);
        }
        if (client.getSessionsOffered() != sessionsOffered) {
            assocDocumentChanged = true;
            client.setSessionsOffered(sessionsOffered);
        }
        if (client.getSessionsAttended() != sessionsAttended) {
            assocDocumentChanged = true;
            client.setSessionsAttended(sessionsAttended);
        }
        if (client.getSessionsCancelled() != sessionsCancelled) {
            assocDocumentChanged = true;
            client.setSessionsCancelled(sessionsCancelled);
        }
        if (client.getSessionsDNA() != sessionsDNA) {
            assocDocumentChanged = true;
            client.setSessionsDNA(sessionsDNA);
        }
        // Time to first sessions
        if (client.getStartCase() != null) {
            long startDay = CRISUtil.midnightEarlier(client.getStartCase().getReferenceDate()).getTime() / 84600000;
            if (firstSession.getTime() > Long.MIN_VALUE) {
                long firstSessionDay = CRISUtil.midnightEarlier(firstSession).getTime() / 84600000;
                long daysToFirstSession = firstSessionDay - startDay;
                if (daysToFirstSession > 0 && client.getDaysToFirstSession() != daysToFirstSession) {
                    assocDocumentChanged = true;
                    client.setDaysToFirstSession(daysToFirstSession);

                }
            }
            if (firstAttendedSession.getTime() > Long.MIN_VALUE) {
                long firstAttendedSessionDay = CRISUtil.midnightEarlier(firstAttendedSession).getTime() / 84600000;
                long daysToFirstAttendedSession = firstAttendedSessionDay - startDay;
                if (daysToFirstAttendedSession > 0 && client.getDaysToFirstAttendedSession() != daysToFirstAttendedSession) {
                    assocDocumentChanged = true;
                    client.setDaysToFirstAttendedSession(daysToFirstAttendedSession);

                }
            }
        }
        firstSession = new Date(Long.MIN_VALUE);
        firstAttendedSession = new Date(Long.MIN_VALUE);
        // Finally, save if changed
        if (assocDocumentChanged) {
            client.save(false);
        }

    }

    private void testForAssociatedDocument(Document document) {
        switch (document.getDocumentType()) {

            case Document.Case:
                if (!document.getCancelledFlag()) {
                    Case thisCase = (Case) document;
                    // Test for most recent Case
                    if (!currentCaseFound) {
                        currentCaseFound = true;
                        if (client.getCurrentCaseID() == null ||
                                !client.getCurrentCaseID().equals(document.getDocumentID())) {
                            client.setCurrentCaseID(thisCase.getDocumentID());
                            client.setCurrentCase(thisCase);
                            assocDocumentChanged = true;
                        }
                    }
                    // Test for Start Case
                    if (!startCaseFound && thisCase.getCaseType().equals("Start")) {
                        startCaseFound = true;
                        if (client.getStartcaseID() == null ||
                                !client.getStartcaseID().equals(document.getDocumentID())) {
                            client.setStartcaseID(thisCase.getDocumentID());
                            client.setStartCase(thisCase);
                            assocDocumentChanged = true;
                        }
                    }
                }
                break;

            case Document.ClientSession:
                ClientSession thisSession = (ClientSession) document;
                // Only count sessions up to the case start
                if (!startCaseFound) {
                    sessionsOffered++;
                    firstSession = thisSession.getReferenceDate();
                    if (thisSession.getCancelledFlag()) {
                        sessionsCancelled++;
                    } else if (thisSession.isAttended()) {
                        sessionsAttended++;
                        firstAttendedSession = thisSession.getReferenceDate();
                    } else {
                        // Could be in the future
                        if (thisSession.getReferenceDate().before(new Date())) {
                            sessionsDNA++;
                        }
                    }
                }
                break;

            case Document.Contact:
                if (!document.getCancelledFlag()) {
                    Contact thisContact = (Contact) document;
                    switch (thisContact.getContactType().getItemValue()) {
                        case "Agency Contact":
                            if (!currentAgencyFound) {
                                currentAgencyFound = true;
                                if (client.getCurrentAgencyID() == null ||
                                        !client.getCurrentAgencyID().equals(document.getDocumentID())) {
                                    client.setCurrentAgencyID(thisContact.getDocumentID());
                                    client.setCurrentAgency(thisContact);
                                    assocDocumentChanged = true;
                                }
                            }
                            break;
                        case "School Contact":
                            if (!currentSchoolFound) {
                                currentSchoolFound = true;
                                if (client.getCurrentSchoolID() == null ||
                                        !client.getCurrentSchoolID().equals(document.getDocumentID())) {
                                    client.setCurrentSchoolID(thisContact.getDocumentID());
                                    client.setCurrentSchool(thisContact);
                                    assocDocumentChanged = true;
                                }
                            }
                            break;
                    }
                }
                break;

            case Document.CriteriaAssessmentTool:
                if (!currentToolFound && !document.getCancelledFlag()) {
                    currentToolFound = true;
                    CriteriaAssessmentTool thisTool = (CriteriaAssessmentTool) document;
                    if (client.getCurrentToolID() == null ||
                            !client.getCurrentToolID().equals(document.getDocumentID())) {
                        client.setCurrentToolID(thisTool.getDocumentID());
                        client.setCurrentTool(thisTool);
                        assocDocumentChanged = true;
                    }
                }
                break;

            case Document.Transport:
                if (!document.getCancelledFlag()) {
                    Transport thisTransport = (Transport) document;
                    if (thisTransport.isBooked() && thisTransport.isRequiredOutbound())
                        transportBooked++;
                    if (thisTransport.isBooked() && thisTransport.isRequiredReturn())
                        transportBooked++;
                    if (thisTransport.isRequiredOutbound() && thisTransport.isUsedOutbound())
                        transportUsed++;
                    if (thisTransport.isRequiredReturn() && thisTransport.isUsedReturn())
                        transportUsed++;
                }
                break;
        }
    }

    private void addToDocumentTypes(Document document) {
        String newDocumentType = null;
        switch (document.getDocumentType()) {
            case Document.Note:
                Note note = (Note) document;
                // Build 119 2 May 2019 Introduced an error where note could be created with a
                // null note type id. In this case it can be ignored
                if (note.getNoteTypeID() != null) {
                    newDocumentType = note.getNoteType().getItemValue();
                }
                break;
            case Document.PdfDocument:
                PdfDocument pdfDocument = (PdfDocument) document;
                newDocumentType = pdfDocument.getPdfType().getItemValue();
                break;
            default:
                newDocumentType = document.getDocumentTypeString();
        }
        if (newDocumentType != null && !documentTypes.contains(newDocumentType)) {
            documentTypes.add(newDocumentType);
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ALLOW_EXPORT)) {
            MenuItem selectExport = menu.add(0, MENU_EXPORT, 1, "Export to Google Sheets");
            selectExport.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        // Build 102 Add Share at client level
        //MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        //shareOption.setVisible(false);
        shareOption = menu.findItem(R.id.menu_item_share);
        shareOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        // Build 110 - Call moved to onResume so that it reflects current set of documents
        //createShareActionProvider(shareOption);
        MenuItem selectAllOption = menu.add(0, MENU_SELECT_ALL, 10, "Show All Documents");
        selectAllOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        MenuItem selectCancelledOption = menu.add(0, MENU_SELECT_UNCANCELLED, 11, "Show Uncancelled Documents");
        selectCancelledOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        MenuItem selectContactOption = menu.add(0, MENU_SELECT_CONTACTS, 12, "Show Contact Documents");
        selectContactOption.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_action_show_contacts));
        selectContactOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        MenuItem selectTypeOption = menu.add(0, MENU_SELECT_TYPE, 13, "Select Documents by Type");
        selectTypeOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        MenuItem sortDateOption = menu.add(0, MENU_SORT_DATE, 20, "Sort by Date");
        sortDateOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        MenuItem sortTypeOption = menu.add(0, MENU_SORT_TYPE, 21, "Sort by Type");
        sortTypeOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        if (((ListClientHeader) getActivity()).isFollowingClient()) {
            followOption = menu.add(0, MENU_UNFOLLOW_CLIENT, 30, "Un-follow Client");
            followOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        } else {
            followOption = menu.add(0, MENU_FOLLOW_CLIENT, 30, "Follow Client");
            followOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        MenuItem sendMyWeekOption = menu.add(0, MENU_SEND_MYWEEK_LINK, 40, "Send MyWeek Link");
        sortTypeOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);



        final MenuItem searchItem = menu.findItem(R.id.action_search);
        ActionBar supportActionBar = ((ListClientHeader) getActivity()).getSupportActionBar();
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
                    ((ListActivity) getActivity()).setMode(Document.Mode.EDIT);
                    onResume();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    populateSuggestionAdapter(newText);
                    return false;
                }
            });
            sv.setSuggestionsAdapter(mAdapter);

            // Search threshold is 2 characters by default
            AutoCompleteTextView searchAutoCompleteTextView = (AutoCompleteTextView) sv.findViewById(R.id.search_src_text);
            searchAutoCompleteTextView.setThreshold(1);

            sv.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int position) {
                    return false;
                }

                @Override
                public boolean onSuggestionClick(int position) {
                    String hashTag = mAdapter.getCursor().getString(1);
                    int hashPos = sv.getQuery().toString().lastIndexOf('#');
                    sv.setQuery(sv.getQuery().toString().substring(0, hashPos) + hashTag, true);
                    return false;
                }
            });
            MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    searchText = "";
                    isSearchIconified = true;
                    ((ListActivity) getActivity()).setMode(Document.Mode.EDIT);
                    onResume();
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

    // SHARE MENU ITEM (Both methods are required)
    private void createShareActionProvider(MenuItem menuItem) {
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        Date now = new Date();
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        // Build text string from documents
        String summary = String.format("%s - Case record snapshot\ngenerated on %s by %s\n\n",
                client.getFullName(),
                sDateTime.format(now.getTime()),
                currentUser.getFullName());
        summary += String.format("%s\n", client.textSummary());
        // Build 125 - A crash has occurred due to a null adapterList at this point
        if (adapterList != null) {
            for (int i = 0; i < adapterList.size(); i++) {
                Document document = adapterList.get(i);
                summary += String.format("------ %s ",
                        document.getDocumentTypeString().toUpperCase());
                if (document.getReferenceDate().getTime() != Long.MIN_VALUE) {
                    summary += String.format("---- %s ",
                            sDate.format(document.getReferenceDate()));
                }
                summary += String.format("----\n%s\n",
                        document.textSummary());
            }
        }
        // Generate share Intent
        shareIntent.putExtra(Intent.EXTRA_TEXT, summary);
        shareActionProvider.setShareIntent(shareIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_EXPORT:

                ((ListActivity) getActivity()).setExportListType("One Client");
                ((ListActivity) getActivity()).setExportSelection(client.getFullName());
                ((ListActivity) getActivity()).setExportSort(" ");
                ((ListActivity) getActivity()).setExportSearch(" ");
                ((ListActivity) getActivity()).setClientAdapterList(new ArrayList<Client>());
                ((ListActivity) getActivity()).getClientAdapterList().add(client);
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment fragment = new CRISExport();
                fragmentTransaction.replace(R.id.content, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                return true;
            case MENU_SELECT_ALL:
                selectMode = SelectMode.ALL;
                // V2.0 Set mode to NEW (not READ) to force the data re-load
                ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
                onResume();
                return true;

            case MENU_SELECT_UNCANCELLED:
                selectMode = SelectMode.UNCANCELLED;
                // V2.0 Set mode to NEW (not READ) to force the data re-load
                ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
                onResume();
                return true;

            case MENU_SELECT_CONTACTS:
                if (selectMode == SelectMode.CONTACT) {
                    selectMode = previousSelectMode;
                } else {
                    previousSelectMode = selectMode;
                    selectMode = SelectMode.CONTACT;

                }
                // V2.0 Set mode to NEW (not READ) to force the data re-load
                ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
                onResume();
                return true;

            case MENU_SELECT_TYPE:
                doSelectDocumentType();
                return true;

            case MENU_FOLLOW_CLIENT:
                ((ListClientHeader) getActivity()).setFollowingClient(true);
                localDB.setFollow(currentUser.getUserID(), client.getClientID(), true);
                menu.removeItem(MENU_FOLLOW_CLIENT);
                followOption = menu.add(0, MENU_UNFOLLOW_CLIENT, 30, "Un-follow Client");
                followOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                ((ListClientHeader) getActivity()).loadHeader(client);
                return true;

            case MENU_UNFOLLOW_CLIENT:
                ((ListClientHeader) getActivity()).setFollowingClient(false);
                localDB.setFollow(currentUser.getUserID(), client.getClientID(), false);
                menu.removeItem(MENU_UNFOLLOW_CLIENT);
                followOption = menu.add(0, MENU_FOLLOW_CLIENT, 30, "Follow Client");
                followOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                ((ListClientHeader) getActivity()).loadHeader(client);
                return true;

            case MENU_SORT_DATE:
                sortMode = SortMode.DATE;
                Collections.sort(adapterList, Document.comparatorDate);
                adapter.notifyDataSetChanged();
                return true;

            case MENU_SORT_TYPE:
                sortMode = SortMode.TYPE;
                Collections.sort(adapterList, Document.comparatorType);
                adapter.notifyDataSetChanged();
                return true;

            case R.id.action_search:
                // Search is displayed automatically
                return true;

            case MENU_SEND_MYWEEK_LINK:

                return true;

            default:
                throw new CRISException("Unexpected menu option:" + item.getItemId());

        }
    }


    private ArrayList<Document> loadDocuments() {
        ArrayList<Document> documents = localDB.getAllDocuments(client.getClientID());
        // Search for Notes and handle the note/responses
        for (Document document : documents) {
            if (document.getDocumentType() == Document.Note) {
                Note initialNote = (Note) document;
                if (!initialNote.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                    ArrayList<Note> responses = localDB.getAllResponses(initialNote);
                    // Only interested in Notes with responses
                    if (responses.size() > 0) {
                        // Set fields in response to match initial note
                        Note response = null;
                        for (Note dbResponse : responses) {
                            // Find the member of documents equal to this response
                            for (Document responseDoc : documents) {
                                if (responseDoc.getDocumentID().equals(dbResponse.getDocumentID())) {
                                    response = (Note) responseDoc;
                                    break;
                                }
                            }
                            // Now update the fields
                            if (response != null) {
                                response.setStickyFlag(initialNote.isStickyFlag());
                                response.setStickyDate(initialNote.getStickyDate());
                                response.setCancelledFlag(initialNote.getCancelledFlag());
                                response.setCancellationReason(initialNote.getCancellationReason());
                                response.setCancellationDate(initialNote.getCancellationDate());
                                response.setCancelledByID(initialNote.getCancelledByID());
                                // Load NoteType from the initial Note so that sort by type works
                                NoteType responseNoteType = (NoteType) localDB.getListItem(initialNote.getNoteTypeID());
                                // Set the icon to the appropriate response type
                                NoteType initialNoteType = (NoteType) initialNote.getNoteType();
                                switch (initialNoteType.getNoteIcon()) {

                                    case NoteType.ICON_COLOUR_RED:
                                        responseNoteType.setNoteIcon(NoteType.ICON_COLOUR_RESPONSE_RED);
                                        break;
                                    case NoteType.ICON_COLOUR_AMBER:
                                        responseNoteType.setNoteIcon(NoteType.ICON_COLOUR_RESPONSE_AMBER);
                                        break;
                                    case NoteType.ICON_COLOUR_GREEN:
                                        responseNoteType.setNoteIcon(NoteType.ICON_COLOUR_RESPONSE_GREEN);
                                        break;
                                    default:
                                        responseNoteType.setNoteIcon(NoteType.ICON_COLOUR_RESPONSE_BLUE);
                                        break;
                                }
                                response.setInitialNote(initialNote);
                                response.setNoteType(responseNoteType);

                                // Load the response into the initial note's responseContent
                                User author = localDB.getUser(response.getCreatedByID());
                                String content = "";
                                if (initialNote.getResponseContent() != null) {
                                    content += initialNote.getResponseContent();
                                }
                                content += String.format("\n\n--- On %s, %s responded: ---\n\n%s",
                                        sDateTime.format(response.getCreationDate()),
                                        author.getFullName(),
                                        response.getContent());
                                initialNote.setResponseContent(content);
                            }
                        }
                    }
                }
            }
        }
        return documents;
    }

    private void populateSuggestionAdapter(String query) {
        int hashPos = query.lastIndexOf('#');
        final MatrixCursor c = new MatrixCursor(new String[]{BaseColumns._ID, "hashTag"});
        if (hashPos >= 0) {
            String hashTag = query.substring(hashPos);
            for (int i = 0; i < hashTags.length; i++) {
                if (hashTags[i].toLowerCase().startsWith(hashTag.toLowerCase()))
                    c.addRow(new Object[]{i, hashTags[i]});
            }
        }
        mAdapter.changeCursor(c);
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

    private void doClientSession() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Client Sessions")
                .setMessage("Please use the 'My Session' view to view and update the client's session information..")
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void cannotEditResponse() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Response documents cannot be edited")
                .setMessage("Response documents assume the characteristics of their " +
                        "associated initial note. Therefore, changing the stickiness or " +
                        "cancelling must be done to the entire set at once (via the " +
                        "initial document).\n\n" +
                        "Note: Individual responses may not be cancelled since their content " +
                        "may well be relevant to the note as a whole and may therefore have have " +
                        "had an effect on previous decisions which would be hidden if part of the " +
                        "note were to be removed.")
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void doReadDocument(int position) {
        ((ListActivity) getActivity()).setMode(Document.Mode.READ);
        listViewState = listView.onSaveInstanceState();
        Document document = adapterList.get(position);
        // Check access
        boolean accessAllowed = false;
        // Build 086 - Replaced by Demographic document check
        //if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_DOCUMENTS)) {
        //    accessAllowed = true;
        //} else if (document.getDocumentType() == Document.Note &&
        //        currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_NOTES)) {
        //    accessAllowed = true;
        ///}
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_DOCUMENTS)) {
            accessAllowed = true;
        } else {
            // From Build 086 READ_NOTES is used for READ_DEMOGRAPHICDOCUMENTS
            switch (document.getDocumentType()) {
                case Document.Client:
                case Document.Case:
                case Document.Contact:
                    if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_NOTES)) {
                        accessAllowed = true;
                    }
                    break;
                case Document.Note:
                    if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_NOTES)) {
                        // Build 086 - Special case for Notes. If READ_NOTES (demographic documents)
                        // only sticky notes are allowed
                        Note note = (Note) document;
                        if (note.isStickyFlag()) {
                            accessAllowed = true;
                        }
                    }
                    break;
                default:
                    accessAllowed = false;
            }
        }
        if (accessAllowed) {
            ((ListActivity) getActivity()).setDocument(document);
            localDB.read(document, currentUser);
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction;
            Fragment fragment;

            switch (document.getDocumentType()) {
                case Document.Case:
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragment = new ReadCase();
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case Document.Client:
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragment = new ReadClient();
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case Document.Contact:
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragment = new ReadContact();
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case Document.ClientSession:
                    doClientSession();
                    break;
                case Document.CriteriaAssessmentTool:
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragment = new ReadCAT();
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case Document.Image:
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragment = new ReadImage();
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case Document.MyWeek:
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragment = new ReadMyWeek();
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case Document.Note:
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragment = new EditNote();
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case Document.PdfDocument:
                    PdfDocument pdfDocument = (PdfDocument) document;
                    PdfDocument.displayPDFDocument(pdfDocument, parent.getContext());
                    break;
                case Document.Transport:
                    fragmentTransaction = fragmentManager.beginTransaction();
                    fragment = new ReadTransport();
                    fragmentTransaction.replace(R.id.content, fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
            }
        } else {
            doNoPrivilege();
        }
    }

    private boolean doEditDocument(int position) {
        ((ListActivity) getActivity()).setMode(Document.Mode.EDIT);
        listViewState = listView.onSaveInstanceState();
        Document document = adapterList.get(position);
        // Build 098 - There ia a 'very' intermittent bug which does not return a valid document
        if (document != null) {
            Note note;
            // Check access. Get rid of note responses first since they have a special error message
            boolean accessAllowed = true;
            if (document.getDocumentType() == Document.Note) {
                note = (Note) document;
                if (note.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                    cannotEditResponse();
                    accessAllowed = false;
                }
            }
            // Now the privilege checks
            if (accessAllowed) {
                // Build 086 - Replaced by Demographic document check
                //accessAllowed = false;
                //if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_ALL_DOCUMENTS)) {
                //    accessAllowed = true;
                //} else if (document.getDocumentType() == Document.Note &&
                //        currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_NOTES)) {
                //    accessAllowed = true;
                //}
                if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_ALL_DOCUMENTS)) {
                    accessAllowed = true;
                } else {
                    // From Build 086 WRITE_NOTES is used for WRITE_DEMOGRAPHICDOCUMENTS
                    switch (document.getDocumentType()) {
                        case Document.Client:
                        case Document.Case:
                        case Document.Contact:
                            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_NOTES)) {
                                accessAllowed = true;
                            }
                            break;
                        case Document.Note:
                            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_NOTES)) {
                                // Build 086 - Special case for Notes. If WRITE_NOTES (demographic documents)
                                // only sticky notes are allowed
                                note = (Note) document;
                                if (note.isStickyFlag()) {
                                    accessAllowed = true;
                                }
                            }
                            break;
                        default:
                            accessAllowed = false;
                    }
                }
                if (accessAllowed) {
                    ((ListActivity) getActivity()).setDocument(document);
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction;
                    Fragment fragment;
                    switch (document.getDocumentType()) {
                        case Document.Case:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditCase();
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        case Document.Client:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditClient();
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        case Document.Contact:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditContact();
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        case Document.ClientSession:
                            doClientSession();
                            break;
                        case Document.CriteriaAssessmentTool:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditCAT();
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        case Document.Image:
                            ((ListActivity) getActivity()).tryEditFileDocument(Document.Mode.EDIT, Document.Image);
                            break;
                        case Document.MyWeek:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditMyWeek();
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        case Document.Note:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditNote();
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        case Document.PdfDocument:
                            ((ListActivity) getActivity()).tryEditFileDocument(Document.Mode.EDIT, Document.PdfDocument);
                            break;
                        case Document.Transport:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditTransport();
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        default:
                            throw new CRISException(String.format(Locale.UK,
                                    "Unexpected document type: %d", document.getDocumentType()));
                    }
                } else {
                    doNoPrivilege();
                }
            }
        }
        return true;
    }

    private void doNewDocument() {
        ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
        listViewState = listView.onSaveInstanceState();
        final PickList documentTypes = new PickList(localDB, ListType.DOCUMENT_TYPE, 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Create a new:");
        ArrayList<String> itemList = documentTypes.getOptions();
        String[] items = itemList.toArray(new String[itemList.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String documentType = documentTypes.getOptions().get(which);
                // Check access
                boolean accessAllowed = false;
                // Build 086 - Replaced by Demographic document check
                //if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_ALL_DOCUMENTS)) {
                //    accessAllowed = true;
                //} else if (Document.getDocumentType(documentType) == Document.Note &&
                //        currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_NOTES)) {
                //    accessAllowed = true;
                //} else if (Document.getDocumentType(documentType) == Document.Note &&
                //        currentUser.getRole().hasPrivilege(Role.PRIVILEGE_CREATE_NOTES)) {
                //    accessAllowed = true;
                //}
                // Build 086 - Replaced by Demographic document check
                if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_ALL_DOCUMENTS)) {
                    accessAllowed = true;
                } else {
                    // From Build 086 WRITE_NOTES is used for WRITE_DEMOGRAPHICDOCUMENTS
                    switch (Document.getDocumentType(documentType)) {
                        case Document.Client:
                        case Document.Case:
                        case Document.Contact:
                        case Document.Note:
                            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_NOTES) ||
                                    currentUser.getRole().hasPrivilege(Role.PRIVILEGE_CREATE_NOTES)) {
                                accessAllowed = true;
                            }
                            break;
                        default:
                            accessAllowed = false;
                    }
                }
                if (accessAllowed) {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction;
                    Fragment fragment;
                    switch (Document.getDocumentType(documentType)) {
                        case Document.Case:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditCase();
                            ((ListActivity) getActivity()).setDocument(new Case(currentUser, client.getClientID()));
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        case Document.Contact:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditContact();
                            ((ListActivity) getActivity()).setDocument(new Contact(currentUser, client.getClientID()));
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        case Document.CriteriaAssessmentTool:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditCAT();
                            ((ListActivity) getActivity()).setDocument(
                                    new CriteriaAssessmentTool(currentUser, client.getClientID()));
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        case Document.Image:
                            ((ListActivity) getActivity()).setDocument(new Image(currentUser, client.getClientID()));
                            ((ListActivity) getActivity()).tryEditFileDocument(Document.Mode.NEW, Document.Image);
                            break;
                        case Document.Note:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditNote();
                            ((ListActivity) getActivity()).setDocument(new Note(currentUser, client.getClientID()));
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        case Document.PdfDocument:
                            ((ListActivity) getActivity()).setDocument(new PdfDocument(currentUser, client.getClientID()));
                            ((ListActivity) getActivity()).tryEditFileDocument(Document.Mode.NEW, Document.PdfDocument);
                            break;
                        case Document.Transport:
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragment = new EditTransport();
                            ((ListActivity) getActivity()).setDocument(new Transport(currentUser, client.getClientID()));
                            fragmentTransaction.replace(R.id.content, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            break;
                        default:
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Not Yet Implemented")
                                    .setMessage("Unfortunately, this part of the system is still on the drawing board.")
                                    .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                                    .show();
                    }
                } else {
                    doNoPrivilege();
                }
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void displayDocumentHistory(int position, SwipeDetector.Action action) {
        Document document = adapterList.get(position);
        listViewState = listView.onSaveInstanceState();
        // Check access
        boolean accessAllowed = false;
        // Build 086 - Replaced by Demographic document check
        //if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_DOCUMENTS)) {
        //    accessAllowed = true;
        //} else if (document.getDocumentType() == Document.Note &&
        //        currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_NOTES)) {
        //    accessAllowed = true;
        ///}
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_DOCUMENTS)) {
            accessAllowed = true;
        } else {
            // From Build 086 READ_NOTES is used for READ_DEMOGRAPHICDOCUMENTS
            switch (document.getDocumentType()) {
                case Document.Client:
                case Document.Case:
                case Document.Contact:
                    if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_NOTES)) {
                        accessAllowed = true;
                    }
                    break;
                case Document.Note:
                    if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_NOTES)) {
                        // Build 086 - Special case for Notes. If READ_NOTES (demographic documents)
                        // only sticky notes are allowed
                        Note note = (Note) document;
                        if (note.isStickyFlag()) {
                            accessAllowed = true;
                        }
                    }
                    break;
                default:
                    accessAllowed = false;
            }
        }
        if (accessAllowed) {
            //Loop through all instances of the document gathering data
            String history = "";
            ArrayList<UUID> recordIDs = localDB.getRecordIDs(document);
            for (int i = 0; i < recordIDs.size(); i++) {
                boolean isEarliest = (i == recordIDs.size() - 1);
                history += localDB.getDocumentMetaData(recordIDs.get(i), isEarliest, action);
                if (!isEarliest) {
                    switch (document.getDocumentType()) {
                        case Document.Case:
                            history += Case.getChanges(localDB, recordIDs.get(i + 1), recordIDs.get(i), action);
                            break;
                        case Document.Client:
                            history += Client.getChanges(localDB, recordIDs.get(i + 1), recordIDs.get(i), action);
                            break;
                        case Document.Contact:
                            history += Contact.getChanges(localDB, recordIDs.get(i + 1), recordIDs.get(i), action);
                            break;
                        case Document.ClientSession:
                            history += ClientSession.getChanges(localDB,
                                    recordIDs.get(i + 1),
                                    recordIDs.get(i),
                                    action);
                            break;
                        case Document.CriteriaAssessmentTool:
                            history += CriteriaAssessmentTool.getChanges(localDB, recordIDs.get(i + 1), recordIDs.get(i), action);
                        case Document.Image:
                            history += Image.getChanges(localDB, recordIDs.get(i + 1), recordIDs.get(i), action);
                            break;
                        case Document.MyWeek:
                            history += MyWeek.getChanges(localDB, recordIDs.get(i + 1), recordIDs.get(i), action);
                            break;
                        case Document.Note:
                            history += Note.getChanges(localDB, recordIDs.get(i + 1), recordIDs.get(i), action);
                            break;
                        case Document.PdfDocument:
                            history += PdfDocument.getChanges(localDB, recordIDs.get(i + 1), recordIDs.get(i), action);
                            break;
                        case Document.Transport:
                            history += Transport.getChanges(localDB, recordIDs.get(i + 1), recordIDs.get(i), action);
                            break;
                    }
                }
            }
            // Add some detail about the session
            if (action.equals(SwipeDetector.Action.RL)) {
                switch (document.getDocumentType()) {
                    case Document.ClientSession:
                        ClientSession clientSession = (ClientSession) document;
                        if (clientSession.getSession() != null) {
                            history += String.format("\nAssociated Session:\n\nName: %s\nDate: %s\nID: %s\n",
                                    clientSession.getSession().getSessionName(),
                                    sDateTime.format(clientSession.getSession().getReferenceDate()),
                                    clientSession.getSession().getDocumentID());
                        }
                        break;
                    case Document.MyWeek:
                        MyWeek myWeek = (MyWeek) document;
                        if (myWeek.getSession() != null) {
                            history += String.format("\nAssociated Session:\n\nName: %s\nDate: %s\nID: %s\n",
                                    myWeek.getSession().getSessionName(),
                                    sDateTime.format(myWeek.getSession().getReferenceDate()),
                                    myWeek.getSession().getDocumentID());
                        }
                        break;
                    case Document.Note:
                        Note note = (Note) document;
                        if (note.getSession() != null) {
                            history += String.format("\nAssociated Session:\n\nName: %s\nDate: %s\nID: %s\n",
                                    note.getSession().getSessionName(),
                                    sDateTime.format(note.getSession().getReferenceDate()),
                                    note.getSession().getDocumentID());
                        }
                        break;
                    case Document.PdfDocument:
                        PdfDocument pdfDocument = (PdfDocument) document;
                        if (pdfDocument.getSession() != null) {
                            history += String.format("\nAssociated Session:\n\nName: %s\nDate: %s\nID: %s\n",
                                    pdfDocument.getSession().getSessionName(),
                                    sDateTime.format(pdfDocument.getSession().getReferenceDate()),
                                    pdfDocument.getSession().getDocumentID());
                        }
                        break;
                }
            }
            if (document.getDocumentType() == Document.Client) {
                history += String.format("\nThe current document contents are:\n\n%s\n",
                        document.textSummary());
            } else {
                history += String.format("\nThe current document contents are:\n\n%s\n%s\n",
                        client.textSummary(),
                        document.textSummary());
            }
            Intent intent = new Intent(getActivity(), AlertAndContinue.class);
            intent.putExtra("title", String.format("Change History - %s", document.getDocumentTypeString()));
            intent.putExtra("message", history);
            startActivity(intent);
        } else {
            doNoPrivilege();
        }
    }


    private void doSelectDocumentType() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Only Display Documents of Type:");
        String[] items = documentTypes.toArray(new String[documentTypes.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String documentType = documentTypes.get(which);
                selectMode = SelectMode.TYPE;
                selectedDocumentType = documentType;
                // V2.0 Set mode to NEW (not READ) to force the data re-load
                ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
                onResume();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private class DocumentAdapter extends ArrayAdapter<Document> {

        // Constructor
        DocumentAdapter(Context context, List<Document> objects) {
            super(context, 0, objects);
        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.layout_list_item, parent, false);
            }

            ImageView viewItemIcon = (ImageView) convertView.findViewById(R.id.item_icon);
            TextView viewItemDate = (TextView) convertView.findViewById(R.id.item_date);
            TextView viewItemMainText = (TextView) convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = (TextView) convertView.findViewById(R.id.item_additional_text);

            final Document document = adapterList.get(position);

            // Set the colour (for cancellations)
            int color;
            if (document.getCancelledFlag()) {
                color = ContextCompat.getColor(convertView.getContext(), R.color.red);

            } else {
                color = ContextCompat.getColor(convertView.getContext(), R.color.text_grey);
            }
            viewItemMainText.setTextColor(color);
            viewItemAdditionalText.setTextColor(color);
            viewItemDate.setTextColor(color);

            SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
            viewItemDate.setText(sDate.format(document.getReferenceDate()));
            viewItemMainText.setText(document.getDocumentTypeString());
            viewItemAdditionalText.setText(document.getSummaryLine1());
            switch (document.getDocumentType()) {
                case Document.Case:
                    switch (((Case) document).getClientStatus()) {
                        case Case.RED:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_case_red));
                            break;
                        case Case.AMBER:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_case_amber));
                            break;
                        case Case.GREEN:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_case_green));
                            break;
                    }
                    viewItemMainText.setText(String.format("%s %s", document.getDocumentTypeString(), ((Case) document).getCaseType()));
                    break;
                case Document.Client:
                    boolean isBirthday = false;
                    if (client.getDateOfBirth().getTime() != Long.MIN_VALUE) {
                        // Calculate client's age
                        Calendar dob = Calendar.getInstance();
                        dob.setTime(client.getDateOfBirth());
                        Calendar now = Calendar.getInstance();
                        now.setTime(new Date());
                        int age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
                        if (now.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                            age--;
                        }
                        // DOB is midnight which is treated as yesterday for DAY of YEAR
                        int dayDOB = dob.get(Calendar.DAY_OF_YEAR) + 1;
                        int dayToday = now.get(Calendar.DAY_OF_YEAR);
                        if (dayDOB == dayToday) {
                            isBirthday = true;
                        }
                        viewItemDate.setText(String.format(Locale.UK,
                                "%s (%d)", sDate.format(client.getDateOfBirth()), age));
                    }
                    if (client.getCurrentCase() == null) {
                        viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_grey));
                    } else {
                        Case currentCase = client.getCurrentCase();
                        // Summary now has this text
                        /*
                        // Unpick the current case
                        String tier = "No Tier";
                        String group = "No Group";
                        String keyworkerName = "No Keyworker";
                        String keyworkerContact = "";
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
                        String additionalText = String.format("%s - %s - %s",
                                tier,
                                group,
                                keyworkerName);
                        if (!keyworkerContact.isEmpty()) {
                            additionalText += String.format(" (%s)", keyworkerContact);
                        }
                        viewItemAdditionalText.setText(additionalText);
                        */
                        if (isBirthday) {
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_birthday_cake));
                        } else {
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
                    break;
                case Document.ClientSession:
                    viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_sessions));
                    break;
                case Document.Contact:
                    Contact contactDocument = (Contact) document;
                    String mainText = contactDocument.getContactType().getItemValue();
                    if (contactDocument.getEndDate().getTime() != Long.MIN_VALUE) {
                        mainText += String.format(" (Ended: %s)",
                                sDate.format(contactDocument.getEndDate()));
                    }
                    viewItemMainText.setText(mainText);
                    switch (contactDocument.getContactType().getItemValue()) {
                        case "Agency Contact":
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_contact_agency));
                            break;
                        case "School Contact":
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_contact_school));
                            break;
                        default:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_contact));
                    }
                    break;
                case Document.Image:
                    viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_image));
                    break;
                case Document.Note:
                    Note noteDocument = (Note) document;
                    NoteType noteType = (NoteType) noteDocument.getNoteType();
                    viewItemMainText.setText(noteType.getItemValue());
                    switch (noteType.getNoteIcon()) {
                        case NoteType.ICON_COLOUR_RED:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_red));
                            break;
                        case NoteType.ICON_COLOUR_AMBER:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_amber));
                            break;
                        case NoteType.ICON_COLOUR_GREEN:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_green));
                            break;
                        case NoteType.ICON_COLOUR_BLUE:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_blue));
                            break;
                        case NoteType.ICON_COLOUR_RESPONSE_RED:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_red));
                            break;
                        case NoteType.ICON_COLOUR_RESPONSE_AMBER:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_amber));
                            break;
                        case NoteType.ICON_COLOUR_RESPONSE_GREEN:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_green));
                            break;
                        case NoteType.ICON_COLOUR_RESPONSE_BLUE:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_blue));
                            break;
                        case NoteType.ICON_COLOUR_UNKNOWN:
                            // Pre-V1.1
                            if (noteDocument.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                                viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_response_blue));
                            } else {
                                viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_blue));
                            }
                            break;
                        // Build 128 - Automatically generated Text/Email/Phone Notes may have odd NoteType due to earlier bug
                        default:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_note_blue));
                            break;
                    }
                    break;
                case Document.CriteriaAssessmentTool:
                    viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_criteria_assesmnt_tool));
                    break;
                case Document.MyWeek:
                    MyWeek myWeekDocument = (MyWeek) document;
                    switch ((int) myWeekDocument.getScore()) {
                        case 1:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_red));
                            break;
                        case 2:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_orange));
                            break;
                        case 3:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_yellow));
                            break;
                        case 4:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_green));
                            break;
                        case 5:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_blue));
                            break;
                        case 6:
                            viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_purple));
                            break;
                    }
                    break;
                case Document.PdfDocument:
                    PdfDocument pdfDocument = (PdfDocument) document;
                    viewItemMainText.setText(pdfDocument.getPdfType().getItemValue());
                    viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pdf_document));
                    break;
                case Document.Transport:
                    Transport transport = (Transport) document;
                    if ((!transport.isRequiredOutbound() || (transport.isRequiredOutbound() && transport.isUsedOutbound())) &&
                            (!transport.isRequiredReturn() || (transport.isRequiredReturn() && transport.isUsedReturn()))) {
                        viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_transport_green));
                    } else if (transport.isBooked()) {
                        viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_transport_amber));
                    } else {
                        viewItemIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_transport_red));
                    }
                    break;
                default:
                    throw new CRISException(String.format(Locale.UK,
                            "Unexpected document type when setting document icon: %d", document.getDocumentType()));
            }


            return convertView;
        }
    }


}
