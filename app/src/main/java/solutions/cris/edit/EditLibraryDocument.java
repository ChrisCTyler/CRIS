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
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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

import solutions.cris.CRISActivity;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.list.ListLibrary;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.User;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.ExceptionHandler;
import solutions.cris.utils.PickList;

public class EditLibraryDocument extends CRISActivity {

    private PdfDocument editPdfDocument;
    private LocalDB localDB;
    private User currentUser;
    private PickList pdfPickList;

    // UI references.
    private EditText titleView;
    private EditText issueDateView;
    private Spinner pdfTypeView;
    private Spinner fileNameView;

    private SimpleDateFormat sDate;
    private boolean isNewMode;
    File pdfFile = null;

    private TextView hintTextView;
    private boolean hintTextDisplayed = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add the global uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        // CurrentUser always exists so if this check fails then exception in child
        // // has rendered system inconsistent so exit and let Main start from scratch()
        currentUser = User.getCurrentUser();
        if (currentUser == null) {
            finish();
        } else {
            localDB = LocalDB.getInstance();
            setContentView(R.layout.activity_edit_pdf_document);

            // Preset sDate for use throughout the activity
            sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);
            sDate.setLenient(false);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            // Get the parameter passed with the Intent
            isNewMode = getIntent().getBooleanExtra(Main.EXTRA_IS_NEW_MODE, false);
            if (isNewMode) {
                editPdfDocument = (PdfDocument) getIntent().getSerializableExtra(Main.EXTRA_DOCUMENT);
                toolbar.setTitle(getString(R.string.app_name) + " - New Pdf Document");
            } else {
                int listPos = getIntent().getIntExtra(Main.EXTRA_LIST_POSITION, 0);
                editPdfDocument = (PdfDocument) ListLibrary.adapterList.get(listPos);
                toolbar.setTitle(getString(R.string.app_name) + " - Edit Pdf Document");
            }
            setSupportActionBar(toolbar);

            // Set up the form.
            titleView = (EditText) findViewById(R.id.pdf_document_title);
            issueDateView = (EditText) findViewById(R.id.pdf_document_issue_date);
            fileNameView = (Spinner) findViewById(R.id.file_name_spinner);
            pdfTypeView = (Spinner) findViewById(R.id.pdf_type_spinner);

            // CANCEL BOX
            if (editPdfDocument.getCancelledFlag()) {
                LinearLayout cancelBoxView = (LinearLayout) findViewById(R.id.cancel_box_layout);
                cancelBoxView.setVisibility(View.VISIBLE);
                TextView cancelBy = (TextView) findViewById(R.id.cancel_by);
                String byText = "by ";
                User cancelUser = localDB.getUser(editPdfDocument.getCancelledByID());
                byText += cancelUser.getFullName() + " on ";
                byText += sDate.format(editPdfDocument.getCancellationDate());
                cancelBy.setText(byText);
                TextView cancelReason = (TextView) findViewById(R.id.cancel_reason);
                cancelReason.setText(String.format("Reason: %s", editPdfDocument.getCancellationReason()));
            }

            // Initialise the Pdf Type Spinner
            pdfPickList = new PickList(localDB, ListType.LIBRARY_PDF_TYPE);
            ArrayAdapter<String> pdfAdapter = new
                    ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pdfPickList.getOptions());
            pdfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            pdfTypeView.setAdapter(pdfAdapter);
            pdfTypeView.setSelection(pdfPickList.getDefaultPosition());

            // Set up the hint text
            hintTextView = (TextView) findViewById(R.id.hint_text);
            hintTextView.setText(getHintText());
            // Restore value of hintDisplayed (Set to opposite, toggle to come
            if (savedInstanceState != null) {
                hintTextDisplayed = !savedInstanceState.getBoolean(Main.HINT_DISPLAYED);
            }
            toggleHint();
            hintTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleHint();
                }
            });

            // Initialise the Filename spinner
            final ArrayList<String> fileList = new ArrayList<>();
            // Check the state of the external media
            String state = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(state) &&
                    !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                // Not much point continuing of the external media is not available
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

            ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
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
            Button cancelButton = (Button) findViewById(R.id.cancel_button);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
            // Save Button
            Button saveButton = (Button) findViewById(R.id.save_button);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (validate()) {
                        if (save()) {
                            finish();
                        }
                    }
                }
            });
        }
        // Load initial values
        if (editPdfDocument.getSummary() != null) {
            titleView.setText(editPdfDocument.getSummary(), null);
            Date issueDate = editPdfDocument.getReferenceDate();
            issueDateView.setText(sDate.format(issueDate.getTime()));
            int position = pdfPickList.getOptions().indexOf(editPdfDocument.getPdfType().getItemValue());
            pdfTypeView.setSelection(position);
        }
    }

    // MENU BLOCK
    private static final int MENU_CANCEL_DOCUMENT = Menu.FIRST + 1;
    private static final int MENU_UNCANCEL_DOCUMENT = Menu.FIRST + 2;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_empty, menu);
        super.onCreateOptionsMenu(menu);
        // CANCEL DOCUMENT
        if (editPdfDocument.getCancelledFlag()) {
            MenuItem cancelOption = menu.add(0, MENU_UNCANCEL_DOCUMENT, 2, "Remove Cancellation");
            cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        } else {
            MenuItem cancelOption = menu.add(0, MENU_CANCEL_DOCUMENT, 3, "Cancel Document");
            cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return true;
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
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void issueDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog endDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                issueDateView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        endDatePickerDialog.show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the state
        savedInstanceState.putBoolean(Main.HINT_DISPLAYED, hintTextDisplayed);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
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

    private void cancelDocument(boolean cancelType) {
        if (cancelType) {
            // Get the reason and then call the validate/save sequence.
            final EditText editText = new EditText(this);
            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            new AlertDialog.Builder(this)
                    .setView(editText)
                    .setTitle("Cancel Document")
                    .setMessage("Documents may not be removed, but cancelling them " +
                            "will remove them from view unless the user explicitly requests " +
                            "them. Please specify a cancellation reason")
                    .setPositiveButton("CancelDocument", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (editText.getText().length() > 0) {
                                editPdfDocument.setCancellationDate(new Date());
                                editPdfDocument.setCancellationReason(editText.getText().toString());
                                editPdfDocument.setCancelledByID(currentUser.getUserID());
                                editPdfDocument.setCancelledFlag(true);
                                if (validate()) {
                                    if (save()) {
                                        finish();
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
            editPdfDocument.setCancelledFlag(false);
            editPdfDocument.setCancellationReason("");
            editPdfDocument.setCancellationDate(new Date(Long.MIN_VALUE));
            editPdfDocument.setCancelledByID(null);
            if (validate()) {
                if (save()) {
                    finish();
                }
            }
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
            editPdfDocument.setSummary(sTitle);
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
                editPdfDocument.setReferenceDate(dIssueDate);
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
            editPdfDocument.setPdfTypeID(selectedListItem.getListItemID());
            editPdfDocument.setPdfType(selectedListItem);
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
                editPdfDocument.setBlobID(localDB.saveBlob(buffer));
            } catch (Exception ex) {
                throw new CRISException("Unexpected exception (" + ex.getMessage() + ") processing PDF file: " + pdfFile.getPath());
            } finally {
                try {
                    if (ios != null) {
                        ios.close();
                    }
                } catch (IOException e) {
                    // Close was not necessary
                }
            }
        }
        editPdfDocument.save(isNewMode);
        if (isNewMode) {
            // Append the new user to the list of users
            ListLibrary.dbDocuments.add(editPdfDocument);
        }
        // Delete the temporary file from disk
        if (pdfFile != null && pdfFile.exists()) {
            if (!pdfFile.delete()){
                throw new CRISException("Failed to delete temporary pdf file from disk.");
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
}
