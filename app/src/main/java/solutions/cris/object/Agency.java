package solutions.cris.object;

import android.database.sqlite.SQLiteConstraintException;

import java.io.Serializable;
import java.util.ArrayList;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListComplexListItems;

/**
 * Copyright CRIS Solutions 26/01/2017.
 */

public class Agency extends ListItem implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_AGENCY;

    public Agency(User currentUser, String itemValue, int itemOrder) {
        super(currentUser, ListType.AGENCY, itemValue, itemOrder);
        agencyAddress = "";
        agencyPostcode = "";
        agencyContactNumber = "";
        agencyEmailAddress = "";
        agencyAdditionalInformation = "";
    }

    private String agencyAddress;
    public String getAgencyAddress() {
        return agencyAddress;
    }
    public void setAgencyAddress(String agencyAddress) {
        this.agencyAddress = agencyAddress;
    }

    private String agencyPostcode;
    public String getAgencyPostcode() {
        return agencyPostcode;
    }
    public void setAgencyPostcode(String agencyPostcode) {
        this.agencyPostcode = agencyPostcode;
    }

    private String agencyContactNumber;
    public String getAgencyContactNumber() {
        return agencyContactNumber;
    }
    public void setAgencyContactNumber(String agencyContactNumber) {
        this.agencyContactNumber = agencyContactNumber;
    }

    private String agencyEmailAddress;
    public String getAgencyEmailAddress() {
        return agencyEmailAddress;
    }
    public void setAgencyEmailAddress(String agencyEmailAddress) {
        this.agencyEmailAddress = agencyEmailAddress;
    }

    private String agencyAdditionalInformation;
    public String getAgencyAdditionalInformation() {
        return agencyAdditionalInformation;
    }
    public void setAgencyAdditionalInformation(String agencyAdditionalInformation) {
        this.agencyAdditionalInformation = agencyAdditionalInformation;
    }

    public String textSummary(){
        String summary = String.format("%s\n",getAgencyAddress());
        summary += String.format("%s\n", getAgencyPostcode());
        summary += String.format("Contact Number: %s\n", getAgencyContactNumber());
        summary += String.format("Email: %s\n",getAgencyEmailAddress());
        if (!getAgencyAdditionalInformation().isEmpty()){
            summary += String.format("Additional Information:\n%s", getAgencyAdditionalInformation());
        }
        return summary;
    }

}
