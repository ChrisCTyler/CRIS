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
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Client;
import solutions.cris.object.Document;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.PickList;

/**
 * Copyright CRIS.Solutions 13/12/2016.
 */

public class EditClient extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    // UI references.
    private EditText firstNamesView;
    private EditText lastNameView;
    private EditText dateOfBirthView;
    private EditText addressView;
    private EditText postcodeView;
    private EditText contactNumberView;
    private EditText contactNumber2View;
    private EditText emailView;
    private Spinner genderView;
    private Spinner ethnicityView;

    private Client editClient;
    private LocalDB localDB;
    private View parent;
    private boolean isNewMode;

    private PickList genderPickList;
    private PickList ethnicityPicklist;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.edit_client, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        if (((ListActivity) getActivity()).getMode() == Document.Mode.NEW) {
            isNewMode = true;
        }
        // Get the document to be edited from the activity
        editClient = (Client) ((ListActivity) getActivity()).getDocument();
        // New field added v1.1
        if (editClient.getContactNumber2() == null) {
            editClient.setContactNumber2("");
        }
        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();

        if (isNewMode) {
            toolbar.setTitle(getString(R.string.app_name) + " - New Client");
        } else {
            toolbar.setTitle(getString(R.string.app_name) + " - Edit Client");
        }

        // Hide the FAB
        fab.setVisibility(View.GONE);

        // Clear the footer
        TextView footer = getActivity().findViewById(R.id.footer);
        footer.setText("");

        localDB = LocalDB.getInstance();

        // Set up the form.
        firstNamesView = parent.findViewById(R.id.first_names);
        lastNameView = parent.findViewById(R.id.last_name);
        dateOfBirthView = parent.findViewById(R.id.date_of_birth);
        addressView = parent.findViewById(R.id.address);
        postcodeView = parent.findViewById(R.id.postcode);
        contactNumberView = parent.findViewById(R.id.contact_number);
        contactNumber2View = parent.findViewById(R.id.contact_number2);
        emailView = parent.findViewById(R.id.email);
        genderView = parent.findViewById(R.id.gender_spinner);
        ethnicityView = parent.findViewById(R.id.ethnicity_spinner);

        // Initialise the Gender Spinner
        genderPickList = new PickList(localDB, ListType.GENDER);
        ArrayAdapter<String> genderAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, genderPickList.getOptions());
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderView.setAdapter(genderAdapter);
        genderView.setSelection(genderPickList.getDefaultPosition());

        // Initialise the Ethnicity Spinner
        ethnicityPicklist = new PickList(localDB, ListType.ETHNICITY);
        ArrayAdapter<String> ethnicityAdpter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, ethnicityPicklist.getOptions());
        ethnicityAdpter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ethnicityView.setAdapter(ethnicityAdpter);
        ethnicityView.setSelection(ethnicityPicklist.getDefaultPosition());

        dateOfBirthView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                dateOfBirthPicker();
                return true;
            }
        });

        // Cancel Button
        Button cancelButton = parent.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Cancel so no need to update list of documents
                ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                ((ListActivity) getActivity()).setDocument(null);
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.popBackStack();
            }
        });
        // Save Button
        Button saveButton = parent.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {
                    editClient.save(isNewMode);
                    // Build 144 - Need to update client in calling activity so that
                    // any subsequent changes are saved against theis new client
                    ((ListActivity) getActivity()).setClient(editClient);
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.popBackStack();
                }
            }
        });

        // Load initial values
        if (editClient.getFirstNames() != null) {
            firstNamesView.setText(editClient.getFirstNames(), null);
            lastNameView.setText(editClient.getLastName(), null);
            Date dateOfBirth = editClient.getDateOfBirth();
            dateOfBirthView.setText(sDate.format(dateOfBirth.getTime()));
            addressView.setText(editClient.getAddress(), null);
            postcodeView.setText(editClient.getPostcode(), null);
            contactNumberView.setText(editClient.getContactNumber(), null);
            if (!editClient.getContactNumber2().isEmpty()) {
                contactNumber2View.setText(editClient.getContactNumber2());
            }
            emailView.setText(editClient.getEmailAddress(), null);
            // Build 170 - Instance of null gender found so handle the possibility
            // int position = genderPickList.getOptions().indexOf(editClient.getGender().getItemValue());
            int position = 0;
            ListItem gender = editClient.getGender();
            // Build 171 - Handle unexpected null ListItem ID
            //if (gender != null) {
            if (!gender.getItemValue().equals("Unknown")) {
                position = genderPickList.getOptions().indexOf(gender.getItemValue());
                genderView.setSelection(position);
            }
            // Build 171 - Handle unexpected null ListItem ID
            ListItem ethnicity = editClient.getEthnicity();
            if (!ethnicity.getItemValue().equals("Unknown")) {
                position = ethnicityPicklist.getOptions().indexOf(editClient.getEthnicity().getItemValue());
                ethnicityView.setSelection(position);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        // Share option only exists if called from ListClientHeader
        if (shareOption != null) {
            shareOption.setVisible(false);
        }
    }

    private void dateOfBirthPicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog startDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                dateOfBirthView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        startDatePickerDialog.show();
    }

    // Validate the document
    private boolean validate() {
        boolean success = true;

        // Clear any existing errors

        firstNamesView.setError(null);
        lastNameView.setError(null);
        dateOfBirthView.setError(null);
        addressView.setError(null);
        postcodeView.setError(null);
        contactNumberView.setError(null);
        emailView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        // FirstNames
        String sFirstName = firstNamesView.getText().toString().trim();
        if (TextUtils.isEmpty(sFirstName)) {
            firstNamesView.setError(getString(R.string.error_field_required));
            focusView = firstNamesView;
            success = false;
        } else {
            editClient.setFirstNames(sFirstName);
        }
        // LastName
        String sLastName = lastNameView.getText().toString().trim();
        if (TextUtils.isEmpty(sLastName)) {
            lastNameView.setError(getString(R.string.error_field_required));
            focusView = lastNameView;
            success = false;
        } else {
            editClient.setLastName(sLastName);
        }

        // DateOfBirth
        String sDateOfBirth = dateOfBirthView.getText().toString();
        if (TextUtils.isEmpty(sDateOfBirth)) {
            dateOfBirthView.setError(getString(R.string.error_field_required));
            focusView = dateOfBirthView;
            success = false;
        } else {
            Date dDateOfBirth = CRISUtil.parseDate(sDateOfBirth);
            if (dDateOfBirth == null) {
                dateOfBirthView.setError(getString(R.string.error_invalid_date));
                focusView = dateOfBirthView;
                success = false;
            } else {
                editClient.setDateOfBirth(dDateOfBirth);
            }
        }

        // Address
        // Build 188 - Address is now non-mandatory
        /*
        String sAddress = addressView.getText().toString().trim();
        if (TextUtils.isEmpty(sAddress)) {
            addressView.setError(getString(R.string.error_field_required));
            focusView = addressView;
            success = false;
        } else {
            editClient.setAddress(sAddress);
        }
        */
        editClient.setAddress(addressView.getText().toString().trim());

        // Postcode
        // Build 188 - Postcode is now non-mandatory
        /*
        String sPostcode = postcodeView.getText().toString().trim();
        if (TextUtils.isEmpty(sPostcode)) {
            postcodeView.setError(getString(R.string.error_field_required));
            focusView = postcodeView;
            success = false;
        } else {
            editClient.setPostcode(sPostcode);
        }
        */
        editClient.setPostcode(postcodeView.getText().toString().trim());

        // ContactNumber
        // Build 188 - Contact Number is now non-mandatory
        /*
        String sContactNumber = contactNumberView.getText().toString().trim();
        if (TextUtils.isEmpty(sContactNumber)) {
            contactNumberView.setError(getString(R.string.error_field_required));
            focusView = contactNumberView;
            success = false;
        } else {
            editClient.setContactNumber(contactNumberView.getText().toString().trim());
        }
        */
        editClient.setContactNumber(contactNumberView.getText().toString().trim());

        // Contact Number2
        editClient.setContactNumber2(contactNumber2View.getText().toString().trim());

        // Email
        // Build 188 - EmailAddress is now non-mandatory
        /*
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
            editClient.setEmailAddress(sEmail);
        }
        */
        // Still need to check for a valid email address if one is specified
        String sEmail = emailView.getText().toString().trim().toLowerCase();
        if (!TextUtils.isEmpty(sEmail) && !isEmailValid(sEmail)) {
            emailView.setError(getString(R.string.error_invalid_email));
            focusView = emailView;
            success = false;
        } else {
            editClient.setEmailAddress(sEmail);
        }

        //Gender
        ListItem newGender = genderPickList.getListItems().get(genderView.getSelectedItemPosition());
        // Test for Please select
        if (newGender.getItemOrder() == -1) {
            TextView errorText = (TextView) genderView.getSelectedView();
            errorText.setError("anything here, just to add the icon");
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = genderView;
            success = false;
        } else {
            editClient.setGenderID(newGender.getListItemID());
            // PickList contained
            editClient.setGender(localDB.getListItem(editClient.getGenderID()));
        }

        //Ethnicity
        ListItem newEthnicity = ethnicityPicklist.getListItems().get(ethnicityView.getSelectedItemPosition());
        // Test for Please select
        if (newEthnicity.getItemOrder() == -1) {
            TextView errorText = (TextView) ethnicityView.getSelectedView();
            errorText.setError("anything here, just to add the icon");
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = ethnicityView;
            success = false;
        } else {
            editClient.setEthnicityID(newEthnicity.getListItemID());
            // PickList contained
            editClient.setEthnicity(localDB.getListItem(editClient.getEthnicityID()));
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

}
