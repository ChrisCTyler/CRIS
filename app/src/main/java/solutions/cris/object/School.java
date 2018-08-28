package solutions.cris.object;

import java.io.Serializable;

/**
 * Copyright CRIS Solutions on 26/01/2017.
 */

public class School extends ListItem implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_SCHOOL;

    public School(User currentUser, String itemValue, int itemOrder) {
        super(currentUser, ListType.SCHOOL, itemValue, itemOrder);
        schoolAddress = "";
        schoolPostcode = "";
        schoolContactNumber = "";
        schoolEmailAddress = "";
        schoolHeadTeacher = "";
        schoolAdditionalInformation = "";
    }

    private String schoolAddress;
    public String getSchoolAddress() {
        return schoolAddress;
    }
    public void setSchoolAddress(String schoolAddress) {
        this.schoolAddress = schoolAddress;
    }

    private String schoolPostcode;
    public String getSchoolPostcode() {
        return schoolPostcode;
    }
    public void setSchoolPostcode(String schoolPostcode) {
        this.schoolPostcode = schoolPostcode;
    }

    private String schoolContactNumber;
    public String getSchoolContactNumber() {
        return schoolContactNumber;
    }
    public void setSchoolContactNumber(String schoolContactNumber) {
        this.schoolContactNumber = schoolContactNumber;
    }

    private String schoolEmailAddress;
    public String getSchoolEmailAddress() {
        return schoolEmailAddress;
    }
    public void setSchoolEmailAddress(String schoolEmailAddress) {
        this.schoolEmailAddress = schoolEmailAddress;
    }

    private String schoolHeadTeacher;
    public String getSchoolHeadTeacher() {
        return schoolHeadTeacher;
    }
    public void setSchoolHeadTeacher(String schoolHeadTeacher) {
        this.schoolHeadTeacher = schoolHeadTeacher;
    }

    private String schoolAdditionalInformation;
    public String getSchoolAdditionalInformation() {
        return schoolAdditionalInformation;
    }
    public void setSchoolAdditionalInformation(String schoolAdditionalInformation) {
        this.schoolAdditionalInformation = schoolAdditionalInformation;
    }

    public String textSummary(){
        String summary = String.format("%s\n",getSchoolAddress());
        summary += String.format("%s\n", getSchoolPostcode());
        summary += String.format("Contact Number: %s\n", getSchoolContactNumber());
        summary += String.format("Email: %s\n",getSchoolEmailAddress());
        summary += String.format("Head Teacher: %s\n",getSchoolHeadTeacher());
        if (!getSchoolAdditionalInformation().isEmpty()){
            summary += String.format("Additional Information:\n%s", getSchoolAdditionalInformation());
        }
        return summary;
    }

}
