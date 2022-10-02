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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.CRISActivity;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.ChangePassword;
import solutions.cris.edit.EditUser;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.Contact;
import solutions.cris.object.Document;
import solutions.cris.object.MyWeek;
import solutions.cris.object.Note;
import solutions.cris.object.NoteType;
import solutions.cris.object.PdfDocument;
import solutions.cris.object.Role;
import solutions.cris.object.Transport;
import solutions.cris.object.User;
import solutions.cris.read.ReadClientHeader;
import solutions.cris.read.ReadUser;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.utils.CRISMenuItem;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.ExceptionHandler;

public class ListUnreadDocuments extends CRISActivity {

    private ArrayList<CRISMenuItem> menuItems;
    private UnreadDocumentAdapter adapter;
    public static int swipeValue;

    private ListView listView;
    private LocalDB localDB;
    private User currentUser;
    private Date startTime;
    private int currentPosition = 0;
    private Toolbar toolbar;
    private TextView footer;
    private String footerText;
    private int docCount = 0;

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
            setContentView(R.layout.activity_list_with_footer);
            footer = findViewById(R.id.footer);
            toolbar = findViewById(R.id.toolbar);
            toolbar.setTitle(getString(R.string.app_name) + " - Unread Documents");
            setSupportActionBar(toolbar);
            footerText = "";
            this.listView = findViewById(R.id.list_view);
            this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    doReadDocument(position);
                }
            });

            localDB = LocalDB.getInstance();
            // Initialise the list of Unread Documents
            menuItems = new ArrayList<>();
            // Create the Main menu
            adapter = new UnreadDocumentAdapter(this, menuItems);
            // Display in the List View
            listView.setAdapter(adapter);
            // And load it
            new GetUnreadDocuments().execute();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Clear and timings from the footer
        footer.setText(footerText);
        // Check that the unread documents have not been read since the last resume
        UUID currentUserID = currentUser.getUserID();
        for (int j = menuItems.size() - 1; j >= 0; j--) {
            CRISMenuItem menuItem = menuItems.get(j);
            ArrayList<Document> docList = menuItem.getDocumentList();
            for (int i = docList.size() - 1; i >= 0; i--) {
                if (localDB.isRead(docList.get(i).getDocumentID(), currentUserID)) {
                    docList.remove(i);
                    docCount--;
                }
            }
            if (docList.size() == 0) {
                menuItems.remove(j);
            }
        }
        if (docCount > 0) {
            footerText = String.format(Locale.UK, "%d documents", docCount);
            footer.setText(footerText);
        }
        adapter.notifyDataSetChanged();
    }

    private void getUnreadDocuments() {

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

    private void doReadDocument(int position) {
        CRISMenuItem menuItem = menuItems.get(position);
        Document document = menuItem.getDocumentList().get(0);
        // Handle library documents
        if (document.getClientID().equals(Client.nonClientDocumentID)) {
            localDB.read(document, currentUser);
            PdfDocument.displayPDFDocument((PdfDocument) document, this);
        } else {
            Intent intent = new Intent(this, ReadClientHeader.class);
            // User is serializable so can pass as extra to EditUser Activity
            intent.putExtra(Main.EXTRA_UNREAD_MENU_ITEM, menuItem);
            startActivity(intent);
        }
    }

    private class UnreadDocumentAdapter extends ArrayAdapter<CRISMenuItem> {

        UnreadDocumentAdapter(Context context, List<CRISMenuItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public
        @NonNull
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
            String itemTitle = menuItem.getTitle();
            if (menuItem.getDocumentList() != null) {
                itemTitle += "(1 of " + menuItem.getDocumentList().size() + ")";
            }
            convertView.setTag(menuItem);

            if (menuItem.getSummary().length() == 0) {
                viewItemTitle.setVisibility(View.VISIBLE);
                if (menuItem.getSortString().startsWith("99")) {
                    //viewItemTitle.setTextSize(getResources().getDimension(R.dimen.text_size_hint));
                    viewItemTitle.setTextSize(15);
                }
                viewItemTitle.setText(itemTitle);
                viewItemMainText.setVisibility(View.GONE);
                viewItemAdditionalText.setVisibility(View.GONE);
            } else {
                viewItemTitle.setVisibility(View.GONE);
                viewItemMainText.setVisibility(View.VISIBLE);
                viewItemAdditionalText.setVisibility(View.VISIBLE);
                viewItemMainText.setText(itemTitle);
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
                // Check for no time
                SimpleDateFormat sTime = new SimpleDateFormat("HH:mm", Locale.UK);
                Date displayDate = menuItem.getDisplayDate();
                if (sTime.format(displayDate).equals("00:00")) {
                    SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
                    viewItemDate.setText(sDate.format(displayDate));
                } else {
                    SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK);
                    viewItemDate.setText(sDate.format(displayDate));
                }

            }
            return convertView;
        }
    }

    /**
     * An asynchronous task that handles the loading and processing of the KPI data.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class GetUnreadDocuments extends AsyncTask<Void, Integer, String> {

        private ArrayList<Document> LoadUnreadDocuments(User currentUser) {
            ArrayList<Document> documents = new ArrayList<>();

            // Build 158 - Optimisation
            boolean readAllDocuments = false;
            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_ALL_DOCUMENTS)) {
                readAllDocuments = true;
            }
            boolean readNotes = false;
            if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_READ_NOTES)) {
                readNotes = true;
            }

            Cursor cursor = localDB.getUnreadDocumentsCursor(currentUser);
            int lastProgress = 0;
            int count = 0;
            int total = cursor.getCount();
            if (cursor != null && cursor.getCount() > 0) {
                // Build 151 - Spurious error where cursor exists but index crashes
                // Added try/catch to restart after crash but limit to 10 to
                // prevent an infinite loop if cursor.MoveToNext() does anything odd.
                int exceptionCount = 0;
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    count++;
                    // Build 151 - Trap indexing crash
                    try {
                        // Test the document's visibility. Library documents are always visible
                        // Other documents depend on the user's privileges
                        // 19 Oct 2017 Build 089 Restrict Unread docs to Notes and MyWeek
                        int documentType = cursor.getInt(0);
                        //if (documentType != Document.Session &&
                        //        documentType != Document.ClientSession) {
                        if (documentType == Document.Note ||
                                documentType == Document.MyWeek) {
                            String clientID = cursor.getString(2);
                            boolean addDocument = false;
                            if (clientID.equals(Document.nonClientDocumentID.toString())) {
                                addDocument = true;
                            } else {
                                // Build 158 Move the client test to only when sure we want to add the document
                                //Client client = (Client) getDocument(UUID.fromString(clientID));
                                // Build 098 - test here (instead of Main so that count is correct in Sync)
                                // Due to an earlier bug, there can be spurious documents which are not
                                // linked to a client. These should be ignored
                                //if (client != null) {
                                if (readAllDocuments) {
                                    addDocument = true;
                                } else if (readNotes) {
                                    // From Build 086 READ_NOTES is used for READ_DEMOGRAPHICDOCUMENTS
                                    // NB: Test for Sticky Notes is later
                                    if (documentType == Document.Note) {
                                        addDocument = true;
                                    }
                                }
                                if (addDocument) {
                                    // Final check that the document's creation date is after the follow date
                                    Date docDate = new Date(cursor.getLong(3));
                                    Date followDate = localDB.followStartDate(currentUser.getUserID(), UUID.fromString(clientID));
                                    // Build 101 - Display docs with same date as follow date
                                    //if (!followDate.before(docDate)) {
                                    if (docDate.before(followDate)) {
                                        addDocument = false;
                                    }
                                }
                            }
                            if (addDocument) {
                                Document document = localDB.deSerializeDocument(cursor.getBlob(1), documentType);
                                // Build 086 - Special case for Notes. If READ_NOTES (demographic documents)
                                // only sticky notes are allowed
                                if (documentType == Document.Note) {
                                    Note note = (Note) document;
                                    if (!readAllDocuments) {
                                        if (!note.isStickyFlag()) {
                                            document = null;
                                        }
                                    }
                                    // Build 127 - Remove Text Message, Phone Message and Email Notes
                                    if (note.getNoteType().getItemValue().toLowerCase().equals("text message")) {
                                        document = null;
                                    } else if (note.getNoteType().getItemValue().toLowerCase().equals("phone message")) {
                                        document = null;
                                    } else if (note.getNoteType().getItemValue().toLowerCase().equals("email")) {
                                        document = null;
                                    }
                                }
                                if (document != null) {
                                    // Build 158 - Finally check that client is non-null (See Build 98 fix earlier)
                                    Client client = (Client) localDB.getDocument(UUID.fromString(clientID));
                                    if (client != null) {
                                        documents.add(document);
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // Build 151 - Allow 10 crashes then exit from loop
                        if (exceptionCount < 10) {
                            exceptionCount++;
                        } else {
                            break;
                        }
                    }
                    int progress = 100 * count / total;
                    if (progress > lastProgress) {
                        publishProgress(progress);
                        lastProgress = progress;
                    }

                    cursor.moveToNext();
                }
            }
            cursor.close();
            return documents;
        }

        @Override
        protected String doInBackground(Void... params) {
            LocalDB localDB = LocalDB.getInstance();
            String output = "";
            int count = 0;
            // Remove existing non-static menu items
            for (int i = 0; i < menuItems.size(); i++) {
                if (menuItems.get(i).getDocumentList() != null) {
                    menuItems.remove(i);
                }
            }
            // Get the list of unread documents
            ArrayList<Document> unreadDocuments = LoadUnreadDocuments(currentUser);
            // Sort by ClientID then ReferenceDate
            Collections.sort(unreadDocuments, Document.comparatorUnread);
            // Loop through, creating new MenuItems for each client
            String currentUserID = "";
            CRISMenuItem menuItem = null;
            String menuItemTitle;
            for (int i = 0; i < unreadDocuments.size(); i++) {
                Client client;
                Document document = unreadDocuments.get(i);
                if (!document.getClientID().toString().equals(currentUserID)) {
                    currentUserID = document.getClientID().toString();
                    // Create a new menuitem
                    if (document.getClientID().equals(Document.nonClientDocumentID)) {
                        PdfDocument pdfDocument = (PdfDocument) document;
                        menuItemTitle = "Library ";
                        menuItem = new CRISMenuItem(menuItemTitle,
                                pdfDocument.getPdfType().getItemValue() + " - " + pdfDocument.getSummary(),
                                R.drawable.ic_pdf_document, document.getReferenceDate());
                    } else {
                        // Need Client's name
                        client = (Client) localDB.getDocument(document.getClientID());
                        // Build 098 - Now handled in localDb.getUnreadDocuments
                        // Due to an earlier bug, there can be spurious documents which are not
                        // linked to a client. These should be ignored
                        //if (client == null) {
                        //    continue;
                        //}
                        menuItemTitle = client.getFullName();
                        menuItem = new CRISMenuItem(menuItemTitle,
                                document.getDocumentTypeString(),
                                R.drawable.ic_client_green, document.getReferenceDate(),
                                client.getLastName());
                        // 6/12/2017 Build 097 Moved following code to here to deal with
                        // spurious null client
                        // 19/10/2017 Build 089 Change colour of icon to 'reddest' document
                        // if document is a note
                        if (document.getDocumentType() == Document.Note) {
                            Note noteDocument = (Note) document;
                            NoteType noteType = (NoteType) noteDocument.getNoteType();
                            switch (noteType.getNoteIcon()) {
                                case NoteType.ICON_COLOUR_RED:
                                case NoteType.ICON_COLOUR_RESPONSE_RED:
                                    menuItem.setIcon(R.drawable.ic_client_red);
                                    menuItem.setSortString("031" + client.getLastName());
                                    break;
                                case NoteType.ICON_COLOUR_AMBER:
                                case NoteType.ICON_COLOUR_RESPONSE_AMBER:
                                    if (menuItem.getIcon() != R.drawable.ic_client_red) {
                                        menuItem.setIcon(R.drawable.ic_client_amber);
                                        menuItem.setSortString("032" + client.getLastName());
                                    }
                                    break;
                                default:
                                    // Leave it Green
                            }
                        }
                    }
                    // Build 099 - Moved following code to here to fix programming error
                    // Create an empty document list
                    menuItem.setDocumentList(new ArrayList<Document>());
                    // Add to the list of MenuItems
                    menuItems.add(menuItem);
                }
                if (menuItem != null) {
                    menuItem.getDocumentList().add(document);
                }
            }
            docCount = unreadDocuments.size();
            return String.format(Locale.UK, "%d documents", docCount);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // Runs on UI Thread
            footer.setText(String.format("loading... (%d%%)", values[0]));
        }

        @Override
        protected void onPreExecute() {
            // Runs on UI Thread
            startTime = new Date();     // Used to display execution time
            footer.setText("loading...");
        }

        @Override
        protected void onPostExecute(String output) {
            // Runs on UI Thread
            // Set the footer text
            footerText = output;
            Date endTime = new Date();
            long elapsed = (endTime.getTime() - startTime.getTime()) / 1000;
            if (elapsed > 0) {
                footer.setText(String.format("%s (%d sec)", footerText, elapsed));
            } else {
                footer.setText(footerText);
            }

            adapter.notifyDataSetChanged();
        }


    }

}
