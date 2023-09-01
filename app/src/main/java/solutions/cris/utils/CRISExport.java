package solutions.cris.utils;
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

import android.app.DatePickerDialog;
// Build 200 Use the androidX Fragment class
//import android.app.Fragment;
//import android.app.FragmentManager;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.Contact;
import solutions.cris.object.CriteriaAssessmentTool;
import solutions.cris.object.Document;
import solutions.cris.object.Group;
import solutions.cris.object.Image;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.MACAYC18;
import solutions.cris.object.MyWeek;
import solutions.cris.object.Note;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.Session;
import solutions.cris.object.Transport;
import solutions.cris.object.User;

import static android.app.Activity.RESULT_OK;

public class CRISExport extends Fragment implements EasyPermissions.PermissionCallbacks {
//public class CRISExport extends Fragment  {

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS};

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private EditText typeView;
    private EditText selectionView;
    private EditText sortView;
    private EditText searchView;
    private EditText startDateView;
    private EditText endDateView;
    private CheckBox selectAll;
    TextView resultView;
    private View parent;

    GoogleAccountCredential mCredential;
    ArrayList<Client> clientAdapterList = null;
    ArrayList<Session> sessionAdapterList = null;
    ProgressDialog mProgress;
    String organisation = "";
    ArrayList<DocumentSelector> documents = new ArrayList<>();
    LocalDB localDB;
    String listType;
    // Build 209 Pass localSettings directly
    LocalSettings localSettings;

    // Build 217 - If user cancels the export (Cancel or back arrow) cancel the export task
    // to prevent getActivity() exception when eport finishes and there is nowhere to go
    // backk to
    AsyncTask thisMakeRequestTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        //setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.cris_export, container, false);
        return parent;
    }
    // Build 218 Stop the background task if the fragment is ended
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (thisMakeRequestTask != null){
            thisMakeRequestTask.cancel(true);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        localDB = LocalDB.getInstance();
        // Build 209 Pass localSettings directly
        localSettings = LocalSettings.getInstance();
        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        toolbar.setTitle(getString(R.string.app_name) + " - Export to Google Sheets");

        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        // Hide the FAB
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
        TextView footer = getActivity().findViewById(R.id.footer);
        // Clear the footer
        footer.setText("");

        listType = ((ListActivity) getActivity()).getExportListType();
        switch (listType) {
            case "My Clients":
            case "All Clients":
            case "One Client":
                clientAdapterList = ((ListActivity) getActivity()).getClientAdapterList();
                break;
            case "My Sessions":
            case "One Session":
                sessionAdapterList = ((ListActivity) getActivity()).getSessionAdapterList();
                break;
        }


        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                        getActivity().getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        // Get the previous organisation from  shared preferences
        SharedPreferences prefs =
                getActivity().getSharedPreferences(getString(R.string.shared_preference_file), Context.MODE_PRIVATE);
        if (prefs.contains(getString(R.string.pref_organisation))) {
            organisation = prefs.getString(getString(R.string.pref_organisation), "");
        }

        mProgress = new ProgressDialog(getActivity());
        mProgress.setMessage("Initiating Google Sheets API ...");

        typeView = parent.findViewById(R.id.type);
        selectionView = parent.findViewById(R.id.selection);
        sortView = parent.findViewById(R.id.sort);
        searchView = parent.findViewById(R.id.search);
        startDateView = parent.findViewById(R.id.start_date);
        endDateView = parent.findViewById(R.id.end_date);
        LinearLayout checkboxLayout = parent.findViewById(R.id.checkbox_layout);
        selectAll = parent.findViewById(R.id.select_all);
        resultView = parent.findViewById(R.id.result);

        typeView.setFocusable(false);

        typeView.setText(listType);
        selectionView.setFocusable(false);
        String selectMode = ((ListActivity) getActivity()).getExportSelection();
        selectionView.setText(selectMode);
        sortView.setFocusable(false);
        String sortMode = ((ListActivity) getActivity()).getExportSort();
        sortView.setText(sortMode);
        searchView.setFocusable(false);
        String searchText = ((ListActivity) getActivity()).getExportSearch();
        searchView.setText(searchText);

        startDateView.setVisibility(View.VISIBLE);
        startDateView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                startDatePicker();
                return true;
            }
        });
        endDateView.setVisibility(View.VISIBLE);
        endDateView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                endDatePicker();
                return true;
            }
        });

        // Set up checkboxes for document types
        selectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    // Checked
                    for (DocumentSelector ds : documents) {
                        ds.getCheckBox().setChecked(true);
                    }
                }
            }
        });
        localDB = LocalDB.getInstance();
        ArrayList<ListItem> listItems = localDB.getAllListItems(ListType.DOCUMENT_TYPE.toString(), false);
        Collections.sort(listItems, ListItem.comparator);
        int position = 2;
        addDocumentType(checkboxLayout, "Client", position++);
        addDocumentType(checkboxLayout, "Case", position++);
        addDocumentType(checkboxLayout, "Criteria Assessment Tool", position++);
        addDocumentType(checkboxLayout, "Pdf Document", position++);
        addDocumentType(checkboxLayout, "MACA-YC18", position++);
        addDocumentType(checkboxLayout, "My Week", position++);
        addDocumentType(checkboxLayout, "Note", position++);
        addDocumentType(checkboxLayout, "Contact", position++);
        addDocumentType(checkboxLayout, "ClientSession", position++);
        // Build 234 Session is not a client document
        //addDocumentType(checkboxLayout, "Session", position++);
        addDocumentType(checkboxLayout, "Image", position++);
        addDocumentType(checkboxLayout, "Transport", position++);

        // If there is no document selection, hide the selector
        if (!listType.equals("My Clients") && !listType.equals("All Clients")) {
            LinearLayout checkboxlayout = parent.findViewById(R.id.checkbox_layout);
            checkboxlayout.setVisibility(View.GONE);
        }

        // Save Button
        final Button saveButton = parent.findViewById(R.id.export_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveButton.setEnabled(false);
                doExport();
                // Build 218 Do not re-enable so that export cannot be done twice
                //saveButton.setEnabled(true);
            }
        });

        // Cancel Button
        Button cancelButton = parent.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Build 200 Use the androidX Fragment class
                //FragmentManager fragmentManager = getFragmentManager();
                //fragmentManager.popBackStack();
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void addDocumentType(LinearLayout checkboxLayout,
                                 String documentType,
                                 int position) {
        CheckBox cb = new CheckBox(getActivity());
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!b) {
                    //Unchecked
                    if (selectAll.isChecked()) {
                        selectAll.setChecked(false);
                    }
                }
            }
        });
        cb.setText(documentType);
        switch (listType) {
            case "My Clients":
            case "All Clients":
                if (documentType.equals("Client")) {
                    cb.setChecked(true);
                } else {
                    cb.setChecked(false);
                }
                break;
            case "One Client":
                cb.setChecked(true);
                break;
            case "Session":
                if (documentType.equals("ClientSession")) {
                    cb.setChecked(true);
                } else {
                    cb.setChecked(false);
                }
        }
        checkboxLayout.addView(cb, position);
        DocumentSelector ds = new DocumentSelector();
        ds.setCheckBox(cb);
        ds.setDocumentType(Document.getDocumentType(documentType));
        documents.add(ds);
    }

    private void startDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog startDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                startDateView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        startDatePickerDialog.show();
    }

    private void endDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog endDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                endDateView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        endDatePickerDialog.show();
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void doExport() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            resultView.setText("No network connection available.");
        } else {
            Date startDate = getStartDate();
            Date endDate = getEndDate();
            if (startDate != null && endDate != null) {
                // StartDate/EndDate consistency check
                if (!startDate.before(endDate)) {
                    endDateView.setError(getString(R.string.error_invalid_date_order));
                    endDateView.requestFocus();
                    endDateView.requestFocusFromTouch();
                } else {
                    // Cannot access the checkbox setting in non-UI thread
                    for (DocumentSelector document : documents) {
                        document.setChecked(document.getCheckBox().isChecked());
                    }
                    thisMakeRequestTask = new MakeRequestTask(mCredential,
                            clientAdapterList,
                            sessionAdapterList,
                            typeView.getText().toString(),
                            selectionView.getText().toString(),
                            sortView.getText().toString(),
                            searchView.getText().toString(),
                            startDate, endDate).execute();


                }
            }
        }
    }

    private Date getStartDate() {
        Date startDate;
        String sDate = startDateView.getText().toString();
        if (TextUtils.isEmpty(sDate)) {
            startDate = new Date(Long.MIN_VALUE);
        } else {
            startDate = CRISUtil.parseDate(sDate);
            if (startDate == null) {
                startDateView.setError(getString(R.string.error_invalid_date));
                startDateView.requestFocus();
                startDateView.requestFocusFromTouch();
            } else {
                startDate = CRISUtil.midnightEarlier(startDate);
            }
        }
        return startDate;
    }

    private Date getEndDate() {
        Date endDate;
        String sDate = endDateView.getText().toString();
        if (TextUtils.isEmpty(sDate)) {
            endDate = new Date(Long.MAX_VALUE);
        } else {
            endDate = CRISUtil.parseDate(sDate);
            if (endDate == null) {
                endDateView.setError(getString(R.string.error_invalid_date));
                endDateView.requestFocus();
                endDateView.requestFocusFromTouch();
                endDate = null;
            } else {
                endDate = CRISUtil.midnightLater(endDate);
            }
        }
        return endDate;
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                getActivity(), Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getActivity().getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                doExport();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    public void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    resultView.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    doExport();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getActivity().getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        doExport();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    doExport();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(getActivity());
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(getActivity());
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                getActivity(),
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    // Build 179 Enable progress string
    //private class MakeRequestTask extends AsyncTask<Void, Void, String> {
    public class MakeRequestTask extends AsyncTask<Void, String, String> {

        private Exception mLastError = null;
        private CRISSpreadsheet crisSpreadsheet = null;
        private Date startDate;
        private Date endDate;

        MakeRequestTask(GoogleAccountCredential credential,
                        ArrayList<Client> clientAdapterList,
                        ArrayList<Session> sessionAdapterList,
                        String listType,
                        String selection,
                        String sort,
                        String search,
                        Date startDate,
                        Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            // Instantiate the CRISSpreadsheet (create/write done in doInBackground)
            crisSpreadsheet = new CRISSpreadsheet(credential);
            crisSpreadsheet.setOrganisation(organisation);
            crisSpreadsheet.setListType(listType);
            crisSpreadsheet.setSelection(selection);
            crisSpreadsheet.setSort(sort);
            crisSpreadsheet.setSearch(search);
            crisSpreadsheet.setStartDate(startDate);
            crisSpreadsheet.setEndDate(endDate);
        }

        @Override
        protected String doInBackground(Void... params) {
            if (clientAdapterList != null) {
                publishProgress("Client Export...");
                return doClientList(clientAdapterList);
            } else {
                publishProgress("Session Export...");
                return doSessionList(sessionAdapterList);
            }
        }


        /* Build 220 Added progress bar for session export
        private String doSessionListOld(ArrayList<Session> adapterList) {
            try {
                ArrayList<ClientSession> clientDocuments;
                publishProgress("Creating spreadsheet...");
                crisSpreadsheet.create(User.getCurrentUser());
                int sheetID = 1;
                int docTotal = 0;
                int docCount = 0;
                int docDisplay = 0;
                publishProgress("Exporting session details...");
                crisSpreadsheet.addSheet("Session", sheetID);
                crisSpreadsheet.loadSheet("Session",
                        Session.getSessionData(adapterList, getActivity()),
                        Session.getExportSheetConfiguration(sheetID));
                sheetID++;
                publishProgress("Exporting individual client sessions...");
                clientDocuments = localDB.getAllClientSessions(adapterList, startDate, endDate);
                crisSpreadsheet.addSheet("ClientSession", sheetID);
                List<List<Object>> content = new ArrayList<>();
                content.add(ClientSession.getExportFieldNames());
                // Build 217 Correct long-standing MyWeek issue
                List<List<Object>> myWeekContent = new ArrayList<>();
                myWeekContent.add(MyWeek.getExportFieldNames());
                docTotal = clientDocuments.size();
                for (Document clientDocument : clientDocuments) {
                    if (docCount++ * 100 / docTotal > docDisplay) {
                        publishProgress(String.format(Locale.UK, "Exporting-Client Sessions %d%% of %d",
                                ++docDisplay,
                                docTotal));
                    }
                    ClientSession clientSession = (ClientSession) clientDocument;
                    // Add each document (needs associated note/pdf/transport documents)
                    Client client = clientSession.getClient();
                    if (client != null) {
                        // Find any associated note / pdf
                        MyWeek sessionMyWeek = (MyWeek) localDB.getSessionDocumentsOfType(
                                client.getClientID(), Document.MyWeek, clientSession.getSessionID());
                        Note sessionNote = (Note) localDB.getSessionDocumentsOfType(
                                client.getClientID(), Document.Note, clientSession.getSessionID());
                        PdfDocument sessionPdf = (PdfDocument) localDB.getSessionDocumentsOfType(
                                client.getClientID(), Document.PdfDocument, clientSession.getSessionID());
                        Transport sessionTransport = (Transport) localDB.getSessionDocumentsOfType(
                                client.getClientID(), Document.Transport, clientSession.getSessionID());
                        content.add(clientSession.getExportData(client,
                                sessionMyWeek,
                                sessionNote,
                                sessionPdf,
                                sessionTransport));
                        // Build 217
                        if (sessionMyWeek != null){
                            myWeekContent.add(sessionMyWeek.getExportData(client));
                        }
                    }
                }
                crisSpreadsheet.loadSheet("ClientSession",
                        content,
                        //ClientSession.getClientSessionData(clientDocuments),
                        ClientSession.getExportSheetConfiguration(sheetID));

                publishProgress("Exporting MyWeek records...");
                sheetID++;
                crisSpreadsheet.addSheet("MyWeek", sheetID);
                crisSpreadsheet.loadSheet("MyWeek",
                        myWeekContent,
                        MyWeek.getExportSheetConfiguration(sheetID));
                //
                // Build 217 Correct long-standing MyWeek issue. Only MyWeek associated with
                // the exported sessions should be exported
                //publishProgress("Exporting MyWeek records...");
                //ArrayList<Document> myWeekDocuments = localDB.getAllDocumentsOfType(
                //        clientDocuments, Document.MyWeek);
                //crisSpreadsheet.addSheet("MyWeek", sheetID);
                //content = new ArrayList<>();
                //content.add(MyWeek.getExportFieldNames());
                //docTotal = myWeekDocuments.size();
                //for (Document myWeekDocument : myWeekDocuments) {
                //    if (docCount++ * 100 / docTotal > docDisplay) {
                //        publishProgress(String.format(Locale.UK, "Exporting-My Weeks %d%% of %d",
                //                ++docDisplay,
                //                docTotal));
                //    }
                //    MyWeek thisDocument = (MyWeek) myWeekDocument;
                //    Client client = (Client) localDB.getDocument(thisDocument.getClientID());
                //    if (client != null) {
                //        content.add(thisDocument.getExportData(client));
                //    }
                //}
                //crisSpreadsheet.loadSheet("MyWeek",
                //        content,
                //        //MyWeek.getMyWeekData(myWeekDocuments),
                //        MyWeek.getExportSheetConfiguration(sheetID));

                return String.format("Data exported to: %s", crisSpreadsheet.getSpreadSheetName());
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return String.format("ERROR: %s", e.getMessage());
            }
        }

         */

        private ArrayList<Session> getSessions(ArrayList<Session> adapterList){
            ArrayList<Session> sessionList = new ArrayList<>();
            //Build 222 Only export sessions between start and end date
            for (Session session:adapterList){
                if (session.getReferenceDate().before(endDate) &&
                        session.getReferenceDate().after(startDate)){
                    sessionList.add(session);
                }
            }
            return sessionList;
        }

        private String doSessionList(ArrayList<Session> adapterList) {
            try {
                publishProgress("Creating spreadsheet...");
                crisSpreadsheet.create(User.getCurrentUser());
                int sheetID = 1;
                int docTotal = 0;
                int docCount = 0;
                int docDisplay = 0;
                publishProgress("Exporting session details...");
                ArrayList<Session> sessionList = getSessions(adapterList);
                crisSpreadsheet.addSheet("Session", sheetID);
                docTotal = sessionList.size();
                List<List<Object>> content = new ArrayList<>();
                content.add(Session.getExportFieldNames(localSettings));
                for (Session session : sessionList) {
                    if (docTotal > 100) {
                        if (docCount++ * 100 / docTotal > docDisplay) {
                            publishProgress(String.format(Locale.UK, "Exporting-Sessions %d%% of %d",
                                       ++docDisplay,
                                    docTotal));
                        }
                    }
                    content.add(session.getExportData());
                }
                crisSpreadsheet.loadSheet("Session",
                        content,
                        Session.getExportSheetConfiguration(sheetID));
                sheetID++;
                publishProgress("Exporting individual client sessions...");
                ArrayList<ClientSession> clientDocuments = localDB.getAllClientSessions(sessionList, startDate, endDate);
                crisSpreadsheet.addSheet("ClientSession", sheetID);
                content = new ArrayList<>();
                content.add(ClientSession.getExportFieldNames());
                // Build 217 Correct long-standing MyWeek issue
                List<List<Object>> myWeekContent = new ArrayList<>();
                myWeekContent.add(MyWeek.getExportFieldNames());
                docTotal = clientDocuments.size();
                for (Document clientDocument : clientDocuments) {
                    if (docTotal > 100) {
                        if (docCount++ * 100 / docTotal > docDisplay) {
                            publishProgress(String.format(Locale.UK, "Exporting-Client Sessions %d%% of %d",
                                    ++docDisplay,
                                    docTotal));
                        }
                    }
                    ClientSession clientSession = (ClientSession) clientDocument;
                    // Add each document (needs associated note/pdf/transport documents)
                    Client client = clientSession.getClient();
                    if (client != null) {
                        Case relevantCase = localDB.getRelevantCase(clientDocument.getClientID(), clientSession.getReferenceDate());
                        // Find any associated note / pdf
                        MyWeek sessionMyWeek = (MyWeek) localDB.getSessionDocumentsOfType(
                                client.getClientID(), Document.MyWeek, clientSession.getSessionID());
                        Note sessionNote = (Note) localDB.getSessionDocumentsOfType(
                                client.getClientID(), Document.Note, clientSession.getSessionID());
                        PdfDocument sessionPdf = (PdfDocument) localDB.getSessionDocumentsOfType(
                                client.getClientID(), Document.PdfDocument, clientSession.getSessionID());
                        Transport sessionTransport = (Transport) localDB.getSessionDocumentsOfType(
                                client.getClientID(), Document.Transport, clientSession.getSessionID());
                        content.add(clientSession.getExportData(client,
                                sessionMyWeek,
                                sessionNote,
                                sessionPdf,
                                sessionTransport,
                                relevantCase));
                        // Build 217
                        if (sessionMyWeek != null){
                            myWeekContent.add(sessionMyWeek.getExportData(client));
                        }
                    }
                }
                crisSpreadsheet.loadSheet("ClientSession",
                        content,
                        //ClientSession.getClientSessionData(clientDocuments),
                        ClientSession.getExportSheetConfiguration(sheetID));

                publishProgress("Exporting MyWeek records...");
                sheetID++;
                crisSpreadsheet.addSheet("MyWeek", sheetID);
                crisSpreadsheet.loadSheet("MyWeek",
                        myWeekContent,
                        MyWeek.getExportSheetConfiguration(sheetID));

                return String.format("Data exported to: %s", crisSpreadsheet.getSpreadSheetName());
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return String.format("ERROR: %s", e.getMessage());
            }
        }
        private String doClientList(ArrayList<Client> adapterList) {
            try {
                publishProgress("Creating spreadsheet...");
                crisSpreadsheet.create(User.getCurrentUser());
                int sheetID = 0;
                for (DocumentSelector document : documents) {
                    if (document.isChecked()) {
                        sheetID++;
                        publishProgress(String.format("Exporting-%ss", Document.getDocumentTypeString(document.getDocumentType())));
                        if (document.getDocumentType() == Document.Client) {
                            crisSpreadsheet.addSheet("Client", sheetID);
                            ArrayList<Client> clientList = doClients(adapterList);
                            int docCount = 0;
                            int docDisplay = 0;
                            int docTotal = clientList.size();
                            List<List<Object>> content = new ArrayList<>();
                            content.add(Client.getExportFieldNames(localSettings));
                            for (Client client : clientList) {
                                if (docTotal > 100) {
                                    if (docCount++ * 100 / docTotal > docDisplay) {
                                        publishProgress(String.format(Locale.UK, "Exporting-%ss %d%% of %d",
                                                Document.getDocumentTypeString(document.getDocumentType()),
                                                ++docDisplay,
                                                docTotal));
                                    }
                                }
                                content.add(client.getExportData());
                            }
                            crisSpreadsheet.loadSheet("Client",
                                    content,
                                    Client.getExportSheetConfiguration(sheetID));
                        } else {
                            loadContent(adapterList, document, sheetID);
                        }
                    }
                }
                return String.format("Data exported to: %s", crisSpreadsheet.getSpreadSheetName());
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return String.format("ERROR: %s", e.getMessage());
            }
        }

        private void loadContent(ArrayList<Client> adapterList,
                                 DocumentSelector document,
                                 int sheetID) throws java.io.IOException {
            ArrayList<Document> clientDocuments = localDB.getAllDocumentsOfType(
                    adapterList, document.getDocumentType(), startDate, endDate);
            List<List<Object>> content = new ArrayList<>();
            int docCount = 0;
            int docDisplay = 0;
            int docTotal = clientDocuments.size();
            for (Document clientDocument : clientDocuments) {
                if (docTotal > 100) {
                    if (docCount++ * 100 / docTotal > docDisplay) {
                        publishProgress(String.format(Locale.UK, "Exporting-%ss %d%% of %d",
                                Document.getDocumentTypeString(document.getDocumentType()),
                                ++docDisplay,
                                docTotal));
                    }
                }
                Client client = (Client) localDB.getDocument(clientDocument.getClientID());
                if (client != null) {
                    switch (document.getDocumentType()) {
                        case Document.ClientSession:
                            ClientSession clientSession = (ClientSession) clientDocument;
                            Case relevantCase = localDB.getRelevantCase(clientDocument.getClientID(), clientSession.getReferenceDate());
                            // Find any associated note / pdf
                            MyWeek sessionMyWeek = (MyWeek) localDB.getSessionDocumentsOfType(
                                    client.getClientID(), Document.MyWeek, clientSession.getSessionID());
                            Note sessionNote = (Note) localDB.getSessionDocumentsOfType(
                                    client.getClientID(), Document.Note, clientSession.getSessionID());
                            PdfDocument sessionPdf = (PdfDocument) localDB.getSessionDocumentsOfType(
                                    client.getClientID(), Document.PdfDocument, clientSession.getSessionID());
                            Transport sessionTransport = (Transport) localDB.getSessionDocumentsOfType(
                                    client.getClientID(), Document.Transport, clientSession.getSessionID());
                            content.add(clientSession.getExportData(client,
                                    sessionMyWeek,
                                    sessionNote,
                                    sessionPdf,
                                    sessionTransport,
                                    relevantCase));
                            break;
                        case Document.Case:
                            Case caseDocument = (Case) clientDocument;
                            content.add(caseDocument.getExportData(client));
                            break;
                        case Document.CriteriaAssessmentTool:
                            CriteriaAssessmentTool toolDocument = (CriteriaAssessmentTool) clientDocument;
                            content.add(toolDocument.getExportData(client));
                            break;
                        case Document.Contact:
                            Contact contactDocument = (Contact) clientDocument;
                            content.add(contactDocument.getExportData(client));
                            break;
                        case Document.Image:
                            Image imageDocument = (Image) clientDocument;
                            content.add(imageDocument.getExportData(client));
                            break;
                        case Document.MACAYC18:
                            MACAYC18 macayc18Document = (MACAYC18) clientDocument;
                            content.add(macayc18Document.getExportData(client));
                            break;
                        case Document.MyWeek:
                            MyWeek myWeekDocument = (MyWeek) clientDocument;
                            content.add(myWeekDocument.getExportData(client));
                            break;
                        case Document.Note:
                            Note noteDocument = (Note) clientDocument;
                            content.add(noteDocument.getExportData(client));
                            break;
                        case Document.PdfDocument:
                            PdfDocument pdfDocument = (PdfDocument) clientDocument;
                            content.add(pdfDocument.getExportData(client));
                            break;
                        case Document.Transport:
                            Transport transportDocument = (Transport) clientDocument;
                            content.add(transportDocument.getExportData(client));
                            break;
                    }

                }
            }
            switch (document.getDocumentType()) {
                case Document.ClientSession:
                    content.add(0, ClientSession.getExportFieldNames());
                    crisSpreadsheet.addSheet("ClientSession", sheetID);
                    crisSpreadsheet.loadSheet("ClientSession",
                            content,
                            ClientSession.getExportSheetConfiguration(sheetID));
                    break;
                case Document.Case:
                    content.add(0, Case.getExportFieldNames(localSettings));
                    crisSpreadsheet.addSheet("Case", sheetID);
                    crisSpreadsheet.loadSheet("Case",
                            content,
                            Case.getExportSheetConfiguration(sheetID));
                    break;
                case Document.CriteriaAssessmentTool:
                    content.add(0, CriteriaAssessmentTool.getExportFieldNames());
                    crisSpreadsheet.addSheet("CriteriaAssessmentTool", sheetID);
                    crisSpreadsheet.loadSheet("CriteriaAssessmentTool",
                            content,
                            CriteriaAssessmentTool.getExportSheetConfiguration(sheetID));
                    break;
                case Document.Contact:
                    content.add(0, Contact.getExportFieldNames());
                    crisSpreadsheet.addSheet("Contact", sheetID);
                    crisSpreadsheet.loadSheet("Contact",
                            content,
                            Contact.getExportSheetConfiguration(sheetID));
                    break;
                case Document.Image:
                    content.add(0, Image.getExportFieldNames());
                    crisSpreadsheet.addSheet("Image", sheetID);
                    crisSpreadsheet.loadSheet("Image",
                            content,
                            Image.getExportSheetConfiguration(sheetID));
                    break;
                case Document.MACAYC18:
                    content.add(0, MACAYC18.getExportFieldNames());
                    crisSpreadsheet.addSheet("MACA-YC18", sheetID);
                    crisSpreadsheet.loadSheet("MACA-YC18",
                            content,
                            MACAYC18.getExportSheetConfiguration(sheetID));
                    break;
                case Document.MyWeek:
                    content.add(0, MyWeek.getExportFieldNames());
                    crisSpreadsheet.addSheet("MyWeek", sheetID);
                    crisSpreadsheet.loadSheet("MyWeek",
                            content,
                            MyWeek.getExportSheetConfiguration(sheetID));
                    break;
                case Document.Note:
                    content.add(0, Note.getExportFieldNames());
                    crisSpreadsheet.addSheet("Note", sheetID);
                    crisSpreadsheet.loadSheet("Note",
                            content,
                            Note.getExportSheetConfiguration(sheetID));
                    break;
                case Document.PdfDocument:
                    content.add(0, PdfDocument.getExportFieldNames());
                    crisSpreadsheet.addSheet("PdfDocument", sheetID);
                    crisSpreadsheet.loadSheet("PdfDocument",
                            content,
                            PdfDocument.getExportSheetConfiguration(sheetID));
                    break;
                case Document.Transport:
                    content.add(0, Transport.getExportFieldNames());
                    crisSpreadsheet.addSheet("Transport", sheetID);
                    crisSpreadsheet.loadSheet("Transport",
                            content,
                            Transport.getExportSheetConfiguration(sheetID));
                    break;
            }
        }

        // Build 201 Addition of Attendance Points requires calculation of session
        // fields in client documents
        private ArrayList<Client> doClients(ArrayList<Client> adapterList) {
            ArrayList<Client> clientList = new ArrayList<>();
            int docTotal = adapterList.size();
            int docCount = 0;
            int docDisplay = 0;
            for (Client client : adapterList) {
                if (docCount++ * 100 / docTotal > docDisplay) {
                    publishProgress(String.format(Locale.UK, "Calculating Session Totals %d%% of %d Clients",
                            ++docDisplay,
                            docTotal));
                }
                boolean include = false;
                // If no date range set then all clients should be included
                if (startDate.getTime() == Long.MIN_VALUE && endDate.getTime() == Long.MAX_VALUE) {
                    include = true;
                } else {
                    // Include case if open before end date and close after start date (or still open)
                    if (client.getStartCase() != null) {
                        if (client.getStartCase().getReferenceDate().before(endDate)) {
                            if (client.getCurrentCase() != null) {
                                Case currentCase = client.getCurrentCase();
                                if (currentCase.getCaseType().equals("Close")) {
                                    if (currentCase.getReferenceDate().after(startDate)) {
                                        include = true;
                                    }
                                } else {
                                    // Case still in progress
                                    include = true;
                                }
                            } else {
                                // Current Case should always exist (=Start Case if no newer case record)
                                include = true;
                            }
                        }
                    }
                }
                if (include) {
                    updateSessionTotals(client);
                    clientList.add(client);
                }
            }

            return clientList;
        }

        private void updateSessionTotals(Client client) {

            client.setSessionsOffered(0);
            client.setSessionsAttended(0);
            client.setSessionsCancelled(0);
            client.setSessionsDNA(0);
            client.setAttendanceScore(0);
            // Get the client sessions for the adapterList clients (ordered by ClientID)
            ArrayList<ClientSession> clientSessions =
                    localDB.getAllClientSessions(client, startDate, endDate);
            if (clientSessions.size() > 0) {
                // Initialise the counters
                boolean startCaseFound = false;
                Case startCase = client.getStartCase();
                if (startCase != null) {
                    startCaseFound = true;
                }
                int sessionsOffered = 0;
                int sessionsAttended = 0;
                int sessionsCancelled = 0;
                int sessionsDNA = 0;
                int attendanceScore = 0;
                Date today = new Date();
                for (ClientSession clientSession : clientSessions) {
                    // Build 208 Ignore spurious ClientSession documents without a linked session.
                    Session session = clientSession.getSession();
                    if (session != null) {
                        // Process this session
                        // Offered counter only counts sessions prior to Case Start
                        if (!startCaseFound || startCase.getReferenceDate().after(clientSession.getReferenceDate())) {
                            sessionsOffered++;
                        }
                        if (clientSession.getCancelledFlag()) {
                            sessionsCancelled++;
                        } else if (clientSession.isAttended()) {
                            sessionsAttended++;
                            attendanceScore += clientSession.getSession().getAttendancePoints();
                        } else {
                            // Could be in the future
                            if (clientSession.getReferenceDate().before(today)) {
                                // Build 226 - Only score negatively for ad-hoc sessions
                                if (session.getGroupID().equals(Group.adHocGroupID)) {
                                    sessionsDNA++;
                                    attendanceScore -= clientSession.getSession().getAttendancePoints();
                                }
                            }
                        }
                    }
                }

                client.setSessionsOffered(sessionsOffered);
                client.setSessionsAttended(sessionsAttended);
                client.setSessionsCancelled(sessionsCancelled);
                client.setSessionsDNA(sessionsDNA);
                client.setAttendanceScore(attendanceScore);
            }

        }


        @Override
        protected void onPreExecute() {
            resultView.setText("Starting export...");
            mProgress.show();
        }


        @Override
        protected void onPostExecute(String output) {
            mProgress.hide();
            resultView.setText(output);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            // Runs on UI Thread
            //resultView.setText(values[0]);
            mProgress.setMessage(values[0]);
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            SheetTest.REQUEST_AUTHORIZATION);
                } else {
                    // Re-throw the error to log the System Error
                    throw new CRISException(mLastError);
                }
            } else {
                resultView.setText("Request cancelled.");
            }
        }
    }

    private class DocumentSelector {

        private int documentType;

        public int getDocumentType() {
            return documentType;
        }

        public void setDocumentType(int documentType) {
            this.documentType = documentType;
        }

        private CheckBox checkBox;

        public CheckBox getCheckBox() {
            return checkBox;
        }

        public void setCheckBox(CheckBox checkBox) {
            this.checkBox = checkBox;
        }

        private boolean isChecked;

        public boolean isChecked() {
            return isChecked;
        }

        public void setChecked(boolean checked) {
            isChecked = checked;
        }
    }

}
