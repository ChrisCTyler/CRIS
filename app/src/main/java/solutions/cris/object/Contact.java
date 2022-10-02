package solutions.cris.object;

import android.content.res.Resources;

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

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListClientHeader;
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
        // Build 181 - New field, default to true on new records
        freeSchoolMeals = true;
        // Build 183 - New field
        specialNeeds = false;
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
        // Build 171 - Handle the unexpected null ListItemID case
        if (contactTypeID != null && contactType == null) {
            LocalDB localDB = LocalDB.getInstance();
            contactType = localDB.getListItem(contactTypeID);
        }
        if (contactType == null) {
            contactType = new ListItem(User.getCurrentUser(), ListType.CONTACT_TYPE, "Unknown", 0);
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
        // Build 171 - Handle the unexpected null ListItemID case
        if (agencyID != null && agency == null) {
            LocalDB localDB = LocalDB.getInstance();
            agency = localDB.getListItem(getAgencyID());
        }
        if (agency == null) {
            // Build 176 Agency is a complex list item
            //agency = new ListItem(User.getCurrentUser(),ListType.AGENCY,"Unknown",0);
            agency = new Agency(User.getCurrentUser(), "Unknown", 0);
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
        // Build 171 - Handle the unexpected null ListItemID case
        if (schoolID != null && school == null) {
            LocalDB localDB = LocalDB.getInstance();
            school = localDB.getListItem(getSchoolID());
        }
        if (school == null) {
            // Build 176 School is a complex list item
            //school = new ListItem(User.getCurrentUser(),ListType.SCHOOL,"Unknown",0);
            school = new School(User.getCurrentUser(), "Unknown", 0);
        }
        return (School) school;
    }

    public void setSchool(ListItem school) {
        this.school = school;
    }

    // Build 181 - Added Free School Meals
    private boolean freeSchoolMeals;

    public boolean isFreeSchoolMeals() {
        return freeSchoolMeals;
    }

    public void setFreeSchoolMeals(boolean freeSchoolMeals) {
        this.freeSchoolMeals = freeSchoolMeals;
    }

    // Build 183 - Added SEND
    private boolean specialNeeds;

    public boolean isSpecialNeeds() {
        return specialNeeds;
    }

    public void setSpecialNeeds(boolean specialNeeds) {
        this.specialNeeds = specialNeeds;
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
        // Build 171 - Handle the unexpected null ListItemID case
        if (relationshipTypeID != null && relationshipType == null) {
            LocalDB localDB = LocalDB.getInstance();
            relationshipType = localDB.getListItem(relationshipTypeID);
        }
        if (relationshipType == null) {
            relationshipType = new ListItem(User.getCurrentUser(), ListType.RELATIONSHIP, "Unknown", 0);
        }
        return relationshipType;
    }

    public void setRelationshipType(ListItem relationshipType) {
        this.relationshipType = relationshipType;
    }

    public void clear() {
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

    // Build 181 - Added for new Free School Meals field
    private String displayBoolean(boolean value) {
        if (value) return "Yes";
        else return "No";
    }

    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("EEE dd MMM yyyy", Locale.UK);
        // Build the string
        String summary = super.textSummary();
        switch (getContactType().getItemValue()) {
            case "School Contact":
                summary += "School: " + getSchool().getItemValue() + "\n";
                summary += getSchool().getSchoolAddress() + "\n";
                summary += getSchool().getSchoolPostcode() + "\n";
                summary += "School Contact: " + getContactName() + "\n";
                summary += "Relationship: " + getRelationshipType().getItemValue() + "\n";
                // Build 181
                summary += String.format("Free School Meals: %s\n",
                        displayBoolean(freeSchoolMeals));
                // Build 183
                summary += String.format("Special Educational Needs and Disabilities: %s\n",
                        displayBoolean(specialNeeds));
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
                summary += "Address:\n" + getContactAddress() + "\n";
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

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action) {
        Contact previousDocument = (Contact) localDB.getDocumentByRecordID(previousRecordID);
        Contact thisDocument = (Contact) localDB.getDocumentByRecordID(thisRecordID);
        String changes = Document.getChanges(previousDocument, thisDocument);
        if (!previousDocument.getContactType().equals(thisDocument.getContactType())) {
            changes += CRISUtil.getChanges(previousDocument.getContactType(), thisDocument.getContactType(), "ContactType");
        } else {
            switch (previousDocument.getContactType().getItemValue()) {
                case "School Contact":
                    changes += CRISUtil.getChanges(previousDocument.getSchool(), thisDocument.getSchool(), "School");
                    changes += CRISUtil.getChanges(previousDocument.getContactName(), thisDocument.getContactName(), "Contact Name");
                    changes += CRISUtil.getChanges(previousDocument.getRelationshipType(), thisDocument.getRelationshipType(), "Relationship Type");
                    // Build 181
                    changes += CRISUtil.getChanges(previousDocument.isFreeSchoolMeals(), thisDocument.isFreeSchoolMeals(), "Free School Meals");
                    // Build 183
                    changes += CRISUtil.getChanges(previousDocument.isSpecialNeeds(), thisDocument.isSpecialNeeds(), "Special Educational Needs and Disabilities");
                    break;
                case "Agency Contact":
                    changes += CRISUtil.getChanges(previousDocument.getAgency(), thisDocument.getAgency(), "Agency");
                    changes += CRISUtil.getChanges(previousDocument.getContactName(), thisDocument.getContactName(), "Contact Name");
                    changes += CRISUtil.getChanges(previousDocument.getRelationshipType(), thisDocument.getRelationshipType(), "Relationship Type");
                    break;
                default:
                    changes += CRISUtil.getChanges(previousDocument.getContactName(), thisDocument.getContactName(), "Contact Name");
                    changes += CRISUtil.getChanges(previousDocument.getRelationshipType(), thisDocument.getRelationshipType(), "Relationship Type");
                    changes += CRISUtil.getChanges(previousDocument.getContactAddress(), thisDocument.getContactAddress(), "Address");
                    changes += CRISUtil.getChanges(previousDocument.getContactPostcode(), thisDocument.getContactPostcode(), "Postcode");
            }
            changes += CRISUtil.getChangesDate(previousDocument.getStartDate(), thisDocument.getStartDate(), "Start Date");
            changes += CRISUtil.getChangesDate(previousDocument.getEndDate(), thisDocument.getEndDate(), "End Date");
            changes += CRISUtil.getChanges(previousDocument.getContactContactNumber(), thisDocument.getContactContactNumber(), "Contact Number");
            changes += CRISUtil.getChanges(previousDocument.getContactEmailAddress(), thisDocument.getContactEmailAddress(), "Email Address");
            changes += CRISUtil.getChanges(previousDocument.getContactAdditionalInformation(), thisDocument.getContactAdditionalInformation(), "Additional Information");
        }
        if (changes.length() == 0) {
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
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
        fNames.add("Start Date");
        fNames.add("End Date");
        fNames.add("Contact Type");
        fNames.add("Organisation");
        fNames.add("Relationship");
        fNames.add("Name");
        fNames.add("Contact Num.");
        fNames.add("Contact Email");
        fNames.add("Free School Meals");
        return fNames;
    }


    public static List<List<Object>> getContactData(ArrayList<Document> documents) {
        LocalDB localDB = LocalDB.getInstance();
        Client client = null;
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames());
        for (Document document : documents) {
            if (client == null || document.getClientID() != client.getClientID()) {
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
                                        .setTextFormat(new TextFormat().setFontSize(11))
                                        .setNumberFormat(new NumberFormat().setType("DATE"))
                                ))
                        .setFields("UserEnteredFormat")
                        .setRange(new GridRange()
                                .setSheetId(sheetID)
                                // Build 139 - Adding Year Group to Export shifts column to right
                                .setStartColumnIndex(6)
                                .setEndColumnIndex(8)
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
        // Build 171 Tidy up
        //row.add(getItemValue(getContactType()));
        row.add(getContactType().getItemValue());

        switch (getContactType().getItemValue()) {
            case "School Contact":
                // Tidy up
                //row.add(getItemValue(getSchool()));
                //row.add(getItemValue(getRelationshipType()));
                row.add(getSchool().getItemValue());
                row.add(getRelationshipType().getItemValue());
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
                // Build 181
                row.add(displayExportBoolean(freeSchoolMeals));
                // Build 183
                row.add(displayExportBoolean(specialNeeds));
                break;
            case "Agency Contact":
                // Build 171 Tidy up
                //row.add(getItemValue(getAgency()));
                //row.add(getItemValue(getRelationshipType()));
                row.add(getAgency().getItemValue());
                row.add(getRelationshipType().getItemValue());
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
                    } else {
                        row.add("");
                    }
                } else {
                    row.add(getContactEmailAddress());
                }
                //freeSchoolMeals
                row.add("");
                //specialNeeds
                row.add("");
                break;
            default:
                row.add("");
                // Build 171 Tidy up
                //row.add(getItemValue(getRelationshipType()));
                row.add(getRelationshipType().getItemValue());
                row.add(getContactName());
                row.add(String.format("'%s", getContactContactNumber()));
                row.add(getContactEmailAddress());
                //freeSchoolMeals
                row.add("");
                //specialNeeds
                row.add("");
        }

        return row;

    }

    private String displayExportBoolean(boolean value) {
        if (value) return "True";
        else return "False";
    }

    // Build 171 Tidy up
    /*
    private String getItemValue(ListItem item){
        if (item == null){
            return "Unknown";
        } else {
            return item.getItemValue();
        }
    }

     */
}
