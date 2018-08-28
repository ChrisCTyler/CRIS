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
import solutions.cris.list.ListClientHeader;

/**
 * Copyright CRIS.Solutions 27/01/2017.
 */

public class Contact extends Document implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_CONTACT;

    public Contact(User currentUser, UUID clientID) {
        super(currentUser, clientID, Document.Contact);
        contactTypeID = null;
        contactType = null;
        agencyID = null;
        agency = null;
        schoolID = null;
        school = null;
        startDate = new Date(Long.MIN_VALUE);
        endDate = new Date(Long.MIN_VALUE);
        contactName = "";
        contactAddress = "";
        contactPostcode = "";
        contactContactNumber = "";
        contactEmailAddress = "";
        contactAdditionalInformation = "";
        relationshipTypeID = null;
        relationshipType = null;
    }

    private UUID contactTypeID;
    private ListItem contactType;

    public UUID getContactTypeID() {
        return contactTypeID;
    }

    public void setContactTypeID(UUID contactTypeID) {
        this.contactTypeID = contactTypeID;
    }

    public ListItem getContactType() {
        if (contactTypeID != null && contactType == null) {
            LocalDB localDB = LocalDB.getInstance();
            contactType = localDB.getListItem(contactTypeID);
        }
        return contactType;
    }

    public void setContactType(ListItem contactType) {
        this.contactType = contactType;
    }

    private UUID agencyID;
    private ListItem agency;

    public UUID getAgencyID() {
        return agencyID;
    }

    public void setAgencyID(UUID agencyID) {
        this.agencyID = agencyID;
    }

    public Agency getAgency() {
        if (agencyID != null && agency == null) {
            LocalDB localDB = LocalDB.getInstance();
            agency =  localDB.getListItem(getAgencyID());
        }
        return (Agency) agency;
    }

    public void setAgency(ListItem agency) {
        this.agency = agency;
    }

    private UUID schoolID;
    private ListItem school;

    public UUID getSchoolID() {
        return schoolID;
    }

    public void setSchoolID(UUID schoolID) {
        this.schoolID = schoolID;
    }

    public School getSchool() {
        if (schoolID != null && school == null) {
            LocalDB localDB = LocalDB.getInstance();
            school =  localDB.getListItem(getSchoolID());
        }
        return (School) school;
    }

    public void setSchool(ListItem school) {
        this.school = school;
    }

    private String contactName;

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    private String contactAddress;

    public String getContactAddress() {
        return contactAddress;
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    private String contactPostcode;

    public String getContactPostcode() {
        return contactPostcode;
    }

    public void setContactPostcode(String contactPostcode) {
        this.contactPostcode = contactPostcode;
    }

    private String contactContactNumber;

    public String getContactContactNumber() {
        return contactContactNumber;
    }

    public void setContactContactNumber(String contactContactNumber) {
        this.contactContactNumber = contactContactNumber;
    }

    private String contactEmailAddress;

    public String getContactEmailAddress() {
        return contactEmailAddress;
    }

    public void setContactEmailAddress(String contactEmailAddress) {
        this.contactEmailAddress = contactEmailAddress;
    }

    private String contactAdditionalInformation;

    public String getContactAdditionalInformation() {
        return contactAdditionalInformation;
    }

    public void setContactAdditionalInformation(String contactAdditionalInformation) {
        this.contactAdditionalInformation = contactAdditionalInformation;
    }

    //StartDate
    private Date startDate;

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    //EndDate
    private Date endDate;

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    // Relationship
    private UUID relationshipTypeID;
    private ListItem relationshipType;

    public UUID getRelationshipTypeID() {
        return relationshipTypeID;
    }

    public void setRelationshipTypeID(UUID relationshipTypeID) {
        this.relationshipTypeID = relationshipTypeID;
    }

    public ListItem getRelationshipType() {
        if (relationshipTypeID != null && relationshipType == null) {
            LocalDB localDB = LocalDB.getInstance();
            relationshipType = localDB.getListItem(relationshipTypeID);
        }
        return relationshipType;
    }

    public void setRelationshipType(ListItem relationshipType) {
        this.relationshipType = relationshipType;
    }

    public void clear(){
        setContactType(null);
        setAgency(null);
        setSchool(null);
        setRelationshipType(null);
    }

    public void save(boolean isNewMode) {
        LocalDB localDB = LocalDB.getInstance();
        ListItem contactType = getContactType();
        Agency agency = getAgency();
        School school = getSchool();
        ListItem relationshipType = getRelationshipType();
        clear();

        // Load the Document fields
        setReferenceDate(getStartDate());
        String summaryText = getContactName();
        if (!getContactContactNumber().isEmpty()) {
            summaryText += String.format(" (%s)", getContactContactNumber());
        } else {
            if (!getContactEmailAddress().isEmpty()) {
                summaryText += String.format(" (%s)", getContactEmailAddress());
            }
        }
        switch (contactType.getItemValue()) {
            case "School Contact":
                summaryText += String.format(", %s", school.getItemValue());
                break;
            case "Agency Contact":
                summaryText += String.format(", %s", agency.getItemValue());
                break;
            default:
                summaryText += String.format(", %s", relationshipType.getItemValue());
        }
        setSummary(summaryText);


        localDB.save(this, isNewMode, User.getCurrentUser());

        setContactType(contactType);
        setAgency(agency);
        setSchool(school);
        setRelationshipType(relationshipType);
    }

    public boolean search(String searchText) {
        if (searchText.isEmpty()) {
            return true;
        } else {
            String text = String.format("%s %s %s %s %s %s %s %s",
                    getContactType().getItemValue(),
                    getContactName(),
                    getContactAddress(),
                    getContactPostcode(),
                    getContactContactNumber(),
                    getContactEmailAddress(),
                    getContactAdditionalInformation(),
                    getRelationshipType().getItemValue());
            if (getAgency() != null) {
                text += getAgency().getItemValue();
            }
            if (getSchool() != null) {
                text += getSchool().getItemValue();
            }
            return text.toLowerCase().contains(searchText.toLowerCase());
        }
    }

    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("EEE dd MMM yyyy", Locale.UK);
        // Build the string
        String summary = "";
        switch (getContactType().getItemValue()) {
            case "School Contact":
                summary += "School: " + getSchool().getItemValue() + "\n";
                summary += getSchool().getSchoolAddress() + "\n";
                summary += getSchool().getSchoolPostcode() + "\n";
                summary += "School Contact: " + getContactName() + "\n";
                summary += "Relationship: " + getRelationshipType().getItemValue() + "\n";
                break;
            case "Agency Contact":
                summary += "Agency: " + getAgency().getItemValue() + "\n";
                summary += getAgency().getAgencyAddress() + "\n";
                summary += getAgency().getAgencyPostcode() + "\n";
                summary += "Agency Contact: " + getContactName() + "\n";
                summary += "Relationship: " + getRelationshipType().getItemValue() + "\n";
                break;
            default:
                summary += String.format("%s: %s\n", getContactType().getItemValue(), getContactName());
                summary += "Relationship: " + getRelationshipType().getItemValue() + "\n";
                summary += getContactAddress() + "\n";
                summary += getContactPostcode() + "\n";
        }
        summary += "Contact Number: " + getContactContactNumber() + "\n";
        summary += "Email Address : " + getContactEmailAddress() + "\n";
        summary += "Start Date: ";
        if (getStartDate().getTime() != Long.MIN_VALUE) {
            summary += sDate.format(getStartDate());
        }
        summary += "\n";
        summary += "End Date: ";
        if (getEndDate().getTime() != Long.MIN_VALUE) {
            summary += sDate.format(getEndDate());
        }
        summary += "\n";
        summary += "Additional Information:\n" + getContactAdditionalInformation();
        return summary;
    }

    private static List<Object> getExportFieldNames() {
        List<Object> fNames = new ArrayList<>();
        fNames.add("Firstnames");
        fNames.add("Lastname");
        fNames.add("Date of Birth");
        fNames.add("Age");
        fNames.add("Postcode");
        fNames.add("Start Date");
        fNames.add("End Date");
        fNames.add("Contact Type");
        fNames.add("Organisation");
        fNames.add("Relationship");
        fNames.add("Name");
        fNames.add("Contact Num.");
        fNames.add("Contact Email");
        return fNames;
    }

    public static List<List<Object>> getContactData(ArrayList<Document> documents) {
        LocalDB localDB = LocalDB.getInstance();
        Client client = null;
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames());
        for (Document document : documents) {
            if (client == null || document.getClientID() != client.getClientID()){
                // New client
                client = (Client) localDB.getDocument(document.getClientID());
            }
            Contact thisDocument = (Contact) document;
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
                                .setStartIndex(7))
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
        // 6/7th column is a date
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
        row.add(client.getPostcode());
        if (getStartDate().getTime() != Long.MIN_VALUE) {
            row.add(sDate.format(getStartDate()));
        } else {
            row.add("");
        }
        if (getEndDate().getTime() != Long.MIN_VALUE) {
            row.add(sDate.format(getEndDate()));
        } else {
            row.add("");
        }
            row.add(getItemValue(getContactType()));

        switch (getContactType().getItemValue()) {
            case "School Contact":
                    row.add(getItemValue(getSchool()));
                row.add(getItemValue(getRelationshipType()));
                row.add(getContactName());
                if (getContactContactNumber().isEmpty()) {
                    if (getSchool() != null) {
                        row.add(String.format("'%s", getSchool().getSchoolContactNumber()));
                    } else {
                        row.add("");
                    }
                } else {
                    row.add(String.format("'%s", getContactContactNumber()));
                }
                if (getContactEmailAddress().isEmpty()) {
                    if (getSchool() != null) {
                        row.add(getSchool().getSchoolEmailAddress());
                    } else {
                        row.add("");
                    }
                } else {
                    row.add(getContactEmailAddress());
                }
                break;
            case "Agency Contact":
                row.add(getItemValue(getAgency()));
                row.add(getItemValue(getRelationshipType()));
                row.add(getContactName());
                if (getContactContactNumber().isEmpty()) {
                    if (getAgency() != null) {
                        row.add(String.format("'%s", getAgency().getAgencyContactNumber()));
                    } else {
                        row.add("");
                    }
                } else {
                    row.add(String.format("'%s", getContactContactNumber()));
                }
                if (getContactEmailAddress().isEmpty()) {
                    if (getAgency() != null) {
                        row.add(getAgency().getAgencyEmailAddress());
                    }else {
                        row.add("");
                    }
                } else {
                    row.add(getContactEmailAddress());
                }
                break;
            default:
                row.add("");
                row.add(getItemValue(getRelationshipType()));
                row.add(getContactName());
                row.add(String.format("'%s", getContactContactNumber()));
                row.add(getContactEmailAddress());
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
}
