package solutions.cris.db;

import android.app.LauncherActivity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.exceptions.CRISBadOrgException;
import solutions.cris.exceptions.CRISException;
import solutions.cris.crypto.AESEncryption;
import solutions.cris.object.Agency;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.Contact;
import solutions.cris.object.CriteriaAssessmentTool;
import solutions.cris.object.Document;
import solutions.cris.object.Group;
import solutions.cris.object.Image;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.MACAYC18;
import solutions.cris.object.MyWeek;
import solutions.cris.object.Note;
import solutions.cris.object.NoteType;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.RawDocument;
import solutions.cris.object.Role;
import solutions.cris.object.School;
import solutions.cris.object.Session;
import solutions.cris.object.Sync;
import solutions.cris.object.SyncActivity;
import solutions.cris.object.SystemError;
import solutions.cris.object.Transport;
import solutions.cris.object.TransportOrganisation;
import solutions.cris.object.User;
import solutions.cris.sync.SyncAdapter;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.SwipeDetector;

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

public class LocalDB {

    private SQLiteDatabase database;
    private String databaseName;
    private LocalDBOpenHelper openHelper;
    private static volatile LocalDB instance;

    private LocalDB(Context context, String databaseName, String organisation) {

        this.openHelper = new LocalDBOpenHelper(context, databaseName, organisation);
        this.databaseName = databaseName;
    }

    public static synchronized LocalDB getInstance() {
        if (instance == null) {
            // Implies getInstance was not called in LoginOld
            throw new CRISException("Database has not been opened yet.");
        }
        return instance;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public static synchronized LocalDB getInstance(Context context, String organisation) throws CRISBadOrgException {
        String dbName = AESEncryption.getDatabaseName(organisation);
        if (instance == null || !instance.getDatabaseName().equals(dbName)) {
            instance = new LocalDB(context, dbName, organisation);
            instance.open();
            // Double check that the organisation in the prefs (which will be used to generate
            // the encryption key is the same as the one in the database (database name is
            // not completely unique and could be generated from a different org string
            String[] tableColumns = new String[]{"Organisation"};
            Cursor cursor = instance.database.query("Organisation", tableColumns, null, null, null, null, null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                if (!organisation.equals(cursor.getString(0))) {
                    throw new CRISBadOrgException(organisation, cursor.getString(0));
                }
            }
            cursor.close();
        }
        return instance;
    }

    // Build 178 - If an upgrade occurs, return the latest version and reset
    // openHelper.getOldVersion to prevent repeated execution
    //public void checkDBUpgrade(User currentUser) {
    public int checkDBUpgrade(User currentUser) {
        int version = this.openHelper.getOldVersion();
        while (version < this.openHelper.getNewVersion()) {
            version++;
            ArrayList<ListItem> list = this.openHelper.getUpgradeListItems(version);
            for (ListItem item : list) {
                if (!listItemExists(item.getListType(), item.getItemValue())) {
                    save(item, true, currentUser);
                }
            }
        }

        // V2.0.076 Added SessionID to Document table which needs to be populated
        updateClientSessionSessionID();

        // Build 178 - If an upgrade occurs, return the latest version
        if (this.openHelper.getOldVersion() != this.openHelper.getNewVersion()) {
            // Prevent repeat running
            this.openHelper.setOldVersion(version);
            return version;
        } else {
            return 0;
        }
    }

    public void updateClientSessionSessionID() {
        // New database field SessionID needs to be populated for all ClientSessions
        ArrayList<ClientSession> clientSessions = new ArrayList<>();
        String[] tableColumns = new String[]{"SerialisedObject", "RecordID", "SessionID"};
        // V2.0.079 MotoG has SessionID = 'null' following DB Version 19 SQL (SET SessionID = '')
        //String whereClause = "DocumentType = ? AND HistoryDate = ? AND SessionID = ''";
        String whereClause = "DocumentType = ? AND HistoryDate = ? AND length(SessionID) < 10 ";
        String[] whereArgs = new String[]{Integer.toString(Document.ClientSession), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                // Deserialize the object
                ClientSession clientSession = (ClientSession) deSerializeDocument(cursor.getBlob(0), Document.ClientSession);
                if (clientSession.getSessionID() != null) {
                    String sql = String.format("UPDATE Document SET SessionID = '%s' WHERE RecordID = '%s' ",
                            clientSession.getSessionID().toString(), cursor.getString(1));
                    database.execSQL(sql);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
    }

    public void open() {
        this.database = openHelper.getWritableDatabase();
    }

    public void close() {
        if (database != null) {
            this.database.close();
        }
    }

    public void reEncrypt() {
        AESEncryption aesEncryption = AESEncryption.getInstance();
        // Get all CRIS objects and re-encrypt using new AES encryption
        try {

            database.execSQL("BEGIN TRANSACTION");
            reEncrypt(aesEncryption, "ListItem");
            reEncrypt(aesEncryption, "User");
            reEncrypt(aesEncryption, "Document");
            reEncryptBlob(aesEncryption);
            database.execSQL("COMMIT");
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw ex;
        }
    }

    private void reEncryptBlob(AESEncryption aesEncryption) {
        //ArrayList<User> users = new ArrayList<User>();
        String[] tableColumns = new String[]{"BlobID", "Content"};
        Cursor cursor = database.query("Blobs", tableColumns, null, null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                byte[] decrypt = aesEncryption.decrypt(AESEncryption.OLD_CIPHER, cursor.getBlob(1));
                byte[] encrypt = aesEncryption.encrypt(AESEncryption.LOCAL_CIPHER, decrypt);
                ContentValues values = new ContentValues();
                values.put("Content", encrypt);
                String whereClause = "BlobID = ?";
                String[] whereArgs = new String[]{cursor.getString(0)};
                database.update("Blobs", values, whereClause, whereArgs);
                cursor.moveToNext();
            }
        }
        cursor.close();
    }

    private void reEncrypt(AESEncryption aesEncryption, String table) {
        //ArrayList<User> users = new ArrayList<User>();
        String[] tableColumns = new String[]{"RecordID", "SerialisedObject"};
        Cursor cursor = database.query(table, tableColumns, null, null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                byte[] decrypt = aesEncryption.decrypt(AESEncryption.OLD_CIPHER, cursor.getBlob(1));
                byte[] encrypt = aesEncryption.encrypt(AESEncryption.LOCAL_CIPHER, decrypt);
                ContentValues values = new ContentValues();
                values.put("SerialisedObject", encrypt);
                String whereClause = "RecordID = ?";
                String[] whereArgs = new String[]{cursor.getString(0)};
                database.update(table, values, whereClause, whereArgs);
                cursor.moveToNext();
            }
        }
        cursor.close();
    }

    private byte[] serialize(Serializable obj, String cipherMode) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(obj);
        } catch (IOException ex) {
            throw new CRISException("Error serializing CRIS object");
        }
        byte[] serialised = b.toByteArray();
        if (!cipherMode.equals(AESEncryption.NO_ENCRYPTION)) {
            AESEncryption aesEncryption = AESEncryption.getInstance();
            serialised = aesEncryption.encrypt(cipherMode, serialised);
        }
        return serialised;
    }

    // SYSTEM ADMIN

    public Cursor rawSQL(String query, String[] selectionArgs) {
        String output = "Running SQL...\n";
        LocalDB localDB = LocalDB.getInstance();
        Cursor cursor = database.rawQuery(query, selectionArgs);
        return cursor;
    }

    // READ AUDIT
    //ReadByID CHAR(36) NOT NULL
    //DocumentID CHAR(36) NOT NULL
    //SyncID CHAR(36)
    //ReadDate INTEGER NOT NULL
    //PRIMARY KEY (ReadByID, DocumentID)

    public void read(Document document, User currentUser) {
        ContentValues values = new ContentValues();
        values.put("ReadByID", currentUser.getUserID().toString());
        values.put("DocumentID", document.getDocumentID().toString());
        values.putNull("SyncID");
        values.put("ReadDate", new Date().getTime());
        database.replaceOrThrow("ReadAudit", null, values);
    }

    public boolean isRead(UUID documentID, UUID userID) {
        boolean readFlag = false;
        String[] tableColumns = new String[]{"ReadByID"};
        String whereClause = " DocumentID = ? AND ReadByID = ?";
        String[] whereArgs = new String[]{documentID.toString(), userID.toString()};
        Cursor cursor = database.query("ReadAudit", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            readFlag = true;
        }
        cursor.close();
        return readFlag;
    }

    public int downloadReadAudits(JSONObject jsonOutput, Sync sync) {
        JSONArray names;
        try {
            database.execSQL("BEGIN TRANSACTION");
            names = jsonOutput.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                if (!name.equals("result")) {
                    JSONObject row = jsonOutput.getJSONObject(names.getString(i));
                    ContentValues values = new ContentValues();
                    values.put("ReadByID", row.getString("ReadByID"));
                    values.put("DocumentID", row.getString("DocumentID"));
                    values.put("SyncID", row.getString("SyncID"));
                    values.put("ReadDate", row.getLong("ReadDate"));
                    database.replaceOrThrow("ReadAudit", null, values);
                }
            }
            // Save the sync

            save(sync);
            database.execSQL("COMMIT");
        } catch (JSONException ex) {
            database.execSQL("ROLLBACK");
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw ex;
        }
        return names.length() - 1;
    }

    // Follow
    // UserID CHAR(36)
    // ClientID CHAR(36)
    // Cancelled INTEGER
    // Build 100 - New overloading of SetFollow that enables the creation date of a document
    // to be set as the follow start date. For use where a note is being used to trigger a
    // follow and so the data needs to ensure that the note itself becomes visible
    public void setFollow(UUID userID, UUID clientID, boolean follow) {
        Date now = new Date();
        setFollow(userID, clientID, follow, now);
    }

    public void setFollow(UUID userID, UUID clientID, boolean follow, Date referenceDate) {
        ContentValues values = new ContentValues();
        values.put("UserID", userID.toString());
        values.put("ClientID", clientID.toString());
        values.putNull("SyncID");
        int cancelled = 1;
        if (follow) {
            cancelled = 0;
        }
        values.put("Cancelled", cancelled);
        // Build 100 Use referenceDate as the follow 'start date' so that it is equal
        // to the creation date of the associated note
        //Date now = new Date();
        //values.put("StartDate", now.getTime());
        values.put("StartDate", referenceDate.getTime());
        database.replaceOrThrow("Follow", null, values);
    }

    public boolean isFollowing(UUID userID, UUID clientID) {
        boolean follow = false;
        String[] tableColumns = new String[]{"Cancelled"};
        String whereClause = "UserID = ? AND ClientID = ?";
        String[] whereArgs = new String[]{userID.toString(), clientID.toString()};
        Cursor cursor = database.query("Follow", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            if (cursor.getInt(0) == 0) {
                follow = true;
            }
        }
        cursor.close();
        return follow;
    }

    public Date followStartDate(UUID userID, UUID clientID) {
        Date followDate = new Date(Long.MIN_VALUE);
        String[] tableColumns = new String[]{"StartDate"};
        String whereClause = "UserID = ? AND ClientID = ?";
        String[] whereArgs = new String[]{userID.toString(), clientID.toString()};
        Cursor cursor = database.query("Follow", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            followDate = new Date(cursor.getLong(0));
        }
        cursor.close();
        return followDate;
    }

    // Build 161 - Added the facility to recheck whether a record was added and add if
    // if it was missed. String action is Add or Recheck
    public int downloadFollows(JSONObject jsonOutput, Sync sync) {
        JSONArray names;

        try {
            database.execSQL("BEGIN TRANSACTION");
            names = jsonOutput.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);

                if (!name.equals("result")) {
                    JSONObject row = jsonOutput.getJSONObject(names.getString(i));

                    ContentValues values = new ContentValues();
                    values.put("UserID", row.getString("UserID"));
                    values.put("ClientID", row.getString("ClientID"));
                    values.put("SyncID", row.getString("SyncID"));
                    values.put("Cancelled", row.getInt("Cancelled"));
                    // To cover any data uploaded before StartDate was added (DBv4)
                    if (row.isNull("StartDate")) {
                        values.put("StartDate", Long.MIN_VALUE);
                    } else {
                        values.put("StartDate", row.getLong("StartDate"));
                    }
                    database.replaceOrThrow("Follow", null, values);
                }
            }
            // Save the sync

            save(sync);
            database.execSQL("COMMIT");
        } catch (JSONException ex) {
            database.execSQL("ROLLBACK");
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw ex;
        }
        return names.length() - 1;
    }

    // Blobs
    // Android has a limit on Cursor of 1Mbyte. Therefore large blobs (pdf/images) must
    // be stored in chunks. The chunk size has been chosen (aritrarily) as 500K
    private static final UUID BLOB_LAST_CHUNK = UUID.fromString("16f29bd0-abf1-11e6-80f5-76304dec7eb7");

    public UUID saveBlob(byte[] buffer) {
        int chunkSize = 500000;
        ArrayList<byte[]> chunks = new ArrayList<>();
        int start = 0;
        while (start < buffer.length) {
            int end = Math.min(buffer.length, start + chunkSize);
            chunks.add(Arrays.copyOfRange(buffer, start, end));
            start += chunkSize;
        }
        UUID blobID = null;
        UUID nextChunk = LocalDB.BLOB_LAST_CHUNK;
        for (int i = chunks.size() - 1; i >= 0; i--) {
            ContentValues values = new ContentValues();
            blobID = UUID.randomUUID();
            values.put("BlobID", blobID.toString());
            // Encrypt the blob
            byte[] encrypted;
            AESEncryption aesEncryption = AESEncryption.getInstance();
            encrypted = aesEncryption.encrypt(AESEncryption.LOCAL_CIPHER, chunks.get(i));
            values.put("Content", encrypted);
            values.put("NextChunk", nextChunk.toString());
            database.insertOrThrow("Blobs", null, values);
            nextChunk = blobID;
        }
        return blobID;
    }

    public byte[] getBlob(UUID blobID) {
        String[] tableColumns = new String[]{"NextChunk, Content"};
        String whereClause = "BlobID = ?";
        String[] whereArgs;
        AESEncryption aesEncryption = AESEncryption.getInstance();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            while (!blobID.equals(LocalDB.BLOB_LAST_CHUNK)) {
                whereArgs = new String[]{blobID.toString()};
                Cursor cursor = database.query("Blobs", tableColumns, whereClause, whereArgs, null, null, null);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    blobID = UUID.fromString(cursor.getString(0));
                    byte[] decryptedChunk;
                    decryptedChunk = aesEncryption.decrypt(AESEncryption.LOCAL_CIPHER, cursor.getBlob(1));
                    outputStream.write(decryptedChunk);
                }
                cursor.close();
            }
        } catch (java.io.IOException ex) {
            throw new CRISException("IO Exception whilst loading Blob from local database.");
        }
        return outputStream.toByteArray();
    }

    public boolean existsBlobByBlobID(String blobID) {
        boolean exists = false;
        String[] tableColumns = new String[]{"Content"};
        String whereClause = "BlobID = ?";
        String[] whereArgs = new String[]{blobID};
        Cursor cursor = database.query("Blobs", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            exists = true;
        }
        cursor.close();
        return exists;
    }

    public Cursor getOneUnsyncedBlob(UUID syncID) {
        // Set one Blob to new SyncID
        String sql = "UPDATE Blobs SET SyncID = \"" + syncID.toString() + "\" WHERE BlobID in (SELECT BlobID FROM Blobs WHERE SyncID is null LIMIT 1)";
        database.execSQL(sql);
        // Return cursor with this record
        String[] tableColumns = new String[]{"*"};
        String whereClause = "SyncID = ?";
        String[] whereArgs = new String[]{syncID.toString()};
        return database.query("Blobs", tableColumns, whereClause, whereArgs, null, null, null);
    }

    public int downloadBlobs(JSONObject jsonOutput, Sync sync, int action, SyncActivity syncActivity) {
        JSONArray names;
        int count = 0;
        try {
            database.execSQL("BEGIN TRANSACTION");
            names = jsonOutput.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                if (!name.equals("result")) {
                    JSONObject row = jsonOutput.getJSONObject(names.getString(i));

                    ContentValues values = new ContentValues();
                    values.put("BlobID", row.getString("BlobID"));
                    values.put("SyncID", row.getString("SyncID"));
                    AESEncryption aesEncryption = AESEncryption.getInstance();
                    // Decrypt the Web blob using the Web cipher
                    byte[] decryptedChunk = aesEncryption.decrypt(AESEncryption.WEB_CIPHER,
                            Base64.decode(row.getString("Content"), Base64.DEFAULT));
                    // Encrypt using Local cipher
                    byte[] encrypted = aesEncryption.encrypt(AESEncryption.LOCAL_CIPHER, decryptedChunk);
                    values.put("Content", encrypted);
                    values.put("NextChunk", row.getString("NextChunk"));
                    // If this is a new record, use insert
                    String blobID = row.getString("BlobID");
                    boolean recordExists = existsBlobByBlobID(blobID);

                    if (!recordExists) {
                        try {
                            database.insertOrThrow("Blobs", null, values);
                            count++;
                            if (action == SyncAdapter.RECHECK) {
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "Blob inserted {%s}", row.getString("BlobID")));
                            }
                        } catch (Exception ex) {
                            syncActivity.appendLog(String.format(Locale.UK,
                                    "Blob insert failed: %s", ex.getMessage()));
                        }
                    } else {
                        //Build 178 if record exists ALWAYS leave it alone
                        /*
                        // Record already exists so simply replace the contents
                        if (action == SyncAdapter.ADD) {
                            try {
                                database.replaceOrThrow("Blobs", null, values);
                                count++;
                            } catch (Exception ex) {
                                // Ignore, nothing should reach here
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "Blob replace failed: %s", ex.getMessage()));
                            }
                        } else {
                            // In RECHECK case, if the record already exists, leave it alone
                        }
                         */
                    }
                }
            }
            // Save the sync
            if (action == SyncAdapter.ADD) {
                save(sync);
            }
            database.execSQL("COMMIT");
        } catch (JSONException ex) {
            database.execSQL("ROLLBACK");
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw ex;
        }
        // Build 161 - In RECHECK case only some of the records will have been updated
        //return names.length() - 1;
        return count;
    }

    // Build 107 29 Aug 2018
    // It looks as if there is a problem with calling UpdateSyncID immediately followed by
    // getRecordsbySyncID. There apperas to be an optimisation such that the update returns
    // after updating the indexes and continues to update the data in the background.
    // This leads to the record set returned by getRecordsBySyncID (called using the newly
    // updated SyncID) returning records where the syncID has not yet been updated. This leads
    // to a knock-on error where the records sent to the server (SyncAdapter) have a null syncID
    // which causes a database constraint error.
    // The solution is to replace the two functions with a single function which does both actions
    // and then to not rely on the syncID in the returned record set. Note: combining the functions
    // does not fix the problem, but deprecating the two exiting functions and commenting the
    // replacement should help to avoid the problem in the future.
    //public Cursor getRecordsBySyncID(String tableName, UUID syncID) {
    //    String[] tableColumns = new String[]{"*"};
    //    String whereClause = "SyncID = ?";
    //    String[] whereArgs = new String[]{syncID.toString()};
    //    return database.query(tableName, tableColumns, whereClause, whereArgs, null, null, null);
    //}
    //
    //public void updateSyncID(String tableName, UUID syncID) {
    //     String sql = "UPDATE " + tableName + " SET SyncID = \"" + syncID.toString() + "\" WHERE SyncID is null";
    //    database.execSQL(sql);
    //}

    // Build 128 - Need to limit sets of unsynced records to prevent JSON/HTTP problems
    // Build 128 - Updating the table after the cursor is found leads to an invalid cursor so update
    // then get cursor on the new syncID
    // Update SQL cannot use LIMIT clause because some tablets/smartphones don't enable it
    // so need to use more sophisticated SQL
    /*
    public Cursor getUnsyncedRecords(String tableName, UUID newSyncID) {
        String[] tableColumns = new String[]{"*"};
        String whereClause = "SyncID is null";
        Cursor systemErrors = database.query(tableName, tableColumns, whereClause, null, null, null, null);
        if (systemErrors.getCount() > 0) {
            String sql = "UPDATE " + tableName + " SET SyncID = \"" + newSyncID.toString() + "\" WHERE SyncID is null";
            database.execSQL(sql);
        }
        return systemErrors;
    }
    */
    public Cursor getUnsyncedRecordsWithRecordID(String tableName, UUID newSyncID) {
        // Update the first 200 rows where SyncID is Null
        String sql = "UPDATE " + tableName + " SET SyncID = \"" + newSyncID.toString() + "\" WHERE RecordID IN " +
                "(SELECT RecordID FROM " + tableName + " WHERE SyncID is null LIMIT 200)";
        database.execSQL(sql);
        // Get these rows in a cursor
        String[] tableColumns = new String[]{"*"};
        String whereClause = String.format("SyncID = '%s'", newSyncID.toString());
        Cursor unsyncedRecords = database.query(tableName, tableColumns, whereClause, null, null, null, null);
        return unsyncedRecords;
    }

    public Cursor getUnsyncedRecords(String tableName, UUID newSyncID) {
        // This is only used for Follows and ReadAudits which will be relatively small batches
        String sql = "UPDATE " + tableName + " SET SyncID = \"" + newSyncID.toString() + "\" WHERE SyncID is null";
        database.execSQL(sql);
        // Get these rows in a cursor
        String[] tableColumns = new String[]{"*"};
        String whereClause = String.format("SyncID = '%s'", newSyncID.toString());
        Cursor unsyncedRecords = database.query(tableName, tableColumns, whereClause, null, null, null, null);
        return unsyncedRecords;
    }

    public void nullSyncID(String tableName, UUID syncID) {
        String sql = "UPDATE " + tableName + " SET SyncID = null WHERE SyncID = \"" + syncID.toString() + "\"";
        //String sql = "UPDATE " + tableName + " SET SyncID = null ";
        database.execSQL(sql);
    }

    public int getDatabaseVersion() {
        return database.getVersion();
    }

    // USER
    // RecordID CHAR(16) PRIMARY KEY NOT NULL
    // UserID CHAR(16) NOT NULL
    // HistoryDate INTEGER NOT NULL
    // SyncID CHAR(16) NOT NULL
    // CreationDate INTEGER NOT NULL
    // CreatedByID CHAR(16) NOT NULL
    // EmailAddress CHAR(200) NOT NULL
    // Name CHAR(200) NOT NULL
    // StartDate INTEGER NOT NULL
    // EndDate INTEGER
    // ContactNumber CHAR(200) NOT NULL
    // SerialisedObject BLOB
    public int numUsers() {
        // Test for no user records
        String query = "SELECT UserID FROM User";
        Cursor cursor = database.rawQuery(query, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    // GET USER
    public User getUser(String emailAddress) {
        String whereClause = "EmailAddress = ? AND HistoryDate = ?";
        String[] whereArgs = new String[]{emailAddress, Long.toString(Long.MIN_VALUE)};
        return getUser(whereClause, whereArgs);
    }

    public User getUser(UUID UserID) {

        String whereClause = "UserID = ? AND HistoryDate = ?";
        String[] whereArgs = new String[]{UserID.toString(), Long.toString(Long.MIN_VALUE)};
        return getUser(whereClause, whereArgs);
    }

    public ArrayList<UUID> getRecordIDs(User user) {
        UUID userID = user.getUserID();
        ArrayList<UUID> recordIDs = new ArrayList<>();
        String[] tableColumns = new String[]{"RecordID"};
        String whereClause;
        String[] whereArgs;
        whereClause = "UserID = ?";
        whereArgs = new String[]{userID.toString()};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, "CreationDate DESC");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                recordIDs.add(UUID.fromString(cursor.getString(0)));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return recordIDs;
    }

    public User getUserByRecordID(UUID recordID) {
        User newUser = null;
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        whereClause = "RecordID = ?";
        whereArgs = new String[]{recordID.toString()};
        Cursor cursor = database.query("User", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            newUser = deSerializeUser(cursor.getBlob(0));
        }
        cursor.close();
        return newUser;
    }

    public boolean existsUserByRecordID(String recordID) {
        boolean exists = false;
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        whereClause = "RecordID = ?";
        whereArgs = new String[]{recordID};
        Cursor cursor = database.query("User", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            exists = true;
        }
        cursor.close();
        return exists;
    }

    private User deSerializeUser(byte[] encrypted) {
        User newUser;
        AESEncryption aesEncryption = AESEncryption.getInstance();
        byte[] decrypt = aesEncryption.decrypt(AESEncryption.LOCAL_CIPHER, encrypted);
        ByteArrayInputStream b = new ByteArrayInputStream(decrypt);
        try {
            ObjectInputStream o = new ObjectInputStream(b);
            newUser = (User) o.readObject();
            o.close();
        } catch (ClassNotFoundException ex) {
            throw new CRISException("Error deserializing object User class not found");
        } catch (Exception ex) {
            throw new CRISException("Error deserializing User object: " + ex.getMessage());
        }
        // Set the passwordExpiryDate (users created before v16
        if (newUser.getPasswordExpiryDate() == null) {
            newUser.setPasswordExpiryDate(new Date(Long.MIN_VALUE));
        }
        // V2.0 Role not cleared from user before saving prior to V2.0.076
        newUser.clear();
        return newUser;
    }

    private User getUser(String whereClause, String[] whereArgs) {
        User newUser = null;
        String[] tableColumns = new String[]{"SerialisedObject"};
        Cursor cursor = database.query("User", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            newUser = deSerializeUser(cursor.getBlob(0));
        }
        cursor.close();
        return newUser;
    }

    public ArrayList<User> getAllUsers() {
        ArrayList<User> users = new ArrayList<>();
        String[] tableColumns = new String[]{"SerialisedObject"};
        // Ignore The Client User
        String whereClause = "HistoryDate = ? AND UserID != 'b800cf1b-4bfc-4915-8545-14e99e5e7669'";
        String[] whereArgs = new String[]{Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("User", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                users.add(deSerializeUser(cursor.getBlob(0)));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return users;
    }

    public void save(User user, boolean newMode, User currentUser) {
        ContentValues values = new ContentValues();
        try {
            database.execSQL("BEGIN TRANSACTION");
            if (!newMode) {
                // Edit so write history date into existing record and clear the SyncID
                // The history date will also be the Creation date for the new record
                Date historyDate = new Date();
                String whereClause = "RecordID = ?";
                String[] whereArgs = new String[]{user.getRecordID().toString()};
                values.put("HistoryDate", historyDate.getTime());
                values.putNull("SyncID");
                database.update("User", values, whereClause, whereArgs);
                values.clear();
                // Update CRISObject fields to create a new document
                user.setRecordID(UUID.randomUUID());
                user.setCreationDate(historyDate);
                user.setCreatedByID(currentUser.getUserID());
            }
            // Clear the run-time field
            Role role = user.getRole();
            user.setRole(null);
            // Now write the new record
            values.put("RecordID", user.getRecordID().toString());
            values.putNull("SyncID");
            values.put("UserID", user.getUserID().toString());
            values.put("HistoryDate", Long.MIN_VALUE);
            values.put("CreationDate", user.getCreationDate().getTime());
            values.put("CreatedByID", user.getCreatedByID().toString());
            values.put("EmailAddress", user.getEmailAddress());
            values.put("Name", user.getFullName());
            values.put("SerialisedObject", serialize(user, AESEncryption.LOCAL_CIPHER));
            database.insertOrThrow("User", null, values);
            database.execSQL("COMMIT");
            // Replace the role
            user.setRole(role);
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw ex;
        }
    }

    // Build 161 - Added the facility to recheck whether a record was added and add if
    // if it was missed. String action is Add or Recheck
    public int downloadUsers(JSONObject jsonOutput, Sync sync, int action, SyncActivity syncActivity) {
        JSONArray names;
        int count = 0;
        // Build 163 - Add a random small value to the history date to prevent constraint errors
        Double randomInterval = (Math.random() * 100) + 1;
        try {
            database.execSQL("BEGIN TRANSACTION");
            names = jsonOutput.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                if (!name.equals("result")) {
                    JSONObject row = jsonOutput.getJSONObject(names.getString(i));

                    ContentValues values = new ContentValues();
                    values.put("RecordID", row.getString("RecordID"));
                    values.put("SyncID", row.getString("SyncID"));
                    values.put("UserID", row.getString("UserID"));
                    values.put("HistoryDate", row.getLong("HistoryDate"));
                    values.put("CreationDate", row.getLong("CreationDate"));
                    values.put("CreatedByID", row.getString("CreatedByID"));
                    values.put("EmailAddress", row.getString("EmailAddress"));
                    values.put("Name", row.getString("Name"));
                    AESEncryption aesEncryption = AESEncryption.getInstance();
                    // Decrypt the Web blob using the Web cipher
                    byte[] decrypted = aesEncryption.decrypt(AESEncryption.WEB_CIPHER,
                            Base64.decode(row.getString("SerialisedObject"), Base64.DEFAULT));
                    // Encrypt using Local cipher
                    byte[] encrypted = aesEncryption.encrypt(AESEncryption.LOCAL_CIPHER, decrypted);
                    // Write to the local database
                    values.put("SerialisedObject", encrypted);
                    // If this is a new record, use insert
                    String recordID = row.getString("RecordID");
                    boolean recordExists = existsUserByRecordID(recordID);

                    if (!recordExists) {
                        try {
                            database.insertOrThrow("User", null, values);
                            count++;
                            if (action == SyncAdapter.RECHECK) {
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "User inserted {%s}", row.getString("RecordID")));
                            }
                        } catch (Exception ex) {
                            if (ex.getMessage().contains("UNIQUE constraint failed")) {
                                // This is the two independent edits case
                                // Get the local current User
                                UUID userUUID = UUID.fromString(row.getString("UserID"));
                                User user = getUser(userUUID);
                                Long downloadCreationDate = row.getLong("CreationDate");
                                if (user != null) {
                                    // The older of the two needs the addition of a history date
                                    if (user.getCreationDate().getTime() > downloadCreationDate) {
                                        values.remove("HistoryDate");
                                        // The local User may already be history so modify its creation date
                                        // slightly so that this nes User does not clash
                                        // Build 163 - Add a random interval to avoid constraint errors
                                        //values.put("HistoryDate", user.getCreationDate().getTime() + 10);
                                        values.put("HistoryDate", user.getCreationDate().getTime() + randomInterval.longValue());
                                        // Retry the insert
                                        try {
                                            database.insertOrThrow("User", null, values);
                                            count++;
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "User inserted (History modified) {%s}", row.getString("RecordID")));
                                        } catch (Exception ex2) {
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "Failed to insert User (History Modified: %s", ex2.getMessage()));
                                        }
                                    } else {
                                        // Update the HistoryDate in the local User
                                        String whereClause = "RecordID = ?";
                                        String[] whereArgs = new String[]{user.getRecordID().toString()};
                                        ContentValues userValues = new ContentValues();
                                        // Build 163 - Add a random interval to avoid constraint errors
                                        //userValues.put("HistoryDate", downloadCreationDate);
                                        userValues.put("HistoryDate", downloadCreationDate + randomInterval.longValue());
                                        // Do not clear the SyncID since this is a local fix. Each
                                        // copy will fix it's own records
                                        //values.putNull("SyncID");
                                        // Build 163 - Add exception handler, just in case
                                        try {
                                            database.update("User", userValues, whereClause, whereArgs);
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "Local User HistoryDate set {%s}", user.getRecordID().toString()));
                                            try {
                                                database.insertOrThrow("User", null, values);
                                                count++;
                                                syncActivity.appendLog(String.format(Locale.UK,
                                                        "User inserted {%s}", row.getString("RecordID")));
                                            } catch (Exception ex2) {
                                                syncActivity.appendLog(String.format(Locale.UK,
                                                        "Still failed to insert User: %s", ex2.getMessage()));
                                            }
                                        } catch (Exception ex2) {
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "Error adding history date to local user: %s", ex2.getMessage()));
                                        }
                                    }
                                } else {
                                    syncActivity.appendLog(String.format(Locale.UK,
                                            "User insert failed but no local User found: %s", ex.getMessage()));
                                }
                            } else {
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "User insert failed (Not Constraint error): %s", ex.getMessage()));
                            }
                        }
                    } else {
                        //Build 178 if record exists ALWAYS leave it alone
                        /*
                        // Record already exists so simply replace the contents
                        if (action == SyncAdapter.ADD) {
                            try {
                                database.replaceOrThrow("User", null, values);
                                count++;
                            } catch (Exception ex) {
                                // Ignore, nothing should reach here
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "User replace failed: %s", ex.getMessage()));
                            }
                        } else {
                            // In RECHECK case, if the record already exists, leave it alone
                        }
                         */
                    }
                }

            }

            // Save the sync
            // Build 161 - Only save the sync if Add (Recheck already has the sync record)
            if (action == SyncAdapter.ADD) {
                save(sync);
            }
            database.execSQL("COMMIT");
        } catch (JSONException ex) {
            database.execSQL("ROLLBACK");
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw ex;
        }
        // Build 161 - Recheck last 100
        //return names.length() - 1;
        return count;
    }

    // DOCUMENT
    // RecordID CHAR(16) PRIMARY KEY NOT NULL
    //DocumentID CHAR(16)
    //HistoryDate INTEGER
    //SyncID CHAR(16)
    //CreationDate INTEGER
    //CreatedByID CHAR(16)
    //Cancelled INTEGER
    //ClientID CHAR(16)
    //DocumentType INTEGER
    //SerialisedObject BLOB
    //ReferenceDate INTEGER

    public Document deSerializeDocument(byte[] encrypted, int docType) {
        Document newDocument = null;
        AESEncryption aesEncryption = AESEncryption.getInstance();
        byte[] decrypt = aesEncryption.decrypt(AESEncryption.LOCAL_CIPHER, encrypted);
        ByteArrayInputStream b = new ByteArrayInputStream(decrypt);
        try {
            ObjectInputStream o = new ObjectInputStream(b);
            switch (docType) {
                case Document.Contact:
                    Contact newContact = (Contact) o.readObject();
                    newContact.clear();
                    newDocument = newContact;
                    break;
                case Document.Case:
                    Case newCase = (Case) o.readObject();
                    newCase.clear();
                    newDocument = newCase;
                    break;
                case Document.Client:
                    Client client = (Client) o.readObject();
                    client.clear();
                    newDocument = client;
                    break;
                case Document.ClientSession:
                    ClientSession clientSession = (ClientSession) o.readObject();
                    // V2.0 Prior to build 2.0.076 clientSession wasn't cleared correctly
                    clientSession.clear();
                    newDocument = clientSession;
                    break;
                case Document.CriteriaAssessmentTool:
                    CriteriaAssessmentTool cat = (CriteriaAssessmentTool) o.readObject();
                    newDocument = cat;
                    break;
                case Document.Image:
                    Image image = (Image) o.readObject();
                    newDocument = image;
                    break;
                case Document.MACAYC18:
                    MACAYC18 macayc18 = (MACAYC18) o.readObject();
                    newDocument = macayc18;
                    break;
                case Document.MyWeek:
                    MyWeek newMyWeek = (MyWeek) o.readObject();
                    newMyWeek.clear();
                    newDocument = newMyWeek;
                    break;
                case Document.Note:
                    Note newNote = (Note) o.readObject();
                    newNote.clear();
                    newDocument = newNote;
                    break;
                case Document.PdfDocument:
                    PdfDocument pdfDocument = (PdfDocument) o.readObject();
                    pdfDocument.clear();
                    newDocument = pdfDocument;
                    break;
                case Document.Session:
                    Session session = (Session) o.readObject();
                    session.clear();
                    newDocument = session;
                    break;
                case Document.Transport:
                    Transport newTransport = (Transport) o.readObject();
                    // V2.0 Prior to build 2.0.076 clientSession wasn't cleared correctly
                    newTransport.clear();
                    newDocument = newTransport;
                    break;
                default:
                    //throw new CRISException(String.format("Unknown Document Type: %d", docType));
                    // This will occur when a new document type has been added and this is an older
                    // non-upgraded version. Best option is to ignore the document
            }
            o.close();
        } catch (ClassNotFoundException ex) {
            throw new CRISException("Error deserializing object Document class not found");
        } catch (Exception ex) {
            throw new CRISException("Error deserializing Document object: " + ex.getMessage());
        }
        return newDocument;
    }

    public Cursor getUnreadDocumentsCursor(User currentUser) {
        Cursor cursor = null;
        String query = "SELECT DOC.DocumentType, DOC.SerialisedObject, DOC.ClientID, DOC.CreationDate FROM Document AS DOC " +
                "LEFT OUTER JOIN ReadAudit RA ON RA.ReadByID = ? AND " +
                "DOC.DocumentID = RA.DocumentID AND RA.ReadDate > DOC.CreationDate  " +
                "WHERE DOC.HistoryDate = " + Long.MIN_VALUE + " " +
                // Build 101 - Only search for Notes and My Weeks
                "AND (DOC.DocumentType = 3 OR DOC.DocumentType = 5)  " +
                "AND RA.DocumentID IS NULL AND DOC.CreatedByID != ? AND DOC.Cancelled = 0 " +
                "AND DOC.DocumentType <> -1 " +
                "AND (DOC.ClientID IN (SELECT ClientID FROM Follow WHERE UserID = ? AND Cancelled = 0) " +
                "OR DOC.ClientID = ?) " +
                "GROUP BY (DOC.DocumentID) ORDER BY DOC.ClientID";
        String[] selectionArgs = new String[]{currentUser.getUserID().toString(),
                currentUser.getUserID().toString(),
                currentUser.getUserID().toString(),
                Client.nonClientDocumentID.toString()};
        try {
            cursor = database.rawQuery(query, selectionArgs);
        } catch (Exception ex) {
            // ignore
        }
        return cursor;

    }

    public ArrayList<Document> getUnreadDocuments(User currentUser) {
        ArrayList<Document> documents = new ArrayList<>();

        // Build 158 - Optimisation
        boolean readAllDocuments = false;
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_DOCUMENTS)) {
            readAllDocuments = true;
        }
        boolean readNotes = false;
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_NOTES)) {
            readNotes = true;
        }
        // Build 151 - Belt and braces
        try {
            //Cursor cursor = database.rawQuery(query, selectionArgs);
            Cursor cursor = getUnreadDocumentsCursor(currentUser);
            if (cursor.getCount() > 0) {
                // Build 151 - Spurious error where cursor exists but index crashes
                // Added try/catch to restart after crash but limit to 10 to
                // prevent an infinite loop if cursor.MoveToNext() does anything odd.
                int exceptionCount = 0;
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    // Build 151 - Trap indexing crash
                    try {
                        // Test the document's visibility. Library documents are always visible
                        // Other documents depend on the user's privileges
                        // 19 Oct 2017 Build 089 Restrict Unread docs to Notes and MyWeek
                        int documentType = cursor.getInt(0);
                        //if (documentType != Document.Session &&
                        //        documentType != Document.ClientSession) {
                        if (documentType == Document.Note ||
                                documentType == Document.MyWeek) {
                            String clientID = cursor.getString(2);
                            boolean addDocument = false;
                            if (clientID.equals(Document.nonClientDocumentID.toString())) {
                                addDocument = true;
                            } else {
                                // Build 158 Move the client test to only when sure we want to add the document
                                //Client client = (Client) getDocument(UUID.fromString(clientID));
                                // Build 098 - test here (instead of Main so that count is correct in Sync)
                                // Due to an earlier bug, there can be spurious documents which are not
                                // linked to a client. These should be ignored
                                //if (client != null) {
                                if (readAllDocuments) {
                                    addDocument = true;
                                } else if (readNotes) {
                                    // From Build 086 READ_NOTES is used for READ_DEMOGRAPHICDOCUMENTS
                                    // NB: Test for Sticky Notes is later
                                    if (documentType == Document.Note) {
                                        addDocument = true;
                                    }
                                }
                                //}
                                //else{
                                //    // From Build 086 READ_NOTES is used for READ_DEMOGRAPHICDOCUMENTS
                                //    switch (documentType) {
                                //        case Document.Client:
                                //        case Document.Case:
                                //        case Document.Contact:
                                //        case Document.Note:
                                //            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_NOTES)){
                                //                addDocument = true;
                                //            }
                                //           break;
                                //        default:
                                //            addDocument = false;
                                //    }
                                //}
                                if (addDocument) {
                                    // Final check that the document's creation date is after the follow date
                                    Date docDate = new Date(cursor.getLong(3));
                                    Date followDate = followStartDate(currentUser.getUserID(), UUID.fromString(clientID));
                                    // Build 101 - Display docs with same date as follow date
                                    //if (!followDate.before(docDate)) {
                                    if (docDate.before(followDate)) {
                                        addDocument = false;
                                    }
                                }
                            }
                            if (addDocument) {
                                Document document = deSerializeDocument(cursor.getBlob(1), documentType);
                                // Build 086 - Special case for Notes. If READ_NOTES (demographic documents)
                                // only sticky notes are allowed
                                if (documentType == Document.Note) {
                                    Note note = (Note) document;
                                    if (!readAllDocuments) {
                                        if (!note.isStickyFlag()) {
                                            document = null;
                                        }
                                    }
                                    // Build 127 - Remove Text Message, Phone Message and Email Notes
                                    if (note.getNoteType().getItemValue().toLowerCase().equals("text message")) {
                                        document = null;
                                    } else if (note.getNoteType().getItemValue().toLowerCase().equals("phone message")) {
                                        document = null;
                                    } else if (note.getNoteType().getItemValue().toLowerCase().equals("email")) {
                                        document = null;
                                    }
                                }
                                if (document != null) {
                                    // Build 158 - Finally check that client is non-null (See Build 98 fix earlier)
                                    Client client = (Client) getDocument(UUID.fromString(clientID));
                                    if (client != null) {
                                        documents.add(document);
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // Build 151 - Allow 10 crashes then exit from loop
                        if (exceptionCount < 10) {
                            exceptionCount++;
                        } else {
                            break;
                        }
                    }
                    cursor.moveToNext();
                }
            }
            cursor.close();
        } catch (Exception ex) {
            // ignore
        }
        return documents;
    }

    public ArrayList<Client> getAllClients() {
        ArrayList<Client> clients = new ArrayList<>();
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause = "DocumentType = ? AND HistoryDate = ?";
        String[] whereArgs = new String[]{Integer.toString(Document.Client), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Client client = (Client) deSerializeDocument(cursor.getBlob(0), Document.Client);
                if (client != null) {
                    clients.add(client);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return clients;
    }

    public ArrayList<Session> getAllSessions() {
        ArrayList<Session> sessions = new ArrayList<>();
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause = "DocumentType = ? AND HistoryDate = ?";
        String[] whereArgs = new String[]{Integer.toString(Document.Session), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Session session = (Session) deSerializeDocument(cursor.getBlob(0), Document.Session);
                if (session != null) {
                    sessions.add(session);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return sessions;
    }

    public ArrayList<Session> getFutureSessions() {
        Date today = CRISUtil.midnightLater(new Date());
        ArrayList<Session> sessions = new ArrayList<>();
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause = "DocumentType = ? AND HistoryDate = ?";
        String[] whereArgs = new String[]{Integer.toString(Document.Session), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Session session = (Session) deSerializeDocument(cursor.getBlob(0), Document.Session);
                if (session != null) {
                    if (session.getReferenceDate().after(today)) {
                        sessions.add(session);
                    }
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return sessions;
    }

    public ArrayList<ClientSession> getAllClientSessions(Session session) {
        ArrayList<Session> sessions = new ArrayList<>();
        sessions.add(session);
        return getAllClientSessions(sessions);
    }

    public ArrayList<ClientSession> getAllClientSessions(ArrayList<Session> sessions) {
        ArrayList<ClientSession> clientSessions = new ArrayList<>();
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause = "DocumentType = ? AND HistoryDate = ? AND SessionID IN (";
        for (Session session : sessions) {
            whereClause += String.format("'%s',", session.getDocumentID());
        }
        whereClause = whereClause.substring(0, whereClause.length() - 1) + ")";
        String[] whereArgs = new String[]{Integer.toString(Document.ClientSession), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ClientSession clientSession = (ClientSession) deSerializeDocument(cursor.getBlob(0), Document.ClientSession);
                clientSessions.add(clientSession);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return clientSessions;
    }

    public ArrayList<ClientSession> getAllClientSessions(ArrayList<Session> sessions, Date startDate, Date endDate) {
        ArrayList<ClientSession> clientSessions = new ArrayList<>();
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause = "DocumentType = ? AND HistoryDate = ? AND SessionID IN (";
        for (Session session : sessions) {
            whereClause += String.format("'%s',", session.getDocumentID());
        }
        whereClause = whereClause.substring(0, whereClause.length() - 1) + ")";
        String[] whereArgs = new String[]{Integer.toString(Document.ClientSession), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ClientSession clientSession = (ClientSession) deSerializeDocument(cursor.getBlob(0), Document.ClientSession);
                if (clientSession.getReferenceDate().after(startDate) && clientSession.getReferenceDate().before(endDate)) {
                    clientSessions.add(clientSession);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return clientSessions;
    }

    public ArrayList<Note> getAllResponses(Note mainNote) {
        ArrayList<Note> responses = new ArrayList<>();
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        whereClause = "ReferenceDate = ? AND HistoryDate = ?";
        whereArgs = new String[]{Long.toString(mainNote.getReferenceDate().getTime()), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Note note = (Note) deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
                if (note != null && !note.getDocumentID().equals(mainNote.getDocumentID())) {
                    responses.add(note);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return responses;
    }

    public ArrayList<Document> getAllDocuments(UUID clientID) {
        ArrayList<Document> documents = new ArrayList<>();
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        whereClause = "ClientID = ? AND HistoryDate = ?";
        whereArgs = new String[]{clientID.toString(), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Document document = deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
                // New document types (post this version) will return as null documents, ignore.
                if (document != null) {
                    documents.add(document);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return documents;
    }

    // Build 139 - Added KPI Views (needs Order By ClientID, ReferenceDate
    public ArrayList<Document> getAllDocumentsOfType(int documentType) {
        ArrayList<Document> documents = new ArrayList<>();
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        String orderBy = "ClientID,ReferenceDate";
        whereClause = "DocumentType = ? AND HistoryDate = ?";
        whereArgs = new String[]{Integer.toString(documentType), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, orderBy);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Document document = deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
                // New document types (post this version) will return as null documents, ignore.
                if (document != null) {
                    documents.add(document);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return documents;
    }

    // Build 181
    public boolean isSessionIDSet(UUID documentID) {
        boolean isSet = false;
        Document newDocument = null;
        String[] tableColumns = new String[]{"SessionID"};
        String whereClause;
        String[] whereArgs;
        whereClause = "DocumentID = ? AND HistoryDate = ?";
        whereArgs = new String[]{documentID.toString(), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String sessionID = cursor.getString(0);
            if (sessionID.length() > 0) {
                isSet = true;
            }
        }
        cursor.close();
        return isSet;
    }

    public ArrayList<RawDocument> getAllRawDocumentsOfType(int documentType) {
        return getAllRawDocumentsOfType(documentType, new Date(Long.MIN_VALUE), new Date(Long.MAX_VALUE));
    }

    public ArrayList<RawDocument> getAllRawDocumentsOfType(int documentType, Date minDate, Date maxDate) {
        ArrayList<RawDocument> documents = new ArrayList<>();
        String[] tableColumns = new String[]{"RecordID, CreationDate, CreatedByID, DocumentID, Cancelled, ClientID, ReferenceDate"};
        String whereClause;
        String[] whereArgs;
        String orderBy = "ClientID, ReferenceDate";
        whereClause = "DocumentType = ? AND HistoryDate = ? AND ReferenceDate > ? AND ReferenceDate < ?";
        whereArgs = new String[]{Integer.toString(documentType),
                Long.toString(Long.MIN_VALUE), Long.toString(minDate.getTime()), Long.toString(maxDate.getTime())};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, orderBy);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                UUID recordID = UUID.fromString(cursor.getString(0));
                Date creationDate = new Date();
                creationDate.setTime(cursor.getLong(1));
                UUID createdByID = UUID.fromString(cursor.getString(2));
                UUID documentID = UUID.fromString(cursor.getString(3));
                int cancelledInt = cursor.getInt(4);
                boolean cancelledFlag = false;
                if (cancelledInt == 1) {
                    cancelledFlag = true;
                }
                UUID clientID = UUID.fromString(cursor.getString(5));
                Date referenceDate = new Date();
                referenceDate.setTime(cursor.getLong(6));
                RawDocument document = new RawDocument(recordID, creationDate, createdByID, documentID, cancelledFlag, clientID, referenceDate, documentType);
                documents.add(document);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return documents;
    }

    //Build 139 - KPI Views - Get Documents in a date range - one per client
    public ArrayList<Document> getRangeOfDocumentsOfType(UUID clientID, int documentType, Date startDate, Date endDate) {
        ArrayList<Document> documents = new ArrayList<>();
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        String orderBy = "ClientID,ReferenceDate";
        whereClause = "ClientID = ? AND HistoryDate = ? AND DocumentType = ? AND " +
                "ReferenceDate > ? AND ReferenceDate < ? ";
        whereArgs = new String[]{clientID.toString(),
                Long.toString(Long.MIN_VALUE),
                Integer.toString(documentType),
                String.format("%d", startDate.getTime()),
                String.format("%d", endDate.getTime()),
        };
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Document document = deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
                // New document types (post this version) will return as null documents, ignore.
                if (document != null) {
                    documents.add(document);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return documents;
    }

    public ArrayList<Document> getRangeOfDocumentsOfType(int documentType, Date startDate, Date endDate) {
        ArrayList<Document> documents = new ArrayList<>();
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        String orderBy = "ClientID,ReferenceDate";
        whereClause = "DocumentType = ? AND HistoryDate = ? AND " +
                "ReferenceDate > ? AND ReferenceDate < ? ";
        whereArgs = new String[]{Integer.toString(documentType),
                Long.toString(Long.MIN_VALUE),
                String.format("%d", startDate.getTime()),
                String.format("%d", endDate.getTime()),
        };
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Document document = deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
                // New document types (post this version) will return as null documents, ignore.
                if (document != null) {
                    documents.add(document);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return documents;
    }

    public ArrayList<Document> getAllDocumentsOfType(UUID clientID, int documentType) {
        ArrayList<Document> documents = new ArrayList<>();
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        whereClause = "ClientID = ? AND HistoryDate = ? AND DocumentType = ? ";
        whereArgs = new String[]{clientID.toString(), Long.toString(Long.MIN_VALUE), Integer.toString(documentType)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Document document = deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
                // New document types (post this version) will return as null documents, ignore.
                if (document != null) {
                    documents.add(document);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return documents;
    }

    // Build 181
    public ArrayList<Document> getAllDocumentsOfType(UUID clientID, int documentType, UUID sessionID) {
        ArrayList<Document> documents = new ArrayList<>();
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        whereClause = "ClientID = ? AND HistoryDate = ? AND DocumentType = ? AND SessionID = ?";
        whereArgs = new String[]{clientID.toString(), Long.toString(Long.MIN_VALUE), Integer.toString(documentType), sessionID.toString()};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Document document = deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
                // New document types (post this version) will return as null documents, ignore.
                if (document != null) {
                    documents.add(document);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return documents;
    }

    // Build 128 Duplicate Note Fix
    public ArrayList<Note> getAllBroadcastNotes() {
        ArrayList<Note> notes = new ArrayList<>();
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause = "HistoryDate = ? AND DocumentType = ? ";
        String[] whereArgs;
        whereArgs = new String[]{Long.toString(Long.MIN_VALUE), Integer.toString(Document.Note)};
        String orderBy = "ClientID, CreationDate";
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, orderBy);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Note note = (Note) deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
                if (note != null) {
                    notes.add(note);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return notes;
    }

    public ArrayList<Document> getAllDocumentsOfType(ArrayList<ClientSession> clientSessions, int documentType) {
        ArrayList<Document> documents = new ArrayList<>();
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        whereClause = "HistoryDate = ? AND DocumentType = ? AND ClientID IN (";
        for (ClientSession clientSession : clientSessions) {
            whereClause += String.format("'%s',", clientSession.getClientID());
        }
        whereClause = whereClause.substring(0, whereClause.length() - 1) + ")";
        whereArgs = new String[]{Long.toString(Long.MIN_VALUE), Integer.toString(documentType)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, "ClientID");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Document document = deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
                // New document types (post this version) will return as null documents, ignore.
                if (document != null) {
                    documents.add(document);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return documents;
    }

    public ArrayList<Document> getAllDocumentsOfType(ArrayList<Client> clients, int documentType, Date startDate, Date endDate) {
        ArrayList<Document> documents = new ArrayList<>();
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        whereClause = "HistoryDate = ? AND DocumentType = ? AND ClientID IN (";
        for (Client client : clients) {
            whereClause += String.format("'%s',", client.getClientID());
        }
        whereClause = whereClause.substring(0, whereClause.length() - 1) + ")";
        whereArgs = new String[]{Long.toString(Long.MIN_VALUE), Integer.toString(documentType)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, "ClientID");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Document document = deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
                // New document types (post this version) will return as null documents, ignore.
                if (document != null) {
                    if (document.getReferenceDate().after(startDate) && document.getReferenceDate().before(endDate)) {
                        documents.add(document);
                    }
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return documents;
    }


    public Document getDocument(UUID documentID) {
        Document newDocument = null;
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        whereClause = "DocumentID = ? AND HistoryDate = ?";
        whereArgs = new String[]{documentID.toString(), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            newDocument = deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
        }
        cursor.close();
        return newDocument;
    }

    public Document getDocumentByRecordID(UUID recordID) {
        Document newDocument = null;
        String[] tableColumns = new String[]{"DocumentType", "SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        whereClause = "RecordID = ?";
        whereArgs = new String[]{recordID.toString()};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            newDocument = deSerializeDocument(cursor.getBlob(1), cursor.getInt(0));
        }
        cursor.close();
        return newDocument;
    }

    public boolean existsDocumentByRecordID(String recordID) {
        boolean exists = false;
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause;
        String[] whereArgs;
        whereClause = "RecordID = ?";
        whereArgs = new String[]{recordID};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            exists = true;
        }
        cursor.close();
        return exists;
    }

    public String getDocumentMetaData(UUID recordID, boolean isEarliest, SwipeDetector.Action action) {
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        String content = "";
        String[] tableColumns = new String[]{"CreatedByID", "CreationDate", "SyncID", "DocumentID", "HistoryDate"};
        String whereClause;
        String[] whereArgs;
        whereClause = "RecordID = ? ";
        whereArgs = new String[]{recordID.toString()};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String userName = "Unknown User";
            UUID userID = UUID.fromString(cursor.getString(0));
            User user = getUser(userID);
            if (user != null) {
                userName = user.getFullName();
            }
            if (action.equals(SwipeDetector.Action.RL)) {
                content += String.format("Document ID: %s\n", cursor.getString(3));
                content += String.format("Record ID: %s\n", recordID.toString());
                String syncID = cursor.getString(2);
                if (syncID == null) {
                    content += "Not yet synced:\n";
                } else if (syncID.length() < 36) {
                    content += String.format("\nSyncID is not a UUID and Sync record not found (%s):\n", syncID);
                } else {
                    Sync sync = getSync(UUID.fromString(syncID));
                    if (sync == null) {
                        content += String.format("\nSync record not found (%s):\n", syncID);
                    } else {
                        content += String.format("Synced on %s:\n", sDateTime.format(sync.getSyncDate()));
                    }
                }
                Long historyDate = cursor.getLong(4);
                if (historyDate != Long.MIN_VALUE) {
                    content += String.format("Superceded On: %s\n", sDateTime.format(historyDate));
                }
            }
            if (isEarliest) {
                content += String.format("Document created By: %s on %s.\n",
                        userName,
                        sDateTime.format(cursor.getLong(1)));
            } else {

                content += String.format("The following changes were made by %s on %s:\n",
                        userName,
                        sDateTime.format(cursor.getLong(1)));
            }
        } else {
            content += String.format("Document not found, RecordID = %s\n", recordID.toString());
        }
        cursor.close();
        return content;
    }

    public ArrayList<UUID> getRecordIDs(Document document) {
        UUID documentID = document.getDocumentID();
        ArrayList<UUID> recordIDs = new ArrayList<>();
        String[] tableColumns = new String[]{"RecordID"};
        String whereClause;
        String[] whereArgs;
        whereClause = "DocumentID = ?";
        whereArgs = new String[]{documentID.toString()};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, "CreationDate DESC");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                recordIDs.add(UUID.fromString(cursor.getString(0)));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return recordIDs;
    }

    public Date getLatestDocument(Client client) {
        Date latest = client.getCreationDate();
        String[] tableColumns = new String[]{"ReferenceDate", "DocumentType"};
        String whereClause;
        String[] whereArgs;
        Date now = new Date();
        String nowString = Long.toString(now.getTime());
        whereClause = "ClientID = ? AND HistoryDate = ? AND Cancelled = 0 AND DocumentType != 'Client' AND ReferenceDate < ? ";
        whereArgs = new String[]{client.getClientID().toString(), Long.toString(Long.MIN_VALUE), nowString};
        String orderBy = "ReferenceDate DESC LIMIT 1";
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, orderBy);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();

            long time = cursor.getLong(0);
            if (time != Long.MIN_VALUE && time != 0) {
                latest = new Date(time);
            }
        }
        cursor.close();
        return latest;

    }

    public HashMap<UUID, Date> getLatestDocumentDates() {
        HashMap<UUID, Date> latestDates = new HashMap<UUID, Date>();
        Date now = new Date();
        String[] selectionArgs = new String[]{Long.toString(now.getTime())};
        String query = "SELECT ClientID, MAX(ReferenceDate) FROM Document WHERE ReferenceDate < ? GROUP BY ClientID";
        Cursor cursor = database.rawQuery(query, selectionArgs);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                long time = cursor.getLong(1);
                if (time != Long.MIN_VALUE && time != 0) {
                    UUID clientUUID = UUID.fromString(cursor.getString(0));
                    latestDates.put(clientUUID, new Date(time));
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return latestDates;

    }

    // Build 128 Duplicate Notes - Add a 'remove' function which just sets a document's
    // history date without creating a new one (essentially a delete function
    public void remove(Document document) {
        ContentValues values = new ContentValues();
        try {
            database.execSQL("BEGIN TRANSACTION");
            // Write history date into existing record and clear the SyncID
            // The history date will also be the Creation date for the new record
            Date historyDate = new Date();
            String whereClause = "RecordID = ?";
            String[] whereArgs = new String[]{document.getRecordID().toString()};
            values.put("HistoryDate", historyDate.getTime());
            values.putNull("SyncID");
            database.update("Document", values, whereClause, whereArgs);
            // All done so commit the changes
            database.execSQL("COMMIT");
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw new CRISException(String.format(Locale.UK, "%s", ex.getMessage()));
            //throw ex;
        }
    }

    // Document
    public void save(Document document, boolean newMode, User currentUser) {
        ContentValues values = new ContentValues();
        try {
            database.execSQL("BEGIN TRANSACTION");
            if (!newMode) {
                // Build 144 - There are some circumstances where the document passed in
                // the parameter may have been superceded by another call to the save
                // so check we have the most recent document
                // Build 145 - Wrong solution. This will stop changes being made. Need to
                // fix at source in ListClientDocumentsFragment
                //document = getDocument(document.getDocumentID());
                // Edit so write history date into existing record and clear the SyncID
                // The history date will also be the Creation date for the new record
                Date historyDate = new Date();
                String whereClause = "RecordID = ?";
                String[] whereArgs = new String[]{document.getRecordID().toString()};
                values.put("HistoryDate", historyDate.getTime());
                values.putNull("SyncID");
                database.update("Document", values, whereClause, whereArgs);
                values.clear();
                // Update CRISObject fields to create a new document
                document.setRecordID(UUID.randomUUID());
                document.setCreationDate(historyDate);
                document.setCreatedByID(currentUser.getUserID());
            }
            // Now write the new record
            values.put("RecordID", document.getRecordID().toString());
            values.putNull("SyncID");
            values.put("DocumentID", document.getDocumentID().toString());
            values.put("HistoryDate", Long.MIN_VALUE);
            values.put("CreationDate", document.getCreationDate().getTime());
            values.put("CreatedByID", document.getCreatedByID().toString());
            values.put("Cancelled", document.getCancelledFlag());
            values.put("ClientID", document.getClientID().toString());
            values.put("DocumentType", document.getDocumentType());
            values.put("SerialisedObject", serialize(document, AESEncryption.LOCAL_CIPHER));
            // Added in Database version 2
            values.put("ReferenceDate", document.getReferenceDate().getTime());
            // Added in Database version 18
            // Build 181 - Modified to include Notes, etc. created via Session Register
            switch (document.getDocumentType()) {
                case Document.ClientSession:
                    if (((ClientSession) document).getSessionID() != null) {
                        values.put("SessionID", ((ClientSession) document).getSessionID().toString());
                    } else {
                        values.put("SessionID", "");
                    }
                    break;
                case Document.Note:
                    if (((Note) document).getSessionID() != null) {
                        values.put("SessionID", ((Note) document).getSessionID().toString());
                    } else {
                        values.put("SessionID", "");
                    }
                    break;
                case Document.PdfDocument:
                    if (((PdfDocument) document).getSessionID() != null) {
                        values.put("SessionID", ((PdfDocument) document).getSessionID().toString());
                    } else {
                        values.put("SessionID", "");
                    }
                    break;
                case Document.Transport:
                    if (((Transport) document).getSessionID() != null) {
                        values.put("SessionID", ((Transport) document).getSessionID().toString());
                    } else {
                        values.put("SessionID", "");
                    }
                    break;
                case Document.MyWeek:
                    if (((MyWeek) document).getSessionID() != null) {
                        values.put("SessionID", ((MyWeek) document).getSessionID().toString());
                    } else {
                        values.put("SessionID", "");
                    }
                    break;
            }

            /*
            if (document.getDocumentType() == Document.ClientSession) {
                if (((ClientSession) document).getSessionID() != null) {
                    values.put("SessionID", ((ClientSession) document).getSessionID().toString());
                } else {
                    values.put("SessionID", "");
                }
            } else {
                values.put("SessionID", "");
            }
            */
            database.insertOrThrow("Document", null, values);
            // All done so commit the changes
            database.execSQL("COMMIT");
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw new CRISException(String.format(Locale.UK, "%s", ex.getMessage()));
            //throw ex;
        }
    }

    // Build 161 - Added the facility to recheck whether a record was added and add if
    // if it was missed. String action is Add or Recheck
    // This is also the function where we must resolve edit clashes. It is possible, but hopefully
    // rare that the document to be downloaded is a new edit and the local document is also a new
    // edit. Both will have null history dates causing an exception in the InsertOrThrow
    public int downloadDocuments(JSONObject jsonOutput, Sync sync, int action, SyncActivity syncActivity) {
        JSONArray names;
        int count = 0;
        // Build 163 - Add a random small value to the history date to prevent constraint errors
        Double randomInterval = (Math.random() * 100) + 1;
        try {
            database.execSQL("BEGIN TRANSACTION");
            names = jsonOutput.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                if (!name.equals("result")) {
                    JSONObject row = jsonOutput.getJSONObject(names.getString(i));

                    // Build the values array for the Insert or Replace
                    ContentValues values = new ContentValues();
                    values.put("RecordID", row.getString("RecordID"));
                    values.put("SyncID", row.getString("SyncID"));
                    values.put("DocumentID", row.getString("DocumentID"));
                    values.put("HistoryDate", row.getLong("HistoryDate"));
                    values.put("CreationDate", row.getLong("CreationDate"));
                    values.put("CreatedByID", row.getString("CreatedByID"));
                    values.put("Cancelled", row.getInt("Cancelled"));
                    values.put("ClientID", row.getString("ClientID"));
                    values.put("DocumentType", row.getInt("DocumentType"));
                    AESEncryption aesEncryption = AESEncryption.getInstance();
                    // Decrypt the Web blob using the Web cipher
                    byte[] decrypted = aesEncryption.decrypt(AESEncryption.WEB_CIPHER,
                            Base64.decode(row.getString("SerialisedObject"), Base64.DEFAULT));
                    // Encrypt using Local cipher
                    byte[] encrypted = aesEncryption.encrypt(AESEncryption.LOCAL_CIPHER, decrypted);
                    // Write to the local database
                    values.put("SerialisedObject", encrypted);
                    long referenceDate;
                    try {
                        referenceDate = row.getLong("ReferenceDate");
                    } catch (Exception ex) {
                        referenceDate = Long.MIN_VALUE;
                    }
                    values.put("ReferenceDate", referenceDate);
                    // SessionID added in DB Version 18
                    String sessionID;
                    try {
                        sessionID = row.getString("SessionID");
                    } catch (Exception ex) {
                        sessionID = "";
                    }
                    values.put("SessionID", sessionID);

                    // If this is a new record, use insert
                    String recordID = row.getString("RecordID");
                    boolean recordExists = existsDocumentByRecordID(recordID);

                    if (!recordExists) {
                        try {
                            database.insertOrThrow("Document", null, values);
                            count++;
                            if (action == SyncAdapter.RECHECK) {
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "Document inserted {%s}", row.getString("RecordID")));
                            }
                        } catch (Exception ex) {
                            if (ex.getMessage().contains("UNIQUE constraint failed")) {
                                // This is the two independent edits case
                                // Get the local current document
                                UUID documentUUID = UUID.fromString(row.getString("DocumentID"));
                                Document document = getDocument(documentUUID);
                                Long downloadCreationDate = row.getLong("CreationDate");
                                if (document != null) {
                                    // The older of the two needs the addition of a history date
                                    if (document.getCreationDate().getTime() > downloadCreationDate) {
                                        values.remove("HistoryDate");
                                        // The local document may already be history so modify its creation date
                                        // slightly so that this nes document does not clash
                                        // Build 163 - Add a random interval to avoid constraint errors
                                        //values.put("HistoryDate", document.getCreationDate().getTime());
                                        values.put("HistoryDate", document.getCreationDate().getTime() + randomInterval.longValue());
                                        // Retry the insert
                                        try {
                                            database.insertOrThrow("Document", null, values);
                                            count++;
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "Document inserted (History modified) {%s}", row.getString("RecordID")));
                                        } catch (Exception ex2) {
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "Failed to insert document (History Modified: %s", ex2.getMessage()));
                                        }
                                    } else {
                                        // Update the HistoryDate in the local document
                                        String whereClause = "RecordID = ?";
                                        String[] whereArgs = new String[]{document.getRecordID().toString()};
                                        ContentValues documentValues = new ContentValues();
                                        // Build 163 - Add a random interval to avoid constraint errors
                                        documentValues.put("HistoryDate", downloadCreationDate + randomInterval.longValue());
                                        // Do not clear the SyncID since this is a local fix. Each
                                        // copy will fix it's own records
                                        //values.putNull("SyncID");
                                        // Build 163 - Add exception handler, just in case
                                        try {
                                            database.update("Document", documentValues, whereClause, whereArgs);
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "Local Document HistoryDate set {%s}", document.getRecordID().toString()));
                                            try {
                                                database.insertOrThrow("Document", null, values);
                                                count++;
                                                syncActivity.appendLog(String.format(Locale.UK,
                                                        "Document inserted {%s}", row.getString("RecordID")));
                                            } catch (Exception ex2) {
                                                syncActivity.appendLog(String.format(Locale.UK,
                                                        "Still failed to insert document: %s", ex2.getMessage()));
                                            }
                                        } catch (Exception ex2) {
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "Error adding history date to local document: %s", ex2.getMessage()));
                                        }
                                    }
                                } else {
                                    syncActivity.appendLog(String.format(Locale.UK,
                                            "Document insert failed but no local document found: %s", ex.getMessage()));
                                }
                            } else {
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "Document insert failed (Not Constraint error): %s", ex.getMessage()));
                            }
                        }
                    } else {
                        //Build 178 if record exists ALWAYS leave it alone
                        /*
                        // Record already exists so simply replace the contents
                        if (action == SyncAdapter.ADD) {
                            try {
                                database.replaceOrThrow("Document", null, values);
                                count++;
                            } catch (Exception ex) {
                                // Ignore, nothing should reach here
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "Document replace failed: %s", ex.getMessage()));
                            }
                        } else {
                            // In RECHECK case, if the record already exists, leave it alone
                        }
                         */
                    }
                }
            }
            // Save the sync
            // Build 161 - Only save the sync if Add (Recheck already has the sync record)
            if (action == SyncAdapter.ADD) {
                save(sync);
            }
            database.execSQL("COMMIT");

        } catch (JSONException ex) {
            database.execSQL("ROLLBACK");
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            syncActivity.appendLog(String.format(Locale.UK,
                    "Exception: %s", ex.getMessage()));
            throw ex;
        }

        // Build 161 - In RECHECK case only some of the records will have been updated
        //return names.length() - 1;
        return count;
    }

    // Build 143 - Download Website MyWeek Questionnaires

    public int downloadWebsiteMyWeeks(JSONObject jsonOutput) {
        // This is similar to a standard download, but builds a MyWeek document
        // from the record in the myweek table of teh MyWeek website
        // It uses a special user The Client rather than the current user so that
        // 'follow' mechanism is not compromised.
        User theClient = getUser(User.theClientUser);
        if (theClient == null) {
            // One time creation of The Client user.
            theClient = new User(User.theClientUser);
            theClient.setEmailAddress("the.client@giggle.com");
            theClient.setContactNumber("09090909");
            theClient.setFirstName("The");
            theClient.setLastName("Client");
            theClient.setStartDate(new Date());
            theClient.setRoleID(Role.systemAdministratorID);
            theClient.setNewPassword("Roger.That");
            theClient.setPasswordExpiryDate(new Date(Long.MAX_VALUE));
            theClient.save(true);
        }

        JSONArray names;
        try {

            names = jsonOutput.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);

                if (!name.equals("result")) {
                    JSONObject row = jsonOutput.getJSONObject(names.getString(i));
                    String clientID = row.getString("ClientID");
                    String sessionID = row.getString("SessionID");
                    // PHP Dates are seconds.milliseconds from epoch start
                    Long creationDate = row.getLong("CreationDate") * 1000;
                    Integer schoolScore = row.getInt("SchoolScore");
                    Integer friendshipScore = row.getInt("FriendshipScore");
                    Integer familyScore = row.getInt("FamilyScore");
                    // Build 148 - Need to encode/decode the Notes to sort out spurious characters
                    //byte[] bNotes = Base64.decode(row.getString("Notes"), Base64.DEFAULT);
                    //String notes = new String(bNotes,StandardCharsets.ISO_8859_1);
                    // Now create the new MyWeek document
                    // Build 152 - Bad UUID string seen once. Will follow up if it happens again
                    UUID clientUUID;
                    MyWeek myWeek = null;
                    try {
                        clientUUID = UUID.fromString(clientID);
                        myWeek = new MyWeek(theClient, UUID.fromString(clientID));
                    } catch (Exception ex) {
                        // Ignore, this will be caught in System Admin MyWeek checker
                    }
                    if (myWeek != null) {
                        Date referenceDate = new Date(creationDate);
                        myWeek.setReferenceDate(referenceDate);
                        myWeek.setSchoolScore(schoolScore);
                        myWeek.setFriendshipScore(friendshipScore);
                        myWeek.setHomeScore(familyScore);
                        myWeek.setSessionID(UUID.fromString(sessionID));
                        // Build 152 - Check for bad decode here and fail gracefully to avoid
                        // the exception which would lose all subsequent MyWeeks in this batch
                        try {
                            byte[] bNotes = Base64.decode(row.getString("Notes"), Base64.DEFAULT);
                            String notes = new String(bNotes, StandardCharsets.ISO_8859_1);
                            myWeek.setNote(notes);
                        } catch (Exception ex) {
                            myWeek.setNote("Error decoding the notes, please inform the System Administrator");
                        }
                        // Check whether MyWeek link was created from a session register
                        //Document session = getDocument(UUID.fromString(sessionID));
                        //if (session != null){
                        //    myWeek.setSessionID(UUID.fromString(sessionID));
                        //}
                        // Always set sessionID even though it tis a randomID for MyWeeks
                        // created directly from the client record (not the session register)
                        // This is necessary because it may be a different tablet/smartphone
                        // that creates the MyWeeks to the one that created the session and
                        // the session may not have been synced yet.

                        // and save it
                        myWeek.save(true);
                    }
                }

            }

        } catch (JSONException ex) {
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        } catch (Exception ex) {
            throw ex;
        }
        return names.length() - 1;
    }

    // Build 148 - Check for missing MyWeek records
    // Note: Cannot use the SessionID when checking for the existence of MyWeeke documents
    // since it is only saved (outside of the object) for ClientSession documents, so use
    // the reference date which is accurate to milli-second so will be unique
    public RawDocument getRawDocument(int documentType, String ClientID, Long CreationDate) {
        RawDocument document = null;
        String[] tableColumns = new String[]{"RecordID, CreationDate, CreatedByID, DocumentID, Cancelled, ClientID, ReferenceDate"};
        String whereClause;
        String[] whereArgs;
        whereClause = "DocumentType = ? AND HistoryDate = ? AND ClientID = ? AND ReferenceDate = ?";
        whereArgs = new String[]{Integer.toString(documentType),
                Long.toString(Long.MIN_VALUE), ClientID, Long.toString(CreationDate)};
        Cursor cursor = database.query("Document", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            UUID recordID = UUID.fromString(cursor.getString(0));
            Date creationDate = new Date();
            creationDate.setTime(cursor.getLong(1));
            UUID createdByID = UUID.fromString(cursor.getString(2));
            UUID documentID = UUID.fromString(cursor.getString(3));
            int cancelledInt = cursor.getInt(4);
            boolean cancelledFlag = false;
            if (cancelledInt == 1) {
                cancelledFlag = true;
            }
            UUID clientID = UUID.fromString(cursor.getString(5));
            Date referenceDate = new Date();
            referenceDate.setTime(cursor.getLong(6));
            document = new RawDocument(recordID, creationDate, createdByID, documentID, cancelledFlag, clientID, referenceDate, documentType);
        }
        cursor.close();
        return document;
    }

    public String checkWebsiteMyWeeks(JSONObject jsonOutput) {
        String output = "";
        int missing = 0;
        // This is similar to a standard download, but builds a MyWeek document
        // from the record in the myweek table of teh MyWeek website
        // It uses a special user The Client rather than the current user so that
        // 'follow' mechanism is not compromised.
        User theClient = getUser(User.theClientUser);
        if (theClient == null) {
            // One time creation of The Client user.
            theClient = new User(User.theClientUser);
            theClient.setEmailAddress("the.client@giggle.com");
            theClient.setContactNumber("09090909");
            theClient.setFirstName("The");
            theClient.setLastName("Client");
            theClient.setStartDate(new Date());
            theClient.setRoleID(Role.systemAdministratorID);
            theClient.setNewPassword("Roger.That");
            theClient.setPasswordExpiryDate(new Date(Long.MAX_VALUE));
            theClient.save(true);
        }

        JSONArray names;
        JSONObject row;
        try {

            names = jsonOutput.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);

                if (!name.equals("result")) {
                    row = jsonOutput.getJSONObject(names.getString(i));
                    String clientID = row.getString("ClientID");
                    String sessionID = row.getString("SessionID");
                    // PHP Dates are seconds.milliseconds from epoch start
                    Long creationDate = row.getLong("CreationDate") * 1000;
                    Integer schoolScore = row.getInt("SchoolScore");
                    Integer friendshipScore = row.getInt("FriendshipScore");
                    Integer familyScore = row.getInt("FamilyScore");
                    // Build 148 - Need to encode/decode the Notes to sort out spurious characters
                    //byte[] bNotes = Base64.decode(row.getString("Notes"), Base64.DEFAULT);
                    //String notes = new String(bNotes,StandardCharsets.ISO_8859_1);

                    // Get the client document
                    UUID clientUUID;
                    ArrayList<Document> clients = null;
                    try {
                        clientUUID = UUID.fromString(clientID);
                        clients = getAllDocumentsOfType(clientUUID, Document.Client);
                    } catch (Exception ex) {
                        output += String.format("%d. Client had bad UUID string: %s\n", i, clientID);
                    }
                    if (clients == null || clients.size() == 0) {
                        output += String.format("%d. Client not found: %s\n", i, clientID);
                    } else {
                        Client client = (Client) clients.get(0);
                        // Check whether the record already exists
                        RawDocument document = getRawDocument(Document.MyWeek, clientID, creationDate);
                        if (document == null) {
                            output += String.format("%d. Missing MyWeek for %s ", i, client.getFullName());
                            missing++;
                            // Now add the missing MyWeek document
                            MyWeek myWeek = new MyWeek(theClient, UUID.fromString(clientID));
                            Date referenceDate = new Date(creationDate);
                            myWeek.setReferenceDate(referenceDate);
                            myWeek.setSchoolScore(schoolScore);
                            myWeek.setFriendshipScore(friendshipScore);
                            myWeek.setHomeScore(familyScore);
                            myWeek.setSessionID(UUID.fromString(sessionID));
                            try {
                                // Build 152 - Catch spurious characters causing decode errors
                                byte[] bNotes = Base64.decode(row.getString("Notes"), Base64.DEFAULT);
                                String notes = new String(bNotes, StandardCharsets.ISO_8859_1);
                                myWeek.setNote(notes);
                                // and save it
                                myWeek.save(true);
                                output += " ... Added\n";
                            } catch (Exception ex) {
                                output += String.format("%d. Exception decoding note for %s\n", i, client.getFullName());
                            }
                        } else {
                            output += String.format("%d. Found MyWeek for %s\n", i, client.getFullName());
                        }
                    }
                } else {
                    output += String.format("%s. Found row with name: ", name);
                }
            }
            output += String.format("Missing/Added MyWeek documents =  %d\n", missing);
        } catch (
                JSONException ex) {
            output += "checkWebsiteMyWeeks() JSON Error: " + ex.getMessage();
        } catch (
                Exception ex) {
            output += "checkWebsiteMyWeeks() Error: " + ex.getMessage();
        }
        return output;
    }


    // LIST ITEM
    // RecordID CHAR(16) PRIMARY KEY NOT NULL
    //ListItemID CHAR(16)
    //HistoryDate INTEGER
    //SyncID CHAR(16)
    //CreationDate INTEGER
    //CreatedByID CHAR(16)
    //ListType INTEGER
    //ItemValue CHAR(200)
    //IsDisplayed INTEGER NOT NULL
    //SerialisedObject BLOB

    private ListItem deSerializeListItem(byte[] encrypted, String listType) {
        ListItem newListItem = null;
        AESEncryption aesEncryption = AESEncryption.getInstance();
        byte[] decrypt = aesEncryption.decrypt(AESEncryption.LOCAL_CIPHER, encrypted);
        ByteArrayInputStream b = new ByteArrayInputStream(decrypt);
        try {
            ObjectInputStream o = new ObjectInputStream(b);
            switch (listType) {
                case "AGENCY":
                    newListItem = (Agency) o.readObject();
                    break;
                case "GROUP":
                    // Groups created before V1.2 were simple ListItems so need to spoof the record
                    try {
                        newListItem = (ListItem) o.readObject();
                        Group newGroup = (Group) newListItem;
                        newListItem = newGroup;
                    } catch (ClassCastException ex) {
                        if (newListItem != null) {
                            User creator = getUser(newListItem.getCreatedByID());
                            // 19 Oct 2017 Build 089 Handle case where CreatedByID not found.
                            // No idea why the user cannot be found
                            if (creator == null) {
                                creator = new User(User.unknownUser);
                            }
                            Group newGroup = new Group(creator,
                                    newListItem.getItemValue(), newListItem.getItemOrder());
                            newGroup.setRecordID(newListItem.getRecordID());
                            newGroup.setListItemID(newListItem.getListItemID());
                            newGroup.setIsDisplayed(newListItem.isDisplayed());
                            newGroup.setIsDefault(newListItem.isDefault());
                            newGroup.setItemOrder(newListItem.getItemOrder());
                            newListItem = newGroup;
                        }
                    }
                    break;
                case "ROLE":
                    newListItem = (Role) o.readObject();
                    break;
                case "SCHOOL":
                    newListItem = (School) o.readObject();
                    break;
                case "NOTE_TYPE":
                    try {
                        //Build 121 - Build 119 added some rogue NoteTypes as ListItems which cannot be
                        // cast to NoteType which is a complex type. They need to be consigned to history
                        newListItem = (ListItem) o.readObject();
                        NoteType newNoteType = (NoteType) newListItem;
                        newListItem = newNoteType;
                    } catch (ClassCastException ex) {
                        if (newListItem != null) {
                            ContentValues values = new ContentValues();
                            Date historyDate = new Date();
                            String whereClause = "RecordID = ?";
                            String[] whereArgs = new String[]{newListItem.getRecordID().toString()};
                            values.put("HistoryDate", historyDate.getTime());
                            values.putNull("SyncID");
                            database.update("ListItem", values, whereClause, whereArgs);
                        }
                        newListItem = null;
                    }
                    break;
                case "TRANSPORT_ORGANISATION":
                    newListItem = (TransportOrganisation) o.readObject();
                    break;
                default:
                    newListItem = (ListItem) o.readObject();
            }
            o.close();
        } catch (ClassNotFoundException ex) {
            //throw new CRISException("Error deserializing object ListItem class not found");
            // This will occur when a new document type has been added and this is an older
            // non-upgraded version. Best option is to ignore the document
            newListItem = null;
        } catch (Exception ex) {
            throw new CRISException("Error deserializing ListItem object: " + ex.getMessage());
        }
        return newListItem;
    }

    public ArrayList<ListItem> getAllListItems(String listType, boolean includeHidden) {
        ArrayList<ListItem> listItems = new ArrayList<>();
        String[] tableColumns = new String[]{"ListType", "SerialisedObject"};
        String whereClause;
        if (includeHidden) {
            whereClause = "ListType = ? AND HistoryDate = ?";
        } else {
            whereClause = "ListType = ? AND HistoryDate = ? AND IsDisplayed = 1";
        }
        String[] whereArgs = new String[]{listType, Long.toString(Long.MIN_VALUE)};
        String orderBy = "ItemValue";
        Cursor cursor = database.query("ListItem", tableColumns, whereClause, whereArgs, null, null, orderBy);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ListItem newItem = deSerializeListItem(cursor.getBlob(1), cursor.getString(0));
                if (newItem != null) {
                    listItems.add(newItem);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return listItems;
    }

    // Build 175 Function to get ListItems for resetting of ListItem IDs
    public ArrayList<ListItem> getAllListItemsByValue() {
        ArrayList<ListItem> listItems = new ArrayList<>();
        String[] tableColumns = new String[]{"RecordID, ListType, ItemValue, SerialisedObject"};
        Cursor cursor = database.query("ListItem", tableColumns, null, null, null, null, "ListType, ItemValue, CreationDate ASC");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ListItem newItem = deSerializeListItem(cursor.getBlob(3), cursor.getString(1));
                if (newItem != null) {
                    listItems.add(newItem);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return listItems;
    }

    public ListItem getListItem(UUID listItemID) {
        ListItem listItem = null;
        String[] tableColumns = new String[]{"ListType", "SerialisedObject"};
        String whereClause = "ListItemID = ? AND HistoryDate = ?";
        String[] whereArgs = new String[]{listItemID.toString(), Long.toString(Long.MIN_VALUE)};
        String listType = "Unknown";
        Cursor cursor = database.query("ListItem", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            listType = cursor.getString(0);
            listItem = deSerializeListItem(cursor.getBlob(1), listType);
            // BUILD 164Test - Fix for listitem without a listitemID
            if (listItem.getListItemID() == null) {
                throw new CRISException("Found listitem with no listitemID");
            }
        }
        cursor.close();
        if (listItem == null) {
            // This may be because a new version has introduced a new list item but the
            // new item should not be called in an older version of the code. One exception
            // to this rule may be a list item which is promoted from simple to complex (such
            // as the Group)
            //throw new CRISException("Error deserializing object. ListType = " + listType);
        }
        return listItem;
    }


    public ListItem getListItem(String itemValue, ListType listType) {
        ListItem listItem = null;
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause = "ItemValue = ? AND HistoryDate = ? AND ListType = ?";
        String[] whereArgs = new String[]{itemValue, Long.toString(Long.MIN_VALUE), listType.toString()};
        Cursor cursor = database.query("ListItem", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            listItem = deSerializeListItem(cursor.getBlob(0), listType.toString());
        }
        cursor.close();
        if (listItem == null) {
            // This may be because a new version has introduced a new list item but the
            // new item should not be called in an older version of the code. One exception
            // to this rule may be a list item which is promoted from simple to complex (such
            // as the Group)
            //throw new CRISException("Error deserializing object. ListType = " + listType);
        }
        return listItem;
    }

    public ListItem getListItemByRecordID(UUID recordID) {
        ListItem listItem = null;
        String[] tableColumns = new String[]{"ListType", "SerialisedObject"};
        String whereClause = "RecordID = ?";
        String[] whereArgs = new String[]{recordID.toString()};
        String listType = "Unknown";
        Cursor cursor = database.query("ListItem", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            listType = cursor.getString(0);
            listItem = deSerializeListItem(cursor.getBlob(1), listType);
        }
        cursor.close();
        return listItem;
    }

    public boolean existsListItemByRecordID(String recordID) {
        boolean exists = false;
        String[] tableColumns = new String[]{"ListType", "SerialisedObject"};
        String whereClause = "RecordID = ?";
        String[] whereArgs = new String[]{recordID};
        String listType = "Unknown";
        Cursor cursor = database.query("ListItem", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            exists = true;
        }
        cursor.close();
        return exists;
    }

    public ArrayList<UUID> getRecordIDs(ListItem listItem) {
        UUID listItemID = listItem.getListItemID();
        ArrayList<UUID> recordIDs = new ArrayList<>();
        String[] tableColumns = new String[]{"RecordID"};
        String whereClause;
        String[] whereArgs;
        whereClause = "ListItemID = ?";
        whereArgs = new String[]{listItemID.toString()};
        Cursor cursor = database.query("ListItem", tableColumns, whereClause, whereArgs, null, null, "CreationDate DESC");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                recordIDs.add(UUID.fromString(cursor.getString(0)));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return recordIDs;
    }

    public ArrayList<UUID> getListItemRecordIDsByValue(ListItem listItem) {
        String listType = listItem.getListType().toString();
        String itemValue = listItem.getItemValue();

        return getListItemRecordIDsByValue(listType, itemValue);
    }

    public ArrayList<UUID> getListItemRecordIDsByValue(String listType, String itemValue) {
        ArrayList<UUID> recordIDs = new ArrayList<>();
        String[] tableColumns = new String[]{"RecordID"};
        String whereClause;
        String[] whereArgs;
        whereClause = "ListType = ? AND ItemValue = ?";
        whereArgs = new String[]{listType, itemValue};
        Cursor cursor = database.query("ListItem", tableColumns, whereClause, whereArgs, null, null, "CreationDate DESC");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                recordIDs.add(UUID.fromString(cursor.getString(0)));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return recordIDs;
    }

    // Build 177 - Tidy Up calls from ListListItems and ListComplexListItems
    public String getListItemMetaData(UUID recordID) {
        return getListItemMetaData(recordID, SwipeDetector.Action.RL);
    }

    public String getListItemMetaData(UUID recordID, SwipeDetector.Action action) {
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        String content = "";
        String[] tableColumns = new String[]{"CreatedByID", "CreationDate", "SyncID", "ListItemID", "HistoryDate"};
        String whereClause;
        String[] whereArgs;
        whereClause = "RecordID = ? ";
        whereArgs = new String[]{recordID.toString()};
        Cursor cursor = database.query("ListItem", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            Long historyDate = cursor.getLong(4);
            String userName = "Unknown User";
            User user = getUser(UUID.fromString(cursor.getString(0)));
            if (user != null) {
                userName = user.getFullName();
            }
            content = String.format("Created by: %s on %s.\n",
                    userName,
                    sDateTime.format(cursor.getLong(1)));
            if (action.equals(SwipeDetector.Action.RL)) {
                content += String.format("ListItem ID: %s\n", cursor.getString(3));
                content += String.format("Record ID: %s\n", recordID.toString());
                String syncID = cursor.getString(2);
                if (syncID == null) {
                    content += "Not yet synced:";
                } else if (syncID.length() < 36) {
                    content += String.format("\nSyncID is not a UUID and Sync record not found (%s):\n", syncID);
                } else {
                    Sync sync = getSync(UUID.fromString(syncID));
                    if (sync == null) {
                        content += String.format("\nSync record not found (%s):\n", syncID);
                    } else {
                        content += String.format("Synced on %s:\n", sDateTime.format(sync.getSyncDate()));
                    }
                }
            }
            if (historyDate != Long.MIN_VALUE) {
                content += String.format("Superceded On: %s\n", sDateTime.format(historyDate));
            } else {
                content += "Current (HistoryDate is Null)\n";
            }

        } else {
            content = String.format("List Item not found, RecordID = %s\n", recordID.toString());
        }
        cursor.close();
        return content;
    }


    private boolean listItemExists(ListType listType, String itemValue) {
        String[] tableColumns = new String[]{"ListItemID"};
        String whereClause = "ListType = ? AND ItemValue = ?";
        String[] whereArgs = new String[]{listType.toString(), itemValue};
        Cursor cursor = database.query("ListItem", tableColumns, whereClause, whereArgs, null, null, null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }


    public void save(ListItem listItem, boolean newMode, User currentUser) {
        ContentValues values = new ContentValues();
        try {
            database.execSQL("BEGIN TRANSACTION");
            if (!newMode) {
                // Edit so write history date into existing record and clear the SyncID
                // The history date will also be the Creation date for the new record
                Date historyDate = new Date();
                String whereClause = "RecordID = ?";
                String[] whereArgs = new String[]{listItem.getRecordID().toString()};
                values.put("HistoryDate", historyDate.getTime());
                values.putNull("SyncID");
                database.update("ListItem", values, whereClause, whereArgs);
                values.clear();
                // Update CRISObject fields to create a new document
                listItem.setRecordID(UUID.randomUUID());
                listItem.setCreationDate(historyDate);
                listItem.setCreatedByID(currentUser.getUserID());
            }
            // Now write the new record
            values.put("RecordID", listItem.getRecordID().toString());
            values.putNull("SyncID");
            values.put("ListItemID", listItem.getListItemID().toString());
            values.put("HistoryDate", Long.MIN_VALUE);
            values.put("CreationDate", listItem.getCreationDate().getTime());
            values.put("CreatedByID", listItem.getCreatedByID().toString());
            values.put("ListType", listItem.getListType().toString());
            values.put("ItemValue", listItem.getItemValue());
            values.put("IsDisplayed", listItem.isDisplayed());
            values.put("SerialisedObject", serialize(listItem, AESEncryption.LOCAL_CIPHER));
            database.insertOrThrow("ListItem", null, values);
            database.execSQL("COMMIT");
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw ex;
        }
    }

    // Build 177 Special Update to fix ListItemID
    public void listItemUpdate(ListItem listItem, User currentUser) {
        ContentValues values = new ContentValues();
        try {
            database.execSQL("BEGIN TRANSACTION");
            String whereClause = "RecordID = ?";
            String[] whereArgs = new String[]{listItem.getRecordID().toString()};
            values.put("ListItemID", listItem.getListItemID().toString());
            values.put("SerialisedObject", serialize(listItem, AESEncryption.LOCAL_CIPHER));
            database.update("ListItem", values, whereClause, whereArgs);
            database.execSQL("COMMIT");
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            String message = displayListItemHistory(listItem);
            throw new CRISException(message);
        }
    }

    private String displayListItemHistory(ListItem listItem) {
        //Loop through all instances of the document gathering data
        String history = "";
        ArrayList<UUID> recordIDs = getRecordIDs(listItem);
        //ArrayList<UUID> recordIDs = getListItemRecordIDsByValue(listItem);
        for (int i = 0; i < recordIDs.size(); i++) {
            history += getListItemMetaData(recordIDs.get(i));
        }
        return history;
    }


    // Build 161 - Added the facility to recheck whether a record was added and add if
    // if it was missed. String action is Add or Recheck
    public int downloadListItems(JSONObject jsonOutput, Sync sync, int action, SyncActivity syncActivity) {
        JSONArray names;
        int count = 0;
        // Build 163 - Add a random small value to the history date to prevent constraint errors
        Double randomInterval = (Math.random() * 100) + 1;
        try {
            database.execSQL("BEGIN TRANSACTION");
            names = jsonOutput.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                if (!name.equals("result")) {
                    JSONObject row = jsonOutput.getJSONObject(names.getString(i));

                    ContentValues values = new ContentValues();
                    values.put("RecordID", row.getString("RecordID"));
                    values.put("SyncID", row.getString("SyncID"));
                    values.put("ListItemID", row.getString("ListItemID"));
                    values.put("HistoryDate", row.getLong("HistoryDate"));
                    values.put("CreationDate", row.getLong("CreationDate"));
                    values.put("CreatedByID", row.getString("CreatedByID"));
                    values.put("ListType", row.getString("ListType"));
                    values.put("ItemValue", row.getString("ItemValue"));
                    values.put("IsDisplayed", row.getInt("IsDisplayed"));
                    AESEncryption aesEncryption = AESEncryption.getInstance();
                    // Decrypt the Web blob using the Web cipher
                    byte[] decrypted = aesEncryption.decrypt(AESEncryption.WEB_CIPHER,
                            Base64.decode(row.getString("SerialisedObject"), Base64.DEFAULT));
                    // Encrypt using Local cipher
                    byte[] encrypted = aesEncryption.encrypt(AESEncryption.LOCAL_CIPHER, decrypted);
                    // Write to the local database
                    values.put("SerialisedObject", encrypted);
                    // If this is a new record, use insert
                    String recordID = row.getString("RecordID");
                    boolean recordExists = existsListItemByRecordID(recordID);

                    if (!recordExists) {
                        try {
                            database.insertOrThrow("ListItem", null, values);
                            count++;
                            if (action == SyncAdapter.RECHECK) {
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "ListItem inserted {%s}", row.getString("RecordID")));
                            }
                        } catch (Exception ex) {
                            if (ex.getMessage().contains("UNIQUE constraint failed")) {
                                // This is the two independent edits case
                                // Get the local current ListItemListItem
                                UUID listItemUUID = UUID.fromString(row.getString("ListItemID"));
                                ListItem listItem = getListItem(listItemUUID);
                                Long downloadCreationDate = row.getLong("CreationDate");
                                if (listItem != null) {
                                    // The older of the two needs the addition of a history date
                                    if (listItem.getCreationDate().getTime() > downloadCreationDate) {
                                        values.remove("HistoryDate");
                                        // The local ListItemt may already be history so modify its creation date
                                        // slightly so that this next ListItem does not clash
                                        // Build 163 - Add a random interval to avoid constraint errors
                                        //values.put("HistoryDate", listItem.getCreationDate().getTime() + 10);
                                        values.put("HistoryDate", listItem.getCreationDate().getTime() + randomInterval.longValue());
                                        // Retry the insert
                                        try {
                                            database.insertOrThrow("ListItem", null, values);
                                            count++;
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "ListItem inserted (History modified) {%s}", row.getString("RecordID")));
                                        } catch (Exception ex2) {
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "Failed to insert ListItem (History Modified: %s", ex2.getMessage()));
                                        }
                                    } else {
                                        // Update the HistoryDate in the local ListItem
                                        String whereClause = "RecordID = ?";
                                        String[] whereArgs = new String[]{listItem.getRecordID().toString()};
                                        ContentValues listItemValues = new ContentValues();
                                        // Build 163 - Add a random interval to avoid constraint errors
                                        //listItemValues.put("HistoryDate", downloadCreationDate);
                                        listItemValues.put("HistoryDate", downloadCreationDate + randomInterval.longValue());
                                        // Do not clear the SyncID since this is a local fix. Each
                                        // copy will fix it's own records
                                        //values.putNull("SyncID");
                                        // Build 163 - Add exception handler, just in case
                                        try {
                                            database.update("ListItem", listItemValues, whereClause, whereArgs);
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "Local ListItem HistoryDate set {%s}", listItem.getRecordID().toString()));
                                            try {
                                                database.insertOrThrow("ListItem", null, values);
                                                count++;
                                                syncActivity.appendLog(String.format(Locale.UK,
                                                        "ListItem inserted {%s}", row.getString("RecordID")));
                                            } catch (Exception ex2) {
                                                syncActivity.appendLog(String.format(Locale.UK,
                                                        "Still failed to insert ListItem: %s", ex2.getMessage()));
                                            }
                                        } catch (Exception ex2) {
                                            syncActivity.appendLog(String.format(Locale.UK,
                                                    "Error adding history date to local listitem: %s", ex2.getMessage()));
                                        }
                                    }
                                } else {
                                    // Build 178 - Ignore ListItems with unexpected ListItemIDs
                                    String listType = row.getString("ListType");
                                    String itemValue = row.getString("ItemValue");
                                    syncActivity.appendLog(String.format("Bad ListItemID: %s %s", listType, itemValue));
                                    /*
                                    // Build 178 Diagnostics
                                    try {
                                        // Build 177 - Constraint must be ListType,ItemValue,HistoryDate so add more information
                                        String message = String.format(Locale.UK,
                                                "ListItem insert failed but no local ListItem found: %s\n", ex.getMessage());
                                        // Build 178 - Bug, ListItem will be null
                                        //ArrayList<UUID> recordIDs = getListItemRecordIDsByValue(listItem);

                                        message += String.format("ListType: %s\n",listType);
                                        message += String.format("itemValue: %s\n",itemValue);
                                        message += String.format("LID: %s\n",row.getString("ListItemID"));
                                        message += String.format("RID: %s\n",row.getString("RecordID"));
                                        Date historyDate = new Date(row.getLong("HistoryDate"));
                                        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
                                        message += String.format("HistoryDate: %s\n",sDateTime.format(historyDate));
                                        ArrayList<UUID> recordIDs = getListItemRecordIDsByValue(listType, itemValue);

                                        for (int record = 0; record < recordIDs.size(); record++) {
                                            message += getListItemMetaData(recordIDs.get(record));
                                            message += "-------------------------------------------------------------\n";
                                        }
                                        syncActivity.appendLog(message);
                                    } catch (Exception ex1){
                                        syncActivity.appendLog(String.format(Locale.UK,
                                                "ListItem insert failed but no local ListItem found: %s\n", ex.getMessage()));
                                    }
                                    */
                                }
                            } else {
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "ListItem insert failed (Not Constraint error): %s", ex.getMessage()));
                            }
                        }
                    } else {
                        // Build 178 - if the record already exists, leave it alone in ADD case well
                        // This code can modify the ListItems unexpectedly
                        /*
                        // Record already exists so simply replace the contents
                        if (action == SyncAdapter.ADD) {
                            try {
                                database.replaceOrThrow("ListItem", null, values);
                                count++;
                            } catch (Exception ex) {
                                // Ignore, nothing should reach here
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "ListItem replace failed: %s", ex.getMessage()));
                            }
                        } else {
                            // In RECHECK case, if the record already exists, leave it alone
                        }

                         */
                    }
                }
            }
            // Save the sync
            // Build 161 - Only save the sync if Add (Recheck already has the sync record)
            if (action == SyncAdapter.ADD) {
                save(sync);
            }
            database.execSQL("COMMIT");
        } catch (
                JSONException ex) {
            database.execSQL("ROLLBACK");
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        } catch (
                Exception ex) {
            database.execSQL("ROLLBACK");
            throw ex;
        }
        // Build 161 - Recheck last 100
        //return names.length() - 1;
        return count;
    }


    // SYSTEM ERROR
    // RecordID CHAR(16) PRIMARY KEY NOT NULL
    // SystemErrorID CHAR(16) NOT NULL
    // HistoryDate INTEGER NOT NULL
    // SyncID CHAR(16) NOT NULL
    // CreationDate INTEGER NOT NULL
    // CreatedByID CHAR(16) NOT NULL
    // SerialisedObject BLOB

    /*
    private SystemError deSerializeSystemError(byte[] encrypted){
        SystemError newSystemError = null;
        AESEncryption aesEncryption = AESEncryption.getInstance();
        byte[] decrypt = aesEncryption.decrypt(AESEncryption.LOCAL_CIPHER, encrypted);
        ByteArrayInputStream b = new ByteArrayInputStream(decrypt);
        try {
            ObjectInputStream o = new ObjectInputStream(b);
            newSystemError = (SystemError) o.readObject();
            o.close();
        } catch (ClassNotFoundException ex) {
            throw new CRISException("Error deserializing object SystemError class not found");
        } catch (Exception ex) {
            throw new CRISException("Error deserializing SystemError object: " + ex.getMessage());
        }
        return newSystemError;
    }
    */
    private SystemError deSerializeSystemError(byte[] serialised) {
        SystemError newSystemError = null;
        ByteArrayInputStream b = new ByteArrayInputStream(serialised);
        try {
            ObjectInputStream o = new ObjectInputStream(b);
            newSystemError = (SystemError) o.readObject();
            o.close();
        } catch (ClassNotFoundException ex) {
            // No point in failing since the error cannot be displayed
            //throw new CRISException("Error deserializing object SystemError class not found");
        } catch (Exception ex) {
            // No point in failing since the error cannot be displayed
            //throw new CRISException("Error deserializing SystemError object: " + ex.getMessage());
        }
        return newSystemError;
    }

    public ArrayList<SystemError> getAllSystemErrors(long limit) {
        ArrayList<SystemError> errors = new ArrayList<>();
        String[] tableColumns = new String[]{"SerialisedObject"};
        String orderBy = "CreationDate DESC";
        String limitClause = null;
        if (limit > 0) {
            limitClause = Long.toString(limit);
        }
        Cursor cursor = database.query("SystemError", tableColumns, null, null, null, null, orderBy, limitClause);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                SystemError systemError = deSerializeSystemError(cursor.getBlob(0));
                if (systemError != null) {
                    errors.add(systemError);
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return errors;
    }

    public SystemError getSystemError(UUID systemErrorID) {
        SystemError systemError = null;
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause = "SystemErrorID = ? AND HistoryDate = ?";
        String[] whereArgs = new String[]{systemErrorID.toString(), Long.toString(Long.MIN_VALUE)};
        Cursor cursor = database.query("SystemError", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            systemError = deSerializeSystemError(cursor.getBlob(0));
        }
        cursor.close();
        return systemError;
    }

    public SystemError getSystemErrorByRecordID(UUID recordID) {
        SystemError systemError = null;
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause = "RecordID = ?";
        String[] whereArgs = new String[]{recordID.toString()};
        Cursor cursor = database.query("SystemError", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            systemError = deSerializeSystemError(cursor.getBlob(0));
        }
        cursor.close();
        return systemError;
    }

    public boolean existsSystemErrorByRecordID(String recordID) {
        boolean exists = false;
        String[] tableColumns = new String[]{"SerialisedObject"};
        String whereClause = "RecordID = ?";
        String[] whereArgs = new String[]{recordID};
        Cursor cursor = database.query("SystemError", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            exists = true;
        }
        cursor.close();
        return exists;
    }

    public void save(SystemError systemError) {
        ContentValues values = new ContentValues();
        values.put("RecordID", systemError.getRecordID().toString());
        values.putNull("SyncID");
        values.put("CreationDate", systemError.getCreationDate().getTime());
        values.put("CreatedByID", systemError.getCreatedByID().toString());
        values.put("SerialisedObject", serialize(systemError, AESEncryption.NO_ENCRYPTION));
        database.insertOrThrow("SystemError", null, values);
    }


    // Build 161 - Added the facility to recheck whether a record was added and add if
    // if it was missed. String action is Add or Recheck
    public int downloadSystemErrors(JSONObject jsonOutput, Sync sync, int action, SyncActivity syncActivity) {
        JSONArray names;
        int count = 0;
        try {
            database.execSQL("BEGIN TRANSACTION");
            names = jsonOutput.names();
            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);

                if (!name.equals("result")) {
                    JSONObject row = jsonOutput.getJSONObject(names.getString(i));

                    ContentValues values = new ContentValues();
                    values.put("RecordID", row.getString("RecordID"));
                    values.put("SyncID", row.getString("SyncID"));
                    values.put("CreationDate", row.getLong("CreationDate"));
                    values.put("CreatedByID", row.getString("CreatedByID"));
                    /*
                    AESEncryption aesEncryption = AESEncryption.getInstance();
                    // Decrypt the Web blob using the Web cipher
                    byte[] decrypted = aesEncryption.decrypt(AESEncryption.WEB_CIPHER,
                            Base64.decode(row.getString("SerialisedObject"), Base64.DEFAULT));
                    // Encrypt using Local cipher
                    byte[] encrypted  = aesEncryption.encrypt(AESEncryption.LOCAL_CIPHER,decrypted);
                    // Write to the local database
                    values.put("SerialisedObject", encrypted);
                    */
                    values.put("SerialisedObject", Base64.decode(row.getString("SerialisedObject"), Base64.DEFAULT));
                    // If this is a new record, use insert
                    String recordID = row.getString("RecordID");
                    boolean recordExists = existsSystemErrorByRecordID(recordID);

                    if (!recordExists) {
                        try {
                            database.insertOrThrow("SystemError", null, values);
                            count++;
                            if (action == SyncAdapter.RECHECK) {
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "SystemError inserted {%s}", row.getString("RecordID")));
                            }
                        } catch (Exception ex) {
                            syncActivity.appendLog(String.format(Locale.UK,
                                    "System Error insert failed: %s", ex.getMessage()));
                        }
                    } else {
                        //Build 178 if record exists ALWAYS leave it alone
                        /*
                        // Record already exists so simply replace the contents
                        if (action == SyncAdapter.ADD) {
                            try {
                                database.replaceOrThrow("SystemError", null, values);
                                count++;
                            } catch (Exception ex) {
                                // Ignore, nothing should reach here
                                syncActivity.appendLog(String.format(Locale.UK,
                                        "SystemError replace failed: %s", ex.getMessage()));
                            }
                        } else {
                            // In RECHECK case, if the record already exists, leave it alone
                        }
                         */
                    }
                }
            }
            // Save the sync
            // Build 161 - Only save the sync if Add (Recheck already has the sync record)
            if (action == SyncAdapter.ADD) {
                save(sync);
            }

            database.execSQL("COMMIT");
        } catch (JSONException ex) {
            database.execSQL("ROLLBACK");
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw ex;
        }
        // Build 161 - Recheck last 100
        //return names.length() - 1;
        return count;
    }

    // SYNC
    //SyncID CHAR(16)
    //SyncDate INTEGER
    //TableName CHAR(20)
    public Sync getSync(UUID syncID) {
        Sync newSync = null;
        String[] tableColumns = new String[]{"*"};
        String whereClause = "SyncID = ?";
        String[] whereArgs = new String[]{syncID.toString()};
        Cursor cursor = database.query("Sync", tableColumns, whereClause, whereArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            newSync = new Sync(cursor.getString(2), 0);
            newSync.setSyncID(UUID.fromString(cursor.getString(0)));
            newSync.setSyncDate(new Date(cursor.getLong(1)));
        }
        cursor.close();
        return newSync;
    }

    public void save(Sync sync) {
        ContentValues values = new ContentValues();
        values.put("SyncID", sync.getSyncID().toString());
        values.put("SyncDate", sync.getSyncDate().getTime());
        values.put("TableName", sync.getTableName());
        database.insertOrThrow("Sync", null, values);
    }

    // SYNC ACTIVITY
    //RecordID CHAR(16)
    //CreationDate
    //CompletionDate
    //CreatedByID CHAR(16)
    //Result CHAR(10)
    //Summary CHAR(200)
    //Log TEXT
    public ArrayList<SyncActivity> getAllSyncResults(User currentUser) {
        ArrayList<SyncActivity> syncResults = new ArrayList<>();
        String[] tableColumns = new String[]{"*"};
        String orderBy = "CreationDate DESC";
        Cursor cursor = database.query("SyncActivity", tableColumns, null, null, null, null, orderBy);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                SyncActivity newResult = new SyncActivity(currentUser);
                newResult.setRecordID(UUID.fromString(cursor.getString(0)));
                newResult.setCreationDate(new Date(cursor.getLong(1)));
                newResult.setCompletionDate(new Date(cursor.getLong(2)));
                newResult.setCreatedByID(UUID.fromString(cursor.getString(3)));
                newResult.setResult(cursor.getString(4));
                newResult.setSummary(cursor.getString(5));
                newResult.setLog(cursor.getString(6));
                syncResults.add(newResult);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return syncResults;
    }

    // Build 158 - New function
    public ArrayList<SyncActivity> getRecentSyncResults(User currentUser, String count) {
        ArrayList<SyncActivity> syncResults = new ArrayList<>();
        String[] tableColumns = new String[]{"*"};
        String orderBy = "CreationDate DESC";
        Cursor cursor = database.query("SyncActivity", tableColumns, null, null, null, null, orderBy, count);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                SyncActivity newResult = new SyncActivity(currentUser);
                newResult.setRecordID(UUID.fromString(cursor.getString(0)));
                newResult.setCreationDate(new Date(cursor.getLong(1)));
                newResult.setCompletionDate(new Date(cursor.getLong(2)));
                newResult.setCreatedByID(UUID.fromString(cursor.getString(3)));
                newResult.setResult(cursor.getString(4));
                newResult.setSummary(cursor.getString(5));
                newResult.setLog(cursor.getString(6));
                syncResults.add(newResult);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return syncResults;
    }

    public SyncActivity getLatestSyncActivity(User currentUser) {
        String[] tableColumns = new String[]{"*"};
        String orderBy = "CreationDate DESC LIMIT 1";
        Cursor cursor = database.query("SyncActivity", tableColumns, null, null, null, null, orderBy);
        return getSyncActivity(currentUser, cursor);
    }

    private SyncActivity getSyncActivity(User currentUser, Cursor cursor) {
        SyncActivity syncActivity = new SyncActivity(currentUser);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            syncActivity.setRecordID(UUID.fromString(cursor.getString(0)));
            syncActivity.setCreationDate(new Date(cursor.getLong(1)));
            syncActivity.setCompletionDate(new Date(cursor.getLong(2)));
            syncActivity.setCreatedByID(UUID.fromString(cursor.getString(3)));
            syncActivity.setResult(cursor.getString(4));
            syncActivity.setSummary(cursor.getString(5));
            syncActivity.setLog(cursor.getString(6));
        }
        cursor.close();
        return syncActivity;
    }

    public void save(SyncActivity syncActivity) {
        ContentValues values = new ContentValues();
        values.put("RecordID", syncActivity.getRecordID().toString());
        values.put("CreationDate", syncActivity.getCreationDate().getTime());
        values.put("CompletionDate", syncActivity.getCompletionDate().getTime());
        values.put("CreatedByID", syncActivity.getCreatedByID().toString());
        values.put("Result", syncActivity.getResult());
        values.put("Summary", syncActivity.getSummary());
        values.put("Log", syncActivity.getLog());
        database.insertOrThrow("SyncActivity", null, values);
    }
}
