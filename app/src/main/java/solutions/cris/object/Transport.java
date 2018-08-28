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

/**
 * Copyright CRIS Solutions on 26/01/2017.
 */

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
        String summary = String.format("%s\n%s\n%s\ntel: %s\nemail: %s\n\n",
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
                                .setStartColumnIndex(10)
                                .setEndColumnIndex(11)
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
                                .setStartColumnIndex(13)
                                .setEndColumnIndex(14)
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
