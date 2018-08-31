package solutions.cris.utils;

import java.util.ArrayList;

import solutions.cris.db.LocalDB;
import solutions.cris.object.Group;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.Role;
import solutions.cris.object.User;

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

public class GroupPickList extends PickList {

    public GroupPickList(LocalDB localDB ) {
        super(localDB, ListType.GROUP, -1);
    }

    public GroupPickList(LocalDB localDB, int defaultPosition ) {
        super(localDB, ListType.GROUP, defaultPosition);
    }

    public GroupPickList(LocalDB localDB, User currentUser) {
        super(localDB, ListType.GROUP);
        // Only include visible groups
        ArrayList<ListItem> newListItems = new ArrayList<>();
        ArrayList<String> newOptions = new ArrayList<>();
        for (int i=getListItems().size()-1; i>=0; i--){
            ListItem listItem = getListItems().get(i);
            if (!listItem.getItemValue().equals("Please select")) {
                Group group = (Group) listItem;
                boolean selected = false;
                if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_EDIT_ALL_SESSIONS)) {
                    selected = true;
                } else if (group.getKeyWorkerID() != null &&
                        group.getKeyWorkerID().equals(currentUser.getUserID())) {
                    selected = true;
                } else if (group.getSessionCoordinatorID() != null &&
                        group.getSessionCoordinatorID().equals(currentUser.getUserID())) {
                    selected = true;
                }
                if (!selected) {
                    getListItems().remove(i);
                    getOptions().remove(i);
                }
            }
        }
        // Include the AdHoc group
        getListItems().add(1, Group.getAdHocGroup());
        getOptions().add(1,"Ad-Hoc Group");
    }


}
