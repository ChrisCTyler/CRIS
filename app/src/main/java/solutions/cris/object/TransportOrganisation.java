package solutions.cris.object;

import java.io.Serializable;
import java.text.SimpleDateFormat;
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

public class TransportOrganisation extends ListItem implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_TRANSPORT_ORGANISATION;

    public TransportOrganisation(User currentUser, String itemValue, int itemOrder) {
        super(currentUser, ListType.TRANSPORT_ORGANISATION, itemValue, itemOrder);
        address = "";
        postcode = "";
        contactNumber = "";
        emailAddress = "";
        additionalInformation = "";
    }

    private String address;
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    private String postcode;
    public String getPostcode() {
        return postcode;
    }
    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    private String contactNumber;
    public String getContactNumber() {
        return contactNumber;
    }
    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    private String emailAddress;
    public String getEmailAddress() {
        return emailAddress;
    }
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    private String additionalInformation;
    public String getAdditionalInformation() {
        return additionalInformation;
    }
    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public String textSummary(){
        String summary = super.textSummary();
        summary += String.format("Address:\n%s\n%s\n",getAddress(), getPostcode());
        summary += String.format("Contact Number: %s\n", getContactNumber());
        summary += String.format("Email: %s\n",getEmailAddress());
        if (!getAdditionalInformation().isEmpty()){
            summary += String.format("Additional Information:\n%s", getAdditionalInformation());
        }
        return summary;
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action){
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        LocalSettings localSettings = LocalSettings.getInstance();
        TransportOrganisation previousItem = (TransportOrganisation) localDB.getListItemByRecordID(previousRecordID);
        TransportOrganisation thisItem = (TransportOrganisation) localDB.getListItemByRecordID(thisRecordID);
        String changes = ListItem.getChanges(previousItem, thisItem);
        changes += CRISUtil.getChanges(previousItem.getAddress(), thisItem.getAddress(), "Address");
        changes += CRISUtil.getChanges(previousItem.getPostcode(), thisItem.getPostcode(), "Postcode");
        changes += CRISUtil.getChanges(previousItem.getContactNumber(), thisItem.getContactNumber(), "Contact Number");
        changes += CRISUtil.getChanges(previousItem.getEmailAddress(), thisItem.getEmailAddress(), "Email Address");
        changes += CRISUtil.getChanges(previousItem.getAdditionalInformation(), thisItem.getAdditionalInformation(), "Additional Information");
        if (changes.length() == 0){
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
    }
}

