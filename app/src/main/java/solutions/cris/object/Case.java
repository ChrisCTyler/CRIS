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
import solutions.cris.utils.LocalSettings;

/**
 * Copyright CRIS.Solutions 01/12/2016.
 */

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

    public void clear(){
        setTier(null);
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

    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        // Use local settings for 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance();
        // Build the string
        String summary = "Case Type: " + getCaseType() + "\n";
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
        if (group != null) {
            summary += getGroup().getItemValue();
        }
        if (isDoNotInviteFlag()){
            summary += " (Do not invite to Sessions)";
        }
        summary += "\n";
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

    private static List<Object> getExportFieldNames(Activity activity) {
        final LocalSettings localSettings = LocalSettings.getInstance(activity);
        List<Object> fNames = new ArrayList<>();
        fNames.add("Firstnames");
        fNames.add("Lastname");
        fNames.add("Date of Birth");
        fNames.add("Age");
        fNames.add("Postcode");
        fNames.add("Case Date");
        fNames.add("Case Type");
        fNames.add("Status");
        fNames.add(localSettings.Group);
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
                                .setStartColumnIndex(5)
                                .setEndColumnIndex(6)
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
