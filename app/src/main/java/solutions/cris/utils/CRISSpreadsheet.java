package solutions.cris.utils;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.DimensionProperties;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Response;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import solutions.cris.object.User;

/**
 * Created by Chris Tyler on 15/06/2017.
 */

public class CRISSpreadsheet {

    private com.google.api.services.sheets.v4.Sheets mService = null;
    private String spreadsheetID;

    public CRISSpreadsheet(GoogleAccountCredential credential) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("CRIS")
                .build();
        SimpleDateFormat sDate = new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.UK);
        spreadSheetName = String.format("CRIS_%s", sDate.format(new Date()));
    }

    private String organisation = "";

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    private String listType = "";

    public void setListType(String listType) {
        this.listType = listType;
    }

    private String selection = "";

    public void setSelection(String selection) {
        this.selection = selection;
    }

    private String sort = "";

    public void setSort(String sort) {
        this.sort = sort;
    }

    private String search = "";

    public void setSearch(String search) {
        this.search = search;
    }

    private Date startDate = new Date(Long.MIN_VALUE);

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    private Date endDate = new Date(Long.MAX_VALUE);

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    private String spreadSheetName;

    public String getSpreadSheetName() {
        return spreadSheetName;
    }

    public void setSpreadSheetName(String spreadSheetName) {
        this.spreadSheetName = spreadSheetName;
    }

    public void create(User currentUser) throws IOException {
        // Create a new Spreadsheet
        Spreadsheet newSpreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties()
                .setTitle(spreadSheetName)
                .setLocale("en_GB")
                        .setTimeZone("Europe/London")
                .setDefaultFormat(new CellFormat()
                        .setTextFormat(new TextFormat()
                        .setFontSize(11))));
        Spreadsheet spreadsheet = mService.spreadsheets().create(newSpreadsheet).execute();
        spreadsheetID = spreadsheet.getSpreadsheetId();
        // Set the 1st sheet name to CRIS
        configureInitialSheet();
        loadInitialSheet(currentUser);
    }

    public void configureInitialSheet() throws IOException {
        List<Request> requests = new ArrayList<>();
        requests.add(new Request()
                .setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
                        .setFields("*")
                        .setProperties(new SheetProperties()
                                .setSheetId(0)
                                .setTitle("CRIS")
                                .setIndex(0)
                                .setSheetType("GRID")
                                .setGridProperties(new GridProperties()
                                        .setColumnCount(2)
                                        .setRowCount(17))
                        )));
        // Set the Cell dimensions
        requests.add(new Request()
                .setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                        .setFields("pixelSize")
                        .setProperties(new DimensionProperties()
                                .setPixelSize(500))
                        .setRange(new DimensionRange()
                                .setSheetId(0)
                                .setDimension("COLUMNS")
                                .setStartIndex(1)
                                .setEndIndex(2))
                ));
        // 1st column is bold
        requests.add(new Request()
                .setRepeatCell(new RepeatCellRequest()
                        .setCell(new CellData()
                                .setUserEnteredFormat(new CellFormat()
                                        .setVerticalAlignment("MIDDLE")
                                        .setTextFormat(new TextFormat()
                                                .setBold(true)
                                                .setFontSize(11))))
                        .setFields("UserEnteredFormat")
                        .setRange(new GridRange()
                                .setSheetId(0)
                                .setStartColumnIndex(0)
                                .setEndColumnIndex(1)
                                .setStartRowIndex(1))));
        // CRIS formatting
        requests.add(new Request()
                .setRepeatCell(new RepeatCellRequest()
                        .setCell(new CellData()
                                .setUserEnteredFormat(new CellFormat()
                                        .setTextFormat(new TextFormat()
                                                .setFontSize(24))
                                ))
                        .setFields("UserEnteredFormat")
                        .setRange(new GridRange()
                                .setSheetId(0)
                                .setStartColumnIndex(0)
                                .setEndColumnIndex(1)
                                .setStartRowIndex(0)
                                .setEndRowIndex(1))));
        requests.add(new Request()
                .setRepeatCell(new RepeatCellRequest()
                        .setCell(new CellData()
                                .setUserEnteredFormat(new CellFormat()
                                        .setVerticalAlignment("MIDDLE")
                                        .setTextFormat(new TextFormat()
                                                .setBold(true)
                                                .setFontSize(11))
                                ))
                        .setFields("UserEnteredFormat")
                        .setRange(new GridRange()
                                .setSheetId(0)
                                .setStartColumnIndex(1)
                                .setEndColumnIndex(2)
                                .setStartRowIndex(0)
                                .setEndRowIndex(1))));
        // Warning text is wrapped
        requests.add(new Request()
                .setRepeatCell(new RepeatCellRequest()
                        .setCell(new CellData()

                                .setUserEnteredFormat(new CellFormat()
                                        .setTextFormat(new TextFormat()
                                                .setFontSize(11))
                                        .setWrapStrategy("WRAP")
                                ))
                        .setFields("UserEnteredFormat")
                        .setRange(new GridRange()
                                .setSheetId(0)
                                .setStartColumnIndex(1)
                                .setStartRowIndex(16))));

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);
        mService.spreadsheets().batchUpdate(spreadsheetID, body).execute();
    }

    private void loadInitialSheet(User currentUser) throws IOException {
        List<List<Object>> values = new ArrayList<>();
        values.add(getRow("CRIS", "Care Record Information System"));
        values.add(getRow("", ""));
        values.add(getRow("Organisation:", organisation));
        values.add(getRow("", ""));
        values.add(getRow("List Type:", listType));
        values.add(getRow("", ""));
        values.add(getRow("Selection:", selection));
        values.add(getRow("Sort:", sort));
        values.add(getRow("Search:", search));
        values.add(getRow("", ""));
        values.add(getRow("Start Date:", getDate(startDate)));
        values.add(getRow("End Date:", getDate(endDate)));
        values.add(getRow("", ""));
        values.add(getRow("Exported By:", currentUser.getFullName()));
        values.add(getRow("Export date:", getDate(new Date())));
        values.add(getRow("", ""));
        values.add(getRow("WARNING:", getWarning()));
        ValueRange body = new ValueRange()
                .setValues(values);
        mService.spreadsheets().values().update(spreadsheetID, "CRIS!A1:B17", body)
                .setValueInputOption("RAW")
                .execute();

    }

    private List<Object> getRow(String col1, String col2) {
        List<Object> row = new ArrayList<>();
        row.add(col1);
        row.add(col2);
        return row;

    }

    private String getDate(Date date) {
        SimpleDateFormat sDate = new SimpleDateFormat("EEE dd MMM yyyy", Locale.UK);
        if (date.getTime() == Long.MIN_VALUE ||
                date.getTime() == Long.MAX_VALUE) {
            return "";
        } else {
            return sDate.format(date);
        }
    }

    private String getWarning() {
        return "This spreadsheet contains confidential client data. Unlike the CRIS application, " +
                "this export is not encrypted so you must take the necessary steps to protect " +
                "its contents by storing it is a physically safe location and/or encrpyting it.";
    }

    public void addSheet(String title, int sheetID) throws IOException {
        // Create the sheet
        List<Request> requests = new ArrayList<>();
        Request newRequest = new Request();
        AddSheetRequest sheetRequest = new AddSheetRequest();
        SheetProperties properties = new SheetProperties();
        properties.setTitle(title);
        properties.setSheetId(sheetID);
        properties.setGridProperties(new GridProperties().setFrozenRowCount(1));
        sheetRequest.setProperties(properties);
        newRequest.setAddSheet(sheetRequest);
        requests.add(newRequest);
                BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);
        mService.spreadsheets().batchUpdate(spreadsheetID, body).execute();
    }

    public void loadSheet(String title, List<List<Object>> content, List<Request> configuration) throws IOException{

        // Write the content
        ValueRange vRange = new ValueRange()
                .setValues(content);
        mService.spreadsheets().values().update(spreadsheetID, String.format("%s!A1", title), vRange)
                .setValueInputOption("USER_ENTERED")
                .execute();
        // Set the configuration (if specified)
        if (configuration != null) {
            BatchUpdateSpreadsheetRequest configRequest =
                    new BatchUpdateSpreadsheetRequest().setRequests(configuration);
            mService.spreadsheets().batchUpdate(spreadsheetID, configRequest).execute();
        }
    }

    public Spreadsheet test() throws IOException{
        String spreadsheetId = "1_RpK6UYRON6GjNH5ru-hAvKtJUvwawM0Ubndqc7rh1k";
        return mService.spreadsheets().get(spreadsheetId).execute();
    }
}
