package solutions.cris.list;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
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
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import solutions.cris.Login;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.EditImage;
import solutions.cris.edit.EditPdfDocument;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.CriteriaAssessmentTool;
import solutions.cris.object.Document;
import solutions.cris.object.MyWeek;
import solutions.cris.object.Status;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;
import solutions.cris.utils.LocalSettings;

import static solutions.cris.list.ListLibrary.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE;

public class ListClientHeader extends ListActivity {

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
                loadHeader(getClient());
            } else {
                // Start the List Documents fragment
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                ListClientDocumentsFragment fragment = new ListClientDocumentsFragment();
                fragmentTransaction.add(R.id.content, fragment);
                fragmentTransaction.commit();
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




    private Drawable getStarIcon(int score){
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
        ImageView headerAttendance5 = (ImageView) findViewById(R.id.header_attendance_5);
        ImageView headerMyWeek5 = (ImageView) findViewById(R.id.header_myweek_5);
        headerAttendance5.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross_grey));
        headerMyWeek5.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_star_grey));
        if (clientSessions[4] != null) {
            if (clientSessions[4].isAttended()) {
                headerAttendance5.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tick));
                headerMyWeek5.setImageDrawable(getStarIcon(clientSessions[4].getStatus()));
            } else {
                headerAttendance5.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross));
            }
        }
        ImageView headerAttendance4 = (ImageView) findViewById(R.id.header_attendance_4);
        ImageView headerMyWeek4 = (ImageView) findViewById(R.id.header_myweek_4);
        headerAttendance4.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross_grey));
        headerMyWeek4.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_star_grey));
        if (clientSessions[3] != null) {
            if (clientSessions[3].isAttended()) {
                headerAttendance4.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tick));
                headerMyWeek4.setImageDrawable(getStarIcon(clientSessions[3].getStatus()));
            } else {
                headerAttendance4.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross));
            }
        }
        ImageView headerAttendance3 = (ImageView) findViewById(R.id.header_attendance_3);
        ImageView headerMyWeek3 = (ImageView) findViewById(R.id.header_myweek_3);
        headerAttendance3.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross_grey));
        headerMyWeek3.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_star_grey));
        if (clientSessions[2] != null) {
            if (clientSessions[2].isAttended()) {
                headerAttendance3.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tick));
                headerMyWeek3.setImageDrawable(getStarIcon(clientSessions[2].getStatus()));
            } else {
                headerAttendance3.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross));
            }
        }
        ImageView headerAttendance2 = (ImageView) findViewById(R.id.header_attendance_2);
        ImageView headerMyWeek2 = (ImageView) findViewById(R.id.header_myweek_2);
        headerAttendance2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross_grey));
        headerMyWeek2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_star_grey));
        if (clientSessions[1] != null) {
            if (clientSessions[1].isAttended()) {
                headerAttendance2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tick));
                headerMyWeek2.setImageDrawable(getStarIcon(clientSessions[1].getStatus()));
            } else {
                headerAttendance2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross));
            }
        }
        ImageView headerAttendance1 = (ImageView) findViewById(R.id.header_attendance_1);
        ImageView headerMyWeek1 = (ImageView) findViewById(R.id.header_myweek_1);
        headerAttendance1.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross_grey));
        headerMyWeek1.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_star_grey));
        if (clientSessions[0] != null) {
            if (clientSessions[0].isAttended()) {
                headerAttendance1.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tick));
                headerMyWeek1.setImageDrawable(getStarIcon(clientSessions[0].getStatus()));
            } else {
                headerAttendance1.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross));
            }
        }
    }

    public void loadHeader(Client client) {

        LocalSettings localSettings = LocalSettings.getInstance(this);

        // Sets isBirthdayFlag;
        int age = client.getAge();

        ImageView headerIcon = (ImageView) findViewById(R.id.header_icon);
        TextView firstNamesView = (TextView) findViewById(R.id.header_first_names);
        TextView lastNameView = (TextView) findViewById(R.id.header_last_name);
        TextView dateOfBirthView = (TextView) findViewById(R.id.header_date_of_birth);
        TextView addressView = (TextView) findViewById(R.id.header_address);
        TextView postcodeView = (TextView) findViewById(R.id.header_postcode);
        final TextView contactNumberView = (TextView) findViewById(R.id.header_contact_number);
        final TextView contactNumber2View = (TextView) findViewById(R.id.header_contact_number2);
        final TextView emailView = (TextView) findViewById(R.id.header_email_address);
        TextView genderView = (TextView) findViewById(R.id.header_gender);
        TextView groupView = (TextView) findViewById(R.id.header_group);
        TextView groupLabel = (TextView) findViewById(R.id.header_label_group);
        TextView tierView = (TextView) findViewById(R.id.header_tier);
        TextView tierLabel = (TextView) findViewById(R.id.header_label_tier);
        final TextView keyworkerView = (TextView) findViewById(R.id.header_keyworker);
        TextView keyworkerLabel = (TextView) findViewById(R.id.header_label_keyworker);
        TextView toolView = (TextView) findViewById(R.id.header_tool);
        TextView toolLabel = (TextView) findViewById(R.id.header_label_tool);
        TextView following = (TextView) findViewById(R.id.header_following);

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
        if (client.getCurrentTool() == null) {
            toolLabel.setVisibility(View.GONE);
            toolView.setVisibility(View.GONE);
        } else {
            Document tool = client.getCurrentTool();
            CriteriaAssessmentTool cat = (CriteriaAssessmentTool) tool;
            toolLabel.setVisibility(View.VISIBLE);
            toolLabel.setText(String.format("%s: ", cat.getScoreLabel()));
            toolView.setVisibility(View.VISIBLE);
            toolView.setText(String.format("%s  ", cat.getScoreText()));
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
