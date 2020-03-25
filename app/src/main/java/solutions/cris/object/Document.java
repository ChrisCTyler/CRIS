package solutions.cris.object;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.LocalSettings;

///        CRIS - Client Record Information System
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
public abstract class Document extends CrisObject implements Serializable, DocumentSearch {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_DOCUMENT;
    public static final UUID nonClientDocumentID = UUID.fromString("a5d087dc-92d2-11e6-ae22-56b6b6499611");

    public enum Mode {NEW, READ, EDIT, RESPONSE}

    public static final int Client = -1;
    public static final int Case = 0;
    public static final int CriteriaAssessmentTool = 1;
    public static final int PdfDocument = 2;
    public static final int MyWeek = 3;
    // Set public when TRA developed
    private static final int TransportRequestAssessment = 4;
    public static final int Note = 5;
    public static final int Contact = 6;
    public static final int ClientSession = 7;
    public static final int Session = 8;
    public static final int Image = 9;
    public static final int Status = 10;
    public static final int Transport = 11;
    // UPDATE GetDocumentType Methods when new Document Type is added

    public Document(User currentUser, UUID clientID, int documentType) {
        super(currentUser);
        documentID = UUID.randomUUID();
        this.clientID = clientID;
        this.documentType = documentType;
        cancelledFlag = false;
        referenceDate = new Date(Long.MIN_VALUE);
    }

    //DocumentID
    private UUID documentID;

    public UUID getDocumentID() {
        return documentID;
    }

    //CancelledFlag
    private boolean cancelledFlag;

    public boolean getCancelledFlag() {
        return cancelledFlag;
    }

    public void setCancelledFlag(boolean cancelledFlag) {
        this.cancelledFlag = cancelledFlag;
    }

    //CancellationDate
    private Date cancellationDate;

    public Date getCancellationDate() {
        return cancellationDate;
    }

    public void setCancellationDate(Date cancellationDate) {
        this.cancellationDate = cancellationDate;
    }

    //CancelledByID
    private UUID cancelledByID;

    public UUID getCancelledByID() {
        return cancelledByID;
    }

    public void setCancelledByID(UUID cancelledByID) {
        this.cancelledByID = cancelledByID;
    }

    //CancellationReason
    private String cancellationReason;

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    //ClientID
    private UUID clientID;

    public UUID getClientID() {
        return clientID;
    }

    void setClientID(UUID clientID) {
        this.clientID = clientID;
    }

    //ReferenceDate
    private Date referenceDate;

    public Date getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(Date referenceDate) {
        this.referenceDate = referenceDate;
    }

    //DocumentType
    private int documentType;

    public int getDocumentType() {
        return documentType;
    }

    /* Always set when document is instantiated
    public void setDocumentType(int documentType) {
        this.documentType = documentType;
    }
    */
    //Summary
    private String summary;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummaryLine1() {
        String line1 = "";
        if (summary != null) {
            String[] lines = summary.split("\n");
            line1 = lines[0];
            if (lines.length > 1) {
                line1 += "...";
            }
        }
        return line1;
    }

    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);

        String summary = "";

        if (getCancelledFlag()){
            String cancellationDate = "Unknown Date";
            if (getCancellationDate() != null &&
                    !getCancellationDate().equals(Long.MIN_VALUE)){
                cancellationDate = sDate.format(getCancellationDate());
            }
            String cancellationReason = "Reason not given";
            if (getCancellationReason() != null){
                cancellationReason = getCancellationReason();
            }
            String cancelledBy = "Unknown User";
            if (getCancelledByID() != null){
                LocalDB localDB = LocalDB.getInstance();
                User user = localDB.getUser(getCancelledByID());
                if (user != null){
                    cancelledBy = user.getFullName();
                }
            }
            summary += String.format("Document cancelled by %s on %s\nReason:%s\n",
                    cancelledBy,
                    cancellationDate,
                    cancellationReason);
        }

        return summary;
    }

    public static Comparator<Document> comparatorDate = new Comparator<Document>() {
        @Override
        public int compare(Document o1, Document o2) {
            Date date1 = o1.getReferenceDate();
            Date date2 = o2.getReferenceDate();
            int compare = date2.compareTo(date1);
            // If dates are equal, this is a note/response combination
            if (compare == 0) {
                // Note/Response so compare using creation date unless Note/Response pair
                compare = o1.getCreationDate().compareTo(o2.getCreationDate());
                if (o1.getDocumentType() == Note && o2.getDocumentType() == Note) {
                    Note note1 = (Note) o1;
                    Note note2 = (Note) o2;
                    UUID n1 = note1.getNoteTypeID();
                    UUID n2 = note2.getNoteTypeID();
                    if (!n1.equals(n2)) {
                        if (n1.equals(NoteType.responseNoteTypeID)) {
                            compare = 1;
                        } else {
                            compare = -1;
                        }
                    }
                }
            } else {
                if (o1.getDocumentType() == Note) {
                    // Sticky Date overrides reference date
                    Note note1 = (Note) o1;
                    if (note1.getStickyDate() != null &&
                            note1.getStickyDate().getTime() != Long.MIN_VALUE &&
                            note1.getStickyDate().after(new Date())) {
                        date1 = note1.getStickyDate();
                    }
                    // Sticky flag overrides sticky date
                    if (note1.isStickyFlag()) {
                        date1 = new Date(Long.MAX_VALUE);
                    }
                }
                if (o2.getDocumentType() == Note) {
                    // Sticky Date overrides reference date
                    Note note2 = (Note) o2;
                    if (note2.getStickyDate() != null &&
                            note2.getStickyDate().getTime() != Long.MIN_VALUE &&
                            note2.getStickyDate().after(new Date())) {
                        date2 = note2.getStickyDate();
                    }
                    // Sticky flag overrides sticky date
                    if (note2.isStickyFlag()) {
                        date2 = new Date(Long.MAX_VALUE);
                    }
                }
                compare = date2.compareTo(date1);
                if (compare == 0) {
                    compare = o1.getCreationDate().compareTo(o2.getCreationDate());
                }
            }
            return compare;
        }
    };

    public static Comparator<Document> comparatorAZ = new Comparator<Document>() {
        @Override
        public int compare(Document o1, Document o2) {
            return o1.getSummary().compareTo(o2.getSummary());
        }
    };

    public static Comparator<Document> comparatorUnread = new Comparator<Document>() {
        @Override
        public int compare(Document o1, Document o2) {
            int compare = o1.getClientID().compareTo(o2.getClientID());
            if (compare == 0) {
                // Build 101 - Show earliest documents first
                //compare = o2.getReferenceDate().compareTo(o1.getReferenceDate());
                compare = o1.getReferenceDate().compareTo(o2.getReferenceDate());
                if (compare == 0) {
                    compare = o1.getCreationDate().compareTo(o2.getCreationDate());
                    if (o1.getDocumentType() == Note && o2.getDocumentType() == Note) {
                        Note note1 = (Note) o1;
                        Note note2 = (Note) o2;
                        UUID n1 = note1.getNoteTypeID();
                        UUID n2 = note2.getNoteTypeID();
                        if (!n1.equals(n2)) {
                            if (n1.equals(NoteType.responseNoteTypeID)) {
                                compare = 1;
                            } else {
                                compare = -1;
                            }
                        }
                    }
                }
            }
            return compare;
        }
    };

    public static Comparator<Document> comparatorType = new Comparator<Document>() {
        @Override
        public int compare(Document o1, Document o2) {
            int compare;
            String o1String;
            switch (o1.getDocumentType()) {
                case Document.PdfDocument:
                    PdfDocument p1 = (PdfDocument) o1;
                    o1String = p1.getPdfType().getItemValue();
                    break;
                case Document.Note:
                    Note note1 = (Note) o1;
                    if (note1.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                        o1String = note1.getInitialNote().getNoteType().getItemValue();
                    } else {
                        o1String = note1.getNoteType().getItemValue();
                    }
                    break;
                default:
                    o1String = o1.getDocumentTypeString();
            }
            String o2String;
            switch (o2.getDocumentType()) {
                case Document.PdfDocument:
                    PdfDocument p2 = (PdfDocument) o2;
                    o2String = p2.getPdfType().getItemValue();
                    break;
                case Document.Note:
                    Note note2 = (Note) o2;
                    if (note2.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                        o2String = note2.getInitialNote().getNoteType().getItemValue();
                    } else {
                        o2String = note2.getNoteType().getItemValue();
                    }
                    break;
                default:
                    o2String = o2.getDocumentTypeString();
            }

            compare = o1String.compareTo(o2String);
            if (compare == 0) {
                compare = o2.getReferenceDate().compareTo(o1.getReferenceDate());
                if (compare == 0) {
                    compare = o1.getCreationDate().compareTo(o2.getCreationDate());
                    if (o1.getDocumentType() == Note && o2.getDocumentType() == Note) {
                        Note note1 = (Note) o1;
                        Note note2 = (Note) o2;
                        UUID n1 = note1.getNoteTypeID();
                        UUID n2 = note2.getNoteTypeID();
                        if (!n1.equals(n2)) {
                            if (n1.equals(NoteType.responseNoteTypeID)) {
                                compare = 1;
                            } else {
                                compare = -1;
                            }
                        }
                    }
                }
            }
            return compare;
        }
    };

    public static Comparator<Document> comparatorTypeLib = new Comparator<Document>() {
        @Override
        public int compare(Document o1, Document o2) {
            int compare;
            PdfDocument p1 = (PdfDocument) o1;
            PdfDocument p2 = (PdfDocument) o2;
            compare = p1.getPdfType().getItemValue().compareTo(p2.getPdfType().getItemValue());
            if (compare == 0) {
                compare = o1.getSummary().compareTo(o2.getSummary());
            }
            return compare;
        }
    };

    public static int getDocumentType(String sDocumentType) {
        switch (sDocumentType) {
            case "Case":
                return Document.Case;
            case "CAT (Criteria Assessment Tool)":
            case "Criteria Assessment Tool":
                return Document.CriteriaAssessmentTool;
            case "Client":
                return Document.Client;
            case "ClientSession":
                return Document.ClientSession;
            case "Contact":
                return Document.Contact;
            case "Image":
                return Document.Image;
            case "My Week":
                return Document.MyWeek;
            // Inconsistency in getDocumentTypeString means both PdfDocument and Pdf Document are possible
            case "Pdf Document":
            case "PdfDocument":
                return Document.PdfDocument;
            case "Session":
                return Document.Session;
            case "Transport Request Assessment":
                return Document.TransportRequestAssessment;
            case "Note":
                return Document.Note;
            case "Status":
                return Document.Status;
            case "Transport":
                return Document.Transport;
            default:
                throw new CRISException("Unexpected document type string: " + sDocumentType);
        }
    }

    public String getDocumentTypeString() {
        switch (documentType) {
            case Document.Case:
                return "Case";
            case Document.Client:
                return "Client";
            case Document.ClientSession:
                return "ClientSession";
            case Document.Contact:
                return "Contact";
            case Document.CriteriaAssessmentTool:
                return "Criteria Assessment Tool";
            case Document.Image:
                return "Image";
            case Document.MyWeek:
                return "My Week";
            case Document.Note:
                return "Note";
            case Document.PdfDocument:
                return "PdfDocument";
            case Document.Session:
                return "Session";
            case Document.Status:
                return "Status";
            case Document.Transport:
                return "Transport";
            case Document.TransportRequestAssessment:
                return "Transport Request Assesssment";
            default:
                throw new CRISException(String.format(Locale.UK,
                        "Unexpected DocumentType: %d", documentType));
        }
    }

    public static String getDocumentTypeString(int documentType){
        switch (documentType) {
            case Document.Case:
                return "Case";
            case Document.Client:
                return "Client";
            case Document.ClientSession:
                return "ClientSession";
            case Document.Contact:
                return "Contact";
            case Document.CriteriaAssessmentTool:
                return "Criteria Assessment Tool";
            case Document.Image:
                return "Image";
            case Document.MyWeek:
                return "My Week";
            case Document.Note:
                return "Note";
            case Document.PdfDocument:
                return "PdfDocument";
            case Document.Session:
                return "Session";
            case Document.Status:
                return "Status";
            case Document.Transport:
                return "Transport";
            case Document.TransportRequestAssessment:
                return "Transport Request Assesssment";
            default:
                return String.format(Locale.UK,"Unexpected DocumentType: %d", documentType);
        }
    }

    public static String getChanges(Document previousDocument, Document thisDocument){
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        LocalSettings localSettings = LocalSettings.getInstance();
        String changes = "";
        changes += CRISUtil.getChangesDate(previousDocument.getReferenceDate(), thisDocument.getReferenceDate(), "Document Date");
        //changes += CRISUtil.getChanges(previousDocument.getSummary(), thisDocument.getSummary(), "Document Summary");
        changes += CRISUtil.getChanges(previousDocument.getCancelledFlag(), thisDocument.getCancelledFlag(), "Cancelled flag");
        changes += CRISUtil.getChangesDateTime(previousDocument.getCancellationDate(), thisDocument.getCancellationDate(), "Cancellation Date");
        changes += CRISUtil.getChanges(previousDocument.getCancellationReason(), thisDocument.getCancellationReason(), "Cancellation Reason");
        return changes;
    }


}
