package solutions.cris;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import solutions.cris.crypto.AESEncryption;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.EditUser;
import solutions.cris.exceptions.CRISBadOrgException;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Role;
import solutions.cris.object.SystemError;
import solutions.cris.object.User;
import solutions.cris.sync.SyncManager;
import solutions.cris.sync.WebConnection;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.ExceptionHandler;

//import static solutions.cris.sync.JSONFactory.getPostJSON;
//import static solutions.cris.sync.JSONFactory.postJSON;

public class Login extends CRISActivity {


    private EditText organisationView;
    private EditText emailView;
    private EditText passwordView;
    private CheckBox showPassword;
    private Button signInButton;
    private ProgressBar progressBar;
    private TextView progressBarText;

    private Context context;
    private LocalDB localDB;
    AESEncryption aesEncryption;
    SharedPreferences prefs;

    private String organisation = "";
    private String email = "";
    private String password = "";

    private String organisationError = "";
    private String emailError = "";
    private String passwordError = "";

    private SyncManager syncManager;

    private enum State {New, Locked, OrgSet, EmailSet, Authenticated, FirstTimeUser}

    private boolean lockMode = false;

    private Login.State state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;

        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialise the SyncManager for later use
        syncManager = SyncManager.getSyncManager(this);

        organisationView = (EditText) findViewById(R.id.organisation);
        emailView = (EditText) findViewById(R.id.email);
        passwordView = (EditText) findViewById(R.id.password);
        showPassword = (CheckBox) findViewById(R.id.show_password_text);
        signInButton = (Button) findViewById(R.id.login_sign_in);
        progressBar = (ProgressBar) findViewById(R.id.login_progress);
        progressBarText = (TextView) findViewById(R.id.login_progress_text);

        // Clear any existing Current User
        User.setCurrentUser(null);

        // Get the previous organisation from  shared preferences
        prefs = getSharedPreferences(getString(R.string.shared_preference_file), Context.MODE_PRIVATE);
        if (prefs.contains(getString(R.string.pref_organisation))) {
            organisation = prefs.getString(getString(R.string.pref_organisation), "");
        }
        // Deal with pre-v1.0.027 versions with email preferences
        if (prefs.contains(getString(R.string.pref_emailAddress))) {
            email = prefs.getString(getString(R.string.pref_emailAddress), "");
            if (!email.isEmpty()) {
                // Save in the post v1.0.027 format
                CRISUtil.saveOrg(this, organisation, email);
            }
            // Remove the pref
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(getString(R.string.pref_emailAddress));
            editor.apply();
        }

        // Set up the password onClickListner
        showPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    passwordView.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    showPassword.setChecked(true);
                }
            }
        });

        // Set up an Organisation listener to deal with change of Organisation
        organisationView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    state = State.OrgSet;
                } else {
                    // Check whether new Org has org_
                    organisation = organisationView.getText().toString().trim();
                    if (!organisation.isEmpty()) {
                        email = CRISUtil.getOrgEmail(context, organisation, false);
                        if (!email.isEmpty()) {
                            emailView.setText(email);
                            emailView.setError(null);
                            if (!emailView.hasFocus()) {
                                state = State.EmailSet;
                            }
                        }
                    }
                }
                setEmailViewEditState();
            }
        });

        // If there is an organisation, check for the associated email
        if (!organisation.isEmpty()) {
            email = CRISUtil.getOrgEmail(this, organisation, false);
            if (email.isEmpty()) {
                state = State.OrgSet;
            } else {
                if (getIntent().hasExtra(Main.EXTRA_EMAIL_ADDRESS)) {
                    // Email address is simply a flag to indicate that login activity was
                    // started from SyncAdapter or Lock
                    // This not only instantiates the class but also creates the database if it doesn't exist.
                    this.localDB = LocalDB.getInstance(this, organisation);
                    state = Login.State.Locked;
                } else {
                    state = Login.State.EmailSet;
                }
            }
        } else {
            state = State.New;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        organisationView.setError(null);
        emailView.setError(null);
        passwordView.setError(null);
        organisationView.setText(organisation);
        emailView.setText(email);
        passwordView.setText(password);

        // Set the visibility of fields and buttons
        switch (state) {

            case New:
                organisationView.setVisibility(View.VISIBLE);
                emailView.setVisibility(View.VISIBLE);
                setEmailViewEditState();
                organisationView.requestFocus();
                passwordView.setVisibility(View.VISIBLE);
                showPassword.setVisibility(View.VISIBLE);
                signInButton.setText(R.string.action_sign_in);
                break;

            case Locked:
                lockMode = true;
                organisationView.setVisibility(View.GONE);
                setEmailViewEditState();
                passwordView.setVisibility(View.VISIBLE);
                passwordView.requestFocus();
                showPassword.setVisibility(View.VISIBLE);
                signInButton.setText(R.string.action_unlock);
                break;

            case OrgSet:
                organisationView.setVisibility(View.VISIBLE);
                emailView.setVisibility(View.VISIBLE);
                setEmailViewEditState();
                emailView.requestFocus();
                passwordView.setVisibility(View.VISIBLE);
                showPassword.setVisibility(View.VISIBLE);
                signInButton.setText(R.string.action_sign_in);
                break;

            case EmailSet:
                organisationView.setVisibility(View.VISIBLE);
                emailView.setVisibility(View.VISIBLE);
                setEmailViewEditState();
                passwordView.setVisibility(View.VISIBLE);
                passwordView.requestFocus();
                showPassword.setVisibility(View.VISIBLE);
                // Remove in production version
                signInButton.setText(R.string.action_sign_in);
                break;

            case FirstTimeUser:
                showFirstTimeUseAlert();
                break;

            case Authenticated:
                // Turn on periodic syncing
                syncManager.addPeriodicSync();

                if (lockMode){
                    finish();
                } else {
                    // Check whether policy manager is enabled
                    Intent intent = new Intent(DevicePolicyManager
                            .ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                            compName);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Activating the device administrator enables allows CRIS to use the " +
                                    "Android screen lock when the user selects Lock " +
                                    "menu option or whenever a MyWeek is created/edited. It is " +
                                    "important that a pin, lock password or swipe gesture is " +
                                    "set up to prevent unauthorised users simply swiping to " +
                                    "restart the app and access confidential information.");
                    startActivityForResult(intent, RESULT_ENABLE);
                    return;
                }
        }

        if (!organisationError.isEmpty()) {
            organisationView.setError(organisationError);
            organisationView.requestFocus();
        } else if (!emailError.isEmpty()) {
            emailView.setError(emailError);
            emailView.requestFocus();
        } else if (!passwordError.isEmpty()) {
            passwordView.setError(passwordError);
            passwordView.requestFocus();
        }

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // The authentication happens in a background task because it
                // may well involve calls to the Internet
                signInButton.setEnabled(false);

                organisation = organisationView.getText().toString().trim();
                email = emailView.getText().toString().trim().toLowerCase();
                password = passwordView.getText().toString().trim();
                if (validateFields()) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBarText.setVisibility(View.VISIBLE);
                    progressBarText.setText(R.string.action_authenticating);
                    new Login.AuthenticationActivity().execute();
                } else {
                    signInButton.setEnabled(true);
                    onResume();

                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_ENABLE:
                if (resultCode == AppCompatActivity.RESULT_OK ) {

                }
                if (deviceManager.isAdminActive(compName)) {
                    deviceManager.setPasswordQuality(compName, DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                    if (!deviceManager.isActivePasswordSufficient()) {
                        Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                        startActivity(intent);
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        // Back not allowed
    }

    private boolean validateFields() {
        boolean success = true;
        organisationError = "";
        if (organisation.length() == 0) {
            organisationError = getString(R.string.error_field_required);
            success = false;
        } else {
            if (state == State.New){
                state = State.OrgSet;
            }
        }
        emailError = "";
        if (TextUtils.isEmpty(email)) {
            emailError = getString(R.string.error_field_required);
            success = false;
        }
        passwordError = "";
        if (TextUtils.isEmpty(password)) {
            passwordError = getString(R.string.error_field_required);
            success = false;
        }
        if (success) {
            // Instantiate the AESEncryption Services for later usr
            aesEncryption = AESEncryption.setInstance(organisation, email);
        }
        return success;
    }

    private void showFirstTimeUseAlert() {
        // Build an Alert
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("First Time Use");
        String message = "Welcome to CRIS. You must first create an initial user " +
                "who will become the system administrator, adding the " +
                "subsequent users and managing the system.";
        builder.setMessage(message);
        // Add the Continue button
        builder.setPositiveButton(R.string.action_continue, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked Continue button
                doFirstTimeUse();
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void doFirstTimeUse() {
        // And create him/her
        User newUser = new User(User.firstTimeUser);
        Intent intent = new Intent(this, EditUser.class);
        // User is serializable so can pass as extra to EditUser Activity
        intent.putExtra(Main.EXTRA_IS_NEW_MODE, true);
        intent.putExtra(Main.EXTRA_IS_FIRST_USE, true);
        intent.putExtra(Main.EXTRA_DOCUMENT, newUser);
        intent.putExtra(Main.EXTRA_ORGANISATION, organisation);
        startActivity(intent);
        finish();
    }

    private void setEmailViewEditState() {
        switch (state) {

            case New:
            case OrgSet:
                emailView.setVisibility(View.VISIBLE);
                emailView.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                emailView.setTextColor(ContextCompat.getColor(this, R.color.black));
                emailView.setFocusable(true);
                emailView.setFocusableInTouchMode(true);
                break;
            case EmailSet:
            case Locked:
                emailView.setVisibility(View.VISIBLE);
                emailView.setInputType(InputType.TYPE_NULL);
                emailView.setTextColor(ContextCompat.getColor(this, R.color.holo_grey));
                emailView.setFocusable(false);
                emailView.setFocusableInTouchMode(false);
                break;
        }
    }

    private class AuthenticationActivity extends AsyncTask<String, Void, String> {

        // Constructor
        AuthenticationActivity() {
        }

        @Override
        protected String doInBackground(String... arg0) {
            if (authenticateOrganisation()) {
                if (attemptLogin()) {
                    state = State.Authenticated;
                }
            }
            return "";
        }

        private boolean isConnected() {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }

        private boolean authenticateOrganisation() {
            boolean authenticated = false;

            // Organisation specified so open local database, if not already open
            // This not only instantiates the class but also creates the database if it doesn't exist.
            try {
                localDB = LocalDB.getInstance(context, organisation);
                if (localDB.numUsers() == 0) {
                    // Local DB not found so need to check for Web database
                    if (isConnected()) {
                        authenticated = authenticateWebOrganisation();
                    } else {
                        organisationError = getString(R.string.error_not_connected);
                    }
                } else {
                    // Local database is populated
                    authenticated = true;
                }
                if (authenticated) {
                    // If the org has been authenticated then check whether the preferences need updating
                    String previousOrganisation = "";
                    if (prefs.contains(getString(R.string.pref_organisation))) {
                        previousOrganisation = prefs.getString(getString(R.string.pref_organisation), "");
                    }
                    if (!organisation.equals(previousOrganisation)) {
                        // Save the new organisation as a preference
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(getString(R.string.pref_organisation), organisation);
                        editor.apply();
                    }
                }
            }
            catch (CRISBadOrgException ex){
                organisationError = "Invalid Organisation" +
                        "";
            }
            return authenticated;
        }

        private boolean authenticateWebOrganisation() {
            boolean authenticated = false;

            String errorMessage = "";
            //JSONObject postJSON = getPostJSON(localDB);
            WebConnection webConnection = new WebConnection(localDB);
            //JSONObject jsonOutput = postJSON("check_database.php", postJSON);
            JSONObject jsonOutput = webConnection.post("check_database.php");
            try {
                String result = jsonOutput.getString("result");
                if (result.equals("FAILURE")) {
                    errorMessage = jsonOutput.getString("error_message");
                } else {
                    String webOrg = jsonOutput.getString("organisation");
                    if (!webOrg.equals(organisation)) {
                        errorMessage = "Organisation not found. (Org)";
                    }
                }
                if (errorMessage.length() == 0) {
                    authenticated = true;
                } else {
                    if (errorMessage.startsWith("Organisation not found")) {
                        organisationError = errorMessage;
                    } else { // No tables found
                        state = State.FirstTimeUser;
                    }
                }
            } catch (JSONException ex) {
                throw new CRISException("Error authenticating organisation (web): " + ex.getMessage());
            }
            return authenticated;
        }

        private boolean attemptLogin() {
            boolean authenticated = false;

            if (localDB.numUsers() == 0) {
                // Local DB not found so need to check the Web database
                if (isConnected()) {
                    // Attempt to login against web database
                    if (webAuthenticate()) {
                        // WebAuthenticate() has set the current user using the Web user record
                        User currentUser = User.getCurrentUser();
                        // Need to save the user to enable Sync Adapter to be able to open the user
                        // from the database. In the ensuing sync, the replace will simply overwrite the record.
                        localDB.save(currentUser, true, currentUser);
                        // Since the database state is unknown until the sync succeeds (user or
                        // role records may not exist), set the current user to NoPriv until
                        // user can be retrieved from the local database(No privileges needed for Sync)
                        currentUser.setRoleID(Role.noPrivilegeID);
                        Role role = new Role(currentUser, Role.noPrivilegeID);
                        currentUser.setRole(role);
                        User.setCurrentUser(currentUser);
                        // Save the organisation/email for future use.
                        CRISUtil.saveOrg(context, organisation, email);
                        // Manual sync will be triggered in Main activity (User = NoPriv)
                        authenticated = true;
                    }
                } else {
                    organisationError = getString(R.string.error_not_connected);
                }

            } else {
                // Attempt to login against local database
                // Check that User record can be retrieved
                User newUser;
                try {
                    newUser = localDB.getUser(email);
                    if (newUser.authenticatePassword(password)) {
                        User.setCurrentUser(newUser);
                        // Save the organisation/email for future use.
                        CRISUtil.saveOrg(context, organisation, email);
                        authenticated = true;
                    } else {
                        // If password has been reset by System Administrator then sync
                        // may be needed to get it into the local database
                        syncManager.requestManualSync();
                        passwordError = getString(R.string.error_login_failed);
                    }
                } catch (Exception ex) {
                    // Local database exists but user could not be retrieved so this
                    // is a change of user.
                    //Check that the new email/password is valid first
                    if (webAuthenticate()) {
                        // User has been authenticated against the web database
                        // The local database will need to be re-encrypted.
                        // Start with the oldEmail
                        String oldEmail = CRISUtil.getOrgEmail(context, organisation, true);
                        if (oldEmail.isEmpty()) {
                            throw new CRISException("Failed to reEncrypt, oldEmail not set");
                        }
                        // Get the encryption keys for the old Email
                        AESEncryption.setInstance(organisation, oldEmail);
                        //Re-loading the AESEncryption instance will
                        // save the old key and allow re-encrypt to work
                        AESEncryption.setInstance(organisation, email);
                        // ReEncrypt with the ney key
                        localDB.reEncrypt();
                        User currentUser = User.getCurrentUser();
                        // WebAuthenticate() has set the current user using the Web user record
                        // The current user may not exist in the local database so attempt to save
                        // so that sync will work
                        // In the ensuing sync, the replace will simply overwrite the record.
                        try {
                            localDB.save(currentUser, true, currentUser);
                        }
                        catch (Exception constraint){
                            // If the user already exists, this operation will fail
                        }
                        // Since the database state is unknown until the sync succeeds (user or
                        // role records may not exist), set the current user to NoPriv until
                        // user can be retrieved from the local database(No privileges needed for Sync)
                        currentUser.setRoleID(Role.noPrivilegeID);
                        Role role = new Role(currentUser, Role.noPrivilegeID);
                        currentUser.setRole(role);
                        User.setCurrentUser(currentUser);
                        // Save the organisation/email for future use.
                        CRISUtil.saveOrg(context, organisation, email);
                        // Manual sync will be triggered in Main activity (User = NoPriv)
                        authenticated = true;
                    }
                }
            }
            return authenticated;
        }

        private boolean webAuthenticate() {
            boolean authenticated = false;
            //JSONObject postJSON = getPostJSON(localDB);
            WebConnection webConnection = new WebConnection(localDB);
            try {
                JSONObject user = new JSONObject();
                user.put("EmailAddress", email);
                //postJSON.put("user", user);
                webConnection.getInputJSON().put("user", user);
            } catch (JSONException ex) {
                throw new CRISException("Error creating JSON object: " + ex.getMessage());
            }
            JSONObject jsonOutput;
            String result;
            try {
                //jsonOutput = postJSON("get_password.php", postJSON);
                jsonOutput = webConnection.post("get_password.php");
                result = jsonOutput.getString("result");
                try {
                    switch (result) {
                        case "SUCCESS":
                            User newUser;
                            byte[] buf = Base64.decode(jsonOutput.getString("user"), Base64.DEFAULT);
                            // If user not found, result will be empty string so fall through auth=False
                            if (buf.length > 0) {
                                byte[] decrypt = aesEncryption.decrypt(AESEncryption.WEB_CIPHER, buf);
                                ByteArrayInputStream b = new ByteArrayInputStream(decrypt);
                                try {
                                    ObjectInputStream o = new ObjectInputStream(b);
                                    newUser = (User) o.readObject();
                                    o.close();
                                } catch (ClassNotFoundException ex) {
                                    throw new CRISException("Error deserializing object User class not found");
                                } catch (Exception ex) {
                                    throw new CRISException("Error deserializing User object: " + ex.getMessage());
                                }
                                if (newUser.authenticatePassword(password)) {
                                    User.setCurrentUser(newUser);
                                    authenticated = true;
                                } else {
                                    //passwordError = getString(R.string.error_login_failed);
                                    passwordError = "Invalid password";
                                }
                            } else {
                                //passwordError = getString(R.string.error_login_failed);
                                emailError = "No such user";
                            }
                            break;
                        case "FAILURE":
                            String errorMessage = jsonOutput.getString("error_message");
                            throw new CRISException("Web authentication error: " + errorMessage);
                        default:
                            throw new CRISException("Invalid result from get_password.php: " + result);
                    }
                } catch (JSONException ex) {
                    throw new CRISException("Error parsing JSON data: " + ex.getMessage());
                }
            }
            catch (Exception ex){
                SystemError systemError = new SystemError(new User(User.unknownUser), ex);
                localDB.save(systemError);
                emailError = "Internet connection failed, please retry";
            }
            return authenticated;
        }

        @Override
        protected void onPostExecute(String ignore) {
            progressBar.setVisibility(View.GONE);
            progressBarText.setVisibility(View.GONE);
            signInButton.setEnabled(true);
            onResume();
        }

    }
}
