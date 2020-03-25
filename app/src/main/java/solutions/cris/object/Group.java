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

public class Group extends ListItem implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_GROUP;

    public static final UUID adHocGroupID = UUID.fromString("587f2835-d5f6-4593-ab95-137489ae3162");

    public final static String[] frequencyTypeValues = {"Please select", "Days",
            "Weeks", "Fortnights", "Months"};

    public Group(User currentUser, String itemValue, int itemOrder) {
        super(currentUser, ListType.GROUP, itemValue, itemOrder);
        keyWorkerID = null;
        sessionCoordinatorID = null;
        frequency = 0;
        frequencyType = "Please Select";
        groupAddress = "";
        groupPostcode = "";

    }

    public static Group getAdHocGroup() {
        Group group = new Group(new User(User.unknownUser),"Ad Hoc Group", 0);
        group.setListItemID(Group.adHocGroupID);
        return group;
    }

    private UUID keyWorkerID;

    public UUID getKeyWorkerID() {
        return keyWorkerID;
    }

    public void setKeyWorkerID(UUID keyWorkerID) {
        this.keyWorkerID = keyWorkerID;
    }

    private UUID sessionCoordinatorID;

    public void setSessionCoordinatorID(UUID sessionCoordinatorID) {
        this.sessionCoordinatorID = sessionCoordinatorID;
    }

    private User keyWorker;

    public User getKeyWorker() {
        if (keyWorkerID != null && keyWorker == null){
            LocalDB localDB = LocalDB.getInstance();
            keyWorker = localDB.getUser(keyWorkerID);
        }
        return keyWorker;
    }

    public void setKeyWorker(User keyWorker) {
        this.keyWorker = keyWorker;
    }

    public UUID getSessionCoordinatorID() {
        return sessionCoordinatorID;
    }

    private int frequency;

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    private User sessionCoordinator;

    public User getSessionCoordinator() {
        if (sessionCoordinatorID != null && sessionCoordinator == null) {
            LocalDB localDB = LocalDB.getInstance();
            sessionCoordinator = localDB.getUser(sessionCoordinatorID);
        }
        return sessionCoordinator;
    }

    public void setSessionCoordinator(User sessionCoordinator) {
        this.sessionCoordinator = sessionCoordinator;
    }

    private String frequencyType;

    public String getFrequencyType() {
        return frequencyType;
    }

    public void setFrequencyType(String frequencyType) {
        boolean found = false;
        for (String frequencyTypeValue : frequencyTypeValues) {
            if (frequencyTypeValue.equals(frequencyType)) {
                found = true;
            }
        }
        if (found) {
            this.frequencyType = frequencyType;
        } else {
            throw new CRISException("Unexpected Frequency Type: " + frequencyType);
        }
    }

    private String groupAddress;

    public String getGroupAddress() {
        return groupAddress;
    }

    public void setGroupAddress(String groupAddress) {
        this.groupAddress = groupAddress;
    }

    private String groupPostcode;

    public String getGroupPostcode() {
        return groupPostcode;
    }

    public void setGroupPostcode(String groupPostcode) {
        this.groupPostcode = groupPostcode;
    }

    private void clear(){
        setKeyWorker(null);
        setSessionCoordinator(null);
    }

    public void save(boolean newMode){
        LocalDB localDB = LocalDB.getInstance();

        // Remove the run-time fields before serializing
        User keyWorker = getKeyWorker();
        User sessionCoordinator = getSessionCoordinator();
        clear();

        localDB.save(this, newMode, User.getCurrentUser());

        setKeyWorker(keyWorker);
        setSessionCoordinator(sessionCoordinator);
    }

    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance();
        String summary = super.textSummary();
        if (getKeyWorker() != null) {
            summary += String.format("%s: %s\n",
                    localSettings.Keyworker,
                    getKeyWorker().getFullName());
        }
        if (getSessionCoordinator() != null) {
            summary += String.format("%s: %s\n",
                    localSettings.SessionCoordinator,
                    getSessionCoordinator().getFullName());
        }
        summary += String.format("Frequency: every %s %s\n",getFrequency(),getFrequencyType());
        summary += String.format("Address:\n%s\n%s\n", getGroupAddress(), getGroupPostcode());
        return summary;
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action) {
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance();
        Group previousItem = (Group) localDB.getListItemByRecordID(previousRecordID);
        Group thisItem = (Group) localDB.getListItemByRecordID(thisRecordID);
        String changes = ListItem.getChanges(previousItem, thisItem);
        changes += CRISUtil.getChanges(previousItem.getKeyWorker(), thisItem.getKeyWorker(), localSettings.Keyworker);
        changes += CRISUtil.getChanges(previousItem.getSessionCoordinator(), thisItem.getSessionCoordinator(), localSettings.SessionCoordinator);
        changes += CRISUtil.getChanges(previousItem.getGroupAddress(), thisItem.getGroupAddress(), "Address");
        changes += CRISUtil.getChanges(previousItem.getFrequency(), thisItem.getFrequency(), "Frequency");
        changes += CRISUtil.getChanges(previousItem.getFrequencyType(), thisItem.getFrequencyType(), "Frequency Type");
        changes += CRISUtil.getChanges(previousItem.getGroupPostcode(), thisItem.getGroupPostcode(), "Postcode");
        if (changes.length() == 0) {
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
    }
}