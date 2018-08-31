package solutions.cris.object;

import java.io.Serializable;

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
        String summary = String.format("%s\n",getAddress());
        summary += String.format("%s\n", getPostcode());
        summary += String.format("Contact Number: %s\n", getContactNumber());
        summary += String.format("Email: %s\n",getEmailAddress());
        if (!getAdditionalInformation().isEmpty()){
            summary += String.format("Additional Information:\n%s", getAdditionalInformation());
        }
        return summary;
    }

}

