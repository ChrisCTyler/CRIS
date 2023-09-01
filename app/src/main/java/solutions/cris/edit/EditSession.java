package solutions.cris.edit;
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
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

// Build 200 Use the androidX Fragment class
//import android.app.Fragment;
//import android.app.FragmentManager;
import androidx.fragment.app.Fragment;

import java.text.ParseException;
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
import solutions.cris.list.ListActivity;
import solutions.cris.list.ListClients;
import solutions.cris.list.ListSessions;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.Contact;
import solutions.cris.object.Document;
import solutions.cris.object.Group;
import solutions.cris.object.ListItem;
import solutions.cris.object.Role;
import solutions.cris.object.Session;
import solutions.cris.object.User;
import solutions.cris.utils.BatchInviteDialogFragment;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.GroupPickList;
import solutions.cris.utils.LocalSettings;
import solutions.cris.utils.PickList;

public class EditSession extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);
    private static final SimpleDateFormat sTime = new SimpleDateFormat("HH:mm", Locale.UK);

    private Session editDocument;

    private Spinner groupSpinner;
    private EditText nameView;
    private EditText addressView;
    private EditText postcodeView;
    private EditText dateView;
    private EditText timeView;
    private Spinner keyworkerSpinner;
    private Spinner sessionCoordinatorSpinner;
    private Spinner otherStaffSpinner;
    private ListView otherStaffListView;
    private TextView durationView;
    private EditText additionalInformationView;
    // Build 200 new field, add points if attended, minus points if DNA
    private TextView attendancePointsView;

    private LocalDB localDB;
    private View parent;
    private boolean isNewMode;
    private ArrayList<User> otherStaffList;

    private GroupPickList groupPickList;
    private PickList keyworkerPickList;
    private PickList staffPicklist;

    private TextView hintTextView;
    private boolean hintTextDisplayed = true;

    // Build 217 If session date is changed, change the date in the client sessions
    boolean dateChanged = false;

    // Build 200 - This is called by the event listener in ListClients as a result of a OK
    // in the BatchInviteDialogFragment
    public void pickListDialogFragmentOK() {
        // SelectedIDs contains the batch invite list
        ArrayList<Client>inviteeList = new ArrayList<>();
        ArrayList<Client> clients = localDB.getAllClients();

        switch (((ListActivity) getActivity()).getSelectMode()){
            case NONE:
                // Save with no invitees
                break;
            case GROUPS:
                boolean match = false;
                UUID groupID = null;
                for (Client client : clients) {
                    // Build 238 match needs to be set false on every interation of this for loop
                    // otherwise first match will then include everyone else
                    match = false;
                    if (client.getCurrentCase() != null) {
                        Case currentCase = client.getCurrentCase();
                        if (!currentCase.getCaseType().equals("Close")) {
                            if (currentCase.getGroupID() != null) {
                                groupID = currentCase.getGroupID();
                                if (((ListSessions) getActivity()).getSelectedIDs().contains(groupID)) {
                                    match = true;
                                }
                            }
                            if (currentCase.getGroup2ID() != null) {
                                groupID = currentCase.getGroup2ID();
                                if (((ListSessions) getActivity()).getSelectedIDs().contains(groupID)) {
                                    match = true;
                                }
                            }
                        }
                    }
                    if (match){
                        inviteeList.add(client);
                    }
                }
                break;

            case KEYWORKERS:
                for (Client client : clients) {
                    if (client.getCurrentCase() != null) {
                        Case currentCase = client.getCurrentCase();
                        if (!currentCase.getCaseType().equals("Close")) {
                            if (currentCase.getKeyWorkerID() != null) {
                                UUID keyworkerID = currentCase.getKeyWorkerID();
                                if (((ListSessions) getActivity()).getSelectedIDs().contains(keyworkerID)) {
                                    inviteeList.add(client);
                                }
                            }
                        }
                    }
                }
                break;
            case COMMISSIONERS:
                for (Client client : clients) {
                    if (client.getCurrentCase() != null) {
                        Case currentCase = client.getCurrentCase();
                        if (!currentCase.getCaseType().equals("Close")) {
                            if (currentCase.getCommissionerID() != null) {
                                UUID commissionerID = currentCase.getCommissionerID();
                                if (((ListSessions) getActivity()).getSelectedIDs().contains(commissionerID)) {
                                    inviteeList.add(client);
                                }
                            }
                        }
                    }
                }
                break;

            case SCHOOLS:
                for (Client client : clients) {
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
                                    if (((ListSessions) getActivity()).getSelectedIDs().contains(schoolID)) {
                                        inviteeList.add(client);
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case AGENCIES:
                for (Client client : clients) {
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
                                    if (((ListSessions) getActivity()).getSelectedIDs().contains(agencyID)) {
                                        inviteeList.add(client);
                                    }
                                }
                            }
                        }
                    }
                }
                break;
        }
        if (inviteeList.size() > 0){
            new AlertDialog.Builder(getActivity())
                    .setTitle("Batch Invite Wizard")
                    .setMessage(String.format("%d clients will be invited to this session. " +
                            "Are you sure you wish to continue?", inviteeList.size()))
                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            editDocument.save(isNewMode, inviteeList);
                            // Build 200 Use the androidX Fragment class
                            //FragmentManager fragmentManager = getFragmentManager();
                            //fragmentManager.popBackStack();
                            getParentFragmentManager().popBackStack();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    })
                    .show();
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Batch Invite Wizard")
                    .setMessage("No clients have been selected. Do you wish to save the " +
                            "session anyway?")
                    .setPositiveButton("Save Session", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            editDocument.save(isNewMode, inviteeList);
                            // Build 200 Use the androidX Fragment class
                            //FragmentManager fragmentManager = getFragmentManager();
                            //fragmentManager.popBackStack();
                            getParentFragmentManager().popBackStack();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    })
                    .show();
        }


    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.edit_session, container, false);
        ((ListSessions) getActivity()).setSelectedIDs(new ArrayList<>());
        ((ListActivity) getActivity()).setSelectMode(((ListActivity.SelectMode.NONE)));
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (((ListActivity) getActivity()).getMode() == Document.Mode.NEW){
            isNewMode = true;
        } else {
            isNewMode = false;
        }

        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        if (isNewMode) {
            toolbar.setTitle(getString(R.string.app_name) + " - New Session");
        } else {
            toolbar.setTitle(getString(R.string.app_name) + " - Edit Session");
        }

        // Build 225 - Declare the Batch Invite button so that its visibility can
        // be controlled by teh Group Spinner handler
        Button batchInviteButton = (Button) parent.findViewById(R.id.batch_invite_button);

        // Hide the FAB
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        fab.setVisibility(View.GONE);

        // Clear the footer
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        footer.setText("");

        // Get the document to be edited from the activity
        editDocument = (Session) ((ListActivity) getActivity()).getSession();
        localDB = LocalDB.getInstance();

        // Handle sessions which pre-date the otherStaff list
        if (editDocument.getOtherStaffIDList() == null) {
            editDocument.clearOtherStaffIDList();
        }

        // CANCEL BOX
        if (editDocument.getCancelledFlag()) {
            LinearLayout cancelBoxView = (LinearLayout) parent.findViewById(R.id.cancel_box_layout);
            cancelBoxView.setVisibility(View.VISIBLE);
            TextView cancelBy = (TextView) parent.findViewById(R.id.cancel_by);
            String byText = "by ";
            User cancelUser = localDB.getUser(editDocument.getCancelledByID());
            byText += cancelUser.getFullName() + " on ";
            byText += sDate.format(editDocument.getCancellationDate());
            cancelBy.setText(byText);
            TextView cancelReason = (TextView) parent.findViewById(R.id.cancel_reason);
            cancelReason.setText(String.format("Reason: %s", editDocument.getCancellationReason()));
        }

        groupSpinner = (Spinner) parent.findViewById(R.id.group_spinner);
        nameView = (EditText) parent.findViewById(R.id.name);
        addressView = (EditText) parent.findViewById(R.id.address);
        postcodeView = (EditText) parent.findViewById(R.id.postcode);
        keyworkerSpinner = (Spinner) parent.findViewById(R.id.keyworker_spinner);
        sessionCoordinatorSpinner = (Spinner) parent.findViewById(R.id.session_coordinator_spinner);
        otherStaffSpinner = (Spinner) parent.findViewById(R.id.other_staff_spinner);
        otherStaffListView = (ListView) parent.findViewById(R.id.other_staff_list);
        dateView = (EditText) parent.findViewById(R.id.session_date);
        timeView = (EditText) parent.findViewById(R.id.session_time);
        durationView = (EditText) parent.findViewById(R.id.duration);
        additionalInformationView = (EditText) parent.findViewById(R.id.additional_information);
        // Build 200 new field, add points if attended, minus points if DNA
        attendancePointsView = (EditText) parent.findViewById(R.id.attendance_points);

        final LocalSettings localSettings = LocalSettings.getInstance(getActivity());
        TextView groupLabel = (TextView) parent.findViewById(R.id.group_label_text);
        groupLabel.setText(localSettings.Group + "*");
        TextView keyworkerLabel = (TextView) parent.findViewById(R.id.keyworker_label_text);
        keyworkerLabel.setText(localSettings.Keyworker + "*");
        TextView sessionCoordinatorLabelText = (TextView) parent.findViewById(R.id.session_coordinator_label_text);
        // Build 2.1.088 Fix to session label
        //keyworkerLabel.setText(localSettings.SessionCoordinator + "*");
        sessionCoordinatorLabelText.setText(localSettings.SessionCoordinator + "*");
        // 19 Oct 2017 Build 089 Added local setting for Other Staff
        TextView OtherStaffLabel = (TextView) parent.findViewById(R.id.other_staff_label_text);
        OtherStaffLabel.setText(localSettings.OtherStaff);

        // Set up the hint text
        hintTextView = (TextView) parent.findViewById(R.id.hint_text);
        hintTextView.setText(getHintText());
        // Restore value of hintDisplayed (Set to opposite, toggle to come
        if (savedInstanceState != null) {
            hintTextDisplayed = !savedInstanceState.getBoolean(Main.HINT_DISPLAYED);
        }
        toggleHint();
        hintTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleHint();
            }
        });

        // Initialise the Group Spinner
        groupPickList = new GroupPickList(localDB, ((ListActivity) getActivity()).getCurrentUser());
        ArrayAdapter<String> groupAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, groupPickList.getOptions());
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupAdapter);
        groupSpinner.setSelection(groupPickList.getDefaultPosition());
        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                // Build 225 - Hide the Batch Invite button visible, only show if adHocGroup
                batchInviteButton.setVisibility(View.GONE);
                if (position != 0 && isNewMode) {
                    Group selected = (Group) groupPickList.getListItems().get(position);
                    if (selected.getListItemID() != Group.adHocGroupID) {
                        // Preset the associated fields
                        nameView.setText(selected.getItemValue());
                        addressView.setText(selected.getGroupAddress());
                        postcodeView.setText(selected.getGroupPostcode());
                        keyworkerSpinner.setSelection(keyworkerPickList.getPosition(selected.getKeyWorker()));
                        sessionCoordinatorSpinner.setSelection(
                                staffPicklist.getPosition(selected.getSessionCoordinator()));
                        // V2.0.077 Only use a calculated date if the existing date is blank
                        // Otherwise it overwrites with every edit since this is called whenever
                        // the edit page displays
                        // Work out the date of session from previous session
                        if (dateView.getText().toString().isEmpty()) {
                            ArrayList<Session> sessionList = localDB.getAllSessions();
                            Collections.sort(sessionList, Session.comparatorDate);
                            for (Session session : sessionList) {
                                // ignore self
                                if (!session.getDocumentID().equals(editDocument.getDocumentID())) {
                                    // Ignore cancelled documents
                                    if (!session.getCancelledFlag()) {
                                        if (session.getGroupID().equals(selected.getListItemID())) {
                                            // Found most recent
                                            Calendar next = Calendar.getInstance();
                                            next.setTime(session.getReferenceDate());
                                            switch (selected.getFrequencyType()) {
                                                case "Days":
                                                    next.add(Calendar.DATE, selected.getFrequency());
                                                    break;
                                                case "Weeks":
                                                    next.add(Calendar.DATE, selected.getFrequency() * 7);
                                                    break;
                                                case "Fortnights":
                                                    next.add(Calendar.DATE, selected.getFrequency() * 14);
                                                    break;
                                                case "Months":
                                                    next.add(Calendar.MONTH, selected.getFrequency());
                                            }
                                            Date nextDate = next.getTime();
                                            dateView.setText(sDate.format(nextDate));
                                            timeView.setText(sTime.format(nextDate));
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Build 225 - Make the Batch Invite button visible
                        batchInviteButton.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                nameView.setText("");
                addressView.setText("");
                postcodeView.setText("");
                keyworkerSpinner.setSelection(-1);
                sessionCoordinatorSpinner.setSelection(-1);
            }
        });


        dateView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                sessionDatePicker();
                return true;
            }
        });

        timeView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                sessionTimePicker();
                return true;
            }
        });


        // Initialise the Staff Spinners
        ArrayList<User> users = localDB.getAllUsers();
        Collections.sort(users, User.comparator);
        ArrayList<User> keyworkers = new ArrayList<>();
        for (User user : users) {
            if (user.getRole().hasPrivilege(Role.PRIVILEGE_USER_IS_KEYWORKER)) {
                keyworkers.add(user);
            }
        }

        keyworkerPickList = new PickList(keyworkers);
        ArrayAdapter<String> keyworkerAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, keyworkerPickList.getOptions());
        keyworkerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keyworkerSpinner.setAdapter(keyworkerAdapter);
        keyworkerSpinner.setSelection(keyworkerPickList.getDefaultPosition());

        staffPicklist = new PickList(users);
        ArrayAdapter<String> sessionCoordinatorAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, staffPicklist.getOptions());
        sessionCoordinatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sessionCoordinatorSpinner.setAdapter(sessionCoordinatorAdapter);
        sessionCoordinatorSpinner.setSelection(staffPicklist.getDefaultPosition());

        ArrayAdapter<String> otherStaffAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, staffPicklist.getOptions());
        otherStaffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        otherStaffSpinner.setAdapter(otherStaffAdapter);
        otherStaffSpinner.setSelection(staffPicklist.getDefaultPosition());
        // Selecting an 'other' staff adds the staff member to the dynamic other staff list
        otherStaffSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                // Add the new staff member to the other staff list
                // Build 200 - PickList.getUsers replaced with getObjects
                //User otherStaff = staffPicklist.getUsers().get(position);
                User otherStaff = (User) staffPicklist.getObjects().get(position);
                if (!otherStaff.getUserID().equals(User.unknownUser)) {
                    boolean found = false;
                    for (User otherStaffMember : otherStaffList) {
                        if (otherStaffMember.getUserID().equals(otherStaff.getUserID())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        otherStaffList.add(otherStaff);
                        displayOtherStaffList();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Do nothing
            }
        });

        // Cancel Button
        Button cancelButton = (Button) parent.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Build 200 Use the androidX Fragment class
                //FragmentManager fragmentManager = getFragmentManager();
                //fragmentManager.popBackStack();
                getParentFragmentManager().popBackStack();
            }
        });
        // Build 225 - Batch Invite Button
        batchInviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {
                    // Display the Batch Invite wizard
                    BatchInviteDialogFragment dialog = new BatchInviteDialogFragment(localDB, localSettings);
                    dialog.show(getParentFragmentManager(), "BatchInvite");
                }
            }
        });
        // Save Button
        Button saveButton = (Button) parent.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {
                    // Build 200 Batch Invite
                    UUID groupID = editDocument.getGroupID();
                    if (isNewMode) {
                        ArrayList<Client>inviteeList = new ArrayList<>();
                        if (!groupID.equals(Group.adHocGroupID)) {
                            // Normal group, so build an invitee list for the group
                            ArrayList<Client> clients = localDB.getAllClients();
                            for (Client client : clients) {
                                if (client.getCurrentCase() != null) {
                                    if (client.getCurrentCase().getCaseType().equals("Start") ||
                                            client.getCurrentCase().getCaseType().equals("Update")){
                                        if (client.getCurrentCase().getGroupID().equals(groupID) &&
                                                !client.getCurrentCase().isDoNotInviteFlag()) {
                                            inviteeList.add(client);
                                        } else if (client.getCurrentCase().getGroup2ID() != null){
                                            if (client.getCurrentCase().getGroup2ID().equals(groupID) &&
                                                    !client.getCurrentCase().isDoNotInvite2Flag()) {
                                                inviteeList.add(client);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        editDocument.save(isNewMode, inviteeList);
                    } else {
                        // editMode so just save the session document
                        editDocument.save(isNewMode, null);
                        if (dateChanged) {
                            // Build 217 Save all of the Client Sessions to update their ReferenceDate
                            ArrayList<ClientSession> clirentSessions =
                                    localDB.getAllClientSessions(editDocument);
                            for (ClientSession clientSession : clirentSessions) {
                                clientSession.save(false);
                            }

                        }
                    }
                    // Build 200 Use the androidX Fragment class
                    //FragmentManager fragmentManager = getFragmentManager();
                    //fragmentManager.popBackStack();
                    getParentFragmentManager().popBackStack();
                }
            }
        });

        otherStaffList = new ArrayList<>();
        if (isNewMode) {
            nameView.clearFocus();
        } else {
            // Edit Mode
            groupSpinner.setSelection(groupPickList.getPosition(editDocument.getGroup()));
            nameView.setText(editDocument.getSessionName());
            addressView.setText(editDocument.getAddress());
            postcodeView.setText(editDocument.getPostcode());
            keyworkerSpinner.setSelection(keyworkerPickList.getPosition(editDocument.getKeyWorker()));
            sessionCoordinatorSpinner.setSelection(staffPicklist.getPosition(editDocument.getSessionCoordinator()));
            dateView.setText(sDate.format(editDocument.getReferenceDate()));
            timeView.setText(sTime.format(editDocument.getReferenceDate()));
            durationView.setText(String.format(Locale.UK, "%d", editDocument.getDuration()));
            additionalInformationView.setText(editDocument.getAdditionalInformation());
            // Build 200 new field, add points if attended, minus points if DNA
            attendancePointsView.setText(String.format(Locale.UK, "%d", editDocument.getAttendancePoints()));
            if (editDocument.getOtherStaffIDList() != null) {
                for (UUID otherStaffID : editDocument.getOtherStaffIDList()) {
                    otherStaffList.add(localDB.getUser(otherStaffID));
                }
            }
            displayOtherStaffList();
        }
    }

    // MENU BLOCK
    private static final int MENU_CANCEL_DOCUMENT = Menu.FIRST + 1;
    private static final int MENU_UNCANCEL_DOCUMENT = Menu.FIRST + 2;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Initialise the Cancellation menu option
        if (editDocument.getCancelledFlag()) {
            MenuItem cancelOption = menu.add(0, MENU_UNCANCEL_DOCUMENT, 2, "Remove Cancellation");
            cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        } else {
            MenuItem cancelOption = menu.add(0, MENU_CANCEL_DOCUMENT, 3, "Cancel Document");
            cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case MENU_CANCEL_DOCUMENT:
                cancelDocument(true);
                return true;

            case MENU_UNCANCEL_DOCUMENT:
                cancelDocument(false);
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the state
        savedInstanceState.putBoolean(Main.HINT_DISPLAYED, hintTextDisplayed);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private void toggleHint() {
        if (hintTextDisplayed) {
            hintTextView.setMaxLines(2);
            hintTextDisplayed = false;
        } else {
            hintTextDisplayed = true;
            hintTextView.setMaxLines(hintTextView.getLineCount());
        }
    }

    private void cancelDocument(boolean cancelType) {
        if (cancelType) {
            // Get the reason and then call the validate/save sequence.
            final EditText editText = new EditText(getActivity());
            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            new AlertDialog.Builder(getActivity())
                    .setView(editText)
                    .setTitle("Cancel Document")
                    .setMessage("Documents may not be removed, but cancelling them " +
                            "will remove them from view unless the user explicitly requests " +
                            "them. Please specify a cancellation reason")
                    .setPositiveButton("CancelDocument", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (editText.getText().length() > 0) {
                                editDocument.setCancellationDate(new Date());
                                editDocument.setCancellationReason(editText.getText().toString());
                                editDocument.setCancelledByID(((ListActivity) getActivity()).getCurrentUser().getUserID());
                                editDocument.setCancelledFlag(true);
                                if (validate()) {
                                    // Build 200 - No inviteeList
                                    editDocument.save(isNewMode, null);
                                    // Build 200 Use the androidX Fragment class
                                    //FragmentManager fragmentManager = getFragmentManager();
                                    //fragmentManager.popBackStack();
                                    getParentFragmentManager().popBackStack();
                                 }
                            }
                        }
                    })
                    .setNegativeButton("DoNotCancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        } else {  // Uncancel the Document
            editDocument.setCancelledFlag(false);
            editDocument.setCancellationReason("");
            editDocument.setCancellationDate(new Date(Long.MIN_VALUE));
            editDocument.setCancelledByID(null);
            if (validate()) {
                // Build 200 - No inviteeList
                editDocument.save(isNewMode, null);
// Build 200 Use the androidX Fragment class
                //FragmentManager fragmentManager = getFragmentManager();
                //fragmentManager.popBackStack();
                getParentFragmentManager().popBackStack();
            }
        }
    }

    private void sessionDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                dateView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void sessionTimePicker() {
        Calendar newCalendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(0, 0, 0, hour, minute);
                timeView.setText(sTime.format(newDate.getTime()));
            }
        }, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), true);
        timePickerDialog.show();
    }

    private void displayOtherStaffList() {
        // Clear the spinner
        otherStaffSpinner.setSelection(staffPicklist.getDefaultPosition());
        // Sort the list
        Collections.sort(otherStaffList, User.comparator);
        OtherStaffAdapter adapter = new OtherStaffAdapter(getActivity(), otherStaffList);
        otherStaffListView.setAdapter(adapter);
        // Adjust the height of the list of otherStaff based on the size of the array
        setListViewHeightBasedOnChildren(otherStaffListView, adapter);
    }

    public static void setListViewHeightBasedOnChildren(ListView listView, OtherStaffAdapter listAdapter) {
        if (listAdapter != null) {
            int totalHeight = 0;
            for (int i = 0; i < listAdapter.getCount(); i++) {
                View listItem = listAdapter.getView(i, null, listView);
                listItem.measure(0, 0);
                totalHeight += listItem.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
            listView.setLayoutParams(params);
            listView.requestLayout();
        }
    }

    // Validate the document
    private boolean validate() {
        boolean success = true;

        // Clear any existing errors
        nameView.setError(null);
        addressView.setError(null);
        postcodeView.setError(null);
        dateView.setError(null);
        additionalInformationView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        //Group
        ListItem newGroup = groupPickList.getListItems().get(groupSpinner.getSelectedItemPosition());
        // Test for Please select
        if (newGroup.getItemOrder() == -1) {
            TextView errorText = (TextView) groupSpinner.getSelectedView();
            errorText.setError("anything here, just to add the icon");
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = groupSpinner;
            success = false;
        } else {
            editDocument.setGroupID(newGroup.getListItemID());
            // PickList contained
            if (editDocument.getGroupID().equals(Group.adHocGroupID)) {
                editDocument.setGroup(Group.getAdHocGroup());
            } else {
                editDocument.setGroup((Group) localDB.getListItem(editDocument.getGroupID()));
            }

            // Name
            String sName = nameView.getText().toString().trim();
            if (TextUtils.isEmpty(sName)) {
                nameView.setError(getString(R.string.error_field_required));
                focusView = nameView;
                success = false;
            } else {
                editDocument.setSessionName(sName);
            }

            // Address
            String sAddress = addressView.getText().toString().trim();
            if (TextUtils.isEmpty(sAddress)) {
                addressView.setError(getString(R.string.error_field_required));
                focusView = addressView;
                success = false;
            } else {
                editDocument.setAddress(sAddress);
            }

            // Postcode
            String sPostcode = postcodeView.getText().toString().trim();
            if (TextUtils.isEmpty(sPostcode)) {
                postcodeView.setError(getString(R.string.error_field_required));
                focusView = postcodeView;
                success = false;
            } else {
                editDocument.setPostcode(sPostcode);
            }

            //Keyworker
            // Build 200 - PickList.getUsers replaced with getObjects
            //User newKeyworker = keyworkerPickList.getUsers().get(keyworkerSpinner.getSelectedItemPosition());
            User newKeyworker = (User) keyworkerPickList.getObjects().get(keyworkerSpinner.getSelectedItemPosition());
            // Test for Please select
            if (newKeyworker.getUserID().equals(User.unknownUser)) {
                TextView errorText = (TextView) keyworkerSpinner.getSelectedView();
                //errorText.setError("anything here, just to add the icon");
                errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                focusView = keyworkerSpinner;
                success = false;
            } else {
                editDocument.setKeyWorkerID(newKeyworker.getUserID());
                // PickList contained
                editDocument.setKeyWorker(localDB.getUser(editDocument.getKeyWorkerID()));
            }

            // Session Coordinator
            // Build 200 - PickList.getUsers replaced with getObjects
            //User newSessionCoordinator = staffPicklist.getUsers().get(sessionCoordinatorSpinner.getSelectedItemPosition());
            User newSessionCoordinator = (User) staffPicklist.getObjects().get(sessionCoordinatorSpinner.getSelectedItemPosition());
            // Test for Please select
            if (newSessionCoordinator.getUserID().equals(User.unknownUser)) {
                TextView errorText = (TextView) sessionCoordinatorSpinner.getSelectedView();
                //errorText.setError("anything here, just to add the icon");
                errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                focusView = sessionCoordinatorSpinner;
                success = false;
            } else {
                editDocument.setSessionCoordinatorID(newSessionCoordinator.getUserID());
                // PickList contained
                editDocument.setSessionCoordinator(localDB.getUser(editDocument.getSessionCoordinatorID()));
            }

            // Other Staff
            // Clear the existing list
            editDocument.clearOtherStaffIDList();
            for (User otherStaff : otherStaffList) {
                if (!otherStaff.getUserID().equals(editDocument.getKeyWorkerID()) &&
                        !otherStaff.getUserID().equals(editDocument.getSessionCoordinatorID())) {
                    editDocument.addOtherStaffID(otherStaff.getUserID());
                }
            }

            // Date/Time
            boolean dateError = false;
            String sDate = dateView.getText().toString().trim();
            if (TextUtils.isEmpty(sDate)) {
                dateView.setError(getString(R.string.error_field_required));
                focusView = dateView;
                success = false;
                dateError = true;
            } else {
                Date dDate = CRISUtil.parseDate(sDate + " " + sTime);
                if (dDate == null) {
                    dateView.setError(getString(R.string.error_invalid_date));
                    focusView = dateView;
                    success = false;
                    dateError = true;
                }
            }

            String sTime = timeView.getText().toString().trim();
            if (TextUtils.isEmpty(sTime)) {
                timeView.setError(getString(R.string.error_field_required));
                focusView = timeView;
                success = false;
                dateError = true;
            } else {
                Date dTime = CRISUtil.parseTime(sTime);
                if (dTime == null) {
                    timeView.setError(getString(R.string.error_invalid_date));
                    focusView = timeView;
                    success = false;
                    dateError = true;
                }
            }

            if (!dateError) {
                // Build 154 - Corrected the date pattern to use 24 hour clock not 12 hour
                //SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy hh:mm", Locale.UK);
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy kk:mm", Locale.UK);
                Date dDateTime;
                try {
                    dDateTime = sdf.parse(String.format("%s %s", sDate, sTime));
                    // Build 217
                    if (!isNewMode){
                        if (!editDocument.getReferenceDate().equals(dDateTime)){
                            dateChanged = true;
                        }
                    }
                    editDocument.setReferenceDate(dDateTime);
                } catch (ParseException ex) {
                    dateView.setError(getString(R.string.error_invalid_date));
                    focusView = dateView;
                    success = false;
                }
            }

            // Duration
            String sDuration = durationView.getText().toString().trim();
            if (TextUtils.isEmpty(sDuration)) {
                // Build 201 Duration must be non-zero
                //editDocument.setDuration(0);
                durationView.setError(getString(R.string.error_number_not_positive));
                focusView = durationView;
                success = false;

            } else {
                int duration;
                try {
                    duration = Integer.parseInt(sDuration);
                    if (duration <= 0) {
                        durationView.setError(getString(R.string.error_number_not_positive));
                        focusView = durationView;
                        success = false;
                    } else {
                        editDocument.setDuration(duration);
                    }
                } catch (Exception ex) {
                    durationView.setError(getString(R.string.error_invalid_integer));
                    focusView = durationView;
                    success = false;
                }

            }

            // AdditionalInformation
            String sAdditionalInformation = additionalInformationView.getText().toString().trim();
            if (!TextUtils.isEmpty(sAdditionalInformation)) {
                editDocument.setAdditionalInformation(additionalInformationView.getText().toString().trim());
            }

            // Build 200 new field, add points if attended, minus points if DNA
            String sAttendancePoints = attendancePointsView.getText().toString().trim();
            if (TextUtils.isEmpty(sAttendancePoints)) {
                // Build 201 Duration must be non-zero
                //editDocument.setAttendancePoints(0);
                attendancePointsView.setError(getString(R.string.error_number_0_to_10));
                focusView = attendancePointsView;
                success = false;

            } else {
                int attendancePoints;
                try {
                    attendancePoints = Integer.parseInt(sAttendancePoints);
                    if (attendancePoints < 0 || attendancePoints > 10) {
                        attendancePointsView.setError(getString(R.string.error_number_0_to_10));
                        focusView = attendancePointsView;
                        success = false;
                    } else {
                        editDocument.setAttendancePoints(attendancePoints);
                    }
                } catch (Exception ex) {
                    attendancePointsView.setError(getString(R.string.error_invalid_integer));
                    focusView = attendancePointsView;
                    success = false;
                }

            }
        }

        if (!success) {
            focusView.requestFocus();
            focusView.requestFocusFromTouch();
        }
        return success;
    }



    private String getHintText() {
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance(getActivity());
        return String.format("Selecting a %s will automatically fill-in the other fields.", localSettings.Group);
    }

    private class OtherStaffAdapter extends ArrayAdapter<User> {

        // Constructor
        OtherStaffAdapter(Context context, List<User> objects) {
            super(context, 0, objects);
        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.layout_other_staff_list_item, parent, false);
            }

            CheckBox otherStaffCheckBox = (CheckBox) convertView.findViewById(R.id.other_staff_item);

            final User otherStaff = otherStaffList.get(position);

            otherStaffCheckBox.setChecked(true);
            otherStaffCheckBox.setTag(otherStaff);
            otherStaffCheckBox.setText(otherStaff.getFullName());
            otherStaffCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton checkBox, boolean checked) {
                    if (!checked) {
                        User otherStaff = (User) checkBox.getTag();
                        otherStaffList.remove(otherStaff);
                        displayOtherStaffList();
                    }
                }
            });
            return convertView;
        }
    }

}