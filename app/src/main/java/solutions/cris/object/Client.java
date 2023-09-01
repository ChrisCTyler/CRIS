package solutions.cris.object;

import android.app.Activity;
import android.content.Context;

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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;
import solutions.cris.utils.CRISExport;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.LocalSettings;
import solutions.cris.utils.SwipeDetector;

import static solutions.cris.object.Case.AMBER;
import static solutions.cris.object.Case.GREEN;
import static solutions.cris.object.Case.RED;

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
public class Client extends Document implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_CLIENT;

    public Client(User currentUser) {
        // This is a new client so allocate a new Client UUID which will be used
        // by all subsequent documents created for this client
        super(currentUser, UUID.randomUUID(), Document.Client);
        // For the client document, the DocumentID andf ClientID need to be the same
        this.setClientID(this.getDocumentID());
    }

    private Date dateOfBirth;

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        this.setReferenceDate(dateOfBirth);     // The reference date for the Client doc is the DOB
    }

    // Age is derived both from DOB and today's date so is always invalid
    // in the database (stored as Int.MIN)
    private int age = Integer.MIN_VALUE;

    public void setAge(int age) {
        this.age = age;
    }

    public int getAge() {
        // V2.0 added age for previous de-serialisations will return 0 not Integer.MIN
        if (age == Integer.MIN_VALUE || age == 0) {
            // Calculate client's age
            Calendar dob = Calendar.getInstance();
            dob.setTime(CRISUtil.midnightEarlier(dateOfBirth));
            Calendar now = Calendar.getInstance();
            now.setTime(CRISUtil.midnightEarlier(new Date()));
            age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            int monthDOB = dob.get(Calendar.MONTH);
            int monthNow = now.get(Calendar.MONTH);
            if (monthNow < monthDOB) {
                age--;
            } else if (monthNow == monthDOB) {
                int dayDOB = dob.get(Calendar.DAY_OF_MONTH);
                int dayToday = now.get(Calendar.DAY_OF_MONTH);
                if (dayToday < dayDOB) {
                    age--;
                } else if (dayDOB == dayToday) {
                    isBirthday = true;
                }
            }
        }
        return age;
    }

    // Build 139 - Add Year Group to Export
    public int getYearGroup() {
        // Changes for DoB 1st September
        // Year Group 1 = 5 years old on or before 1st Sept
        // Range  is 1 to 14, if child under 5 then 0 if over 19 then 99
        int ageSept;
        // Calculate client's age on 1st September
        Calendar dob = Calendar.getInstance();
        dob.setTime(CRISUtil.midnightEarlier(dateOfBirth));
        // Get year of last september
        Calendar today = Calendar.getInstance();
        today.setTime(CRISUtil.midnightEarlier(new Date()));
        int yearStart = today.get(Calendar.YEAR);
        if (today.get(Calendar.MONTH) < 8) {
            {
                yearStart--;
            }
        }
        int yearGroup = yearStart - dob.get(Calendar.YEAR);
        if (dob.get(Calendar.MONTH) >= 8) {
            yearGroup--;
        }
        if (yearGroup < 5) {
            yearGroup = 0;
        } else {
            yearGroup = yearGroup - 4;
        }
        if (yearGroup > 14) {
            yearGroup = 99;
        }
        return yearGroup;
    }

    private boolean isBirthday;

    public boolean isBirthday() {
        return isBirthday;
    }

    private String firstNames;

    public String getFirstNames() {
        return firstNames;
    }

    public void setFirstNames(String firstNames) {
        this.firstNames = firstNames;
    }

    private String lastName;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        // For partial User created for ListUsers only the FirstName is used
        String localFullName = firstNames;
        if (lastName != null) {
            localFullName += " " + lastName;
        }
        return localFullName;
    }

    private String address;

    // Build 188 - Address is now non-mandatory
    public String getAddress() {
        if (address == null) {
            return "";
        } else {
            return address;
        }
    }

    public void setAddress(String address) {
        this.address = address;
    }

    private String postcode;

    // Build 188 - Postcode is now non-mandatory
    public String getPostcode() {
        if (postcode == null) {
            return "";
        } else {
            return postcode;
        }
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    private String contactNumber;

    public String getContactNumber() {
        if (contactNumber == null) {
            return "";
        } else {
            return contactNumber;
        }
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    private String contactNumber2;

    public String getContactNumber2() {
        if (contactNumber2 == null) {
            return "";
        } else {
            return contactNumber2;
        }
    }

    public void setContactNumber2(String contactNumber2) {
        this.contactNumber2 = contactNumber2;
    }

    private String emailAddress;

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    private UUID genderID;
    private ListItem gender;

    public UUID getGenderID() {
        return genderID;
    }

    public void setGenderID(UUID genderID) {
        this.genderID = genderID;
    }

    public ListItem getGender() {
        if (genderID != null && gender == null) {
            LocalDB localDB = LocalDB.getInstance();
            gender = localDB.getListItem(genderID);
        }
        // Build 171 - Handle the unexpected null ListItem ID case
        if (gender == null) {
            gender = new ListItem(User.getCurrentUser(), ListType.GENDER, "Unknown", 0);
        }
        return gender;
    }

    public void setGender(ListItem gender) {
        this.gender = gender;
    }

    private UUID ethnicityID;
    private ListItem ethnicity;

    public UUID getEthnicityID() {
        return ethnicityID;
    }

    public void setEthnicityID(UUID ethnicityID) {
        this.ethnicityID = ethnicityID;
    }

    public ListItem getEthnicity() {
        // Build 171 - Handle the unexpected null ListItemID case
        if (ethnicityID != null && ethnicity == null) {
            LocalDB localDB = LocalDB.getInstance();
            ethnicity = localDB.getListItem(ethnicityID);
        }
        if (ethnicity == null) {
            ethnicity = new ListItem(User.getCurrentUser(), ListType.ETHNICITY, "Unknown", 0);
        }
        return ethnicity;
    }

    public void setEthnicity(ListItem ethnicity) {
        this.ethnicity = ethnicity;
    }

    private UUID currentToolID;
    private Document currentTool;

    public UUID getCurrentToolID() {
        return currentToolID;
    }

    public void setCurrentToolID(UUID currentToolID) {
        this.currentToolID = currentToolID;
    }

    public Document getCurrentTool() {
        if (currentToolID == null) {
            currentTool = null;
        } else if (currentTool == null) {
            LocalDB localDB = LocalDB.getInstance();
            currentTool = localDB.getDocument(currentToolID);
        }
        return currentTool;
    }

    public void setCurrentTool(Document currentTool) {
        this.currentTool = currentTool;
    }

    private UUID currentCaseID;
    private Case currentCase;

    public UUID getCurrentCaseID() {
        return currentCaseID;
    }

    public void setCurrentCaseID(UUID currentCaseID) {
        this.currentCaseID = currentCaseID;
    }

    public Case getCurrentCase() {
        if (currentCaseID == null) {
            currentCase = null;
        } else if (currentCase == null) {
            LocalDB localDB = LocalDB.getInstance();
            currentCase = (Case) localDB.getDocument(currentCaseID);
        }
        return currentCase;
    }

    public void setCurrentCase(Case currentCase) {
        this.currentCase = currentCase;
    }

    private UUID startcaseID;
    private Case startCase;

    public UUID getStartcaseID() {
        return startcaseID;
    }

    public void setStartcaseID(UUID startcaseID) {
        this.startcaseID = startcaseID;
    }

    public solutions.cris.object.Case getStartCase() {
        if (startcaseID == null) {
            startCase = null;
        } else if (startCase == null) {
            LocalDB localDB = LocalDB.getInstance();
            startCase = (Case) localDB.getDocument(startcaseID);
        }
        return startCase;
    }

    public void setStartCase(Case startCase) {
        this.startCase = startCase;
    }

    private Date latestDocument;

    public Date getLatestDocument() {
        return latestDocument;
    }

    public void setLatestDocument(Date latestDocument) {
        this.latestDocument = latestDocument;
    }

    private UUID currentSchoolID;
    private Contact currentSchool;

    public UUID getCurrentSchoolID() {
        return currentSchoolID;
    }

    public void setCurrentSchoolID(UUID currentSchoolID) {
        this.currentSchoolID = currentSchoolID;
    }

    public solutions.cris.object.Contact getCurrentSchool() {
        if (currentSchoolID == null) {
            currentSchool = null;
        } else if (currentSchool == null) {
            LocalDB localDB = LocalDB.getInstance();
            currentSchool = (Contact) localDB.getDocument(currentSchoolID);
        }
        return currentSchool;
    }

    public void setCurrentSchool(solutions.cris.object.Contact currentSchool) {
        this.currentSchool = currentSchool;
    }

    private UUID currentAgencyID;
    private Contact currentAgency;

    public UUID getCurrentAgencyID() {
        return currentAgencyID;
    }

    public void setCurrentAgencyID(UUID currentAgencyID) {
        this.currentAgencyID = currentAgencyID;
    }

    public solutions.cris.object.Contact getCurrentAgency() {
        if (currentAgencyID == null) {
            currentAgency = null;
        } else if (currentAgency == null) {
            LocalDB localDB = LocalDB.getInstance();
            currentAgency = (Contact) localDB.getDocument(currentAgencyID);
        }
        return currentAgency;
    }

    public void setCurrentAgency(solutions.cris.object.Contact currentAgency) {
        this.currentAgency = currentAgency;
    }

    private long daysToFirstSession;

    public long getDaysToFirstSession() {
        return daysToFirstSession;
    }

    public void setDaysToFirstSession(long daysToFirstSession) {
        this.daysToFirstSession = daysToFirstSession;
    }

    private long daysToFirstAttendedSession;

    public long getDaysToFirstAttendedSession() {
        return daysToFirstAttendedSession;
    }

    public void setDaysToFirstAttendedSession(long daysToFirstAttendedSession) {
        this.daysToFirstAttendedSession = daysToFirstAttendedSession;
    }

    private boolean transportRequired;

    public boolean isTransportRequired() {
        return transportRequired;
    }

    public void setTransportRequired(boolean transportRequired) {
        this.transportRequired = transportRequired;
    }

    private String transportRequiredType;

    public String getTransportRequiredType() {
        // V1.5.073 New field so can be null in de-serialised objects
        if (transportRequiredType == null) return "";
        else return transportRequiredType;
    }

    public void setTransportRequiredType(String transportRequiredType) {
        this.transportRequiredType = transportRequiredType;
    }

    private int transportBooked;

    public int getTransportBooked() {
        return transportBooked;
    }

    public void setTransportBooked(int transportBooked) {
        this.transportBooked = transportBooked;
    }

    private int transportUsed;

    public int getTransportUsed() {
        return transportUsed;
    }

    public void setTransportUsed(int transportUsed) {
        this.transportUsed = transportUsed;
    }

    private int sessionsOffered;

    public int getSessionsOffered() {
        return sessionsOffered;
    }

    public void setSessionsOffered(int sessionsOffered) {
        this.sessionsOffered = sessionsOffered;
    }

    private int sessionsAttended;

    public int getSessionsAttended() {
        return sessionsAttended;
    }

    public void setSessionsAttended(int sessionsAttended) {
        this.sessionsAttended = sessionsAttended;
    }

    private int sessionsCancelled;

    public int getSessionsCancelled() {
        return sessionsCancelled;
    }

    public void setSessionsCancelled(int sessionsCancelled) {
        this.sessionsCancelled = sessionsCancelled;
    }

    private int sessionsDNA;

    public int getSessionsDNA() {
        return sessionsDNA;
    }

    public void setSessionsDNA(int sessionsDNA) {
        this.sessionsDNA = sessionsDNA;
    }

    // Build 201 - Added Attendance points to session and score here.
    // Calculation method changed from load in ListClientDocuments to the method
    // client.loadSessionExportData()
    private int attendanceScore;

    public int getAttendanceScore() {
        return attendanceScore;
    }

    public void setAttendanceScore(int attendanceScore) {
        this.attendanceScore = attendanceScore;
    }

    public String lastEntry() {
        String duration = "Unknown";
        if (latestDocument != null) {
            Date today = new Date();
            long time = today.getTime() - latestDocument.getTime();
            long minutes = time / (1000 * 60);
            if (minutes < 61) {
                duration = String.format(Locale.UK, "%d minutes ago", minutes);
            } else {
                long hours = minutes / 60;
                if (hours < 25) {
                    duration = String.format(Locale.UK, "%d hours ago", hours);
                } else {
                    long days = hours / 24;
                    if (days < 2) {
                        duration = String.format(Locale.UK, "%d day ago", days);
                    } else if (days < 8) {
                        duration = String.format(Locale.UK, "%d days ago", days);
                    } else {
                        long weeks = days / 7;
                        if (weeks < 2) {
                            duration = String.format(Locale.UK, "%d week ago", weeks);
                        } else if (weeks < 5) {
                            duration = String.format(Locale.UK, "%d weeks ago", weeks);
                        } else {
                            long months = days / 30;
                            if (months < 2) {
                                duration = String.format(Locale.UK, "%d month ago", months);
                            } else if (months < 13) {
                                duration = String.format(Locale.UK, "%d months ago", months);
                            } else {
                                long years = days / 365;
                                if (years < 2) {
                                    duration = String.format(Locale.UK, "%d years ago", years);
                                } else {
                                    duration = String.format(Locale.UK, "%d years ago", years);
                                }
                            }
                        }
                    }
                }
            }
        }
        return duration;
    }

    public void clear() {
        setCurrentAgency(null);
        setCurrentSchool(null);
        setGender(null);
        setEthnicity(null);
        setCurrentTool(null);
        setCurrentCase(null);
        setStartCase(null);
        // Don't restore the age since it may not have been calculated
        // and will get calculated ion first call to getAge()
        setAge(Integer.MIN_VALUE);
        isBirthday = false;
    }

    public void save(boolean isNewMode) {
        LocalDB localDB = LocalDB.getInstance();
        Contact currentAgency = getCurrentAgency();
        Contact currentSchool = getCurrentSchool();
        ListItem gender = getGender();
        ListItem ethnicity = getEthnicity();
        Document tool = getCurrentTool();
        Case currentCase = getCurrentCase();
        Case startCase = getStartCase();
        clear();

        setReferenceDate(getDateOfBirth());
        if (getCurrentCase() == null) {
            setSummary("New Case.");
        } else {
            String tier = "No Tier";
            String group = "No Group";
            String keyworkerName = "No Keyworker";
            String keyworkerContact = "";
            if (currentCase.getTier() != null) {
                tier = currentCase.getTier().getItemValue();
            }
            if (currentCase.getGroup() != null) {
                group = currentCase.getGroup().getItemValue();
            }
            if (currentCase.getKeyWorker() != null) {
                keyworkerName = currentCase.getKeyWorker().getFullName();
                keyworkerContact = currentCase.getKeyWorker().getContactNumber();
            }
            String summaryText = String.format("%s, %s, %s",
                    tier,
                    group,
                    keyworkerName);
            if (!keyworkerContact.isEmpty()) {
                summaryText += String.format(" (%s)", keyworkerContact);
            }
            setSummary(summaryText);
        }


        // Save the client
        localDB.save(this, isNewMode, User.getCurrentUser());

        setCurrentAgency(currentAgency);
        setCurrentSchool(currentSchool);
        setGender(gender);
        setEthnicity(ethnicity);
        setCurrentTool(tool);
        setCurrentCase(currentCase);
        setStartCase(startCase);

    }

    public static Comparator<Client> comparatorLastFirst = new Comparator<Client>() {
        @Override
        public int compare(Client o1, Client o2) {
            if (o1.getLastName().equals(o2.getLastName())) {
                return o1.getFirstNames().compareTo(o2.getFirstNames());
            } else {
                return o1.getLastName().compareTo(o2.getLastName());
            }
        }
    };

    public static Comparator<Client> comparatorFirstLast = new Comparator<Client>() {
        @Override
        public int compare(Client o1, Client o2) {
            if (o1.getFirstNames().equals(o2.getFirstNames())) {
                return o1.getLastName().compareTo(o2.getLastName());
            } else {
                return o1.getFirstNames().compareTo(o2.getFirstNames());
            }
        }
    };

    public static Comparator<Client> comparatorCaseStart = new Comparator<Client>() {
        @Override
        public int compare(Client o1, Client o2) {
            Date o1Start;
            if (o1.getStartCase() != null) {
                o1Start = o1.getStartCase().getReferenceDate();
            } else {
                o1Start = new Date(Long.MAX_VALUE);
            }
            Date o2Start;
            if (o2.getStartCase() != null) {
                o2Start = o2.getStartCase().getReferenceDate();
            } else {
                o2Start = new Date(Long.MAX_VALUE);
            }
            return o2Start.compareTo(o1Start);
        }
    };

    public static Comparator<Client> comparatorAge = new Comparator<Client>() {
        @Override
        public int compare(Client o1, Client o2) {
            return o2.getDateOfBirth().compareTo(o1.getDateOfBirth());
        }
    };

    public static Comparator<Client> comparatorGroup = new Comparator<Client>() {
        @Override
        public int compare(Client o1, Client o2) {
            Case c1 = o1.getCurrentCase();
            String g1 = "*Not allocated";
            if (c1 != null && c1.getGroup() != null) {
                g1 = c1.getGroup().getItemValue();
            }
            Case c2 = o2.getCurrentCase();
            String g2 = "*Not allocated";
            if (c2 != null && c2.getGroup() != null) {
                g2 = c2.getGroup().getItemValue();
            }
            if (g1.equals(g2)) {
                return o1.getLastName().compareTo(o2.getLastName());
            } else {
                return g1.compareTo(g2);
            }
        }
    };

    public static Comparator<Client> comparatorStatus = new Comparator<Client>() {
        @Override
        public int compare(Client o1, Client o2) {
            Case c1 = o1.getCurrentCase();
            Integer s1 = 99;
            if (c1 != null) {
                s1 = c1.getClientStatus();
            }
            Case c2 = o2.getCurrentCase();
            Integer s2 = 99;
            if (c2 != null) {
                s2 = c2.getClientStatus();
            }
            if (s1.equals(s2)) {
                return o1.getLastName().compareTo(o2.getLastName());
            } else {
                return s1.compareTo(s2);
            }
        }
    };

    public static Comparator<Client> comparatorKeyworker = new Comparator<Client>() {
        @Override
        public int compare(Client o1, Client o2) {
            Case c1 = o1.getCurrentCase();
            String k1 = "*Not allocated";
            if (c1 != null && c1.getKeyWorker() != null) {
                k1 = c1.getKeyWorker().getFullName();
            }
            Case c2 = o2.getCurrentCase();
            String k2 = "*Not allocated";
            if (c2 != null && c2.getKeyWorker() != null) {
                k2 = c2.getKeyWorker().getFullName();
            }
            if (k1.equals(k2)) {
                return o1.getLastName().compareTo(o2.getLastName());
            } else {
                return k1.compareTo(k2);
            }
        }
    };

    public static List<Object> getExportFieldNames(LocalSettings localSettings) {
        List<Object> fNames = new ArrayList<>();
        fNames.add("Firstnames");
        fNames.add("Lastname");
        fNames.add("Date of Birth");
        fNames.add("Age");
        // Build 139 - Add Year Group to Export
        fNames.add("Year Group");
        fNames.add("Address");
        fNames.add("Postcode");
        fNames.add("Contact Number");
        fNames.add("Alt. Contact Number");
        fNames.add("Email Address");
        fNames.add("Gender");
        fNames.add("Ethnicity");
        fNames.add("Last Entry (Days)");
        fNames.add("CAT");
        fNames.add("Status");
        fNames.add(localSettings.Group);
        // Build 139 - Second Group
        fNames.add(localSettings.Group + "2");
        fNames.add(localSettings.Keyworker);
        fNames.add(localSettings.Commisioner);
        fNames.add(localSettings.Tier);
        fNames.add("School");
        fNames.add("Agency");
        fNames.add("Case Start date");
        fNames.add("Days to 1st Session");
        fNames.add("Days to 1st Att. Session");
        fNames.add("Case Close Date");
        fNames.add("Transport Required");
        fNames.add("Transport Booked");
        fNames.add("Transport Used");
        fNames.add("Sessions Offered");
        fNames.add("Sessions Attended");
        fNames.add("Sessions Cancelled");
        fNames.add("Sessions DNA");
        fNames.add("Attendance Score");
        return fNames;
    }

    /*
    public static List<List<Object>> getClientData(ArrayList<Client> adapterList,
                                                   LocalSettings localSettings,
                                                   LocalDB localDB,
                                                   Date startDate,
                                                   Date endDate) {
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames(localSettings));
        for (Client client : adapterList) {
            content.add(client.getExportData());
        }
        return content;

    }

     */

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
                                .setStartIndex(4)
                                .setEndIndex(5))
                ));
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
        requests.add(new Request()
                .setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                        .setFields("pixelSize")
                        .setProperties(new DimensionProperties()
                                .setPixelSize(150))
                        .setRange(new DimensionRange()
                                .setSheetId(sheetID)
                                .setDimension("COLUMNS")
                                .setStartIndex(14))
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
        // 20th column is a date (CaseStartdate)
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
                                .setStartColumnIndex(19)
                                .setEndColumnIndex(20)
                                .setStartRowIndex(1))));
        // 23rd column is a date (CaseClosedate)
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
                                .setStartColumnIndex(22)
                                .setEndColumnIndex(23)
                                .setStartRowIndex(1))));
        return requests;
    }

    public List<Object> getExportData() {
        SimpleDateFormat sDate = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);

        List<Object> row = new ArrayList<>();
        row.add(getFirstNames());
        row.add(getLastName());
        row.add(sDate.format(getDateOfBirth()));
        row.add(getAge());
        // Build 139 - Add Year Group to Export
        row.add(getYearGroup());
        row.add(getAddress());
        row.add(getPostcode());
        row.add(String.format("'%s", getContactNumber()));
        row.add(String.format("'%s", getContactNumber2()));
        row.add(getEmailAddress());
        row.add(getGender().getItemValue());
        row.add(getEthnicity().getItemValue());
        if (getLatestDocument() != null) {
            Date today = new Date();
            long time = today.getTime() - getLatestDocument().getTime();
            long days = time / (1000 * 60 * 60 * 24);
            row.add(days);
        } else {
            row.add("");
        }
        // Current Tool
        Document tool = getCurrentTool();

        if (tool != null) {
            // Build 187 - Add MACA as possible tool
            switch (tool.getDocumentType()) {
                case Document.CriteriaAssessmentTool:
                    CriteriaAssessmentTool cat = (CriteriaAssessmentTool) tool;
                    row.add(cat.getScoreText());
                    break;
                case Document.MACAYC18:
                    MACAYC18 macayc18 = (MACAYC18) tool;
                    row.add(macayc18.getScoreText());
            }

        } else {
            row.add("");    // Current Tool
        }
        if (getCurrentCase() != null) {
            switch (getCurrentCase().getClientStatus()) {
                case RED:
                    row.add("red");
                    break;
                case AMBER:
                    row.add("amber");
                    break;
                case GREEN:
                    row.add("green");
                    break;
            }
            if (getCurrentCase().getGroup() != null) {
                row.add(getCurrentCase().getGroup().getItemValue());
            } else {
                row.add("");    // Group
            }
            // Build 139 - Second Group
            if (getCurrentCase().getGroup2() != null) {
                row.add(getCurrentCase().getGroup2().getItemValue());
            } else {
                row.add("");    // Group
            }
            if (getCurrentCase().getKeyWorker() != null) {
                row.add(getCurrentCase().getKeyWorker().getFullName());
            } else {
                row.add("");    // Keyworker
            }
            if (getCurrentCase().getCommissioner() != null) {
                row.add(getCurrentCase().getCommissioner().getItemValue());
            } else {
                row.add("");    // Commissioner
            }
            if (getCurrentCase().getTier() != null) {
                row.add(getCurrentCase().getTier().getItemValue());
            } else {
                row.add("");    // Tier
            }
        } else {
            row.add("");    // Status
            row.add("");    // Group
            row.add("");    // Keyworker
            row.add("");    // Commissioner
            row.add("");    // Tier
        }
        if (getCurrentSchool() != null) {
            if (getCurrentSchool().getSchool() != null) {
                row.add(getCurrentSchool().getSchool().getItemValue());
            } else {
                row.add(String.format("Invalid Contact (No School) - %s", getCurrentSchool().getContactName()));
            }
        } else {
            row.add("");
        }
        if (getCurrentAgency() != null) {
            if (getCurrentAgency().getAgency() != null) {
                row.add(getCurrentAgency().getAgency().getItemValue());
            } else {
                row.add(String.format("Invalid Contact (No Agency) - %s", getCurrentAgency().getContactName()));
            }
        } else {
            row.add("");
        }
        if (getStartCase() != null) {
            row.add(sDate.format(getStartCase().getReferenceDate()));
        } else {
            row.add("");
        }
        if (getDaysToFirstSession() == 0) {
            row.add("");
        } else {
            row.add(String.format(Locale.UK, "%d", getDaysToFirstSession()));
        }
        if (getDaysToFirstAttendedSession() == 0) {
            row.add("");
        } else {
            row.add(String.format(Locale.UK, "%d", getDaysToFirstAttendedSession()));
        }
        if (getCurrentCase() != null && getCurrentCase().getCaseType().equals("Close")) {
            row.add(sDate.format(getCurrentCase().getReferenceDate()));

        } else {
            row.add("");
        }
        if (isTransportRequired()) {
            row.add("TRUE");
        } else {
            row.add("FALSE");
        }
        row.add(String.format(Locale.UK, "%d", getTransportBooked()));
        row.add(String.format(Locale.UK, "%d", getTransportUsed()));
        row.add(String.format(Locale.UK, "%d", getSessionsOffered()));
        row.add(String.format(Locale.UK, "%d", getSessionsAttended()));
        row.add(String.format(Locale.UK, "%d", getSessionsCancelled()));
        row.add(String.format(Locale.UK, "%d", getSessionsDNA()));
        // Build 201 Added Attendance Points
        row.add(String.format(Locale.UK, "%d", getAttendanceScore()));
        return row;
    }

    public boolean search(String searchText) {
        if (searchText.isEmpty()) {
            return true;
        } else {
            boolean found;
            String text = String.format("%s %s %s %s %s %s %s %s",
                    getFirstNames(),
                    getLastName(),
                    getAddress(),
                    getPostcode(),
                    getContactNumber(),
                    getContactNumber2(),
                    getEmailAddress(),
                    getGender().getItemValue(), getEthnicity().getItemValue());
            found = text.toLowerCase().contains(searchText.toLowerCase());
            // Build 158 - Removed currentcase search to speed up search (about 4x)
            //if (!found) {
            // So that search works as expected in ListClients
            //if (currentCase != null) {
            //    found = currentCase.search(searchText);
            //}
            //}
            return found;
        }
    }

    public String shortTextSummary() {
        return "Client Name: " + getFirstNames() + " " + getLastName() + "\n";
    }

    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);

        String summary = super.textSummary();
        summary += "Name: " + getFirstNames() + " " + getLastName() + "\n";
        summary += "Date of Birth: ";
        if (getDateOfBirth().getTime() != Long.MIN_VALUE) {
            summary += sDate.format(getDateOfBirth());
        }
        summary += "\n";
        summary += "Address: " + getAddress() + "\n";
        summary += "Postcode: " + getPostcode() + "\n";
        summary += "Contact Number: " + getContactNumber() + "\n";
        if (getContactNumber2() != null) {
            summary += "Alt. Contact Number: " + getContactNumber2() + "\n";
        }
        summary += "Email Address : " + getEmailAddress() + "\n";
        summary += "Gender: " + getGender().getItemValue() + "\n";
        summary += "Ethnicity: " + getEthnicity().getItemValue() + "\n";
        return summary;
    }


    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action) {
        Client previousDocument = (Client) localDB.getDocumentByRecordID(previousRecordID);
        Client thisDocument = (Client) localDB.getDocumentByRecordID(thisRecordID);
        String changes = Document.getChanges(previousDocument, thisDocument);
        changes += CRISUtil.getChanges(previousDocument.getFullName(), thisDocument.getFullName(), "Name");
        changes += CRISUtil.getChangesDate(previousDocument.getDateOfBirth(), thisDocument.getDateOfBirth(), "DOB");
        changes += CRISUtil.getChanges(previousDocument.getAddress(), thisDocument.getAddress(), "Address");
        changes += CRISUtil.getChanges(previousDocument.getPostcode(), thisDocument.getPostcode(), "Postcode");
        changes += CRISUtil.getChanges(previousDocument.getContactNumber(), thisDocument.getContactNumber(), "Contact Number");
        changes += CRISUtil.getChanges(previousDocument.getContactNumber2(), thisDocument.getContactNumber2(), "Alt. Contact Number");
        changes += CRISUtil.getChanges(previousDocument.getEmailAddress(), thisDocument.getEmailAddress(), "Email Address");
        changes += CRISUtil.getChanges(previousDocument.getGender(), thisDocument.getGender(), "Gender");
        changes += CRISUtil.getChanges(previousDocument.getEthnicity(), thisDocument.getEthnicity(), "Ethnicity");
        changes += CRISUtil.getChanges(previousDocument.getCurrentAgency(), thisDocument.getCurrentAgency(), "Current Agency");
        changes += CRISUtil.getChanges(previousDocument.getCurrentSchool(), thisDocument.getCurrentSchool(), "Current School");
        changes += CRISUtil.getChanges(previousDocument.getCurrentCase(), thisDocument.getCurrentCase());
        if (changes.length() == 0) {
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
    }
}
