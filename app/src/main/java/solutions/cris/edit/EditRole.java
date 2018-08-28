package solutions.cris.edit;

import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import solutions.cris.CRISActivity;
import solutions.cris.Login;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListComplexListItems;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.Role;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;

public class EditRole extends CRISActivity {

    private Role editRole;
    private LocalDB localDB;
    private User currentUser;
    private boolean isNewMode;

    // UI references.
    private EditText roleNameView;
    private CheckBox isDisplayed;
    private CheckBox isDefault;
    private CheckBox accessAllClients;
    private CheckBox readAllClients;
    private CheckBox writeAllClients;
    private CheckBox accessMyClients;
    private CheckBox readMyClients;
    private CheckBox writeMyClients;
    private CheckBox accessAllDocuments;
    private CheckBox readAllDocuments;
    private CheckBox writeAllDocuments;
    private CheckBox accessNotes;
    private CheckBox readNotes;
    private CheckBox writeNotes;
    //private CheckBox accessSessions;
    //private CheckBox readSessions;
    //private CheckBox writeSessions;
    private CheckBox systemAdministration;
    private CheckBox createNotes;
    private CheckBox createNewSessions;
    private CheckBox editAllSessions;
    //private CheckBox viewUserRecord;
    private CheckBox manageLibraryDocuments;
    private CheckBox createNewClients;
    private CheckBox userIsKeyworker;
    private CheckBox supervisorSetToFollow;
    private CheckBox allowExport;

    private static final String CLIENT_HINT_DISPLAYED = "solutions.cris.ClientHistDisplayed";
    private static final String DOCUMENT_HINT_DISPLAYED = "solutions.cris.ClientHistDisplayed";
    private static final String GENERAL_HINT_DISPLAYED = "solutions.cris.ClientHistDisplayed";
    private TextView clientHintTextView;
    private boolean clientHintTextDisplayed = true;
    private TextView documentHintTextView;
    private boolean documentHintTextDisplayed = true;
    private TextView generalHintTextView;
    private boolean generalHintTextDisplayed = true;

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
            setContentView(R.layout.activity_edit_role);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            // Get the parameter passed with the Intent
            isNewMode = getIntent().getBooleanExtra(Main.EXTRA_IS_NEW_MODE, false);
            if (isNewMode) {
                editRole = (Role) getIntent().getSerializableExtra(Main.EXTRA_ROLE);
                toolbar.setTitle(getString(R.string.app_name) + " - New Role");
            } else {
                int listPos = getIntent().getIntExtra(Main.EXTRA_LIST_POSITION, 0);
                editRole = (Role) ListComplexListItems.items.get(listPos);
                toolbar.setTitle(getString(R.string.app_name) + " - Edit Role");
            }
            setSupportActionBar(toolbar);

            // Set up the form.
            roleNameView = (EditText) findViewById(R.id.role_name);
            isDisplayed = (CheckBox) findViewById(R.id.is_displayed);
            isDefault = (CheckBox) findViewById(R.id.is_default);
            accessAllClients = (CheckBox) findViewById(R.id.pref_allclients_access);
            readAllClients = (CheckBox) findViewById(R.id.pref_allclients_read);
            writeAllClients = (CheckBox) findViewById(R.id.pref_allclients_write);
            accessMyClients = (CheckBox) findViewById(R.id.pref_myclients_access);
            readMyClients = (CheckBox) findViewById(R.id.pref_myclients_read);
            writeMyClients = (CheckBox) findViewById(R.id.pref_myclients_write);
            accessAllDocuments = (CheckBox) findViewById(R.id.pref_alldocuments_access);
            readAllDocuments = (CheckBox) findViewById(R.id.pref_alldocuments_read);
            writeAllDocuments = (CheckBox) findViewById(R.id.pref_alldocuments_write);
            accessNotes = (CheckBox) findViewById(R.id.pref_notes_access);
            readNotes = (CheckBox) findViewById(R.id.pref_notes_read);
            writeNotes = (CheckBox) findViewById(R.id.pref_notes_write);
            //accessSessions = (CheckBox) findViewById(R.id.pref_sessions_access);
            //readSessions = (CheckBox) findViewById(R.id.pref_sessions_read);
            //writeSessions = (CheckBox) findViewById(R.id.pref_sessions_write);
            systemAdministration = (CheckBox) findViewById(R.id.pref_system_administrator);
            createNotes = (CheckBox) findViewById(R.id.pref_create_notes);
            createNewSessions = (CheckBox) findViewById(R.id.pref_create_sessions);
            editAllSessions = (CheckBox) findViewById(R.id.pref_edit_all_sessions);
            //viewUserRecord = (CheckBox) findViewById(R.id.pref_view_user_record);
            manageLibraryDocuments = (CheckBox) findViewById(R.id.pref_manage_library);
            createNewClients = (CheckBox) findViewById(R.id.pref_create_new_clients);
            userIsKeyworker = (CheckBox) findViewById(R.id.pref_user_is_keyworker);
            supervisorSetToFollow = (CheckBox) findViewById(R.id.pref_set_supervisor_set_to_follow);
            allowExport = (CheckBox) findViewById(R.id.pref_allow_export);

            // Set up the hint text
            clientHintTextView = (TextView) findViewById(R.id.client_hint_text);
            documentHintTextView = (TextView) findViewById(R.id.document_hint_text);
            generalHintTextView = (TextView) findViewById(R.id.general_hint_text);
            // Restore value of hintDisplayed (Set to opposite, toggle to come
            if (savedInstanceState != null) {
                clientHintTextDisplayed = !savedInstanceState.getBoolean(CLIENT_HINT_DISPLAYED);
                documentHintTextDisplayed = !savedInstanceState.getBoolean(DOCUMENT_HINT_DISPLAYED);
                generalHintTextDisplayed = !savedInstanceState.getBoolean(GENERAL_HINT_DISPLAYED);
            }
            // Client
            clientHintTextView.setText(getClientHintText());
            clientHintTextDisplayed = toggleHint(clientHintTextView, clientHintTextDisplayed);
            clientHintTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clientHintTextDisplayed = toggleHint(clientHintTextView, clientHintTextDisplayed);
                }
            });
            // Document
            documentHintTextView.setText(getDocumentHintText());
            documentHintTextDisplayed = toggleHint(documentHintTextView, documentHintTextDisplayed);
            documentHintTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    documentHintTextDisplayed = toggleHint(documentHintTextView, documentHintTextDisplayed);
                }
            });
            // General
            generalHintTextView.setText(getGeneralHintText());
            generalHintTextDisplayed = toggleHint(generalHintTextView, generalHintTextDisplayed);
            generalHintTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    generalHintTextDisplayed = toggleHint(generalHintTextView, generalHintTextDisplayed);
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
        isDisplayed.setChecked(editRole.isDisplayed());
        isDefault.setChecked(editRole.isDefault());
        if (editRole.getItemValue() != null) {
            roleNameView.setText(editRole.getItemValue(), null);
            accessAllClients.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_ACCESS_ALL_CLIENTS));
            readAllClients.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_READ_ALL_CLIENTS));
            writeAllClients.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_WRITE_ALL_CLIENTS));
            accessMyClients.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_ACCESS_MY_CLIENTS));
            readMyClients.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_READ_MY_CLIENTS));
            writeMyClients.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_WRITE_MY_CLIENTS));
            accessAllDocuments.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_ACCESS_ALL_DOCUMENTS));
            readAllDocuments.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_READ_ALL_DOCUMENTS));
            writeAllDocuments.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_WRITE_ALL_DOCUMENTS));
            accessNotes.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_ACCESS_NOTES));
            readNotes.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_READ_NOTES));
            writeNotes.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_WRITE_NOTES));
            //accessSessions.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_ACCESS_SESSIONS));
            //readSessions.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_READ_SESSIONS));
            //writeSessions.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_WRITE_SESSIONS));
            systemAdministration.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_SYSTEM_ADMINISTRATOR));
            createNotes.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_CREATE_NOTES));
            createNewSessions.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_CREATE_SESSIONS));
            editAllSessions.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_EDIT_ALL_SESSIONS));
            //viewUserRecord.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_VIEW_USER_RECORD));
            manageLibraryDocuments.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_WRITE_LIBRARY_DOCUMENTS));
            createNewClients.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_CREATE_NEW_CLIENTS));
            userIsKeyworker.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_USER_IS_KEYWORKER));
            supervisorSetToFollow.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_SUPERVISOR_SET_TO_FOLLOW));
            allowExport.setChecked(editRole.hasPrivilege(Role.PRIVILEGE_ALLOW_EXPORT));
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

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the state
        savedInstanceState.putBoolean(CLIENT_HINT_DISPLAYED, clientHintTextDisplayed);
        savedInstanceState.putBoolean(DOCUMENT_HINT_DISPLAYED, documentHintTextDisplayed);
        savedInstanceState.putBoolean(GENERAL_HINT_DISPLAYED, generalHintTextDisplayed);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private boolean toggleHint(TextView view, boolean current) {
        if (current) {
            view.setMaxLines(2);
            return false;
        } else {

            view.setMaxLines(view.getLineCount());
            return true;
        }
    }

    // Validate the document
    private boolean validate() {
        boolean success = true;

        // Clear any existing errors
        roleNameView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        // Role Name
        String sRoleName = roleNameView.getText().toString().trim();
        if (TextUtils.isEmpty(sRoleName)) {
            roleNameView.setError(getString(R.string.error_field_required));
            focusView = roleNameView;
            success = false;
        } else {
            editRole.setItemValue(sRoleName);
        }

        editRole.setIsDisplayed(isDisplayed.isChecked());
        editRole.setIsDefault(isDefault.isChecked());

        // Privileges
        if (editRole.getListItemID().equals(Role.systemAdministratorID) && !systemAdministration.isChecked()) {
            // System Admin role may not be removed from System Administrator
            systemAdministration.setChecked(true);
        }
        editRole.setPrivilege(Role.PRIVILEGE_SYSTEM_ADMINISTRATOR, systemAdministration.isChecked());

        editRole.setPrivilege(Role.PRIVILEGE_ACCESS_ALL_CLIENTS, accessAllClients.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_READ_ALL_CLIENTS, readAllClients.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_WRITE_ALL_CLIENTS, writeAllClients.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_ACCESS_MY_CLIENTS, accessMyClients.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_READ_MY_CLIENTS, readMyClients.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_WRITE_MY_CLIENTS, writeMyClients.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_ACCESS_ALL_DOCUMENTS, accessAllDocuments.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_READ_ALL_DOCUMENTS, readAllDocuments.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_WRITE_ALL_DOCUMENTS, writeAllDocuments.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_ACCESS_NOTES, accessNotes.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_READ_NOTES, readNotes.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_WRITE_NOTES, writeNotes.isChecked());
        //editRole.setPrivilege(Role.PRIVILEGE_ACCESS_SESSIONS, accessSessions.isChecked());
        //editRole.setPrivilege(Role.PRIVILEGE_READ_SESSIONS, readSessions.isChecked());
        //editRole.setPrivilege(Role.PRIVILEGE_WRITE_SESSIONS, writeSessions.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_CREATE_NOTES, createNotes.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_CREATE_SESSIONS, createNewSessions.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_EDIT_ALL_SESSIONS, editAllSessions.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_WRITE_LIBRARY_DOCUMENTS, manageLibraryDocuments.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_CREATE_NEW_CLIENTS, createNewClients.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_USER_IS_KEYWORKER, userIsKeyworker.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_SUPERVISOR_SET_TO_FOLLOW, supervisorSetToFollow.isChecked());
        editRole.setPrivilege(Role.PRIVILEGE_ALLOW_EXPORT, allowExport.isChecked());

        if (!success) {
            focusView.requestFocus();
            focusView.requestFocusFromTouch();
        }
        return success;
    }

    // Save the document
    private boolean save() {
        boolean success = true;
        try {
            if (isNewMode) {
                // Append the new user to the list of users
                ListComplexListItems.items.add(editRole);
            }
            editRole.save(isNewMode);
        } catch (SQLiteConstraintException ex) {
            // ItemValue was not unique
            roleNameView.setError(getString(R.string.error_value_not_unique));
            roleNameView.requestFocus();
            success = false;
        }
        // If this item has been set to the Default, remove any other default
        if (editRole.isDefault()){
            LocalDB localDB = LocalDB.getInstance();
            ArrayList<ListItem> items = localDB.getAllListItems(ListType.ROLE.toString(), true);
            for (ListItem item:items){
                if (item.isDefault() && !item.getItemValue().equals(editRole.getItemValue())) {
                    item.setIsDefault(false);
                    item.save(false);
                }
            }
        }
        return success;
    }

    private String getClientHintText() {
        return "A role is allocated to each user as they are created (it may be changed later). " +
                "The role is used to check whether the user has the privilege for different options " +
                "and to set what level of the client records are visible. The first set of options " +
                "control access at the overall client level.\n\n" +
                "Tick ‘Access’ to allow access to the main menu options ‘My Clients’ and ‘All Clients’. " +
                "This will enable the user to see the names of clients and their main case worker’s name " +
                "and contact details. The set of ‘My Clients’ contains clients where I am the main case " +
                "worker, where I am a volunteer associated with the client’s Group or I am a volunteer " +
                "associated with a forthcoming ad-hoc session that the client has been invited to attend.\n\n" +
                "Tick ‘Read’ to allow the user to select a client from ‘My Clients’ or ‘All Clients’ and " +
                "view the key demographic details and a list of documents. Exactly which documents are " +
                "visible will depend on the ‘document’ settings below.\n\n" +
                "Tick ‘Write’ to enable the user to create or update visible documents under ‘My Clients’ " +
                "or ‘All Clients’. Note that enabling write without enabling read will not have any effect " +
                "since the user will not be able to access a client’s records and therefore will not have " +
                "access to the ‘create document’ menu options.";

    }

    private String getDocumentHintText() {
        return "Select the ‘Access’ options to enable the document summary to be seen in the view" +
                "of a client’s records. The information displayed is different for each document " +
                "type but will provide a summary of the documents contents and therefore may include " +
                // 11 Oct 2017 Build 2.1.086
                //"sensitive information. All documents grants access to all document types, ‘Notes’ " +
                // Already Replaced
                //"restricts the user to note documents and ‘Sessions’ restricts the user to group " +
                //"and ad-hoc sessions, enabling the use of session registers.\n\n" +

                //"restricts the user to note documents.\n\n" +
                "sensitive information. All documents grants access to all document types, " +
                "‘Demographic Documents’ restricts the user to Client, Contact, Case documents " +
                "and sticky Notes. This may be useful in differentiating between full and " +
                "administrative users\n\n" +
                "Select the ‘Read’ option to allow the user to read the contents of visible documents.\n\n" +
                "Select the ‘Write’ option to enable the user to create new documents and edit those " +
                "which may be edited.";

    }

    private String getGeneralHintText() {
        return "The following options control access to specific functionality. The System Administrator " +
                "option enables access to the System Administration menu option, is required to " +
                "create/update user profiles and manage lists. Lists range from the simple pick-lists, " +
                "such as ‘gender’, used to provide drop-down lists of options on forms to more " +
                "sophisticated lists such as Note Types, Schools, Agencies and Roles where each list " +
                "entry has organisation definable fields and options.\n\n" +
                "The other options are hopefully self-explanatory.";

    }

}
