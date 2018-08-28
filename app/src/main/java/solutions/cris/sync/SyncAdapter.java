package solutions.cris.sync;

/**
 * Copyright CRIS.Solutions 31/10/2016.
 */

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.crypto.AESEncryption;
import solutions.cris.db.LocalDB;
import solutions.cris.db.LocalDBOpenHelper;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Sync;
import solutions.cris.object.SyncActivity;
import solutions.cris.object.SystemError;
import solutions.cris.object.User;

//import static solutions.cris.sync.JSONFactory.getPostJSON;
//import static solutions.cris.sync.JSONFactory.postJSON;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
class SyncAdapter extends AbstractThreadedSyncAdapter {

    // Global variables
    private LocalDB localDB;
    private String syncActivityID = "";
    private AESEncryption aesEncryption;
    private SyncActivity syncActivity;
    private long syncTimeOffset = 0;
    //private Context context = getContext();
    private static final int notificationID = 0;
    // Define a variable to contain a content resolver instance
    //ContentResolver mContentResolver;

    SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        //this.context = context;
        //If your app uses a content resolver, get an instance of it from the incoming Context
        //mContentResolver = context.getContentResolver();
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
    */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        //If your app uses a content resolver, get an instance of it from the incoming Context
        //mContentResolver = context.getContentResolver();
    }


    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {

        String prefOrganisation = "";

        // Check for new unread documents if the organisation is the current pref
        SharedPreferences prefs = getContext().getSharedPreferences(
                getContext().getString(R.string.shared_preference_file), Context.MODE_PRIVATE);
        if (prefs.contains(getContext().getString(R.string.pref_organisation))) {
            prefOrganisation = prefs.getString(getContext().getString(R.string.pref_organisation), "");
        }
        // The sync routines run on their own thread. Access to the database is
        // all that is common between the UI and the sync thread.
        // Tablet may be syncing more than one database/organisation via the
        // set of org_ files, each of which contains the organisation and the email
        // Each database will be synced in turn
        String[] files = getContext().fileList();
        for (String fileName : files) {
            syncActivityID = "";
            try {
                // Remove the org_ from the filename to give the database name
                if (fileName.startsWith("org_")) {
                    String dbName = fileName.substring(4);
                    FileInputStream fis;
                    byte[] bBuf;
                    fis = getContext().openFileInput(fileName);
                    bBuf = new byte[fis.available()];
                    int len = fis.read(bBuf);
                    fis.close();
                    if (len > 0) {
                        String sBuf = new String(bBuf);
                        String organisation = "";
                        String email = "";
                        if (!sBuf.isEmpty()) {
                            String[] lines = sBuf.split("\n");
                            organisation = lines[0];
                            if (lines.length > 1) {
                                email = lines[1];
                            }
                        }
                        if (organisation.isEmpty()) {
                            throw new CRISException("Attempting sync with no organisation in file: " + fileName);
                        } else if (email.isEmpty()) {
                            throw new CRISException("Attempting sync with no email in file: " + fileName);
                        } else if (!dbName.equals(AESEncryption.getDatabaseName(organisation))) {
                            throw new CRISException(String.format("Attempting sync for %s with invalid dbName: %s",
                                    AESEncryption.getDatabaseName(organisation), dbName));
                        } else {
                            boolean runningOrganisation = false;
                            if (prefOrganisation.equals(organisation)) {
                                runningOrganisation = true;
                            }
                            performOneSync(organisation, email, runningOrganisation);
                        }
                    }
                }

            } catch (Exception ex) {
                Intent intent = new Intent();
                intent.setAction(SyncManager.SYNC_ACTION);
                intent.putExtra(SyncManager.SYNC_STATUS, "FAILURE");
                intent.putExtra(SyncManager.SYNC_ACTIVITY_ID, syncActivityID);
                intent.putExtra(SyncManager.SYNC_EXCEPTION_MESSAGE, ex.getMessage());
                getContext().sendBroadcast(intent);
            }
        }
    }

    private void performOneSync(String organisation, String email, boolean runningOrganisation) {

        // Any failure of a web service call must raise an exception which will
        // log a system error. It is assumed that the exception will remain until
        // the problem has been rectified but it is not safe to continue synchronising
        // if an upload or download has failed.
        //
        // The order is:
        // 1. Check database for need to create/upgrade
        // 2. Download records and deal with any conflicts
        // 3. Upload

        int uploadCount = 0;
        int downloadCount;
        String syncStatus = "SUCCESS";
        String syncExceptionMessage = "";
        try {
            // Open the database on this thread
            localDB = LocalDB.getInstance(getContext(), organisation);
            // Instantiate the Encryption services on this thread
            aesEncryption = AESEncryption.setInstance(organisation, email);

            // Get the current user
            User currentUser = localDB.getUser(email);

            // Database is available so error handling will work from here on
            try {
                // Create a Sync Result record
                syncActivity = new SyncActivity(currentUser);
                syncActivityID = syncActivity.getRecordID().toString();
                // The sync date must be later than the latest sync date already
                // on the web server. Due to tablets potentially having incorrect time/date
                // settings, it is necessary to get the latest Sync record from the webserver
                // and ensure this date is later
                setSyncTimeOffset(localDB, syncActivity);
                // Get the number of unread documents pre-sync
                int unreadDocCount = localDB.getUnreadDocuments(currentUser).size();
                // Check web database exists and create tables if necessary
                checkWebDatabaseVersion(organisation);
                // Download new records
                downloadCount = downloadRecords();

                // Upload unsynced records
                uploadCount += uploadBlobTable();
                uploadCount += uploadReadAudits();
                uploadCount += uploadListItems();
                uploadCount += uploadUsers();
                uploadCount += uploadSystemErrors();
                uploadCount += uploadDocuments();
                uploadCount += uploadReadAudits();
                uploadCount += uploadFollows();

                // Check for new unread documents if the organisation is the current pref
                if (runningOrganisation) {
                    checkUnread(unreadDocCount, currentUser);
                }
                // Save the Sync Result record
                Date completionDate = new Date();
                float fElapsed = completionDate.getTime() - syncActivity.getCreationDate().getTime();
                fElapsed = fElapsed / 1000;
                syncActivity.setResult("SUCCESS");
                syncActivity.setSummary(String.format(Locale.UK,
                        "%d records uploaded, %d records downloaded. (%.1f seconds)",
                        uploadCount, downloadCount, fElapsed));
                syncActivity.appendLog("Successful completion.");
                syncActivity.setCompletionDate(completionDate);
                localDB.save(syncActivity);
            } catch (Exception ex) {
                // Log a System Error
                SystemError systemError = new SystemError(currentUser, ex);
                localDB.save(systemError);
                syncActivity.setResult("FAILURE");
                syncActivity.setSummary("Exception - " + ex.getMessage());
                StringWriter sbStackTrace = new StringWriter();
                ex.printStackTrace(new PrintWriter(sbStackTrace));
                syncActivity.appendLog("*************** Stack Trace *************\n" + sbStackTrace.toString());
                syncActivity.setCompletionDate(new Date());
                localDB.save(syncActivity);
            }
        } catch (Exception ex) {
            syncStatus = "FAILURE";
            syncExceptionMessage = ex.getMessage();
        } finally {
            // Send Broadcast if this was the currently running org
            Intent intent = new Intent();
            intent.setAction(SyncManager.SYNC_ACTION);
            intent.putExtra(SyncManager.SYNC_STATUS, syncStatus);
            intent.putExtra(SyncManager.SYNC_ACTIVITY_ID, syncActivityID);
            intent.putExtra(SyncManager.SYNC_EXCEPTION_MESSAGE, syncExceptionMessage);
            intent.putExtra(SyncManager.SYNC_ORGANISATION, organisation);
            getContext().sendBroadcast(intent);

        }
    }

    private void checkUnread(int previousCount, User currentUser) {
        int unreadDocCount = localDB.getUnreadDocuments(currentUser).size();
        syncActivity.appendLog(String.format(Locale.UK,
                "Check Unread Docs: %d new, %d total",
                unreadDocCount - previousCount, unreadDocCount));
        if (unreadDocCount == 0) {
            // Cancel any existing notification
            NotificationManager notificationManager =
                    (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationID);
            syncActivity.appendLog("Cleared Notification");
        } else {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(getContext())
                            .setAutoCancel(true)
                            .setColor(ResourcesCompat.getColor(getContext().getResources(), R.color.colorPrimary, null))
                            .setSmallIcon(R.drawable.ic_cris_notification)
                            .setContentTitle("Unread CRIS Documents");

            // Two different notifications depending on whether sync has found new unread documents
            if (unreadDocCount > previousCount) {
                builder.setContentText(String.format(Locale.UK,
                        "You have %d new unread documents (%d in total)",
                        unreadDocCount - previousCount, unreadDocCount));
                builder.setDefaults(Notification.DEFAULT_ALL);
            } else {
                builder.setContentText(String.format(Locale.UK,
                        "You have %d unread documents", unreadDocCount));
            }

            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(getContext(), Main.class);
            resultIntent.putExtra(Main.EXTRA_EMAIL_ADDRESS, currentUser.getEmailAddress());
            // The stack builder object will contain an artificial back stack for the
            // started Activity. Not strictlt necessary since we restart in Main
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getContext());
            stackBuilder.addParentStack(Main.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(notificationID, builder.build());
            syncActivity.appendLog("Updated/Created Notification");
        }
    }

    private void checkWebDatabaseVersion(String organisation) throws java.io.IOException {
        //JSONObject postJSON = getPostJSON(localDB);
        WebConnection webConnection = new WebConnection(localDB);
        //JSONObject jsonOutput = postJSON("check_database.php", postJSON);
        JSONObject jsonOutput = webConnection.post("check_database.php");
        try {
            String result = jsonOutput.getString("result");
            if (result.equals("SUCCESS")) {
                // Check for possible database upgrade
                int currentVersion = Integer.parseInt(jsonOutput.getString("version"));
                upgradeDatabase(currentVersion, localDB.getDatabaseVersion(), organisation);
            } else {
                // No User table found so create tables
                upgradeDatabase(-1, localDB.getDatabaseVersion(), organisation);
            }
        } catch (JSONException ex) {
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        }
    }

    private void upgradeDatabase(int oldVersion, int newVersion, String organisation) {
        int version = oldVersion;
        syncActivity.appendLog(String.format(Locale.UK,
                "Upgrade Check - (%d,%d)", oldVersion, newVersion));
        if (newVersion > oldVersion) {
            //JSONObject postJSON = getPostJSON(localDB);
            WebConnection webConnection = new WebConnection(localDB);
            try {
                // Create a set of upgrade SQL statements as JSON objects
                while (version < newVersion) {
                    version++;
                    int count = 0;
                    ArrayList<String> sqlList = LocalDBOpenHelper.getWebUpgradeSql(version);
                    for (String sql : sqlList) {
                        //postJSON.put("Update_" + Integer.toString(version) + "_" + Integer.toString(count++), sql);
                        webConnection.getInputJSON().put("Update_" + Integer.toString(version) + "_" + Integer.toString(count++), sql);
                    }
                }
                // Finally, write the organisation
                String sql = String.format("INSERT INTO Version (Organisation, VersionNumber) VALUES ('%s', '%s');",
                        organisation, newVersion);
                //postJSON.put("InsertVersion", sql);
                webConnection.getInputJSON().put("InsertVersion", sql);

            } catch (JSONException ex) {
                throw new CRISException("Error creating JSON object: " + ex.getMessage());
            }
            //JSONObject jsonOutput = postJSON("upgrade_database.php", postJSON);
            JSONObject jsonOutput = webConnection.post("upgrade_database.php");
            try {
                String result = jsonOutput.getString("result");
                if (result.equals("SUCCESS")) {
                    syncActivity.appendLog("Database upgraded.");
                } else {
                    // Get failure messages associated with input SQL JSON Objects
                    JSONArray names = jsonOutput.names();
                    String failureMessage = "";
                    for (int i = 0; i < names.length(); i++) {
                        failureMessage += names.get(i).toString() + ":" +
                                jsonOutput.getString(names.get(i).toString()) + "\n";
                    }
                    throw new CRISException("Errors in Web Database upgrade Sql: \n" + failureMessage);
                }
            } catch (JSONException ex) {
                throw new CRISException("Error parsing JSON data: " + ex.getMessage());
            }
        }
    }

    private int downloadRecords() {
        int count = 0;
        try {
            // Get the SyncIDs for syncs since last successful SyncActivity start date (CreationDate)
            JSONObject jsonSync = getDownloadSyncIDs();
            // Loop through the output rows
            JSONArray names = jsonSync.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                if (!name.equals("result")) {
                    JSONObject row = jsonSync.getJSONObject(names.getString(i));
                    // Ignoring SyncIDs in local database (ones this tablet created in last sync)
                    if (localDB.getSync(UUID.fromString(row.getString("SyncID"))) == null) {
                        Sync newSync = new Sync(row.getString("TableName"), 0);
                        newSync.setSyncID(UUID.fromString(row.getString("SyncID")));
                        newSync.setSyncDate(new Date(row.getLong("SyncDate")));
                        JSONObject jsonOutput;
                        //JSONObject postJSON = getPostJSON(localDB);
                        WebConnection webConnection = new WebConnection(localDB);
                        JSONObject syncValues = new JSONObject();
                        syncValues.put("SyncID", newSync.getSyncID().toString());
                        syncValues.put("TableName", newSync.getTableName());
                        //json.put("sync", syncValues);
                        webConnection.getInputJSON().put("sync", syncValues);
                        //jsonOutput = postJSON("get_records.php", json);
                        jsonOutput = webConnection.post("get_records.php");
                        String result = jsonOutput.getString("result");
                        if (result.equals("FAILURE")) {
                            throw new CRISException("get_records.php: " + jsonOutput.getString("error_message"));
                        } else {
                            int records;
                            switch (row.getString("TableName")) {
                                case "ListItem":
                                    records = localDB.downloadListItems(jsonOutput, newSync);
                                    break;
                                case "User":
                                    records = localDB.downloadUsers(jsonOutput, newSync);
                                    break;
                                case "Document":
                                    records = localDB.downloadDocuments(jsonOutput, newSync);
                                    break;
                                case "SystemError":
                                    records = localDB.downloadSystemErrors(jsonOutput, newSync);
                                    break;
                                case "Blobs":
                                    records = localDB.downloadBlobs(jsonOutput, newSync);
                                    break;
                                case "ReadAudit":
                                    records = localDB.downloadReadAudits(jsonOutput, newSync);
                                    break;
                                case "Follow":
                                    records = localDB.downloadFollows(jsonOutput, newSync);
                                    break;
                                default:
                                    throw new CRISException("Unexpected table name in downloadRecords: " + row.getString("TableName"));
                            }
                            syncActivity.appendLog(String.format(Locale.UK,
                                    "Downloaded %s (%d)", row.getString("TableName"), records));
                            count += records;
                        }
                    }
                }
            }
        } catch (JSONException ex) {
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        }

        return count;
    }

    private JSONObject getDownloadSyncIDs() {
        JSONObject jsonOutput;
        Long lastSync = Long.MIN_VALUE;
        //JSONObject postJSON = getPostJSON(localDB);
        WebConnection webConnection = new WebConnection(localDB);
        try {
            JSONObject values = new JSONObject();
            values.put("SyncDate", lastSync);
            //json.put("sync", values);
            webConnection.getInputJSON().put("sync", values);
            //jsonOutput = postJSON("get_sync_records.php", json);
            jsonOutput = webConnection.post("get_sync_records.php");
            String result = jsonOutput.getString("result");
            if (result.equals("FAILURE")) {
                throw new CRISException("get_sync_records.php: " + jsonOutput.getString("error_message"));
            }

        } catch (Exception ex) {
            throw new CRISException(ex.getMessage());
        }
        return jsonOutput;
    }

    private int uploadReadAudits() {
        int count = 0;
        Sync sync = new Sync("ReadAudit", syncTimeOffset);

        // Update SyncID
        localDB.updateSyncID("ReadAudit", sync.getSyncID());
        Cursor cursor = localDB.getRecordsBySyncID("ReadAudit", sync.getSyncID());
        if (cursor.getCount() > 0) {
            try {
                //JSONObject postJSON = getPostJSON(localDB);
                WebConnection webConnection = new WebConnection(localDB);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    // Create JSON Object for this record
                    JSONObject values = new JSONObject();
                    values.put("ReadByID", cursor.getString(0));
                    values.put("DocumentID", cursor.getString(1));
                    values.put("SyncID", cursor.getString(2));
                    values.put("ReadDate", cursor.getLong(3));
                    //json.put("ReadAudit_" + Integer.toString(count++), values);
                    webConnection.getInputJSON().put("ReadAudit_" + Integer.toString(count++), values);
                    cursor.moveToNext();
                }
                // Add in the Sync record
                JSONObject values = new JSONObject();
                values.put("SyncID", sync.getSyncID().toString());
                values.put("SyncDate", sync.getSyncDate().getTime());
                values.put("TableName", sync.getTableName());
                //json.put("sync", values);
                webConnection.getInputJSON().put("sync", values);
                //JSONObject jsonOutput = postJSON("insert_read_audits.php", json);
                JSONObject jsonOutput = webConnection.post("insert_read_audits.php");
                try {
                    String result = jsonOutput.getString("result");
                    if (result.equals("FAILURE")) {
                        throw new CRISException(jsonOutput.getString("error_message"));
                    }
                } catch (JSONException ex) {
                    throw new CRISException("Error parsing JSON data: " + ex.getMessage());
                }
                try {
                    localDB.save(sync);
                }
                catch (Exception ex){
                    throw new CRISException("Error saving sync - " + ex.getMessage());
                }
                syncActivity.appendLog(String.format(Locale.UK,
                        "Uploaded ReadAudits (%d)", count));
            } catch (Exception ex) {
                // Remove the SyncID
                localDB.nullSyncID("ReadAudit", sync.getSyncID());
                throw new CRISException(ex.getMessage());
            }
        }
        cursor.close();
        return count;
    }

    private int uploadFollows() {
        int count = 0;
        Sync sync = new Sync("Follow", syncTimeOffset);

        // Update SyncID
        localDB.updateSyncID("Follow", sync.getSyncID());
        Cursor cursor = localDB.getRecordsBySyncID("Follow", sync.getSyncID());
        if (cursor.getCount() > 0) {
            try {
                //JSONObject postJSON = getPostJSON(localDB);
                WebConnection webConnection = new WebConnection(localDB);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    // Create JSON Object for this record
                    JSONObject values = new JSONObject();
                    values.put("UserID", cursor.getString(0));
                    values.put("ClientID", cursor.getString(1));
                    values.put("SyncID", cursor.getString(2));
                    values.put("Cancelled", cursor.getInt(3));
                    values.put("StartDate", cursor.getLong(4));
                    //json.put("Follow_" + Integer.toString(count++), values);
                    webConnection.getInputJSON().put("Follow_" + Integer.toString(count++), values);
                    cursor.moveToNext();
                }
                // Add in the Sync record
                JSONObject values = new JSONObject();
                values.put("SyncID", sync.getSyncID().toString());
                values.put("SyncDate", sync.getSyncDate().getTime());
                values.put("TableName", sync.getTableName());
                //json.put("sync", values);
                webConnection.getInputJSON().put("sync", values);
                //JSONObject jsonOutput = postJSON("insert_follows_2.php", json);
                JSONObject jsonOutput = webConnection.post("insert_follows_2.php");
                try {
                    String result = jsonOutput.getString("result");
                    if (result.equals("FAILURE")) {
                        throw new CRISException(jsonOutput.getString("error_message"));
                    }
                } catch (JSONException ex) {
                    throw new CRISException("Error parsing JSON data: " + ex.getMessage());
                }
                localDB.save(sync);
                syncActivity.appendLog(String.format(Locale.UK, "Uploaded Follows (%d)", count));
            } catch (Exception ex) {
                // Remove the SyncID
                localDB.nullSyncID("Follow", sync.getSyncID());
                throw new CRISException(ex.getMessage());
            }
        }
        cursor.close();
        return count;
    }

    private int uploadListItems() {
        int count = 0;
        Sync sync = new Sync("ListItem", syncTimeOffset);

        // Update SyncID
        localDB.updateSyncID("ListItem", sync.getSyncID());
        Cursor cursor = localDB.getRecordsBySyncID("ListItem", sync.getSyncID());
        if (cursor.getCount() > 0) {
            try {
                //JSONObject postJSON = getPostJSON(localDB);
                WebConnection webConnection = new WebConnection(localDB);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    // Create JSON Object for this record
                    JSONObject values = new JSONObject();
                    values.put("RecordID", cursor.getString(0));
                    values.put("ListItemID", cursor.getString(1));
                    values.put("HistoryDate", cursor.getLong(2));
                    values.put("SyncID", cursor.getString(3));
                    values.put("CreationDate", cursor.getLong(4));
                    values.put("CreatedByID", cursor.getString(5));
                    values.put("ListType", cursor.getString(6));
                    values.put("ItemValue", cursor.getString(7));
                    values.put("IsDisplayed", cursor.getLong(8));
                    // Decrypt the Web blob using the Web cipher
                    byte[] decrypted = aesEncryption.decrypt(AESEncryption.LOCAL_CIPHER, cursor.getBlob(9));
                    // Encrypt using Local cipher
                    byte[] encrypted = aesEncryption.encrypt(AESEncryption.WEB_CIPHER, decrypted);
                    // Write to the local database
                    values.put("SerialisedObject", Base64.encodeToString(encrypted, Base64.DEFAULT));
                    // Add to overall JSON object
                    //json.put("ListItem_" + Integer.toString(count++), values);
                    webConnection.getInputJSON().put("ListItem_" + Integer.toString(count++), values);
                    cursor.moveToNext();
                }
                // Add in the Sync record
                JSONObject values = new JSONObject();
                values.put("SyncID", sync.getSyncID().toString());
                values.put("SyncDate", sync.getSyncDate().getTime());
                values.put("TableName", sync.getTableName());
                //json.put("sync", values);
                webConnection.getInputJSON().put("sync", values);
                //JSONObject jsonOutput = postJSON("insert_list_items.php", json);
                JSONObject jsonOutput = webConnection.post("insert_list_items.php");
                try {
                    String result = jsonOutput.getString("result");
                    if (result.equals("FAILURE")) {
                        throw new CRISException(jsonOutput.getString("error_message"));
                    }
                } catch (JSONException ex) {
                    throw new CRISException("Error parsing JSON data: " + ex.getMessage());
                }
                localDB.save(sync);
                syncActivity.appendLog(String.format(Locale.UK, "Uploaded ListItems (%d)", count));
            } catch (Exception ex) {
                // Remove the SyncID
                localDB.nullSyncID("ListItem", sync.getSyncID());
                throw new CRISException(ex.getMessage());
            }
        }
        cursor.close();
        return count;
    }

    private int uploadUsers() {
        int count = 0;
        Sync sync = new Sync("User", syncTimeOffset);
        // Update SyncID
        localDB.updateSyncID("User", sync.getSyncID());
        Cursor cursor = localDB.getRecordsBySyncID("User", sync.getSyncID());
        if (cursor.getCount() > 0) {
            try {
                //JSONObject postJSON = getPostJSON(localDB);
                WebConnection webConnection = new WebConnection(localDB);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    // Create JSON Object for this record
                    JSONObject values = new JSONObject();
                    values.put("RecordID", cursor.getString(0));
                    values.put("UserID", cursor.getString(1));
                    values.put("HistoryDate", cursor.getLong(2));
                    values.put("SyncID", cursor.getString(3));
                    values.put("CreationDate", cursor.getLong(4));
                    values.put("CreatedByID", cursor.getString(5));
                    values.put("EmailAddress", cursor.getString(6));
                    values.put("Name", cursor.getString(7));
                    // Decrypt the Web blob using the Web cipher
                    byte[] decrypted = aesEncryption.decrypt(AESEncryption.LOCAL_CIPHER,cursor.getBlob(8));
                    // Encrypt using Local cipher
                    byte[] encrypted  = aesEncryption.encrypt(AESEncryption.WEB_CIPHER,decrypted);
                    // Write to the web
                    values.put("SerialisedObject", Base64.encodeToString(encrypted, Base64.DEFAULT));
                    syncActivity.appendLog("Name: " + cursor.getString(7));
                    // Add to overall JSON object
                    //json.put("User_" + Integer.toString(count++), values);
                    webConnection.getInputJSON().put("User_" + Integer.toString(count++), values);
                    cursor.moveToNext();
                }
                // Add in the Sync record
                JSONObject values = new JSONObject();
                values.put("SyncID", sync.getSyncID().toString());
                values.put("SyncDate", sync.getSyncDate().getTime());
                values.put("TableName", sync.getTableName());
                //json.put("sync", values);
                webConnection.getInputJSON().put("sync", values);
                //JSONObject jsonOutput = postJSON("insert_users.php", json);
                JSONObject jsonOutput = webConnection.post("insert_users.php");
                try {
                    String result = jsonOutput.getString("result");
                    if (result.equals("FAILURE")) {
                        throw new CRISException(jsonOutput.getString("error_message"));
                    }
                } catch (JSONException ex) {
                    throw new CRISException("Error parsing JSON data: " + ex.getMessage());
                }
                localDB.save(sync);
                syncActivity.appendLog(String.format(Locale.UK, "Upload Users (%d)", count));
            } catch (Exception ex) {
                // Remove the SyncID
                localDB.nullSyncID("User", sync.getSyncID());
                throw new CRISException(ex.getMessage());
            }
        }
        cursor.close();
        return count;
    }

    private int uploadSystemErrors() {
        int count = 0;
        Sync sync = new Sync("SystemError", syncTimeOffset);

        // Update SyncID
        localDB.updateSyncID("SystemError", sync.getSyncID());
        Cursor cursor = localDB.getRecordsBySyncID("SystemError", sync.getSyncID());
        if (cursor.getCount() > 0) {
            try {
                //JSONObject postJSON = getPostJSON(localDB);
                WebConnection webConnection = new WebConnection(localDB);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    // Create JSON Object for this record
                    JSONObject values = new JSONObject();
                    values.put("RecordID", cursor.getString(0));
                    values.put("SyncID", cursor.getString(1));
                    values.put("CreationDate", cursor.getLong(2));
                    values.put("CreatedByID", cursor.getString(3));
                    /*
                    // Decrypt the Web blob using the Web cipher
                    byte[] decrypted = aesEncryption.decrypt(AESEncryption.LOCAL_CIPHER,cursor.getBlob(4));
                    // Encrypt using Local cipher
                    byte[] encrypted  = aesEncryption.encrypt(AESEncryption.WEB_CIPHER,decrypted);
                    // Write to the local database
                    values.put("SerialisedObject", Base64.encodeToString(encrypted, Base64.DEFAULT));
                    */
                    values.put("SerialisedObject", Base64.encodeToString(cursor.getBlob(4), Base64.DEFAULT));
                    // Add to overall JSON object
                    //json.put("SystemError_" + Integer.toString(count++), values);
                    webConnection.getInputJSON().put("SystemError_" + Integer.toString(count++), values);
                    cursor.moveToNext();
                }
                // Add in the Sync record
                JSONObject values = new JSONObject();
                values.put("SyncID", sync.getSyncID().toString());
                values.put("SyncDate", sync.getSyncDate().getTime());
                values.put("TableName", sync.getTableName());
                //json.put("sync", values);
                webConnection.getInputJSON().put("sync", values);
                //JSONObject jsonOutput = postJSON("insert_system_errors.php", json);
                JSONObject jsonOutput = webConnection.post("insert_system_errors.php");
                try {
                    String result = jsonOutput.getString("result");
                    if (result.equals("FAILURE")) {
                        throw new CRISException("test - " + jsonOutput.getString("error_message"));
                    }
                } catch (JSONException ex) {
                    throw new CRISException("Error parsing JSON data: " + ex.getMessage());
                }
                localDB.save(sync);
                syncActivity.appendLog(String.format(Locale.UK, "Upload SystemErrors (%d)", count));
            } catch (JSONException ex) {
                // Remove the SyncID
                localDB.nullSyncID("SystemError", sync.getSyncID());
                throw new CRISException("Error parsing JSON data: " + ex.getMessage());
            } catch (Exception ex) {
                // Remove the SyncID
                localDB.nullSyncID("SystemError", sync.getSyncID());
                throw ex;
            }
        }
        cursor.close();
        return count;
    }

    private int uploadDocuments() {
        int count = 0;
        Sync sync = new Sync("Document", syncTimeOffset);
        // Update SyncID
        localDB.updateSyncID("Document", sync.getSyncID());
        Cursor cursor = localDB.getRecordsBySyncID("Document", sync.getSyncID());
        if (cursor.getCount() > 0) {
            try {
                //JSONObject postJSON = getPostJSON(localDB);
                WebConnection webConnection = new WebConnection(localDB);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    // Create JSON Object for this record
                    JSONObject values = new JSONObject();
                    values.put("RecordID", cursor.getString(0));
                    values.put("DocumentID", cursor.getString(1));
                    values.put("HistoryDate", cursor.getLong(2));
                    values.put("SyncID", cursor.getString(3));
                    values.put("CreationDate", cursor.getLong(4));
                    values.put("CreatedByID", cursor.getString(5));
                    values.put("Cancelled", cursor.getLong(6));
                    values.put("ClientID", cursor.getString(7));
                    values.put("DocumentType", cursor.getInt(8));
                    // Decrypt the Web blob using the Web cipher
                    byte[] decrypted = aesEncryption.decrypt(AESEncryption.LOCAL_CIPHER, cursor.getBlob(9));
                    // Encrypt using Local cipher
                    byte[] encrypted = aesEncryption.encrypt(AESEncryption.WEB_CIPHER, decrypted);
                    // Write to the local database
                    values.put("SerialisedObject", Base64.encodeToString(encrypted, Base64.DEFAULT));
                    // Added in version 2
                    long referenceDate;
                    try {
                        referenceDate = cursor.getLong(10);
                    } catch (Exception ex) {
                        referenceDate = Long.MIN_VALUE;
                    }
                    values.put("ReferenceDate", referenceDate);
                    // Added in version 18
                    String sessionID;
                    try {
                        sessionID = cursor.getString(11);
                    } catch (Exception ex) {
                        sessionID = "";
                    }
                    values.put("SessionID", sessionID);
                    // Add to overall JSON object
                    //json.put("Document_" + Integer.toString(count++), values);
                    webConnection.getInputJSON().put("Document_" + Integer.toString(count++), values);
                    cursor.moveToNext();
                }
                // Add in the Sync record
                JSONObject values = new JSONObject();
                values.put("SyncID", sync.getSyncID().toString());
                values.put("SyncDate", sync.getSyncDate().getTime());
                values.put("TableName", sync.getTableName());
                //json.put("sync", values);
                webConnection.getInputJSON().put("sync", values);
                // ReferenceDate column added in version 2
                //JSONObject jsonOutput = postJSON("insert_documents_2.php", json);
                //JSONObject jsonOutput = webConnection.post("insert_documents_2.php");
                // SessionID added in version 18
                JSONObject jsonOutput = webConnection.post("insert_documents_18.php");
                try {
                    String result = jsonOutput.getString("result");
                    if (result.equals("FAILURE")) {
                        throw new CRISException(jsonOutput.getString("error_message"));
                    }
                } catch (JSONException ex) {
                    throw new CRISException("Error parsing JSON data: " + ex.getMessage());
                }
                localDB.save(sync);
                syncActivity.appendLog(String.format(Locale.UK,"Upload Documents (%d)", count));
            } catch (JSONException ex) {
                // Remove the SyncID
                localDB.nullSyncID("Document", sync.getSyncID());
                throw new CRISException("Error parsing JSON data: " + ex.getMessage());
            } catch (Exception ex) {
                // Remove the SyncID
                localDB.nullSyncID("Document", sync.getSyncID());
                throw ex;
            }
        }
        cursor.close();
        return count;
    }

    private int uploadBlobTable() {
        int count = 0;
        while (uploadOneBlob()) {
            count++;
        }
        if (count > 0) {
            syncActivity.appendLog(String.format(Locale.UK, "Upload Blobs (%d)", count));
        }
        return count;
    }

    private boolean uploadOneBlob() {
        boolean success = false;
        Sync sync = new Sync("Blobs", syncTimeOffset);
        //JSONObject postJSON = getPostJSON(localDB);
        WebConnection webConnection = new WebConnection(localDB);
        Cursor cursor = localDB.getOneUnsyncedBlob(sync.getSyncID());
        if (cursor.getCount() > 0) {
            try {
                cursor.moveToFirst();
                JSONObject values = new JSONObject();
                values.put("blobID", cursor.getString(0));
                values.put("syncID", cursor.getString(1));
                // Decrypt the Web blob using the Web cipher
                byte[] decrypted = aesEncryption.decrypt(AESEncryption.LOCAL_CIPHER, cursor.getBlob(2));
                // Encrypt using Local cipher
                byte[] encrypted = aesEncryption.encrypt(AESEncryption.WEB_CIPHER, decrypted);
                // Write to the local database
                values.put("content", Base64.encodeToString(encrypted, Base64.DEFAULT));
                values.put("NextChunk", cursor.getString(3));
                //json.put("blob", values);
                webConnection.getInputJSON().put("blob", values);
                // Add in the Sync record
                values = new JSONObject();
                values.put("SyncID", sync.getSyncID().toString());
                values.put("SyncDate", sync.getSyncDate().getTime());
                values.put("TableName", sync.getTableName());
                //json.put("sync", values);
                webConnection.getInputJSON().put("sync", values);
                //JSONObject jsonOutput = postJSON("insert_blob.php", json);
                JSONObject jsonOutput = webConnection.post("insert_blob.php");
                String result = jsonOutput.getString("result");
                if (result.equals("FAILURE")) {
                    throw new CRISException(jsonOutput.getString("error_message"));
                }
                localDB.save(sync);
                success = true;
            } catch (Exception ex) {
                // Remove the SyncID
                localDB.nullSyncID("Blobs", sync.getSyncID());
                throw new CRISException(ex.getMessage());
            } finally {
                cursor.close();
            }
        }
        return success;
    }

    private void setSyncTimeOffset(LocalDB localDB, SyncActivity syncActivity) {
        //JSONObject postJSON = getPostJSON(localDB);
        WebConnection webConnection = new WebConnection(localDB);
        //JSONObject jsonOutput = postJSON("get_latest_sync_date.php", postJSON);
        JSONObject jsonOutput = webConnection.post("get_latest_sync_date.php");
        String syncDate = "";
        try {
            String result = jsonOutput.getString("result");
            if (result.equals("SUCCESS")) {
                syncDate = jsonOutput.getString("sync_date");
                if (!syncDate.equals("null")) {
                    // Check for possible database upgrade
                    long latestSyncTime = Long.parseLong(syncDate);
                    long currentSyncTime = syncActivity.getCreationDate().getTime();
                    if (latestSyncTime >= currentSyncTime) {
                        // Use a date 1 second later than the latest Sync Date on the server
                        syncTimeOffset = currentSyncTime - latestSyncTime + 1000;
                    }
                }
            } else {
                String error_message = jsonOutput.getString("error_message");
                if (error_message.equals("Table Sync is empty")) {
                    // No sync records yet
                    syncTimeOffset = 0;
                } else {
                    throw new CRISException(jsonOutput.getString("error_message"));
                }
            }
            float offset = syncTimeOffset / 1000;
            syncActivity.appendLog(String.format(Locale.UK, "Sync Time Offset: %.1f", offset));
        } catch (JSONException ex) {
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        } catch (Exception ex) {
            throw new CRISException("Exception: (" + syncDate + ") - " + ex.getMessage());
        }

    }
}






