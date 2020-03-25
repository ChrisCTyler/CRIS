package solutions.cris.read;
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
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.EditNote;
import solutions.cris.exceptions.CRISException;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.Document;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.User;
import solutions.cris.utils.CRISMenuItem;
import solutions.cris.utils.ExceptionHandler;
import solutions.cris.utils.LocalSettings;
import solutions.cris.utils.OnSwipeTouchListener;

public class ReadClientHeader extends ListActivity {

    public static final String CURRENT_POSITION = "solutions.cris.CurrentPosition";

    private LocalDB localDB;
    private Menu menu;
    CRISMenuItem menuItem;
    private int currentPosition = 0;
    public static int swipeValue = 0;

    //private boolean isFollowingClient = false;

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
            localDB = LocalDB.getInstance();

            // Check whether we're recreating a previously destroyed instance
            if (savedInstanceState != null) {
                // Restore value of members from saved state
                currentPosition = savedInstanceState.getInt(CURRENT_POSITION);
            } else {
                currentPosition = 0;
            }
            swipeValue = 0;

            setMode(Document.Mode.READ);
            menuItem = (CRISMenuItem) getIntent().getSerializableExtra(Main.EXTRA_UNREAD_MENU_ITEM);
            // Get a sample document to enable the client to be established
            setDocument(menuItem.getDocumentList().get(0));
            setClient((Client) localDB.getDocument(getDocument().getClientID()));
            //if (localDB.isFollowing(getCurrentUser().getUserID(), getClient().getClientID())) {
            //    isFollowingClient = true;
            //}
            loadHeader(getClient());

            // Swipe Left and Right
            LinearLayout mainLayout = (LinearLayout) findViewById(R.id.main_layout);

            mainLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
                @Override
                public void onSwipeRight() {
                    swipeValue = -1;
                    onResume();
                }

                @Override
                public void onSwipeLeft() {
                    swipeValue = 1;
                    onResume();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Handle the swipe value
        if (swipeValue != 0) {
            currentPosition += swipeValue;
            if (currentPosition >= 0 && currentPosition < menuItem.getDocumentList().size()) {
                setDocument(menuItem.getDocumentList().get(currentPosition));
                if (getDocument().getDocumentType() != Document.PdfDocument) {
                    swipeValue = 0;
                }

            } else {
                finish();
            }
        }
        doReadDocument();
    }

    // MENU BLOCK
    private static final int MENU_UNFOLLOW_CLIENT = Menu.FIRST + 3;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_share, menu);
        super.onCreateOptionsMenu(menu);
        MenuItem followOption = menu.add(0, MENU_UNFOLLOW_CLIENT, 2, "Un-follow Client");
        followOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case MENU_UNFOLLOW_CLIENT:
                localDB.setFollow(getCurrentUser().getUserID(), getClient().getClientID(), false);
                menu.removeItem(MENU_UNFOLLOW_CLIENT);
                // No longer following so exit the unread docs activity
                finish();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the state
        savedInstanceState.putInt(CURRENT_POSITION, currentPosition);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private void doReadDocument() {
        localDB.read(getDocument(), getCurrentUser());
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction;
        Fragment fragment;
        switch (getDocument().getDocumentType()) {
            case Document.Case:
                fragmentTransaction = fragmentManager.beginTransaction();
                fragment = new ReadCase();
                fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            case Document.Client:
                fragmentTransaction = fragmentManager.beginTransaction();
                fragment = new ReadClient();
                fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            case Document.Contact:
                fragmentTransaction = fragmentManager.beginTransaction();
                fragment = new ReadContact();
                fragmentTransaction.replace(R.id.content, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            case Document.CriteriaAssessmentTool:
                fragmentTransaction = fragmentManager.beginTransaction();
                fragment = new ReadCAT();
                fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            case Document.Image:
                fragmentTransaction = fragmentManager.beginTransaction();
                fragment = new ReadImage();
                fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            case Document.MyWeek:
                fragmentTransaction = fragmentManager.beginTransaction();
                fragment = new ReadMyWeek();
                fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            case Document.Note:
                fragmentTransaction = fragmentManager.beginTransaction();
                fragment = new EditNote();
                fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            case Document.PdfDocument:
                PdfDocument.displayPDFDocument((PdfDocument) getDocument(), this);
                // If swipe used to get to this pdf then continue and onResume
                // will re-apply the swipe. Otherwise, this was the first
                // document so exit from unread functionality
                if (swipeValue == 0) {
                    finish();
                }
                break;
            case Document.Transport:
                fragmentTransaction = fragmentManager.beginTransaction();
                fragment = new ReadTransport();
                fragmentTransaction.replace(R.id.content, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
        }
    }

    public void loadHeader(Client client) {
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

        ImageView headerIcon = (ImageView) findViewById(R.id.header_icon);
        TextView firstNamesView = (TextView) findViewById(R.id.header_first_names);
        TextView lastNameView = (TextView) findViewById(R.id.header_last_name);
        TextView dateOfBirthView = (TextView) findViewById(R.id.header_date_of_birth);
        TextView addressView = (TextView) findViewById(R.id.header_address);
        TextView postcodeView = (TextView) findViewById(R.id.header_postcode);
        TextView contactNumberView = (TextView) findViewById(R.id.header_contact_number);
        TextView contactNumber2View = (TextView) findViewById(R.id.header_contact_number2);
        TextView emailView = (TextView) findViewById(R.id.header_email_address);
        TextView genderView = (TextView) findViewById(R.id.header_gender);
        TextView groupView = (TextView) findViewById(R.id.header_group);
        TextView groupLabel = (TextView) findViewById(R.id.header_label_group);
        TextView tierView = (TextView) findViewById(R.id.header_tier);
        TextView tierLabel = (TextView) findViewById(R.id.header_label_tier);
        TextView keyworkerView = (TextView) findViewById(R.id.header_keyworker);
        TextView keyworkerLabel = (TextView) findViewById(R.id.header_label_keyworker);
        TextView toolLabel = (TextView) findViewById(R.id.header_label_tool);
        TextView following = (TextView) findViewById(R.id.header_following);

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
                            "Unexpected client status: %d",
                            client.getCurrentCase().getClientStatus()));
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
            String keyworkerContact = "";
            Case currentCase = client.getCurrentCase();
            if (currentCase.getTier() != null) {
                tier = currentCase.getTier().getItemValue();
            }
            if (currentCase.getGroup() != null) {
                group = currentCase.getGroup().getItemValue();
            }
            // Build 139 - Second Group (Indicate a second group)
            if (currentCase.getGroup2ID() != null) {
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

        // Tool has not been calculated in the Read Document case so hide the label
        toolLabel.setVisibility(View.GONE);

        // User must be following client (or read would not exist) so no need to display
        following.setVisibility(View.GONE);

        // ClientSessions are unknown so hide Attendance/MyWeek indicators
        ImageView headerAttendance5 = (ImageView) findViewById(R.id.header_attendance_5);
        ImageView headerMyWeek5 = (ImageView) findViewById(R.id.header_myweek_5);
        headerAttendance5.setVisibility(View.GONE);
        headerMyWeek5.setVisibility(View.GONE);
        ImageView headerAttendance4 = (ImageView) findViewById(R.id.header_attendance_4);
        ImageView headerMyWeek4 = (ImageView) findViewById(R.id.header_myweek_4);
        headerAttendance4.setVisibility(View.GONE);
        headerMyWeek4.setVisibility(View.GONE);
        ImageView headerAttendance3 = (ImageView) findViewById(R.id.header_attendance_3);
        ImageView headerMyWeek3 = (ImageView) findViewById(R.id.header_myweek_3);
        headerAttendance3.setVisibility(View.GONE);
        headerMyWeek3.setVisibility(View.GONE);
        ImageView headerAttendance2 = (ImageView) findViewById(R.id.header_attendance_2);
        ImageView headerMyWeek2 = (ImageView) findViewById(R.id.header_myweek_2);
        headerAttendance2.setVisibility(View.GONE);
        headerMyWeek2.setVisibility(View.GONE);
        ImageView headerAttendance1 = (ImageView) findViewById(R.id.header_attendance_1);
        ImageView headerMyWeek1 = (ImageView) findViewById(R.id.header_myweek_1);
        headerAttendance1.setVisibility(View.GONE);
        headerMyWeek1.setVisibility(View.GONE);
    }
}
