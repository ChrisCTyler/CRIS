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

// Build 200 Use the androidX Fragment class
//import android.app.FragmentManager;
//import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.Document;
import solutions.cris.object.Group;
import solutions.cris.object.ListItem;
import solutions.cris.object.Session;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;
import solutions.cris.utils.LocalSettings;
import solutions.cris.utils.PickList;
import solutions.cris.utils.PickListDialogFragment;

public class ListSessionClients extends ListActivity implements PickListDialogFragment.PickListDialogListener {

    public static final String EDIT_DOCUMENT = "solutions.cris.EditDocument";
    public static final String DOCUMENT_MODE = "solutions.cris.DocumentMode";

    private TextView infoTextView;
    private String infoText = "";
    private boolean infoTextDisplayed = true;
    private boolean editMyWeek = false;

    String keyworkerContact = "";

    // Build 160 - There are error reports where myClients becomes null. I cannot reproduce the
    // error so don't know why, but try presetting in the declaration to see if it helps
    //private boolean myClients;
    private boolean myClients = true;

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

    public void clearSelectedValues(){
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
        setSelectMode(((PickListDialogFragment)dialog).getSelectMode());
        // Clear and the load the selectedIDs array
        CheckBox checkBoxes[] = ((PickListDialogFragment)dialog).getCheckBoxes();
        PickList pickList = ((PickListDialogFragment)dialog).getPickList();
        selectedIDs.clear();
        for (int i = 0; i < checkBoxes.length; i++) {
            if (checkBoxes[i].isChecked()) {
                // Keyworkers are a special case, list of users, not ListItems
                if (getSelectMode() == SelectMode.KEYWORKERS){
                    User keyWorker = ((User)pickList.getObjects().get(i));
                    selectedIDs.add(keyWorker.getUserID());
                    addToSelectedValues(keyWorker.getFullName());
                } else {
                    ListItem listItem = pickList.getListItems().get(i);
                    selectedIDs.add(listItem.getListItemID());
                    addToSelectedValues(listItem.getItemValue());
                }
            }
        }
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("ListSessionClientsFragment");
        if (fragment != null && fragment.isVisible()) {
            ((ListSessionClientsFragment) fragment).pickListDialogFragmentOK();
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
            setContentView(R.layout.activity_list_session_clients);
            setToolbar((Toolbar) findViewById(R.id.toolbar));
            getToolbar().setTitle(getString(R.string.app_name) + " - Session");
            setSupportActionBar(getToolbar());

            setFab((FloatingActionButton) findViewById(R.id.fab));
            LocalDB localDB = LocalDB.getInstance();
            setSession((Session) getIntent().getSerializableExtra(Main.EXTRA_DOCUMENT));

            // Set up the additionalInfo text
            if (getSession().getOtherStaffIDList() != null &&
                    getSession().getOtherStaffIDList().size() > 0) {
                for (UUID otherStaffID : getSession().getOtherStaffIDList()) {
                    User otherStaff = localDB.getUser(otherStaffID);
                    infoText += String.format("%s (%s)\n", otherStaff.getFullName(), otherStaff.getContactNumber());
                }
            }
            if (!getSession().getAdditionalInformation().isEmpty()) {
                infoText += String.format("\n%s", getSession().getAdditionalInformation());
            }
            infoTextView = findViewById(R.id.hint_text);
            if (infoText.isEmpty()) {
                LinearLayout hintBox = findViewById(R.id.hint_box);
                hintBox.setVisibility(View.GONE);
            }
            // Restore value of hintDisplayed (Set to opposite, toggle to come
            if (savedInstanceState != null) {
                infoTextDisplayed = !savedInstanceState.getBoolean(Main.HINT_DISPLAYED);
            }
            toggleHint();
            infoTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleHint();
                }
            });

            // Display the header details
            loadSessionHeader(getSession());
            // Check whether we're recreating a previously destroyed instance
            if (savedInstanceState != null) {
                // Restore value of members from saved state
                setDocument((Document) savedInstanceState.getSerializable(EDIT_DOCUMENT));
                String sMode = savedInstanceState.getString(DOCUMENT_MODE);
                setMode(Document.Mode.valueOf(sMode));

            } else {
                // Start the List Documents fragment
                //FragmentManager fragmentManager = getFragmentManager();
                //FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                //ListSessionClientsFragment fragment = new ListSessionClientsFragment();
                //fragmentTransaction.add(R.id.content, fragment);
                //fragmentTransaction.commit();
                // Build 200 Use androidX fragment class
                Fragment fragment = new ListSessionClientsFragment();
                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.content, fragment, "ListSessionClientsFragment")
                        .commit();
                // V2.0 Preset mode to NEW (not READ) to force the dataload on first pass.
                setMode(Document.Mode.NEW);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // Build 110 - Removed share from Session views as not relevant
        //inflater.inflate(R.menu.menu_search_share, menu);
        // Build 186 Only one menu should be inflated since search is in both
        //inflater.inflate(R.menu.menu_search, menu);
        // Build 126 - Add share option so that Readfragments can offer a share
        //inflater.inflate(R.menu.menu_search, menu);
        inflater.inflate(R.menu.menu_search_share, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!editMyWeek){
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the state
        savedInstanceState.putBoolean(Main.HINT_DISPLAYED, infoTextDisplayed);
        savedInstanceState.putSerializable(EDIT_DOCUMENT, getDocument());
        savedInstanceState.putString(DOCUMENT_MODE, getMode().toString());
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private void toggleHint() {
        if (infoTextDisplayed) {
            infoTextView.setText(String.format("Other staff/Additional Information: (Touch to expand)\n\n%s", infoText));
            infoTextView.setMaxLines(2);
            infoTextDisplayed = false;
        } else {
            infoTextView.setText(String.format("Other staff/Additional Information: (Touch to collapse)\n\n%s", infoText));
            infoTextDisplayed = true;
            infoTextView.setMaxLines(infoTextView.getLineCount());
        }
    }


    public void setEditMyWeek(boolean editMyWeek) {
        this.editMyWeek = editMyWeek;
    }

    private void composeEmail(String[] addresses){
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, "CRIS");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void dialPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void loadSessionHeader(Session session) {
        LocalSettings localSettings = LocalSettings.getInstance(this);

        LinearLayout sessionHeaderLayout = findViewById(R.id.header);
        LinearLayout clientHeaderLayout = findViewById(R.id.header_client);
        LinearLayout hintBox = findViewById(R.id.hint_box);
        ImageView headerIcon = findViewById(R.id.header_icon);
        TextView nameView = findViewById(R.id.header_name);
        TextView dateView = findViewById(R.id.header_date);
        TextView addressView = findViewById(R.id.header_address);
        TextView postcodeView = findViewById(R.id.header_postcode);
        TextView sessionCoordinatorView = findViewById(R.id.header_session_cooordinator);
        TextView sessionCoordinatorLabel = findViewById(R.id.header_session_coordinator_label);
        TextView keyworkerView = findViewById(R.id.header_keyworker);
        TextView keyworkerLabel = findViewById(R.id.header_keyworker_label);

        clientHeaderLayout.setVisibility(View.GONE);
        sessionHeaderLayout.setVisibility(View.VISIBLE);
        hintBox.setVisibility(View.VISIBLE);
        headerIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_sessions));
        nameView.setText(String.format("%s ", session.getSessionName()));
        SimpleDateFormat sDate = new SimpleDateFormat("EEE d MMM yyyy HH:mm", Locale.UK);
        dateView.setText(sDate.format(session.getReferenceDate()));
        addressView.setText(session.getAddress());
        postcodeView.setText(session.getPostcode());
        sessionCoordinatorLabel.setText(String.format("%s: ", localSettings.SessionCoordinator));
        sessionCoordinatorView.setText(String.format("%s (%s)",
                session.getSessionCoordinator().getFullName(),
                session.getSessionCoordinator().getContactNumber()));
        keyworkerLabel.setText(String.format("%s: ", localSettings.Keyworker));
        keyworkerView.setText(String.format("%s (%s)",
                session.getKeyWorker().getFullName(),
                session.getKeyWorker().getContactNumber()));
    }

    public void loadClientHeader(Client client) {

        // Calculate client's age
        Calendar dob = Calendar.getInstance();
        dob.setTime(client.getDateOfBirth());
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        int age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        LocalSettings localSettings = LocalSettings.getInstance(this);

        LinearLayout sessionHeaderLayout = findViewById(R.id.header);
        LinearLayout clientHeaderLayout = findViewById(R.id.header_client);
        LinearLayout hintBox = findViewById(R.id.hint_box);
        ImageView headerIcon = findViewById(R.id.header_icon);
        TextView firstNamesView = findViewById(R.id.header_first_names);
        TextView lastNameView = findViewById(R.id.header_last_name);
        TextView dateOfBirthView = findViewById(R.id.header_date_of_birth);
        TextView addressView = findViewById(R.id.header_client_address);
        TextView postcodeView = findViewById(R.id.header_client_postcode);
        final TextView contactNumberView = findViewById(R.id.header_contact_number);
        final TextView contactNumber2View = findViewById(R.id.header_contact_number2);
        final TextView emailView = findViewById(R.id.header_email_address);
        TextView genderView = findViewById(R.id.header_gender);
        TextView groupView = findViewById(R.id.header_group);
        TextView groupLabel = findViewById(R.id.header_label_group);
        TextView tierView = findViewById(R.id.header_tier);
        TextView tierLabel = findViewById(R.id.header_label_tier);
        final TextView keyworkerView = findViewById(R.id.header_client_keyworker);
        TextView keyworkerLabel = findViewById(R.id.header_label_keyworker);

        clientHeaderLayout.setVisibility(View.VISIBLE);
        sessionHeaderLayout.setVisibility(View.GONE);
        hintBox.setVisibility(View.GONE);

        emailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!emailView.getText().toString().isEmpty()) {
                    String[] addresses = new String[1];
                    addresses[0] = emailView.getText().toString();
                    composeEmail(addresses);
                }

            }
        });

        contactNumberView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!contactNumberView.getText().toString().isEmpty()) {
                    dialPhoneNumber(contactNumberView.getText().toString());
                }

            }
        });

        contactNumber2View.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!contactNumber2View.getText().toString().isEmpty()) {
                    dialPhoneNumber(contactNumber2View.getText().toString());
                }

            }
        });

        keyworkerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!keyworkerContact.isEmpty()) {
                    dialPhoneNumber(keyworkerContact);
                }

            }
        });

        if (client.getCurrentCase() == null) {
            headerIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_client_grey));
        } else {
            switch (client.getCurrentCase().getClientStatus()) {
                case Case.RED:
                    headerIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_client_red));
                    break;
                case Case.AMBER:
                    headerIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_client_amber));
                    break;
                case Case.GREEN:
                    headerIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_client_green));
                    break;
                default:
                    throw new CRISException(String.format(Locale.UK,
                            "Unexpected client status: %d", client.getCurrentCase().getClientStatus()));
            }
        }
        firstNamesView.setText(String.format("%s ", client.getFirstNames()));
        lastNameView.setText(client.getLastName());
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        dateOfBirthView.setText(String.format(Locale.UK,
                "%s (%d)", sDate.format(client.getDateOfBirth()), age));
        addressView.setText(client.getAddress());
        postcodeView.setText(client.getPostcode());
        contactNumberView.setText(client.getContactNumber());
        if (client.getContactNumber2() != null) {
            contactNumber2View.setText(client.getContactNumber2());
        }
        emailView.setText(client.getEmailAddress());
        //genderView.setText(client.getGender().getItemValue());
        // Build 170 - Instance of null gender found so handle the possibility
        ListItem gender = client.getGender();
        if (gender == null) {
            genderView.setText("Unknown");
        } else {
            genderView.setText(gender.getItemValue());
        }


        groupLabel.setText(String.format("%s: ", localSettings.Group));
        tierLabel.setText(String.format("%s: ", localSettings.Tier));
        keyworkerLabel.setText(String.format("%s: ", localSettings.Keyworker));

        if (client.getCurrentCase() != null) {
            // Unpick the current case
            String tier = "No Tier";
            String group = "No Group";
            String keyworkerName = "No Keyworker";
            Case currentCase = client.getCurrentCase();
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
            groupView.setText(group);
            tierView.setText(tier);
            String keyworkerText = keyworkerName;
            if (!keyworkerContact.isEmpty()) {
                keyworkerText += String.format(" (%s)", keyworkerContact);
            }
            keyworkerView.setText(keyworkerText);
        }
    }


}
