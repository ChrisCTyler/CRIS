package solutions.cris.edit;
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
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Document;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.User;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.PickList;

/**
 * Copyright CRIS.Solutions 06/12/2016.
 */

public class EditPdfDocument extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private LocalDB localDB;
    private View parent;
    private User currentUser;
    private PdfDocument editDocument;
    private PickList pdfPickList;
    File pdfFile = null;
    private boolean isNewMode;

    // UI references.
    private EditText titleView;
    private EditText issueDateView;
    private Spinner pdfTypeView;
    private Spinner fileNameView;

    private TextView hintTextView;
    private boolean hintTextDisplayed = true;
    private TextView summaryHintTextView;
    private boolean summaryHintTextDisplayed = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.edit_pdf_document, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        localDB = LocalDB.getInstance();
        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        FloatingActionButton fab;
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        currentUser = ((ListActivity)getActivity()).getCurrentUser();
        editDocument = (PdfDocument) ((ListActivity) getActivity()).getDocument();
        fab = ((ListActivity) getActivity()).getFab();
        if (((ListActivity) getActivity()).getMode() == Document.Mode.NEW) {
            isNewMode = true;
        }

        if (isNewMode){
            toolbar.setTitle(getString(R.string.app_name) + " - New Pdf Document");
        } else {
            toolbar.setTitle(getString(R.string.app_name) + " - Edit Pdf Document");
        }

        // Hide the FAB
        fab.setVisibility(View.GONE);

        // Clear the footer
        footer.setText("");

        // CANCEL BOX
        if (editDocument.getCancelledFlag()) {
            LinearLayout cancelBoxView = (LinearLayout) parent.findViewById(R.id.cancel_box_layout);
            cancelBoxView.setVisibility(View.VISIBLE);
            TextView cancelBy = (TextView) parent.findViewById(R.id.cancel_by);
            String byText = "by ";
            User cancelUser = localDB.getUser(editDocument.getCancelledByID());
            byText += cancelUser.getFullName() + " on ";
            byText += sDate.format(editDocument.getCancellationDate());
            cancelBy.setText(byText);
            TextView cancelReason = (TextView) parent.findViewById(R.id.cancel_reason);
            cancelReason.setText(String.format("Reason: %s",editDocument.getCancellationReason()));
        }

        // Set up the form.
        titleView = (EditText) parent.findViewById(R.id.pdf_document_title);
        issueDateView = (EditText) parent.findViewById(R.id.pdf_document_issue_date);
        fileNameView = (Spinner) parent.findViewById(R.id.file_name_spinner);
        pdfTypeView = (Spinner) parent.findViewById(R.id.pdf_type_spinner);

        // Initialise the Pdf Type Spinner
        pdfPickList = new PickList(localDB, ListType.CLIENT_PDF_TYPE);
        ArrayAdapter<String> pdfAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, pdfPickList.getOptions());
        pdfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pdfTypeView.setAdapter(pdfAdapter);
        pdfTypeView.setSelection(pdfPickList.getDefaultPosition());

        // Set up the hint text
        hintTextView = (TextView) parent.findViewById(R.id.hint_text);
        hintTextView.setText(getHintText());
        toggleHint();
        hintTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleHint();
            }
        });

        // Set up the Summary hint text
        summaryHintTextView = (TextView) parent.findViewById(R.id.summary_hint_text);
        summaryHintTextView.setText(getSummaryHintText());
        toggleSummaryHint();
        summaryHintTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSummaryHint();
            }
        });

        // Initialise the Filename spinner
        final ArrayList<String> fileList = new ArrayList<>();
        // Check the state of the external media
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) &&
                !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Not much point continuing of the external media is not available
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("External Media Not Found");
            String message = "Error accessing local storage: " + state;
            builder.setMessage(message);
            // Add the Continue button
            builder.setPositiveButton(R.string.action_continue, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked Continue button
                }
            });
            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
            fileList.add("External media not available.");
        } else {
            // Get the list of PDF files
            File pathCRIS = Environment.getExternalStoragePublicDirectory("CRIS");
            File[] files = new File(pathCRIS.getPath()).listFiles();
            if (files == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("No CRIS Directory found");
                String message = "To enable PDF files to be uploaded into the database, they must be " +
                        "copied to the CRIS directory on the tablet/phone which does not currently exist. " +
                        "Please create the directory (on local storage, not the SD card), add at least one " +
                        "PDF file and then try again.";
                builder.setMessage(message);
                // Add the Continue button
                builder.setPositiveButton(R.string.action_continue, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked Continue button
                    }
                });
                // Create the AlertDialog
                AlertDialog dialog = builder.create();
                dialog.show();
                fileList.add("No CRIS Directory found.");
            } else {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith("pdf")) {
                        fileList.add(file.getName());
                    }
                }
            }
            if (isNewMode) {
                if (fileList.size() == 0) {
                    fileList.add(0, "No PDF files in CRIS folder");
                } else if (fileList.size() > 1) {
                    fileList.add(0, "Please select a file");
                }
            } else {
                fileList.add(0, "Use existing file");
            }
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, fileList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fileNameView.setAdapter(dataAdapter);
        fileNameView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (titleView.getText().length() == 0 || issueDateView.getText().length() == 0) {
                    String sFileName = fileList.get(position);
                    if (sFileName.toLowerCase().endsWith("pdf")) {
                        File pathCRIS = Environment.getExternalStoragePublicDirectory("CRIS");
                        pdfFile = new File(pathCRIS, sFileName);
                        if (titleView.getText().length() == 0) {
                            titleView.setText(sFileName.substring(0, sFileName.length() - 4));
                        }
                        if (issueDateView.getText().length() == 0) {
                            Date lastModified = new Date(pdfFile.lastModified());
                            SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);
                            issueDateView.setText(sDate.format(lastModified));
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Initialise the Issuedate date picker
        issueDateView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                issueDatePicker();
                return true;
            }
        });

        // Cancel Button
        Button cancelButton = (Button) parent.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Cancel so no need to update list of documents
                ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.popBackStack();
            }
        });
        // Save Button
        Button saveButton = (Button) parent.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {
                    if (save()) {
                        FragmentManager fragmentManager = getFragmentManager();
                        fragmentManager.popBackStack();
                    }
                }
            }
        });

        // Load initial values
        if (editDocument.getSummary() != null) {
            titleView.setText(editDocument.getSummary(), null);
            Date issueDate = editDocument.getReferenceDate();
            issueDateView.setText(sDate.format(issueDate.getTime()));
            int position = pdfPickList.getOptions().indexOf(editDocument.getPdfType().getItemValue());
            pdfTypeView.setSelection(position);
        } else {
            issueDateView.setText(sDate.format(new Date()));
        }

    }

    // MENU BLOCK
    private static final int MENU_CANCEL_DOCUMENT = Menu.FIRST + 1;
    private static final int MENU_UNCANCEL_DOCUMENT = Menu.FIRST + 2;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Initialise the Cancellation menu option
        if (editDocument.getCancelledFlag()) {
            MenuItem cancelOption = menu.add(0,MENU_UNCANCEL_DOCUMENT, 2, "Remove Cancellation");
            cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        } else {
            MenuItem cancelOption = menu.add(0,MENU_CANCEL_DOCUMENT, 3, "Cancel Document");
            cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        // Share option only exists if called from ListClientHeader
        if (shareOption != null) {
            shareOption.setVisible(false);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case MENU_CANCEL_DOCUMENT:
                cancelDocument(true);
                return true;

            case MENU_UNCANCEL_DOCUMENT:
                cancelDocument(false);
                return true;

            default:
                return false;
        }
    }

    private void cancelDocument(boolean cancelType) {
        if (cancelType) {
            // Get the reason and then call the validate/save sequence.
            final EditText editText = new EditText(getActivity());
            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            new AlertDialog.Builder(getActivity())
                    .setView(editText)
                    .setTitle("Cancel Document")
                    .setMessage("Documents may not be removed, but cancelling them " +
                            "will remove them from view unless the user explicitly requests " +
                            "them. Please specify a cancellation reason")
                    .setPositiveButton("CancelDocument", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (editText.getText().length() > 0) {
                                editDocument.setCancellationDate(new Date());
                                editDocument.setCancellationReason(editText.getText().toString());
                                editDocument.setCancelledByID(currentUser.getUserID());
                                editDocument.setCancelledFlag(true);
                                if (validate()) {
                                    if (save()) {
                                        FragmentManager fragmentManager = getFragmentManager();
                                        fragmentManager.popBackStack();
                                    }
                                }
                            }
                        }
                    })
                    .setNegativeButton("DoNotCancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        } else {  // Uncancel the Document
            editDocument.setCancelledFlag(false);
            editDocument.setCancellationReason("");
            editDocument.setCancellationDate(new Date(Long.MIN_VALUE));
            editDocument.setCancelledByID(null);
            if (validate()) {
                if (save()) {
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.popBackStack();
                }
            }
        }
    }


    private void issueDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog endDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                issueDateView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        endDatePickerDialog.show();
    }

    private void toggleHint() {
        if (hintTextDisplayed) {
            hintTextView.setMaxLines(2);
            hintTextDisplayed = false;
        } else {
            hintTextDisplayed = true;
            hintTextView.setMaxLines(hintTextView.getLineCount());
        }
    }

    private void toggleSummaryHint() {
        if (summaryHintTextDisplayed) {
            summaryHintTextView.setMaxLines(2);
            summaryHintTextDisplayed = false;
        } else {
            summaryHintTextDisplayed = true;
            summaryHintTextView.setMaxLines(summaryHintTextView.getLineCount());
        }
    }

    // Validate the document
    private boolean validate() {
        boolean success = true;

        // Clear any existing errors
        titleView.setError(null);
        issueDateView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        // Title
        String sTitle = titleView.getText().toString().trim();
        if (TextUtils.isEmpty(sTitle)) {
            titleView.setError(getString(R.string.error_field_required));
            focusView = titleView;
            success = false;
        } else {
            editDocument.setSummary(sTitle);
        }
        // IssueDate
        String sIssueDate = issueDateView.getText().toString();
        if (TextUtils.isEmpty(sIssueDate)) {
            issueDateView.setError(getString(R.string.error_field_required));
            focusView = issueDateView;
            success = false;
        } else {
            Date dIssueDate = CRISUtil.parseDate(sIssueDate);
            if (dIssueDate == null) {
                issueDateView.setError(getString(R.string.error_invalid_date));
                focusView = issueDateView;
                success = false;
            } else {
                editDocument.setReferenceDate(dIssueDate);
            }
        }

        // PdfType
        ListItem selectedListItem = pdfPickList.getListItems().get(pdfTypeView.getSelectedItemPosition());
        if (selectedListItem.getItemOrder() == -1) {
            // Please select
            TextView errorText = (TextView) pdfTypeView.getSelectedView();
            errorText.setError("anything here, just to add the icon");
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = pdfTypeView;
            success = false;
        } else {
            editDocument.setPdfTypeID(selectedListItem.getListItemID());
            editDocument.setPdfType(selectedListItem);
        }


        // FileName
        String sFileName = String.valueOf(fileNameView.getSelectedItem());
        if (sFileName.toLowerCase().endsWith("pdf")) {
            File pathCRIS = Environment.getExternalStoragePublicDirectory("CRIS");
            pdfFile = new File(pathCRIS, sFileName);
        } else if (sFileName.compareTo("Use existing file") != 0) {
            TextView errorText = (TextView) fileNameView.getSelectedView();
            errorText.setError("anything here, just to add the icon");
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = fileNameView;
            success = false;
        }

        if (!success) {
            focusView.requestFocus();
            focusView.requestFocusFromTouch();
        }
        return success;
    }

    private boolean save() {
        if (pdfFile != null) {
            byte[] buffer = new byte[(int) pdfFile.length()];
            InputStream ios = null;
            try {
                ios = new FileInputStream(pdfFile);
                if (ios.read(buffer) == -1) {
                    throw new CRISException("EOF reached while trying to read the whole PDF file: " + pdfFile.getPath());
                }
                // Store the content in Blobs table
                editDocument.setBlobID(localDB.saveBlob(buffer));
            } catch (Exception ex) {
                throw new CRISException("Unexpected exception (" + ex.getMessage() + ") processing PDF file: " + pdfFile.getPath());
            } finally {
                try {
                    if (ios != null)
                        ios.close();
                } catch (IOException e) {
                    // ios was not open
                }
            }
        }
        editDocument.save(isNewMode);
        // Delete the temporary file from disk
        if (pdfFile != null && pdfFile.exists()) {
            if (!pdfFile.delete()){
                throw new CRISException("Unable to delete temporary pdf file");
            }
        }
        return true;
    }
    private String getHintText() {
        String hintText = "";
        if (!isNewMode) {
            hintText = "You can select a Pdf file from the list below. However, if you are simply changing " +
                    "the title or issue date, do not choose a Pdf and the existing file will be used.\n";
        }
        hintText += "To import a new Pdf, copy the file into the CRIS directory on " +
                "the smartphone/tablet and it will then appear in the list below. If this is the first " +
                "file to be imported on this device, you will have to create a CRIS directory. The " +
                "directory must be named CRIS and must be on the smartphone/tablet, not on the SD card.";
        return hintText;
    }

    private String getSummaryHintText() {
        return "The first line of the summary should be used as the title and will be displayed in the " +
                "document list. If the summary contains more than one line, the summary will be " +
                "displayed to the user before displaying the Pdf document itself. This enables " +
                "the summary to be used to highlight the main points of the Pdf document so that " +
                "the user does not necessarily have to read the whole document";

    }

}
