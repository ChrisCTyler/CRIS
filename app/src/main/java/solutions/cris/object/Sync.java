package solutions.cris.object;

import java.util.Date;
import java.util.UUID;

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
