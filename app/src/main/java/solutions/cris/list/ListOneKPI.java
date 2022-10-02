package solutions.cris.list;
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

import android.app.Activity;
import android.app.LauncherActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import solutions.cris.CRISActivity;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.CrisObject;
import solutions.cris.object.Document;
import solutions.cris.object.Group;
import solutions.cris.object.ListItem;
import solutions.cris.object.Note;
import solutions.cris.object.RawDocument;
import solutions.cris.object.Session;
import solutions.cris.object.Sync;
import solutions.cris.object.User;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.utils.CRISKPIItem;
import solutions.cris.utils.CRISMenuItem;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.ExceptionHandler;

public class ListOneKPI extends CRISActivity {

    // Number of columns in the associated layout. Unused columns have visibility removed
    // and column width is equal so visible columns expand to fill the available space
    final static int areaMax = 10;

    private ArrayList<CRISKPIItem> kpiItems;
    private User currentUser;
    private ArrayList<UUID> areaList;
    private String kpiType;
    private Date startTime;
    KPIAdapter adapter;
    ListView listView;
    TextView kpiHeaderTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CurrentUser always exists so if this check fails then exception in child
        // // has rendered system inconsistent so exit and let Main start from scratch()
        currentUser = User.getCurrentUser();
        if (currentUser == null) {
            finish();
        } else {
            // Add the global uncaught exception handler
            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
            setContentView(R.layout.activity_list_one_kpi);
            // Parameter passed from ListKPI menu
            kpiType = getIntent().getStringExtra(Main.EXTRA_KPI_TYPE);
            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setTitle(getString(R.string.app_name) + " - " + kpiType);
            setSupportActionBar(toolbar);
            kpiHeaderTextView = findViewById(R.id.kpi_header_text);

            // Initialise the areaList (columns)
            areaList = new ArrayList();

            // Initialise the list of KPI Items (rows)
            kpiItems = new ArrayList<>();

            // Load the data in the background
            new LoadData().execute();

            // Set up the adapter
            listView = findViewById(R.id.list_view);
            adapter = new KPIAdapter(this, kpiItems);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_empty, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }


    private class KPIAdapter extends ArrayAdapter<CRISKPIItem> {

        KPIAdapter(Context context, List<CRISKPIItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {

            TextView viewItemTitle;
            TextView[] viewArea = new TextView[areaMax];

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_kpi_list_item, parent, false);
                viewItemTitle = convertView.findViewById(R.id.item_title);
                viewArea[1] = convertView.findViewById(R.id.item_area_1);
                viewArea[2] = convertView.findViewById(R.id.item_area_2);
                viewArea[3] = convertView.findViewById(R.id.item_area_3);
                viewArea[4] = convertView.findViewById(R.id.item_area_4);
                viewArea[5] = convertView.findViewById(R.id.item_area_5);
                viewArea[6] = convertView.findViewById(R.id.item_area_6);
                viewArea[7] = convertView.findViewById(R.id.item_area_7);
                viewArea[8] = convertView.findViewById(R.id.item_area_8);
                viewArea[9] = convertView.findViewById(R.id.item_area_9);

                convertView.setTag(R.id.tag_view_item_title, viewItemTitle);
                convertView.setTag(R.id.tag_view_item_area_1, viewArea[1]);
                convertView.setTag(R.id.tag_view_item_area_2, viewArea[2]);
                convertView.setTag(R.id.tag_view_item_area_3, viewArea[3]);
                convertView.setTag(R.id.tag_view_item_area_4, viewArea[4]);
                convertView.setTag(R.id.tag_view_item_area_5, viewArea[5]);
                convertView.setTag(R.id.tag_view_item_area_6, viewArea[6]);
                convertView.setTag(R.id.tag_view_item_area_7, viewArea[7]);
                convertView.setTag(R.id.tag_view_item_area_8, viewArea[8]);
                convertView.setTag(R.id.tag_view_item_area_9, viewArea[9]);
            } else {
                viewItemTitle = (TextView) convertView.getTag(R.id.tag_view_item_title);
                viewArea[1] = (TextView) convertView.getTag(R.id.tag_view_item_area_1);
                viewArea[2] = (TextView) convertView.getTag(R.id.tag_view_item_area_2);
                viewArea[3] = (TextView) convertView.getTag(R.id.tag_view_item_area_3);
                viewArea[4] = (TextView) convertView.getTag(R.id.tag_view_item_area_4);
                viewArea[5] = (TextView) convertView.getTag(R.id.tag_view_item_area_5);
                viewArea[6] = (TextView) convertView.getTag(R.id.tag_view_item_area_6);
                viewArea[7] = (TextView) convertView.getTag(R.id.tag_view_item_area_7);
                viewArea[8] = (TextView) convertView.getTag(R.id.tag_view_item_area_8);
                viewArea[9] = (TextView) convertView.getTag(R.id.tag_view_item_area_9);
            }

            final CRISKPIItem kpiItem = kpiItems.get(position);

            // Build the string
            viewItemTitle.setText(kpiItem.getTitle());
            HashMap<UUID, int[]> kpiMap = kpiItem.getKpiMap();
            int areaCount = 1;
            for (UUID key : areaList) {
                // There is a preset maximum of areaMax columns
                if (areaCount < areaMax) {
                    switch (kpiType) {
                        case "Total Cases":
                        case "Total Cases (Last 12 Months)":
                            if (kpiMap.containsKey(key)) {
                                int[] values = kpiMap.get(key);
                                viewArea[areaCount++].setText(String.format("%d", values[0]));
                            } else {
                                // This area had no values so output zeroes
                                viewArea[areaCount++].setText(String.format("%d", 0, 0));
                            }
                            break;

                        case "Total/Active Cases":
                        case "Total/Active Cases (Last 12 Months)":
                            if (kpiMap.containsKey(key)) {
                                int[] values = kpiMap.get(key);
                                viewArea[areaCount++].setText(String.format("%d/%d", values[0], values[1]));
                            } else {
                                // This area had no values so output zeroes
                                viewArea[areaCount++].setText(String.format("%d/%d", 0, 0));
                            }
                            break;
                        case "Avg. Session Attendance":
                        case "Avg. Session Attendance (Last 12 Months)":
                            // Build 157 - Total Attendance KPI
                        case "Total Session Attendance":
                        case "Total Session Attendance (Last 12 Months)":
                            if (kpiMap.containsKey(key)) {
                                int[] values = kpiMap.get(key);
                                viewArea[areaCount++].setText(String.format("%d/%d/%d", values[0], values[1], values[2]));
                            } else {
                                // This area had no values so output zeroes
                                viewArea[areaCount++].setText(String.format("%d/%d/%d", 0, 0, 0));
                            }
                            break;
                        default:
                            throw new CRISException(String.format("Unknown KPI Type: %s", kpiType));
                    }
                }
            }
            // Remove unnecessary columns
            for (int i = areaCount; i < areaMax; i++) {
                viewArea[i].setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    /**
     * An asynchronous task that handles the loading and processing of the KPI data.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class LoadData extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            // Runs in Background Thread
            switch (kpiType) {
                case "Total Cases":
                    initKPIList(4);
                    doTotalActiveCases(kpiType);
                    break;
                case "Total Cases (Last 12 Months)":
                    initKPIList(12);
                    doTotalActiveCases(kpiType);
                    break;
                case "Total/Active Cases":
                    initKPIList(4);
                    doTotalActiveCases(kpiType);
                    break;
                case "Total/Active Cases (Last 12 Months)":
                    initKPIList(12);
                    doTotalActiveCases(kpiType);
                    break;
                case "Avg. Session Attendance":
                    initKPIList(4);
                    doAverageAttendance(kpiType);
                    break;
                case "Avg. Session Attendance (Last 12 Months)":
                    initKPIList(12);
                    doAverageAttendance(kpiType);
                    break;
                // Build 157 - Total Attendance KPI
                case "Total Session Attendance":
                    initKPIList(4);
                    doTotalAttendance(kpiType);
                    break;
                case "Total Session Attendance (Last 12 Months)":
                    initKPIList(12);
                    doTotalAttendance(kpiType);
                    break;
                default:
                    throw new CRISException(String.format("Unknown KPI Type: %s", kpiType));
            }
            // This string is not used
            return "Data Loaded";
        }

        @Override
        protected void onPreExecute() {
            // Runs on UI Thread
            startTime = new Date();     // Used to display execution time
            kpiHeaderTextView.setText("Processing data... (0%)");   // Initial text
        }

        @Override
        protected void onPostExecute(String output) {
            // Runs on UI Thread
            // Display the header text and load the adapter
            Date endTime = new Date();
            long elapsed = (endTime.getTime() - startTime.getTime()) / 1000;
            switch (kpiType) {
                case "Total Cases":
                    doHeaderSetup("Total Cases");
                    break;
                case "Total Cases (Last 12 Months)":
                    doHeaderSetup("Total Cases");
                    break;
                case "Total/Active Cases":
                    doHeaderSetup(String.format("Total Cases/At Least One Attended Session (%d seconds)", elapsed));
                    break;
                case "Total/Active Cases (Last 12 Months)":
                    doHeaderSetup(String.format("Total Cases/At Least One Attended Session (%d seconds)", elapsed));
                    break;
                case "Avg. Session Attendance":
                    doHeaderSetup(String.format("Regular/Ad-Hoc/Both (%d seconds)", elapsed));
                    break;
                case "Avg. Session Attendance (Last 12 Months)":
                    doHeaderSetup(String.format("Regular Session%%/Ad-Hoc Session%%/Both%% (%d seconds)", elapsed));
                    break;
                // Build 157
                case "Total Session Attendance":
                    doHeaderSetup(String.format("Regular/Ad-Hoc/Both (%d seconds)", elapsed));
                    break;
                case "Total Session Attendance (Last 12 Months)":
                    doHeaderSetup(String.format("Regular Session%%/Ad-Hoc Session%%/Both%% (%d seconds)", elapsed));
                    break;
                default:
                    throw new CRISException(String.format("Unknown KPI Type: %s", kpiType));
            }
            // Load the adapter
            listView.setAdapter(adapter);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // Runs on UI Thread
            kpiHeaderTextView.setText(String.format("Processing data... (%d%%)", values[0]));
        }

        private void initKPIList(int size) {

            // First the previous 12 months
            // Set the cal variable to midnight on 1st of next month
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.add(Calendar.MONTH, 1);
            if (size == 12) {
                // Now create kpiItems for this and the previous 11 months
                for (int i = 1; i < 13; i++) {
                    // Roll back by 1 minute to create the month end date
                    cal.add(Calendar.MINUTE, -1);
                    Date monthEnd = cal.getTime();
                    // Now set to midnight at beginning of month
                    cal.add(Calendar.MINUTE, 1);
                    cal.add(Calendar.MONTH, -1);
                    kpiItems.add(new CRISKPIItem(String.format("%tb", cal.getTime()), cal.getTime(), monthEnd, CRISKPIItem.kpiItemType.MONTH));
                }
            } else if (size == 4) {
                // Now this year and back to 2017 (start of CRIS)
                cal.setTime(new Date());
                cal.set(Calendar.DAY_OF_YEAR, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.add(Calendar.YEAR, 1);
                while (cal.get(Calendar.YEAR) > 2017) {
                    // Roll back by 1 minute to create the year end date
                    cal.add(Calendar.MINUTE, -1);
                    Date yearEnd = cal.getTime();
                    // Now set to midnight at beginning of year
                    cal.add(Calendar.MINUTE, 1);
                    cal.add(Calendar.YEAR, -1);
                    kpiItems.add(new CRISKPIItem(String.format("%tY", cal.getTime()), cal.getTime(), yearEnd, CRISKPIItem.kpiItemType.YEAR));
                }
            } else {
                throw new CRISException(String.format("Invalid KPI List size: %d", size));
            }
        }

        private void incrementCount(CRISKPIItem kpiItem, UUID area, int element, int maxSize) {
            HashMap kpiMap = kpiItem.getKpiMap();
            if (kpiMap.containsKey(area)) {
                int[] values = (int[]) kpiMap.get(area);
                values[element]++;
            } else {
                int[] newValues = new int[maxSize];
                newValues[element] = 1;
                kpiMap.put(area, newValues);
            }
        }

        private void doHeaderSetup(String headerText) {
            LocalDB localDB = LocalDB.getInstance();

            // Load the Header
            kpiHeaderTextView = findViewById(R.id.kpi_header_text);
            kpiHeaderTextView.setText(headerText);

            // Load the column Headers
            TextView[] viewArea = new TextView[areaMax];
            TextView viewItemTitle = findViewById(R.id.item_title);
            viewArea[1] = findViewById(R.id.item_area_1);
            viewArea[2] = findViewById(R.id.item_area_2);
            viewArea[3] = findViewById(R.id.item_area_3);
            viewArea[4] = findViewById(R.id.item_area_4);
            viewArea[5] = findViewById(R.id.item_area_5);
            viewArea[6] = findViewById(R.id.item_area_6);
            viewArea[7] = findViewById(R.id.item_area_7);
            viewArea[8] = findViewById(R.id.item_area_8);
            viewArea[9] = findViewById(R.id.item_area_9);
            int areaCount = 1;
            for (UUID key : areaList) {
                if (key.equals(ListItem.commissionerTotalID)) {
                    viewArea[areaCount++].setText("Total");
                } else if (key.equals(ListItem.commissionerUnknownID)) {
                    viewArea[areaCount++].setText("Unknown");
                } else {
                    ListItem commissioner = localDB.getListItem(key);
                    viewArea[areaCount++].setText(commissioner.getItemValue());
                }
            }
            // Remove unnecessary columns
            for (int i = areaCount; i < areaMax; i++) {
                viewArea[i].setVisibility(View.GONE);
            }
        }

        // Loop through the case documents to get case start and end dates and commissioners
        // then create a mapping for clientID to commissioner/area in each slot (row)
        private void loadActiveCases() {
            LocalDB localDB = LocalDB.getInstance();
            // Get the set of Case documents sorted by ClientID then Reference date
            ArrayList<Document> caseDocuments = localDB.getAllDocumentsOfType(Document.Case);
            UUID clientID = null;
            Date caseStart = new Date(Long.MIN_VALUE);
            UUID area = null;
            for (Document document : caseDocuments) {
                Case caseDocument = (Case) document;
                // Deal with switch to new client by completing previous case with today's date
                if (clientID != null && !clientID.equals(caseDocument.getClientID())) {
                    doProcessOneCase(caseStart, new Date(), area, clientID);
                    // Clear the loop variables
                    caseStart = new Date(Long.MIN_VALUE);
                    //area = "";
                    area = null;
                }
                // Save the client ID
                clientID = caseDocument.getClientID();
                // Proceed based on the case document type
                switch (caseDocument.getCaseType()) {
                    case "Start":
                        // Set some variables
                        caseStart = caseDocument.getReferenceDate();
                        area = caseDocument.getCommissionerID();
                        break;
                    case "Update":
                        // Treat as end of one case and start of another with potentially new area
                        doProcessOneCase(caseStart, caseDocument.getReferenceDate(), area, clientID);
                        area = caseDocument.getCommissionerID();
                        caseStart = caseDocument.getReferenceDate();
                        break;
                    case "Close":
                        doProcessOneCase(caseStart, caseDocument.getReferenceDate(), area, clientID);
                        caseStart = new Date(Long.MIN_VALUE);
                        break;
                    case "Reject":
                        // Treat as end of case (case may have been started) then clear existing case
                        // If no start the caseStart will be null so nothing will be processed
                        caseStart = new Date(Long.MIN_VALUE);
                        break;
                    default:
                        throw new CRISException(String.format("Unexpected Case Type: %s", caseDocument.getCaseType()));
                }
            }
            // Complete the final client
            if (clientID != null) {
                doProcessOneCase(caseStart, new Date(), area, clientID);
            }
            publishProgress(5);
        }

        // Called from loadActiveCases when a case end/update is found
        private void doProcessOneCase(Date caseStart, Date caseEnd, UUID area, UUID clientID) {
            // If the parameters are null then no Case Start has been recorded so ignore the end
            if (caseStart.after(new Date(Long.MIN_VALUE)) && area != null) {
                // Update the areaList, if necessary
                if (!areaList.contains(area)) {
                    areaList.add(area);
                }
                // Allocate an area (from case) to each slot
                for (int i = kpiItems.size(); i > 0; i--) {
                    CRISKPIItem item = kpiItems.get(i - 1);
                    //If case start before slotEnd and caseEnd after slotStart then allocate area
                    if (caseStart.before(item.getSlotEnd()) && caseEnd.after(item.getSlotStart())) {
                        HashMap<UUID, UUID> clientArea = item.getClientArea();
                        clientArea.put(clientID, area);
                    }
                }
            }
        }

        // Main load routine for Total Cases and Total/Active Cases
        private void doTotalActiveCases(String kpiType) {
            // Use the case documents to set the client Area mapping in each slot
            loadActiveCases();

            // Sort the Area List
            Collections.sort(areaList);

            // Count the number of clients
            for (int i = 0; i < kpiItems.size(); i++) {
                CRISKPIItem item = kpiItems.get(i);
                HashMap<UUID, UUID> clientArea = item.getClientArea();
                // Iterate over the clientArea HashMap incrementing the total counts (element 0)
                for (HashMap.Entry<UUID, UUID> entry : clientArea.entrySet()) {
                    incrementCount(item, entry.getValue(), 0, 2);
                    //incrementCount(item, ListItem.commissionerTotalID, 0, 2);
                }
            }

            if (kpiType.equals("Total/Active Cases")) {
                // loadAttendedSessions
                //loadAttendedSessionsYearSlots();
                loadTotalActiveAttendedSessions(4);
            } else if (kpiType.equals("Total/Active Cases (Last 12 Months)")) {
                //loadAttendedSessionsMonthSlots();
                loadTotalActiveAttendedSessions(12);
            }

            // Calculate the values for the Total column
            for (int i = 0; i < kpiItems.size(); i++) {
                int[] totals = {0, 0};
                CRISKPIItem item = kpiItems.get(i);
                HashMap<UUID, int[]> kpiMap = item.getKpiMap();
                for (UUID key : areaList) {
                    if (kpiMap.containsKey(key)) {
                        int[] values = kpiMap.get(key);
                        totals[0] += values[0];
                        totals[1] += values[1];
                    }
                }
                kpiMap.put(ListItem.commissionerTotalID, totals);
            }
            // Add the Total to the areaList
            areaList.add(ListItem.commissionerTotalID);
        }

        // Additional rountine to load the number of Active clients (at least one
        // attended client session in the slot (Year/Month)
        private void loadTotalActiveAttendedSessions(int slotMax) {
            LocalDB localDB = LocalDB.getInstance();
            // Get the set of ClientSessions ordered by clientID then ReferenceDate
            ArrayList<RawDocument> clientSessions =
                    localDB.getAllRawDocumentsOfType(Document.ClientSession, new Date(Long.MIN_VALUE), new Date());
            publishProgress(10);
            // Preset the loop variables
            int docMax = clientSessions.size();
            int docCount = 0;
            int docDisplay = 0;
            UUID area = null;
            UUID clientID = null;
            boolean foundAttendedSession = false;
            // Start with earliest Year Slot -1 and MIN slot end date
            int slot = slotMax;
            CRISKPIItem kpiItem = null;
            Date slotEndDate = new Date(Long.MIN_VALUE);
            for (RawDocument document : clientSessions) {
                // Do progress bar
                if (docCount++ * 20 / docMax > docDisplay) {
                    docDisplay++;
                    publishProgress(10 + (docDisplay * 5 * 9 / 10));
                }
                // Check for a new client
                if (clientID == null || !document.getClientID().equals(clientID)) {
                    // New client ID
                    foundAttendedSession = false;
                    clientID = document.getClientID();
                    slot = slotMax;
                    slotEndDate = new Date(Long.MIN_VALUE);
                }
                // check for required move beyond existing slot Year slots are 16 to 12
                while (slot >= 0 && document.getReferenceDate().after(slotEndDate)) {
                    slot--;
                    if (slot < 0) {
                        kpiItem = null;
                    } else {
                        kpiItem = kpiItems.get(slot);
                        slotEndDate = kpiItem.getSlotEnd();
                        if (kpiItem.getClientArea().containsKey(clientID)) {
                            area = kpiItem.getClientArea().get(clientID);
                            foundAttendedSession = false;
                        } else {
                            // Should not reach here since it implies a client session outside of a case
                            // so set found to true to ignore this client
                            foundAttendedSession = true;
                        }
                    }
                }
                // If completed year slots then jump out of loop (should not occur since last slot is current year
                if (kpiItem == null) {
                    break;
                }
                // Once an attended session is found ignore the rest for this client
                if (!foundAttendedSession) {
                    // Check whether session is attended
                    ClientSession clientSession = (ClientSession) localDB.getDocumentByRecordID(document.getRecordID());
                    if (clientSession != null && clientSession.isAttended()) {
                        // Increment area count
                        incrementCount(kpiItem, area, 1, 2);
                        //incrementCount(kpiItem, ListItem.commissionerTotalID, 1, 2);
                        foundAttendedSession = true;
                    }
                }
            }
        }

        // Main routine for Avg. Session Attendance
        private void doAverageAttendance(String kpiType) {
            LocalDB localDB = LocalDB.getInstance();
            // Use the case documents to set the client Area mapping in each slot
            loadActiveCases();
            // Sort the Area List
            Collections.sort(areaList);
            // SEt the number of slots (rows) based on the kpiType
            int slotMax = 0;
            if (kpiType.equals("Avg. Session Attendance")) {
                slotMax = 4;
            } else if (kpiType.equals("Avg. Session Attendance (Last 12 Months)")) {
                slotMax = 12;
            } else {
                throw new CRISException(String.format("Unexpected kpiType: %s", kpiType));
            }
            // Used to count the progress steps
            int progressIncrement = 80/(slotMax-1);
            // Get the average attendance for each slot in turn
            for (int i = 0; i < slotMax; i++) {
                CRISKPIItem kpiItem = kpiItems.get(i);
                sessionAttendanceLoadOneSlot(kpiItem, progressIncrement,i+1);
            }
            // Calculate the averages and the values for the Total column
            for (int i = 0; i < kpiItems.size(); i++) {
                int[] totals = {0, 0, 0, 0, 0, 0, 0};
                CRISKPIItem item = kpiItems.get(i);
                HashMap<UUID, int[]> kpiMap = item.getKpiMap();
                for (UUID key : areaList) {
                    if (kpiMap.containsKey(key)) {
                        int[] values = kpiMap.get(key);
                        totals[3] += values[3];
                        totals[4] += values[4];
                        totals[5] += values[5];
                        totals[6] += values[6];
                        // Do averages (note: integer divide truncates so multiply first)
                        if (values[5] > 0) {
                            values[0] = values[3] * 100 / values[5];
                        }
                        if (values[6] > 0) {
                            values[1] = values[4] * 100 / values[6];
                        }
                        if (values[5] + values[6] > 0) {
                            values[2] = (values[3] + values[4]) * 100 / (values[5] + values[6]);
                        }
                        kpiMap.put(key, values);
                    }
                }
                // Finally calculate the averages for the total column
                if (totals[5] > 0) {
                    totals[0] = totals[3] * 100 / totals[5];
                }
                if (totals[6] > 0) {
                    totals[1] = totals[4] * 100 / totals[6];
                }
                if (totals[5] + totals[6] > 0) {
                    totals[2] = (totals[3] + totals[4]) * 100 / (totals[5] + totals[6]);
                }
                // And load the totals column kpiItem
                kpiMap.put(ListItem.commissionerTotalID, totals);
            }
            // Add the Total column header to the areaList
            areaList.add(ListItem.commissionerTotalID);
        }

        // Build 157 - Main routine for Total. Session Attendance
        private void doTotalAttendance(String kpiType) {
            LocalDB localDB = LocalDB.getInstance();
            // Use the case documents to set the client Area mapping in each slot
            loadActiveCases();
            // Sort the Area List
            Collections.sort(areaList);
            // SEt the number of slots (rows) based on the kpiType
            int slotMax = 0;
            if (kpiType.equals("Total Session Attendance")) {
                slotMax = 4;
            } else if (kpiType.equals("Total Session Attendance (Last 12 Months)")) {
                slotMax = 12;
            } else {
                throw new CRISException(String.format("Unexpected kpiType: %s", kpiType));
            }
            // Used to count the progress steps
            int progressIncrement = 80 / (slotMax - 1);
            // Get the total attendance for each slot in turn
            for (int i = 0; i < slotMax; i++) {
                CRISKPIItem kpiItem = kpiItems.get(i);
                sessionAttendanceLoadOneSlot(kpiItem, progressIncrement, i + 1);
            }
            // Calculate the averages and the values for the Total column
            for (int i = 0; i < kpiItems.size(); i++) {
                int[] totals = {0, 0, 0, 0, 0, 0, 0};
                CRISKPIItem item = kpiItems.get(i);
                HashMap<UUID, int[]> kpiMap = item.getKpiMap();
                for (UUID key : areaList) {
                    if (kpiMap.containsKey(key)) {
                        int[] values = kpiMap.get(key);
                        totals[3] += values[3];
                        totals[4] += values[4];
                        totals[5] += values[5];
                        totals[6] += values[6];
                        // Do totals
                        //values[0] = values[3] * 100 / values[5];
                        values[0] = values[3];
                        //values[1] = values[4] * 100 / values[6];
                        values[1] = values[4];
                        //values[2] = (values[3] + values[4]) * 100 / (values[5] + values[6]);
                        values[2] = (values[3] + values[4]);
                        kpiMap.put(key, values);
                    }
                }
                // Finally calculate the averages for the total column

                //totals[0] = totals[3] * 100 / totals[5];
                totals[0] = totals[3];
                //totals[1] = totals[4] * 100 / totals[6];
                totals[1] = totals[4];
                //totals[2] = (totals[3] + totals[4]) * 100 / (totals[5] + totals[6]);
                totals[2] = (totals[3] + totals[4]);

                // And load the totals column kpiItem
                kpiMap.put(ListItem.commissionerTotalID, totals);
            }
            // Add the Total column header to the areaList
            areaList.add(ListItem.commissionerTotalID);
        }

        private void sessionAttendanceLoadOneSlot(CRISKPIItem kpiItem,
                                                  int progressIncrement, int count) {
            LocalDB localDB = LocalDB.getInstance();
            Date slotStartDate = kpiItem.getSlotStart();
            Date slotEndDate = kpiItem.getSlotEnd();
            // Get the session documents and create a sessionID->groupType mapping
            // Group type is 3 for regular and 4 for ad-hoc (offset into kpiItem arrays)
            HashMap<UUID, Integer> sessionTypes = new HashMap<>();
            ArrayList<Document> sessions = localDB.getRangeOfDocumentsOfType(Document.Session,
                    slotStartDate, slotEndDate);
            // For each session load the mapping
            for (Document document : sessions) {
                Session session = (Session) document;
                if (session.getGroupID().equals(Group.adHocGroupID)) {
                    sessionTypes.put(session.getDocumentID(), 4);
                } else {
                    sessionTypes.put(session.getDocumentID(), 3);
                }
            }
            // Now Get all client sessions and count the attended ones
            ArrayList<Document> clientSessions;
            clientSessions =
                    localDB.getRangeOfDocumentsOfType(Document.ClientSession,
                            slotStartDate, slotEndDate);
            // Preset the loop variables
            int docMax = clientSessions.size();
            UUID area = null;
            UUID clientID = null;
            for (Document document : clientSessions) {
                boolean caseFound = false;
                ClientSession clientSession = (ClientSession) document;
                // Check for a new client
                if (clientID == null || !clientSession.getClientID().equals(clientID)) {
                    // New client ID
                    //foundAttendedSession = false;
                    clientID = clientSession.getClientID();
                    if (kpiItem.getClientArea().containsKey(clientID)) {
                        area = kpiItem.getClientArea().get(clientID);
                    } else {
                        // There are a small number of client sessions which fall outside of
                        // a client's case and therefore have no commissioner/area for a given slot
                        // Following code would allocate them to 'Unknown'
                        //area = ListItem.commissionerUnknownID;
                        // Might be the first time 'Unknown' area has appeared
                        //if (!areaList.contains(area)) {
                        //    areaList.add(area);
                        //}
                        // This version simply ignores them
                        area = null;
                    }
                }
                if (area != null) {
                    // Get the GroupType if there is a session for this client session
                    // Otherwise ignore (very unlikely)
                    if (sessionTypes.containsKey(clientSession.getSessionID())) {
                        int sessionType = sessionTypes.get(clientSession.getSessionID());
                        // increment the total sessions
                        incrementCount(kpiItem, area, sessionType + 2, 7);
                        // Check whether session is attended
                        if (clientSession.isAttended()) {
                            // Increment attended count
                            incrementCount(kpiItem, area, sessionType, 7);
                        }
                    }
                }
            }
            publishProgress(10 + (progressIncrement * count));
        }
    }
}
