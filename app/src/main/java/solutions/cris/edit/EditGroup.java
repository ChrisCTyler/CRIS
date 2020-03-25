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
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import solutions.cris.CRISActivity;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListComplexListItems;
import solutions.cris.object.Group;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.Role;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;
import solutions.cris.utils.PickList;

public class EditGroup extends CRISActivity {

    private Group editGroup;
    private LocalDB localDB;
    private User currentUser;
    private boolean newMode;

    // UI references.
    private EditText groupNameView;
    private CheckBox isDisplayed;
    private CheckBox isDefault;
    private Spinner keyworkerSpinner;
    private Spinner sessionCoordinatorSpinner;
    private EditText addressView;
    private EditText postcodeView;
    private EditText frequencyView;
    private Spinner frequencyTypeSpinner;

    private PickList keyworkerPickList;
    private PickList sessionCoordinatorPickList;

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
            setContentView(R.layout.activity_edit_group);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            // Get the parameter passed with the Intent
            newMode = getIntent().getBooleanExtra(Main.EXTRA_IS_NEW_MODE, false);
            if (newMode) {
                editGroup = (Group) getIntent().getSerializableExtra(Main.EXTRA_GROUP);
                toolbar.setTitle(getString(R.string.app_name) + " - New Group");
            } else {
                int listPos = getIntent().getIntExtra(Main.EXTRA_LIST_POSITION, 0);
                editGroup = (Group) ListComplexListItems.items.get(listPos);
                toolbar.setTitle(getString(R.string.app_name) + " - Edit Group");
            }
            setSupportActionBar(toolbar);

            // Handle pre V1.2 Groups
            if (editGroup.getGroupAddress() == null) editGroup.setGroupAddress("");
            if (editGroup.getGroupPostcode() == null) editGroup.setGroupPostcode("");
            if (editGroup.getFrequencyType() == null) editGroup.setFrequencyType("Please select");

            // Set up the form.
            groupNameView = (EditText) findViewById(R.id.group_name);
            isDisplayed = (CheckBox) findViewById(R.id.is_displayed);
            isDefault = (CheckBox) findViewById(R.id.is_default);
            keyworkerSpinner = (Spinner) findViewById(R.id.keyworker_spinner);
            sessionCoordinatorSpinner = (Spinner) findViewById(R.id.session_coordinator_spinner);
            addressView = (EditText) findViewById(R.id.address);
            postcodeView = (EditText) findViewById(R.id.postcode);
            frequencyView = (EditText) findViewById(R.id.frequency);
            frequencyTypeSpinner = (Spinner) findViewById(R.id.frequency_type_spinner);

            // Initialise the Keyworker/Session Coordoinator Spinners
            ArrayList<User> users = localDB.getAllUsers();
            ArrayList<User> keyworkers = new ArrayList<>();
            for (User user : users) {
                if (user.getRole().hasPrivilege(Role.PRIVILEGE_USER_IS_KEYWORKER)) {
                    keyworkers.add(user);
                }
            }

            keyworkerPickList = new PickList(keyworkers);
            ArrayAdapter<String> keyworkerAdapter = new
                    ArrayAdapter<>(this, android.R.layout.simple_spinner_item, keyworkerPickList.getOptions());
            keyworkerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            keyworkerSpinner.setAdapter(keyworkerAdapter);
            keyworkerSpinner.setSelection(keyworkerPickList.getDefaultPosition());

            sessionCoordinatorPickList = new PickList(users);
            ArrayAdapter<String> sessionCoordinatorAdapter = new
                    ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sessionCoordinatorPickList.getOptions());
            sessionCoordinatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sessionCoordinatorSpinner.setAdapter(sessionCoordinatorAdapter);
            sessionCoordinatorSpinner.setSelection(sessionCoordinatorPickList.getDefaultPosition());

            // Initialise the case Type Spinner
            final ArrayAdapter<String> frequencyTypeAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, Group.frequencyTypeValues);
            frequencyTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            frequencyTypeSpinner.setAdapter(frequencyTypeAdapter);

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

            // Load initial values
            isDisplayed.setChecked(editGroup.isDisplayed());
            isDefault.setChecked(editGroup.isDefault());
            if (editGroup.getItemValue() != null) {
                groupNameView.setText(editGroup.getItemValue(), null);
                keyworkerSpinner.setSelection(keyworkerPickList.getPosition(editGroup.getKeyWorker()));
                sessionCoordinatorSpinner.setSelection(sessionCoordinatorPickList.getPosition(editGroup.getSessionCoordinator()));
                addressView.setText(editGroup.getGroupAddress(), null);
                postcodeView.setText(editGroup.getGroupPostcode(), null);
                frequencyView.setText(String.format(Locale.UK, "%d", editGroup.getFrequency()));
                frequencyTypeSpinner.setSelection(frequencyTypeAdapter.getPosition(editGroup.getFrequencyType()));
            }
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
        groupNameView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        // Group Name
        String sGroupName = groupNameView.getText().toString().trim();
        if (TextUtils.isEmpty(sGroupName)) {
            groupNameView.setError(getString(R.string.error_field_required));
            focusView = groupNameView;
            success = false;
        } else {
            editGroup.setItemValue(sGroupName);
        }

        editGroup.setIsDisplayed(isDisplayed.isChecked());
        editGroup.setIsDefault(isDefault.isChecked());

        //Keyworker
        User newKeyworker = keyworkerPickList.getUsers().get(keyworkerSpinner.getSelectedItemPosition());
        // Test for Please select
        if (newKeyworker.getUserID().equals(User.unknownUser)) {
                TextView errorText = (TextView) keyworkerSpinner.getSelectedView();
                //errorText.setError("anything here, just to add the icon");
                errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                focusView = keyworkerSpinner;
                success = false;

        } else {
            editGroup.setKeyWorkerID(newKeyworker.getUserID());
            // PickList contained
            editGroup.setKeyWorker(localDB.getUser(editGroup.getKeyWorkerID()));
        }

        //Session Coordinator (Not mandatory)
        User newSessionCoordinator = sessionCoordinatorPickList.getUsers().get(sessionCoordinatorSpinner.getSelectedItemPosition());
        if (!newSessionCoordinator.getUserID().equals(User.unknownUser)) {
            editGroup.setSessionCoordinatorID(newSessionCoordinator.getUserID());
            // PickList contained
            editGroup.setSessionCoordinator(localDB.getUser(editGroup.getSessionCoordinatorID()));
        }

        // Address
        String sAddress = addressView.getText().toString().trim();
        if (TextUtils.isEmpty(sAddress)) {
            addressView.setError(getString(R.string.error_field_required));
            focusView = addressView;
            success = false;
        } else {
            editGroup.setGroupAddress(sAddress);
        }

        // Postcode
        String sPostcode = postcodeView.getText().toString().trim();
        if (TextUtils.isEmpty(sPostcode)) {
            postcodeView.setError(getString(R.string.error_field_required));
            focusView = postcodeView;
            success = false;
        } else {
            editGroup.setGroupPostcode(sPostcode);
        }

        // Frequency
        String sFrequency = frequencyView.getText().toString().trim();
        if (TextUtils.isEmpty(sFrequency)) {
            frequencyView.setError(getString(R.string.error_field_required));
                focusView = frequencyView;
                success = false;
        } else {
            int frequency;
            try {
                frequency = Integer.parseInt(sFrequency);
                if (frequency < 0){
                    frequencyView.setError(getString(R.string.error_number_not_positive));
                    focusView = frequencyView;
                    success = false;
                } else {
                    editGroup.setFrequency(frequency);
                }
            }
            catch (Exception ex){
                frequencyView.setError(getString(R.string.error_invalid_integer));
                focusView = frequencyView;
                success = false;
            }
        }

        //Frequency Type
        String newFrequencyType= frequencyTypeSpinner.getSelectedItem().toString();
        if (newFrequencyType.equals("Please select")){
            TextView errorText = (TextView) frequencyTypeSpinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = frequencyTypeSpinner;
            success = false;
        } else {
            editGroup.setFrequencyType(newFrequencyType);
        }

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
            editGroup.save(newMode);
        } catch (SQLiteConstraintException ex) {
            // ItemValue was not unique
            groupNameView.setError(getString(R.string.error_value_not_unique));
            groupNameView.requestFocus();
            success = false;
        }
        // If this item has been set to the Default, remove any other default
        if (editGroup.isDefault()){
            LocalDB localDB = LocalDB.getInstance();
            ArrayList<ListItem> groups = localDB.getAllListItems(ListType.GROUP.toString(), true);
            for (ListItem group:groups){
                if (group.isDefault() && !group.getItemValue().equals(editGroup.getItemValue())) {
                    group.setIsDefault(false);
                    group.save(false);
                }
            }
        }
        return success;
    }
}
