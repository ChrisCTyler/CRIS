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
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.content.ContextCompat;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.Document;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.Role;
import solutions.cris.object.Session;
import solutions.cris.object.User;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.GroupPickList;
import solutions.cris.utils.LocalSettings;
import solutions.cris.utils.PickList;

public class EditCase extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);
    // MENU BLOCK
    private static final int MENU_CANCEL_DOCUMENT = Menu.FIRST + 1;
    private static final int MENU_UNCANCEL_DOCUMENT = Menu.FIRST + 2;
    private Case editDocument;
    private Client client;
    private Spinner caseTypeSpinner;
    private EditText referenceDateView;
    private ImageView clientRedIcon;
    private ImageView clientAmberIcon;
    private ImageView clientGreenIcon;
    private EditText caseSummaryView;
    private EditText overdueThresholdView;
    private Spinner tierSpinner;
    private Spinner groupSpinner;
    // Build 139 - Second Group
    private Spinner group2Spinner;
    private Spinner keyworkerSpinner;
    private Spinner coworker1Spinner;
    private Spinner coworker2Spinner;
    private Spinner commissionerSpinner;
    private Spinner transportRequiredSpinner;
    private EditText specialInstructionsView;
    // Build 105
    private CheckBox photographyConsentCheckbox;
    private CheckBox doNotInviteCheckbox;
    // Build 139 - Second Group
    private CheckBox doNotInvite2Checkbox;
    private LocalDB localDB;
    private View parent;
    private int clientStatus = Case.AMBER;
    private boolean isNewMode = false;
    private PickList tierPickList;
    private GroupPickList groupPickList;
    // Build 139 - Second Group
    private GroupPickList group2PickList;
    private PickList keyworkerPickList;
    private PickList coworker1PickList;
    private PickList coworker2PickList;
    private PickList commissionerPickList;
    private TextView hintTextView;
    private boolean hintTextDisplayed = true;
    // Build 110 - Add to existing sessions
    private UUID oldGroupID;
    // Build 139 - Second Group
    private UUID oldGroup2ID;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.edit_case, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (((ListActivity) getActivity()).getMode() == Document.Mode.NEW) {
            isNewMode = true;
        }
        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        if (isNewMode) {
            toolbar.setTitle(getString(R.string.app_name) + " - New Case");
        } else {
            toolbar.setTitle(getString(R.string.app_name) + " - Edit Case");
        }

        // Hide the FAB
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        fab.setVisibility(View.GONE);

        // Clear the footer
        TextView footer = getActivity().findViewById(R.id.footer);
        footer.setText("");

        // Set up the hint text
        hintTextView = getActivity().findViewById(R.id.hint_text);
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

        // Get the document to be edited from the activity
        client = ((ListActivity) getActivity()).getClient();
        editDocument = (Case) ((ListActivity) getActivity()).getDocument();
        localDB = LocalDB.getInstance();


        // CANCEL BOX
        if (editDocument.getCancelledFlag()) {
            LinearLayout cancelBoxView = parent.findViewById(R.id.cancel_box_layout);
            cancelBoxView.setVisibility(View.VISIBLE);
            TextView cancelBy = parent.findViewById(R.id.cancel_by);
            String byText = "by ";
            User cancelUser = localDB.getUser(editDocument.getCancelledByID());
            byText += cancelUser.getFullName() + " on ";
            byText += sDate.format(editDocument.getCancellationDate());
            cancelBy.setText(byText);
            TextView cancelReason = parent.findViewById(R.id.cancel_reason);
            cancelReason.setText(String.format("Reason: %s", editDocument.getCancellationReason()));
        }

        caseTypeSpinner = parent.findViewById(R.id.case_type_spinner);
        referenceDateView = parent.findViewById(R.id.reference_date);
        clientRedIcon = parent.findViewById(R.id.client_red_icon);
        clientAmberIcon = parent.findViewById(R.id.client_amber_icon);
        clientGreenIcon = parent.findViewById(R.id.client_green_icon);
        caseSummaryView = parent.findViewById(R.id.case_summary);
        overdueThresholdView = parent.findViewById(R.id.overdue_threshold);
        tierSpinner = parent.findViewById(R.id.tier_spinner);
        groupSpinner = parent.findViewById(R.id.group_spinner);
        // Build 139 - Second Group
        group2Spinner = parent.findViewById(R.id.group2_spinner);
        keyworkerSpinner = parent.findViewById(R.id.keyworker_spinner);
        coworker1Spinner = parent.findViewById(R.id.coworker1_spinner);
        coworker2Spinner = parent.findViewById(R.id.coworker2_spinner);
        commissionerSpinner = parent.findViewById(R.id.comissioner_spinner);
        transportRequiredSpinner = parent.findViewById(R.id.transport_required_spinner);
        specialInstructionsView = parent.findViewById(R.id.specialInstructions);
        // Build 105
        photographyConsentCheckbox = parent.findViewById(R.id.photography_consent_flag);
        doNotInviteCheckbox = parent.findViewById(R.id.do_not_invite_flag);
        // Build 139 - Second Group
        doNotInvite2Checkbox = parent.findViewById(R.id.do_not_invite2_flag);

        // Set the 'local' labels
        final LocalSettings localSettings = LocalSettings.getInstance(getActivity());
        TextView tierLabel = parent.findViewById(R.id.tier_label_text);
        tierLabel.setText(localSettings.Tier);
        TextView groupLabel = parent.findViewById(R.id.group_label_text);
        groupLabel.setText(localSettings.Group);
        // Build 139 - Second Group
        TextView group2Label = parent.findViewById(R.id.group2_label_text);
        group2Label.setText(localSettings.Group + "2");
        TextView keyworkerLabel = parent.findViewById(R.id.keyworker_label_text);
        keyworkerLabel.setText(localSettings.Keyworker);
        TextView coworker1Label = parent.findViewById(R.id.coworker1_label_text);
        coworker1Label.setText(localSettings.Coworker1);
        TextView coworker2Label = parent.findViewById(R.id.coworker2_label_text);
        coworker2Label.setText(localSettings.Coworker2);
        final TextView commissionerLabel = parent.findViewById(R.id.commissioner_label_text);
        commissionerLabel.setText(localSettings.Commisioner);
        // Build 105
        TextView photographyConsentLabel = parent.findViewById(R.id.photography_consent_label_text);

        // Initialise the status icons
        clientRedIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientStatus = Case.RED;
                clientRedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red));
                clientAmberIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber_grey));
                clientGreenIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green_grey));
                overdueThresholdView.setText(String.valueOf(localSettings.RedThreshold));
            }
        });
        clientAmberIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientStatus = Case.AMBER;
                clientRedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red_grey));
                clientAmberIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber));
                clientGreenIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green_grey));
                overdueThresholdView.setText(String.valueOf(localSettings.AmberThreshold));
            }
        });
        clientGreenIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientStatus = Case.GREEN;
                clientRedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red_grey));
                clientAmberIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber_grey));
                clientGreenIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green));
                overdueThresholdView.setText(String.valueOf(localSettings.GreenThreshold));
            }
        });

        // Enable long-press to inherit previous case summary
        caseSummaryView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (client.getCurrentCase() != null) {
                    Case currentCase = client.getCurrentCase();
                    String currentText = caseSummaryView.getText().toString();
                    caseSummaryView.setText(currentText + currentCase.getCaseSummary());
                }
                return true;
            }
        });

        // Initialise the case Type Spinner
        final ArrayAdapter<String> caseTypeAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, Case.caseTypes);
        caseTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        caseTypeSpinner.setAdapter(caseTypeAdapter);
        caseTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = caseTypeAdapter.getItem(position);
                if (selected != null) {
                    switch (selected) {
                        case "Start":
                        case "Update":
                            break;
                        case "Close":
                        case "Reject":
                            overdueThresholdView.setText("");
                            tierSpinner.setSelection(0);
                            groupSpinner.setSelection(0);
                            // Build 139 - Second Group
                            group2Spinner.setSelection(0);
                            keyworkerSpinner.setSelection(0);
                            coworker1Spinner.setSelection(0);
                            coworker2Spinner.setSelection(0);
                            commissionerSpinner.setSelection(0);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        referenceDateView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                referenceDatePicker();
                return true;
            }
        });

        // Initialise the Tier Spinner
        tierPickList = new PickList(localDB, ListType.TIER);
        ArrayAdapter<String> tierAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, tierPickList.getOptions());
        tierAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tierSpinner.setAdapter(tierAdapter);
        tierSpinner.setSelection(tierPickList.getDefaultPosition());

        // Initialise the Group Spinner
        groupPickList = new GroupPickList(localDB);
        ArrayAdapter<String> groupAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, groupPickList.getOptions());
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupAdapter);
        groupSpinner.setSelection(groupPickList.getDefaultPosition());

        // Build 139 - Second Group
        // Initialise the Group 2 Spinner
        group2PickList = new GroupPickList(localDB);
        ArrayAdapter<String> group2Adapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, group2PickList.getOptions());
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        group2Spinner.setAdapter(group2Adapter);
        group2Spinner.setSelection(group2PickList.getDefaultPosition());

        // Initialise the Keyworker/Co-worker Spinners
        ArrayList<User> users = localDB.getAllUsers();
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

        coworker1PickList = new PickList(users);
        ArrayAdapter<String> coworker1Adapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, coworker1PickList.getOptions());
        coworker1Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        coworker1Spinner.setAdapter(coworker1Adapter);
        coworker1Spinner.setSelection(coworker1PickList.getDefaultPosition());

        coworker2PickList = new PickList(users);
        ArrayAdapter<String> coworker2Adapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, coworker2PickList.getOptions());
        coworker2Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        coworker2Spinner.setAdapter(coworker2Adapter);
        coworker2Spinner.setSelection(coworker2PickList.getDefaultPosition());

        commissionerPickList = new PickList(localDB, ListType.COMMISSIONER);
        ArrayAdapter<String> commissionerAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, commissionerPickList.getOptions());
        commissionerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        commissionerSpinner.setAdapter(commissionerAdapter);
        commissionerSpinner.setSelection(commissionerPickList.getDefaultPosition());

        // Initialise the case Type Spinner
        final ArrayAdapter<String> transportRequiredAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, Case.transportRequirements);
        transportRequiredAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transportRequiredSpinner.setAdapter(transportRequiredAdapter);

        // Cancel Button
        Button cancelButton = parent.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Cancel so no need to update list of documents
                ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.popBackStack();
            }
        });
        // Save Button
        Button saveButton = parent.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {
                    // Deal with future invites if this is a new Case document and the group
                    // has changed (or is being set for the first time, Case Start)
                    if (isNewMode) {

                        // Build 125 - 18/07/2019 The following build 110 change is not relevant
                        // for a new 'reject' case document and will crash since oldGroupID will
                        //be null and editDocument.getGroupID() will also be null. However, there
                        // is a weird case where a user is changing an active case to rejected.
                        // Though this is incorrect use to CRIS, it should cancel any future
                        // sessions, so rather than checking the newCaseType, it is better to
                        // check for a null editDocument.getGroupID()

                        // Build 110 - The following code deals with invalidated invites to
                        // existing future sessions. First, get the list of future sessions
                        ArrayList<Session> futureSessionList = localDB.getFutureSessions();
                        // Find sessions for the old  and new groups
                        final ArrayList<Session> oldGroupSessions = new ArrayList<>();
                        final ArrayList<Session> newGroupSessions = new ArrayList<>();
                        // Build 139 - Modify code to handle 2 groups
                        boolean manageInvalidatedSessions = false;

                        if (editDocument.getGroupID() != null) {
                            UUID newGroupID = editDocument.getGroupID();
                            if (oldGroupID == null ||
                                    !newGroupID.equals(oldGroupID)) {
                                manageInvalidatedSessions = true;
                                // Identify the sessions potentially invalidated
                                for (Session session : futureSessionList) {
                                    if (session.getGroupID().equals(newGroupID)) {
                                        newGroupSessions.add(session);
                                    } else if (oldGroupID != null && session.getGroupID().equals(oldGroupID)) {
                                        oldGroupSessions.add(session);
                                    }
                                }
                            }
                        }
                        // Build 139 - Handle second group by adding to the session arrays

                        if (editDocument.getGroup2ID() != null) {
                             UUID newGroup2ID = editDocument.getGroup2ID();
                            if (oldGroup2ID == null ||
                                    !newGroup2ID.equals(oldGroup2ID)) {
                                manageInvalidatedSessions = true;
                                // Identify the sessions potentially invalidated
                                for (Session session : futureSessionList) {
                                    if (session.getGroupID().equals(newGroup2ID)) {
                                        newGroupSessions.add(session);
                                    } else if (oldGroup2ID != null && session.getGroupID().equals(oldGroup2ID)) {
                                        oldGroupSessions.add(session);
                                    }
                                }
                            }
                        }
                        // Check whether there are invalidated sessions and process
                        if (manageInvalidatedSessions) {
                            // First deal with future sessions of the old group(s)
                            if (oldGroupSessions.size() > 0) {
                                // Get all client sessions for all future sessions of the old group(s)
                                ArrayList<ClientSession> oldClientSessions =
                                        localDB.getAllClientSessions(oldGroupSessions,
                                                new Date(Long.MIN_VALUE),
                                                new Date(Long.MAX_VALUE));

                                // And cancel any that exist for this client
                                for (ClientSession clientSession : oldClientSessions) {
                                    if (clientSession.getClientID().equals(client.getClientID())) {
                                        // If client session was already cancelled, do not cancel
                                        // again so that the original reason is preserved
                                        if (!clientSession.getCancelledFlag()) {
                                            // Cancel this ClientSession
                                            clientSession.setCancellationDate(new Date());
                                            clientSession.setCancellationReason("Client moved to different group");
                                            clientSession.setCancelledByID(((ListActivity) getActivity()).getCurrentUser().getUserID());
                                            clientSession.setCancelledFlag(true);
                                            clientSession.setReserved(false);
                                            clientSession.save(false);
                                        }
                                    }
                                }
                            }
                            // Now, deal with the future sessions of the new group
                            if (newGroupSessions.size() > 0) {
                                // Get all client sessions for all future sessions of the new group
                                ArrayList<ClientSession> newClientSessions =
                                        localDB.getAllClientSessions(newGroupSessions,
                                                new Date(Long.MIN_VALUE),
                                                new Date(Long.MAX_VALUE));
                                // and check that this client isn't already invited to any future sessions for the new group
                                for (ClientSession clientSession : newClientSessions) {
                                    if (clientSession.getClientID().equals(client.getClientID())) {
                                        // Remove session from newGroupSessions
                                        for (Session session : newGroupSessions) {
                                            if (session.getDocumentID().equals(clientSession.getSessionID())) {
                                                newGroupSessions.remove(session);
                                                break;
                                            }
                                        }
                                        // It is possible that new group client sessions could be
                                        // cancelled, if so, uncancel
                                        if (clientSession.getCancelledFlag()) {
                                            clientSession.setCancellationDate(null);
                                            clientSession.setCancellationReason("");
                                            clientSession.setCancelledByID(null);
                                            clientSession.setCancelledFlag(false);
                                            // And save the change
                                            clientSession.save(false);
                                        }
                                    }
                                }

                                // If there are still new sessions, see if the user wants to add invites
                                // Display the dialog unless both groups are 'do not invite
                                // Note: This may display a spurious dialog if all of the new group
                                // sessions are for the 'do not invite' group, but not really a problem
                                boolean displayDialog = false;
                                if (!editDocument.isDoNotInviteFlag()){
                                    displayDialog = true;
                                }
                                if (!editDocument.isDoNotInvite2Flag()){
                                    displayDialog = true;
                                }
                                if (displayDialog && newGroupSessions.size() > 0) {
                                    final User currentUser = ((ListActivity) getActivity()).getCurrentUser();

                                    String messageText = "";
                                    if (oldGroupID == null) {
                                        messageText = "Do you want invitations to be created for any " +
                                                "existing, future sessions for the client's new group(s)?";
                                    } else {
                                        messageText = "The client is moving to one or more new groups so invitations " +
                                                "to future sessions for the old group(s) have been automatically cancelled. " +
                                                "Do you want invitations to be created for any " +
                                                "existing, future sessions for the client's new group(s)?";
                                    }
                                    new AlertDialog.Builder(getActivity())
                                            .setTitle("Invite to Existing Sessions")
                                            .setMessage(messageText)
                                            .setPositiveButton("Invite to Sessions", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Create the new sessions
                                                    for (Session session : newGroupSessions) {
                                                        // Only create if 'do not invite' is false
                                                        boolean createSession = false;
                                                        UUID newGroupID = editDocument.getGroupID();
                                                        if (newGroupID != null &&
                                                                session.getGroupID().equals(newGroupID) &&
                                                                !editDocument.isDoNotInviteFlag()){
                                                            createSession = true;
                                                        }
                                                        UUID newGroup2ID = editDocument.getGroup2ID();
                                                        if (newGroup2ID != null &&
                                                                session.getGroupID().equals(newGroup2ID) &&
                                                                !editDocument.isDoNotInvite2Flag()){
                                                            createSession = true;
                                                        }
                                                        if (createSession) {
                                                            ClientSession clientSession = new ClientSession(
                                                                    currentUser,
                                                                    client.getClientID());
                                                            clientSession.setSessionID(session.getDocumentID());
                                                            clientSession.setReferenceDate(session.getReferenceDate());
                                                            clientSession.setSession(session);
                                                            clientSession.setAttended(false);
                                                            clientSession.setReserved(false);
                                                            clientSession.setCancelledFlag(false);
                                                            clientSession.save(true);
                                                        }

                                                    }
                                                    editDocument.save(isNewMode);
                                                    FragmentManager fragmentManager = getFragmentManager();
                                                    fragmentManager.popBackStack();
                                                }
                                            })
                                            .setNegativeButton("Do Not Invite", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    editDocument.save(isNewMode);
                                                    FragmentManager fragmentManager = getFragmentManager();
                                                    fragmentManager.popBackStack();
                                                }
                                            })
                                            .show();
                                } else {
                                    editDocument.save(isNewMode);
                                    FragmentManager fragmentManager = getFragmentManager();
                                    fragmentManager.popBackStack();
                                }
                            } else {
                                editDocument.save(isNewMode);
                                FragmentManager fragmentManager = getFragmentManager();
                                fragmentManager.popBackStack();
                            }
                        } else {
                            editDocument.save(isNewMode);
                            FragmentManager fragmentManager = getFragmentManager();
                            fragmentManager.popBackStack();
                        }

                    } else {
                        editDocument.save(isNewMode);
                        FragmentManager fragmentManager = getFragmentManager();
                        fragmentManager.popBackStack();
                    }
                }
            }
        });

        // Set Defaults
        Date today = new Date();

        if (client.getCurrentCase() == null) {
            caseTypeSpinner.setSelection(caseTypeAdapter.getPosition("Start"));
            referenceDateView.setText(sDate.format(today));
            overdueThresholdView.setText("");
            transportRequiredSpinner.setSelection(caseTypeAdapter.getPosition("No"));
            photographyConsentCheckbox.setChecked(false);
            doNotInviteCheckbox.setChecked(false);
            doNotInvite2Checkbox.setChecked(false);
        } else {
            if (isNewMode) {
                referenceDateView.setText(sDate.format(today));
                Case currentCase = client.getCurrentCase();
                // Transport requirements are persistent
                transportRequiredSpinner.setSelection(transportRequiredAdapter.getPosition(currentCase.getTransportRequired()));
                specialInstructionsView.setText(currentCase.getTransportSpecialInstructions());
                switch (currentCase.getCaseType()) {
                    case "Start":
                    case "Update":
                        photographyConsentCheckbox.setChecked(currentCase.isPhotographyConsentFlag());
                        doNotInviteCheckbox.setChecked(currentCase.isDoNotInviteFlag());
                        caseTypeSpinner.setSelection(caseTypeAdapter.getPosition("Update"));
                        overdueThresholdView.setText(String.format(Locale.UK, "%d", currentCase.getOverdueThreshold()));
                        tierSpinner.setSelection(tierPickList.getPosition(currentCase.getTier()));
                        groupSpinner.setSelection(groupPickList.getPosition(currentCase.getGroup()));
                        // Build 110 - Save current group for managing group changes
                        oldGroupID = currentCase.getGroupID();
                        // Build 139 - Second Group
                        if (currentCase.getGroup2ID() != null){
                            group2Spinner.setSelection(groupPickList.getPosition(currentCase.getGroup2()));
                            oldGroup2ID = currentCase.getGroup2ID();
                            doNotInvite2Checkbox.setChecked(currentCase.isDoNotInvite2Flag());
                        }
                        keyworkerSpinner.setSelection(keyworkerPickList.getPosition(currentCase.getKeyWorker()));
                        coworker1Spinner.setSelection(coworker1PickList.getPosition(currentCase.getCoWorker1()));
                        coworker2Spinner.setSelection(coworker2PickList.getPosition(currentCase.getCoWorker2()));
                        commissionerSpinner.setSelection(commissionerPickList.getPosition(currentCase.getCommissioner()));
                        clientRedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red_grey));
                        clientAmberIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber_grey));
                        clientGreenIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green_grey));
                        switch (currentCase.getClientStatus()) {
                            case Case.RED:
                                clientStatus = Case.RED;
                                clientRedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red));
                                break;
                            case Case.AMBER:
                                clientStatus = Case.AMBER;
                                clientAmberIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber));
                                break;
                            case Case.GREEN:
                                clientStatus = Case.GREEN;
                                clientGreenIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green));
                        }
                        break;
                    case "Close":
                    case "Reject":
                        photographyConsentCheckbox.setChecked(false);
                        doNotInviteCheckbox.setChecked(false);
                        caseTypeSpinner.setSelection(caseTypeAdapter.getPosition("Start"));
                }
            } else {
                // Edit Mode
                // Build 171 - Handle unexpected null ListItem ID
                if (!editDocument.getCaseType().equals("Unknown")) {
                    caseTypeSpinner.setSelection(caseTypeAdapter.getPosition(editDocument.getCaseType()));
                }
                referenceDateView.setText(sDate.format(editDocument.getReferenceDate()));
                caseSummaryView.setText((editDocument.getCaseSummary()));
                photographyConsentCheckbox.setChecked(editDocument.isPhotographyConsentFlag());
                overdueThresholdView.setText(String.format(Locale.UK, "%d", editDocument.getOverdueThreshold()));
                tierSpinner.setSelection(tierPickList.getPosition(editDocument.getTier()));
                groupSpinner.setSelection(groupPickList.getPosition(editDocument.getGroup()));
                // Build 110 - Save current group for managing group changes
                oldGroupID = editDocument.getGroupID();
                doNotInviteCheckbox.setChecked(editDocument.isDoNotInviteFlag());
                // Build 139 - Second Group
                if (editDocument.getGroup2ID() != null){
                    group2Spinner.setSelection(groupPickList.getPosition(editDocument.getGroup2()));
                    oldGroup2ID = editDocument.getGroup2ID();
                    doNotInvite2Checkbox.setChecked(editDocument.isDoNotInvite2Flag());
                }

                keyworkerSpinner.setSelection(keyworkerPickList.getPosition(editDocument.getKeyWorker()));
                coworker1Spinner.setSelection(coworker1PickList.getPosition(editDocument.getCoWorker1()));
                coworker2Spinner.setSelection(coworker2PickList.getPosition(editDocument.getCoWorker2()));
                commissionerSpinner.setSelection(commissionerPickList.getPosition(editDocument.getCommissioner()));
                clientRedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red_grey));
                clientAmberIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber_grey));
                clientGreenIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green_grey));
                clientStatus = editDocument.getClientStatus();
                switch (clientStatus) {
                    case Case.RED:
                        clientRedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red));
                        break;
                    case Case.AMBER:
                        clientAmberIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber));
                        break;
                    case Case.GREEN:
                        clientGreenIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green));
                }
                transportRequiredSpinner.setSelection(transportRequiredAdapter.getPosition(editDocument.getTransportRequired()));
                specialInstructionsView.setText(editDocument.getTransportSpecialInstructions());
            }
        }
    }

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
        MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        shareOption.setVisible(false);
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
                                    editDocument.save(isNewMode);
                                    FragmentManager fragmentManager = getFragmentManager();
                                    fragmentManager.popBackStack();
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
                editDocument.save(isNewMode);
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.popBackStack();
            }
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

    private void referenceDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog startDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                referenceDateView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        startDatePickerDialog.show();
    }

    // Validate the document
    private boolean validate() {
        boolean success = true;

        // Clear any existing errors

        referenceDateView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        //Case Type (No please select, so a value must exist)
        String newCaseType = caseTypeSpinner.getSelectedItem().toString();
        editDocument.setCaseType(newCaseType);
        editDocument.setClientStatus(clientStatus);

        // ReferenceDate
        String sReferenceDate = referenceDateView.getText().toString();
        if (TextUtils.isEmpty(sReferenceDate)) {
            referenceDateView.setError(getString(R.string.error_field_required));
            focusView = referenceDateView;
            success = false;
        } else {
            Date dReferenceDate = CRISUtil.parseDate(sReferenceDate);
            if (dReferenceDate == null) {
                referenceDateView.setError(getString(R.string.error_invalid_date));
                focusView = referenceDateView;
                success = false;
            } else {
                // Date must not be in the future
                Date today = new Date();
                if (today.before(dReferenceDate)) {
                    referenceDateView.setError(getString(R.string.error_date_in_future));
                    focusView = referenceDateView;
                    success = false;
                } else {
                    editDocument.setReferenceDate(dReferenceDate);
                }
            }
        }

        // CaseSummary
        if (caseSummaryView.getText().toString().isEmpty()) {
            editDocument.setCaseSummary("");
        } else {
            editDocument.setCaseSummary(caseSummaryView.getText().toString());
        }

        // PhotographyConsent
        editDocument.setPhotographyConsentFlag(photographyConsentCheckbox.isChecked());

        // OverdueThreshold
        String sOverdueThreshold = overdueThresholdView.getText().toString().trim();
        if (TextUtils.isEmpty(sOverdueThreshold)) {
            if (newCaseType.equals("Close") || newCaseType.equals("Reject")) {
                editDocument.setOverdueThreshold(0);
            } else {
                overdueThresholdView.setError(getString(R.string.error_field_required));
                focusView = overdueThresholdView;
                success = false;
            }
        } else {
            int overdueThreshold;
            try {
                overdueThreshold = Integer.parseInt(sOverdueThreshold);
                if (overdueThreshold <= 0) {
                    overdueThresholdView.setError(getString(R.string.error_number_not_positive));
                    focusView = overdueThresholdView;
                    success = false;
                } else {
                    editDocument.setOverdueThreshold(overdueThreshold);
                }
            } catch (Exception ex) {
                overdueThresholdView.setError(getString(R.string.error_invalid_integer));
                focusView = overdueThresholdView;
                success = false;
            }

        }
        //Tier
        ListItem newTier = tierPickList.getListItems().get(tierSpinner.getSelectedItemPosition());
        // Test for Please select
        if (newTier.getItemOrder() == -1) {
            if (newCaseType.equals("Close") || newCaseType.equals("Reject")) {
                editDocument.setTierID(null);
                editDocument.setTier(null);
            } else {
                TextView errorText = (TextView) tierSpinner.getSelectedView();
                //errorText.setError("anything here, just to add the icon");
                errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                focusView = tierSpinner;
                success = false;
            }
        } else {
            editDocument.setTierID(newTier.getListItemID());
            // PickList contained
            editDocument.setTier(localDB.getListItem(editDocument.getTierID()));
        }
        //Group
        ListItem newGroup = groupPickList.getListItems().get(groupSpinner.getSelectedItemPosition());
        // Test for Please select
        if (newGroup.getItemOrder() == -1) {
            if (newCaseType.equals("Close") || newCaseType.equals("Reject")) {
                editDocument.setGroupID(null);
                editDocument.setGroup(null);
            } else {
                TextView errorText = (TextView) groupSpinner.getSelectedView();
                //errorText.setError("anything here, just to add the icon");
                errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                focusView = groupSpinner;
                success = false;
            }
        } else {
            editDocument.setGroupID(newGroup.getListItemID());
            // PickList contained
            editDocument.setGroup(localDB.getListItem(editDocument.getGroupID()));
        }

        // DoNotInvite
        editDocument.setDoNotInviteFlag(doNotInviteCheckbox.isChecked());

        // Build 139 - Second Group
        //Group 2 (Not mandatory)
        ListItem newGroup2 = group2PickList.getListItems().get(group2Spinner.getSelectedItemPosition());
        if (newGroup2.getItemOrder() != -1) {
            // Check for duplicate Group1/Group2
            if (newGroup.getListItemID().equals(newGroup2.getListItemID())) {
                TextView errorText = (TextView) group2Spinner.getSelectedView();
                //errorText.setError("anything here, just to add the icon");
                errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                focusView = group2Spinner;
                success = false;
            } else {
                editDocument.setGroup2ID(newGroup2.getListItemID());
                // PickList contained
                editDocument.setGroup2(localDB.getListItem(editDocument.getGroup2ID()));
            }
        }

        // DoNotInvite2
        editDocument.setDoNotInvite2Flag(doNotInvite2Checkbox.isChecked());

        //Keyworker
        User newKeyworker = keyworkerPickList.getUsers().get(keyworkerSpinner.getSelectedItemPosition());
        // Test for Please select
        if (newKeyworker.getUserID().equals(User.unknownUser)) {
            if (newCaseType.equals("Close") || newCaseType.equals("Reject")) {
                editDocument.setKeyWorkerID(null);
                editDocument.setKeyWorker(null);
            } else {
                TextView errorText = (TextView) keyworkerSpinner.getSelectedView();
                //errorText.setError("anything here, just to add the icon");
                errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                focusView = keyworkerSpinner;
                success = false;
            }
        } else {
            editDocument.setKeyWorkerID(newKeyworker.getUserID());
            // PickList contained
            editDocument.setKeyWorker(localDB.getUser(editDocument.getKeyWorkerID()));
        }

        //Coworker 1 (Not mandatory)
        User newCoworker1 = coworker1PickList.getUsers().get(coworker1Spinner.getSelectedItemPosition());
        if (!newCoworker1.getUserID().equals(User.unknownUser)) {
            editDocument.setCoWorker1ID(newCoworker1.getUserID());
            // PickList contained
            editDocument.setCoWorker1(localDB.getUser(editDocument.getCoWorker1ID()));
        }

        //Coworker 2 (Not mandatory)
        User newCoworker2 = coworker2PickList.getUsers().get(coworker2Spinner.getSelectedItemPosition());
        if (!newCoworker2.getUserID().equals(User.unknownUser)) {
            editDocument.setCoWorker2ID(newCoworker2.getUserID());
            // PickList contained
            editDocument.setCoWorker2(localDB.getUser(editDocument.getCoWorker2ID()));
        }

        //Commissioner
        ListItem newCommissioner = commissionerPickList.getListItems().get(commissionerSpinner.getSelectedItemPosition());
        // Test for Please select
        if (newCommissioner.getItemOrder() == -1) {
            if (newCaseType.equals("Close") || newCaseType.equals("Reject")) {
                editDocument.setCommissionerID(null);
                editDocument.setCommissioner(null);
            } else {
                TextView errorText = (TextView) commissionerSpinner.getSelectedView();
                //errorText.setError("anything here, just to add the icon");
                errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                focusView = commissionerSpinner;
                success = false;
            }
        } else {
            editDocument.setCommissionerID(newCommissioner.getListItemID());
            // PickList contained
            editDocument.setCommissioner(localDB.getListItem(editDocument.getCommissionerID()));
        }

        //Transport Required (No please select, so a value must exist)
        String newTransportRequired = transportRequiredSpinner.getSelectedItem().toString();
        editDocument.setTransportRequired(newTransportRequired);
        editDocument.setTransportSpecialInstructions(specialInstructionsView.getText().toString().trim());

        if (!success) {
            focusView.requestFocus();
            focusView.requestFocusFromTouch();
        }
        return success;
    }

    private String getHintText() {
        return "The Overdue Threshold should be set to the number of days after which a " +
                "review should have been carried out. The default values are: 7 days for " +
                "a 'red' client, 28 days for an 'amber' client and 90 days for a 'green' " +
                "client. Changing the status of the client will automatically change the " +
                "overdue threshold value but it may then be modified to any positive number. \n\n" +
                "If a note is not added to the client's record before the overdue threshold, " +
                "the client will appear in the 'overdue' view in My Clients or All Clients " +
                "showing how overdue the result is. \n\n" +
                "Note: this is different to the 'Last entry' value which is also shown on " +
                "these two views. The Last entry is based on documents of any type, whereas " +
                "the overdue facility only checks for 'note' documents.";

    }
}
