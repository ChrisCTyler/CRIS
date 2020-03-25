package solutions.cris.object;

import android.database.sqlite.SQLiteConstraintException;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListComplexListItems;
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
        String summary = super.textSummary();
        summary += String.format("Address:\n%s\n%s\n",getAgencyAddress(), getAgencyPostcode());
        summary += String.format("Contact Number: %s\n", getAgencyContactNumber());
        summary += String.format("Email: %s\n",getAgencyEmailAddress());
        if (!getAgencyAdditionalInformation().isEmpty()){
            summary += String.format("Additional Information:\n%s", getAgencyAdditionalInformation());
        }
        return summary;
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action){
        Agency previousItem = (Agency) localDB.getListItemByRecordID(previousRecordID);
        Agency thisItem = (Agency) localDB.getListItemByRecordID(thisRecordID);
        String changes = ListItem.getChanges(previousItem, thisItem);
        changes += CRISUtil.getChanges(previousItem.getAgencyAddress(), thisItem.getAgencyAddress(), "Address");
        changes += CRISUtil.getChanges(previousItem.getAgencyPostcode(), thisItem.getAgencyPostcode(), "Postcode");
        changes += CRISUtil.getChanges(previousItem.getAgencyContactNumber(), thisItem.getAgencyContactNumber(), "Contact Number");
        changes += CRISUtil.getChanges(previousItem.getAgencyEmailAddress(), thisItem.getAgencyEmailAddress(), "Email Address");
        changes += CRISUtil.getChanges(previousItem.getAgencyAdditionalInformation(), thisItem.getAgencyAdditionalInformation(), "Additional Information");
        if (changes.length() == 0){
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
    }

}
