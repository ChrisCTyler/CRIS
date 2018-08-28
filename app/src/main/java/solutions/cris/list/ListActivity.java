package solutions.cris.list;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Locale;

import solutions.cris.CRISActivity;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.edit.EditImage;
import solutions.cris.edit.EditPdfDocument;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Client;
import solutions.cris.object.Document;
import solutions.cris.object.Session;
import solutions.cris.object.User;

import static solutions.cris.list.ListLibrary.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE;

/**
 * Created by Chris Tyler on 08/09/2017.
 */

public abstract class ListActivity extends CRISActivity {

    protected ListActivity() {
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
    }

    private boolean doEditFileDocument() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction;
        Fragment fragment;
        switch (getMode()) {
            case NEW:
            case EDIT:
                fragmentTransaction = fragmentManager.beginTransaction();
                if (docType == Document.PdfDocument) {
                    fragment = new EditPdfDocument();
                } else if (docType == Document.Image) {
                    fragment = new EditImage();
                } else {
                    throw new CRISException(
                            String.format(Locale.UK,
                                    "Call of doEditFileDocument in ListClientHeader with docType = %d",
                                    docType));
                }
                fragmentTransaction.replace(R.id.content, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
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
            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE: {
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
