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
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListComplexListItems;
import solutions.cris.object.Agency;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;

public class EditAgency extends CRISActivity {

    private Agency editAgency;
    private LocalDB localDB;
    private User currentUser;
    private boolean newMode;


    // UI references.
    private EditText agencyNameView;
    private CheckBox isDisplayed;
    private CheckBox isDefault;
    private EditText addressView;
    private EditText postcodeView;
    private EditText contactNumberView;
    private EditText emailView;
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
            setContentView(R.layout.activity_edit_agency);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            // Get the parameter passed with the Intent
            newMode = getIntent().getBooleanExtra(Main.EXTRA_IS_NEW_MODE, false);
            if (newMode) {
                editAgency = (Agency) getIntent().getSerializableExtra(Main.EXTRA_AGENCY);
                toolbar.setTitle(getString(R.string.app_name) + " - New Agency");
            } else {
                int listPos = getIntent().getIntExtra(Main.EXTRA_LIST_POSITION, 0);
                editAgency = (Agency) ListComplexListItems.items.get(listPos);
                toolbar.setTitle(getString(R.string.app_name) + " - Edit Agency");
            }
            setSupportActionBar(toolbar);

            // Set up the form.
            agencyNameView = (EditText) findViewById(R.id.name);
            isDisplayed = (CheckBox) findViewById(R.id.is_displayed);
            isDefault = (CheckBox) findViewById(R.id.is_default);
            addressView = (EditText) findViewById(R.id.address);
            postcodeView = (EditText) findViewById(R.id.postcode);
            contactNumberView = (EditText) findViewById(R.id.contact_number);
            emailView = (EditText) findViewById(R.id.email);
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
        isDisplayed.setChecked(editAgency.isDisplayed());
        isDefault.setChecked(editAgency.isDefault());
        if (editAgency.getItemValue() != null) {
            agencyNameView.setText(editAgency.getItemValue(), null);
            addressView.setText(editAgency.getAgencyAddress(), null);
            postcodeView.setText(editAgency.getAgencyPostcode(), null);
            contactNumberView.setText(editAgency.getAgencyContactNumber(), null);
            emailView.setText(editAgency.getAgencyEmailAddress(), null);
            additionalInformationView.setText(editAgency.getAgencyAdditionalInformation());
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
        agencyNameView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        // Agency Name
        String sAgencyName = agencyNameView.getText().toString().trim();
        if (TextUtils.isEmpty(sAgencyName)) {
            agencyNameView.setError(getString(R.string.error_field_required));
            focusView = agencyNameView;
            success = false;
        } else {
            editAgency.setItemValue(sAgencyName);
        }

        editAgency.setIsDisplayed(isDisplayed.isChecked());
        editAgency.setIsDefault(isDefault.isChecked());

        // Address
        String sAddress = addressView.getText().toString().trim();
        if (TextUtils.isEmpty(sAddress)) {
            addressView.setError(getString(R.string.error_field_required));
            focusView = addressView;
            success = false;
        } else {
            editAgency.setAgencyAddress(sAddress);
        }

        // Postcode
        String sPostcode = postcodeView.getText().toString().trim();
        if (TextUtils.isEmpty(sPostcode)) {
            postcodeView.setError(getString(R.string.error_field_required));
            focusView = postcodeView;
            success = false;
        } else {
            editAgency.setAgencyPostcode(sPostcode);
        }

        // ContactNumber
        String sContactNumber = contactNumberView.getText().toString().trim();
        if (TextUtils.isEmpty(sContactNumber)) {
            contactNumberView.setError(getString(R.string.error_field_required));
            focusView = contactNumberView;
            success = false;
        } else {
            editAgency.setAgencyContactNumber(contactNumberView.getText().toString().trim());
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
            editAgency.setAgencyEmailAddress(sEmail);
        }

        // AdditionalInformation (non-mandatory)
        editAgency.setAgencyAdditionalInformation(additionalInformationView.getText().toString().trim());

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
                ListComplexListItems.items.add(editAgency);
            }
            editAgency.save(newMode);
        } catch (SQLiteConstraintException ex) {
            // ItemValue was not unique
            agencyNameView.setError(getString(R.string.error_value_not_unique));
            agencyNameView.requestFocus();
            success = false;
        }
        // If this item has been set to the Default, remove any other default
        if (editAgency.isDefault()){
            LocalDB localDB = LocalDB.getInstance();
            ArrayList<ListItem> items = localDB.getAllListItems(ListType.AGENCY.toString(), true);
            for (ListItem item:items){
                if (item.isDefault() && !item.getItemValue().equals(editAgency.getItemValue())) {
                    item.setIsDefault(false);
                    item.save(false);
                }
            }
        }
        return success;
    }
}
