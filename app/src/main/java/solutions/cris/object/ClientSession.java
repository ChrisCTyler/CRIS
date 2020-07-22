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

public class ClientSession extends Document implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_CLIENT_SESSION;

    public ClientSession(User currentUser, UUID clientID) {
        super(currentUser, clientID, Document.ClientSession);
        attended = false;
        status = 0;
        //sessionID = null;
        session = null;
    }

    // SessionID added in Database 18
    private UUID sessionID;

    public UUID getSessionID() {
        return sessionID;
    }

    public void setSessionID(UUID sessionID) {
        this.sessionID = sessionID;
    }

    private Session session;

    public Session getSession() {
        if (getSessionID() != null && session == null) {
            LocalDB localDB = LocalDB.getInstance();
            session = (Session) localDB.getDocument(getSessionID());
        }
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    private Client client;

    public Client getClient() {
        if (getClientID() != null && client == null) {
            LocalDB localDB = LocalDB.getInstance();
            client = (Client) localDB.getDocument(getClientID());
        }
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    private boolean attended;

    public boolean isAttended() {
        return attended;
    }

    public void setAttended(boolean attended) {
        this.attended = attended;
    }

    // Build 110
    private boolean reserved;

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    // Status is not used. Use StatusDocument base class status.getScore() instead
    private int status;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void clear() {
        setSession(null);
        setClient(null);
    }

    public void save(boolean isNewMode) {
        LocalDB localDB = LocalDB.getInstance();
        Session session = getSession();
        Client client = getClient();
        clear();

        // Update the summary
        String summaryText = String.format("%s", getSession().getSessionName());
        if (getCancelledFlag()) {
            summaryText += ", Cancelled";
        } else if (isAttended()) {
            summaryText += ", Attended";
        }
        setSummary(summaryText);
        // Following is unnecessary but fixes bug in intital build where Refdate not set
        setReferenceDate(getSession().getReferenceDate());

        localDB.save(this, isNewMode, User.getCurrentUser());

        setSession(session);
        setClient(client);
    }

    public boolean search(String searchText) {
        throw new CRISException("Invalid search() call from ClientSession");
    }

    //
    public String textSummary() {
        LocalDB localDB = LocalDB.getInstance();
        SimpleDateFormat sDate = new SimpleDateFormat("EEE dd MMM yyyy", Locale.UK);
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        // Build the string
        String summary = super.textSummary();
        if (getSession() != null) {
            summary += session.textSummary();
        }
        if (attended) {
            summary += "Attended: YES\n";
        } else {
            summary += "Attended: NO\n";
        }
        // Build 110
        if (reserved) {
            summary += "Reserved: YES\n";
        } else {
            summary += "Reserved: NO\n";
        }
        if (getCancelledFlag()) {
            summary += String.format("Cancelled: YES\n%s\n", getCancellationReason());
        } else {
            summary += "Cancelled: NO\n";
        }
        return summary;
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action) {
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        LocalSettings localSettings = LocalSettings.getInstance();
        ClientSession previousDocument = (ClientSession) localDB.getDocumentByRecordID(previousRecordID);
        ClientSession thisDocument = (ClientSession) localDB.getDocumentByRecordID(thisRecordID);
        String changes = Document.getChanges(previousDocument, thisDocument);
        changes += CRISUtil.getChanges(previousDocument.isAttended(), thisDocument.isAttended(), "Attended");
        changes += CRISUtil.getChanges(previousDocument.isReserved(), thisDocument.isReserved(), "Reserved");
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
        // Build 155 - Add Address to export
        fNames.add("Address");
        fNames.add("Postcode");
        fNames.add("Session Date");
        fNames.add("Attended");
        // Build 110
        fNames.add("Reserved");
        fNames.add("Cancelled");
        fNames.add("Score");
        fNames.add("Group");
        fNames.add("Session");
        fNames.add("Coordinator");
        fNames.add("Postcode");
        fNames.add("Note");
        fNames.add("PdfDocument");
        fNames.add("Transport");
        fNames.add("Booked");
        fNames.add("Outbound");
        fNames.add("Used");
        fNames.add("Return");
        fNames.add("Used");

        return fNames;
    }

    public static List<List<Object>> getClientSessionData(ArrayList<? extends Document> documents) {
        LocalDB localDB = LocalDB.getInstance();
        Client client = null;
        // Find any associated note / pdf
        ArrayList<Document> myWeeks = null;
        ArrayList<Document> notes = null;
        ArrayList<Document> pdfDocuments = null;
        ArrayList<Document> transportDocuments = null;
        // Build the 1st row
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames());
        // Add each document (needs associated note/pdf/transport documents)
        for (Document document : documents) {
            if (client == null || document.getClientID() != client.getClientID()) {
                // New client
                client = (Client) localDB.getDocument(document.getClientID());
                myWeeks = localDB.getAllDocumentsOfType(client.getClientID(), Document.MyWeek);
                notes = localDB.getAllDocumentsOfType(client.getClientID(), Document.Note);
                pdfDocuments = localDB.getAllDocumentsOfType(client.getClientID(), Document.PdfDocument);
                transportDocuments = localDB.getAllDocumentsOfType(client.getClientID(), Document.Transport);
            }
            ClientSession thisDocument = (ClientSession) document;
            MyWeek sessionMyWeek = null;
            if (myWeeks != null) {
                for (Document myWeekDocument : myWeeks) {
                    MyWeek myWeek = (MyWeek) myWeekDocument;
                    if (myWeek.getSessionID() != null &&
                            myWeek.getSessionID().equals(thisDocument.getSessionID())) {
                        sessionMyWeek = myWeek;
                        break;
                    }
                }
            }
            Note sessionNote = null;
            if (notes != null) {
                for (Document noteDocument : notes) {
                    Note note = (Note) noteDocument;
                    if (note.getSessionID() != null &&
                            note.getSessionID().equals(thisDocument.getSessionID())) {
                        sessionNote = note;
                        break;
                    }
                }
            }
            PdfDocument sessionPdf = null;
            if (pdfDocuments != null) {
                for (Document pdfDocument : pdfDocuments) {
                    PdfDocument pdf = (PdfDocument) pdfDocument;
                    if (pdf.getSessionID() != null &&
                            pdf.getSessionID().equals(thisDocument.getSessionID())) {
                        sessionPdf = pdf;
                        break;
                    }
                }
            }
            Transport sessionTransport = null;
            if (transportDocuments != null) {
                for (Document transportDocument : transportDocuments) {
                    Transport transport = (Transport) transportDocument;
                    if (transport.getSessionID() != null &&
                            transport.getSessionID().equals(thisDocument.getSessionID())) {
                        sessionTransport = transport;
                        break;
                    }
                }
            }
            content.add(thisDocument.getExportData(client,
                    sessionMyWeek,
                    sessionNote,
                    sessionPdf,
                    sessionTransport));
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
                                // Build 139 - Adding Year Group to Export shifts column to right
                                // Build 155 - Adding Address to Export shifts column to right
                                .setStartIndex(12)
                                .setEndIndex(15))
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
                                // Build 139 - Adding Year Group to Export shifts column to right
                                // Build 155 - Adding Address to Export shifts column to right
                                .setStartColumnIndex(7)
                                .setEndColumnIndex(8)
                                .setStartRowIndex(1))));
        return requests;
    }

    public List<Object> getExportData(Client client, Status sessionStatus, Note note, PdfDocument pdfDocument, Transport transport) {
        LocalDB localDB = LocalDB.getInstance();
        SimpleDateFormat sDate = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);


        List<Object> row = new ArrayList<>();
        row.add(client.getFirstNames());
        row.add(client.getLastName());
        row.add(sDate.format(client.getDateOfBirth()));
        row.add(client.getAge());
        // Build 139 - Add Year Group to Export
        row.add(client.getYearGroup());
        // Build 155 Added Address to Export
        row.add(client.getAddress());
        row.add(client.getPostcode());
        if (getReferenceDate().getTime() != Long.MIN_VALUE) {
            row.add(sDate.format(getReferenceDate()));
        } else {
            row.add("");
        }
        if (isAttended()) {
            row.add("True");
        } else {
            row.add("False");
        }
        // Build 110
        if (isReserved()) {
            row.add("True");
        } else {
            row.add("False");
        }
        // Build 110
        if (getCancelledFlag()) {
            row.add("True");
        } else {
            row.add("False");
        }
        if (sessionStatus == null) {
            row.add(0);
        } else {
            row.add(sessionStatus.getScore());
        }
        Session session = getSession();
        if (session == null) {
            row.add("Unknown");
            row.add("Unknown");
            row.add("Unknown");
            row.add("Unknown");
        } else {
            row.add(getItemValue(session.getGroup()));
            row.add(session.getSessionName());
            row.add(getFullName(session.getSessionCoordinator()));
            row.add(session.getPostcode());
        }
        if (note == null) {
            row.add("True");
        } else {
            row.add("False");
        }
        if (pdfDocument == null) {
            row.add("True");
        } else {
            row.add("False");
        }
        if (transport == null) {
            row.add("");    // Transport
            row.add("");    // Booked
            row.add("");    // Outbound
            row.add("");    // Used
            row.add("");    // Return
            row.add("");    // Used
        } else {
            row.add(getItemValue(transport.getTransportOrganisation()));
            if (transport.isBooked()) {          // Booked
                row.add("True");
            } else {
                row.add("False");
            }

            if (transport.isRequiredOutbound()) {// Outbound
                row.add("True");
            } else {
                row.add("False");
            }
            if (transport.isUsedOutbound()) {    // Used
                row.add("True");
            } else {
                row.add("False");
            }
            if (transport.isRequiredReturn()) {  // Return
                row.add("True");
            } else {
                row.add("False");
            }
            if (transport.isUsedReturn()) {      // Used
                row.add("True");
            } else {
                row.add("False");
            }
        }
        return row;

    }

    private String getItemValue(ListItem item) {
        if (item == null) {
            return "Unknown";
        } else {
            return item.getItemValue();
        }
    }

    private String getFullName(User user) {
        if (user == null) {
            return "Unknown";
        } else {
            return user.getFullName();
        }
    }
}