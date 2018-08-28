package solutions.cris.object;

import java.io.Serializable;
import java.util.UUID;

import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;

/**
 * Created by Chris Tyler on 20/02/2017.
 */

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
}