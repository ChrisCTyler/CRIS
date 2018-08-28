package solutions.cris.object;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.NumberFormat;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.TextFormat;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;

/**
 * Copyright CRIS.Solutions 21/12/2016.
 */

public class Note extends Document implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_NOTE;

    public Note(User currentUser, UUID clientID){
        super(currentUser, clientID, Document.Note);
        stickyDate = new Date(Long.MIN_VALUE);
        noteTypeID = null;
        noteType = null;
        content = "";
        stickyFlag = false;
        sessionID = null;
        session = null;
        initialNote = null;
        responseContent = "";
    }

    private UUID noteTypeID;
    private ListItem noteType;
    public UUID getNoteTypeID() {
        return noteTypeID;
    }
    public void setNoteTypeID(UUID noteTypeID) {this.noteTypeID = noteTypeID;}
    public ListItem getNoteType() {
        if (noteTypeID != null && noteType == null) {
            if (noteTypeID.equals(NoteType.responseNoteTypeID)) {
                noteType = new NoteType(NoteType.responseNoteTypeID);
            } else {
                LocalDB localDB = LocalDB.getInstance();
                noteType = localDB.getListItem(noteTypeID);
            }
        }
        return noteType;
    }
    public void setNoteType(ListItem noteType) {this.noteType = noteType;}

    private String content;
    public String getContent() {return content;}
    public void setContent(String content) {this.content = content;}

    private String responseContent;
    public String getResponseContent() {return responseContent;}
    public void setResponseContent(String responseContent) {this.responseContent = responseContent;}

    // V1.1 Replaced with initialNote.
    //private UUID initialNoteTypeID;
    //private ListItem initialNoteType;
    /*
    public UUID getInitialNoteTypeID() {
        return initialNoteTypeID;
    }
    public void setInitialNoteTypeID(UUID initialNoteTypeID) {this.initialNoteTypeID = initialNoteTypeID;}
    public ListItem getInitialNoteType() {
        return initialNoteType;
    }
    public void setInitialNoteType(ListItem initialNoteType) {this.initialNoteType = initialNoteType;}
    */
    // InitialNote NB: This is different to normal 'sub-objects' in that it is not loaded in localDB
    // but as part of the ListClientDocumentsFragment
    private Note initialNote;
    public Note getInitialNote() {
        return initialNote;
    }
    public void setInitialNote(Note initialNote) {this.initialNote = initialNote;}

    private boolean stickyFlag;
    public boolean isStickyFlag() {return stickyFlag;}
    public void setStickyFlag(boolean stickyFlag) {this.stickyFlag = stickyFlag;}

    private Date stickyDate;
    public Date getStickyDate() {return stickyDate;}
    public void setStickyDate(Date stickyDate) {this.stickyDate = stickyDate;}

    // V1.2 Note may be 'attached' to a session (accessible through the session register
    private UUID sessionID;

    public UUID getSessionID() {
        return sessionID;
    }

    public void setSessionID(UUID sessionID) {
        this.sessionID = sessionID;
    }

    private Session session;

    public Session getSession() {
        if (sessionID == null) {
            session = null;
        } else if (session == null){
            LocalDB localDB = LocalDB.getInstance();
            session = (Session) localDB.getDocument(sessionID);
        }
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void clear(){
        setNoteType(null);
        setInitialNote(null);
        setResponseContent(null);
        setSession(null);
    }

    public void save(boolean isNewMode, User author) {
        LocalDB localDB = LocalDB.getInstance();
        NoteType noteType = (NoteType) getNoteType();
        Note initialNote = getInitialNote();
        Session session = getSession();
        clear();

        // Load the Document fields, summary uses the first line of the note content
        Date creationDate = getCreationDate();
        if (isNewMode) {

            setReferenceDate(creationDate);
        }
        String[] lines = getContent().split("\n");
        setSummary(String.format("%s - %s ",
                author.getFullName(),
                lines[0]));

        localDB.save(this, isNewMode, User.getCurrentUser());

        setNoteType(noteType);
        setInitialNote(initialNote);
        setSession(session);
        if (isNewMode) {
            // If this note triggers 'Supervisor Follow', setFollow
            if (noteType.isSupervisorSetToFollow()) {
                // Loop through all users
                ArrayList<User> users = localDB.getAllUsers();
                for (User user : users) {
                    // Looking for 'supervisors'
                    if (user.getRole().hasPrivilege(Role.PRIVILEGE_SUPERVISOR_SET_TO_FOLLOW)) {
                        // 19 Oct 2017 Build 089 Only trigger follow if not already following
                        // otherwise the start date is updated and older unread documents
                        // will disappear
                        if (!localDB.isFollowing(user.getUserID(), getClientID())) {
                            // Build 100 Use creationDate as the follow 'start date' so that
                            // the follow date is equal to the creation date of the associated
                            // note and so the associated note will appear as the first unread
                            // document
                            //localDB.setFollow(user.getUserID(), getClientID(), true);
                            localDB.setFollow(user.getUserID(), getClientID(), true, creationDate);
                        }
                    }
                }
            }
        }
    }

    public boolean search(String searchText){
        if (searchText.isEmpty()){
            return true;
        } else {
            String text = String.format("%s %s", getNoteType().getItemValue(),getContent());
            if (getResponseContent() != null) {
                text += " " + getResponseContent();
            }

            return text.toLowerCase().contains(searchText.toLowerCase());
        }
    }

    public String textSummary() {
        LocalDB localDB = LocalDB.getInstance();
        SimpleDateFormat sDate = new SimpleDateFormat("EEE dd MMM yyyy", Locale.UK);
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        // Build the string
        String summary = "Note Type: " + getNoteType().getItemValue() + "\n";
        User author = localDB.getUser(getCreatedByID());
        summary += "Author: " + author.getFullName() + "\n";
        summary += "Date: ";
        if (getReferenceDate().getTime() != Long.MIN_VALUE) {
            summary += sDateTime.format(getReferenceDate());
        }
        summary += "\n";
        if (isStickyFlag()){
            summary += "Sticky: Yes\n";
        } else {
            summary += "Sticky: No\n";
        }
        summary += "Sticky Date: ";
        if (getStickyDate().getTime() != Long.MIN_VALUE) {
            summary += sDate.format(getStickyDate());
        }
        summary += "\n";
        summary += getContent();
        summary += "\n";
        if (getResponseContent() != null){
            summary += getResponseContent();
            summary += "\n";
        }
        return summary;
    }

    private static List<Object> getExportFieldNames() {
        List<Object> fNames = new ArrayList<>();
        fNames.add("Firstnames");
        fNames.add("Lastname");
        fNames.add("Date of Birth");
        fNames.add("Age");
        fNames.add("Postcode");
        fNames.add("Date");
        fNames.add("Note Type");
        fNames.add("Author");
        fNames.add("Sticky");
        fNames.add("Sticky Date");
        return fNames;
    }

    public static List<List<Object>> getNoteData(ArrayList<Document> documents) {
        LocalDB localDB = LocalDB.getInstance();
        Client client = null;
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames());
        for (Document document : documents) {
            if (client == null || document.getClientID() != client.getClientID()){
                // New client
                client = (Client) localDB.getDocument(document.getClientID());
            }
            Note thisDocument = (Note) document;
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
        // 10th column is a date
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
                                .setStartColumnIndex(9)
                                .setEndColumnIndex(10)
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
        row.add(getItemValue(getNoteType()));
        User author = localDB.getUser(getCreatedByID());
        if (author != null){
            row.add(author.getFullName());
        } else {
            row.add("");
        }
        if (isStickyFlag()) {
            row.add("True");
    } else {
        row.add("False");
    }
        if (getStickyDate().getTime() != Long.MIN_VALUE) {
            row.add(sDate.format(getStickyDate()));
        } else {
            row.add("");
        }
        return row;

    }

    private String getItemValue(ListItem item){
        if (item == null){
            return "Unknown";
        } else {
            return item.getItemValue();
        }
    }
}
