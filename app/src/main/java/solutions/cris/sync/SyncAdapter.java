package solutions.cris.sync;

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

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.icu.util.DateInterval;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.res.ResourcesCompat;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
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
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    // Global variables
    private LocalDB localDB;
    private String syncActivityID = "";
    private AESEncryption aesEncryption;
    private SyncActivity syncActivity;
    private long syncTimeOffset = 0;
    // Build 181 - Set when recheck hits PARTIAL limit to indicate the new recheck date
    long partialRecheckDate = 0;
    //private Context context = getContext();
    private static final int notificationID = 0;
    // Define a variable to contain a content resolver instance
    //ContentResolver mContentResolver;

    public final static int IGNORE = 0;
    public final static int ADD = 1;
    public final static int RECHECK = 2;

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

        // Build 181 - Alter exception handler to go to next file if it's a file error
        // but crash completely if it's a sync error
        String organisation = "";
        //boolean runningOrganisation = false;
        String prefOrganisation = "";

        try {
            // Build 181 Get running organisation and execure manual or automatic syncs accordingly
            // Check for new unread documents if the organisation is the current pref
            SharedPreferences prefs = getContext().getSharedPreferences(
                    getContext().getString(R.string.shared_preference_file), Context.MODE_PRIVATE);
            if (prefs.contains(getContext().getString(R.string.pref_organisation))) {
                prefOrganisation = prefs.getString(getContext().getString(R.string.pref_organisation), "");
            }

            // Build 181 - Get the re-sync from date from the extras (zero for no resync)
            Date timeNow = new Date();
            long recheckDate = extras.getLong(SyncManager.SYNC_RECHECK_DATE, timeNow.getTime());
            boolean isRecheck = extras.getBoolean(SyncManager.SYNC_RECHECK, false);
            boolean isManual = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
            // Build 181 - Ensure partial receheck data is now (defined earlier as zero
            partialRecheckDate = timeNow.getTime();

            String diagString = "Automatic - ";
            if (isManual) {
                diagString = "Manual - ";
                if (isRecheck) {
                    diagString += "Recheck";
                } else {
                    diagString += "No Recheck";
                }
            }

            // The sync routines run on their own thread. Access to the database is
            // all that is common between the UI and the sync thread.
            // Tablet may be syncing more than one database/organisation via the
            // set of org_ files, each of which contains the organisation and the email
            // Each database will be synced in turn


            String[] files = getContext().fileList();
            for (String fileName : files) {
                syncActivityID = "";

                // Remove the org_ from the filename to give the database name
                if (fileName.startsWith("org_")) {
                    String dbName = fileName.substring(4);
                    FileInputStream fis;
                    byte[] bBuf;
                    String email = "";

                    fis = getContext().openFileInput(fileName);
                    bBuf = new byte[fis.available()];
                    int len = fis.read(bBuf);
                    fis.close();
                    if (len > 0) {
                        String sBuf = new String(bBuf);

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
                        }
                        // Build 181 - Check whether this is the organisation which is currently logged in
                        if (isManual || isRecheck) {
                            if (prefOrganisation.equals(organisation)) {
                                performOneSync(organisation, email, prefOrganisation, organisation, recheckDate, diagString);
                            }
                        } else {
                            performOneSync(organisation, email, prefOrganisation, organisation, recheckDate, "Automatic");
                        }
                    }
                }
            }
        } catch (
                Exception ex) {
            // Build 181 Broadcast with prefOrganisation so that Main.SyncReceiver() handles it
            Intent intent = new Intent();
            intent.setAction(SyncManager.SYNC_ACTION);
            intent.putExtra(SyncManager.SYNC_STATUS, "FAILURE");
            intent.putExtra(SyncManager.SYNC_ACTIVITY_ID, syncActivityID);
            intent.putExtra(SyncManager.SYNC_EXCEPTION_MESSAGE, String.format("Sync Org: %s\n%s", organisation, ex.getMessage()));
            intent.putExtra(SyncManager.SYNC_ORGANISATION, prefOrganisation);
            getContext().sendBroadcast(intent);
        }

    }

    private void performOneSync(String organisation,
                                String email,
                                String prefOrganisation,
                                String Organisation,
                                long recheckDate,
                                String diagString) {

        // Any failure of a web service call must raise an exception which will
        // log a system error. It is assumed that the exception will remain until
        // the problem has been rectified but it is not safe to continue synchronising
        // if an upload or download has failed.
        //
        // The order is:
        // 1. Check database for need to create/upgrade
        // 2. Download records and deal with any conflicts
        // 3. Upload
        SimpleDateFormat sDate = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
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

                syncActivity.appendLog(diagString);
                Date tempDate = new Date(recheckDate);
                syncActivity.appendLog("Recheck date: " + sDate.format(tempDate));
                // Build 181
                syncActivity.appendLog("Diag date: " + sDate.format(syncActivity.getCreationDate()));
                //syncActivity.appendLog(String.format(Locale.UK, "Diag date: %d", syncActivity.getCreationDate().getTime()));
                syncActivityID = syncActivity.getRecordID().toString();
                // Build 107 - Added Upload Test
                //uploadTest();
                syncActivity.appendLog("Upload Test Complete");
                // The sync date must be later than the latest sync date already
                // on the web server. Due to tablets potentially having incorrect time/date
                // settings, it is necessary to get the latest Sync record from the webserver
                // and ensure this date is later
                setSyncTimeOffset(localDB, syncActivity);
                // Get the number of unread documents pre-sync
                int unreadDocCount = localDB.getUnreadDocuments(currentUser).size();
                // Check web database exists and create tables if necessary
                checkWebDatabaseVersion(organisation);  // Done
                // Download new records
                downloadCount = downloadRecords(recheckDate);
                // Build 143 - Download Website MyWeek Documents
                downloadCount += downloadMyWeeks();

                // Build 128 - Unsynced records are in batches of 200 for repeat until count = 0
                // Upload unsynced records
                uploadCount += uploadBlobTable();       // Done
                int count = 1;
                while (count > 0) {
                    count = uploadReadAudits();
                    uploadCount += count;
                }
                count = 1;
                while (count > 0) {
                    count = uploadListItems();
                    uploadCount += count;
                }
                count = 1;
                while (count > 0) {
                    count = uploadUsers();
                    uploadCount += count;
                }
                count = 1;
                while (count > 0) {
                    count = uploadSystemErrors();
                    uploadCount += count;
                }
                count = 1;
                while (count > 0) {
                    count = uploadDocuments();
                    uploadCount += count;
                }
                count = 1;
                while (count > 0) {
                    count = uploadReadAudits();
                    uploadCount += count;
                }
                count = 1;
                while (count > 0) {
                    count = uploadFollows();
                    uploadCount += count;
                }

                // Check for new unread documents if the organisation is the current pref
                if (prefOrganisation.equals(organisation)) {
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
                // Build 181 Add PARTIAL_RECHECK handler. Load max of 200 syncs at a time
                if (ex.getMessage().startsWith("PARTIAL_RECHECK")) {
                    syncActivity.setResult("PARTIAL_RECHECK");
                    syncActivity.setSummary(ex.getMessage());
                    syncActivity.setCompletionDate(new Date());
                    localDB.save(syncActivity);
                    syncStatus = "PARTIAL_RECHECK";
                }
                // Build 138 - Add Partial Downloads. Load max of 200 syncs at a time
                else if (ex.getMessage().startsWith("PARTIAL")) {
                    syncActivity.setResult("PARTIAL");
                    syncActivity.setSummary(ex.getMessage());
                    syncActivity.setCompletionDate(new Date());
                    localDB.save(syncActivity);
                    syncStatus = "PARTIAL";
                } else {
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
                    syncStatus = "FAILURE";
                    // Build 184 - Report exception Message
                    syncExceptionMessage = ex.getMessage();
                }
            }
        } catch (Exception ex) {
            // Broadcast a failure. Unless this is the running orgaisation it will be ignored
            syncStatus = "FAILURE";
            syncExceptionMessage = ex.getMessage();
        } finally {
            // Build 181 Any broadcast from a non-running organisation will be ignored in Main.SyncReceiver()
            Intent intent = new Intent();
            intent.setAction(SyncManager.SYNC_ACTION);
            intent.putExtra(SyncManager.SYNC_STATUS, syncStatus);
            intent.putExtra(SyncManager.SYNC_ACTIVITY_ID, syncActivityID);
            intent.putExtra(SyncManager.SYNC_EXCEPTION_MESSAGE, syncExceptionMessage);
            intent.putExtra(SyncManager.SYNC_ORGANISATION, organisation);
            // Build 181 Add partial receck data in case its a PARTIAL_RECHECK
            intent.putExtra(SyncManager.SYNC_PARTIAL_RECHECK_DATE, partialRecheckDate);
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
            // Build 150 - Added Channel ID
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(getContext(), Main.CHANNEL_ID)
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
            // Build 230 Crash: Targeting S+ (version 31 and above) requires that one of
            // FLAG_IMMUTABLE or FLAG_MUTABLE be specified when creating a PendingIntent.
            PendingIntent resultPendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            }
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
        JSONObject jsonOutput = webConnection.post("pdo_check_database.php");
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
                        webConnection.getInputJSON().put("Update_" + version + "_" + count++, sql);
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
            JSONObject jsonOutput = webConnection.post("pdo_upgrade_database.php");
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

    private int downloadMyWeeks() {
        // Check for any new MyWeeks from the MyWeek Website and add to their CRIS record
        int count = 0;
        try {
            WebConnection webConnection = new WebConnection(localDB);
            // Build 148 - Encode/decode Notes field
            //JSONObject jsonOutput = webConnection.post("pdo_get_myweek_website_records.php");
            JSONObject jsonOutput = webConnection.post("pdo_get_myweek_website_records_2.php");
            String result = jsonOutput.getString("result");
            if (result.equals("FAILURE")) {
                throw new CRISException("FAILURE: " + jsonOutput.getString("error_message"));
            } else {
                count = localDB.downloadWebsiteMyWeeks(jsonOutput);

                syncActivity.appendLog(String.format(Locale.UK,
                        "Downloaded Website MyWeeks (%d)", count));

            }
        } catch (JSONException ex) {
            throw new CRISException("JSON Error in DownloadRecords(): " + ex.getMessage());

        } catch (Exception ex) {
            throw new CRISException("Error in DownloadRecords(): " + ex.getMessage());
        }
        return count;
    }

    // Build 161 - Modified to recheck recent syncs. Adding records if the records do not exist
    // So that it normally completes in 1 batch, the number of recent syncs is set to syncBatch -10
    // Build 164 - Recheck facility has MAJOR bug. Itis rechecking the forst 50 syncs each time
    // rather than the 50 just before the first null syncID. It is also rechecking before the null
    // records are handled so recehck is repeated in PARTIAL cases. A better solution will be
    // to maintain a FIFO buffer of non-null syncIDs and process it once the null syncIDs have
    // completed
    // Build 181 - Pass in recheckCount (from menu option)
    private int downloadRecords(long recheckDate) {
        SimpleDateFormat sDate = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        int count = 0;
        UUID syncID = null;
        try {
            // Get the SyncIDs for syncs since last successful SyncActivity start date (CreationDate)
            JSONObject jsonSync = getDownloadSyncIDs();
            // Build 138 - Add Partial Downloads. Load max of 200 syncs at a time
            int syncCount = 0;
            int syncBatch = 200;
            // Build 164 - Define the FIFO Queue
            Queue<JSONObject> recheckQueue = new LinkedList<JSONObject>();
            // Loop through the output rows
            JSONArray names = jsonSync.names();
            // Build 181 - Recheck Added
            int syncTotal = names.length();
            int syncsRemaining = syncTotal;
            for (int i = 0; i < syncTotal; i++) {
                // Build 181 - Take account of recehck que when extimating syncsRemaining
                syncsRemaining--;
                String name = names.getString(i);
                if (!name.equals("result")) {
                    JSONObject row = jsonSync.getJSONObject(names.getString(i));
                    // Build 161 - Recheck of last 100
                    //int action = IGNORE;
                    syncID = null;
                    syncID = UUID.fromString(row.getString("SyncID"));
                    // Build 181 - Store local syncID to enable later date check
                    //if (localDB.getSync(syncID) == null) {
                    Sync localSyncID = localDB.getSync(syncID);
                    if (localSyncID == null) {
                        // Build 164 - Call the new downloadOneSyncID unless partial count exceeded
                        // Build 138 - Add Partial Downloads. Load max of 200 syncs at a time
                        syncCount++;
                        if (syncCount > syncBatch) {
                            // Calculate the approximate time remaining
                            // Build 161 - Recheck of last 100
                            //int syncsRemaining = names.length()-i;
                            Date timeNow = new Date();
                            Date timeStart = syncActivity.getCreationDate();
                            long interval = timeNow.getTime() - timeStart.getTime();
                            long secondsRemaining = syncsRemaining * interval / syncBatch / 1000;
                            throw new CRISException(String.format("PARTIAL: %d remaining (approx. %d seconds)", syncsRemaining, secondsRemaining));
                        }
                        count += downloadOneSyncID(syncID, row, ADD);
                        //action = ADD;
                        // Build 163 - Reduce the number of rechecks now that everybody has
                        // downloaded the missing records.
                        //} else if (syncsRemaining < syncBatch - 10){
                        // Build 164 - Add SyncID to queue and maintain queue at size = recheckCount
                        //} else if (syncsRemaining < recheckCount){
                        // Build 181 - Add syncIDs to recheck queue if later than the recheck date
                        //} else {
                        //      action = RECHECK;
                        //      recheckQueue.add(row);
                        //      if (recheckQueue.size() > recheckCount) {
                        //          recheckQueue.remove();
                        //}
                    } else {
                        // Build 181 - Add sync to recheck queue if later than recheck date
                        if (localSyncID.getSyncDate().getTime() >= recheckDate) {
                            recheckQueue.add(row);
                            syncsRemaining++;
                        }
                    }
                }
            }
            // Build 164 = Now process the recheck queue

            boolean first = true;
            int recheckCount = 0;
            while (recheckQueue.size() > 0) {
                JSONObject row = recheckQueue.poll();
                if (row != null) {
                    syncID = UUID.fromString(row.getString("SyncID"));
                    // Build 181 - Display date of first entry in the queue
                    if (first) {
                        first = false;
                        syncActivity.appendLog(String.format("Rechecking %d most recent upload batches", recheckQueue.size()));
                        Sync firstSync = localDB.getSync(syncID);
                        if (firstSync != null) {
                            syncActivity.appendLog("Earliest batch rechecked: " + sDate.format(firstSync.getSyncDate()));
                        }
                    }
                    // Build 181 - Number of recehecks will be significant so implement PARTIAL mechanism
                    syncsRemaining--;
                    syncCount++;
                    if (syncCount > syncBatch) {
                        // Calculate the approximate time remaining
                        // Build 161 - Recheck of last 100
                        //int syncsRemaining = names.length()-i;
                        Date timeNow = new Date();
                        Date timeStart = syncActivity.getCreationDate();
                        long interval = timeNow.getTime() - timeStart.getTime();
                        long secondsRemaining = syncsRemaining * interval / syncBatch / 1000;
                        // Build 181 - PARTIAL will need to restart from this syncID date so set global
                        // variable to use when exception is handled
                        Sync localSyncID = localDB.getSync(syncID);
                        partialRecheckDate = localSyncID.getSyncDate().getTime();
                        syncActivity.appendLog(String.format("%d Missing records added", recheckCount));
                        Date tempDate = new Date(partialRecheckDate);
                        syncActivity.appendLog("Partial Recheck Date: " + sDate.format(tempDate));
                        throw new CRISException(String.format("PARTIAL_RECHECK: %d remaining (approx. %d seconds)", syncsRemaining, secondsRemaining));
                    }

                    // Build 169 - Remove diagnostic
                    //syncActivity.appendLog(String.format(Locale.UK,
                    //        "RECHECK using SyncID: %s", syncID));
                    recheckCount += downloadOneSyncID(syncID, row, RECHECK);
                }
            }

            // Build 181 - Diagnostic if recheck happened
            if (!first) {
                if (recheckCount > 0) {
                    syncActivity.appendLog(String.format("%d Missing records added", recheckCount));
                } else {
                    syncActivity.appendLog("No missing records found");
                }
            }
            count += recheckCount;


        } catch (JSONException ex) {
            // Build 164 - Improve error message
            String id = "null";
            if (syncID != null) {
                id = syncID.toString();
            }
            throw new CRISException("" + String.format("Error parsing JSON data (syncID=%s): %s", id, ex.getMessage()));

        } catch (Exception ex) {
            // Build 163 - Allow PARTIAL exception to propagate
            if (ex.getMessage().startsWith("PARTIAL")) {
                throw ex;
            } else {
                //Build 161 - download can throw SQL exceptions
                throw new CRISException("Error in DownloadRecords(): " + ex.getMessage());
            }
        }
        return count;
    }

    private int downloadOneSyncID(UUID syncID, JSONObject row, int action) throws JSONException {
        int count = 0;
        // Build 161
        if (action == ADD) {
            syncActivity.appendLog(String.format(Locale.UK,
                    "Initiating download using SyncID: %s", syncID));
        }
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
        jsonOutput = webConnection.post("pdo_get_records.php");
        String result = jsonOutput.getString("result");
        if (result.equals("FAILURE")) {
            throw new CRISException("pdo_get_records.php: " + jsonOutput.getString("error_message"));
        } else {
            int records = 0;
            switch (row.getString("TableName")) {
                case "ListItem":
                    records = localDB.downloadListItems(jsonOutput, newSync, action, syncActivity);
                    break;
                case "User":
                    records = localDB.downloadUsers(jsonOutput, newSync, action, syncActivity);
                    break;
                case "Document":
                    records = localDB.downloadDocuments(jsonOutput, newSync, action, syncActivity);
                    break;
                case "SystemError":
                    records = localDB.downloadSystemErrors(jsonOutput, newSync, action, syncActivity);
                    break;
                case "Blobs":
                    records = localDB.downloadBlobs(jsonOutput, newSync, action, syncActivity);
                    break;
                case "ReadAudit":
                    // Build 161
                    if (action == ADD) {
                        records = localDB.downloadReadAudits(jsonOutput, newSync);
                    }
                    break;
                case "Follow":
                    // Build 161
                    if (action == ADD) {
                        records = localDB.downloadFollows(jsonOutput, newSync);
                    }
                    break;
                default:
                    throw new CRISException("Unexpected table name in downloadRecords: " + row.getString("TableName"));
            }
            // Build 161 - Recheck added so count can be zero
            // Build 161
            if (action == ADD) {
                syncActivity.appendLog(String.format(Locale.UK,
                        "Downloaded %s (%d)", row.getString("TableName"), records));

            } else {
                if (records != 0) {
                    syncActivity.appendLog(String.format(Locale.UK,
                            "Recheck %s (%d added)", row.getString("TableName"), records));
                }
            }
            count += records;
        }

        return count;
    }

    private JSONObject getDownloadSyncIDs() {
        JSONObject jsonOutput;
        //JSONObject postJSON = getPostJSON(localDB);
        WebConnection webConnection = new WebConnection(localDB);
        try {
            JSONObject values = new JSONObject();
            // This facility was designed to speed the sync process by only looking at
            // syncIDs created since the last sync on this device. However, not implemented since
            // it potentially misses syncs from devices with incorrect date/time and the total
            // download of syncIDs is not too large to be manageable.
            Long lastSync = Long.MIN_VALUE;
            values.put("SyncDate", lastSync);
            //json.put("sync", values);
            webConnection.getInputJSON().put("sync", values);
            //jsonOutput = postJSON("get_sync_records.php", json);
            jsonOutput = webConnection.post("pdo_get_sync_records.php");
            String result = jsonOutput.getString("result");
            if (result.equals("FAILURE")) {
                throw new CRISException("pdo_get_sync_records.php: " + jsonOutput.getString("error_message"));
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
        // Build 107 -29 Aug 2018
        // Separate update and get replaced due to timing issue, see localDB for more details
        //localDB.updateSyncID("Document", sync.getSyncID());
        //Cursor cursor = localDB.getRecordsBySyncID("ReadAudit", sync.getSyncID());
        Cursor cursor = localDB.getUnsyncedRecords("ReadAudit", sync.getSyncID());
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
                    // Build 107 29 Aug 2018 return from record set cannot be trusted see above
                    //values.put("SyncID", cursor.getString(3));
                    values.put("SyncID", sync.getSyncID().toString());
                    values.put("ReadDate", cursor.getLong(3));
                    //json.put("ReadAudit_" + Integer.toString(count++), values);
                    webConnection.getInputJSON().put("ReadAudit_" + count++, values);
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
                JSONObject jsonOutput = webConnection.post("pdo_insert_read_audits.php");
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
                } catch (Exception ex) {
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
        // Build 107 -29 Aug 2018
        // Separate update and get replaced due to timing issue, see localDB for more details
        //localDB.updateSyncID("Document", sync.getSyncID());
        //Cursor cursor = localDB.getRecordsBySyncID("Follow", sync.getSyncID());
        Cursor cursor = localDB.getUnsyncedRecords("Follow", sync.getSyncID());
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
                    // Build 107 29 Aug 2018 return from record set cannot be trusted see above
                    //values.put("SyncID", cursor.getString(3));
                    values.put("SyncID", sync.getSyncID().toString());
                    values.put("Cancelled", cursor.getInt(3));
                    values.put("StartDate", cursor.getLong(4));
                    //json.put("Follow_" + Integer.toString(count++), values);
                    webConnection.getInputJSON().put("Follow_" + count++, values);
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
                JSONObject jsonOutput = webConnection.post("pdo_insert_follows_2.php");
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
        // Build 107 -29 Aug 2018
        // Separate update and get replaced due to timing issue, see localDB for more details
        //localDB.updateSyncID("Document", sync.getSyncID());
        //Cursor cursor = localDB.getRecordsBySyncID("ListItem", sync.getSyncID());
        Cursor cursor = localDB.getUnsyncedRecordsWithRecordID("ListItem", sync.getSyncID());
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
                    // Build 107 29 Aug 2018 return from record set cannot be trusted see above
                    //values.put("SyncID", cursor.getString(3));
                    values.put("SyncID", sync.getSyncID().toString());
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
                    webConnection.getInputJSON().put("ListItem_" + count++, values);
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
                JSONObject jsonOutput = webConnection.post("pdo_insert_list_items.php");
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
        // Build 107 -29 Aug 2018
        // Separate update and get replaced due to timing issue, see localDB for more details
        //localDB.updateSyncID("Document", sync.getSyncID());
        //Cursor cursor = localDB.getRecordsBySyncID("User", sync.getSyncID());
        Cursor cursor = localDB.getUnsyncedRecordsWithRecordID("User", sync.getSyncID());
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
                    // Build 107 29 Aug 2018 return from record set cannot be trusted see above
                    //values.put("SyncID", cursor.getString(3));
                    values.put("SyncID", sync.getSyncID().toString());
                    values.put("CreationDate", cursor.getLong(4));
                    values.put("CreatedByID", cursor.getString(5));
                    values.put("EmailAddress", cursor.getString(6));
                    values.put("Name", cursor.getString(7));
                    // Decrypt the Web blob using the Web cipher
                    byte[] decrypted = aesEncryption.decrypt(AESEncryption.LOCAL_CIPHER, cursor.getBlob(8));
                    // Encrypt using Local cipher
                    byte[] encrypted = aesEncryption.encrypt(AESEncryption.WEB_CIPHER, decrypted);
                    // Write to the web
                    values.put("SerialisedObject", Base64.encodeToString(encrypted, Base64.DEFAULT));
                    syncActivity.appendLog("Name: " + cursor.getString(7));
                    // Add to overall JSON object
                    //json.put("User_" + Integer.toString(count++), values);
                    webConnection.getInputJSON().put("User_" + count++, values);
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
                JSONObject jsonOutput = webConnection.post("pdo_insert_users.php");
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
        // Build 107 -29 Aug 2018
        // Separate update and get replaced due to timing issue, see localDB for more details
        //localDB.updateSyncID("Document", sync.getSyncID());
        //Cursor cursor = localDB.getRecordsBySyncID("SystemError", sync.getSyncID());
        Cursor cursor = localDB.getUnsyncedRecordsWithRecordID("SystemError", sync.getSyncID());
        if (cursor.getCount() > 0) {
            try {
                //JSONObject postJSON = getPostJSON(localDB);
                WebConnection webConnection = new WebConnection(localDB);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    // Create JSON Object for this record
                    JSONObject values = new JSONObject();
                    values.put("RecordID", cursor.getString(0));
                    // Build 107 29 Aug 2018 return from record set cannot be trusted see above
                    //values.put("SyncID", cursor.getString(3));
                    values.put("SyncID", sync.getSyncID().toString());
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
                    webConnection.getInputJSON().put("SystemError_" + count++, values);
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
                JSONObject jsonOutput = webConnection.post("pdo_insert_system_errors.php");
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

    private void uploadTest() {

        Sync sync = new Sync("SystemError", syncTimeOffset);
        syncActivity.appendLog(String.format(Locale.UK, "Upload Test (Sync): %d", sync.getSyncDate().getTime()));
        try {
            WebConnection webConnection = new WebConnection(localDB);
            // Add in the Sync record
            JSONObject values = new JSONObject();
            values.put("SyncID", sync.getSyncID().toString());
            values.put("SyncDate", sync.getSyncDate().getTime());
            values.put("TableName", sync.getTableName());
            //json.put("sync", values);
            webConnection.getInputJSON().put("sync", values);
            //JSONObject jsonOutput = postJSON("insert_system_errors.php", json);
            JSONObject jsonOutput = webConnection.post("pdo_insert_test.php");
            try {
                String result = jsonOutput.getString("result");
                if (result.equals("FAILURE")) {
                    throw new CRISException("test - " + jsonOutput.getString("error_message"));
                }
            } catch (JSONException ex) {
                throw new CRISException("Error parsing JSON data: " + ex.getMessage());
            }
            localDB.save(sync);
            syncActivity.appendLog(String.format(Locale.UK, "Upload Test Completed"));
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

    private int uploadDocuments() {
        int count = 0;
        Sync sync = new Sync("Document", syncTimeOffset);
        // Update SyncID
        // Build 107 -29 Aug 2018
        // Separate update and get replaced due to timing issue, see localDB for more details
        //localDB.updateSyncID("Document", sync.getSyncID());
        //Cursor cursor = localDB.getRecordsBySyncID("Document", sync.getSyncID());
        Cursor cursor = localDB.getUnsyncedRecordsWithRecordID("Document", sync.getSyncID());
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            try {
                //JSONObject postJSON = getPostJSON(localDB);
                WebConnection webConnection = new WebConnection(localDB);
                while (!cursor.isAfterLast()) {
                    // Create JSON Object for this record
                    JSONObject values = new JSONObject();
                    values.put("RecordID", cursor.getString(0));
                    values.put("DocumentID", cursor.getString(1));
                    values.put("HistoryDate", cursor.getLong(2));
                    // Build 107 29 Aug 2018 return from record set cannot be trusted see above
                    //values.put("SyncID", cursor.getString(3));
                    values.put("SyncID", sync.getSyncID().toString());
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
                    webConnection.getInputJSON().put("Document_" + count++, values);
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
                JSONObject jsonOutput = webConnection.post("pdo_insert_documents_18.php");
                try {
                    String result = jsonOutput.getString("result");
                    if (result.equals("FAILURE")) {
                        throw new CRISException(jsonOutput.getString("error_message"));
                    }
                } catch (JSONException ex) {
                    throw new CRISException("Error parsing JSON data: " + ex.getMessage());
                }
                localDB.save(sync);
                syncActivity.appendLog(String.format(Locale.UK, "Upload Documents (%d)", count));
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
                JSONObject jsonOutput = webConnection.post("pdo_insert_blob.php");
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
                //Build 139 - Correct error message
                //if (error_message.equals("Table Sync is empty")) {
                if (error_message.equals("PHP:Table Sync is empty")) {
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






