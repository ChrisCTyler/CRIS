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

import android.content.Intent;
// Build 200 Use the androidX Fragment class
//import android.app.FragmentManager;
//import android.app.FragmentTransaction;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.CriteriaAssessmentTool;
import solutions.cris.object.Document;
import solutions.cris.object.ListItem;
import solutions.cris.object.MACAYC18;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;
import solutions.cris.utils.LocalSettings;
import solutions.cris.utils.PickList;
import solutions.cris.utils.PickListDialogFragment;

public class ListClientHeader extends ListActivity implements PickListDialogFragment.PickListDialogListener{

    private Menu menu;
    private ClientSession[] clientSessions = new ClientSession[5];

    // These variables are set by callbacks in fragments and need to be
    // restored via saved instance state to survive a tablet orientation reload
    public static final String IS_FOLLOWING_CLIENT = "solutions.cris.IsFollowingClient";
    public static final String EDIT_DOCUMENT = "solutions.cris.EditDocument";
    public static final String DOCUMENT_MODE = "solutions.cris.DocumentMode";

    private boolean isFollowingClient = false;

    //TextView emailView;
    //TextView contactNumberView;
    //TextView contactNumber2View;
    //TextView keyworkerView;
    String keyworkerContact = "";

    // Build 200 Move selectedIDs to here (from ListClientDocumentsFragment) and set following
    // positive button click in PickListDialogFragment
    private ArrayList<String> selectedDocuments;

    public ArrayList<String> getSelectedDocuments() {
        return selectedDocuments;
    }

    public void setSelectedDocuments(ArrayList<String> selectedDocuments) {
        this.selectedDocuments = selectedDocuments;
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
        selectedDocuments.clear();
        for (int i = 0; i < checkBoxes.length; i++) {
            if (checkBoxes[i].isChecked()) {
                String documentType = ((String)pickList.getObjects().get(i));
                selectedDocuments.add(documentType);
                addToSelectedValues(documentType);
            }
        }
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("ListClientDocumentsFragment");
        if (fragment != null && fragment.isVisible()) {
            ((ListClientDocumentsFragment) fragment).pickListDialogFragmentOK();
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
            setContentView(R.layout.activity_list_client_header);
            setToolbar((Toolbar) findViewById(R.id.toolbar));
            getToolbar().setTitle(getString(R.string.app_name));
            setSupportActionBar(getToolbar());

            setFab((FloatingActionButton) findViewById(R.id.fab));
            LocalDB localDB = LocalDB.getInstance();
            // Set the Client from the document passed in the intent
            setClient((Client) getIntent().getSerializableExtra(Main.EXTRA_DOCUMENT));
            if (localDB.isFollowing(getCurrentUser().getUserID(), getClient().getClientID())) {
                isFollowingClient = true;
            }

            // Check whether we're recreating a previously destroyed instance
            if (savedInstanceState != null) {
                // Restore value of members from saved state
                setDocument((Document) savedInstanceState.getSerializable(EDIT_DOCUMENT));
                isFollowingClient = savedInstanceState.getBoolean(IS_FOLLOWING_CLIENT);
                String sMode = savedInstanceState.getString(DOCUMENT_MODE);
                setMode(Document.Mode.valueOf(sMode));
                //Build 125 - No need to loadHeader because onResume in fragment will
                // now reload the adapter. See comment in ListClientDocumentFragment onActivityCreated
                loadHeader(getClient());
            } else {
                // Start the List Documents fragment
                // Build 200 Use the androidX Fragment class
                //FragmentManager fragmentManager = getFragmentManager();
                //FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                //ListClientDocumentsFragment fragment = new ListClientDocumentsFragment();
                //fragmentTransaction.add(R.id.content, fragment);
                //fragmentTransaction.commit();
                Fragment fragment = new ListClientDocumentsFragment();
                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.content, fragment, "ListClientDocumentsFragment")
                        .commit();

                // V2.0 Preset mode to NEW (not READ) to force the dataload on first pass.
                setMode(Document.Mode.NEW);
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
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
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the state
        savedInstanceState.putBoolean(IS_FOLLOWING_CLIENT, isFollowingClient);
        savedInstanceState.putSerializable(EDIT_DOCUMENT, getDocument());
        savedInstanceState.putString(DOCUMENT_MODE, getMode().toString());
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public Menu getMenu() {
        return menu;
    }

    public boolean isFollowingClient() {
        return isFollowingClient;
    }

    public void setFollowingClient(boolean isFollowingClient) {
        this.isFollowingClient = isFollowingClient;
    }

    public ClientSession[] getClientSessions() {
        return clientSessions;
    }

    private void composeEmail(String[] addresses) {
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


    private Drawable getStarIcon(int score) {
        switch (score) {
            case 1:
                return ContextCompat.getDrawable(this, R.drawable.ic_star_red);
            case 2:
                return ContextCompat.getDrawable(this, R.drawable.ic_star_orange);
            case 3:
                return ContextCompat.getDrawable(this, R.drawable.ic_star_yellow);
            case 4:
                return ContextCompat.getDrawable(this, R.drawable.ic_star_green);
            case 5:
                return ContextCompat.getDrawable(this, R.drawable.ic_star_blue);
            case 6:
                return ContextCompat.getDrawable(this, R.drawable.ic_star_purple);
            default:
                return ContextCompat.getDrawable(this, R.drawable.ic_star_grey);
        }
    }

    private void loadClientSessions() {
        ImageView headerAttendance5 = findViewById(R.id.header_attendance_5);
        ImageView headerMyWeek5 = findViewById(R.id.header_myweek_5);
        headerAttendance5.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross_grey));
        headerMyWeek5.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_star_grey));
        if (clientSessions[4] != null) {
            // Build 144 - Show MyWeek score even if session not attended
            headerMyWeek5.setImageDrawable(getStarIcon(clientSessions[4].getStatus()));
            if (clientSessions[4].isAttended()) {
                headerAttendance5.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tick));

            } else {
                headerAttendance5.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross));
            }
        }
        ImageView headerAttendance4 = findViewById(R.id.header_attendance_4);
        ImageView headerMyWeek4 = findViewById(R.id.header_myweek_4);
        headerAttendance4.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross_grey));
        headerMyWeek4.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_star_grey));
        if (clientSessions[3] != null) {
            // Build 144 - Show MyWeek score even if session not attended
            headerMyWeek4.setImageDrawable(getStarIcon(clientSessions[3].getStatus()));
            if (clientSessions[3].isAttended()) {
                headerAttendance4.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tick));

            } else {
                headerAttendance4.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross));
            }
        }
        ImageView headerAttendance3 = findViewById(R.id.header_attendance_3);
        ImageView headerMyWeek3 = findViewById(R.id.header_myweek_3);
        headerAttendance3.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross_grey));
        headerMyWeek3.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_star_grey));
        if (clientSessions[2] != null) {// Build 144 - Show MyWeek score even if session not attended
            headerMyWeek3.setImageDrawable(getStarIcon(clientSessions[2].getStatus()));
            if (clientSessions[2].isAttended()) {
                headerAttendance3.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tick));
            } else {
                headerAttendance3.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross));
            }
        }
        ImageView headerAttendance2 = findViewById(R.id.header_attendance_2);
        ImageView headerMyWeek2 = findViewById(R.id.header_myweek_2);
        headerAttendance2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross_grey));
        headerMyWeek2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_star_grey));
        if (clientSessions[1] != null) {
            // Build 144 - Show MyWeek score even if session not attended
            headerMyWeek2.setImageDrawable(getStarIcon(clientSessions[1].getStatus()));
            if (clientSessions[1].isAttended()) {
                headerAttendance2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tick));
            } else {
                headerAttendance2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross));
            }
        }
        ImageView headerAttendance1 = findViewById(R.id.header_attendance_1);
        ImageView headerMyWeek1 = findViewById(R.id.header_myweek_1);
        headerAttendance1.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross_grey));
        headerMyWeek1.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_star_grey));
        if (clientSessions[0] != null) {
            // Build 144 - Show MyWeek score even if session not attended
            headerMyWeek1.setImageDrawable(getStarIcon(clientSessions[0].getStatus()));
            if (clientSessions[0].isAttended()) {
                headerAttendance1.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tick));
            } else {
                headerAttendance1.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross));
            }
        }
    }

    public void loadHeader(Client client) {

        LocalSettings localSettings = LocalSettings.getInstance(this);

        // Sets isBirthdayFlag;
        int age = client.getAge();

        ImageView headerIcon = findViewById(R.id.header_icon);
        TextView firstNamesView = findViewById(R.id.header_first_names);
        TextView lastNameView = findViewById(R.id.header_last_name);
        TextView dateOfBirthView = findViewById(R.id.header_date_of_birth);
        TextView addressView = findViewById(R.id.header_address);
        TextView postcodeView = findViewById(R.id.header_postcode);
        final TextView contactNumberView = findViewById(R.id.header_contact_number);
        final TextView contactNumber2View = findViewById(R.id.header_contact_number2);
        final TextView emailView = findViewById(R.id.header_email_address);
        TextView genderView = findViewById(R.id.header_gender);
        TextView groupView = findViewById(R.id.header_group);
        TextView groupLabel = findViewById(R.id.header_label_group);
        TextView tierView = findViewById(R.id.header_tier);
        TextView tierLabel = findViewById(R.id.header_label_tier);
        final TextView keyworkerView = findViewById(R.id.header_keyworker);
        TextView keyworkerLabel = findViewById(R.id.header_label_keyworker);
        TextView toolView = findViewById(R.id.header_tool);
        TextView toolLabel = findViewById(R.id.header_label_tool);
        TextView following = findViewById(R.id.header_following);

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

        if (client.isBirthday()) {
            headerIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_birthday_cake));
        } else if (client.getCurrentCase() == null) {
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
        // Build 188 - Address is now non-mandatory
        if (client.getAddress().length() == 0) {
            addressView.setText("Not Specified", null);
        } else {
            addressView.setText(client.getAddress(), null);
        }
        postcodeView.setText(client.getPostcode());
        // Build 188 - Contact Number is now non-mandatory
        //contactNumberView.setText(client.getContactNumber());
        if (client.getContactNumber().length() == 0) {
            contactNumberView.setText("Not Specified", null);
        } else {
            contactNumberView.setText(client.getContactNumber(), null);
        }
        if (client.getContactNumber2() != null) {
            contactNumber2View.setText(client.getContactNumber2());
        }
        // Build 188 - Email Address is now non-mandatory
        //emailView.setText(client.getEmailAddress());
        if (client.getEmailAddress().length() == 0) {
            emailView.setText("Not Specified", null);
        } else {
            emailView.setText(client.getEmailAddress(), null);
        }
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
                group += " plus 1";
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
        if (client.getCurrentTool() == null) {
            toolLabel.setVisibility(View.GONE);
            toolView.setVisibility(View.GONE);
        } else {
            Document tool = client.getCurrentTool();
            //Build 187 - Added MACA as possible tool
            toolLabel.setVisibility(View.VISIBLE);
            toolView.setVisibility(View.VISIBLE);
            switch (tool.getDocumentType()) {
                case Document.CriteriaAssessmentTool:
                    CriteriaAssessmentTool cat = (CriteriaAssessmentTool) tool;
                    toolLabel.setText(String.format("%s: ", cat.getScoreLabel()));
                    toolView.setText(String.format("%s  ", cat.getScoreText()));
                    break;
                case Document.MACAYC18:
                    MACAYC18 macayc18 = (MACAYC18) tool;
                    toolLabel.setText(String.format("%s: ", macayc18.getScoreLabel()));
                    toolView.setText(String.format("%s  ", macayc18.getScoreText()));
                    break;
                default:
                    throw new CRISException(String.format("Unexpected assessment tool in LoadHeader: %d", tool.getDocumentType()));
            }
        }
        if (isFollowingClient) {
            following.setVisibility(View.VISIBLE);
        } else {
            following.setVisibility(View.GONE);
        }

        // Load the ClientSession details
        loadClientSessions();

    }

}
