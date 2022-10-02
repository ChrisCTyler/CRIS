package solutions.cris.db;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.UUID;

import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.User;
import solutions.cris.utils.AlertAndContinue;

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

public class LocalDBOpenHelper extends SQLiteOpenHelper {

    // Update History:
    // Version 2: Added ReferenceDate column to Document
    // Version 3: Added Commissioner list items and Follow table
    // Build 140 - Version 23 added index on DocumentType, HistoryDate,ReferenceDate
    // Build 178 - Run the fitItemID code on upgrade(version = 28)
    private static final int VERSION = 29;
    private String organisation;
    private int oldVersion = 0;
    private int newVersion = 0;

    int getOldVersion() {return oldVersion;}

    public void setOldVersion(int oldVersion) {
        this.oldVersion = oldVersion;
    }

    int getNewVersion() {return newVersion;}

    // Constructor calls parent constructor
    LocalDBOpenHelper(Context context, String databaseName, String organisation) {
        super(context, databaseName, null, VERSION);
        this.organisation = organisation;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // onCreate needs to create the latest version of the database (VERSION)
        // so it calls each of the upgrade SQLs starting with version 1
        for (int version=1; version<=VERSION; version++) {
            ArrayList<String> sqlList = getUpgradeSql(version);
            for (String sql : sqlList) {
                db.execSQL(sql);
            }
        }
        // Add the Organisation record
        String orgSql = String.format("INSERT INTO Organisation (Organisation) VALUES ('%s');", organisation);
        db.execSQL(orgSql);
        this.oldVersion = 0;
        this.newVersion = 1;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
        int version = oldVersion;
        while (version < newVersion) {
            version++;
            ArrayList<String> sqlList = getUpgradeSql(version);
            for (String sql : sqlList) {
                db.execSQL(sql);
            }
            // No need to set the database version explicitly. It will be set
            // to the public variable VERSION when this method completes
        }
    }

    // This is the source of new picklist items
    ArrayList<ListItem> getUpgradeListItems(int version){
        User user = new User(User.unknownUser);
        ArrayList<ListItem> list = new ArrayList<>();
        switch (version){
            case 1:
                list.add(new ListItem(user, ListType.GENDER, "Female", 0));
                list.add(new ListItem(user, ListType.GENDER, "Male", 1));
                list.add(new ListItem(user,ListType.ETHNICITY, "White (British)",0 ));
                list.add(new ListItem(user,ListType.ETHNICITY, "White (Irish)", 1));
                list.add(new ListItem(user,ListType.ETHNICITY, "White (Polish)", 2));
                list.add(new ListItem(user,ListType.ETHNICITY, "White (Traveller/Roma)", 3));
                list.add(new ListItem(user,ListType.ETHNICITY, "White (Other)", 4));
                list.add(new ListItem(user,ListType.ETHNICITY, "Mixed/Multiple Ethnic Groups", 5));
                list.add(new ListItem(user,ListType.ETHNICITY, "Asian (British)", 6));
                list.add(new ListItem(user,ListType.ETHNICITY, "Asian (Indian)", 7));
                list.add(new ListItem(user,ListType.ETHNICITY, "Asian (Pakistani)", 8));
                list.add(new ListItem(user,ListType.ETHNICITY, "Asian (Bangladeshi)", 9));
                list.add(new ListItem(user,ListType.ETHNICITY, "Asian (Chinese)", 10));
                list.add(new ListItem(user,ListType.ETHNICITY, "Asian (Other)", 11));
                list.add(new ListItem(user,ListType.ETHNICITY, "Black (African)", 12));
                list.add(new ListItem(user,ListType.ETHNICITY, "Black (Caribbean)", 13));
                list.add(new ListItem(user,ListType.ETHNICITY, "Black (Other)", 14));
                list.add(new ListItem(user,ListType.ETHNICITY, "Other Ethnic Group", 15));
                list.add(new ListItem(user,ListType.ETHNICITY, "Not known/Did not want to disclose", 16));

                list.add(new ListItem(user, ListType.GROUP, "Test Group 1", 0));
                list.add(new ListItem(user, ListType.GROUP, "Test Group 2", 1));
                list.add(new ListItem(user, ListType.GROUP, "Test Group 3", 2));
                list.add(new ListItem(user, ListType.TIER, "Tier 1", 0));
                list.add(new ListItem(user, ListType.TIER, "Tier 2", 1));
                list.add(new ListItem(user, ListType.TIER, "Tier 3", 2));

                list.add(new ListItem(user, ListType.CLIENT_PDF_TYPE, "Referral Document", 0));
                list.add(new ListItem(user, ListType.CLIENT_PDF_TYPE, "Consent Document", 1));
                list.add(new ListItem(user, ListType.CLIENT_PDF_TYPE, "Case Assessment", 2));

                list.add(new ListItem(user, ListType.DOCUMENT_TYPE, "Case", 0));
                list.add(new ListItem(user, ListType.DOCUMENT_TYPE, "CAT (Criteria Assessment Tool)", 1));
                list.add(new ListItem(user, ListType.DOCUMENT_TYPE, "Pdf Document", 2));
                // V1.2.058 Removed MyWeek so only available through the session register
                // IsDisplayed set to false for existing databases.
                //list.add(new ListItem(user, ListType.DOCUMENT_TYPE, "My Week", 3));
                // V1.5.073 TRA
                //list.add(new ListItem(user, ListType.DOCUMENT_TYPE, "Transport Request Assessment", 4));
                break;
            case 3:
                list.add(new ListItem(user, ListType.COMMISSIONER, "Sample Commissioner 1", 0));
                list.add(new ListItem(user, ListType.COMMISSIONER, "Sample Commissioner 2", 1));
                list.add(new ListItem(user, ListType.COMMISSIONER, "Sample Commissioner 3", 2));
                break;
            case 4:
                // No List changes
                break;
            case 5:
                list.add(new ListItem(user, ListType.HASHTAG, "#ChildInNeed", 0));
                list.add(new ListItem(user, ListType.HASHTAG, "#ChildProtection", 1));
                list.add(new ListItem(user, ListType.HASHTAG, "#CommonAssessmentFramework", 2));
                break;
            case 6:
                list.add(new ListItem(user, ListType.DOCUMENT_TYPE, "Contact", 5));
                list.add(new ListItem(user, ListType.CONTACT_TYPE, "Agency Contact", 0));
                list.add(new ListItem(user, ListType.CONTACT_TYPE, "School Contact", 1));
                list.add(new ListItem(user, ListType.CONTACT_TYPE, "Main Contact", 2));
                list.add(new ListItem(user, ListType.CONTACT_TYPE, "Contact", 3));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Mother (Birth)", 0));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Mother (Step)", 1));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Father (Birth)", 2));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Father (Step)", 3));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Sister", 4));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Brother", 5));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Other Family", 6));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Neighbour", 7));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Teacher", 8));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Social Worker", 9));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Case Worker", 10));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "SENCO", 11));
                list.add(new ListItem(user, ListType.RELATIONSHIP, "Other", 12));
                break;

            case 7:
                list.add(new ListItem(user, ListType.DOCUMENT_TYPE, "Note", 6));
                list.add(new ListItem(user, ListType.DOCUMENT_TYPE, "Image", 7));
                break;
            case 8:
                // Transport records are type TransportOrganisation so cannot be created this way
                //list.add(new ListItem(user, ListType.TRANSPORT_ORGANISATION, "Taxi Company 1", 0));
                //list.add(new ListItem(user, ListType.TRANSPORT_ORGANISATION, "Taxi Company 2", 1));
                break;
            case 9:
            case 10:
                // No change
                break;
            case 11:
                list.add(new ListItem(user, ListType.DOCUMENT_TYPE, "Transport", 8));
                break;
            case 12:
            case 13:
            case 14:
                // No Change
                break;
            case 21:
                // Build 119 2 May 2019 - Text/Email Broadcasting
                // Build 121 4 June - Removed these. Note_Type is a complex list item so
                // this method is inappropriate
                //list.add(new ListItem(user, ListType.NOTE_TYPE, "Text Message", 0));
                //list.add(new ListItem(user, ListType.NOTE_TYPE, "Email", 1));
                //list.add(new ListItem(user, ListType.NOTE_TYPE, "Phone Message", 1));
                break;
            case 29:
                // Build 179 MACA and PANOC
                list.add(new ListItem(user, ListType.DOCUMENT_TYPE, "MACA-YC18", 9));
                list.add(new ListItem(user, ListType.DOCUMENT_TYPE, "PANOC-YC20", 10));
                break;
        }

        return list;
    }

    // This is the SQL to upgrade to each subsequent version in local database (SqllLite)
    private static ArrayList<String> getUpgradeSql(int version){
        ArrayList<String> sqlList = new ArrayList<>();
        switch (version){
            case 1:                                                                 // Initial Create
                sqlList.add("CREATE TABLE Organisation ( " +
                        "Organisation CHAR(200) NOT NULL);");
                sqlList.add("CREATE TABLE ListItem( " +
                        "RecordID CHAR(36) NOT NULL PRIMARY KEY, " +                  // ListItem
                        "ListItemID CHAR(36) NOT NULL, " +
                        "HistoryDate INTEGER NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "CreationDate INTEGER NOT NULL, " +
                        "CreatedByID CHAR(36) NOT NULL, " +
                        "ListType CHAR(200) NOT NULL, " +
                        "ItemValue CHAR(200) NOT NULL, " +
                        "IsDisplayed INTEGER NOT NULL, " +
                        "SerialisedObject BLOB NOT NULL); ");
                sqlList.add("CREATE UNIQUE INDEX ListItemListItemIDListItemHistoryDate ON ListItem(ListItemID, HistoryDate)");
                sqlList.add("CREATE UNIQUE INDEX ListItemListTypeItemValueHistoryDate ON ListItem(ListType, ItemValue, HistoryDate);");
                sqlList.add("CREATE TABLE User( " +                                 // User
                        "RecordID CHAR(36) NOT NULL PRIMARY KEY, " +
                        "UserID CHAR(36) NOT NULL, " +
                        "HistoryDate INTEGER NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "CreationDate INTEGER NOT NULL, " +
                        "CreatedByID CHAR(36) NOT NULL, " +
                        "EmailAddress CHAR(200) NOT NULL, " +
                        "Name CHAR(200) NOT NULL, " +
                        "SerialisedObject BLOB NOT NULL); ");
                sqlList.add("CREATE UNIQUE INDEX UserUserIDHistoryDate ON User(UserID, HistoryDate);");
                sqlList.add("CREATE UNIQUE INDEX UserEmailAddress ON User(EmailAddress, HistoryDate);");
                sqlList.add("CREATE UNIQUE INDEX UserName ON User(Name, HistoryDate);");
                sqlList.add("CREATE TABLE Document( " +                             // Document
                        "RecordID CHAR(36) NOT NULL PRIMARY KEY, " +
                        "DocumentID CHAR(36) NOT NULL, " +
                        "HistoryDate INTEGER NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "CreationDate INTEGER NOT NULL, " +
                        "CreatedByID CHAR(36) NOT NULL, " +
                        "Cancelled INTEGER NOT NULL, " +
                        "ClientID CHAR(36) NOT NULL, " +
                        "DocumentType INTEGER NOT NULL, " +
                        "SerialisedObject BLOB NOT NULL); ");
                sqlList.add("CREATE UNIQUE INDEX DocumentDocumentIDHistoryDate ON Document(DocumentID, HistoryDate);");
                sqlList.add("CREATE TABLE SystemError( " +                          // System Error
                        "RecordID CHAR(36) NOT NULL PRIMARY KEY, " +
                        "SyncID CHAR(36), " +
                        "CreationDate INTEGER NOT NULL, " +
                        "CreatedByID CHAR(36) NOT NULL, " +
                        "SerialisedObject BLOB NOT NULL); ");
                sqlList.add("CREATE TABLE Blobs( " +                                // Blobs
                        "BlobID CHAR(36) PRIMARY KEY NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "Content BLOB NOT NULL, " +
                        "NextChunk CHAR(36) NOT NULL);");
                sqlList.add("CREATE TABLE Sync( " +                                 //Sync
                        "SyncID CHAR(36)PRIMARY KEY NOT NULL, " +
                        "SyncDate INTEGER NOT NULL, " +
                        "TableName CHAR(20) NOT NULL);");
                sqlList.add("CREATE TABLE SyncActivity( "+
                        "RecordID CHAR(36) NOT NULL,  " +
                        "CreationDate INTEGER NOT NULL, " +
                        "CompletionDate INTEGER NOT NULL, " +
                        "CreatedByID CHAR(36) NOT NULL, " +
                        "Result CHAR(10) NOT NULL, " +
                        "Summary CHAR(200) NOT NULL, " +
                        "Log TEXT NOT NULL)");
                sqlList.add("CREATE TABLE ReadAudit( " +                             // ReadAudit
                        "ReadByID CHAR(36) NOT NULL, " +
                        "DocumentID CHAR(36) NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "ReadDate INTEGER NOT NULL, " +
                        "PRIMARY KEY (ReadByID, DocumentID))");
                break;
            case 2:
                sqlList.add("ALTER TABLE Document ADD COLUMN ReferenceDate INTEGER");
                break;
            case 3:
                sqlList.add("CREATE TABLE Follow(" +
                        "UserID CHAR(36) NOT NULL, " +
                        "ClientID CHAR(36) NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "Cancelled INTEGER NOT NULL, " +
                        "PRIMARY KEY (UserID, ClientID))");
                break;
            case 4:
                sqlList.add("ALTER TABLE Follow ADD COLUMN StartDate INTEGER");
                break;
            case 5:
            case 6:
            case 7:
            case 8:
                // No database changes, only new List items
                break;
            case 9:
            case 10:
                sqlList.add("DELETE FROM ListItem WHERE ListType = 'TRANSPORT_ORGANISATION' AND ItemValue = 'Taxi Company 1'; ");
                sqlList.add("DELETE FROM ListItem WHERE ListType = 'TRANSPORT_ORGANISATION' AND ItemValue = 'Taxi Company 2'; ");
                break;
            case 11:
                // No change
                break;
            case 12:
                //sqlList.add("ALTER TABLE Document ADD INDEX DocumentID");
                //sqlList.add("ALTER TABLE ReadAudit ADD INDEX DocumentID");
                break;
            case 13:
                // Index already exists, see case 1
                //sqlList.add("CREATE INDEX DocumentDocumentIDHistoryDate ON Document(DocumentID, HistoryDate);");
                sqlList.add("CREATE INDEX ReadAuditDocumentID ON ReadAudit(DocumentID);");
                break;
            case 14:
                sqlList.add("CREATE INDEX ReadAuditReadByID ON ReadAudit(ReadByID);");
                break;
            case 15:
                sqlList.add("CREATE INDEX DocumentClientIDHistoryDate ON Document(ClientID, HistoryDate);");
                break;
            case 16:
                sqlList.add("CREATE INDEX DocumentDocumentTypeHistoryDate ON Document(DocumentType, HistoryDate);");
                break;
            case 17:
                //sqlList.add("CREATE INDEX DocumentDocumentTypeHistoryDate ON Document(DocumentType, HistoryDate);");
                break;
            case 18:
                sqlList.add("ALTER TABLE Document ADD COLUMN SessionID CHAR(36)");
                break;
            case 19:
                sqlList.add("UPDATE Document SET SessionID = ''");
                sqlList.add("CREATE INDEX DocumentDocumentTypeHistoryDateSessionID ON Document(DocumentType, HistoryDate, SessionID);");
                break;
            case 21:
                // Updated list items only
                break;
            case 22:
                sqlList.add("DELETE FROM ListItem WHERE ListType = 'NOTE_TYPE' AND ItemValue = 'Phone Message'; ");
                break;
            case 23:
                sqlList.add("CREATE INDEX DocumentDocumentTypeHistoryDateReferenceDate ON Document(DocumentType, HistoryDate, ReferenceDate);");
                break;
            case 24:
                sqlList.add("CREATE INDEX FollowUserIDCancelled ON Follow(UserID, Cancelled);");
                break;
            case 25:
                sqlList.add("CREATE INDEX DocumentClientIDReferenceDate ON Document(ClientID, ReferenceDate);");
                break;
        }
        return sqlList;
    }

    // This is the SQL to upgrade to each subsequent version in web database (MySQL)
    // MySQL has MEDIUMBLOB for 16MByte BLOBS
    // Web database has no ID/HistoryDate constraints (See Sync Notes)
    // Web database has no LocalSyncTable
    public static ArrayList<String> getWebUpgradeSql(int version){
        ArrayList<String> sqlList = new ArrayList<>();
        switch (version){
            case 1:                                                                 // Initial Create
                sqlList.add("CREATE TABLE Version ( " +
                        "Organisation CHAR(200) NOT NULL, " +
                        "VersionNumber INTEGER NOT NULL);");
                sqlList.add("CREATE TABLE ListItem( " +
                        "RecordID CHAR(36) NOT NULL PRIMARY KEY, " +                  // ListItem
                        "ListItemID CHAR(36) NOT NULL, " +
                        "HistoryDate BIGINT NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "CreationDate BIGINT NOT NULL, " +
                        "CreatedByID CHAR(36) NOT NULL, " +
                        "ListType CHAR(200) NOT NULL, " +
                        "ItemValue CHAR(200) NOT NULL, " +
                        "IsDisplayed INTEGER NOT NULL, " +
                        "SerialisedObject BLOB NOT NULL); ");
                sqlList.add("CREATE TABLE User( " +                                 // User
                        "RecordID CHAR(36) NOT NULL PRIMARY KEY, " +
                        "UserID CHAR(36) NOT NULL, " +
                        "HistoryDate BIGINT NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "CreationDate BIGINT NOT NULL, " +
                        "CreatedByID CHAR(36) NOT NULL, " +
                        "EmailAddress CHAR(200) NOT NULL, " +
                        "Name CHAR(200) NOT NULL, " +
                        "SerialisedObject BLOB NOT NULL); ");
                sqlList.add("CREATE TABLE Document( " +                             // Document
                        "RecordID CHAR(36) NOT NULL PRIMARY KEY, " +
                        "DocumentID CHAR(36) NOT NULL, " +
                        "HistoryDate BIGINT NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "CreationDate BIGINT NOT NULL, " +
                        "CreatedByID CHAR(36) NOT NULL, " +
                        "Cancelled INTEGER NOT NULL, " +
                        "ClientID CHAR(36) NOT NULL, " +
                        "DocumentType INTEGER NOT NULL, " +
                        "SerialisedObject BLOB NOT NULL); ");
                sqlList.add("CREATE TABLE SystemError( " +                          // System Error
                        "RecordID CHAR(36) NOT NULL PRIMARY KEY, " +
                        "SyncID CHAR(36), " +
                        "CreationDate BIGINT NOT NULL, " +
                        "CreatedByID CHAR(36) NOT NULL, " +
                        "SerialisedObject BLOB NOT NULL); ");
                sqlList.add("CREATE TABLE Blobs( " +                                // Blobs
                        "BlobID CHAR(36) PRIMARY KEY NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "Content MEDIUMBLOB NOT NULL, " +
                        "NextChunk CHAR(36) NOT NULL);");
                sqlList.add("CREATE TABLE Sync( " +                                 //Sync
                        "SyncID CHAR(36)PRIMARY KEY NOT NULL, " +
                        "SyncDate BIGINT NOT NULL, " +
                        "TableName CHAR(20) NOT NULL);");
                sqlList.add("CREATE TABLE ReadAudit( " +                             // ReadAudit
                        "ReadByID CHAR(36) NOT NULL, " +
                        "DocumentID CHAR(36) NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "ReadDate BIGINT NOT NULL, " +
                        "PRIMARY KEY (ReadByID, DocumentID))");
                break;

            case 2:
                sqlList.add("ALTER TABLE Document ADD COLUMN ReferenceDate BIGINT");
                break;

            case 3:
                sqlList.add("CREATE TABLE Follow(" +
                        "UserID CHAR(36) NOT NULL, " +
                        "ClientID CHAR(36) NOT NULL, " +
                        "SyncID CHAR(36), " +
                        "Cancelled INTEGER NOT NULL, " +
                        "PRIMARY KEY (UserID, ClientID))");
                break;
            case 4:
                sqlList.add("ALTER TABLE Follow ADD COLUMN StartDate BIGINT");
                break;
            case 5:
            case 6:
            case 7:
            case 8:
                // No database changes, only new List items
                break;
            case 9:
            case 10:
                sqlList.add("DELETE FROM ListItem WHERE ListType = 'TRANSPORT_ORGANISATION' AND ItemValue = 'Taxi Company 1'; ");
                sqlList.add("DELETE FROM ListItem WHERE ListType = 'TRANSPORT_ORGANISATION' AND ItemValue = 'Taxi Company 2'; ");
                break;
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
                // No change
                break;
            case 18:
                sqlList.add("ALTER TABLE Document ADD COLUMN SessionID CHAR(36)");
                break;
            case 19:
                break;
            // Build 107 31 Aug 2018 Added as a test - should have no effect
            case 20:
                sqlList.add("DELETE FROM ListItem WHERE ListType = 'TRANSPORT_ORGANISATION' AND ItemValue = 'Taxi Company 1'; ");
                break;
            case 21:
                // Updated list items only
                break;
            case 22:
                sqlList.add("DELETE FROM ListItem WHERE ListType = 'NOTE_TYPE' AND ItemValue = 'Phone Message'; ");
                break;
            case 23:
                // No change
                break;
        }
        return sqlList;
    }
}

