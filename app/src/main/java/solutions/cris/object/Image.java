package solutions.cris.object;

import androidx.appcompat.app.AlertDialog;

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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;
import solutions.cris.utils.SwipeDetector;

import static androidx.core.content.FileProvider.getUriForFile;

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

public class Image extends Document implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_IMAGE;

    public Image(User currentUser, UUID clientID) {
        super(currentUser, clientID, Document.Image);
    }

    //BlobID
    private UUID blobID;

    public UUID getBlobID() {
        return blobID;
    }

    public void setBlobID(UUID blobID) {
        this.blobID = blobID;
    }

    public void save(boolean isNewMode){
        LocalDB localDB = LocalDB.getInstance();
        localDB.save(this, isNewMode, User.getCurrentUser());
    }

    public boolean search(String searchText) {
        if (searchText.isEmpty()) {
            return true;
        } else {
            String text = String.format("%s", getSummary());
            return text.toLowerCase().contains(searchText.toLowerCase());
        }
    }

    public String textSummary() {
        LocalDB localDB = LocalDB.getInstance();
        SimpleDateFormat sDate = new SimpleDateFormat("EEE dd MMM yyyy", Locale.UK);
        // Build the string
        String summary = "Date: ";
        if (getReferenceDate().getTime() != Long.MIN_VALUE) {
            summary += sDate.format(getReferenceDate());
        }
        summary += "\n";
        summary += "Summary: " + getSummary() + "\n";
        summary += "See attached image.\n";
        return summary;
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action){
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);

        Image previousDocument = (Image) localDB.getDocumentByRecordID(previousRecordID);
        Image thisDocument = (Image) localDB.getDocumentByRecordID(thisRecordID);
        String changes = Document.getChanges(previousDocument, thisDocument);
        if (changes.length() == 0){
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
    }


    public static List<Object> getExportFieldNames() {
        List<Object> fNames = new ArrayList<>();
        fNames.add("Firstnames");
        fNames.add("Lastname");
        fNames.add("Date of Birth");
        fNames.add("Age");
        // Build 139 - Add Year Group to Export
        fNames.add("Year Group");
        fNames.add("Postcode");
        fNames.add("Date");
        fNames.add("Description");
        return fNames;
    }

        /* Build 208 Moved to CRISExport
    public static List<List<Object>> getImageData(ArrayList<Document> documents) {
        LocalDB localDB = LocalDB.getInstance();
        Client client = null;
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames());
        for (Document document : documents) {
            if (client == null || document.getClientID() != client.getClientID()){
                // New client
                client = (Client) localDB.getDocument(document.getClientID());
            }
            Image thisDocument = (Image) document;
            content.add(thisDocument.getExportData(client));
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
                                .setStartColumnIndex(6)
                                .setEndColumnIndex(7)
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
        // Build 139 - Add Year Group to Export
        row.add(client.getYearGroup());
        row.add(client.getPostcode());
        if (getReferenceDate().getTime() != Long.MIN_VALUE) {
            row.add(sDate.format(getReferenceDate()));
        } else {
            row.add("");
        }
        row.add(getSummary());
        return row;

    }

}
