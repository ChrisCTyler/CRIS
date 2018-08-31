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
import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.Login;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.EditPdfDocument;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.CriteriaAssessmentTool;
import solutions.cris.object.Document;
import solutions.cris.object.Session;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;
import solutions.cris.utils.LocalSettings;

import static solutions.cris.list.ListLibrary.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE;

public class ListSessionClients extends ListActivity {

    public static final String EDIT_DOCUMENT = "solutions.cris.EditDocument";
    public static final String DOCUMENT_MODE = "solutions.cris.DocumentMode";

    private TextView infoTextView;
    private String infoText = "";
    private boolean infoTextDisplayed = true;
    private boolean editMyWeek = false;

    String keyworkerContact = "";

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
            infoTextView = (TextView) findViewById(R.id.hint_text);
            if (infoText.isEmpty()) {
                LinearLayout hintBox = (LinearLayout) findViewById(R.id.hint_box);
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
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                ListSessionClientsFragment fragment = new ListSessionClientsFragment();
                fragmentTransaction.add(R.id.content, fragment);
                fragmentTransaction.commit();
                // V2.0 Preset mode to NEW (not READ) to force the dataload on first pass.
                setMode(Document.Mode.NEW);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
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

        LinearLayout sessionHeaderLayout = (LinearLayout) findViewById(R.id.header);
        LinearLayout clientHeaderLayout = (LinearLayout) findViewById(R.id.header_client);
        LinearLayout hintBox = (LinearLayout)findViewById(R.id.hint_box);
        ImageView headerIcon = (ImageView) findViewById(R.id.header_icon);
        TextView nameView = (TextView) findViewById(R.id.header_name);
        TextView dateView = (TextView) findViewById(R.id.header_date);
        TextView addressView = (TextView) findViewById(R.id.header_address);
        TextView postcodeView = (TextView) findViewById(R.id.header_postcode);
        TextView sessionCoordinatorView = (TextView) findViewById(R.id.header_session_cooordinator);
        TextView sessionCoordinatorLabel = (TextView) findViewById(R.id.header_session_coordinator_label);
        TextView keyworkerView = (TextView) findViewById(R.id.header_keyworker);
        TextView keyworkerLabel = (TextView) findViewById(R.id.header_keyworker_label);

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

        LinearLayout sessionHeaderLayout = (LinearLayout) findViewById(R.id.header);
        LinearLayout clientHeaderLayout = (LinearLayout) findViewById(R.id.header_client);
        LinearLayout hintBox = (LinearLayout)findViewById(R.id.hint_box);
        ImageView headerIcon = (ImageView) findViewById(R.id.header_icon);
        TextView firstNamesView = (TextView) findViewById(R.id.header_first_names);
        TextView lastNameView = (TextView) findViewById(R.id.header_last_name);
        TextView dateOfBirthView = (TextView) findViewById(R.id.header_date_of_birth);
        TextView addressView = (TextView) findViewById(R.id.header_client_address);
        TextView postcodeView = (TextView) findViewById(R.id.header_client_postcode);
        final TextView contactNumberView = (TextView) findViewById(R.id.header_contact_number);
        final TextView contactNumber2View = (TextView) findViewById(R.id.header_contact_number2);
        final TextView emailView = (TextView) findViewById(R.id.header_email_address);
        TextView genderView = (TextView) findViewById(R.id.header_gender);
        TextView groupView = (TextView) findViewById(R.id.header_group);
        TextView groupLabel = (TextView) findViewById(R.id.header_label_group);
        TextView tierView = (TextView) findViewById(R.id.header_tier);
        TextView tierLabel = (TextView) findViewById(R.id.header_label_tier);
        final TextView keyworkerView = (TextView) findViewById(R.id.header_client_keyworker);
        TextView keyworkerLabel = (TextView) findViewById(R.id.header_label_keyworker);

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
        genderView.setText(client.getGender().getItemValue());

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
