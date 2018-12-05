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
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import solutions.cris.db.LocalDB;
import solutions.cris.edit.ChangePassword;
import solutions.cris.exceptions.CRISException;
import solutions.cris.list.ListClients;
import solutions.cris.list.ListLibrary;
import solutions.cris.list.ListSessions;
import solutions.cris.list.ListSyncActivity;
import solutions.cris.list.ListSysAdmin;
import solutions.cris.list.ListUsers;
import solutions.cris.object.Client;
import solutions.cris.object.Document;
import solutions.cris.object.Note;
import solutions.cris.object.NoteType;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.Role;
import solutions.cris.object.SyncActivity;
import solutions.cris.object.SystemError;
import solutions.cris.object.User;
import solutions.cris.read.ReadClientHeader;
import solutions.cris.sync.SyncManager;
//import solutions.cris.utils.CRISDeviceAdmin;
import solutions.cris.utils.CRISMenuItem;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.ExceptionHandler;

public class Main extends CRISActivity {

    public static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0;
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

    // Receiver for broadcasts when Sync Adapter completes each sync
    private SyncReceiver myReceiver;

    private ArrayList<CRISMenuItem> menuItems;
    private MainMenuAdapter adapter;
    private LocalDB localDB;
    private User currentUser;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        setContentView(R.layout.activity_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create new static instance of Sync Manager
        syncManager = SyncManager.getSyncManager(this);

        // and a Sync Receiver to get broadcasts from the Sync Adapter
        myReceiver = new SyncReceiver();
        registerReceiver(myReceiver, new IntentFilter(SyncManager.SYNC_ACTION));


    }

    @Override
    protected void onResume() {
        super.onResume();
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
            localDB.checkDBUpgrade(currentUser);

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
                // Initialise the main menu. Firstly setup the List view listeners
                ListView listView = findViewById(R.id.list_view);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        doListClick(view, position);
                    }
                });
                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        return doListLongClick(view);
                    }
                });
                // Initialise the list of Static Menu Items
                menuItems = new ArrayList<>();
                menuItems.add(0, getSyncMenuItem());

                // If the database is being initialised from the web the current user's role will
                // be NoPriv in which case don't offer any menu items
                if (currentUser.getRoleID() != Role.noPrivilegeID) {
                    menuItems.add(new CRISMenuItem("My Clients", "", R.drawable.ic_group_red, null));
                    menuItems.add(new CRISMenuItem("My Sessions", "", R.drawable.ic_sessions, null));
                    menuItems.add(new CRISMenuItem("All Clients", "", R.drawable.ic_group_green, null));
                    menuItems.add(new CRISMenuItem("Users", "", R.drawable.ic_group_blue, null));
                    menuItems.add(new CRISMenuItem("Library", "", R.drawable.ic_pdf_document, null));
                    if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_SYSTEM_ADMINISTRATOR)) {
                        menuItems.add(new CRISMenuItem("System Administration", "", R.drawable.ic_system_administration, null));
                    }
                    // Build 110 - Updated copyright date
                    //menuItems.add(new CRISMenuItem(String.format("CRIS v%s, \u00A9 2016, cris.solutions", BuildConfig.VERSION_NAME), "",
                    menuItems.add(new CRISMenuItem(String.format("CRIS v%s, \u00A9 2018, cris.solutions", BuildConfig.VERSION_NAME), "",
                            R.drawable.ic_cris_grey, null));
                    // Load the unread documents
                    getUnreadDocuments();
                }

                Collections.sort(menuItems, CRISMenuItem.comparatorAZ);
                // Create the Main menu
                adapter = new MainMenuAdapter(this, menuItems);
                // Display in the List View
                listView.setAdapter(adapter);

                // Create the /CRIS directory if necessary
                checkCRISDirectoryExists();
            }
        }
    }

    private void doListClick(View view, int position) {
        String title = ((CRISMenuItem) view.getTag()).getTitle();
        Intent intent;
        switch (title) {
            case "Sync":
                manualSync();
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
            case "System Administration":
                intent = new Intent(view.getContext(), ListSysAdmin.class);
                startActivity(intent);
                break;
            default:
                if (!title.startsWith("CRIS")) {
                    doReadDocument(position);
                }
        }
    }

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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.show_changes:
                showChanges();
                return true;

            case R.id.action_change_password:
                Intent intent = new Intent(this, ChangePassword.class);
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

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

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

    private void showChanges() {

        String changes = "Added icon in Session Register to show presence of sticky notes\n\n" +
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
                "--------------- Older Changes ---------------\n\n" +
                "Upgraded database interface.\n\n"  +
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
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

            ImageView viewItemIcon = (ImageView) convertView.findViewById(R.id.item_icon);
            TextView viewItemDate = (TextView) convertView.findViewById(R.id.item_date);
            TextView viewItemMainText = (TextView) convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = (TextView) convertView.findViewById(R.id.item_additional_text);
            TextView viewItemTitle = (TextView) convertView.findViewById(R.id.item_title);
            ProgressBar syncProgress = (ProgressBar) convertView.findViewById(R.id.sync_progress);

            final CRISMenuItem menuItem = menuItems.get(position);
            String itemTitle = menuItem.getTitle();
            if (menuItem.getDocumentList() != null) {
                itemTitle += "(1 of " + Integer.toString(menuItem.getDocumentList().size()) + ")";
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
                    default:
                        summary = "Initialising..." + status;
                        syncManager.requestManualSync();
                }
            }
        }
        return new CRISMenuItem("Sync", summary, icon, menuDate);
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
                            syncActivity.setSummary("Exception in Sync Adapter");
                            syncActivity.appendLog("Exception - " + exceptionMessage);
                            syncActivity.setCompletionDate(new Date());
                            localDB.save(syncActivity);
                            SystemError systemError = new SystemError(unknownUser,
                                    "Exception in Sync Adapter: " + exceptionMessage);
                            localDB.save(systemError);
                            // No change to current user since we don't know the state of the
                            // database. User has been authenticated so acn, at least, sync again
                            break;
                        default:
                            throw new CRISException("Unexpected status return from Sync Adpter: " + status);
                    }

                }
            }
        }
    }
}

