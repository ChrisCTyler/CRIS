package solutions.cris;
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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;
import solutions.cris.edit.ChangePassword;
import solutions.cris.exceptions.CRISException;
import solutions.cris.list.ListClients;
import solutions.cris.list.ListKPI;
import solutions.cris.list.ListLibrary;
import solutions.cris.list.ListSessions;
import solutions.cris.list.ListSyncActivity;
import solutions.cris.list.ListSysAdmin;
import solutions.cris.list.ListUnreadDocuments;
import solutions.cris.list.ListUsers;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.Document;
import solutions.cris.object.Group;
import solutions.cris.object.ListItem;
import solutions.cris.object.Note;
import solutions.cris.object.NoteType;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.RawDocument;
import solutions.cris.object.Role;
import solutions.cris.object.Session;
import solutions.cris.object.SyncActivity;
import solutions.cris.object.SystemError;
import solutions.cris.object.User;
import solutions.cris.read.ReadClientHeader;
import solutions.cris.sync.SyncManager;
//import solutions.cris.utils.CRISDeviceAdmin;
import solutions.cris.sync.WebConnection;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.utils.CRISKPIItem;
import solutions.cris.utils.CRISMenuItem;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.ExceptionHandler;

public class Main extends CRISActivity {

    public static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0;
    // Build 119 30 May 2019 Addition of SEND_SMS permission
    public static final int REQUEST_PERMISSION_SEND_SMS = 2;
    public static final String EXTRA_CLIENT_ID = "solutions.cris.ClientId";
    public static final String EXTRA_IS_NEW_MODE = "solutions.cris.EditMode";
    public static final String EXTRA_PASSWORD_EXPIRED = "solutions.cris.PasswordExpired";
    public static final String EXTRA_DOCUMENT = "solutions.cris.LoginOld.Document";
    public static final String EXTRA_IS_FIRST_USE = "solutions.cris.LoginOld.IsFirstUse";
    public static final String EXTRA_LIST_POSITION = "solutions.cris.ListPosition";
    public static final String EXTRA_LIST_TYPE = "solutions.cris.ListType";
    public static final String HINT_DISPLAYED = "solutions.cris.HintDisplayed";
    public static final String EXTRA_EMAIL_ADDRESS = "solutions.cris.EmailAddress";
    public static final String EXTRA_ROLE = "solutions.cris.Role";
    public static final String EXTRA_MY_CLIENTS = "solutions.cris.MyClients";
    public static final String EXTRA_AGENCY = "solutions.cris.Agency";
    public static final String EXTRA_NOTE_TYPE = "solutions.cris.NoteType";
    public static final String EXTRA_SCHOOL = "solutions.cris.School";
    public static final String EXTRA_ORGANISATION = "solutions.cris.Organisation";
    public static final String EXTRA_UNREAD_MENU_ITEM = "solutions.cris.UnreadMenuItem";
    public static final String EXTRA_GROUP = "solutions.cris.Group";
    public static final String EXTRA_TRANSPORT_ORGANISATION = "solutions.cris.TransportOrganisation";
    public static final String EXTRA_SHARE_TEXT = "solutions.cris.ShareText";
    // Builod 139 - Added KPIs
    public static final String EXTRA_KPI_TYPE = "solutions.cris.KPIType";
    private static final int MENU_SYNC_WEBSITE_MYWEEKS = Menu.FIRST + 10;

    public static final String CHANNEL_ID = "solutions.cris.channelid";

    // Receiver for broadcasts when Sync Adapter completes each sync
    private SyncReceiver myReceiver;
    private ArrayList<CRISMenuItem> menuItems;
    private MainMenuAdapter adapter;
    private LocalDB localDB;
    private User currentUser;
    private SyncManager syncManager;
    // Build 116 22 May 2019 Add handler for incoming text via share
    private String shareText;
    // build 179
    boolean inProgress = false;
    // Build 181
    private Date startTime;
    private ListView listView;
    private boolean doCheckUpgrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        // Build 116 22 May 2019 Add handler for incoming text via share
        // At this stage, simply load a text variable with the text.
        // If there is shared text, All Client view will be triggered
        shareText = "";
        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            if (intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
                shareText = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
        }

        setContentView(R.layout.activity_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Build 181
        doCheckUpgrade = true;
        // Initialise the main menu. Firstly setup the List view listeners
        listView = findViewById(R.id.list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                doListClick(view, position);
            }
        });

        // Create new static instance of Sync Manager
        syncManager = SyncManager.getSyncManager(this);

        // and a Sync Receiver to get broadcasts from the Sync Adapter
        myReceiver = new SyncReceiver();
        registerReceiver(myReceiver, new IntentFilter(SyncManager.SYNC_ACTION));


    }

    @Override
    protected void onResume() {
        super.onResume();

        long diagElapsed = -1;

        // Build 181
        startTime = new Date();
        // Create the /CRIS directory if necessary
        checkCRISDirectoryExists();

        // Build 150 - Added Notification Channel
        createNotificationChannel();

        // Initialise the list of Static Menu Items
        menuItems = new ArrayList<>();

        // If we are not logged in, this call will return null
        currentUser = User.getCurrentUser();
        if (currentUser == null) {
            // Not yet logged in so start the LoginOld activity
            Intent intent = new Intent(this, Login.class);
            // This may be initiated by a notification so check for an email address in this activity's intent
            if (getIntent().hasExtra(Main.EXTRA_EMAIL_ADDRESS)) {
                intent.putExtra(Main.EXTRA_EMAIL_ADDRESS, getIntent().getStringExtra(Main.EXTRA_EMAIL_ADDRESS));
            }
            startActivity(intent);
        } else {
            // Get pointer to localDB;
            localDB = LocalDB.getInstance();
            // Okay to check for database upgrade here because AESEncryption has been initialised
            // This will create and new PickList entries
            // Build 178 - On upgrade to version 28, run the fixListItem code
            // Note: checkDBUpgrade now returns the current version if an upgrade occurred
            //localDB.checkDBUpgrade(currentUser);

            // Build 181
            if (doCheckUpgrade) {
                doCheckUpgrade = false;
                int version = localDB.checkDBUpgrade(currentUser);
                // Build 179 - Remove so that this is not run every time
                //if (version == 28) {
                //    new FixItemListIdentifiers().execute();
                //}
                Date diagTime = new Date();
                diagElapsed = (diagTime.getTime() - startTime.getTime()) / 100;
            }
            // Build 181
            //Date diagTime = new Date();
            //long diagElapsed = (diagTime.getTime() - startTime.getTime()) / 1000;
            // Check for password expiry as long as successful sync has occurred
            boolean passwordExpired = false;
            if ((currentUser.getRoleID() != Role.noPrivilegeID) &&
                    (User.getCurrentUser().getPasswordExpiryDate().before(new Date()))) {
                passwordExpired = true;
                Intent intent = new Intent(this, ChangePassword.class);
                intent.putExtra(Main.EXTRA_PASSWORD_EXPIRED, true);
                startActivity(intent);
            }

            if (!passwordExpired) {
                // Build 116 22 May 2019 Add handler for incoming text via share
                // If there is shared text, All Client view will be triggered
                if (shareText.length() > 0) {
                    if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ACCESS_ALL_CLIENTS)) {
                        Intent intent = new Intent(this, ListClients.class);
                        intent.putExtra(Main.EXTRA_MY_CLIENTS, false);
                        intent.putExtra(Main.EXTRA_SHARE_TEXT, shareText);
                        startActivity(intent);
                    } else if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ACCESS_MY_CLIENTS)) {
                        Intent intent = new Intent(this, ListClients.class);
                        intent.putExtra(Main.EXTRA_MY_CLIENTS, true);
                        intent.putExtra(Main.EXTRA_SHARE_TEXT, shareText);
                        startActivity(intent);
                    } else {
                        doNoPrivilege();
                    }
                    finish();
                } else {
                    // Build 181 - Moved View Sync History to menu
                    //listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    //    @Override
                    //    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    //        return doListLongClick(view);
                    //    }
                    //});

                    menuItems.add(0, getSyncMenuItem());
                    // Build 138 - Add partial Initialisation - Check if still initialising
                    SyncActivity syncActivity = localDB.getLatestSyncActivity(currentUser);
                    if (syncActivity != null) {
                        String syncStatus = syncActivity.getResult();
                        if (!syncStatus.equals("PARTIAL")) {
                            // If the database is being initialised from the web the current user's role will
                            // be NoPriv in which case don't offer any menu items
                            if (currentUser.getRoleID() != Role.noPrivilegeID) {
                                menuItems.add(new CRISMenuItem("My Unread Documents", "", R.drawable.ic_star_red, null));
                                menuItems.add(new CRISMenuItem("My Clients", "", R.drawable.ic_group_red, null));
                                menuItems.add(new CRISMenuItem("My Sessions", "", R.drawable.ic_sessions, null));
                                menuItems.add(new CRISMenuItem("All Clients", "", R.drawable.ic_group_green, null));
                                menuItems.add(new CRISMenuItem("Users", "", R.drawable.ic_group_blue, null));
                                menuItems.add(new CRISMenuItem("Library", "", R.drawable.ic_pdf_document, null));
                                // Build 139 - Added KPIs
                                menuItems.add(new CRISMenuItem("Key Performance Indicators", "", R.drawable.ic_chart, null));

                                if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_SYSTEM_ADMINISTRATOR)) {
                                    menuItems.add(new CRISMenuItem("System Administration", "", R.drawable.ic_system_administration, null));
                                }
                                // Build 158 - Moved GetUnreadDocuments to seperate list because it has become too slow
                                // to run here
                                // Load the unread documents
                                // getUnreadDocuments();

                            }
                        }
                    }
                }
            }
        }

        Date endTime = new Date();
        long elapsed = (endTime.getTime() - startTime.getTime()) / 100;
        // Build 110 - Updated copyright date
        //menuItems.add(new CRISMenuItem(String.format("CRIS v%s, \u00A9 2016, cris.solutions", BuildConfig.VERSION_NAME), "",
        //menuItems.add(new CRISMenuItem(String.format("CRIS v%s, \u00A9 2022, cris.solutions (%d-%d secs)",BuildConfig.VERSION_NAME,diagElapsed,elapsed),
        //        "", R.drawable.ic_cris_grey, null));
        menuItems.add(new CRISMenuItem(String.format("CRIS v%s, \u00A9 2022, cris.solutions", BuildConfig.VERSION_NAME),
                "", R.drawable.ic_cris_grey, null));
        // Display the menu
        Collections.sort(menuItems, CRISMenuItem.comparatorAZ);
        // Create the Main menu
        adapter = new MainMenuAdapter(this, menuItems);
        // Display in the List View
        listView.setAdapter(adapter);

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "FLOUR";
            String description = "Shipton Mill";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void doListClick(View view, int position) {
        String title = ((CRISMenuItem) view.getTag()).getTitle();
        Intent intent;
        switch (title) {
            case "Sync":
                manualSync();
                break;
            case "My Unread Documents":
                intent = new Intent(view.getContext(), ListUnreadDocuments.class);
                startActivity(intent);
                break;
            case "My Clients":
                if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ACCESS_MY_CLIENTS)) {
                    intent = new Intent(view.getContext(), ListClients.class);
                    intent.putExtra(Main.EXTRA_MY_CLIENTS, true);
                    startActivity(intent);
                } else {
                    doNoPrivilege();
                }
                break;
            case "My Sessions":
                intent = new Intent(view.getContext(), ListSessions.class);
                startActivity(intent);
                break;
            case "All Clients":
                if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_ACCESS_ALL_CLIENTS)) {
                    intent = new Intent(view.getContext(), ListClients.class);
                    intent.putExtra(Main.EXTRA_MY_CLIENTS, false);
                    intent.putExtra(Main.EXTRA_SHARE_TEXT, shareText);
                    startActivity(intent);
                } else {
                    doNoPrivilege();
                }
                break;
            case "Users":
                intent = new Intent(view.getContext(), ListUsers.class);
                startActivity(intent);
                break;
            case "Library":
                intent = new Intent(view.getContext(), ListLibrary.class);
                intent.putExtra(Main.EXTRA_CLIENT_ID, Document.nonClientDocumentID.toString());
                startActivity(intent);
                break;
            // Build 139 - Added KPIs
            case "Key Performance Indicators":
                intent = new Intent(view.getContext(), ListKPI.class);
                startActivity(intent);
                break;
            case "System Administration":
                intent = new Intent(view.getContext(), ListSysAdmin.class);
                startActivity(intent);
                break;
            default:
                // Build 158 Unread Documents removed
                //if (!title.startsWith("CRIS")) {
                //    doReadDocument(position);
                //}
        }
    }

    // Build 181 - Moved to menu option to avoid accidental manual sync when trying to select
    private boolean doListLongClick(View view) {
        String title = ((CRISMenuItem) view.getTag()).getTitle();
        switch (title) {
            case "Sync":
                Intent intent = new Intent(view.getContext(), ListSyncActivity.class);
                startActivity(intent);
                break;
            default:
                if (title.startsWith("CRIS")) {
                    doLogout();
                }
                // Else do nothing
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu);

        //MenuItem syncMyWeekOption = menu.add(0, MENU_SYNC_WEBSITE_MYWEEKS, 40, "Sync Website MyWeeks");
        //syncMyWeekOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Build 181 Resync Options
            case R.id.action_view_sync_history:
                Intent intent = new Intent(this, ListSyncActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_resync_30:
                manualReSync(30);
                return true;

            case R.id.action_resync_60:
                manualReSync(60);
                return true;

            case R.id.action_resync_90:
                manualReSync(90);
                return true;

            case R.id.show_changes:
                showChanges();
                return true;

            case R.id.action_change_password:
                intent = new Intent(this, ChangePassword.class);
                intent.putExtra(Main.EXTRA_PASSWORD_EXPIRED, false);
                startActivity(intent);
                return true;

            case R.id.action_logout:
                doLogout();
                return true;

            case R.id.disable_device_policy:
                if (deviceManager.isAdminActive(compName)) {
                    deviceManager.removeActiveAdmin(compName);
                }
                return true;

            // Build 178 - Run automatically on upgrade
            // Build 179 restore to menu option
            case R.id.fix_listitems:
                if (inProgress) {
                    inProgressMessage();
                } else {
                    inProgress = true;
                    // Carry out the process in the background
                    new FixItemListIdentifiers().execute();
                }
                return true;


            // Not used, kept for future testing/debugging
            //case MENU_SYNC_WEBSITE_MYWEEKS:
            //    // Load the data in the background
            //    new SyncMyWeeks().execute();
            //    return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }


    // Not used, kept for future testing/debugging
    /*
    private class SyncMyWeeks extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            // Runs in Background Thread
            int count = 0;
            String message = "Starting...";
            try {
                WebConnection webConnection = new WebConnection(localDB);
                JSONObject jsonOutput = webConnection.post("pdo_get_myweek_website_records.php");
                String result = jsonOutput.getString("result");
                if (result.equals("FAILURE")) {
                    throw new CRISException("pdo_get_myweek_website_records.php: " + jsonOutput.getString("error_message"));
                } else {
                    int records = 0;
                    records = localDB.downloadWebsiteMyWeeks(jsonOutput);
                    message = String.format("Sync Completed. New MyWeeks created: %d",records);
                }
            } catch (JSONException ex) {
                // Build 138 - Add Partial Downloads
                message = "Error parsing JSON data: " + ex.getMessage();

            } catch (Exception ex) {
                message = ex.getMessage();
            }
            return message;
        }

        @Override
        protected void onPreExecute() {
            // Runs on UI Thread
        }

        @Override
        protected void onPostExecute(String message) {
            // Runs on UI Thread

            new AlertDialog.Builder(Main.this)
                    .setTitle("Sync MyWeeks")
                    .setMessage(message)
                    .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }
    */

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
    }

    private void doNoPrivilege() {
        new AlertDialog.Builder(this)
                .setTitle("No Privilege")
                .setMessage("Unfortunately, this option is not available.")
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void inProgressMessage() {
        new AlertDialog.Builder(this)
                .setTitle("In Progress")
                .setMessage("The process is still in progress, please wait.")
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void showChanges() {

        String changes = "Added the ability to select clients by Group, Link Worker etc. to the Session Register, \n\n" +
                "--- Older Changes ---\n\n" +
                "Minor mofification to SysAdmin - Link Sticky Notes-\n\n" +
                "Fixed bug which was preventing the display of the Sticky Flag icon in Session Registers (red flag) " +
                "showing clients who have a sticky note (such as an allergy warning)\n\n" +
                "Fixed bug in Batch Invite of groups to sessions which was wrongly inviting non-matching clients\n\n" +
                "Added functionality fo Plans and Financial Support checkboxes and review " +
                "to Case form with associated changes to export and additional KPI reports.\n\n" +
                "Added report of individual document counts\n\n" +
                "Altered session attendance scores so that only ad-hoc sessions score negatively for DNA\n\n" +
                "Fixed bug preventing Session save with no invitees selected\n\n" +
                "Fixed bug in Select Schools which prevented scrolling\n\n" +
                "Revert Session Register default to Show Uncancelled invitees.\n\n" +
                "Update date/time in client session if session date/time is changed.\n\n" +
                "Modify Session Register export to only export MyWeek documents for the session.\n\n" +
                "Addition of point score for session attendance and associated update of client export\n\n" +
                "Convert menu 'select' option to multi-choice checkboxes to allow multiple groups, schools etc." +
                "Add 'Batch Invite' facility for ad-hoc group sessions allowing invite by group, school, keyworker etc.\n\n" +
                "Optimise export facility and improve progress feedback.\n\n" +
                "Rebuild for latest Android release Snow Cone (API Level 32)\n\n" +
                "Extend range of devices supported using AndroidX for Fragment/Dialogs" +
                "Client Address, Contact Number and Email made non-mandatory\n\n" +
                "Addition of MACA-YC18 assessment and associated export\n\n" +
                "Optimised search mechanism in Session Register when adding/removing clients.\n\n" +
                "Corrected view issue with search icon in Session Register\n\n" +
                "Fixed crash following re-display of sessions after creation of new session\n\n" +
                "Added Special Educational Needs and Disabilities (SEND) field to the " +
                "School Contact document to replace the outdated SENCO relationship. \n\n" +
                "Modified export functionality to use the new SEND flag to indicate Special Needs\n\n" +
                "Added a menu option to allow re-sync of all records from last 30/60/90 " +
                "days. This can be used to reload documents which are found to be missing due to " +
                "Internet dropouts. The process can take a significant time to run (especially " +
                "the 60/90 options) so should only be run in a good wireless environment. \n\n" +
                "Optimised the loading of Session registers. Load time should now be negligible " +
                "even in the case of large groups.\n\n" +
                "Added Free School Meals (FSM) field to the School Contact document. New cases " +
                "will default to 'true' but existing records will need to be set by editing " +
                "the most recent School Contact document.\n\n" +
                "Added the following fields to Session Register export: Email, Commissioner, " +
                "Current School, FSM flag and SEND flag.\n\n" +
                "Moved the facility to Show Sync History to main menu from double click of " +
                "SYNC option to prevent accidental manual syncs.\n\n" +

                "Modified System Error view to ignore errors caused by poor internet connections.\n\n" +
                "Added main menu option - Fix ListItems - to correct errors caused by historical bug in synchronisation of lists. " +
                "This option should be run if any list item (gender, ethnicity, tier, group, school etc.) is 'unknown'.\n\n" +
                "Fixed bug in Client Session View\n\n" +
                "Modified sync facility to handle duplicate records\n\n" +
                "Modified School and Agency Search to optimise search time and fix potential crash.\n\n" +
                "Fixed problem with resolution of edit clashes which caused documents to fail to download following sync.\n\n" +
                "Fixed crash when cancelling the creation of a new document\n\n" +
                "Further optimisation of All Clients/My Clients load\n\n" +
                "Fixed response document display issues in clients' notes\n\n" +
                "Moved load timing statistics to footer to enable them to display correctly on smartphones\n\n" +
                "Show Unread Documents moved to separate menu option to improve speed\n\n" +
                "Load of All/My Clients modified to improve speed\n\n" +
                "Load of individual client modified to improve speed\n\n" +
                "Save process for edit//creation of new documents modified to improve speed\n\n" +
                "Case Start Date format fixed on Clent Export\n\n" +
                "Modified Session export to include telephone number\n\n" +
                "Added KPI for Total Session Attendance\n\n" +
                "Modified Session export to include full address\n\n" +
                "Fixed bug where setting session time to 12:00 to 12:59 saved the session as 00:00 to 00:59.\n\n" +
                "Disabled the hashtag functionality in Notes\n\n" +
                "Enabled Broadcast Text from Client List\n\n" +
                "Fixed a whole set of possible crashes due to users switching the screen " +
                "orientation. The fix locks the screen orientation to portrait for a substantial " +
                "proportion of the apps screens. If this proves problematical, it may be possible " +
                "to relax this limit for some screens.\n\n" +
                "A number of minor bug-fixes.\n\n" +
                "Stopped cancelled MyWeek documents from appearing in header.\n\n" +
                "Fixed problem with Notification of unread documents./n/n" +
                "Modified SMS/Email text when sending MyWeek Website link.\n\n" +
                "Fixed bug in Website MyWeek download where non-standard characters in Notes " +
                "caused MyWeeks to be lost.\n\n" +
                "Added system admin facility to check MyWeek website sync and recover missing documents\n\n" +
                "Reduced SMS Send batch size to help seinding isues.\n\n" +
                "Fixed bug causing crash when swiping left and right on unread notes\n\n" +
                "Show MyWeek scores (stars) in client header even if session is not attended.\n\n" +
                "Default 'create note' in Session Register Send MyWeek Link to Yes\n\n" +
                "Add Created By to My Week export to enable MyWeeks created by 'The Client' to be recognised\n\n" +
                "Fixed bug causing intermittant crashes with database constraint errors\n\n" +
                "Added facility to complete MyWeek from link to website. Links may " +
                "be sent by text or email from the Session Register or from an individual " +
                "client using the 'Send MyWeek Link' menu option. Once the MyWeek is " +
                "completed it is downloaded by the next sync and added to the client's records.\n\n" +
                "Added Key Performance Indicator Menu - Active Cases and Percentage Session Attendance.\n\n" +
                "Modified CAT Tool to include Living With/Caring for Grandparents.\n\n" +
                "Add the facility to link a second group to a client's case.\n\n" +
                "Add School Year Group to Export (new column after age)\n\n" +
                "Add a counter and better diagnostics to initialisation of new smartphones/tablets.\n\n" +
                "Send SMS broadcast messages in batches to solve a problem with some network providers.\n\n" +
                "Only display current schools when 'selecting clients by school'\n\n" +
                "Only display current agencies when 'selecting clients by agency'\n\n" +
                "Fixed bug that created multiple Text Message Notes.\n\n" +
                "Created option to remove invalid multiple Text Message Notes.\n\n" +
                "Removed Email and Text Message Notes from Notification list.\n\n" +
                "Fixed crash when reading MyWeek from within a Session Register.\n\n" +
                "Fixed crash when creating new Case documents to reject or close a case. \n\n" +
                "Fixed issue that client's document list was not reloaded on tablet/smartphone re-orientation.\n\n" +
                "Removed redundant local encryption (Android devices have hardware encryption as standard)" +
                "Added facility to broadcast text/email messages to a group of carers " +
                "from Session and Client Lists.\n\n" +
                "CRIS modified to act as a 'share target' for text shares, creating a note under the selected client." +
                "When Note type is Email/Text Message, Share mechanism formats the information for the appropriate application (Email/SMS).\n\n" +
                "Added icon in Session Register to show presence of sticky notes\n\n" +
                "Added Show One School option to All Clients view to display a list of " +
                "all active clients with school record for the given school.\n\n" +
                "Added option to create a list of 'reserves' for a session. A client on the " +
                "reserve list can be invited or removed from the list. Removing a reserve " +
                "will request a cancellation reason, similar to cancelling an invitation.\n\n" +
                "Share option on Client Document view modified to reflect the currently " +
                "selected documents, so that cancelled documents can be shared.\n\n" +
                "Added functionality to handle invites to already existing, future sessions when " +
                "a client moves to a new group. All invites to the old group's future " +
                "sessions will be cancelled automatically. The user will then be given the option " +
                "of having invitations automatically added to any future session for the new " +
                "group. Invitations to future ad-hoc sessions will not be affected by a change " +
                "of group.\n\n" +
                "Fixed crash when client invited to session before case start due " +
                "to unknown status of 'consent to photography'.\n\n" +
                "Added audit trail displaying change history for documents. Swipe the document " +
                "from left to right to see the changes.\n\n" +
                "Upgraded database interface.\n\n" +
                "Added 'Automatically invite to Group Sessions' checkbox to Case " +
                "document and use to control client register for new sessions.\n\n" +
                "Added 'Photography/Media Consent' checkbox to Case document. Replaced PDF " +
                "icon in session register with red/green camera reflecting consent setting. \n\n" +
                "Added MyWeek documents to export of sessions to enable monitoring of trends " +
                "for individual scores\n\n" +
                "Automatically confirm associated taxi journeys when session is marked attended\n\n" +
                "Added search option when adding further clients to a session\n\n" +
                "Added share at client level\n\n" +
                "Added Case Summary to Case documents\n\n" +
                "Change to 'Follow' mechanism to ensure that a note which " +
                "triggers a follow is visible as the first unread document. Previously, " +
                "the follow start was set marginally after the creation date of the " +
                "associated note so only subsequent documents were made visible. \n\n" +
                "Fix to stop unread notes being incorrectly notified\n\n" +
                "Bug fix to handle problem with unattached, unread notes\n\n" +

                "Added filter to System Error list\n\n" +
                "Modified Lock to use Android Screen Lock\n\n" +
                "Unread documents restricted to Library documents, Notes and MyWeek documents\n\n" +
                "Unread documents icon now red/amber/green according to 'worst' unread note colour\n\n" +
                "Fixed bug in labelling of keyworker in session documents\n\n" +
                "Fixed problem when displaying badly-formed system error messages\n\n" +
                "Modification to access control to convert the READ/WRITE_NOTES functionality to " +
                "include all demographic documents (Client, Contact, Case and sticky-Notes). This  enables " +
                "full and 'administrative only' users to be differentiated using READ/WRITE_ALL_DOCUMENTS or " +
                "READ/WRITE_DEMOGRAPHIC_DOCUMENTS respectively.\n\n" +
                "When creating sessions, session name may be changed to create an 'ad-hoc' group session which still automatically invites group members\n\n" +
                "Fixed bug which allowed sessions to be created for groups which were not accessible by the user\n\n" +
                "Fixed bug that displayed multiple copies of session following initial creation\n\n" +
                "Fixed bug which reset name, time, coordinator etc. whenever a session was edited.\n\n" +
                "Added Summary field (unlimited text) to Case document\n\n" +
                "Optimisation to export functionality\n\n" +
                "A number of minor bug fixes\n\n" +
                "Fix for 'birthday' error for clients born in Leap Years\n\n" +
                "Data optimisation to reduce list loading times\n\n" +
                "Optimisation to remove re-loading when browsing client records\n\n" +
                "Export to Google Sheets (with date range)\n\n" +
                "Added duration to Session information\n\n" +
                "Added Sort by Case Start Date and Sort by Age to Client Views\n\n" +
                "Error list restricted to last 100 errors\n\n";
        new AlertDialog.Builder(this)
                .setTitle(String.format("Changes in this Release (v%s)", BuildConfig.VERSION_NAME))
                .setMessage(changes)
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    /*
    private void doReadDocument(int position) {
        CRISMenuItem menuItem = menuItems.get(position);
        Document document = menuItem.getDocumentList().get(0);
        // Handle library documents
        if (document.getClientID().equals(Client.nonClientDocumentID)) {
            localDB.read(document, currentUser);
            PdfDocument.displayPDFDocument((PdfDocument) document, this);
        } else {
            Intent intent = new Intent(this, ReadClientHeader.class);
            // User is serializable so can pass as extra to EditUser Activity
            intent.putExtra(Main.EXTRA_UNREAD_MENU_ITEM, menuItem);
            startActivity(intent);
        }
    }

    */
/*
    private void getUnreadDocuments() {
        // Remove existing non-static menu items
        for (int i = 0; i < menuItems.size(); i++) {
            if (menuItems.get(i).getDocumentList() != null) {
                menuItems.remove(i);
            }
        }
        // Get the list of unread documents
        ArrayList<Document> unreadDocuments = localDB.getUnreadDocuments(currentUser);
        // Sort by ClientID then ReferenceDate
        Collections.sort(unreadDocuments, Document.comparatorUnread);
        // Loop through, creating new MenuItems for each client
        String currentUserID = "";
        CRISMenuItem menuItem = null;
        String menuItemTitle;
        for (int i = 0; i < unreadDocuments.size(); i++) {
            Client client;
            Document document = unreadDocuments.get(i);
            if (!document.getClientID().toString().equals(currentUserID)) {
                currentUserID = document.getClientID().toString();
                // Create a new menuitem
                if (document.getClientID().equals(Document.nonClientDocumentID)) {
                    PdfDocument pdfDocument = (PdfDocument) document;
                    menuItemTitle = "Library ";
                    menuItem = new CRISMenuItem(menuItemTitle,
                            pdfDocument.getPdfType().getItemValue() + " - " + pdfDocument.getSummary(),
                            R.drawable.ic_pdf_document, document.getReferenceDate());
                } else {
                    // Need Client's name
                    client = (Client) localDB.getDocument(document.getClientID());
                    // Build 098 - Now handled in localDb.getUnreadDocuments
                    // Due to an earlier bug, there can be spurious documents which are not
                    // linked to a client. These should be ignored
                    //if (client == null) {
                    //    continue;
                    //}
                    menuItemTitle = client.getFullName();
                    menuItem = new CRISMenuItem(menuItemTitle,
                            document.getDocumentTypeString(),
                            R.drawable.ic_client_green, document.getReferenceDate(),
                            client.getLastName());
                    // 6/12/2017 Build 097 Moved following code to here to deal with
                    // spurious null client
                    // 19/10/2017 Build 089 Change colour of icon to 'reddest' document
                    // if document is a note
                    if (document.getDocumentType() == Document.Note) {
                        Note noteDocument = (Note) document;
                        NoteType noteType = (NoteType) noteDocument.getNoteType();
                        switch (noteType.getNoteIcon()) {
                            case NoteType.ICON_COLOUR_RED:
                            case NoteType.ICON_COLOUR_RESPONSE_RED:
                                menuItem.setIcon(R.drawable.ic_client_red);
                                menuItem.setSortString("031" + client.getLastName());
                                break;
                            case NoteType.ICON_COLOUR_AMBER:
                            case NoteType.ICON_COLOUR_RESPONSE_AMBER:
                                if (menuItem.getIcon() != R.drawable.ic_client_red) {
                                    menuItem.setIcon(R.drawable.ic_client_amber);
                                    menuItem.setSortString("032" + client.getLastName());
                                }
                                break;
                            default:
                                // Leave it Green
                        }
                    }
                }
                // Build 099 - Moved following code to here to fix programming error
                // Create an empty document list
                menuItem.setDocumentList(new ArrayList<Document>());
                // Add to the list of MenuItems
                menuItems.add(menuItem);
            }
            if (menuItem != null) {
                menuItem.getDocumentList().add(document);
            }
        }
    }

 */

    private void doLogout() {
        // Logout
        User.setCurrentUser(null);
        finish();
    }

    private void checkCRISDirectoryExists() {
        // This will require Storage privilege
        // Editing Pdfs needs WRITE_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            testCRISDirectory();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
    }

    private void testCRISDirectory() {
        // Check the state of the external media
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) &&
                !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Not much point continuing of the external media is not available
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("External Media Not Found");
            String message = "Error accessing local storage: " + state;
            builder.setMessage(message);
            // Add the Continue button
            builder.setPositiveButton(R.string.action_continue, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked Continue button
                }
            });
            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            // Get the pathname for the CRIS directory
            File pathCRIS = Environment.getExternalStoragePublicDirectory("CRIS");
            if (!pathCRIS.exists()) {
                // Add the CRIS directory
                if (!pathCRIS.mkdirs()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Cannot create CRIS directory");
                    String message = "The CRIS application has been unable to create a CRIS " +
                            "directory in your tablet/phone's local storage. Without this " +
                            "directory, you will be unable to add Pdf files to your client's " +
                            "records. Please create the directory manually using a File " +
                            "Manager app (or equivalent).";
                    builder.setMessage(message);
                    // Add the Continue button
                    builder.setPositiveButton(R.string.action_continue, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked Continue button
                        }
                    });
                    // Create the AlertDialog
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    testCRISDirectory();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Storage Permission Denied");
                    String message = "You will not be able to add Pdf documents to your client's " +
                            "record because you have denied the 'storage' permission " +
                            "for this application. Please repeat the operation and allow " +
                            "the application to access the local storage. (Note: if you " +
                            "checked 'Do not ask again', you will have to set the permission " +
                            "using the 'Settings' app on your tablet/phone.";
                    builder.setMessage(message);
                    // Add the Continue button
                    builder.setPositiveButton(R.string.action_continue, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked Continue button
                        }
                    });
                    // Create the AlertDialog
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                break;
            }
            default:
                throw new CRISException(String.format(Locale.UK, "Unexpected case value: %d", requestCode));
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public void manualSync() {
        if (isConnected()) {
            // Set Icon(0) will display the 'spinner'
            menuItems.get(0).setIcon(0);
            adapter.notifyDataSetChanged();
            // Request the sync on the Sync Adapter thread. Completion broadcasts the status which
            // is picked up below in the SyncReceiver
            syncManager.requestManualSync();
        } else {
            CRISMenuItem syncItem = menuItems.get(0);
            syncItem.setSummary("Manual sync failed, no Internet connection found");
            syncItem.setIcon(R.drawable.ic_sync_activity_red);
            syncItem.setDisplayDate(null);
            adapter.notifyDataSetChanged();
        }

    }

    // Build 181 Resync Options
    public void manualReSync(long days) {
        if (isConnected()) {
            // Set Icon(0) will display the 'spinner'
            menuItems.get(0).setIcon(0);
            adapter.notifyDataSetChanged();
            // Request the sync on the Sync Adapter thread. Completion broadcasts the status which
            // is picked up below in the SyncReceiver
            // Convert the days into a date in the past (in millisecond format)
            Date timeNow = new Date();
            long checkDate = timeNow.getTime() - (days * 24 * 60 * 60 * 1000);
            syncManager.requestReSync(checkDate);
        } else {
            CRISMenuItem syncItem = menuItems.get(0);
            syncItem.setSummary("Manual sync failed, no Internet connection found");
            syncItem.setIcon(R.drawable.ic_sync_activity_red);
            syncItem.setDisplayDate(null);
            adapter.notifyDataSetChanged();
        }

    }
    private CRISMenuItem getSyncMenuItem() {

        String summary = "Initialising...";
        Date menuDate = null;
        // Set Icon(0) will display the 'spinner'
        int icon = 0;
        // If the current user is NoPriv then kick off a sync (a periodic sync is pending
        // but may not start immediately
        if (currentUser.getRoleID() == Role.noPrivilegeID) {
            syncManager.requestManualSync();
        } else {
            // Get the most recent sync activity record and proceed according to its status
            SyncActivity syncActivity;
            syncActivity = localDB.getLatestSyncActivity(currentUser);
            if (syncActivity == null) {
                // New database setup. Should have a NoPriv user but just in case:
                syncManager.requestManualSync();
            } else {
                menuDate = syncActivity.getCreationDate();
                String status = syncActivity.getResult();
                switch (status) {
                    case "SUCCESS":
                        summary = "Last update was successful.";
                        icon = R.drawable.ic_sync_activity_green;
                        break;
                    case "FAILURE":
                        summary = "Last update failed.";
                        icon = R.drawable.ic_sync_activity_red;
                        break;
                    case "PARTIAL":
                        summary = syncActivity.getSummary();
                        icon = R.drawable.ic_sync_activity_red;
                        syncManager.requestManualSync();
                        break;
                    case "PARTIAL_RECHECK":
                        summary = syncActivity.getSummary();
                        icon = R.drawable.ic_sync_activity_red;
                        // Build 181 - requestReSync already called in SyncReceiver
                        //syncManager.requestReSync();
                        break;
                    default:
                        summary = "Initialising: " + status;
                        syncManager.requestManualSync();
                }
            }
        }
        return new CRISMenuItem("Sync", summary, icon, menuDate);
    }

    /**
     * Build 179 Routine to fix spurious ListItem IDs caused by bug in SyncAdapter
     */
    private class FixItemListIdentifiers extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            LocalDB localDB = LocalDB.getInstance();
            User currentUser = User.getCurrentUser();
            int changes = 0;
            String output = "";
            // Get the ListItems in ListType, ItemValue, CreationDate order
            ArrayList<ListItem> listItems = new ArrayList<>();
            listItems = localDB.getAllListItemsByValue();
            String previousListType = "Initiating...";
            String previousItemValue = "Initiating...";
            UUID previousListItemID = null;
            for (ListItem listItem : listItems) {
                // Check for new ListItem
                if (!listItem.getListType().toString().equals(previousListType) ||
                        !listItem.getItemValue().equals(previousItemValue)) {
                    // Reset the previous values
                    previousListType = listItem.getListType().toString();
                    previousItemValue = listItem.getItemValue();
                    // In most cases, the earliest value is the one to propagate. However, there are a few cases
                    // where the earliest value is incorrect
                    if (previousListType.equals("GROUP") && previousItemValue.equals("Test Group 1")) {
                        previousListItemID = UUID.fromString("508ad0cc-949d-472a-94ad-dab51d899c08");

                    } else if (previousListType.equals("GROUP") && previousItemValue.equals("Test Group 2")) {
                        previousListItemID = UUID.fromString("c8deab65-656f-47ba-9d56-a213d8774439");

                    } else if (previousListType.equals("GROUP") && previousItemValue.equals("Test Group 3")) {
                        previousListItemID = UUID.fromString("d29d2fa8-efae-40dd-b3da-600ee6e5963f");

                    } else if (previousListType.equals("NOTE_TYPE") && previousItemValue.equals("Caring Role")) {
                        previousListItemID = UUID.fromString("f615b680-987d-4da9-a89c-fc1ffad037d7");

                    } else {
                        previousListItemID = listItem.getListItemID();
                    }
                }

                // Check that the ListItemID has not changed
                if (!listItem.getListItemID().equals(previousListItemID)) {
                    // Report the change (ListItem value and current ListItemID
                    output += String.format("%s %s\n", listItem.getListType().toString(), listItem.getItemValue());
                    output += String.format("%s\n", listItem.getListItemID().toString());
                    // Reset the ListItemID
                    listItem.setListItemID(previousListItemID);
                    localDB.listItemUpdate(listItem, currentUser);
                    // Report the change, new ListItemID
                    changes++;
                    output += String.format("->%s\n", previousListItemID.toString());
                }

            }
            output += String.format("Number of Changes: %d\n", changes);
            return output;
        }

        @Override
        protected void onPreExecute() {
            // Runs on UI Thread
        }

        @Override
        protected void onPostExecute(String output) {
            inProgress = false;
            // Runs on UI Thread
            Intent intent = new Intent(getBaseContext(), AlertAndContinue.class);
            intent.putExtra("title", String.format("Build 179 - Fix ListItem Identifiers"));
            output = "A previous bug accidentally modified some of the list items (gender, tier etc.) " +
                    "The following changes fix any problems in the local database.\n\n" +
                    output;
            intent.putExtra("message", output);
            startActivity(intent);
        }
    }

    private class MainMenuAdapter extends ArrayAdapter<CRISMenuItem> {

        MainMenuAdapter(Context context, List<CRISMenuItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_list_item, parent, false);
            }

            ImageView viewItemIcon = convertView.findViewById(R.id.item_icon);
            TextView viewItemDate = convertView.findViewById(R.id.item_date);
            TextView viewItemMainText = convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = convertView.findViewById(R.id.item_additional_text);
            TextView viewItemTitle = convertView.findViewById(R.id.item_title);
            ProgressBar syncProgress = convertView.findViewById(R.id.sync_progress);

            final CRISMenuItem menuItem = menuItems.get(position);
            String itemTitle = menuItem.getTitle();
            if (menuItem.getDocumentList() != null) {
                itemTitle += "(1 of " + menuItem.getDocumentList().size() + ")";
            }
            convertView.setTag(menuItem);

            if (menuItem.getSummary().length() == 0) {
                viewItemTitle.setVisibility(View.VISIBLE);
                if (menuItem.getSortString().startsWith("99")) {
                    //viewItemTitle.setTextSize(getResources().getDimension(R.dimen.text_size_hint));
                    viewItemTitle.setTextSize(15);
                }
                viewItemTitle.setText(itemTitle);
                viewItemMainText.setVisibility(View.GONE);
                viewItemAdditionalText.setVisibility(View.GONE);
            } else {
                viewItemTitle.setVisibility(View.GONE);
                viewItemMainText.setVisibility(View.VISIBLE);
                viewItemAdditionalText.setVisibility(View.VISIBLE);
                viewItemMainText.setText(itemTitle);
                viewItemAdditionalText.setText(menuItem.getSummary());
            }
            if (menuItem.getIcon() == 0) {
                syncProgress.setVisibility(View.VISIBLE);
                viewItemIcon.setVisibility(View.GONE);
            } else {
                syncProgress.setVisibility(View.GONE);
                viewItemIcon.setVisibility(View.VISIBLE);
                viewItemIcon.setImageDrawable(getDrawable(menuItem.getIcon()));
            }
            if (menuItem.getDisplayDate() != null) {
                // Check for no time
                SimpleDateFormat sTime = new SimpleDateFormat("HH:mm", Locale.UK);
                Date displayDate = menuItem.getDisplayDate();
                if (sTime.format(displayDate).equals("00:00")) {
                    SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
                    viewItemDate.setText(sDate.format(displayDate));
                } else {
                    SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK);
                    viewItemDate.setText(sDate.format(displayDate));
                }

            }
            return convertView;
        }
    }

    private class SyncReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            String status = extras.getString(SyncManager.SYNC_STATUS);
            String syncOrganisation = extras.getString(SyncManager.SYNC_ORGANISATION);
            // Get the organisation from  shared preferences
            String prefsOrganisation = "";
            SharedPreferences prefs = getSharedPreferences(getString(R.string.shared_preference_file), Context.MODE_PRIVATE);
            if (prefs.contains(getString(R.string.pref_organisation))) {
                prefsOrganisation = prefs.getString(getString(R.string.pref_organisation), "");
            }
            if (syncOrganisation != null && syncOrganisation.equals(prefsOrganisation)) {
                // Use a 'local' local database pointer in case the activity-wide one has not been set yet
                LocalDB localDB;
                try {
                    // Hopefully, LocalDB.setInstance will have been called in login.
                    localDB = LocalDB.getInstance();
                } catch (Exception ex) {
                    // However, Sync fires as soon as the app is opened so an exception can be received
                    // from the Sync Adapter even before the database has been initialised
                    // This is ignored by setting localDB to null
                    localDB = null;
                }
                // If we don't have a database or a current user yet, ignore the response from the
                // sync adaprter. Reload current user in case Login is active and has nulled it
                currentUser = User.getCurrentUser();
                if (localDB != null && currentUser != null && status != null) {
                    switch (status) {
                        case "SUCCESS":
                            // The sync may have succeeded or failed. Do a re-load from
                            // the database which will, at worst, re-load the NoPriv user
                            User newUser = localDB.getUser(currentUser.getUserID());
                            // There should be no circumstances where a user fails to be returned
                            // but, if so, leave the current user intact
                            if (newUser != null) {
                                // If the user is still NoPriv don't do anything more
                                if (newUser.getRoleID() != Role.noPrivilegeID) {
                                    // Check that the email address has not changed
                                    if (!newUser.getEmailAddress().equals(currentUser.getEmailAddress())) {
                                        // Force a new login (for new email address)
                                        String dbName = localDB.getDatabaseName();
                                        CRISUtil.invalidateOrg(context, dbName);
                                        User.setCurrentUser(null);
                                    } else if ((newUser.getEndDate().getTime() != Long.MIN_VALUE) &&
                                            (newUser.getEndDate().before(new Date()))) {
                                        // User is 'ended' so force a new login (which will fail)
                                        User.setCurrentUser(null);
                                    } else {
                                        // Replace the current user
                                        User.setCurrentUser(newUser);
                                    }
                                    currentUser = User.getCurrentUser();
                                }
                            } else {
                                throw new CRISException("No user found when reloading current user following sync.");
                            }
                            // Call onResume to display the result an allow the user to re-try
                            onResume();
                            break;
                        case "FAILURE":
                            // Exception occurred in Sync adapter when localDB not available so
                            // SyncActivity not created. Therefore, create one here:
                            User unknownUser = new User(User.unknownUser);
                            String exceptionMessage = extras.getString(SyncManager.SYNC_EXCEPTION_MESSAGE);
                            // Sync Adapter may be called before login has completed
                            SyncActivity syncActivity = new SyncActivity(unknownUser);
                            syncActivity.setResult("FAILURE");
                            // Build 181 - Changed exception message
                            //syncActivity.setSummary("Exception in Sync Adapter");
                            syncActivity.setSummary("Exception in Sync Adapter, see System Error message");
                            syncActivity.appendLog("Exception - " + exceptionMessage);
                            syncActivity.setCompletionDate(new Date());
                            localDB.save(syncActivity);
                            SystemError systemError = new SystemError(unknownUser,
                                    "Exception in Sync Adapter: " + exceptionMessage);
                            localDB.save(systemError);
                            // No change to current user since we don't know the state of the
                            // database. User has been authenticated so acn, at least, sync again
                            // Build 181 - Call onResume to display the result an allow the user to re-try
                            onResume();
                            break;
                        case "PARTIAL":
                            // Build 138 - Add Partial Initialisation
                            // Initialisation not complete so kick off another sync
                            syncManager.requestManualSync();
                            // Call onResume to display the result an allow the user to see the progress
                            onResume();
                            break;
                        case "PARTIAL_RECHECK":
                            // Build 181 - Add Partial RecheckInitialisation
                            // Initialisation not complete so kick off another sync
                            long partialRecheckDate = extras.getLong(SyncManager.SYNC_PARTIAL_RECHECK_DATE);
                            syncManager.requestReSync(partialRecheckDate);
                            // Call onResume to display the result an allow the user to see the progress
                            onResume();
                            break;

                        default:
                            throw new CRISException("Unexpected status return from Sync Adapter: " + status);
                    }

                }
            }
        }
    }
}

