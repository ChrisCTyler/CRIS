package solutions.cris.utils;

import java.util.ArrayList;
import java.util.Collections;

import solutions.cris.db.LocalDB;
import solutions.cris.object.Group;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.Session;
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

public class PickList {

    //public final static String KEYWORKER = "solutions.cris.keyworker";

    public PickList(LocalDB localDB, ListType listType) {
        newPickList(localDB, listType, -1);
    }

    public PickList(LocalDB localDB, ListType listType, int defaultPosition) {
        newPickList(localDB, listType, defaultPosition);
    }

    private void newPickList(LocalDB localDB, ListType listType, int defaultPosition){
        this.defaultPosition = defaultPosition;
        options = new ArrayList<>();
        listItems = localDB.getAllListItems(listType.toString(), false);
        Collections.sort(listItems, ListItem.comparator);
        for (ListItem listItem : listItems) {
            if (listItem.isDisplayed()) {
                if (listItem.isDefault()) {
                    defaultPosition = options.size();
                }
                options.add(listItem.getItemValue());
            }
        }
        if (defaultPosition == -1) {
            User unknownUser = new User(User.unknownUser);
            listItems.add(0, new ListItem(unknownUser, listType, "Please select", -1));
            options.add(0, "Please select");
        }
    }

    public PickList(ArrayList<User> sourceUsers) {
        users = new ArrayList<>();
        options = new ArrayList<>();
        for (User user : sourceUsers) {
            users.add(user);
            options.add(user.getFullName());
        }
        User unknownUser = new User(User.unknownUser);
        users.add(0, unknownUser);
        options.add(0, "Please select");
        defaultPosition = 0;
    }

    private ArrayList<User> users;

    public ArrayList<User> getUsers() {
        return users;
    }

    private ArrayList<ListItem> listItems;

    public ArrayList<ListItem> getListItems() {
        return listItems;
    }

    public void setListItems(ArrayList<ListItem> listItems){
        this.listItems = listItems;
    }

    private ArrayList<String> options;

    public ArrayList<String> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<String> options){
        this.options = options;
    }

    private int defaultPosition = -1;

    public int getDefaultPosition() {
        return defaultPosition;
    }

    public int getPosition(ListItem searchListItem){
        if (searchListItem == null) return 0;
        for (ListItem listItem:listItems){
            if (listItem.getListItemID().equals(searchListItem.getListItemID())){
                return listItems.indexOf(listItem);
            }
        }
        return 0;
    }

    public int getPosition(User searchUser){
        if (searchUser == null) return 0;
        for (User user:users){
            if (user.getUserID().equals(searchUser.getUserID())){
                return users.indexOf(user);
            }
        }
        return 0;
    }
}
