package solutions.cris.object;

import java.io.Serializable;

/**
 * Copyright CRIS Solutions on 26/01/2017.
 */

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

