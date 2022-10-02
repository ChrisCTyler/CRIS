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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
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

public class MACAYC18 extends Document implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_MACA_YC18;

    public final static String[] responseValues = {"Please select", "Never",
            "Some of the time", "A lot of the time"};

    public MACAYC18(User currentUser, UUID clientID) {
        super(currentUser, clientID, Document.MACAYC18);
        provideHelpToMother = false;
        provideHelpToStepMother = false;
        provideHelpToFather = false;
        provideHelpToStepFather = false;
        provideHelpToBrothers = 0;
        provideHelpToSisters = 0;
        provideHelpToGrandparents = 0;
        provideHelpToOtherRelative = "";
        provideHelpToFamilyFriend = false;
        provideHelpToOther = "";
        reasonNeedHelp = "";
        caringSubstanceMisuse = false;
        caringPhysicalDisability = false;
        caringLearningDisability = false;
        caringLifeLimiting = false;
        caringMentalHealth = false;
        caringLGBT = false;
        caringTraveller = false;
        caringRural = false;
        caringEthnicMinority = false;
        cleanOwnBedroom01 = "Please select";
        cleanOtherRooms02 = "Please select";
        washDishes03 = "Please select";
        decorateRoom04 = "Please select";
        responsibilityForShopping05 = "Please select";
        carryingHeavyThings06 = "Please select";
        billsBankingBenefits07 = "Please select";
        workPartTime08 = "Please select";
        interpretSignOther09 = "Please select";
        helpDressUndress10 = "Please select";
        helpWash11 = "Please select";
        helpBathShower12 = "Please select";
        keepPersonCompany13 = "Please select";
        makeSureAlright14 = "Please select";
        takeOut15 = "Please select";
        takeSiblingsToSchool16 = "Please select";
        lookAfterSiblingsWithAdult17 = "Please select";
        lookAfterSiblingsOnOwn18 = "Please select";
    }

    // Each 'tool' that can be used in the header 'score' field needs a getScoreLabel()
    // and a getScoreText() method
    public String getScoreLabel() {
        return "MACA";
    }

    private boolean provideHelpToMother;

    public boolean isProvideHelpToMother() {
        return provideHelpToMother;
    }

    public void setProvideHelpToMother(boolean provideHelpToMother) {
        this.provideHelpToMother = provideHelpToMother;
    }

    private boolean provideHelpToStepMother;

    public boolean isProvideHelpToStepMother() {
        return provideHelpToStepMother;
    }

    public void setProvideHelpToStepMother(boolean provideHelpToStepMother) {
        this.provideHelpToStepMother = provideHelpToStepMother;
    }

    private boolean provideHelpToFather;

    public boolean isProvideHelpToFather() {
        return provideHelpToFather;
    }

    public void setProvideHelpToFather(boolean provideHelpToFather) {
        this.provideHelpToFather = provideHelpToFather;
    }

    private boolean provideHelpToStepFather;

    public boolean isProvideHelpToStepFather() {
        return provideHelpToStepFather;
    }

    public void setProvideHelpToStepFather(boolean provideHelpToStepFather) {
        this.provideHelpToStepFather = provideHelpToStepFather;
    }

    private int provideHelpToBrothers;

    public int getProvideHelpToBrothers() {
        return provideHelpToBrothers;
    }

    public void setProvideHelpToBrothers(int provideHelpToBrothers) {
        this.provideHelpToBrothers = provideHelpToBrothers;
    }

    private int provideHelpToSisters;

    public int getProvideHelpToSisters() {
        return provideHelpToSisters;
    }

    public void setProvideHelpToSisters(int provideHelpToSisters) {
        this.provideHelpToSisters = provideHelpToSisters;
    }

    private int provideHelpToGrandparents;

    public int getProvideHelpToGrandparents() {
        return provideHelpToGrandparents;
    }

    public void setProvideHelpToGrandparents(int provideHelpToGrandparents) {
        this.provideHelpToGrandparents = provideHelpToGrandparents;
    }

    private String provideHelpToOtherRelative;

    public String getProvideHelpToOtherRelative() {
        return provideHelpToOtherRelative;
    }

    public void setProvideHelpToOtherRelative(String provideHelpToOtherRelative) {
        this.provideHelpToOtherRelative = provideHelpToOtherRelative;
    }

    private boolean provideHelpToFamilyFriend;

    public boolean isProvideHelpToFamilyFriend() {
        return provideHelpToFamilyFriend;
    }

    public void setProvideHelpToFamilyFriend(boolean provideHelpToFamilyFriend) {
        this.provideHelpToFamilyFriend = provideHelpToFamilyFriend;
    }

    private String provideHelpToOther;

    public String getProvideHelpToOther() {
        return provideHelpToOther;
    }

    public void setProvideHelpToOther(String provideHelpToOther) {
        this.provideHelpToOther = provideHelpToOther;
    }

    private String reasonNeedHelp;

    public String getReasonNeedHelp() {
        return reasonNeedHelp;
    }

    public void setReasonNeedHelp(String reasonNeedHelp) {
        this.reasonNeedHelp = reasonNeedHelp;
    }

    private boolean caringSubstanceMisuse;

    public boolean isCaringSubstanceMisuse() {
        return caringSubstanceMisuse;
    }

    public void setCaringSubstanceMisuse(boolean caringSubstanceMisuse) {
        this.caringSubstanceMisuse = caringSubstanceMisuse;
    }

    private boolean caringPhysicalDisability;

    public boolean isCaringPhysicalDisability() {
        return caringPhysicalDisability;
    }

    public void setCaringPhysicalDisability(boolean caringPhysicalDisability) {
        this.caringPhysicalDisability = caringPhysicalDisability;
    }

    private boolean caringLearningDisability;

    public boolean isCaringLearningDisability() {
        return caringLearningDisability;
    }

    public void setCaringLearningDisability(boolean caringLearningDisability) {
        this.caringLearningDisability = caringLearningDisability;
    }

    private boolean caringLifeLimiting;

    public boolean isCaringLifeLimiting() {
        return caringLifeLimiting;
    }

    public void setCaringLifeLimiting(boolean caringLifeLimiting) {
        this.caringLifeLimiting = caringLifeLimiting;
    }

    private boolean caringMentalHealth;

    public boolean isCaringMentalHealth() {
        return caringMentalHealth;
    }

    public void setCaringMentalHealth(boolean caringMentalHealth) {
        this.caringMentalHealth = caringMentalHealth;
    }

    private boolean caringLGBT;

    public boolean isCaringLGBT() {
        return caringLGBT;
    }

    public void setCaringLGBT(boolean caringLGBT) {
        this.caringLGBT = caringLGBT;
    }

    private boolean caringTraveller;

    public boolean isCaringTraveller() {
        return caringTraveller;
    }

    public void setCaringTraveller(boolean caringTraveller) {
        this.caringTraveller = caringTraveller;
    }

    private boolean caringRural;

    public boolean isCaringRural() {
        return caringRural;
    }

    public void setCaringRural(boolean caringRural) {
        this.caringRural = caringRural;
    }

    private boolean caringEthnicMinority;

    public boolean isCaringEthnicMinority() {
        return caringEthnicMinority;
    }

    public void setCaringEthnicMinority(boolean caringEthnicMinority) {
        this.caringEthnicMinority = caringEthnicMinority;
    }

    private String cleanOwnBedroom01;

    public String getCleanOwnBedroom01() {
        return cleanOwnBedroom01;
    }

    public void setCleanOwnBedroom01(String cleanOwnBedroom01) {
        this.cleanOwnBedroom01 = cleanOwnBedroom01;
    }

    private String cleanOtherRooms02;

    public String getCleanOtherRooms02() {
        return cleanOtherRooms02;
    }

    public void setCleanOtherRooms02(String cleanOtherRooms02) {
        this.cleanOtherRooms02 = cleanOtherRooms02;
    }

    private String washDishes03;

    public String getWashDishes03() {
        return washDishes03;
    }

    public void setWashDishes03(String washDishes03) {
        this.washDishes03 = washDishes03;
    }

    private String decorateRoom04;

    public String getDecorateRoom04() {
        return decorateRoom04;
    }

    public void setDecorateRoom04(String decorateRoom04) {
        this.decorateRoom04 = decorateRoom04;
    }

    private String responsibilityForShopping05;

    public String getResponsibilityForShopping05() {
        return responsibilityForShopping05;
    }

    public void setResponsibilityForShopping05(String responsibilityForShopping05) {
        this.responsibilityForShopping05 = responsibilityForShopping05;
    }

    private String carryingHeavyThings06;

    public String getCarryingHeavyThings06() {
        return carryingHeavyThings06;
    }

    public void setCarryingHeavyThings06(String carryingHeavyThings06) {
        this.carryingHeavyThings06 = carryingHeavyThings06;
    }

    private String billsBankingBenefits07;

    public String getBillsBankingBenefits07() {
        return billsBankingBenefits07;
    }

    public void setBillsBankingBenefits07(String billsBankingBenefits07) {
        this.billsBankingBenefits07 = billsBankingBenefits07;
    }

    private String workPartTime08;

    public String getWorkPartTime08() {
        return workPartTime08;
    }

    public void setWorkPartTime08(String workPartTime08) {
        this.workPartTime08 = workPartTime08;
    }

    private String interpretSignOther09;

    public String getInterpretSignOther09() {
        return interpretSignOther09;
    }

    public void setInterpretSignOther09(String interpretSignOther09) {
        this.interpretSignOther09 = interpretSignOther09;
    }

    private String helpDressUndress10;

    public String getHelpDressUndress10() {
        return helpDressUndress10;
    }

    public void setHelpDressUndress10(String helpDressUndress10) {
        this.helpDressUndress10 = helpDressUndress10;
    }

    private String helpWash11;

    public String getHelpWash11() {
        return helpWash11;
    }

    public void setHelpWash11(String helpWash11) {
        this.helpWash11 = helpWash11;
    }

    private String helpBathShower12;

    public String getHelpBathShower12() {
        return helpBathShower12;
    }

    public void setHelpBathShower12(String helpBathShower12) {
        this.helpBathShower12 = helpBathShower12;
    }

    private String keepPersonCompany13;

    public String getKeepPersonCompany13() {
        return keepPersonCompany13;
    }

    public void setKeepPersonCompany13(String keepPersonCompany13) {
        this.keepPersonCompany13 = keepPersonCompany13;
    }

    private String makeSureAlright14;

    public String getMakeSureAlright14() {
        return makeSureAlright14;
    }

    public void setMakeSureAlright14(String makeSureAlright14) {
        this.makeSureAlright14 = makeSureAlright14;
    }

    private String takeOut15;

    public String getTakeOut15() {
        return takeOut15;
    }

    public void setTakeOut15(String takeOut15) {
        this.takeOut15 = takeOut15;
    }

    private String takeSiblingsToSchool16;

    public String getTakeSiblingsToSchool16() {
        return takeSiblingsToSchool16;
    }

    public void setTakeSiblingsToSchool16(String takeSiblingsToSchool16) {
        this.takeSiblingsToSchool16 = takeSiblingsToSchool16;
    }

    private String lookAfterSiblingsWithAdult17;

    public String getLookAfterSiblingsWithAdult17() {
        return lookAfterSiblingsWithAdult17;
    }

    public void setLookAfterSiblingsWithAdult17(String lookAfterSiblingsWithAdult17) {
        this.lookAfterSiblingsWithAdult17 = lookAfterSiblingsWithAdult17;
    }

    private String lookAfterSiblingsOnOwn18;

    public String getLookAfterSiblingsOnOwn18() {
        return lookAfterSiblingsOnOwn18;
    }

    public void setLookAfterSiblingsOnOwn18(String lookAfterSiblingsOnOwn18) {
        this.lookAfterSiblingsOnOwn18 = lookAfterSiblingsOnOwn18;
    }

    // Subscores
    int getDomesticActivity() {
        int scoreValue = 0;
        scoreValue += getScoreValue(cleanOwnBedroom01);
        scoreValue += getScoreValue(cleanOtherRooms02);
        scoreValue += getScoreValue(washDishes03);
        return scoreValue;
    }

    public String getDomesticActivityText() {
        return String.format(Locale.UK, "%d", getDomesticActivity());
    }

    int getHouseholdManagement() {
        int scoreValue = 0;
        scoreValue += getScoreValue(decorateRoom04);
        scoreValue += getScoreValue(responsibilityForShopping05);
        scoreValue += getScoreValue(carryingHeavyThings06);
        return scoreValue;
    }

    String gethouseholdManagementText() {
        return String.format(Locale.UK, "%d", getHouseholdManagement());
    }

    int getPersonalCare() {
        int scoreValue = 0;
        scoreValue += getScoreValue(helpDressUndress10);
        scoreValue += getScoreValue(helpWash11);
        scoreValue += getScoreValue(helpBathShower12);
        return scoreValue;
    }

    String getpersonalCareText() {
        return String.format(Locale.UK, "%d", getPersonalCare());
    }

    int getEmotionalCare() {
        int scoreValue = 0;
        scoreValue += getScoreValue(keepPersonCompany13);
        scoreValue += getScoreValue(makeSureAlright14);
        scoreValue += getScoreValue(takeOut15);
        return scoreValue;
    }

    String getemotionalCareText() {
        return String.format(Locale.UK, "%d", getEmotionalCare());
    }


    int getSiblingCare() {
        int scoreValue = 0;
        scoreValue += getScoreValue(takeSiblingsToSchool16);
        scoreValue += getScoreValue(lookAfterSiblingsWithAdult17);
        scoreValue += getScoreValue(lookAfterSiblingsOnOwn18);
        return scoreValue;
    }

    String getsiblingCareText() {
        return String.format(Locale.UK, "%d", getSiblingCare());
    }


    int getFinancialPracticalManagement() {
        int scoreValue = 0;
        scoreValue += getScoreValue(billsBankingBenefits07);
        scoreValue += getScoreValue(workPartTime08);
        scoreValue += getScoreValue(interpretSignOther09);
        return scoreValue;
    }

    String getFinancialPracticalCareText() {
        return String.format(Locale.UK, "%d", getFinancialPracticalManagement());
    }

    public int getScore() {
        int scoreValue = 0;
        if (!cleanOwnBedroom01.equals("Please select") &&
                !cleanOtherRooms02.equals("Please select") &&
                !washDishes03.equals("Please select") &&
                !decorateRoom04.equals("Please select") &&
                !responsibilityForShopping05.equals("Please select") &&
                !carryingHeavyThings06.equals("Please select") &&
                !billsBankingBenefits07.equals("Please select") &&
                !workPartTime08.equals("Please select") &&
                !interpretSignOther09.equals("Please select") &&
                !helpDressUndress10.equals("Please select") &&
                !helpWash11.equals("Please select") &&
                !helpBathShower12.equals("Please select") &&
                !keepPersonCompany13.equals("Please select") &&
                !makeSureAlright14.equals("Please select") &&
                !takeOut15.equals("Please select") &&
                !takeSiblingsToSchool16.equals("Please select") &&
                !lookAfterSiblingsWithAdult17.equals("Please select") &&
                !lookAfterSiblingsOnOwn18.equals("Please select")) {
            scoreValue += getDomesticActivity();
            scoreValue += getHouseholdManagement();
            scoreValue += getPersonalCare();
            scoreValue += getEmotionalCare();
            scoreValue += getSiblingCare();
            scoreValue += getFinancialPracticalManagement();
        } else {
            scoreValue = 0;
        }
        return scoreValue;
    }

    public String getScoreText() {
        return String.format(Locale.UK, "%d", getScore());
    }

    private int getScoreValue(String textValue) {
        switch (textValue) {
            case "Never":
                return 0;
            case "Some of the time":
                return 1;
            case "A lot of the time":
                return 2;
            default:
                throw new CRISException(String.format("Unexpected value: %s", textValue));
        }
    }

    private String getScoreInterpretation(int score) {
        if (score >= 18) {
            return "Very high amount of caring activity";
        } else if (score >= 14) {
            return "High amount of caring activity";
        } else if (score >= 10) {
            return "Moderate amount of caring activity";
        } else if (score >= 1) {
            return "Low amount of caring activity";
        } else {
            return "No caring activity recorded";
        }
    }

    // Save the document
    public void save(boolean isNewMode) {
        LocalDB localDB = LocalDB.getInstance();

        // Load the Document fields
        int score = getScore();
        setSummary(String.format(Locale.UK, "Score - %d %s", score, getScoreInterpretation(score)));

        localDB.save(this, isNewMode, User.getCurrentUser());
    }

    // No point in searching for Never etc.
    public boolean search(String searchText) {
        return false;
    }

    private String displayBoolean(boolean value) {
        if (value) return "Yes";
        else return "No";
    }

    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        String summary = super.textSummary();
        int domesticActivity = getDomesticActivity();
        int householdManagement = getHouseholdManagement();
        int personalCare = getPersonalCare();
        int emotionalCare = getEmotionalCare();
        int siblingCare = getSiblingCare();
        int financialPracticalManagement = getFinancialPracticalManagement();
        int score = getScore();
        summary += "MACA - YC18\n";
        summary += "Multidimensional Assessment of Caring Activities\n";
        summary += "Joseph, S, Becker, F and Becker, S (2012) Manual for Measures of Caring Activities and Outcomes for Children and Young People (2nd edition) London: Carers Trust\n\n";
        summary += "Date: " + sDate.format(getReferenceDate()) + "\n\n";
        summary += String.format(Locale.UK, "Domestic Activity - Subscale score: %d\n", domesticActivity);
        summary += String.format(Locale.UK, "Household Management - Subscale score: %d\n", householdManagement);
        summary += String.format(Locale.UK, "Personal Care - Subscale score: %d\n", personalCare);
        summary += String.format(Locale.UK, "Emotional Care - Subscale score: %d\n", emotionalCare);
        summary += String.format(Locale.UK, "Sibling Care - Subscale score: %d\n", siblingCare);
        summary += String.format(Locale.UK, "Financial and Practical Management - Subscale score: %d\n\n", financialPracticalManagement);
        summary += String.format(Locale.UK, "Score: $d %s\n\n", score, getScoreInterpretation(score));
        if (provideHelpToMother) {
            summary += String.format("Care (Mother): %s\n",
                    displayBoolean(provideHelpToMother));
        }
        if (provideHelpToStepMother) {
            summary += String.format("Care (StepMother): %s\n",
                    displayBoolean(provideHelpToStepMother));
        }
        if (provideHelpToFather) {
            summary += String.format("Care (Father): %s\n",
                    displayBoolean(provideHelpToFather));
        }
        if (provideHelpToStepFather) {
            summary += String.format("Care (StepFather): %s\n",
                    displayBoolean(provideHelpToStepFather));
        }
        if (provideHelpToBrothers > 0) {
            summary += String.format("Number of Brothers Cared For: %d\n", provideHelpToBrothers);
        }
        if (provideHelpToSisters > 0) {
            summary += String.format("Number of Sisters Cared For: %d\n", provideHelpToSisters);
        }
        if (provideHelpToGrandparents > 0) {
            summary += String.format("Number of Grandparents Cared For: %d\n", provideHelpToGrandparents);
        }
        if (provideHelpToOtherRelative.length() > 0) {
            summary += String.format("Care (Other Relative): %s\n", provideHelpToOtherRelative);
        }
        if (provideHelpToFamilyFriend) {
            summary += String.format("Care (Family Friend): %s\n",
                    displayBoolean(provideHelpToFamilyFriend));
        }
        if (provideHelpToOther.length() > 0) {
            summary += String.format("Care (Other): %s\n", provideHelpToOther);
        }
        summary += String.format("Person cared for needs help because: %s\n", reasonNeedHelp);
        caringSubstanceMisuse = false;
        if (caringSubstanceMisuse) {
            summary += "I am caring for someone with a substance misuse problem.\n";
        }
        if (caringPhysicalDisability) {
            summary += "I am caring for someone with a physical disability.\n";
        }
        if (caringLearningDisability) {
            summary += "I am caring for someone with a learning disability.\n";
        }
        if (caringLifeLimiting) {
            summary += "I am caring for someone with a life limiting condition.\n";
        }
        if (caringMentalHealth) {
            summary += "I am caring for someone with a mental health illness.\n";
        }
        if (caringLGBT) {
            summary += "I am caring for someone from the lesbian, gay, bisexual or transgender community.\n";
        }
        if (caringTraveller) {
            summary += "I am caring for someone from a travellers community. \n";
        }
        if (caringRural) {
            summary += "I am caring for someone from a rural community \n";
        }
        if (caringEthnicMinority) {
            summary += "I am caring for someone from an ethnic minority community \n";
        }
        summary += "The caring jobs:\n";
        summary += "Clean your own bedroom: " + cleanOwnBedroom01 + "\n";
        summary += "Clean other rooms: " + cleanOtherRooms02 + "\n";
        summary += "Wash up dishes or put dishes in a dishwasher: " + washDishes03 + "\n";
        summary += "Decorate rooms: " + decorateRoom04 + "\n";
        summary += "Take responsibility for shopping for food: " + responsibilityForShopping05 + "\n";
        summary += "Help with lifting or carrying heavy things: " + carryingHeavyThings06 + "\n";
        summary += "Help with financial matters such as dealing with bills, banking money, collecting benefits: " + billsBankingBenefits07 + "\n";
        summary += "Work part time to bring money in: " + workPartTime08 + "\n";
        summary += "Interpret, sign or use another communication system for the person you care for: " + interpretSignOther09 + "\n";
        summary += "Help the person you care for to dress of undress: " + helpDressUndress10 + "\n";
        summary += "Help the person you care for to have a wash: " + helpWash11 + "\n";
        summary += "Help the person you care for to have a batch or shower: " + helpBathShower12 + "\n";
        summary += "Keep the person you care for company e.g. sitting with them, reading to them, talking to them: " + keepPersonCompany13 + "\n";
        summary += "Keep an eye on the person you care for to make sure they are alright: " + makeSureAlright14 + "\n";
        summary += "Take th eperson you care for out e.g. for a walk or to see friends or relatives: " + takeOut15 + "\n";
        summary += "Take brothers or sisters to school: " + takeSiblingsToSchool16 + "\n";
        summary += "Look after brothers or sisters whist another adult is nearby: " + lookAfterSiblingsWithAdult17 + "\n";
        summary += "Look afetr brothers or sisters on your own: " + lookAfterSiblingsOnOwn18 + "\n";
        return summary;
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action) {
        MACAYC18 previousDocument = (MACAYC18) localDB.getDocumentByRecordID(previousRecordID);
        MACAYC18 thisDocument = (MACAYC18) localDB.getDocumentByRecordID(thisRecordID);
        String changes = Document.getChanges(previousDocument, thisDocument);
        changes += CRISUtil.getChanges(previousDocument.isProvideHelpToMother(), thisDocument.isProvideHelpToMother(), "Provide Help To Mother");
        changes += CRISUtil.getChanges(previousDocument.isProvideHelpToStepMother(), thisDocument.isProvideHelpToStepMother(), "Provide Help To StepMother");
        changes += CRISUtil.getChanges(previousDocument.isProvideHelpToFather(), thisDocument.isProvideHelpToFather(), "Provide Help To Father");
        changes += CRISUtil.getChanges(previousDocument.isProvideHelpToStepFather(), thisDocument.isProvideHelpToStepFather(), "Provide Help To StepFather");
        changes += CRISUtil.getChanges(previousDocument.getProvideHelpToBrothers(), thisDocument.getProvideHelpToBrothers(), "Provide Help To Brothers");
        changes += CRISUtil.getChanges(previousDocument.getProvideHelpToSisters(), thisDocument.getProvideHelpToSisters(), "Provide Help To Sisters");
        changes += CRISUtil.getChanges(previousDocument.getProvideHelpToGrandparents(), thisDocument.getProvideHelpToGrandparents(), "Provide Help To Grandparents");
        changes += CRISUtil.getChanges(previousDocument.getProvideHelpToOtherRelative(), thisDocument.getProvideHelpToOtherRelative(), "Other Relative Detail");
        changes += CRISUtil.getChanges(previousDocument.isProvideHelpToFamilyFriend(), thisDocument.isProvideHelpToFamilyFriend(), "Provide Help To FamilyFriend");
        changes += CRISUtil.getChanges(previousDocument.getProvideHelpToOther(), thisDocument.getProvideHelpToOther(), "Other Detail");
        changes += CRISUtil.getChanges(previousDocument.getReasonNeedHelp(), thisDocument.getReasonNeedHelp(), "Reason Need Help");
        changes += CRISUtil.getChanges(previousDocument.isCaringSubstanceMisuse(), thisDocument.isCaringSubstanceMisuse(), " Caring (Substance Misuse)");
        changes += CRISUtil.getChanges(previousDocument.isCaringPhysicalDisability(), thisDocument.isCaringPhysicalDisability(), "Caring (Physical Disability)");
        changes += CRISUtil.getChanges(previousDocument.isCaringLearningDisability(), thisDocument.isCaringLearningDisability(), "Caring (Learning Disability)");
        changes += CRISUtil.getChanges(previousDocument.isCaringLifeLimiting(), thisDocument.isCaringLifeLimiting(), "Caring (Life Limiting)");
        changes += CRISUtil.getChanges(previousDocument.isCaringMentalHealth(), thisDocument.isCaringMentalHealth(), "Caring (Mental Health)");
        changes += CRISUtil.getChanges(previousDocument.isCaringLGBT(), thisDocument.isCaringLGBT(), "Caring (LGBT)");
        changes += CRISUtil.getChanges(previousDocument.isCaringTraveller(), thisDocument.isCaringTraveller(), "Caring (Traveller)");
        changes += CRISUtil.getChanges(previousDocument.isCaringRural(), thisDocument.isCaringRural(), "Caring (Rural)");
        changes += CRISUtil.getChanges(previousDocument.isCaringEthnicMinority(), thisDocument.isCaringEthnicMinority(), "Caring (Ethnic Minority)");
        changes += CRISUtil.getChanges(previousDocument.getCleanOwnBedroom01(), thisDocument.getCleanOwnBedroom01(), "Clean Own Bedroom");
        changes += CRISUtil.getChanges(previousDocument.getCleanOtherRooms02(), thisDocument.getCleanOtherRooms02(), "Clean Other Rooms");
        changes += CRISUtil.getChanges(previousDocument.getWashDishes03(), thisDocument.getWashDishes03(), "Wash Dishes");
        changes += CRISUtil.getChanges(previousDocument.getDecorateRoom04(), thisDocument.getDecorateRoom04(), "Decorate Room");
        changes += CRISUtil.getChanges(previousDocument.getResponsibilityForShopping05(), thisDocument.getResponsibilityForShopping05(), "Responsibility For Shopping");
        changes += CRISUtil.getChanges(previousDocument.getCarryingHeavyThings06(), thisDocument.getCarryingHeavyThings06(), "Carrying Heavy Things");
        changes += CRISUtil.getChanges(previousDocument.getBillsBankingBenefits07(), thisDocument.getBillsBankingBenefits07(), "Bills Banking or Benefits");
        changes += CRISUtil.getChanges(previousDocument.getWorkPartTime08(), thisDocument.getWorkPartTime08(), "Work Part Time)");
        changes += CRISUtil.getChanges(previousDocument.getInterpretSignOther09(), thisDocument.getInterpretSignOther09(), "Interpret Sign Other09");
        changes += CRISUtil.getChanges(previousDocument.getHelpDressUndress10(), thisDocument.getHelpDressUndress10(), "Help Dress Undress");
        changes += CRISUtil.getChanges(previousDocument.getHelpWash11(), thisDocument.getHelpWash11(), "Help Wash");
        changes += CRISUtil.getChanges(previousDocument.getHelpBathShower12(), thisDocument.getHelpBathShower12(), "Help Bath Shower");
        changes += CRISUtil.getChanges(previousDocument.getKeepPersonCompany13(), thisDocument.getKeepPersonCompany13(), "Keep Person Company");
        changes += CRISUtil.getChanges(previousDocument.getMakeSureAlright14(), thisDocument.getMakeSureAlright14(), "Make Sure Alright");
        changes += CRISUtil.getChanges(previousDocument.getTakeOut15(), thisDocument.getTakeOut15(), "Take Out");
        changes += CRISUtil.getChanges(previousDocument.getTakeSiblingsToSchool16(), thisDocument.getTakeSiblingsToSchool16(), "Take Siblings To School");
        changes += CRISUtil.getChanges(previousDocument.getLookAfterSiblingsWithAdult17(), thisDocument.getLookAfterSiblingsWithAdult17(), "Look After Siblings With Adult");
        changes += CRISUtil.getChanges(previousDocument.getLookAfterSiblingsOnOwn18(), thisDocument.getLookAfterSiblingsOnOwn18(), "Look After Siblings On Own");
        changes += CRISUtil.getChanges(previousDocument.getScore(), thisDocument.getScore(), "Score");
        if (changes.length() == 0) {
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
    }

    /*
    private String displayBoolean(boolean value) {
        if (value) return "Yes";
        else return "No";
    }
*/
    private static List<Object> getExportFieldNames() {
        List<Object> fNames = new ArrayList<>();
        fNames.add("Completion Date");
        fNames.add("Gender");
        fNames.add("Initials");
        fNames.add("Date of Birth");
        fNames.add("Help Mother");
        fNames.add("Help Step Mother");
        fNames.add("Help Father");
        fNames.add("Help Step Father");
        fNames.add("Help Brothers");
        fNames.add("Num Brothers");
        fNames.add("Help Sisters");
        fNames.add("Num Sisters");
        fNames.add("Help Grandparents");
        fNames.add("Num Grandparents");
        fNames.add("Help Other Rel");
        fNames.add("Other Rel Detail");
        fNames.add("Help Family Friend");
        fNames.add("Help Other");
        fNames.add("Other Detail");
        fNames.add("Why Need Help");
        fNames.add("Caring Subst Misuse");
        fNames.add("Caring Physical Dis");
        fNames.add("Caring Learning Dis");
        fNames.add("Caring Life Limiting");
        fNames.add("Caring Mental Health");
        fNames.add("Caring LGBT Community");
        fNames.add("Caring Traveller");
        fNames.add("Caring Rural");
        fNames.add("Caring Eth Min");
        fNames.add("Ethnicity");
        fNames.add("Clean Own Bedroom");
        fNames.add("Clean Other Rooms");
        fNames.add("Wash Dishes");
        fNames.add("Decorate Room");
        fNames.add("Responsibility For Shopping");
        fNames.add("Carrying Heavy Things");
        fNames.add("Bills Banking Benefits");
        fNames.add("Work Part Time");
        fNames.add("Interpret Sign Other");
        fNames.add("Help Dress Undress");
        fNames.add("Help Wash");
        fNames.add("Help Bath Shower");
        fNames.add("Keep Person Company");
        fNames.add("Make Sure Alright");
        fNames.add("Take Out");
        fNames.add("Take Siblings To School");
        fNames.add("Look After Siblings With Adult");
        fNames.add("Look After Siblings On Own");
        fNames.add("Score");
        return fNames;
    }

    public static List<List<Object>> getMACAYC18Data(ArrayList<Document> documents) {
        LocalDB localDB = LocalDB.getInstance();
        Client client = null;
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames());
        for (Document document : documents) {
            if (client == null || document.getClientID() != client.getClientID()) {
                // New client
                client = (Client) localDB.getDocument(document.getClientID());
            }
            MACAYC18 thisDocument = (MACAYC18) document;
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
                                .setPixelSize(130))
                        .setRange(new DimensionRange()
                                .setSheetId(sheetID)
                                .setDimension("COLUMNS")
                                .setStartIndex(0)
                                .setEndIndex(48))
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
        // 1st column is a date
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
                                .setStartColumnIndex(0)
                                .setEndColumnIndex(1)
                                .setStartRowIndex(1))));
        // 4th column is a date
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
                                // Build 139 - Adding Year Group to Export shifts column to right
                                .setStartColumnIndex(3)
                                .setEndColumnIndex(4)
                                .setStartRowIndex(1))));
        return requests;
    }

    private String displayExportBoolean(boolean value) {
        if (value) return "Yes";
        else return "No";
    }

    public List<Object> getExportData(Client client) {
        LocalDB localDB = LocalDB.getInstance();
        SimpleDateFormat sDate = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        List<Object> row = new ArrayList<>();

        if (getReferenceDate().getTime() != Long.MIN_VALUE) {
            row.add(sDate.format(getReferenceDate()));
        } else {
            row.add(" ");
        }
        row.add(client.getGender().getItemValue());
        String initials = "";
        if (client.getFirstNames().length() > 0) {
            initials += client.getFirstNames().substring(0, 1);
        }
        if (client.getLastName().length() > 0) {
            initials += client.getLastName().substring(0, 1);
        }
        row.add(initials);
        row.add(sDate.format(client.getDateOfBirth()));
        row.add(displayExportBoolean(provideHelpToMother));
        row.add(displayExportBoolean(provideHelpToStepMother));
        row.add(displayExportBoolean(provideHelpToFather));
        row.add(displayExportBoolean(provideHelpToStepFather));
        if (provideHelpToBrothers > 0) {
            row.add("Yes");
            row.add(String.format("%d", provideHelpToBrothers));
        } else {
            row.add("No");
            row.add("0");
        }
        if (provideHelpToSisters > 0) {
            row.add("Yes");
            row.add(String.format("%d", provideHelpToSisters));
        } else {
            row.add("No");
            row.add("0");
        }
        if (provideHelpToGrandparents > 0) {
            row.add("Yes");
            row.add(String.format("%d", provideHelpToGrandparents));
        } else {
            row.add("No");
            row.add("0");
        }
        if (provideHelpToOtherRelative.length() > 0) {
            row.add("Yes");
            row.add(provideHelpToOtherRelative);
        } else {
            row.add("No");
            row.add(" ");
        }
        row.add(displayExportBoolean(provideHelpToFamilyFriend));
        if (provideHelpToOther.length() > 0) {
            row.add("Yes");
            row.add(provideHelpToOther);
        } else {
            row.add("No");
            row.add(" ");
        }


        row.add(reasonNeedHelp);


        row.add(displayExportBoolean(caringSubstanceMisuse));
        row.add(displayExportBoolean(caringPhysicalDisability));
        row.add(displayExportBoolean(caringLearningDisability));
        row.add(displayExportBoolean(caringLifeLimiting));
        row.add(displayExportBoolean(caringMentalHealth));
        row.add(displayExportBoolean(caringLGBT));
        row.add(displayExportBoolean(caringTraveller));
        row.add(displayExportBoolean(caringRural));
        row.add(displayExportBoolean(caringEthnicMinority));
        row.add(client.getEthnicity().getItemValue());

        row.add(cleanOwnBedroom01);
        row.add(cleanOtherRooms02);
        row.add(washDishes03);
        row.add(decorateRoom04);
        row.add(responsibilityForShopping05);
        row.add(carryingHeavyThings06);
        row.add(billsBankingBenefits07);
        row.add(workPartTime08);
        row.add(interpretSignOther09);
        row.add(helpDressUndress10);
        row.add(helpWash11);
        row.add(helpBathShower12);
        row.add(keepPersonCompany13);
        row.add(makeSureAlright14);
        row.add(takeOut15);
        row.add(takeSiblingsToSchool16);
        row.add(lookAfterSiblingsWithAdult17);
        row.add(lookAfterSiblingsOnOwn18);
        // Score
        row.add(getScore());
        return row;

    }

}
