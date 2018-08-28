package solutions.cris.edit;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import solutions.cris.CRISActivity;
import solutions.cris.Login;
import solutions.cris.crypto.AESEncryption;
import solutions.cris.list.ListUsers;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.Role;
import solutions.cris.object.User;
import solutions.cris.sync.SyncManager;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.ExceptionHandler;
import solutions.cris.utils.PickList;

public class EditUser extends CRISActivity {

    private User editUser;
    private LocalDB localDB;
    private User currentUser;
    private PickList rolePickList;

    // UI references.
    private EditText emailView;
    private EditText firstNameView;
    private EditText lastNameView;
    private EditText startDateView;
    private EditText endDateView;
    private EditText contactNumberView;
    private Spinner roleView;
    private EditText passwordView;
    private EditText passwordCheckView;

    private SimpleDateFormat sDate;
    private boolean isNewMode;
    private boolean firstUse;
    private TextView hintTextView;
    private boolean hintTextDisplayed = true;
    private String organisation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add the global uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        firstUse = getIntent().getBooleanExtra(Main.EXTRA_IS_FIRST_USE, false);
        organisation = getIntent().getStringExtra(Main.EXTRA_ORGANISATION);
        // CurrentUser always exists so if this check fails then exception in child
        // // has rendered system inconsistent so exit and let Main start from scratch()
        currentUser = User.getCurrentUser();
        if (!firstUse && currentUser == null) {
            finish();
        } else {
            localDB = LocalDB.getInstance();
            // Re-load the local current user from the database in case activity is called twice
            // with the same current user and current user is being edited
            // (would cause a constraint exception)
            currentUser = localDB.getUser(currentUser.getUserID());
            setContentView(R.layout.activity_user);

            // Preset sDate for use throughout the activity
            sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);
            sDate.setLenient(false);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            // Get the parameter passed with the Intent
            isNewMode = getIntent().getBooleanExtra(Main.EXTRA_IS_NEW_MODE, false);
            if (isNewMode) {
                editUser = (User) getIntent().getSerializableExtra(Main.EXTRA_DOCUMENT);
                toolbar.setTitle(getString(R.string.app_name) + " - New User");
            } else {
                int listPos = getIntent().getIntExtra(Main.EXTRA_LIST_POSITION, 0);
                editUser = ListUsers.adapterList.get(listPos);
                toolbar.setTitle(getString(R.string.app_name) + " - Edit User");
            }
            setSupportActionBar(toolbar);

            // Set up the form.
            emailView = (EditText) findViewById(R.id.email);
            firstNameView = (EditText) findViewById(R.id.first_name);
            lastNameView = (EditText) findViewById(R.id.last_name);
            startDateView = (EditText) findViewById(R.id.start_date);
            endDateView = (EditText) findViewById(R.id.end_date);
            contactNumberView = (EditText) findViewById(R.id.contact_number);
            roleView = (Spinner) findViewById(R.id.role_spinner);
            passwordView = (EditText) findViewById(R.id.password);
            passwordCheckView = (EditText) findViewById(R.id.password_check);

            if (!firstUse) {
                // Initialise the Role Spinner
                rolePickList = new PickList(localDB, ListType.ROLE);
                ArrayAdapter<String> pdfAdapter = new
                        ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rolePickList.getOptions());
                pdfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                roleView.setAdapter(pdfAdapter);
                roleView.setSelection(rolePickList.getDefaultPosition());
            }
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

            startDateView.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View view) {
                    startDatePicker();
                    return true;
                }
            });
            endDateView.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View view) {
                    endDatePicker();
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
        if (editUser.getEmailAddress() != null) {
            emailView.setText(editUser.getEmailAddress(), null);
            firstNameView.setText(editUser.getFirstName(), null);
            lastNameView.setText(editUser.getLastName(), null);
            Date startDate = editUser.getStartDate();
            startDateView.setText(sDate.format(startDate.getTime()));
            if (editUser.getEndDate().getTime() != Long.MIN_VALUE) {
                Date endDate = editUser.getEndDate();
                endDateView.setText(sDate.format(endDate.getTime()));
            }
            contactNumberView.setText(editUser.getContactNumber(), null);
            int position = rolePickList.getOptions().indexOf(editUser.getRole().getItemValue());
            roleView.setSelection(position);
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

    private void startDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog startDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

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
        DatePickerDialog endDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                endDateView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        endDatePickerDialog.show();
    }

    // Validate the document
    private boolean validate() {
        boolean success = true;

        // Clear any existing errors
        emailView.setError(null);
        firstNameView.setError(null);
        lastNameView.setError(null);
        startDateView.setError(null);
        endDateView.setError(null);
        contactNumberView.setError(null);
        passwordView.setError(null);
        passwordCheckView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        // Email
        String sEmail = emailView.getText().toString().trim().toLowerCase();
        if (TextUtils.isEmpty(sEmail)) {
            emailView.setError(getString(R.string.error_field_required));
            focusView = emailView;
            success = false;
        } else if (!isEmailValid(sEmail)) {
            emailView.setError(getString(R.string.error_invalid_email));
            focusView = emailView;
            success = false;
        } else {
            editUser.setEmailAddress(sEmail);
        }
        // FirstName
        String sFirstName = firstNameView.getText().toString().trim();
        if (TextUtils.isEmpty(sFirstName)) {
            firstNameView.setError(getString(R.string.error_field_required));
            focusView = firstNameView;
            success = false;
        } else {
            editUser.setFirstName(sFirstName);
        }
        // LastName
        String sLastName = lastNameView.getText().toString().trim();
        if (TextUtils.isEmpty(sLastName)) {
            lastNameView.setError(getString(R.string.error_field_required));
            focusView = lastNameView;
            success = false;
        } else {
            editUser.setLastName(sLastName);
        }
        // StartDate
        String sStartDate = startDateView.getText().toString();
        if (TextUtils.isEmpty(sStartDate)) {
            startDateView.setError(getString(R.string.error_field_required));
            focusView = startDateView;
            success = false;
        } else {
            Date dStartDate = CRISUtil.parseDate(sStartDate);
            if (dStartDate == null) {
                startDateView.setError(getString(R.string.error_invalid_date));
                focusView = startDateView;
                success = false;
            } else {
                editUser.setStartDate(dStartDate);
            }
        }
        // EndDate
        String sEndDate = endDateView.getText().toString();
        if (TextUtils.isEmpty(sEndDate)) {
            editUser.setEndDate(new Date(Long.MIN_VALUE));
        } else {
            Date dEndDate = CRISUtil.parseDate(sEndDate);
            if (dEndDate == null) {
                endDateView.setError(getString(R.string.error_invalid_date));
                focusView = endDateView;
                success = false;
            } else {
                editUser.setEndDate(dEndDate);
            }
        }

        // StartDate/EndDate consistency check
        if (editUser.getEndDate().getTime() != Long.MIN_VALUE) {
            if (editUser.getStartDate() != null && editUser.getEndDate() != null) {
                if (!editUser.getStartDate().before(editUser.getEndDate())) {
                    endDateView.setError(getString(R.string.error_invalid_date_order));
                    focusView = endDateView;
                    success = false;
                }
            }
        }

        // ContactNumber
        String sContactNumber = contactNumberView.getText().toString().trim();
        if (TextUtils.isEmpty(sContactNumber)) {
            contactNumberView.setError(getString(R.string.error_field_required));
            focusView = contactNumberView;
            success = false;
        } else {
            editUser.setContactNumber(contactNumberView.getText().toString().trim());
        }

        //Role
        if (firstUse) {
            editUser.setRoleID(Role.systemAdministratorID);
        } else {
            ListItem newRole = rolePickList.getListItems().get(roleView.getSelectedItemPosition());
            // Test for Please select
            if (newRole.getItemOrder() == -1) {
                TextView errorText = (TextView) roleView.getSelectedView();
                errorText.setError("anything here, just to add the icon");
                errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                focusView = roleView;
                success = false;
            } else if (editUser.getRoleID() == null || !editUser.getRoleID().equals(newRole.getListItemID())) {
                // Role has been changed
                if (currentUser.getUserID().equals(editUser.getUserID())) {
                    // User cannot change their own role
                    TextView errorText = (TextView) roleView.getSelectedView();
                    errorText.setError("anything here, just to add the icon");
                    errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                    errorText.setText(R.string.info_not_allowed);
                    focusView = roleView;
                    success = false;
                } else if ((editUser.getRoleID() != null && editUser.getRoleID().equals(Role.systemAdministratorID)) ||
                        newRole.getListItemID().equals(Role.systemAdministratorID)) {
                    if (!currentUser.getRoleID().equals(Role.systemAdministratorID)) {
                        // Only System Administrator can grant/revoke SystemAdministrator role
                        TextView errorText = (TextView) roleView.getSelectedView();
                        errorText.setError("anything here, just to add the icon");
                        errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                        errorText.setText(R.string.info_no_privilege);
                        focusView = roleView;
                        success = false;
                    }
                }
            }
            if (success) {
                editUser.setRoleID(newRole.getListItemID());
                // PickList contained
                editUser.setRole((Role) localDB.getListItem(editUser.getRoleID()));
            }
        }


        // Password
        // Note: Password must be the last check to ensure that the 'cannot use same
        // password' check is not triggered by another field validation problem
        String password = passwordView.getText().toString().trim();
        if (isNewMode || password.length() > 0) {
            String allAlphanumericRegex = "^[a-zA-Z0-9]+$";
            if (password.length() < 8) {
                passwordView.setError(getString(R.string.error_password_too_short));
                focusView = passwordView;
                success = false;
            } else if (password.equals(password.toLowerCase())) {
                passwordView.setError(getString(R.string.error_password_no_caps));
                focusView = passwordView;
                success = false;
            } else if (password.matches(allAlphanumericRegex)) {
                passwordView.setError(getString(R.string.error_password_all_letters));
                focusView = passwordView;
                success = false;
            }
            // PasswordCheck
            String passwordCheck = passwordCheckView.getText().toString().trim();
            if (TextUtils.isEmpty((passwordCheck)) | !password.equals(passwordCheck)) {
                passwordCheckView.setError(getString(R.string.error_incorrect_password_check));
                focusView = passwordCheckView;
                success = false;
            } else if (editUser.authenticatePassword(password)) {
                // New password must be the same as the old password
                passwordCheckView.setError(getString(R.string.error_new_password_is_same));
                focusView = passwordCheckView;
                success = false;
            } else {
                if (success) {
                    // Only set if all other validation has passed, otherwise
                    // 'cannot use same password' check will be triggered on second pass
                    editUser.setNewPassword(passwordView.getText().toString().trim());
                    // Password changed so set password expired to force change at next login
                    // unless the current user is modifying it
                    if (!editUser.getUserID().equals(currentUser.getUserID())) {
                        editUser.setPasswordExpiryDate(new Date(Long.MIN_VALUE));
                    }
                }
            }
        }

        if (!success) {
            focusView.requestFocus();
            focusView.requestFocusFromTouch();
        }
        return success;
    }

    private boolean isEmailValid(String email) {
        Pattern pattern = Patterns.EMAIL_ADDRESS;
        return pattern.matcher(email).matches();
    }

    // Save the document
    private boolean save() {
        boolean success = true;

        try {
            if (isNewMode) {
                if (firstUse) {
                    // First time use
                    User.setCurrentUser(editUser);
                    // Save the organisation/email for future use.
                    CRISUtil.saveOrg(this, organisation, editUser.getEmailAddress());
                    // And initialise the Encryption services
                    AESEncryption.setInstance(organisation, editUser.getEmailAddress());
                } else {
                    // Otherwise append the new user to the list of users
                    ListUsers.users.add(editUser);
                }
            }
            editUser.save(isNewMode);
        } catch (SQLiteConstraintException ex) {
            // Either EmailAddress or Name was not unique
            String message = ex.getMessage();
            if (message.contains("EmailAddress")) {
                emailView.setError(getString(R.string.error_value_not_unique));
                emailView.requestFocus();
            } else {
                firstNameView.setError(getString(R.string.error_value_not_unique));
                lastNameView.setError(getString(R.string.error_value_not_unique));
                firstNameView.requestFocus();
            }
            success = false;
        }
        if (firstUse) {
            // Now we have a first user, save the System Administrator Role
            Role systemAdministrator = (Role) localDB.getListItem(Role.systemAdministratorID);
            if (systemAdministrator == null) {
                systemAdministrator = new Role(currentUser, Role.systemAdministratorID);
                systemAdministrator.save(true);
            }
            // Load the role into current user
            currentUser.setRole((Role) localDB.getListItem(currentUser.getRoleID()));
            // Initialise the synchronisation
            SyncManager syncManager = SyncManager.getSyncManager(this);
            // Turn on periodic syncing
            syncManager.addPeriodicSync();
            // Initiate a manual sync since periodic sync may not start immediately
            syncManager.requestManualSync();
        }

        return success;
    }

    private String getHintText() {
        String passwordRules;
        if (isNewMode) {
            passwordRules = "Passwords must be 8 characters or longer and contain at least " +
                    "one non-alphanumeric character (not: A-Z a-z 0-9).\n" +
                    "The user will need to be informed of new password and " +
                    "will be forced to change this password at first login.";
        } else {
            passwordRules = "The password is optional. If left blank the user's existing " +
                    "password will be retained. If set, the value must be 8 " +
                    "characters or longer and contain at least " +
                    "one non-alphanumeric character (not: A-Z a-z 0-9).\n" +
                    "The user will need to be informed of new password and " +
                    "will be forced to change this password at first login.";
        }
        return passwordRules;
    }
}
