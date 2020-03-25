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

public class RawDocument extends CrisObject implements Serializable {
    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_RAW_DOCUMENT;

    public RawDocument(UUID recordID, Date creationDate, UUID createdByID,
                       UUID documentID, boolean cancelledFlag, UUID clientID,
                       Date referenceDate, int documentType) {
        super(recordID, creationDate, createdByID);
        this.documentID = documentID;
        this.cancelledFlag = cancelledFlag;
        this.clientID = clientID;
        this.referenceDate = referenceDate;
        this.documentType = documentType;
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
    //ClientID
    private UUID clientID;

    public UUID getClientID() {
        return clientID;
    }
    //ReferenceDate
    private Date referenceDate;

    public Date getReferenceDate() {
        return referenceDate;
    }
    //DocumentType
    private int documentType;

    public int getDocumentType() {
        return documentType;
    }
}
