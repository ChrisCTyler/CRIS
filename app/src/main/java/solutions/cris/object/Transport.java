package solutions.cris.object;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;
import solutions.cris.utils.CRISUtil;
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

public class Transport extends Document implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_TRANSPORT;

    public Transport(User currentUser, UUID clientID){
        super(currentUser, clientID, Document.Transport);
        requiredOutbound = true;
        requiredReturn = true;
        outboundDate = new Date(Long.MIN_VALUE);
        returnDate = new Date(Long.MIN_VALUE);
        fromAddress = "";
        fromPostcode = "";
        toAddress = "";
        toPostcode = "";
        additionalInformation = "";
        usedOutbound = false;
        usedReturn = false;
        booked = false;
    }

    private UUID transportOrganisationID;
    private ListItem transportOrganisation;
    public UUID getTransportOrganisationID() {
        return transportOrganisationID;
    }
    public void setTransportOrganisationID(UUID transportOrganisationID) {this.transportOrganisationID = transportOrganisationID;}
    public TransportOrganisation getTransportOrganisation() {
        if (transportOrganisationID != null && transportOrganisation == null) {
            LocalDB localDB = LocalDB.getInstance();
            transportOrganisation = localDB.getListItem(transportOrganisationID);
        }
        return (TransportOrganisation) transportOrganisation;
    }
    public void setTransportOrganisation(ListItem transportOrganisation) {this.transportOrganisation = transportOrganisation;}

    private boolean requiredOutbound;

    public boolean isRequiredOutbound() {
        return requiredOutbound;
    }

    public void setRequiredOutbound(boolean requiredOutbound) {
        this.requiredOutbound = requiredOutbound;
    }

    private boolean requiredReturn;

    public boolean isRequiredReturn() {
        return requiredReturn;
    }

    public void setRequiredReturn(boolean requiredReturn) {
        this.requiredReturn = requiredReturn;
    }

    private boolean booked;

    public boolean isBooked() {
        return booked;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
    }

    private String fromAddress;
    public String getFromAddress() {
        return fromAddress;
    }
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    private String fromPostcode;
    public String getFromPostcode() {
        return fromPostcode;
    }
    public void setFromPostcode(String fromPostcode) {
        this.fromPostcode = fromPostcode;
    }

    private String toAddress;
    public String getToAddress() {
        return toAddress;
    }
    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    private String toPostcode;
    public String getToPostcode() {
        return toPostcode;
    }
    public void setToPostcode(String toPostcode) {
        this.toPostcode = toPostcode;
    }

    private String additionalInformation;
    public String getAdditionalInformation() {
        return additionalInformation;
    }
    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    //StartDate
    private Date outboundDate;
    public Date getOutboundDate() {
        return outboundDate;
    }
    public void setOutboundDate(Date outboundDate) {
        this.outboundDate = outboundDate;
    }

    //EndDate
    private Date returnDate;
    public Date getReturnDate() {
        return returnDate;
    }
    public void setReturnDate(Date returnDate) {
        this.returnDate = returnDate;
    }

    private boolean usedOutbound;

    public boolean isUsedOutbound() {
        return usedOutbound;
    }

    public void setUsedOutbound(boolean usedOutbound) {
        this.usedOutbound = usedOutbound;
    }

    private boolean usedReturn;

    public boolean isUsedReturn() {
        return usedReturn;
    }

    public void setUsedReturn(boolean usedReturn) {
        this.usedReturn = usedReturn;
    }

    // V1.3 Transport docment may be 'attached' to a session (accessible through the session register
    private UUID sessionID;

    public UUID getSessionID() {
        return sessionID;
    }

    public void setSessionID(UUID sessionID) {
        this.sessionID = sessionID;
    }

    private Session session;

    public Session getSession() {
        if (sessionID == null) {
            session = null;
        } else if (session == null){
            LocalDB localDB = LocalDB.getInstance();
            session = (Session) localDB.getDocument(sessionID);
        }
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void clear(){
        setTransportOrganisation(null);
        setSession(null);
    }

    // Save the document
    public void save(boolean isNewMode) {
        SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);
        SimpleDateFormat sTime = new SimpleDateFormat("HH:mm", Locale.UK);
        LocalDB localDB = LocalDB.getInstance();
        TransportOrganisation transportOrganisation = getTransportOrganisation();
        solutions.cris.object.Session session = getSession();
        clear();

        // Load the Document fields
        if (isRequiredOutbound()) {
            setReferenceDate(getOutboundDate());
        } else {
            setReferenceDate(getReturnDate());
        }
        String summary = String.format("%s -", transportOrganisation.getItemValue());
        if (isRequiredOutbound()) {
            summary += String.format(" Out: %s",sTime.format(getOutboundDate()));
        } else {
            summary += " Out: Not Required.";
        }
        if (isRequiredReturn()){
            if (isRequiredOutbound()) {
                if (CRISUtil.midnightEarlier(getOutboundDate()).getTime() ==
                        CRISUtil.midnightEarlier(getReturnDate()).getTime()){
                    summary += String.format(" Return: %s", sTime.format(getReturnDate()));
                } else {
                    summary += String.format(Locale.UK,  " Return: %s %s",
                            sDate.format(getReturnDate()),
                            sTime.format(getReturnDate()));
                }

            } else {
                summary += String.format(" Return: %s", sTime.format(getReturnDate()));
            }
        } else {
            summary += " Return: Not Required.";
        }
        setSummary(String.format(Locale.UK, "%s", summary));

        localDB.save(this, isNewMode, User.getCurrentUser());

        setTransportOrganisation(transportOrganisation);
        setSession(session);
    }

    public boolean search(String searchText){
        if (searchText.isEmpty()){
            return true;
        } else {
            String text = String.format("%s %s %s %s %s %s",
                    getTransportOrganisation().getItemValue(),
                    getFromAddress(),
                    getFromPostcode(),
                    getToAddress(),
                    getToPostcode(),
                    getAdditionalInformation());
            return text.toLowerCase().contains(searchText.toLowerCase());
        }
    }

    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        // Build the string
        String summary = super.textSummary();
        summary += String.format("%s\n%s\n%s\ntel: %s\nemail: %s\n\n",
                getTransportOrganisation().getItemValue(),
                getTransportOrganisation().getAddress(),
                getTransportOrganisation().getPostcode(),
                getTransportOrganisation().getContactNumber(),
                getTransportOrganisation().getEmailAddress());

        if (isRequiredOutbound()){
            summary += String.format("Outbound: %s\nFrom:\n%s\n%s\nTo:\n%s\n%s\n\n",
                    sDate.format(getOutboundDate()),
                    getFromAddress(),getFromPostcode(),
                    getToAddress(), getToPostcode());
        } else {
            summary += "Outbound: NOT REQUIRED\n\n";
        }
        if (isRequiredReturn()){
            summary += String.format("Return: %s\nFrom:\n%s\n%s\nTo:\n%s\n%s\n\n",
                    sDate.format(getReturnDate()),
                    getToAddress(), getToPostcode(),
                    getFromAddress(),getFromPostcode());
        } else {
            summary += "Return: NOT REQUIRED\n\n";
        }
        if (!getAdditionalInformation().isEmpty()){
            summary += String.format("Additional Information:\n%s\n", getAdditionalInformation());
        }
        return summary;
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action){
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        Transport previousDocument = (Transport) localDB.getDocumentByRecordID(previousRecordID);
        Transport thisDocument = (Transport) localDB.getDocumentByRecordID(thisRecordID);
        String changes = Document.getChanges(previousDocument, thisDocument);
        changes += CRISUtil.getChanges(previousDocument.isBooked(), thisDocument.isBooked(), "Booked");
        changes += CRISUtil.getChanges(previousDocument.isRequiredOutbound(), thisDocument.isRequiredOutbound(), "Required Outbound");
        changes += CRISUtil.getChangesDateTime(previousDocument.getOutboundDate(), thisDocument.getOutboundDate(), "Outbound Date");
        changes += CRISUtil.getChanges(previousDocument.isUsedReturn(), thisDocument.isUsedReturn(), "Used Return");
        changes += CRISUtil.getChanges(previousDocument.isRequiredReturn(), thisDocument.isRequiredReturn(), "Required Return");
        changes += CRISUtil.getChangesDateTime(previousDocument.getReturnDate(), thisDocument.getReturnDate(), "Return Date");
        changes += CRISUtil.getChanges(previousDocument.isUsedReturn(), thisDocument.isUsedReturn(), "Used Return");
        changes += CRISUtil.getChanges(previousDocument.getFromAddress(), thisDocument.getFromAddress(), "From Address");
        changes += CRISUtil.getChanges(previousDocument.getFromPostcode(), thisDocument.getFromPostcode(), "From Postcode");
        changes += CRISUtil.getChanges(previousDocument.getToAddress(), thisDocument.getToAddress(), "To Address");
        changes += CRISUtil.getChanges(previousDocument.getToPostcode(), thisDocument.getToPostcode(), "To Postcode");
        changes += CRISUtil.getChanges(previousDocument.getAdditionalInformation(), thisDocument.getAdditionalInformation(), "Additional Information");
        if (changes.length() == 0){
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
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
                                .setStartIndex(5)
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
        // 10th column is a date
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
                                .setStartColumnIndex(11)
                                .setEndColumnIndex(12)
                                .setStartRowIndex(1))));
        // 13th column is a date
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
                                .setStartColumnIndex(14)
                                .setEndColumnIndex(15)
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
        row.add(getItemValue(getTransportOrganisation()));
        if (isBooked()){
            row.add("True");
        } else {
            row.add("False");
        }
        row.add(getFromPostcode());
        row.add(getToPostcode());
        if (isRequiredOutbound()){
            row.add("True");
            if (getOutboundDate().getTime() != Long.MIN_VALUE) {
                row.add(sDate.format(getOutboundDate()));
            } else {
                row.add("");
            }
            if (isUsedOutbound()){
                row.add("True");
            } else {
                row.add("False");
            }
        } else {
            row.add("False");
            row.add("");
            row.add("");
        }
        if (isRequiredReturn()){
            row.add("True");
            if (getReturnDate().getTime() != Long.MIN_VALUE) {
                row.add(sDate.format(getReturnDate()));
            } else {
                row.add("");
            }
            if (isUsedReturn()){
                row.add("True");
            } else {
                row.add("False");
            }
        } else {
            row.add("False");
            row.add("");
            row.add("");
        }

        return row;

    }

    private String getItemValue(ListItem item){
        if (item == null){
            return "Unknown";
        } else {
            return item.getItemValue();
        }
    }

    private static List<Object> getExportFieldNames() {
        List<Object> fNames = new ArrayList<>();
        fNames.add("Firstnames");
        fNames.add("Lastname");
        fNames.add("Date of Birth");
        fNames.add("Age");
        // Build 139 - Add Year Group to Export
        fNames.add("Year Group");
        fNames.add("Postcode");
        fNames.add("Organisation");
        fNames.add("Booked");
        fNames.add("From Pcode");
        fNames.add("To Pcode");
        fNames.add("Outbound");
        fNames.add("Date");
        fNames.add("Used");
        fNames.add("Return");
        fNames.add("Date");
        fNames.add("Used");
        return fNames;
    }

    public static List<List<Object>> getTransportData(ArrayList<Document> documents) {
        LocalDB localDB = LocalDB.getInstance();
        Client client = null;
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames());
        for (Document document : documents) {
            if (client == null || document.getClientID() != client.getClientID()) {
                // New client
                client = (Client) localDB.getDocument(document.getClientID());
            }
            Transport thisDocument = (Transport) document;
            content.add(thisDocument.getExportData(client));
        }
        return content;
    }
}
