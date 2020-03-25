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
import java.util.List;
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

public class Case extends Document implements Serializable {

    public final static int RED = 0;
    public final static int AMBER = 1;
    public final static int GREEN = 2;

    public final static String[] caseTypes = {"Start", "Update", "Close", "Reject"};

    public final static String[] transportRequirements = {"No", "Sometimes", "Always"};

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_CASE;

    public Case(User currentUser, UUID clientID) {
        super(currentUser, clientID, Document.Case);
    }

    private String caseType;

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        boolean found = false;
        for (String item : caseTypes) {
            if (item.equals(caseType)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new CRISException(String.format("Invalid Case Type: %s", caseType));
        }
        this.caseType = caseType;
    }

    private int clientStatus;

    public int getClientStatus() {
        return clientStatus;
    }

    public void setClientStatus(int clientStatus) {
        if (clientStatus < RED || clientStatus > GREEN) {
            throw new CRISException(String.format(Locale.UK, "Invalid Client Status: %d", clientStatus));
        }
        this.clientStatus = clientStatus;
    }

    public static String getClientStatusString(int clientStatus){
        switch (clientStatus){
            case RED:
                return "RED";

            case AMBER:
                return "AMBER";

            case GREEN:
                return "GREEN";

            default:
                return String.format("No such Client Status: %d\n", clientStatus);
        }
    }

    private UUID keyWorkerID;
    private User keyWorker;

    public UUID getKeyWorkerID() {
        return keyWorkerID;
    }

    public void setKeyWorkerID(UUID keyWorkerID) {
        this.keyWorkerID = keyWorkerID;
    }

    public User getKeyWorker() {
        if (keyWorkerID != null  && keyWorker == null) {
            LocalDB localDB = LocalDB.getInstance();
            keyWorker = (localDB.getUser(keyWorkerID));
        }
        return keyWorker;
    }

    public void setKeyWorker(User keyWorker) {
        this.keyWorker = keyWorker;
    }

    private UUID coWorker1ID;
    private User coWorker1;

    public UUID getCoWorker1ID() {
        return coWorker1ID;
    }

    public void setCoWorker1ID(UUID coWorker1ID) {
        this.coWorker1ID = coWorker1ID;
    }

    public User getCoWorker1() {
        if (coWorker1ID != null) {
            LocalDB localDB = LocalDB.getInstance();
            coWorker1 = localDB.getUser(coWorker1ID);
        }
        return coWorker1;
    }

    public void setCoWorker1(User coWorker1) {
        this.coWorker1 = coWorker1;
    }

    private UUID coWorker2ID;
    private User coWorker2;

    public UUID getCoWorker2ID() {
        return coWorker2ID;
    }

    public void setCoWorker2ID(UUID coWorker2ID) {
        this.coWorker2ID = coWorker2ID;
    }

    public User getCoWorker2() {
        if (coWorker2ID!= null) {
            LocalDB localDB = LocalDB.getInstance();
            coWorker2 = localDB.getUser(coWorker2ID);
        }
        return coWorker2;
    }

    public void setCoWorker2(User coWorker2) {
        this.coWorker2 = coWorker2;
    }

    private UUID tierID;
    private ListItem tier;

    public UUID getTierID() {
        return tierID;
    }

    public void setTierID(UUID tierID) {
        this.tierID = tierID;
    }

    public ListItem getTier() {
        if (tierID != null && tier == null) {
            LocalDB localDB = LocalDB.getInstance();
            tier = localDB.getListItem(tierID);
        }
        return tier;
    }

    public void setTier(ListItem tier) {
        this.tier = tier;
    }

    private UUID groupID;
    private ListItem group;

    public UUID getGroupID() {
        return groupID;
    }

    public void setGroupID(UUID groupID) {
        this.groupID = groupID;
    }

    public ListItem getGroup() {
        if (groupID != null) {
            LocalDB localDB = LocalDB.getInstance();
            group = localDB.getListItem(groupID);
        }
        return group;
    }
    public void setGroup(ListItem group) {
        this.group = group;
    }

    // Build 139 - Second Group
    private UUID group2ID;
    private ListItem group2;

    public UUID getGroup2ID() {
        return group2ID;
    }

    public void setGroup2ID(UUID group2ID) {
        this.group2ID = group2ID;
    }

    public ListItem getGroup2() {
        if (group2ID != null) {
            LocalDB localDB = LocalDB.getInstance();
            group2 = localDB.getListItem(group2ID);
        }
        return group2;
    }
    public void setGroup2(ListItem group2) {
        this.group2 = group2;
    }


    private UUID commissionerID;
    private ListItem commissioner;

    public UUID getCommissionerID() {
        return commissionerID;
    }

    public void setCommissionerID(UUID commissionerID) {
        this.commissionerID = commissionerID;
    }

    public ListItem getCommissioner() {
        if (commissionerID != null) {
            LocalDB localDB = LocalDB.getInstance();
            commissioner = localDB.getListItem(commissionerID);
        }
        return commissioner;
    }

    public void setCommissioner(ListItem commissioner) {
        this.commissioner = commissioner;
    }

    private int overdueThreshold;

    public int getOverdueThreshold() {
        return overdueThreshold;
    }

    public void setOverdueThreshold(int overdueThreshold) {
        this.overdueThreshold = overdueThreshold;
    }

    // Added in V1.3.061
    private String transportRequired;

    public String getTransportRequired() {
        if (transportRequired == null || transportRequired.isEmpty()) {
            return transportRequirements[0];
        } else return transportRequired;
    }

    public void setTransportRequired(String transportRequired) {
        boolean found = false;
        for (String item : transportRequirements) {
            if (item.equals(transportRequired)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new CRISException(String.format("Invalid Transport Require option: %s", transportRequired));
        }
        this.transportRequired = transportRequired;
    }

    private String transportSpecialInstructions;

    public String getTransportSpecialInstructions() {
        if (transportSpecialInstructions == null) return "";
        else return transportSpecialInstructions;
    }

    public void setTransportSpecialInstructions(String transportSpecialInstructions) {
        this.transportSpecialInstructions = transportSpecialInstructions;
    }

    // Added in 2.0.84
    private String caseSummary;

    public String getCaseSummary() {
        if (caseSummary == null){
            caseSummary = "";
        }
        return caseSummary;
    }

    public void setCaseSummary(String summary) {
        this.caseSummary = summary;
    }

    // Build 105 - Two new checkboxes
    private boolean photographyConsentFlag;
    public boolean isPhotographyConsentFlag() {return photographyConsentFlag;}
    public void setPhotographyConsentFlag(boolean photographyConsentFlag) {
        this.photographyConsentFlag = photographyConsentFlag;
    }

    private boolean doNotInviteFlag;
    public boolean isDoNotInviteFlag() {return doNotInviteFlag;}
    public void setDoNotInviteFlag(boolean doNotInviteFlag) {
        this.doNotInviteFlag = doNotInviteFlag;
    }

    // Build 139 - Second Group
    private boolean doNotInvite2Flag;
    public boolean isDoNotInvite2Flag() {return doNotInvite2Flag;}
    public void setDoNotInvite2Flag(boolean doNotInvite2Flag) {
        this.doNotInvite2Flag = doNotInvite2Flag;
    }

    public void clear(){
        setTier(null);
        setGroup(null);
        // Build 139 - Second Group
        setGroup(null);
        setKeyWorker(null);
        setCoWorker1(null);
        setCoWorker2(null);
        setCommissioner(null);
    }

    public void save(boolean isNewMode) {
        LocalDB localDB = LocalDB.getInstance();
        ListItem tier = getTier();
        ListItem group = getGroup();
        ListItem group2 = getGroup2();
        // Build 139 Second Group
        User keyWorker = getKeyWorker();
        User coWorker1 = getCoWorker1();
        User coWorker2 = getCoWorker2();
        ListItem commissioner = getCommissioner();
        clear();

        // Load the Document fields
        String summaryText = "";
        if (tier != null) {
            summaryText += tier.getItemValue();
        }
        if (group != null) {
            if (!summaryText.isEmpty()){
                summaryText += ", ";
            }
            summaryText += group.getItemValue();
        }
        // Build 139 - Second Group
        if (group2 != null) {
            if (!summaryText.isEmpty()){
                summaryText += ", ";
            }
            summaryText += " plus 1";
        }
        if (keyWorker != null) {
            if (!summaryText.isEmpty()){
                summaryText += ", ";
            }
            summaryText += String.format("%s,(%s)",
                    keyWorker.getFullName(),
                    keyWorker.getContactNumber());
        }
        setSummary(summaryText);

        localDB.save(this, isNewMode, User.getCurrentUser());

        setTier(tier);
        setGroup(group);
        // Build 139 - Second Group
        setGroup2(group2);
        setKeyWorker(keyWorker);
        setCoWorker1(coWorker1);
        setCoWorker2(coWorker2);
        setCommissioner(commissioner);

        // If keyworker was not following this client, setFollow
        if (getKeyWorkerID() != null) {
            if (!localDB.isFollowing(getKeyWorkerID(), getClientID())) {
                localDB.setFollow(getKeyWorkerID(), getClientID(), true);
            }
        }
    }

    public boolean search(String searchText) {
        if (searchText.isEmpty()) {
            return true;
        } else {
            String text = String.format("%s %s %s ",
                    getCaseType(), getTransportRequired(), getTransportSpecialInstructions());
            if (getTier() != null) {
                text += getTier().getItemValue() + " ";
            }
            if (getGroup() != null) {
                text += getGroup().getItemValue() + " ";
            }
            // Build 139 - Second Group
            if (getGroup2() != null) {
                text += getGroup2().getItemValue() + " ";
            }
            switch (getClientStatus()) {
                case RED:
                    text += "red ";
                    break;
                case AMBER:
                    text += "amber orange";
                    break;
                case GREEN:
                    text += "green ";
                    break;
            }
            if (getKeyWorker() != null) {
                text += getKeyWorker().getFullName() + " ";
            }
            if (getCoWorker1() != null) {
                text += getCoWorker1().getFullName() + " ";
            }
            if (getCoWorker2() != null) {
                text += getCoWorker2().getFullName() + " ";
            }
            if (getCommissioner() != null) {
                text += getCommissioner().getItemValue() + " ";
            }
            return text.toLowerCase().contains(searchText.toLowerCase());
        }
    }

    @Override
    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance();
        // Build the string
        String summary = super.textSummary();
        summary += "Case Type: " + getCaseType() + "\n";
        summary += "Date: ";
        if (getReferenceDate().getTime() != Long.MIN_VALUE) {
            summary += sDate.format(getReferenceDate());
        }
        summary += "\n";
        // Build 103 - Added Case Summary
        if (caseSummary != null){
            summary += String.format("Case Summary: %s\n", caseSummary);
        }
        switch (getClientStatus()) {
            case RED:
                summary += "Status: RED\n";
                break;
            case AMBER:
                summary += "Status: AMBER\n";
                break;
            case GREEN:
                summary += "Status: GREEN\n";
        }
        if (isPhotographyConsentFlag()){
            summary += "Photography/Media Consent: Yes\n";
        } else {
            summary += "Photography/Media Consent: No\n";
        }
        summary += String.format(Locale.UK, "Overdue Threshold: %d\n", getOverdueThreshold());
        summary += localSettings.Tier + ": ";
        if (tier != null) {
            summary += getTier().getItemValue();
        }
        summary += "\n";
        summary += localSettings.Group + ": ";
        if (getGroup() != null) {
            summary += getGroup().getItemValue();
            if (isDoNotInviteFlag()){
                summary += " (Do not invite to Sessions)";
            }
        }
        summary += "\n";
        // Build 139 - Second Group
        if (getGroup2() != null) {
            summary += localSettings.Group + "2: ";
            summary += getGroup2().getItemValue();
            if (isDoNotInvite2Flag()){
                summary += " (Do not invite to Sessions)";
            }
            summary += "\n";
        }
        summary += "Transport Required: " + getTransportRequired() + "\n";
        summary += "Special Instructions: " + getTransportSpecialInstructions() + "\n";
        summary += localSettings.Keyworker + ": ";
        if (getKeyWorker() != null) {
            summary += getKeyWorker().getFullName();
        }
        summary += "\n";
        summary += localSettings.Coworker1 + ": ";
        if (getCoWorker1() != null) {
            summary += getCoWorker1().getFullName();
        }
        summary += "\n";
        summary += localSettings.Coworker2 + ": ";
        if (getCoWorker2() != null) {
            summary += getCoWorker2().getFullName();
        }
        summary += "\n";
        summary += localSettings.Commisioner + ": ";
        if (getCommissioner() != null) {
            summary += getCommissioner().getItemValue();
        }
        summary += "\n";
        return summary;
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action){
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        LocalSettings localSettings = LocalSettings.getInstance();

        Case previousDocument = (Case) localDB.getDocumentByRecordID(previousRecordID);
        Case thisDocument = (Case) localDB.getDocumentByRecordID(thisRecordID);
        String changes = Document.getChanges(previousDocument, thisDocument);
        changes += CRISUtil.getChanges(previousDocument.getCaseType(), thisDocument.getCaseType(), "Case Type");
        if (previousDocument.getClientStatus() != thisDocument.getClientStatus()){
            changes += String.format("Client Status changed from %s to %s\n",
                    getClientStatusString (previousDocument.getClientStatus()),
                    getClientStatusString(thisDocument.getClientStatus()));
        }
        changes += CRISUtil.getChanges(previousDocument.getCaseSummary(), thisDocument.getCaseSummary(), "Case Summary");
        changes += CRISUtil.getChanges(previousDocument.isPhotographyConsentFlag(), thisDocument.isPhotographyConsentFlag(), "Photography/Media Consent");
        changes += CRISUtil.getChanges(previousDocument.getOverdueThreshold(), thisDocument.getOverdueThreshold(), "Overdue Days Threshold");
        changes += CRISUtil.getChanges(previousDocument.getTier(), thisDocument.getTier(), localSettings.Tier);
        changes += CRISUtil.getChanges(previousDocument.getGroup(), thisDocument.getGroup(), localSettings.Group);
        // Build 139 - Second Group
        changes += CRISUtil.getChanges(previousDocument.getGroup2(), thisDocument.getGroup2(), localSettings.Group +"2");
        changes += CRISUtil.getChanges(previousDocument.isDoNotInviteFlag(), thisDocument.isDoNotInviteFlag(), "Do Not Invite flag");
        changes += CRISUtil.getChanges(previousDocument.getTransportRequired(), thisDocument.getTransportRequired(), "Transport Required");
        changes += CRISUtil.getChanges(previousDocument.getTransportSpecialInstructions(), thisDocument.getTransportSpecialInstructions(), "Special Transport Instructions");
        changes += CRISUtil.getChanges(previousDocument.getKeyWorker(), thisDocument.getKeyWorker(), localSettings.Keyworker);
        changes += CRISUtil.getChanges(previousDocument.getCoWorker1(), thisDocument.getCoWorker1(), localSettings.Coworker1);
        changes += CRISUtil.getChanges(previousDocument.getCoWorker2(), thisDocument.getCoWorker2(), localSettings.Coworker2);
        changes += CRISUtil.getChanges(previousDocument.getCommissioner(), thisDocument.getCommissioner(), localSettings.Commisioner);
        if (changes.length() == 0){
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
    }

    private static List<Object> getExportFieldNames(Activity activity) {
        final LocalSettings localSettings = LocalSettings.getInstance(activity);
        List<Object> fNames = new ArrayList<>();
        fNames.add("Firstnames");
        fNames.add("Lastname");
        fNames.add("Date of Birth");
        fNames.add("Age");
        // Build 139 - Add Year Group to Export
        fNames.add("Year Group");
        fNames.add("Postcode");
        fNames.add("Case Date");
        fNames.add("Case Type");
        fNames.add("Status");
        fNames.add(localSettings.Group);
        // Build 139 - Second Group
        fNames.add(localSettings.Group + "2");
        fNames.add(localSettings.Keyworker);
        fNames.add(localSettings.Commisioner);
        fNames.add(localSettings.Tier);
        fNames.add("Transport Required");
        return fNames;
    }

    public static List<List<Object>> getCaseData(ArrayList<Document> documents, Activity activity) {
        LocalDB localDB = LocalDB.getInstance();
        Client client = null;
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames(activity));
        for (Document document : documents) {
            if (client == null || document.getClientID() != client.getClientID()){
                // New client
                client = (Client) localDB.getDocument(document.getClientID());
            }
            Case thisDocument = (Case) document;
            content.add(thisDocument.getExportData(client));
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
                                .setStartIndex(8)
                                .setEndIndex(10))
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
        // 3rd column is a date
        requests.add(new Request()
                .setRepeatCell(new RepeatCellRequest()
                        .setCell(new CellData()
                                .setUserEnteredFormat(new CellFormat()
                                        .setTextFormat(new TextFormat()
                                                .setFontSize(11))
                                        .setNumberFormat(new NumberFormat()
                                                .setType("DATE"))
                                ))
                        .setFields("UserEnteredFormat")
                        .setRange(new GridRange()
                                .setSheetId(sheetID)
                                .setStartColumnIndex(2)
                                .setEndColumnIndex(3)
                                .setStartRowIndex(1))));
        // 6th column is a date
        // Build 139 - Adding Year Group to Export shifts column to right
        requests.add(new Request()
                .setRepeatCell(new RepeatCellRequest()
                        .setCell(new CellData()
                                .setUserEnteredFormat(new CellFormat()
                                        .setTextFormat(new TextFormat()
                                                .setFontSize(11))
                                        .setNumberFormat(new NumberFormat()
                                                .setType("DATE"))
                                ))
                        .setFields("UserEnteredFormat")
                        .setRange(new GridRange()
                                .setSheetId(sheetID)
                                // Build 139 - Adding Year Group to Export shifts column to right
                                .setStartColumnIndex(6)
                                .setEndColumnIndex(7)
                                .setStartRowIndex(1))));
        return requests;
    }

    public List<Object> getExportData(Client client) {
        SimpleDateFormat sDate = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        List<Object> row = new ArrayList<>();
        row.add(client.getFirstNames());
        row.add(client.getLastName());
        row.add(sDate.format(client.getDateOfBirth()));
        row.add(client.getAge());
        // Build 139 - Add Year Group to Export
        row.add(client.getYearGroup());
        row.add(client.getPostcode());
        row.add(sDate.format(getReferenceDate()));
        row.add(getCaseType());

        switch (clientStatus) {
            case RED:
                row.add("red");
                break;
            case AMBER:
                row.add("amber");
                break;
            case GREEN:
                row.add("green");
                break;
        }
        if (getGroup() != null) {
            row.add(getGroup().getItemValue());
        } else {
            row.add("");    // Group
        }
        // Build 139 - Second Group
        if (getGroup2() != null) {
            row.add(getGroup2().getItemValue());
        } else {
            row.add("");    // Group2
        }
        if (getKeyWorker() != null) {
            row.add(getKeyWorker().getFullName());
        } else {
            row.add("");    // Keyworker
        }
        if (getCommissioner() != null) {
            row.add(getCommissioner().getItemValue());
        } else {
            row.add("");    // Commissioner
        }
        if (tier != null) {
            row.add(getTier().getItemValue());
        } else {
            row.add("");    // Tier
        }
        row.add(getTransportRequired());
        return row;

    }
}
