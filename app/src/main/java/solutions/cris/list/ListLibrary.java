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
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
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
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.CRISActivity;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.EditLibraryDocument;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Client;
import solutions.cris.object.Document;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.Role;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;

public class ListLibrary extends CRISActivity {

    public static final String INCLUDE_CANCELLED = "solutions.cris.IncludeCancelled";

    private ListView listView;
    public static List<Document> dbDocuments;
    public static ArrayList<Document> adapterList;
    private LocalDB localDB;
    private User currentUser;
    private boolean includeCancelled = false;
    private int selectedPosition;
    private boolean isNewMode;

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
            setContentView(R.layout.activity_list_with_action);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // Check whether we're recreating a previously destroyed instance
            if (savedInstanceState != null) {
                // Restore value of members from saved state
                includeCancelled = savedInstanceState.getBoolean(INCLUDE_CANCELLED);
            }

            this.listView = (ListView) findViewById(R.id.list_view);

            this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    doReadDocument(position);
                }
            });
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    tryEditPdfDocument(position, false);
                    return true;
                }
            });

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_LIBRARY_DOCUMENTS)) {
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        tryEditPdfDocument(0, true);
                    }
                });
            } else {
                // Hide New Client option
                fab.setVisibility(View.GONE);
            }
            localDB = LocalDB.getInstance();
            UUID clientID = UUID.fromString(getIntent().getStringExtra(Main.EXTRA_CLIENT_ID));
            // Load the documents for special client (ClientID=DOCUMENT) from the database
            dbDocuments = localDB.getAllDocumentsOfType(clientID, Document.PdfDocument);
            // Sort the list by type
            Collections.sort(dbDocuments, Document.comparatorTypeLib);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Create the adapter
        adapterList = new ArrayList<>();
        DocumentAdapter adapter = new DocumentAdapter(this, adapterList);
        for (Document document : dbDocuments) {
            if (includeCancelled || !document.getCancelledFlag()) {
                adapterList.add(document);
            }
        }
        this.listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list_library, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_sort_az:
                Collections.sort(dbDocuments, Document.comparatorAZ);
                onResume();
                return true;

            case R.id.action_sort_type:
                Collections.sort(dbDocuments, Document.comparatorTypeLib);
                onResume();
                return true;

            case R.id.action_show_cancelled_documents:
                // Re-load the documents from the database
                includeCancelled = true;
                onResume();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the state
        savedInstanceState.putBoolean(INCLUDE_CANCELLED, includeCancelled);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CRIS.pdf");
        if (file.exists()) {
            if (!file.delete()) {
                throw new CRISException("Failed to delete temporary file CRIS.pdf in Directory_Documents");
            }
        }
    }

    private void doReadDocument(int position) {
        Document document = adapterList.get(position);
        localDB.read(document, currentUser);
        PdfDocument.displayPDFDocument((PdfDocument)document, this);
    }

    private void tryEditPdfDocument(int position, boolean mode) {
        if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_LIBRARY_DOCUMENTS)) {
            selectedPosition = position;
            isNewMode = mode;
            // Editing Pdfs needs WRITE_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                doEditPdfDocument();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Main.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private boolean doEditPdfDocument() {
        Intent intent = new Intent(this, EditLibraryDocument.class);
        if (isNewMode) {
            PdfDocument newDocument = new PdfDocument(currentUser, Client.nonClientDocumentID);
            intent = new Intent(this, EditLibraryDocument.class);
            // PdfDocument is serializable so can pass as extra to Activity
            intent.putExtra(Main.EXTRA_IS_NEW_MODE, true);
            intent.putExtra(Main.EXTRA_DOCUMENT, newDocument);
            startActivity(intent);
        } else {
            // PdfDocument is serializable so can pass as extra to Activity
            intent.putExtra(Main.EXTRA_IS_NEW_MODE, false);
            intent.putExtra(Main.EXTRA_LIST_POSITION, selectedPosition);
            startActivity(intent);
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Main.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doEditPdfDocument();
                }
                // Else permission denied, so do nothing
                break;
            }
            default:
                throw new CRISException(String.format(Locale.UK, "Unexpected case value: %d", requestCode));
        }
    }

    private class DocumentAdapter extends ArrayAdapter<Document> {

        // Constructor
        DocumentAdapter(Context context, List<Document> objects) {
            super(context, 0, objects);
        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_list_item, parent, false);
            }

            ImageView viewItemIcon = (ImageView) convertView.findViewById(R.id.item_icon);
            TextView viewItemDate = (TextView) convertView.findViewById(R.id.item_date);
            TextView viewItemMainText = (TextView) convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = (TextView) convertView.findViewById(R.id.item_additional_text);

            final Document document = adapterList.get(position);

            SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
            viewItemDate.setText(sDate.format(document.getReferenceDate()));
            if (document.getDocumentType() == Document.PdfDocument) {
                PdfDocument pdfDocument = (PdfDocument) document;
                viewItemMainText.setText(pdfDocument.getPdfType().getItemValue());
                viewItemIcon.setImageDrawable(getDrawable(R.drawable.ic_pdf_document));
            } else {
                viewItemMainText.setText(document.getDocumentTypeString());
                viewItemIcon.setImageDrawable(getDrawable(R.drawable.ic_unknown));
            }
            if (document.getCancelledFlag()) {
                int color = ContextCompat.getColor(convertView.getContext(), R.color.red);
                viewItemMainText.setTextColor(color);
                viewItemAdditionalText.setTextColor(color);
                viewItemDate.setTextColor(color);
            }

            viewItemAdditionalText.setText(document.getSummaryLine1());
            return convertView;
        }
    }

}
