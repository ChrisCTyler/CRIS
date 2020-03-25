package solutions.cris.object;

import android.app.Activity;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.DimensionProperties;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.NumberFormat;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;
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

public class Session extends Document implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_SESSION;


    public Session(User currentUser) {
        super(currentUser, Document.nonClientDocumentID, Document.Session);
        keyWorkerID = null;
        sessionCoordinatorID = null;
        address = "";
        postcode = "";
        additionalInformation = "";
        otherStaffIDList = new ArrayList<>();
    }

    private UUID groupID;

    public UUID getGroupID() {
        return groupID;
    }

    public void setGroupID(UUID groupID) {
        this.groupID = groupID;
    }

    private Group group;

    public Group getGroup() {
        if (groupID != null && group == null) {
            if (groupID.equals(Group.adHocGroupID)) {
                group = Group.getAdHocGroup();
            } else {
                LocalDB localDB = LocalDB.getInstance();
                group = (Group) localDB.getListItem(groupID);
            }
        }
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    private String sessionName;

    public String getSessionName() {
        // this.group may nit have been initialised for instantiate a local group
        // to force a call of getGroup
        Group group = getGroup();
        if (group.getListItemID().equals(Group.adHocGroupID)) return sessionName;
        else return group.getItemValue();
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    // KeyWorker
    private UUID keyWorkerID;

    public UUID getKeyWorkerID() {
        return keyWorkerID;
    }

    public void setKeyWorkerID(UUID keyWorkerID) {
        this.keyWorkerID = keyWorkerID;
    }

    private User keyWorker;

    public User getKeyWorker() {
        if (keyWorkerID != null && keyWorker == null) {
            LocalDB localDB = LocalDB.getInstance();
            keyWorker = localDB.getUser(keyWorkerID);
        }
        return keyWorker;
    }

    public void setKeyWorker(User keyWorker) {
        this.keyWorker = keyWorker;
    }

    // Session Coordinator
    private UUID sessionCoordinatorID;

    public UUID getSessionCoordinatorID() {
        return sessionCoordinatorID;
    }

    public void setSessionCoordinatorID(UUID sessionCoordinatorID) {
        this.sessionCoordinatorID = sessionCoordinatorID;
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

    private ArrayList<UUID> otherStaffIDList;

    public ArrayList<UUID> getOtherStaffIDList() {
        return otherStaffIDList;
    }

    public void addOtherStaffID(UUID otherStaffID) {
        this.otherStaffIDList.add(otherStaffID);
    }

    public void clearOtherStaffIDList() {
        this.otherStaffIDList = new ArrayList<>();
    }

    // Address
    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    //Postcode
    private String postcode;

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public static Comparator<Session> comparatorDate = new Comparator<Session>() {
        @Override
        public int compare(Session o1, Session o2) {
            int compare = o2.getReferenceDate().compareTo(o1.getReferenceDate());
            if (compare == 0)
                compare = o2.getSessionName().compareTo(o1.getSessionName());
            return compare;
        }
    };

    private int duration;

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    private String additionalInformation;

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public void clear(){
        setGroup(null);
        setKeyWorker(null);
        setSessionCoordinator(null);
    }

    // Save the document
    public void save(boolean isNewMode) {
        LocalDB localDB = LocalDB.getInstance();
        // Save the groupID to pass to createClientSedssionDocuments in case the user
        // has changed the Session Name and this becomes an AdHoc session
        UUID groupID = getGroupID();
        Group group = getGroup();
        User keyworker = getKeyWorker();
        User sessionCoordinator = getSessionCoordinator();
        clear();

        // Load the Document fields
        setSummary(String.format("%s (%s)",
                sessionCoordinator.getFullName(),
                sessionCoordinator.getContactNumber()));

        // v2.0.084 4 Oct 2017 If the name has been changed set the group to ad-hoc
        if (!sessionName.toString().trim().equals(group.getItemValue().trim())) {
            // Session Name has been changed
            setGroupID(Group.adHocGroupID);
            group = Group.getAdHocGroup();
        }

        localDB.save(this, isNewMode, User.getCurrentUser());

        setGroup(group);
        setKeyWorker(keyworker);
        setSessionCoordinator(sessionCoordinator);

        // Create the ClientSession documents
        if (isNewMode && !groupID.equals(Group.adHocGroupID)) {
            createClientSessions(localDB, groupID);
        }
    }

    private void createClientSessions(LocalDB localDB, UUID groupID) {
        // Get the list of clients
        ArrayList<Client> clients = localDB.getAllClients();
        User currentUser = User.getCurrentUser();
        for (Client client : clients) {
            // Build 139 - Second Group, Test both groups when deciding whether to create Client Session
            /*
            if (client.getCurrentCase() != null && client.getCurrentCase().getGroupID() != null) {
                //if (client.getCurrentCase().getGroupID().equals(getGroupID()) &&
                // Use original groupID in case user has changed the name, creating AdHoc Session
                if (client.getCurrentCase().getGroupID().equals(groupID) &&
                        (client.getCurrentCase().getCaseType().equals("Start") ||
                                client.getCurrentCase().getCaseType().equals("Update"))) {
                    // Build 105
                    if (!client.getCurrentCase().isDoNotInviteFlag()) {
                        ClientSession clientSession = new ClientSession(currentUser, client.getClientID());
                        clientSession.setSessionID(getDocumentID());
                        clientSession.setReferenceDate(getReferenceDate());
                        clientSession.setSummary(getSessionName());
                        clientSession.setSession(this);
                        clientSession.save(true);
                    }
                }
            }
            */
            boolean addSession = false;
            if (client.getCurrentCase() != null) {
                if (client.getCurrentCase().getCaseType().equals("Start") ||
                    client.getCurrentCase().getCaseType().equals("Update")){
                    if (client.getCurrentCase().getGroupID().equals(groupID) &&
                            !client.getCurrentCase().isDoNotInviteFlag()) {
                        addSession = true;
                    } else if (client.getCurrentCase().getGroup2ID() != null){
                        if (client.getCurrentCase().getGroup2ID().equals(groupID) &&
                                !client.getCurrentCase().isDoNotInvite2Flag()) {
                            addSession = true;
                        }
                    }
                }
            }
            if (addSession){
                ClientSession clientSession = new ClientSession(currentUser, client.getClientID());
                clientSession.setSessionID(getDocumentID());
                clientSession.setReferenceDate(getReferenceDate());
                clientSession.setSummary(getSessionName());
                clientSession.setSession(this);
                clientSession.save(true);
            }
        }
    }

    public static Comparator<Session> comparatorAZ = new Comparator<Session>() {
        @Override
        public int compare(Session o1, Session o2) {
            int compare = o1.getSessionName().compareTo(o2.getSessionName());
            if (compare == 0)
                compare = o2.getReferenceDate().compareTo(o1.getReferenceDate());
            return compare;
        }
    };

    @Override
    public boolean search(String searchText) {
        if (searchText.isEmpty()) {
            return true;
        } else {
            String text = String.format("%s %s %s %s %s %s",
                    getSessionName(),
                    getAddress(), getPostcode(),
                    getKeyWorker().getFullName(),
                    getSessionCoordinator().getFullName(),
                    getAdditionalInformation());
            return text.toLowerCase().contains(searchText.toLowerCase());
        }
    }

    @Override
    public String textSummary() {
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance();
        String summary = String.format("Session: %s\n", getSessionName());
        summary += String.format("Address:\n%s\n%s\n", getAddress(), getPostcode());
        String dateString = "";
        if (getReferenceDate().getTime() != Long.MIN_VALUE) {
            dateString = sDateTime.format(getReferenceDate());
        }
        summary += String.format("Date: %s\n", dateString);
        summary += String.format("Session Coordinator: %s\n", getSessionCoordinator().getFullName());
        summary += String.format("%s: %s\n", localSettings.Keyworker, getKeyWorker().getFullName());
        summary += String.format("%s: %s\n", localSettings.OtherStaff, getOtherStaffString());
        if (getAdditionalInformation().length() > 0) {
            summary += String.format("Additional Information:\n%s\n", getAdditionalInformation());
        }
        summary += String.format(Locale.UK, "Duration: %d minutes:\n", getDuration());
        return summary;
    }

    private static List<Object> getExportFieldNames(Activity activity) {
        final LocalSettings localSettings = LocalSettings.getInstance(activity);
        List<Object> fNames = new ArrayList<>();
        fNames.add(localSettings.Group);
        fNames.add("Session");
        fNames.add(localSettings.SessionCoordinator);
        fNames.add(localSettings.Keyworker);
        // 26 Oct 2017 Build 090 Added Other Staff to export
        fNames.add(localSettings.OtherStaff);
        fNames.add("Address");
        fNames.add("Postcode");
        fNames.add("Duration (Minutes)");
        return fNames;
    }

    public static List<List<Object>> getSessionData(ArrayList<Session> sessions, Activity activity) {
        LocalDB localDB = LocalDB.getInstance();
        Client client = null;
        // Build the 1st row
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames(activity));
        // Add each document (needs associated note/pdf/transport documents)
        for (Session session : sessions) {
            content.add(session.getExportData());
        }
        return content;
    }


    public static List<Request> getExportSheetConfiguration(int sheetID) {
        List<Request> requests = new ArrayList<>();
        // General
        requests.add(new Request()
                .setRepeatCell(new RepeatCellRequest()
                        .setCell(new CellData()
                                .setUserEnteredFormat(new CellFormat()
                                        .setTextFormat(new TextFormat()
                                                .setFontSize(11))))
                        .setFields("UserEnteredFormat")
                        .setRange(new GridRange()
                                .setSheetId(sheetID)
                                .setStartColumnIndex(0)
                                .setStartRowIndex(0))));
        // Set some Cell dimensions
        requests.add(new Request()
                .setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                        .setFields("pixelSize")
                        .setProperties(new DimensionProperties()
                                .setPixelSize(150))
                        .setRange(new DimensionRange()
                                .setSheetId(sheetID)
                                .setDimension("COLUMNS")
                                .setStartIndex(0)
                                .setEndIndex(6))
                ));
        // 1st row is bold/Centered
        requests.add(new Request()
                .setRepeatCell(new RepeatCellRequest()
                        .setCell(new CellData()
                                .setUserEnteredFormat(new CellFormat()
                                        .setHorizontalAlignment("CENTER")
                                        .setWrapStrategy("WRAP")
                                        .setTextFormat(new TextFormat()
                                                .setBold(true)
                                                .setFontSize(11))))
                        .setFields("UserEnteredFormat")
                        .setRange(new GridRange()
                                .setSheetId(sheetID)
                                .setStartColumnIndex(0)
                                .setStartRowIndex(0)
                                .setEndRowIndex(1))));
        return requests;
    }

    public List<Object> getExportData() {
        LocalDB localDB = LocalDB.getInstance();
        SimpleDateFormat sDate = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        List<Object> row = new ArrayList<>();
        row.add(getItemValue(getGroup()));
        row.add(getSessionName());
        row.add(getFullName(getSessionCoordinator()));
        row.add(getFullName(getKeyWorker()));
        // 26 Oct 2017 Build 090 Added Other Staff to export
        row.add(getOtherStaffString());
        row.add(getAddress());
        row.add(getPostcode());
        row.add(getDuration());
        return row;
    }

    private String getOtherStaffString(){
        String otherStaffString = "";
        LocalDB localDB = LocalDB.getInstance();
        if (otherStaffIDList != null && otherStaffIDList.size()>0) {
            for (UUID otherStaffID : otherStaffIDList) {
                User otherStaff = localDB.getUser(otherStaffID);
                if (otherStaff != null) {
                    otherStaffString += otherStaff.getFullName() + ", ";
                }
            }
            if (otherStaffString.length() > 2) {
                otherStaffString = otherStaffString.substring(0, otherStaffString.length() - 2);
            } else {
                otherStaffString = "None";
            }
        }
        return otherStaffString;
    }

    private String getItemValue(ListItem item) {
        if (item == null) {
            return "Unknown";
        } else {
            return item.getItemValue();
        }
    }

    private String getFullName(User user) {
        if (user == null) {
            return "Unknown";
        } else {
            return user.getFullName();
        }
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action){
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        LocalSettings localSettings = LocalSettings.getInstance();

        Session previousDocument = (Session) localDB.getDocumentByRecordID(previousRecordID);
        Session thisDocument = (Session) localDB.getDocumentByRecordID(thisRecordID);
        String changes = Document.getChanges(previousDocument, thisDocument);
        changes += CRISUtil.getChanges(previousDocument.getGroup(), thisDocument.getGroup(), localSettings.Group);
        changes += CRISUtil.getChanges(previousDocument.getSessionName(), thisDocument.getSessionName(), "Session Name");
        changes += CRISUtil.getChanges(previousDocument.getKeyWorker(), thisDocument.getKeyWorker(), localSettings.Keyworker);
        changes += CRISUtil.getChanges(previousDocument.getSessionCoordinator(), thisDocument.getSessionCoordinator(), "Session Coordinator");
        changes += CRISUtil.getChanges(previousDocument.getOtherStaffIDList(), thisDocument.getOtherStaffIDList(), "Other Staff");

        changes += CRISUtil.getChanges(previousDocument.getAddress(), thisDocument.getAddress(), "Address");
        changes += CRISUtil.getChanges(previousDocument.getPostcode(), thisDocument.getPostcode(), "Postcode");
        changes += CRISUtil.getChanges(previousDocument.getDuration(), thisDocument.getDuration(), "Duration");
        changes += CRISUtil.getChanges(previousDocument.getAdditionalInformation(), thisDocument.getAdditionalInformation(), "Additional Information");
        if (changes.length() == 0){
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
    }
}
