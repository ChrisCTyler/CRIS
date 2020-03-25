package solutions.cris.object;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.LocalSettings;
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

public class Role extends ListItem implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_ROLE;

    public static final UUID systemAdministratorID = UUID.fromString("752e7808-acba-11e6-80f5-76304dec7eb7");
    public static final UUID noPrivilegeID = UUID.fromString("2367e279-902f-42a2-b27a-3e1f4aca9efa");

    public static final long PRIVILEGE_ACCESS_ALL_CLIENTS = 0x0001;
    public static final long PRIVILEGE_ACCESS_MY_CLIENTS = 0x0002;
    public static final long PRIVILEGE_READ_ALL_CLIENTS = 0x0004;
    public static final long PRIVILEGE_READ_MY_CLIENTS = 0x0008;
    public static final long PRIVILEGE_WRITE_ALL_CLIENTS = 0x0010;
    public static final long PRIVILEGE_WRITE_MY_CLIENTS = 0x0020;

    public static final long PRIVILEGE_ACCESS_ALL_DOCUMENTS = 0x0040;
    public static final long PRIVILEGE_READ_ALL_DOCUMENTS = 0x0080;
    public static final long PRIVILEGE_WRITE_ALL_DOCUMENTS = 0x0100;

    public static final long PRIVILEGE_ACCESS_NOTES = 0x0200;
    public static final long PRIVILEGE_READ_NOTES = 0x0400;
    public static final long PRIVILEGE_WRITE_NOTES = 0x0800;

    public static final long PRIVILEGE_ACCESS_SESSIONS = 0x1000;
    public static final long PRIVILEGE_READ_SESSIONS = 0x2000;
    public static final long PRIVILEGE_WRITE_SESSIONS = 0x4000;

    public static final long PRIVILEGE_SYSTEM_ADMINISTRATOR = 0x8000;
    public static final long PRIVILEGE_CREATE_NOTES = 0x00010000;
    public static final long PRIVILEGE_CREATE_SESSIONS = 0x00020000;
    public static final long PRIVILEGE_VIEW_USER_RECORD = 0x00040000;
    public static final long PRIVILEGE_WRITE_LIBRARY_DOCUMENTS = 0x00080000;
    public static final long PRIVILEGE_CREATE_NEW_CLIENTS = 0x00100000;
    public static final long PRIVILEGE_USER_IS_KEYWORKER = 0x00200000;
    public static final long PRIVILEGE_SUPERVISOR_SET_TO_FOLLOW = 0x00400000;
    public static final long PRIVILEGE_EDIT_ALL_SESSIONS = 0x00800000;
    public static final long PRIVILEGE_ALLOW_EXPORT = 0x01000000;


    public Role(User currentUser, String itemValue, int itemOrder){
        super(currentUser, ListType.ROLE, itemValue, itemOrder);
        // New user has no privilege;
        privileges = 0;
     }

    public Role(User currentUser, UUID listAdminID){
        super(listAdminID, currentUser, ListType.ROLE, "System Administrator", 0);
        if (listAdminID == Role.systemAdministratorID) {
            this.privileges = Long.MAX_VALUE;
        } else if (listAdminID == Role.noPrivilegeID) {
            this.privileges = 0;
        } else {
            throw new CRISException("Illegal attempt to instantiate Role");
        }
    }

    private long privileges;
    public boolean hasPrivilege(long privilege) {return (privileges & privilege) != 0;}
    public void setPrivilege(long privilege, boolean isChecked) {
        if (isChecked) {
            privileges = privileges | privilege;
        } else {
            privileges = privileges & ~privilege;
        }
    }
    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance();
        String summary = super.textSummary();
        summary += String.format("PRIVILEGE_ACCESS_ALL_CLIENTS: %b", hasPrivilege(PRIVILEGE_ACCESS_ALL_CLIENTS));
        summary += String.format("PRIVILEGE_ACCESS_MY_CLIENTS: %b", hasPrivilege(PRIVILEGE_ACCESS_MY_CLIENTS));
        summary += String.format("PRIVILEGE_READ_ALL_CLIENTS: %b", hasPrivilege(PRIVILEGE_READ_ALL_CLIENTS));
        summary += String.format("PRIVILEGE_READ_MY_CLIENTS: %b", hasPrivilege(PRIVILEGE_READ_MY_CLIENTS));
        summary += String.format("PRIVILEGE_WRITE_ALL_CLIENTS: %b", hasPrivilege(PRIVILEGE_WRITE_ALL_CLIENTS));
        summary += String.format("PRIVILEGE_WRITE_MY_CLIENTS: %b", hasPrivilege(PRIVILEGE_WRITE_MY_CLIENTS));
        summary += String.format("PRIVILEGE_ACCESS_ALL_DOCUMENTS: %b", hasPrivilege(PRIVILEGE_ACCESS_ALL_DOCUMENTS));
        summary += String.format("PRIVILEGE_READ_ALL_DOCUMENTS: %b", hasPrivilege(PRIVILEGE_READ_ALL_DOCUMENTS));
        summary += String.format("PRIVILEGE_WRITE_ALL_DOCUMENTS: %b", hasPrivilege(PRIVILEGE_WRITE_ALL_DOCUMENTS));
        summary += String.format("PRIVILEGE_ACCESS_NOTES: %b", hasPrivilege(PRIVILEGE_ACCESS_NOTES));
        summary += String.format("PRIVILEGE_READ_NOTES: %b", hasPrivilege(PRIVILEGE_READ_NOTES));
        summary += String.format("PRIVILEGE_WRITE_NOTES: %b", hasPrivilege(PRIVILEGE_WRITE_NOTES));
        //summary += String.format("PRIVILEGE_ACCESS_SESSIONS: %b", hasPrivilege(PRIVILEGE_ACCESS_SESSIONS));
        //summary += String.format("PRIVILEGE_READ_SESSIONS: %b", hasPrivilege(PRIVILEGE_READ_SESSIONS));
        //summary += String.format("PRIVILEGE_WRITE_SESSIONS: %b", hasPrivilege(PRIVILEGE_WRITE_SESSIONS));
        summary += String.format("PRIVILEGE_SYSTEM_ADMINISTRATOR: %b", hasPrivilege(PRIVILEGE_SYSTEM_ADMINISTRATOR));
        summary += String.format("PRIVILEGE_CREATE_NOTES: %b", hasPrivilege(PRIVILEGE_CREATE_NOTES));
        summary += String.format("PRIVILEGE_CREATE_SESSIONS: %b", hasPrivilege(PRIVILEGE_CREATE_SESSIONS));
        summary += String.format("PRIVILEGE_VIEW_USER_RECORD: %b", hasPrivilege(PRIVILEGE_VIEW_USER_RECORD));
        summary += String.format("PRIVILEGE_WRITE_LIBRARY_DOCUMENTS: %b", hasPrivilege(PRIVILEGE_WRITE_LIBRARY_DOCUMENTS));
        summary += String.format("PRIVILEGE_CREATE_NEW_CLIENTS: %b", hasPrivilege(PRIVILEGE_CREATE_NEW_CLIENTS));
        summary += String.format("PRIVILEGE_USER_IS_KEYWORKER: %b", hasPrivilege(PRIVILEGE_USER_IS_KEYWORKER));
        summary += String.format("PRIVILEGE_SUPERVISOR_SET_TO_FOLLOW: %b", hasPrivilege(PRIVILEGE_SUPERVISOR_SET_TO_FOLLOW));
        summary += String.format("PRIVILEGE_EDIT_ALL_SESSIONS: %b", hasPrivilege(PRIVILEGE_EDIT_ALL_SESSIONS));
        summary += String.format("PRIVILEGE_ALLOW_EXPORT: %b", hasPrivilege(PRIVILEGE_ALLOW_EXPORT));

        return summary;
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action) {
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance();
        Role previousItem = (Role) localDB.getListItemByRecordID(previousRecordID);
        Role thisItem = (Role) localDB.getListItemByRecordID(thisRecordID);
        String changes = ListItem.getChanges(previousItem, thisItem);
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_ACCESS_ALL_CLIENTS), thisItem.hasPrivilege(PRIVILEGE_ACCESS_ALL_CLIENTS), "PRIVILEGE_ACCESS_ALL_CLIENTS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_ACCESS_MY_CLIENTS), thisItem.hasPrivilege(PRIVILEGE_ACCESS_MY_CLIENTS), "PRIVILEGE_ACCESS_MY_CLIENTS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_READ_ALL_CLIENTS), thisItem.hasPrivilege(PRIVILEGE_READ_ALL_CLIENTS), "PRIVILEGE_READ_ALL_CLIENTS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_READ_MY_CLIENTS), thisItem.hasPrivilege(PRIVILEGE_READ_MY_CLIENTS), "PRIVILEGE_READ_MY_CLIENTS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_WRITE_ALL_CLIENTS), thisItem.hasPrivilege(PRIVILEGE_WRITE_ALL_CLIENTS), "PRIVILEGE_WRITE_ALL_CLIENTS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_WRITE_MY_CLIENTS), thisItem.hasPrivilege(PRIVILEGE_WRITE_MY_CLIENTS), "PRIVILEGE_WRITE_MY_CLIENTS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_ACCESS_ALL_DOCUMENTS), thisItem.hasPrivilege(PRIVILEGE_ACCESS_ALL_DOCUMENTS), "PRIVILEGE_ACCESS_ALL_DOCUMENTS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_READ_ALL_DOCUMENTS), thisItem.hasPrivilege(PRIVILEGE_READ_ALL_DOCUMENTS), "PRIVILEGE_READ_ALL_DOCUMENTS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_WRITE_ALL_DOCUMENTS), thisItem.hasPrivilege(PRIVILEGE_WRITE_ALL_DOCUMENTS), "PRIVILEGE_WRITE_ALL_DOCUMENTS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_ACCESS_NOTES), thisItem.hasPrivilege(PRIVILEGE_ACCESS_NOTES), "PRIVILEGE_ACCESS_NOTES");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_READ_NOTES), thisItem.hasPrivilege(PRIVILEGE_READ_NOTES), "PRIVILEGE_READ_NOTES");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_WRITE_NOTES), thisItem.hasPrivilege(PRIVILEGE_WRITE_NOTES), "PRIVILEGE_WRITE_NOTES");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_ACCESS_SESSIONS), thisItem.hasPrivilege(PRIVILEGE_ACCESS_SESSIONS), "PRIVILEGE_ACCESS_SESSIONS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_READ_SESSIONS), thisItem.hasPrivilege(PRIVILEGE_READ_SESSIONS), "PRIVILEGE_READ_SESSIONS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_WRITE_SESSIONS), thisItem.hasPrivilege(PRIVILEGE_WRITE_SESSIONS), "PRIVILEGE_WRITE_SESSIONS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_SYSTEM_ADMINISTRATOR), thisItem.hasPrivilege(PRIVILEGE_SYSTEM_ADMINISTRATOR), "PRIVILEGE_SYSTEM_ADMINISTRATOR");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_CREATE_NOTES), thisItem.hasPrivilege(PRIVILEGE_CREATE_NOTES), "PRIVILEGE_CREATE_NOTES");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_CREATE_SESSIONS), thisItem.hasPrivilege(PRIVILEGE_CREATE_SESSIONS), "PRIVILEGE_CREATE_SESSIONS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_VIEW_USER_RECORD), thisItem.hasPrivilege(PRIVILEGE_VIEW_USER_RECORD), "PRIVILEGE_VIEW_USER_RECORD");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_WRITE_LIBRARY_DOCUMENTS), thisItem.hasPrivilege(PRIVILEGE_WRITE_LIBRARY_DOCUMENTS), "PRIVILEGE_WRITE_LIBRARY_DOCUMENTS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_CREATE_NEW_CLIENTS), thisItem.hasPrivilege(PRIVILEGE_CREATE_NEW_CLIENTS), "PRIVILEGE_CREATE_NEW_CLIENTS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_USER_IS_KEYWORKER), thisItem.hasPrivilege(PRIVILEGE_USER_IS_KEYWORKER), "PRIVILEGE_USER_IS_KEYWORKER");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_SUPERVISOR_SET_TO_FOLLOW), thisItem.hasPrivilege(PRIVILEGE_SUPERVISOR_SET_TO_FOLLOW), "PRIVILEGE_SUPERVISOR_SET_TO_FOLLOW");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_EDIT_ALL_SESSIONS), thisItem.hasPrivilege(PRIVILEGE_EDIT_ALL_SESSIONS), "PRIVILEGE_EDIT_ALL_SESSIONS");
        changes += CRISUtil.getChanges(previousItem.hasPrivilege(PRIVILEGE_ALLOW_EXPORT), thisItem.hasPrivilege(PRIVILEGE_ALLOW_EXPORT), "PRIVILEGE_ALLOW_EXPORT");
        if (changes.length() == 0) {
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
    }
}
