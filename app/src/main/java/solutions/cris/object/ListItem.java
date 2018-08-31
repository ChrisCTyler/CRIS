package solutions.cris.object;

import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;

import solutions.cris.db.LocalDB;

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
public class ListItem extends CrisObject implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = SVUID_LIST_ITEM;

    // Special for creating the System Administrator Role
    public ListItem(UUID listItemID, User currentUser, ListType listType, String itemValue, int itemOrder){
        super(currentUser);
        initListItem(listItemID, listType, itemValue, itemOrder);
    }
    public ListItem(User currentUser, ListType listType, String itemValue, int itemOrder) {
        super(currentUser);
        initListItem(UUID.randomUUID(), listType, itemValue, itemOrder);
    }

    private void initListItem(UUID listItemID, ListType listType, String itemValue, int itemOrder){
        this.listItemID = listItemID;
        this.listType = listType;
        this.itemValue = itemValue;
        this.itemOrder = itemOrder;
        isDefault = false;
        isDisplayed = true;
        hasBeenModified = true;
    }

    private UUID listItemID;
    public UUID getListItemID() {return listItemID;}
    // Need SetListItem for GROUP in V1.2
    public void setListItemID(UUID listItemID) {this.listItemID = listItemID;}

    // HasBeenModified (Used by the list item editor, value is always false in database)
    private boolean hasBeenModified;
    public boolean hasBeenModified()  {return hasBeenModified;}
    public void setHasBeenModified(boolean hasBeenModified) {this.hasBeenModified = hasBeenModified;}

    //ListType
    private ListType listType;
    public ListType getListType()  {return listType;}
    //public void setListType(ListType listType) {this.listType = listType;}

    //ItemValue
    private String itemValue;
    public String getItemValue()  {return itemValue;}
    public void setItemValue(String itemValue) {this.itemValue = itemValue;}

    //ItemOrder
    private int itemOrder;
    public int getItemOrder()  {return itemOrder;}
    public void setItemOrder(int itemOrder) {this.itemOrder = itemOrder;}

    // IsDefault
    private boolean isDefault;
    public boolean isDefault()  {return isDefault;}
    public void setIsDefault(boolean isDefault) {this.isDefault = isDefault;}

    private boolean isDisplayed;
    public boolean isDisplayed()  {return isDisplayed;}
    public void setIsDisplayed(boolean isDisplayed) {this.isDisplayed = isDisplayed;}

    public void save(boolean newMode){
        LocalDB localDB = LocalDB.getInstance();
        localDB.save(this, newMode, User.getCurrentUser());
    }

    public static Comparator<ListItem> comparator = new Comparator<ListItem>() {
        @Override
        public int compare(ListItem o1, ListItem o2) {
            return (o1.getItemOrder() - o2.getItemOrder());
        }
    };

    public static Comparator<ListItem> comparatorAZ = new Comparator<ListItem>() {
        @Override
        public int compare(ListItem o1, ListItem o2) {
            return (o1.getItemValue().compareTo(o2.getItemValue()));
        }
    };

    public static Comparator<ListItem> comparatorZA = new Comparator<ListItem>() {
        @Override
        public int compare(ListItem o1, ListItem o2) {
            return (o2.getItemValue().compareTo(o1.getItemValue()));
        }
    };

    /*
    public String textSummary(){
        String summary = "List Type:\n" + listType.toString() + "\n\n";
        summary += "Item Value:\n" + itemValue + "\n\n";
        summary += "Item Order:\n" + Integer.toString(itemOrder) + "\n\n";
        summary += "Is Default:\n" + Boolean.toString(isDefault) + "\n\n";
        summary += "Is Displayed:\n" + Boolean.toString(isDisplayed) + "\n\n";
        return summary;
    }
    */
}
