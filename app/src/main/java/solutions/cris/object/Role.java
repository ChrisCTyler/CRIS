package solutions.cris.object;

import java.io.Serializable;
import java.util.UUID;

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
}
