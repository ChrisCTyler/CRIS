package solutions.cris.object;

import java.util.Date;
import java.util.UUID;

/**
 * Copyright CRIS.Solutions 10-Oct-16.
 */

public class Sync {

    // Constructor
    public Sync(String tableName, long syncOffset) {
        syncID = UUID.randomUUID();
        this.tableName = tableName;
        // Sync offset compensates for tablets with incorrect time/dates
        syncDate = new Date();
        if (syncOffset == 0) {
            syncDate.setTime(syncDate.getTime() + syncOffset);
        }
    }

    private UUID syncID;

    public UUID getSyncID() {
        return syncID;
    }

    public void setSyncID(UUID syncID) {
        this.syncID = syncID;
    }

    //SyncDate
    private Date syncDate;

    public Date getSyncDate() {
        return syncDate;
    }

    public void setSyncDate(Date syncDate) {
        this.syncDate = syncDate;
    }

    //TableName
    private String tableName;

    public String getTableName() {
        return tableName;
    }

    //public void setTableName(String tableName) {
    //    this.tableName = tableName;
    //}


}
