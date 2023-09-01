package solutions.cris.list;

import android.Manifest;
// Build 200 Use the androidX Fragment class
//import android.app.Fragment;
//import android.app.FragmentManager;
//import android.app.FragmentTransaction;
import androidx.fragment.app.Fragment;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.CRISActivity;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.edit.EditCase;
import solutions.cris.edit.EditImage;
import solutions.cris.edit.EditPdfDocument;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.Document;
import solutions.cris.object.Session;
import solutions.cris.object.User;
import solutions.cris.utils.LocalSettings;


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

public abstract class ListActivity extends CRISActivity {

    // Build 125 - CRISExport crashes because export selection does not survive
    // a tablet re-orientation.
    // These variables are set by callbacks in fragments and need to be
    // restored via saved instance state to survive a tablet orientation reload
    public static final String EXPORT_LIST_TYPE = "solutions.cris.ExportListType";

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the state
        savedInstanceState.putString(EXPORT_LIST_TYPE, getExportListType());
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    protected ListActivity() {
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            String exportSelection = savedInstanceState.getString(EXPORT_LIST_TYPE);
            setExportListType(exportSelection);
        }
    }

    private FloatingActionButton fab;
    public FloatingActionButton getFab() {
        return fab;
    }
    public void setFab(FloatingActionButton fab) {
        this.fab = fab;
    }

    private Document.Mode mode;
    public Document.Mode getMode() {
        return mode;
    }
    public void setMode(Document.Mode mode) {
        this.mode = mode;
    }

    // Used for the Client in ListClientHeader fragments
    private Client client;
    public Client getClient() {
        return client;
    }
    public void setClient(Client client) {
        this.client = client;
    }

    // Used to identify the selected document in ListClients and ListClientHeader
    private Document document;
    public Document getDocument() {
        return document;
    }
    public void setDocument(Document document) {
        this.document = document;
    }

    private Toolbar toolbar;
    public Toolbar getToolbar() {
        return toolbar;
    }
    public void setToolbar(Toolbar toolbar) {
        this.toolbar = toolbar;
    }

    private User currentUser;
    public User getCurrentUser() {
        return currentUser;
    }
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    private Session session;
    public Session getSession() {
        return session;
    }
    public void setSession(Session session) {
        this.session = session;
    }

    private String exportListType;
    public String getExportListType() {
        return exportListType;
    }
    public void setExportListType(String exportListType) {
        this.exportListType = exportListType;
    }

    private String exportSelection;

    public String getExportSelection() {
        return exportSelection;
    }

    public void setExportSelection(String exportSelection) {
        this.exportSelection = exportSelection;
    }

    private String exportSort;

    public String getExportSort() {
        return exportSort;
    }

    public void setExportSort(String exportSort) {
        this.exportSort = exportSort;
    }

    private String exportSearch;

    public String getExportSearch() {
        return exportSearch;
    }

    public void setExportSearch(String exportSearch) {
        this.exportSearch = exportSearch;
    }

    private ArrayList<Client> clientAdapterList;

    public ArrayList<Client> getClientAdapterList() {
        return clientAdapterList;
    }

    public void setClientAdapterList(ArrayList<Client> clientAdapterList) {
        this.clientAdapterList = clientAdapterList;
    }

    public ArrayList<Session> sessionAdapterList;

    public ArrayList<Session> getSessionAdapterList() {
        return sessionAdapterList;
    }

    public void setSessionAdapterList(ArrayList<Session> sessionAdapterList) {
        this.sessionAdapterList = sessionAdapterList;
    }

    // Build 119 30 May 2019 Added array of clients for passing the client list
    // This is read in the BroadcastMessageFragment Fragment and written in the ListClients and
    // ListSessionClients Fragments.
    private ArrayList<Client> broadcastClientList;

    public ArrayList<Client> getBroadcastClientList() {
        return broadcastClientList;
    }

    public void setBroadcastClientList(ArrayList<Client> broadcastClientList) {
        this.broadcastClientList = broadcastClientList;
    }

    // Build 200 - Replaced single selection with checkbox selection for picklists
    // Build 232 - Added REVIEW_OVERDUE
    public enum SelectMode {ALL, OPEN, FOLLOWED, OVERDUE, UNCANCELLED, GROUPS, KEYWORKERS,
        COMMISSIONERS, SCHOOLS, AGENCIES, CONTACT_DOCUMENTS, DOCUMENT_TYPES, FUTURE,
        SESSION_COORDINATORS, NONE, REVIEW_OVERDUE}

    private SelectMode selectMode = SelectMode.OPEN;

    public SelectMode getSelectMode() {
        return selectMode;
    }

    public void setSelectMode(SelectMode selectMode) {
        this.selectMode = selectMode;
    }

    // Build 116 22 May 2019 Add handler for incoming text via share. Trigger is non-empty
    // text on shareText. Which must be accessible in ListClientFragment and EditNote.
    // EditNote fragment may be invoked from ListSessionClients as well as ListClients(Header)
    private String shareText = "";
    // Build 116 22 May 2019 Add handler for incoming text via share
    public String getShareText() {return shareText;}
    public void setShareText(String shareText){this.shareText = shareText;}

    // This code is here rather than in the ListClientDocuments fragment because
    // the callback onRequestPermissionResult must be declared in this activity
    // not one of its fragments
    private int docType;

    public void tryEditFileDocument(Document.Mode mode, int docType) {
        setMode(mode);
        this.docType = docType;
        // Editing Pdfs needs WRITE_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            doEditFileDocument();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Main.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
    }

    private boolean doEditFileDocument() {
        // Build 200 Use AndroidX fragment class
        //FragmentManager fragmentManager = getFragmentManager();
        //FragmentTransaction fragmentTransaction;
        //Fragment fragment;
        switch (getMode()) {
            case NEW:
            case EDIT:
                //fragmentTransaction = fragmentManager.beginTransaction();
                if (docType == Document.PdfDocument) {
                    //fragment = new EditPdfDocument();
                    getSupportFragmentManager().beginTransaction()
                            .addToBackStack(null)
                            .setReorderingAllowed(true)
                            .replace(R.id.content, EditPdfDocument.class, null)
                            .commit();
                } else if (docType == Document.Image) {
                    //fragment = new EditImage();
                    getSupportFragmentManager().beginTransaction()
                            .addToBackStack(null)
                            .setReorderingAllowed(true)
                            .replace(R.id.content, EditImage.class, null)
                            .commit();
                } else {
                    throw new CRISException(
                            String.format(Locale.UK,
                                    "Call of doEditFileDocument in ListClientHeader with docType = %d",
                                    docType));
                }
                //fragmentTransaction.replace(R.id.content, fragment);
                //fragmentTransaction.addToBackStack(null);
                //fragmentTransaction.commit();

                break;
            default:
                throw new CRISException(
                        String.format("Call of doEditFileDocument in ListClientHeader with mode = %s",
                                getMode().toString()));
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
                    doEditFileDocument();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Storage Permission Denied");
                    String message = "You cannot add Pdf documents to your client's " +
                            "record because you have denied the 'storage' permission " +
                            "for this application. Please repeat the operation and allow " +
                            "the application to access the local storage. (Note: if you " +
                            "checked 'Do not ask again', you will have to set the permission " +
                            "using the 'Settings' app on your tablet/phone.";
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
                }
                break;

            }
            case Main.REQUEST_PERMISSION_SEND_SMS: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                String message = "";
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    builder.setTitle("Send SMS Permission Granted");
                    message = "You can now send the text messages.";
                } else {

                    builder.setTitle("Send SMS Permission Denied");
                    message = "You cannot use the Broadcast Message facility " +
                            "without granting the 'send sms' permission " +
                            "for this application. Please repeat the operation and grant " +
                            "the permission. (Note: if you " +
                            "checked 'Do not ask again', you will have to set the permission " +
                            "using the 'Settings' app on your tablet/phone.";


            }
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
                break;
            }
            default:
                throw new CRISException(String.format(Locale.UK, "Unexpected case value: %d", requestCode));
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

}
