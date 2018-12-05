package solutions.cris.db;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import solutions.cris.object.MyWeek;
import solutions.cris.object.Note;
import solutions.cris.object.NoteType;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.Role;
import solutions.cris.object.School;
import solutions.cris.object.Session;
import solutions.cris.object.Sync;
import solutions.cris.object.SyncActivity;
import solutions.cris.object.SystemError;
import solutions.cris.object.Transport;
import solutions.cris.object.TransportOrganisation;
import solutions.cris.object.User;
import solutions.cris.utils.CRISUtil;

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

    public void checkDBUpgrade(User currentUser) {
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

    private Date followStartDate(UUID userID, UUID clientID) {
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

    public int downloadBlobs(JSONObject jsonOutput, Sync sync) {
        JSONArray names;
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
                    database.replaceOrThrow("Blobs", null, values);
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

    public Cursor getUnsyncedRecords(String tableName, UUID newSyncID) {
        String[] tableColumns = new String[]{"*"};
        String whereClause = "SyncID is null";
        //String[] whereArgs = new String[]{newSyncID.toString()};
        Cursor systemErrors = database.query(tableName, tableColumns, whereClause, null, null, null, null);
        if (systemErrors.getCount() > 0) {
            String sql = "UPDATE " + tableName + " SET SyncID = \"" + newSyncID.toString() + "\" WHERE SyncID is null";
            database.execSQL(sql);
        }
        return systemErrors;
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
        String whereClause = "HistoryDate = ?";
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

    public int downloadUsers(JSONObject jsonOutput, Sync sync) {
        JSONArray names;
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
                    database.replaceOrThrow("User", null, values);
                }

            }

            // Save the sync
            save(sync);
            database.execSQL("COMMIT");
        }
        catch (JSONException ex) {
           database.execSQL("ROLLBACK");
            throw new CRISException("Error parsing JSON data: " + ex.getMessage());
        }
        catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw ex;
        }
        return names.length() - 1;
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

    private Document deSerializeDocument(byte[] encrypted, int docType) {
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

    public ArrayList<Document> getUnreadDocuments(User currentUser) {
        ArrayList<Document> documents = new ArrayList<>();

        String query = "SELECT DOC.DocumentType, DOC.SerialisedObject, DOC.ClientID, DOC.CreationDate FROM Document AS DOC " +
                "LEFT OUTER JOIN ReadAudit RA ON RA.ReadByID = ? AND " +
                "DOC.DocumentID = RA.DocumentID AND RA.ReadDate > DOC.CreationDate  " +
                "WHERE DOC.HistoryDate = " + Long.toString(Long.MIN_VALUE) + " " +
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
        Cursor cursor = database.rawQuery(query, selectionArgs);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
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
                        Client client = (Client) getDocument(UUID.fromString(clientID));
                        // Build 098 - test here (instead of Main so that count is correct in Sync)
                        // Due to an earlier bug, there can be spurious documents which are not
                        // linked to a client. These should be ignored
                        if (client != null) {
                            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_DOCUMENTS)) {
                                addDocument = true;
                            } else if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_NOTES)) {
                                // From Build 086 READ_NOTES is used for READ_DEMOGRAPHICDOCUMENTS
                                // NB: Test for Sticky Notes is later
                                if (documentType == Document.Note) {
                                    addDocument = true;
                                }
                            }
                        }
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
                        if (!currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_DOCUMENTS) &&
                                documentType == Document.Note) {
                            Note note = (Note) document;
                            if (!note.isStickyFlag()) {
                                document = null;
                            }
                        }
                        if (document != null) {
                            documents.add(document);
                        }
                    }
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
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

    public Date getLatestDocument(Client client) {
        Date latest = client.getCreationDate();
        String[] tableColumns = new String[]{"ReferenceDate", "DocumentType"};
        String whereClause;
        String[] whereArgs;
        Date now = new Date();
        whereClause = "ClientID = ? AND HistoryDate = ? AND Cancelled = 0 AND DocumentType != 'Client' AND ReferenceDate < ? ";
        whereArgs = new String[]{client.getClientID().toString(), Long.toString(Long.MIN_VALUE), Long.toString(now.getTime())};
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

    // Document
    public void save(Document document, boolean newMode, User currentUser) {
        ContentValues values = new ContentValues();
        try {
            database.execSQL("BEGIN TRANSACTION");
            if (!newMode) {
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
            if (document.getDocumentType() == Document.ClientSession) {
                if (((ClientSession) document).getSessionID() != null) {
                    values.put("SessionID", ((ClientSession) document).getSessionID().toString());
                } else {
                    values.put("SessionID", "");
                }
            } else {
                values.put("SessionID", "");
            }

            database.insertOrThrow("Document", null, values);
            // All done so commit the changes
            database.execSQL("COMMIT");
        } catch (Exception ex) {
            database.execSQL("ROLLBACK");
            throw new CRISException(String.format(Locale.UK, "%s", ex.getMessage()));
            //throw ex;
        }
    }

    public int downloadDocuments(JSONObject jsonOutput, Sync sync)  {
        JSONArray names;
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
                    database.replaceOrThrow("Document", null, values);
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
                    newListItem = (NoteType) o.readObject();
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

    public int downloadListItems(JSONObject jsonOutput, Sync sync) {
        JSONArray names;
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
                    database.replaceOrThrow("ListItem", null, values);
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

    public void save(SystemError systemError) {
        ContentValues values = new ContentValues();
        values.put("RecordID", systemError.getRecordID().toString());
        values.putNull("SyncID");
        values.put("CreationDate", systemError.getCreationDate().getTime());
        values.put("CreatedByID", systemError.getCreatedByID().toString());
        values.put("SerialisedObject", serialize(systemError, AESEncryption.NO_ENCRYPTION));
        database.insertOrThrow("SystemError", null, values);
    }


    public int downloadSystemErrors(JSONObject jsonOutput, Sync sync) {
        JSONArray names;
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
                    database.replaceOrThrow("SystemError", null, values);
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
