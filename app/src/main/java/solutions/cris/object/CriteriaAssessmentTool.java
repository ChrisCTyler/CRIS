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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;
import solutions.cris.list.ListClientHeader;


/**
 * Copyright CRIS Solutions on 05/02/2017.
 */

public class CriteriaAssessmentTool extends Document implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_CAT;

    public final static String[] homeSituationValues = {"Please select", "Lives with Single Parent",
            "Lives with Both Parents", "Lives with Other Family Member", "Other"};

    public final static String[] childStatusValues = {"Please select", "None",
            "CAF/TAF", "Children in Need Plan", "Child Protection Plan"};

    public final static String[] typeOfSupportValues = {"Please select", "Only Carer",
            "Supported by Parent", "Supported by Sibling", "Supported by Parent and Sibling"};

    public final static String[] frequencyOfCareValues = {"Please select", "1 Hour per Day",
            "2 to 4 Hours per Day", "More Than 4 Hours per Day"};

    public final static String[] frequencyOfSocialisingValues = {"Please select", "None",
            "Once a Week", "Twice a Week or More"};

    public CriteriaAssessmentTool(User currentUser, UUID clientID) {
        super(currentUser, clientID, Document.CriteriaAssessmentTool);
        homeSituation = "Please select";
        childStatus = "Please select";
        typeOfSupport = "Please select";
        personCaredForParent = 0;
        personCaredForSibling = 0;
        personCaredForOther = 0;
        typeOfCareDomestic1 = false;
        typeOfCareDomestic2 = false;
        typeOfCarePersonal = false;
        typeOfCareEmotional = false;
        typeOfCareSupervising = false;
        typeOfConditionMentalHealth = 0;
        typeOfConditionSubstanceMisuse = 0;
        typeOfConditionAlcoholMisuse = 0;
        typeOfConditionLearningDisability = 0;
        typeOfConditionIllHealth = 0;
        typeOfConditionPhysicalDisability = 0;
        typeOfConditionAutism = 0;
        typeOfConditionTerminalIllness = 0;
        frequencyOfCare = "Please select";
        frequencyOfSocialising = "Please select";
        score = 0;
    }

    private String homeSituation;

    public String getHomeSituation() {
        return homeSituation;
    }

    public void setHomeSituation(String homeSituation) {
        this.homeSituation = homeSituation;
    }

    private String childStatus;

    public String getChildStatus() {
        return childStatus;
    }

    public void setChildStatus(String childStatus) {
        this.childStatus = childStatus;
    }

    private String typeOfSupport;

    public String getTypeOfSupport() {
        return typeOfSupport;
    }

    public void setTypeOfSupport(String typeOfSupport) {
        this.typeOfSupport = typeOfSupport;
    }

    private int personCaredForParent;

    public int getPersonCaredForParent() {
        return personCaredForParent;
    }

    public void setPersonCaredForParent(int personCaredForParent) {
        this.personCaredForParent = personCaredForParent;
    }

    private int personCaredForSibling;

    public int getPersonCaredForSibling() {
        return personCaredForSibling;
    }

    public void setPersonCaredForSibling(int personCaredForSibling) {
        this.personCaredForSibling = personCaredForSibling;
    }

    private int personCaredForOther;

    public int getPersonCaredForOther() {
        return personCaredForOther;
    }

    public void setPersonCaredForOther(int personCaredForOther) {
        this.personCaredForOther = personCaredForOther;
    }

    private boolean typeOfCareDomestic1;

    public boolean getTypeOfCareDomestic1() {
        return typeOfCareDomestic1;
    }

    public void setTypeOfCareDomestic1(boolean typeOfCareDomestic1) {
        this.typeOfCareDomestic1 = typeOfCareDomestic1;
    }

    private boolean typeOfCareDomestic2;

    public boolean getTypeOfCareDomestic2() {
        return typeOfCareDomestic2;
    }

    public void setTypeOfCareDomestic2(boolean typeOfCareDomestic2) {
        this.typeOfCareDomestic2 = typeOfCareDomestic2;
    }

    private boolean typeOfCarePersonal;

    public boolean getTypeOfCarePersonal() {
        return typeOfCarePersonal;
    }

    public void setTypeOfCarePersonal(boolean typeOfCarePersonal) {
        this.typeOfCarePersonal = typeOfCarePersonal;
    }

    private boolean typeOfCareEmotional;

    public boolean getTypeOfCareEmotional() {
        return typeOfCareEmotional;
    }

    public void setTypeOfCareEmotional(boolean typeOfCareEmotional) {
        this.typeOfCareEmotional = typeOfCareEmotional;
    }

    private boolean typeOfCareSupervising;

    public boolean getTypeOfCareSupervising() {
        return typeOfCareSupervising;
    }

    public void setTypeOfCareSupervising(boolean typeOfCareSupervising) {
        this.typeOfCareSupervising = typeOfCareSupervising;
    }

    private int typeOfConditionMentalHealth;

    public int getTypeOfConditionMentalHealth() {
        return typeOfConditionMentalHealth;
    }

    public void setTypeOfConditionMentalHealth(int typeOfConditionMentalHealth) {
        this.typeOfConditionMentalHealth = typeOfConditionMentalHealth;
    }

    private int typeOfConditionSubstanceMisuse;

    public int getTypeOfConditionSubstanceMisuse() {
        return typeOfConditionSubstanceMisuse;
    }

    public void setTypeOfConditionSubstanceMisuse(int typeOfConditionSubstanceMisuse) {
        this.typeOfConditionSubstanceMisuse = typeOfConditionSubstanceMisuse;
    }

    private int typeOfConditionAlcoholMisuse;

    public int getTypeOfConditionAlcoholMisuse() {
        return typeOfConditionAlcoholMisuse;
    }

    public void setTypeOfConditionAlcoholMisuse(int typeOfConditionAlcoholMisuse) {
        this.typeOfConditionAlcoholMisuse = typeOfConditionAlcoholMisuse;
    }

    private int typeOfConditionLearningDisability;

    public int getTypeOfConditionLearningDisability() {
        return typeOfConditionLearningDisability;
    }

    public void setTypeOfConditionLearningDisability(int typeOfConditionLearningDisability) {
        this.typeOfConditionLearningDisability = typeOfConditionLearningDisability;
    }

    private int typeOfConditionIllHealth;

    public int getTypeOfConditionIllHealth() {
        return typeOfConditionIllHealth;
    }

    public void setTypeOfConditionIllHealth(int typeOfConditionIllHealth) {
        this.typeOfConditionIllHealth = typeOfConditionIllHealth;
    }

    private int typeOfConditionPhysicalDisability;

    public int getTypeOfConditionPhysicalDisability() {
        return typeOfConditionPhysicalDisability;
    }

    public void setTypeOfConditionPhysicalDisability(int typeOfConditionPhysicalDisability) {
        this.typeOfConditionPhysicalDisability = typeOfConditionPhysicalDisability;
    }

    private int typeOfConditionAutism;

    public int getTypeOfConditionAutism() {
        return typeOfConditionAutism;
    }

    public void setTypeOfConditionAutism(int typeOfConditionAutism) {
        this.typeOfConditionAutism = typeOfConditionAutism;
    }

    private int typeOfConditionTerminalIllness;

    public int getTypeOfConditionTerminalIllness() {
        return typeOfConditionTerminalIllness;
    }

    public void setTypeOfConditionTerminalIllness(int typeOfConditionTerminalIllness) {
        this.typeOfConditionTerminalIllness = typeOfConditionTerminalIllness;
    }

    private String frequencyOfCare;

    public String getFrequencyOfCare() {
        return frequencyOfCare;
    }

    public void setFrequencyOfCare(String frequencyOfCare) {
        this.frequencyOfCare = frequencyOfCare;
    }

    private String frequencyOfSocialising;

    public String getFrequencyOfSocialising() {
        return frequencyOfSocialising;
    }

    public void setFrequencyOfSocialising(String frequencyOfSocialising) {
        this.frequencyOfSocialising = frequencyOfSocialising;
    }

    // Each 'tool' that can be used in the header 'score' field needs a getScoreLabel()
    // and a getScoreText() method
    public String getScoreLabel() {
        return "CAT";
    }

    private int score;

    public int getScore() {
        return score;
    }

    public String getScoreText() {
        return String.format(Locale.UK, "%d", score);
    }

    public void setScore(Client client) {
        score = 0;
        // Calculate client's age
        Calendar dob = Calendar.getInstance();
        dob.setTime(client.getDateOfBirth());
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        int age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        if (age < 12) {
            score += 5;
        } else if (age < 14) {
            score += 3;
        } else if (age < 17) {
            score += 4;
        } else {
            score += 2;
        }
        switch (homeSituation) {
            case "Lives with Single Parent":
                score += 5;
                break;
            case "Lives with Both Parents":
                score += 3;
                break;
            case "Lives with Other Family Member":
            case "Other":
                score += 4;
        }
        switch (childStatus) {
            case "None":
                score += 0;
                break;
            case "CAF/TAF":
                score += 1;
                break;
            case "Children in Need Plan":
                score += 3;
                break;
            case "Child Protection Plan":
                score += 5;
        }
        switch (typeOfSupport) {
            case "Only Carer":
                score += 5;
                break;
            case "Supported by Sibling":
                score += 2;
                break;
            case "Supported by Parent":
            case "Supported by Parent and Sibling":
                score += 1;
        }

        score += (personCaredForParent * 5);
        score += getPCFScore(personCaredForSibling);
        score += getPCFScore(personCaredForOther);

        if (typeOfCareDomestic1) score += 3;
        if (typeOfCareDomestic2) score += 3;
        if (typeOfCarePersonal) score += 5;
        if (typeOfCareEmotional) score += 2;
        if (typeOfCareSupervising) score += 2;

        score += (typeOfConditionMentalHealth * 2);
        score += (typeOfConditionSubstanceMisuse * 2);
        score += (typeOfConditionAlcoholMisuse * 2);
        score += (typeOfConditionLearningDisability * 2);
        score += (typeOfConditionIllHealth * 2);
        score += (typeOfConditionPhysicalDisability * 2);
        score += (typeOfConditionAutism * 2);
        score += (typeOfConditionTerminalIllness * 10);

        switch (frequencyOfCare) {
            case "1 Hour per Day":
                score += 1;
                break;
            case "2 to 4 Hours per Day":
                score += 3;
                break;
            case "More Than 4 Hours per Day":
                score += 5;
        }
        switch (frequencyOfSocialising) {
            case "None":
                score += 5;
                break;
            case "Once a Week":
                score += 1;
                break;
            case "Twice a Week or More":
                score += 0;
        }
    }

    private int getPCFScore(int count) {
        switch (count) {
            case 0:
                return 0;
            case 1:
            case 2:
                return count * 3;
            default:
                return (count + 1) * 3;
        }
    }

    // Save the document
    public void save(boolean isNewMode) {
        LocalDB localDB = LocalDB.getInstance();

        // Load the Document fields
        if (isNewMode){
            setReferenceDate(getCreationDate());
        }
        setSummary(String.format(Locale.UK, "Score - %d", getScore()));

        localDB.save(this, isNewMode, User.getCurrentUser());
    }


    public boolean search(String searchText) {
        if (searchText.isEmpty()) {
            return true;
        } else {
            boolean found;
            String text = String.format("%s %s %s %s %s",
                    homeSituation, childStatus, typeOfSupport,
                    frequencyOfCare, frequencyOfSocialising);
            found = text.toLowerCase().contains(searchText.toLowerCase());
            return found;
        }
    }

    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        String summary = "Criteria Assessment Tool";
        summary += "Date: " + sDate.format(getReferenceDate()) + "\n";
        summary += "Home Siuation: " + homeSituation + "\n";
        summary += "Child Status: " + childStatus + "\n";
        summary += "Type of Support: " + typeOfSupport + "\n";
        if (personCaredForParent > 0) {
            summary += String.format("Number of Parents Cared For: %d\n", personCaredForParent);
        }
        if (personCaredForSibling > 0) {
            summary += String.format("Number of Siblings Cared For: %d\n", personCaredForSibling);
        }
        if (personCaredForOther > 0) {
            summary += String.format("Number of Others Cared For: %d\n", personCaredForOther);
        }
        if (typeOfCareDomestic1) {
            summary += String.format("Care (Domestic - washing, cleaning, food): %s\n",
                    displayBoolean(typeOfCareDomestic1));
        }
        if (typeOfCareDomestic2) {
            summary += String.format("Care (Domestic - assistance with shopping): %s\n",
                    displayBoolean(typeOfCareDomestic2));
        }
        if (typeOfCarePersonal) {
            summary += String.format("Care (Personal): %s\n",
                    displayBoolean(typeOfCarePersonal));
        }
        if (typeOfCareEmotional) {
            summary += String.format("Care (Emotional): %s\n",
                    displayBoolean(typeOfCareEmotional));
        }
        if (typeOfCareSupervising) {
            summary += String.format("Care (Supervising/supporting siblings): %s\n",
                    displayBoolean(typeOfCareSupervising));
        }
        if (typeOfConditionMentalHealth > 0) {
            summary += String.format(Locale.UK, "Number of People Supported with Mental Health Issues: %d\n",
                    typeOfConditionMentalHealth);
        }
        if (typeOfConditionSubstanceMisuse > 0){
            summary += String.format(Locale.UK, "Number of People Supported with Subs. Misuse Issues: %d\n",
                    typeOfConditionSubstanceMisuse);
        }
        if (typeOfConditionAlcoholMisuse > 0){
            summary += String.format(Locale.UK, "Number of People Supported with Alc. Mis. Issues: %d\n",
                    typeOfConditionAlcoholMisuse);
        }
        if (typeOfConditionLearningDisability > 0){
            summary += String.format(Locale.UK, "Number of People Supported with Learning Disability: %d\n",
                    typeOfConditionLearningDisability);
        }
        if (typeOfConditionIllHealth > 0){
            summary += String.format(Locale.UK, "Number of People Supported with Physical Ill Health: %d\n",
                    typeOfConditionIllHealth);
        }
        if (typeOfConditionPhysicalDisability > 0){
            summary += String.format(Locale.UK, "Number of People Supported with Physical Disabilities: %d\n",
                    typeOfConditionPhysicalDisability);
        }
        if (typeOfConditionAutism > 0){
            summary += String.format(Locale.UK, "Number of People Supported with Autistic Spectrum Disorder: %d\n",
                    typeOfConditionAutism);
        }
        if (typeOfConditionTerminalIllness > 0){
            summary += String.format(Locale.UK, "Number of People Supported with Terminal Illness: %d\n",
                    typeOfConditionTerminalIllness);
        }
        summary += "Frequency of Support: " + frequencyOfCare + "\n";
        summary += "Other Socialising: " + frequencyOfSocialising + "\n";
        summary += String.format(Locale.UK, "Score: %d\n", score);
        return summary;
    }

    private String displayBoolean(boolean value) {
        if (value) return "Yes";
        else return "No";
    }

    private static List<Object> getExportFieldNames() {
        List<Object> fNames = new ArrayList<>();
        fNames.add("Firstnames");
        fNames.add("Lastname");
        fNames.add("Date of Birth");
        fNames.add("Age");
        fNames.add("Postcode");
        fNames.add("Date");
        fNames.add("Home Siuation");
        fNames.add("Child Status");
        fNames.add("Type of Support");
        fNames.add("Parents Cared For");
        fNames.add("Siblings Cared For");
        fNames.add("Others Cared For");
        fNames.add("Care Dom. washing, cleaning, food");
        fNames.add("Care Dom. shopping");
        fNames.add("Care Personal");
        fNames.add("Care Emotional");
        fNames.add("Care Supervising supporting siblings");
        fNames.add("Num. Supp. Mental Health Issues");
        fNames.add("Num. Supp. Substance Misuse Issues");
        fNames.add("Num. Supp. Alcohol Misuse Issues");
        fNames.add("Num. Supp. Learning Disability");
        fNames.add("Num. Supp. Physical Ill Health");
        fNames.add("Num. Supp. Physical Disabilities");
        fNames.add("Num. Supp. Autistic Spectrum Disorder");
        fNames.add("Num. Supp. Terminal Illness");
        fNames.add("Frequency of Support");
        fNames.add("Other Socialising");
        fNames.add("Score");
        return fNames;
    }

    public static List<List<Object>> getCATData(ArrayList<Document> documents) {
        LocalDB localDB = LocalDB.getInstance();
        Client client = null;
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames());
        for (Document document : documents) {
            if (client == null || document.getClientID() != client.getClientID()) {
                // New client
                client = (Client) localDB.getDocument(document.getClientID());
            }
            CriteriaAssessmentTool thisDocument = (CriteriaAssessmentTool) document;
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
                                .setStartIndex(6)
                                .setEndIndex(9))
                ));
        // Set some Cell dimensions
        requests.add(new Request()
                .setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                        .setFields("pixelSize")
                        .setProperties(new DimensionProperties()
                                .setPixelSize(150))
                        .setRange(new DimensionRange()
                                .setSheetId(sheetID)
                                .setDimension("COLUMNS")
                                .setStartIndex(25)
                                .setEndIndex(26))
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
        // 6th column is a date
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
                                .setEndColumnIndex(6)
                                .setStartRowIndex(1))));
        return requests;
    }

    public List<Object> getExportData(Client client) {
        LocalDB localDB = LocalDB.getInstance();
        SimpleDateFormat sDate = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        List<Object> row = new ArrayList<>();
        row.add(client.getFirstNames());
        row.add(client.getLastName());
        row.add(sDate.format(client.getDateOfBirth()));
        row.add(client.getAge());
        row.add(client.getPostcode());
        if (getReferenceDate().getTime() != Long.MIN_VALUE) {
            row.add(sDate.format(getReferenceDate()));
        } else {
            row.add("");
        }
        row.add(homeSituation);
        row.add(childStatus);
        row.add(typeOfSupport);
        row.add(personCaredForParent);
        row.add(personCaredForSibling);
        row.add(personCaredForOther);
        row.add(displayExportBoolean(typeOfCareDomestic1));
        row.add(displayExportBoolean(typeOfCareDomestic2));
        row.add(displayExportBoolean(typeOfCarePersonal));
        row.add(displayExportBoolean(typeOfCareEmotional));
        row.add(displayExportBoolean(typeOfCareSupervising));
        row.add(typeOfConditionMentalHealth);
        row.add(typeOfConditionSubstanceMisuse);
        row.add(typeOfConditionAlcoholMisuse);
        row.add(typeOfConditionLearningDisability);
        row.add(typeOfConditionIllHealth);
        row.add(typeOfConditionPhysicalDisability);
        row.add(typeOfConditionAutism);
        row.add(typeOfConditionTerminalIllness);
        row.add(frequencyOfCare);
        row.add(frequencyOfSocialising);
        row.add(score);
        return row;

    }

    private String displayExportBoolean(boolean value) {
        if (value) return "True";
        else return "False";
    }

    private String getItemValue(ListItem item) {
        if (item == null) {
            return "Unknown";
        } else {
            return item.getItemValue();
        }
    }

}
