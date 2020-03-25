package solutions.cris.object;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;

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
public abstract class CrisObject implements Serializable{

    // Fixed UID for CRIS classes 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long SVUID_CRIS_OBJECT = 1234500101L;
    static final long SVUID_CLIENT = 1234500201L;
    static final long SVUID_DOCUMENT = 1234500301L;
    static final long SVUID_LIST_ITEM = 1234500401L;
    //static final long SVUID_READ_FLAG = 1234500501L;
    static final long SVUID_SCHOOL = 1234500601L;
    static final long SVUID_USER = 1234500701L;
    static final long SVUID_PDF_DOCUMENT = 1234500801L;
    static final long SVUID_SYSTEM_ERROR = 1234500901L;
    static final long SVUID_ROLE = 1234501001L;
    static final long SVUID_CASE = 1234501101L;
    static final long SVUID_NOTE_TYPE = 1234501201L;
    static final long SVUID_NOTE = 1234501301L;
    public static final long SVUID_CRIS_MENU_ITEM = 1234501401L;
    static final long SVUID_AGENCY = 1234501501L;
    static final long SVUID_CONTACT = 1234501601L;
    static final long SVUID_CAT = 1234501701L;
    static final long SVUID_SESSION = 1234501801L;
    static final long SVUID_CLIENT_SESSION = 1234501801L;
    static final long SVUID_GROUP = 1234501901L;
    static final long SVUID_IMAGE = 1234502001L;
    static final long SVUID_MY_WEEK = 1234502101L;
    static final long SVUID_STATUS = 1234502201L;
    static final long SVUID_TRANSPORT_ORGANISATION = 1234502301L;
    static final long SVUID_TRANSPORT = 1234502401L;
    // Build 139 -Added KPIs (public because CRISKPIItem declared in Utils not Object
    public static final long SVUID_CRIS_KPI_ITEM = 1234502501L;
    static final long SVUID_RAW_DOCUMENT = 1234502601L;

    private static final long serialVersionUID = SVUID_CRIS_OBJECT;


    // Constructor
    public CrisObject(User currentUser) {
        recordID = UUID.randomUUID();
        creationDate = new Date();
        createdByID = currentUser.getUserID();
    }

    // This is a special case to enable the Unknown/FirstTime to be instantiated
    public CrisObject(UUID userID) {
        if (userID != User.firstTimeUser && userID != User.unknownUser) {
            throw new CRISException("Attempt to instatiate User with Invalid UUID");
        }
        else {
            recordID = UUID.randomUUID();
            creationDate = new Date();
            createdByID = userID;
        }
    }

    // Build 139 - KPI Special Constructor for RawDocument
    public CrisObject(UUID recordID, Date creationDate, UUID createdByID) {
        this.recordID = recordID;
        this. creationDate = creationDate;
        this.createdByID = createdByID;
    }

    //RecordID
    private UUID recordID;
    public UUID getRecordID()  {return recordID;}
    public void setRecordID(UUID recordID) {this.recordID = recordID;}

    //ObjectID
    // Every class which extends CRIS Object will have its own ObjectID (as well as
    // the RecordID. RecordID is the Primary Key and therefore is different for all
    // rcords in the table. Two or more records can share an ObjectID if they are
    // edited versions of the same document. ObjectID, HistoryDate will be unique.

    //CreationDate
    private Date creationDate;
    public Date getCreationDate()  {return creationDate;}
    public void setCreationDate(Date creationDate) {this.creationDate = creationDate;}

    //CreatedByID
    private UUID createdByID;
    public UUID getCreatedByID()  {return createdByID;}
    public void setCreatedByID(UUID createdByID) {this.createdByID = createdByID;}

    public static Comparator<CrisObject> comparatorCreationDate = new Comparator<CrisObject>() {
        @Override
        public int compare(CrisObject o1, CrisObject o2) {
            if (o1.creationDate.after(o2.creationDate)) {return -1;}
            else {return 1;}
        }
    };

}
