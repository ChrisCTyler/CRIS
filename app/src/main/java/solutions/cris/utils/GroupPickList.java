package solutions.cris.utils;

import java.util.ArrayList;

import solutions.cris.db.LocalDB;
import solutions.cris.object.Group;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.Role;
import solutions.cris.object.User;

/**
 * Created by Chris Tyler on 05/10/2017.
 */

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
