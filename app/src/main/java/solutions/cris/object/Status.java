package solutions.cris.object;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;

/**
 * Created by Chris Tyler on 14/04/2017.
 */

public class Status extends Document implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_STATUS;

    public Status(User currentUser, UUID clientID) {
        super(currentUser, clientID, Document.Status);
        score = 0;
    }
    public Status(User currentUser, UUID clientID, int documentType) {
        super(currentUser, clientID, documentType);
        score = 0;
    }

    private long score;

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    // V1.2 MyWeek document MUST be 'attached' to a session (accessible through the session register
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
        } else if (session == null) {
            LocalDB localDB = LocalDB.getInstance();
            session = (Session) localDB.getDocument(sessionID);
        }
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    // Status has no searchable fields
    public boolean search(String searchText) {
        if (searchText.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("EEE dd MMM yyyy", Locale.UK);
        // Build the string
        String summary = "";
        summary += String.format("Date: %s\n",sDate.format(getReferenceDate()));
        summary += String.format(Locale.UK, "Score: %d\n",score);
        return summary;
    }
}
