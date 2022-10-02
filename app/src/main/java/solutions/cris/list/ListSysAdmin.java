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

import androidx.appcompat.app.AlertDialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.CRISActivity;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Document;
import solutions.cris.object.ListItem;
import solutions.cris.object.MyWeek;
import solutions.cris.object.Note;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.Sync;
import solutions.cris.object.Transport;
import solutions.cris.object.User;
import solutions.cris.sync.WebConnection;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.utils.CRISMenuItem;
import solutions.cris.utils.ExceptionHandler;

public class ListSysAdmin extends CRISActivity {

    private ArrayList<CRISMenuItem> menuItems;
    private User currentUser;
    SysAdminAdapter adapter;
    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK);
    // Build 158
    //private Date startTime;
    // build 179
    boolean inProgress = false;

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
            setContentView(R.layout.activity_list);
            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setTitle(getString(R.string.app_name) + " - System Administration");
            setSupportActionBar(toolbar);

            // Initialise the list of Static Menu Items
            menuItems = new ArrayList<>();
            menuItems.add(new CRISMenuItem("List Manager", "", R.drawable.ic_list, null));
            menuItems.add(new CRISMenuItem("System Errors", "", R.drawable.ic_system_error, null));
            // Build 128 Duplicate Text Message Fix
            menuItems.add(new CRISMenuItem("Remove Duplicate Notes", "", R.drawable.ic_system_error, null));
            // Build 148 Check for missing MyWeek website records
            menuItems.add(new CRISMenuItem("Check MyWeek Downloads", "", R.drawable.ic_system_error, null));
            // Build 181
            //menuItems.add(new CRISMenuItem("Run SQL", "", R.drawable.ic_system_error, null));
            menuItems.add(new CRISMenuItem("Link Session documents", "", R.drawable.ic_system_error, null));

            // Setup the List view listener
            ListView listView = findViewById(R.id.list_view);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String title = ((CRISMenuItem) view.getTag()).getTitle();
                    switch (title) {
                        case "List Manager":
                            doListLists();
                            break;
                        case "System Errors":
                            doListErrors();
                            break;
                        case "Remove Duplicate Notes":
                            doDuplicateNotes();
                            break;
                        case "Check MyWeek Downloads":
                            if (inProgress) {
                                inProgressMessage();
                            } else {
                                inProgress = true;
                                // Load the data in the background
                                new CheckMyWeekDownloads().execute();
                            }
                            break;
                        // Build 181
                        case "Run SQL":
                            doSQL();
                            break;
                        case "Link Session documents":
                            linkSessionDocuments();
                            break;
                        default:
                            throw new CRISException("Invalid main Menu Option: " + title);
                    }
                }
            });


            // Create the Main menu
            adapter = new SysAdminAdapter(this, menuItems);
            // Display in the List View
            listView.setAdapter(adapter);

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

    private void inProgressMessage() {
        new AlertDialog.Builder(this)
                .setTitle("In Progress")
                .setMessage("The process is still in progress, please wait.")
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void doListErrors() {
        Intent intent = new Intent(this, ListErrors.class);
        startActivity(intent);
    }

    private void doDuplicateNotes() {
        SimpleDateFormat sDateTime = new SimpleDateFormat("dd MMM HH:mm", Locale.UK);
        LocalDB localDB = LocalDB.getInstance();
        int count = 0;
        int duplicates = 0;
        String noteList = "";
        String content = "";
        String lastContent = "**Rubbish**";
        String lastClient = "";
        ArrayList<Note> notes = localDB.getAllBroadcastNotes();
        for (Note note : notes) {
            if (note.getNoteType().getItemValue().equals("Text Message")) {
                if (!note.getClientID().toString().equals(lastClient)) {
                    lastContent = "**Rubbish**";
                }
                lastClient = note.getClientID().toString();
                count++;
                content = note.getContent();
                if (content.equals(lastContent)) {
                    localDB.remove(note);
                    duplicates++;
                }
                lastContent = content;
            }
        }
        noteList += String.format("\nNotes Found = %d, Duplicates removed = %d", count, duplicates);
        Intent intent = new Intent(this, AlertAndContinue.class);
        intent.putExtra("title", String.format("Duplicate Notes"));
        intent.putExtra("message", noteList);
        startActivity(intent);
    }

    private void doListLists() {
        Intent intent = new Intent(this, ListListTypes.class);
        startActivity(intent);
    }

    // Build 181 - Load SessionID field in the database from the SessionID field in teh object
    // for Note, MyWeek, Transport and PDF documents. Method is locate the documents to update
    // and re-save them. (Change already made in Save to update the field)
    private void linkSessionDocuments() {
        LocalDB localDB = LocalDB.getInstance();
        User currentUser = User.getCurrentUser();
        String result = "";

        long count = 0;
        long fixed = 0;

        ArrayList<Document> documents = localDB.getAllDocumentsOfType(Document.Transport);
        for (Document document : documents) {
            Transport transport = (Transport) document;
            if (transport.getSessionID() != null && transport.getSessionID().toString().length() > 0) {
                // Check that SessionID is set in the database
                if (!localDB.isSessionIDSet(document.getDocumentID())) {
                    count++;
                    if (fixed < 400) {
                        fixed++;
                        transport.save(false);
                    }
                }
            }
        }

        if (fixed < 400) {
            documents = localDB.getAllDocumentsOfType(Document.PdfDocument);
            for (Document document : documents) {
                PdfDocument pdfDocument = (PdfDocument) document;
                if (pdfDocument.getSessionID() != null && pdfDocument.getSessionID().toString().length() > 0) {
                    // Check that SessionID is set in the database
                    if (!localDB.isSessionIDSet(document.getDocumentID())) {
                        count++;
                        if (fixed < 400) {
                            fixed++;
                            pdfDocument.save(false);
                        }
                    }
                }
            }
        }

        if (fixed < 400) {
            documents = localDB.getAllDocumentsOfType(Document.MyWeek);
            for (Document document : documents) {
                MyWeek myWeek = (MyWeek) document;
                if (myWeek.getSessionID() != null && myWeek.getSessionID().toString().length() > 0) {
                    // Check that SessionID is set in the database
                    if (!localDB.isSessionIDSet(document.getDocumentID())) {
                        count++;
                        if (fixed < 400) {
                            fixed++;
                            myWeek.save(false);
                        }
                    }
                }
            }
        }

        if (fixed < 400) {
            documents = localDB.getAllDocumentsOfType(Document.Note);
            for (Document document : documents) {
                Note note = (Note) document;
                if (note.getSessionID() != null && note.getSessionID().toString().length() > 0) {
                    // Check that SessionID is set in the database
                    if (!localDB.isSessionIDSet(document.getDocumentID())) {
                        count++;
                        if (fixed < 400) {
                            fixed++;
                            note.save(false, currentUser);
                        }
                    }
                }
            }
        }

        if (count == 0) {
            result = "No un-linked session documents found.";
        } else {
            result = String.format("Note: %d of %d documents fixed\n", fixed, count);
        }
        new AlertDialog.Builder(this)
                .setTitle("Link Session Documents")
                .setMessage(result)
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void doSQL() {
        LocalDB localDB = LocalDB.getInstance();
        String result = "";
        long count = 0;
        ArrayList<Document> documents = localDB.getAllDocumentsOfType(Document.PdfDocument);
        for (Document document : documents) {
            PdfDocument pdf = (PdfDocument) document;
            if (pdf.getSessionID() != null && pdf.getSessionID().toString().length() > 0) {
                count++;
            }
        }
        result += String.format("PDF: %d\n", count);

        count = 0;
        documents = localDB.getAllDocumentsOfType(Document.MyWeek);
        for (Document document : documents) {
            MyWeek myWeek = (MyWeek) document;
            if (myWeek.getSessionID() != null && myWeek.getSessionID().toString().length() > 0) {
                count++;
            }
        }
        result += String.format("MyWeek: %d\n", count);

        count = 0;
        ArrayList<Document> transportDocuments = localDB.getAllDocumentsOfType(Document.Transport);
        for (Document document : transportDocuments) {
            Transport transport = (Transport) document;
            if (transport.getSessionID() != null && transport.getSessionID().toString().length() > 0) {
                count++;
            }
        }
        //count = localDB.countAllDocumentsOfTypeWithSessionID(Document.Transport);
        result += String.format("Transport: %d\n", count);

        count = 0;
        ArrayList<Document> notes = localDB.getAllDocumentsOfType(Document.Note);
        for (Document document : notes) {
            Note note = (Note) document;
            if (note.getSessionID() != null && note.getSessionID().toString().length() > 0) {
                count++;
            }
        }
        //count = localDB.countAllDocumentsOfTypeWithSessionID(Document.Note);
        result += String.format("Note: %d\n", count);
        new AlertDialog.Builder(this)
                .setTitle("SQL Results")
                .setMessage(result)
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private class SysAdminAdapter extends ArrayAdapter<CRISMenuItem> {

        SysAdminAdapter(Context context, List<CRISMenuItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_list_item, parent, false);
            }

            ImageView viewItemIcon = convertView.findViewById(R.id.item_icon);
            TextView viewItemDate = convertView.findViewById(R.id.item_date);
            TextView viewItemMainText = convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = convertView.findViewById(R.id.item_additional_text);
            TextView viewItemTitle = convertView.findViewById(R.id.item_title);
            ProgressBar syncProgress = convertView.findViewById(R.id.sync_progress);

            final CRISMenuItem menuItem = menuItems.get(position);
            convertView.setTag(menuItem);

            if (menuItem.getSummary().length() == 0) {
                viewItemTitle.setVisibility(View.VISIBLE);
                viewItemTitle.setText(menuItem.getTitle());
                viewItemMainText.setVisibility(View.GONE);
                viewItemAdditionalText.setVisibility(View.GONE);
            } else {
                viewItemTitle.setVisibility(View.GONE);
                viewItemMainText.setVisibility(View.VISIBLE);
                viewItemAdditionalText.setVisibility(View.VISIBLE);
                viewItemMainText.setText(menuItem.getTitle());
                viewItemAdditionalText.setText(menuItem.getSummary());
            }
            if (menuItem.getIcon() == 0) {
                syncProgress.setVisibility(View.VISIBLE);
                viewItemIcon.setVisibility(View.GONE);
            } else {
                syncProgress.setVisibility(View.GONE);
                viewItemIcon.setVisibility(View.VISIBLE);
                viewItemIcon.setImageDrawable(getDrawable(menuItem.getIcon()));
            }
            if (menuItem.getDisplayDate() != null) {
                viewItemDate.setText(sDate.format(menuItem.getDisplayDate()));
            }
            return convertView;
        }
    }

    /**
     * An asynchronous task that handles the loading and processing of the KPI data.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class CheckMyWeekDownloads extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            LocalDB localDB = LocalDB.getInstance();
            int count = 0;
            String output = "Initiating check...\n";
            try {
                WebConnection webConnection = new WebConnection(localDB);
                //JSONObject jsonOutput = webConnection.post("pdo_get_all_myweek_website_records.php");
                JSONObject jsonOutput = webConnection.post("pdo_get_all_myweek_website_records.php");
                String result = jsonOutput.getString("result");
                if (result.equals("FAILURE")) {
                    output += "FAILURE: " + jsonOutput.getString("error_message");
                } else {
                    output += localDB.checkWebsiteMyWeeks(jsonOutput);
                }
            } catch (JSONException ex) {
                output += "JSON Error in CheckMyWeekDownloads(): " + ex.getMessage();

            } catch (Exception ex) {
                output += "Error in CheckMyWeekDownloads(): " + ex.getMessage();
            }
            return output;
        }

        @Override
        protected void onPreExecute() {
            // Runs on UI Thread

        }

        @Override
        protected void onPostExecute(String output) {
            inProgress = false;
            // Runs on UI Thread
            Intent intent = new Intent(getBaseContext(), AlertAndContinue.class);
            intent.putExtra("title", String.format("Missing Website MyWeeks"));
            intent.putExtra("message", output);
            startActivity(intent);
        }


    }

}
