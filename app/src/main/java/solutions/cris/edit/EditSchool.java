package solutions.cris.edit;

import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.regex.Pattern;

import solutions.cris.CRISActivity;
import solutions.cris.Login;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListComplexListItems;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.School;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;

public class EditSchool extends CRISActivity {

    private School editSchool;
    private LocalDB localDB;
    private User currentUser;
    private boolean newMode;


    // UI references.
    private EditText schoolNameView;
    private CheckBox isDisplayed;
    private CheckBox isDefault;
    private EditText addressView;
    private EditText postcodeView;
    private EditText contactNumberView;
    private EditText emailView;
    private EditText headTeacherView;
    private EditText additionalInformationView;

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
            setContentView(R.layout.activity_edit_school);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            // Get the parameter passed with the Intent
            newMode = getIntent().getBooleanExtra(Main.EXTRA_IS_NEW_MODE, false);
            if (newMode) {
                editSchool = (School) getIntent().getSerializableExtra(Main.EXTRA_SCHOOL);
                toolbar.setTitle(getString(R.string.app_name) + " - New School");
            } else {
                int listPos = getIntent().getIntExtra(Main.EXTRA_LIST_POSITION, 0);
                editSchool = (School) ListComplexListItems.items.get(listPos);
                toolbar.setTitle(getString(R.string.app_name) + " - Edit School");
            }
            setSupportActionBar(toolbar);

            // Set up the form.
            schoolNameView = (EditText) findViewById(R.id.name);
            isDisplayed = (CheckBox) findViewById(R.id.is_displayed);
            isDefault = (CheckBox) findViewById(R.id.is_default);
            addressView = (EditText) findViewById(R.id.address);
            postcodeView = (EditText) findViewById(R.id.postcode);
            contactNumberView = (EditText) findViewById(R.id.contact_number);
            emailView = (EditText) findViewById(R.id.email);
            headTeacherView = (EditText) findViewById(R.id.head_teacher);
            additionalInformationView = (EditText) findViewById(R.id.additional_information);

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
        isDisplayed.setChecked(editSchool.isDisplayed());
        isDefault.setChecked(editSchool.isDefault());
        if (editSchool.getItemValue() != null) {
            schoolNameView.setText(editSchool.getItemValue(), null);
            addressView.setText(editSchool.getSchoolAddress(), null);
            postcodeView.setText(editSchool.getSchoolPostcode(), null);
            contactNumberView.setText(editSchool.getSchoolContactNumber(), null);
            emailView.setText(editSchool.getSchoolEmailAddress(), null);
            headTeacherView.setText(editSchool.getSchoolHeadTeacher());
            additionalInformationView.setText(editSchool.getSchoolAdditionalInformation());
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

    // Validate the document
    private boolean validate() {
        boolean success = true;

        // Clear any existing errors
        schoolNameView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        // School Name
        String sSchoolName = schoolNameView.getText().toString().trim();
        if (TextUtils.isEmpty(sSchoolName)) {
            schoolNameView.setError(getString(R.string.error_field_required));
            focusView = schoolNameView;
            success = false;
        } else {
            editSchool.setItemValue(sSchoolName);
        }

        editSchool.setIsDisplayed(isDisplayed.isChecked());
        editSchool.setIsDefault(isDefault.isChecked());

        // Address
        String sAddress = addressView.getText().toString().trim();
        if (TextUtils.isEmpty(sAddress)) {
            addressView.setError(getString(R.string.error_field_required));
            focusView = addressView;
            success = false;
        } else {
            editSchool.setSchoolAddress(sAddress);
        }

        // Postcode
        String sPostcode = postcodeView.getText().toString().trim();
        if (TextUtils.isEmpty(sPostcode)) {
            postcodeView.setError(getString(R.string.error_field_required));
            focusView = postcodeView;
            success = false;
        } else {
            editSchool.setSchoolPostcode(sPostcode);
        }

        // ContactNumber
        String sContactNumber = contactNumberView.getText().toString().trim();
        if (TextUtils.isEmpty(sContactNumber)) {
            contactNumberView.setError(getString(R.string.error_field_required));
            focusView = contactNumberView;
            success = false;
        } else {
            editSchool.setSchoolContactNumber(contactNumberView.getText().toString().trim());
        }

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
            editSchool.setSchoolEmailAddress(sEmail);
        }

        // HeadTeacher
        String sHeadTeacher = headTeacherView.getText().toString().trim();
        if (TextUtils.isEmpty(sHeadTeacher)) {
            headTeacherView.setError(getString(R.string.error_field_required));
            focusView = headTeacherView;
            success = false;
        } else {
            editSchool.setSchoolHeadTeacher(headTeacherView.getText().toString().trim());
        }

        // AdditionalInformation (non-mandatory)
        editSchool.setSchoolAdditionalInformation(additionalInformationView.getText().toString().trim());

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
            if (newMode) {
                // Append the new user to the list of users
                ListComplexListItems.items.add(editSchool);
            }
            editSchool.save(newMode);
        } catch (SQLiteConstraintException ex) {
            // ItemValue was not unique
            schoolNameView.setError(getString(R.string.error_value_not_unique));
            schoolNameView.requestFocus();
            success = false;
        }
        // If this item has been set to the Default, remove any other default
        if (editSchool.isDefault()){
            LocalDB localDB = LocalDB.getInstance();
            ArrayList<ListItem> items = localDB.getAllListItems(ListType.SCHOOL.toString(), true);
            for (ListItem item:items){
                if (item.isDefault() && !item.getItemValue().equals(editSchool.getItemValue())) {
                    item.setIsDefault(false);
                    item.save(false);
                }
            }
        }
        return success;
    }

}