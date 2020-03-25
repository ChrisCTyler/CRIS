package solutions.cris.object;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import androidx.appcompat.app.AlertDialog;

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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.utils.CRISUtil;
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
public class PdfDocument extends Document implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_PDF_DOCUMENT;

    public PdfDocument(User currentUser, UUID clientID){
        super(currentUser, clientID, Document.PdfDocument);
    }

    //PdfTypeID
    private UUID pdfTypeID;
    public UUID getPdfTypeID() {return pdfTypeID;}
    public void setPdfTypeID(UUID pdfTypeID) {this.pdfTypeID = pdfTypeID;}

    // PdfType
    private ListItem pdfType;
    public ListItem getPdfType() {
        if (pdfTypeID != null && pdfType == null) {
            LocalDB localDB = LocalDB.getInstance();
            pdfType = localDB.getListItem(pdfTypeID);
        }
        return pdfType;}
    public void setPdfType(ListItem pdfType) {this.pdfType = pdfType;}

    //BlobID
    private UUID blobID;
    public UUID getBlobID() {return blobID;}
    public void setBlobID(UUID blobID) {this.blobID = blobID;}

    // V1.2 PdfDocment may be 'attached' to a session (accessible through the session register
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

    public void setSession(solutions.cris.object.Session session) {
        this.session = session;
    }

    public void clear(){
        setPdfType(null);
        setSession(null);
    }

    public void save(boolean isNewMode){
        LocalDB localDB = LocalDB.getInstance();
        ListItem pdfType = getPdfType();
        Session session = getSession();
        clear();

        localDB.save(this, isNewMode, User.getCurrentUser());

        setPdfType(pdfType);
        setSession(session);
    }

    public boolean search(String searchText){
        if (searchText.isEmpty()){
            return true;
        } else {
            String text = String.format("%s %s", getPdfType().getItemValue(),getSummary());
            return text.toLowerCase().contains(searchText.toLowerCase());
        }
    }

    public String textSummary(){
        LocalDB localDB = LocalDB.getInstance();
        SimpleDateFormat sDate = new SimpleDateFormat("EEE dd MMM yyyy", Locale.UK);
        // Build the string
        String summary = super.textSummary();
        summary += "Type: " + getPdfType().getItemValue() + "\n";
        summary += "Date: ";
        if (getReferenceDate().getTime() != Long.MIN_VALUE) {
            summary += sDate.format(getReferenceDate());
        }
        summary += "\n";
        summary += "Summary: " + getSummary() + "\n";
        summary += "See attached document.\n";
        return summary;
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action){
        SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);
        PdfDocument previousDocument = (PdfDocument) localDB.getDocumentByRecordID(previousRecordID);
        PdfDocument thisDocument = (PdfDocument) localDB.getDocumentByRecordID(thisRecordID);
        String changes = Document.getChanges(previousDocument, thisDocument);
        changes += CRISUtil.getChanges(previousDocument.getPdfType(), thisDocument.getPdfType(), "Pdf Type");
        if (changes.length() == 0) {
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
    }

    private static PdfDocument displayDocument;
    public static void displayPDFDocument(PdfDocument pdfDocument, Context context){
        displayDocument = pdfDocument;
        // If the document has a multi-line summary, display the summary in a dialog
        if (pdfDocument.getSummaryLine1().endsWith("...")) {
            String line1 = pdfDocument.getSummaryLine1();
            // Remove the ellipsis
            line1 = line1.substring(0,line1.length()-3);
            // Get the rest of the summary
            String rest = pdfDocument.getSummary().substring(line1.length()+1);
            new AlertDialog.Builder(context)
                    .setTitle(line1)
                    .setMessage(rest)
                    .setPositiveButton("Display PDF File", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Context context = ((Dialog) dialog).getContext();
                            pdfIntent(context);
                        }
                    })
                    .setNegativeButton("Return", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Do nothing more
                        }
                    })
                    .show();
        } else {
            pdfIntent(context);
        }
    }

    private static void pdfIntent(Context context){

        LocalDB localDB = LocalDB.getInstance();
        // Get the blob
        byte[] buffer = localDB.getBlob(displayDocument.getBlobID());
        // Clean up any existing pdfs in the local directory
        // Note: because createTempFile is used this step should not be necessary. However,
        // no harm in removing them anyway
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".pdf");
            }
        });
        for (File file:files){
            // Nothing we can do if the delete fails
            file.delete();
        }
        try {
            //File file = File.createTempFile("CRIS", "pdf", dir);
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "CRIS_" + timeStamp + ".pdf";
            File file = new File(dir, imageFileName);
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(buffer);
            bos.flush();
            bos.close();
            //Uri uri = Uri.fromFile(file);
            Uri uri = getUriForFile(context, "solutions.cris.fileprovider", file);
            Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
            pdfIntent.setDataAndType(uri, "application/pdf");
            pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(pdfIntent);
        } catch (Exception ex) {
            throw new CRISException("Error writing Pdf to directory: " + ex.getMessage());
        }
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
        fNames.add("Pdf Type");
        fNames.add("Description");
        return fNames;
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
                                .setStartIndex(6))
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
        row.add(getItemValue(getPdfType()));
        row.add(getSummary());
        return row;

    }

    private String getItemValue(ListItem item){
        if (item == null){
            return "Unknown";
        } else {
            return item.getItemValue();
        }
    }

    public static List<List<Object>> getPdfDocumentData(ArrayList<Document> documents, ArrayList<Client> adapterList) {
        List<List<Object>> content = new ArrayList<>();
        content.add(getExportFieldNames());
        for (Document document : documents) {
            PdfDocument thisDocument = (PdfDocument) document;
            Client thisClient = null;
            for (Client client : adapterList) {
                if (client.getClientID().equals(thisDocument.getClientID())) {
                    content.add(thisDocument.getExportData(client));
                }
            }
        }
        return content;
    }

}
