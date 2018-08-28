package solutions.cris.object;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Copyright CRIS.Solutions 31/10/2016.
 */
//RecordID CHAR(16)
//Sync_ID CHAR(20)
//TableName CHAR(20)
//CreationDate
//CreatedByID CHAR(16)
//Result CHAR(10)
//AdditionalInformation TEXT

public class SyncActivity extends CrisObject {

    private SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.UK);

    // SyncResult uses the SysnID field to record the SyncID of the sync being
    // attempted. The SyncResult is not synced with the web server
    public SyncActivity(User currentUser){
        super(currentUser);
        result = "";
        log = "";
    }

    // CompletionDate
    private Date completionDate;
    public Date getCompletionDate() {return completionDate;}
    public void setCompletionDate(Date completionDate) {this.completionDate = completionDate;}

    // Result
    private String result;
    public String getResult() {return result;}
    public void setResult(String result) {this.result = result;}

    // Summary
    private String summary;
    public String getSummary() {return summary;}
    public void setSummary(String summary) {this.summary = summary;}

    //Log
    private String log;
    public String getLog() {return log;}
    public void setLog(String log) {this.log = log;}
    public void appendLog(String log) {this.log += sDate.format(new Date()) + " - " + log + "\n";}

    public String getTextSummary() {
        return "Sync Result\n\n" +
                "Date: " + sDate.format(getCreationDate()) + "\n\n" +
                "Result: " + result + "\n\n" +
                "Summary: " + summary + "\n\n" +
                log;
    }
}
