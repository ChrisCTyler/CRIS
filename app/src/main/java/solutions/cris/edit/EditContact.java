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
// Build 200 Use the androidX Fragment class
//import android.app.Fragment;
//import android.app.FragmentManager;
import androidx.fragment.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Agency;
import solutions.cris.object.Client;
import solutions.cris.object.Contact;
import solutions.cris.object.Document;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.School;
import solutions.cris.object.User;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.PickList;

public class EditContact extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private Client client;
    private Contact editDocument;

    private Spinner contactTypeSpinner;
    private Spinner agencySpinner;
    private LinearLayout agencyLayout;
    private Spinner schoolSpinner;
    private LinearLayout schoolLayout;
    private TextView backgroundInformationView;
    private LinearLayout backgroundLayout;
    private EditText nameView;
    private LinearLayout addressLayout;
    private EditText addressView;
    private EditText postcodeView;
    private EditText contactNumberView;
    private EditText emailAddressView;
    private EditText startDateView;
    private EditText endDateView;
    private EditText additionalInformationView;
    private Spinner relationshipSpinner;
    // Build 181
    private LinearLayout freeSchoolMealsLayoutView;
    private CheckBox freeSchoolMeals;
    // Build 183
    private LinearLayout specialNeedsLayoutView;
    private CheckBox specialNeeds;

    private LocalDB localDB;
    private View parent;
    private boolean isNewMode = false;

    private PickList contactTypePickList;
    private PickList agencyPickList;
    private PickList schoolPickList;
    private PickList relationshipPickList;

    private TextView hintTextView;
    private boolean hintTextDisplayed = true;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.edit_contact, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (((ListActivity) getActivity()).getMode() == Document.Mode.NEW) {
            isNewMode = true;
        }

        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        if (isNewMode) {
            toolbar.setTitle(getString(R.string.app_name) + " - New Contact");
        } else {
            toolbar.setTitle(getString(R.string.app_name) + " - Edit Contact");
        }

        // Hide the FAB
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        fab.setVisibility(View.GONE);

        // Clear the footer
        TextView footer = getActivity().findViewById(R.id.footer);
        footer.setText("");

        // Get the document to be edited from the activity
        client = ((ListActivity) getActivity()).getClient();
        editDocument = (Contact) ((ListActivity) getActivity()).getDocument();
        localDB = LocalDB.getInstance();

        // CANCEL BOX
        if (editDocument.getCancelledFlag()) {
            LinearLayout cancelBoxView = parent.findViewById(R.id.cancel_box_layout);
            cancelBoxView.setVisibility(View.VISIBLE);
            TextView cancelBy = parent.findViewById(R.id.cancel_by);
            String byText = "by ";
            User cancelUser = localDB.getUser(editDocument.getCancelledByID());
            byText += cancelUser.getFullName() + " on ";
            byText += sDate.format(editDocument.getCancellationDate());
            cancelBy.setText(byText);
            TextView cancelReason = parent.findViewById(R.id.cancel_reason);
            cancelReason.setText(String.format("Reason: %s", editDocument.getCancellationReason()));
        }

        contactTypeSpinner = parent.findViewById(R.id.contact_type_spinner);
        agencySpinner = parent.findViewById(R.id.agency_spinner);
        agencyLayout = parent.findViewById(R.id.agency_layout);
        schoolSpinner = parent.findViewById(R.id.school_spinner);
        schoolLayout = parent.findViewById(R.id.school_layout);
        backgroundInformationView = parent.findViewById(R.id.background_information);
        backgroundLayout = parent.findViewById(R.id.background_layout);
        addressLayout = parent.findViewById(R.id.address_layout);
        nameView = parent.findViewById(R.id.name);
        addressView = parent.findViewById(R.id.address);
        postcodeView = parent.findViewById(R.id.postcode);
        contactNumberView = parent.findViewById(R.id.contact_number);
        emailAddressView = parent.findViewById(R.id.email_address);
        startDateView = parent.findViewById(R.id.start_date);
        endDateView = parent.findViewById(R.id.end_date);
        additionalInformationView = parent.findViewById(R.id.additional_information);
        relationshipSpinner = parent.findViewById(R.id.relationship_spinner);

        // Build 181
        freeSchoolMealsLayoutView = parent.findViewById(R.id.free_school_meals_layout);
        freeSchoolMealsLayoutView.setVisibility(View.GONE);
        freeSchoolMeals = parent.findViewById(R.id.free_school_meals);
        freeSchoolMeals.setVisibility(View.GONE);
        // Build 183
        specialNeedsLayoutView = parent.findViewById(R.id.special_needs_layout);
        specialNeedsLayoutView.setVisibility(View.GONE);
        specialNeeds = parent.findViewById(R.id.special_needs);
        specialNeeds.setVisibility(View.GONE);


        // Set up the hint text
        hintTextView = parent.findViewById(R.id.hint_text);
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

        // Initialise the contact Type Spinner
        contactTypePickList = new PickList(localDB, ListType.CONTACT_TYPE);
        ArrayAdapter<String> contactTypeAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, contactTypePickList.getOptions());
        contactTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        contactTypeSpinner.setAdapter(contactTypeAdapter);
        contactTypeSpinner.setSelection(contactTypePickList.getDefaultPosition());
        // Hide the School and Agency Spinners by default
        schoolLayout.setVisibility(View.GONE);
        agencyLayout.setVisibility(View.GONE);
        backgroundLayout.setVisibility(View.GONE);
        addressView.setVisibility(View.VISIBLE);
        postcodeView.setVisibility(View.VISIBLE);
        contactTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selected = contactTypePickList.getOptions().get(i);
                if (selected != null) {
                    switch (selected) {
                        case "Agency Contact":
                            schoolLayout.setVisibility(View.GONE);
                            editDocument.setSchoolID(null);
                            editDocument.setSchool(null);
                            agencyLayout.setVisibility(View.VISIBLE);
                            backgroundLayout.setVisibility(View.VISIBLE);
                            addressLayout.setVisibility(View.GONE);
                            addressView.setText("");
                            postcodeView.setText("");
                            // Build 181
                            // Build 233 FSM moved to Case Document
                            //freeSchoolMealsLayoutView.setVisibility(View.GONE);
                            //freeSchoolMeals.setVisibility(View.GONE);
                            // Build 183
                            specialNeedsLayoutView.setVisibility(View.GONE);
                            specialNeeds.setVisibility(View.GONE);
                            break;
                        case "School Contact":
                            schoolLayout.setVisibility(View.VISIBLE);
                            agencyLayout.setVisibility(View.GONE);
                            editDocument.setAgencyID(null);
                            editDocument.setAgency(null);
                            backgroundLayout.setVisibility(View.VISIBLE);
                            addressLayout.setVisibility(View.GONE);
                            addressView.setText("");
                            postcodeView.setText("");
                            // Build 181
                            // Build 233 FSM moved to Case Document
                            //freeSchoolMealsLayoutView.setVisibility(View.VISIBLE);
                            //freeSchoolMeals.setVisibility(View.VISIBLE);
                            // Build 233 Fixed bug, following code was missing
                            specialNeedsLayoutView.setVisibility(View.VISIBLE);
                            specialNeeds.setVisibility(View.VISIBLE);
                            break;
                        default:
                            schoolLayout.setVisibility(View.GONE);
                            editDocument.setSchoolID(null);
                            editDocument.setSchool(null);
                            agencyLayout.setVisibility(View.GONE);
                            editDocument.setAgencyID(null);
                            editDocument.setAgency(null);
                            backgroundLayout.setVisibility(View.GONE);
                            addressLayout.setVisibility(View.VISIBLE);
                            // Build 181
                            specialNeedsLayoutView.setVisibility(View.GONE);
                            specialNeeds.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                schoolLayout.setVisibility(View.GONE);
                agencyLayout.setVisibility(View.GONE);
            }
        });

        // Initialise the Agency Spinner
        agencyPickList = new PickList(localDB, ListType.AGENCY);
        ArrayAdapter<String> agencyTypeAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, agencyPickList.getOptions());
        agencyTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        agencySpinner.setAdapter(agencyTypeAdapter);
        agencySpinner.setSelection(agencyPickList.getDefaultPosition());
        agencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ListItem selected = agencyPickList.getListItems().get(i);
                Agency agency = (Agency) localDB.getListItem(selected.getListItemID());
                if (agency != null) {
                    backgroundInformationView.setText(agency.textSummary());
                } else {
                    backgroundInformationView.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                backgroundInformationView.setText("");
            }
        });

        // Initialise the School Spinner
        schoolPickList = new PickList(localDB, ListType.SCHOOL);
        ArrayAdapter<String> schoolTypeAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, schoolPickList.getOptions());
        schoolTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        schoolSpinner.setAdapter(schoolTypeAdapter);
        schoolSpinner.setSelection(schoolPickList.getDefaultPosition());
        schoolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ListItem selected = schoolPickList.getListItems().get(i);
                School school = (School) localDB.getListItem(selected.getListItemID());
                if (school != null) {
                    backgroundInformationView.setText(school.textSummary());
                } else {
                    backgroundInformationView.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                backgroundInformationView.setText("");
            }
        });

        // Initialise the relationhip Spinner
        relationshipPickList = new PickList(localDB, ListType.RELATIONSHIP);
        ArrayAdapter<String> relationshipTypeAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, relationshipPickList.getOptions());
        relationshipTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        relationshipSpinner.setAdapter(relationshipTypeAdapter);
        relationshipSpinner.setSelection(relationshipPickList.getDefaultPosition());


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

        addressView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (client != null) {
                    addressView.setText(client.getAddress());
                    addressView.setSelection(client.getAddress().length());
                    postcodeView.setText(client.getPostcode());
                }
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
                // Build 200 Use the androidX Fragment class
                //FragmentManager fragmentManager = getFragmentManager();
                //fragmentManager.popBackStack();
                getParentFragmentManager().popBackStack();
            }
        });
        // Save Button
        Button saveButton = parent.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {
                    editDocument.save(isNewMode);
                    // Build 200 Use the androidX Fragment class
                    //FragmentManager fragmentManager = getFragmentManager();
                    //fragmentManager.popBackStack();
                    getParentFragmentManager().popBackStack();
                }
            }
        });

        // Set Defaults
        Date today = new Date();

        if (isNewMode) {
            // Build 181 - Default to Free School Meals
            // Build 233 Moved FSM to Case document
            //freeSchoolMeals.setChecked(true);
            // Build 183 - Default to Free School Meals
            specialNeeds.setChecked(false);
            startDateView.setText(sDate.format(today));
            nameView.clearFocus();

        } else {
            // Edit Mode
            // Build 171 - Handle unexpected null ListItem ID
            if (!editDocument.getContactType().getItemValue().equals("Unknown")) {
                contactTypeSpinner.setSelection(contactTypePickList.getPosition(editDocument.getContactType()));
            }
            // Build 125 - 18/07/2019 Odd case where crash happened because contact type was not set
            // in edit mode. Should not be possible due to prior validation check. However, the
            // following code change is safer
            /*
            switch (editDocument.getContactType().getItemValue()) {
                case "Agency Contact":
                    schoolLayout.setVisibility(View.GONE);
                    agencyLayout.setVisibility(View.VISIBLE);
                    addressLayout.setVisibility(View.GONE);
                    agencySpinner.setSelection(agencyPickList.getPosition(editDocument.getAgency()));
                    break;
                case "School Contact":
                    schoolLayout.setVisibility(View.VISIBLE);
                    agencyLayout.setVisibility(View.GONE);
                    addressLayout.setVisibility(View.GONE);
                    schoolSpinner.setSelection(schoolPickList.getPosition(editDocument.getSchool()));
                    break;
                default:
                    schoolLayout.setVisibility(View.GONE);
                    agencyLayout.setVisibility(View.GONE);
                    addressLayout.setVisibility(View.VISIBLE);
            }
            */
            // Preset fo the default case
            schoolLayout.setVisibility(View.GONE);
            agencyLayout.setVisibility(View.GONE);
            addressLayout.setVisibility(View.VISIBLE);
            if (editDocument.getContactType() != null) {
                switch (editDocument.getContactType().getItemValue()) {
                    case "Agency Contact":
                        schoolLayout.setVisibility(View.GONE);
                        agencyLayout.setVisibility(View.VISIBLE);
                        addressLayout.setVisibility(View.GONE);
                        agencySpinner.setSelection(agencyPickList.getPosition(editDocument.getAgency()));
                        break;
                    case "School Contact":
                        schoolLayout.setVisibility(View.VISIBLE);
                        agencyLayout.setVisibility(View.GONE);
                        addressLayout.setVisibility(View.GONE);
                        schoolSpinner.setSelection(schoolPickList.getPosition(editDocument.getSchool()));
                        // Build 181
                        // Build 233
                        //freeSchoolMealsLayoutView.setVisibility(View.VISIBLE);
                        //freeSchoolMeals.setVisibility(View.VISIBLE);
                        //freeSchoolMeals.setChecked(editDocument.isFreeSchoolMeals());
                        // Build 183
                        specialNeedsLayoutView.setVisibility(View.VISIBLE);
                        specialNeeds.setVisibility(View.VISIBLE);
                        specialNeeds.setChecked(editDocument.isSpecialNeeds());
                        break;
                }
            }
            nameView.setText(editDocument.getContactName());
            addressView.setText(editDocument.getContactAddress());
            postcodeView.setText(editDocument.getContactPostcode());
            contactNumberView.setText(editDocument.getContactContactNumber());
            emailAddressView.setText(editDocument.getContactEmailAddress());
            startDateView.setText(sDate.format(editDocument.getStartDate()));
            if (editDocument.getEndDate().getTime() != Long.MIN_VALUE) {
                endDateView.setText(sDate.format(editDocument.getEndDate()));
            }
            additionalInformationView.setText(editDocument.getContactAdditionalInformation());
            // Build 171 - Handle unexpected null ListItem ID
            if (!editDocument.getRelationshipType().getItemValue().equals("Unknown")) {
                relationshipSpinner.setSelection(relationshipPickList.getPosition(editDocument.getRelationshipType()));
            }
        }
    }

    // MENU BLOCK
    private static final int MENU_CANCEL_DOCUMENT = Menu.FIRST + 1;
    private static final int MENU_UNCANCEL_DOCUMENT = Menu.FIRST + 2;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Initialise the Cancellation menu option
        if (editDocument.getCancelledFlag()) {
            MenuItem cancelOption = menu.add(0, MENU_UNCANCEL_DOCUMENT, 2, "Remove Cancellation");
            cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        } else {
            MenuItem cancelOption = menu.add(0, MENU_CANCEL_DOCUMENT, 3, "Cancel Document");
            cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        shareOption.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case MENU_CANCEL_DOCUMENT:
                cancelDocument(true);
                return true;

            case MENU_UNCANCEL_DOCUMENT:
                cancelDocument(false);
                return true;

            default:
                return false;
        }
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

    private void cancelDocument(boolean cancelType) {
        if (cancelType) {
            // Get the reason and then call the validate/save sequence.
            final EditText editText = new EditText(getActivity());
            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            new AlertDialog.Builder(getActivity())
                    .setView(editText)
                    .setTitle("Cancel Document")
                    .setMessage("Documents may not be removed, but cancelling them " +
                            "will remove them from view unless the user explicitly requests " +
                            "them. Please specify a cancellation reason")
                    .setPositiveButton("CancelDocument", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (editText.getText().length() > 0) {
                                editDocument.setCancellationDate(new Date());
                                editDocument.setCancellationReason(editText.getText().toString());
                                editDocument.setCancelledByID(((ListActivity) getActivity()).getCurrentUser().getUserID());
                                editDocument.setCancelledFlag(true);
                                if (validate()) {
                                    editDocument.save(isNewMode);
                                    // Build 200 Use the androidX Fragment class
                                    //FragmentManager fragmentManager = getFragmentManager();
                                    //fragmentManager.popBackStack();
                                    getParentFragmentManager().popBackStack();
                                }
                            }
                        }
                    })
                    .setNegativeButton("DoNotCancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        } else {  // Uncancel the Document
            editDocument.setCancelledFlag(false);
            editDocument.setCancellationReason("");
            editDocument.setCancellationDate(new Date(Long.MIN_VALUE));
            editDocument.setCancelledByID(null);
            if (validate()) {
                editDocument.save(isNewMode);
                // Build 200 Use the androidX Fragment class
                //FragmentManager fragmentManager = getFragmentManager();
                //fragmentManager.popBackStack();
                getParentFragmentManager().popBackStack();
            }
        }
    }

    private void startDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog startDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

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
        DatePickerDialog endDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

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

        nameView.setError(null);
        addressView.setError(null);
        postcodeView.setError(null);
        contactNumberView.setError(null);
        emailAddressView.setError(null);
        startDateView.setError(null);
        endDateView.setError(null);
        additionalInformationView.setError(null);

        // Build 181 - In case School Contact changed to other contact type, set FSM false here
        // Build 233 FSM moved to Case Document
        //editDocument.setFreeSchoolMeals(false);
        // Build 183 - In case Special Needs changed to other contact type, set FSM false here
        editDocument.setSpecialNeeds(false);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        //ContactType
        ListItem newContactType = contactTypePickList.getListItems().get(contactTypeSpinner.getSelectedItemPosition());
        // Test for Please select
        if (newContactType.getItemOrder() == -1) {
            TextView errorText = (TextView) contactTypeSpinner.getSelectedView();
            errorText.setError("anything here, just to add the icon");
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = contactTypeSpinner;
            success = false;
        } else {
            editDocument.setContactTypeID(newContactType.getListItemID());
            // PickList contained
            editDocument.setContactType(localDB.getListItem(editDocument.getContactTypeID()));

            //Agency
            if (newContactType.getItemValue().equals("Agency Contact")) {
                ListItem newAgency = agencyPickList.getListItems().get(agencySpinner.getSelectedItemPosition());
                // Test for Please select
                if (newAgency.getItemOrder() == -1) {
                    TextView errorText = (TextView) agencySpinner.getSelectedView();
                    errorText.setError("anything here, just to add the icon");
                    errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                    focusView = agencySpinner;
                    success = false;
                } else {
                    editDocument.setAgencyID(newAgency.getListItemID());
                    // PickList contained
                    editDocument.setAgency(localDB.getListItem(editDocument.getAgencyID()));
                }
            } else {
                editDocument.setAgencyID(null);
                // PickList contained
                editDocument.setAgency(null);
            }

            //School
            if (newContactType.getItemValue().equals("School Contact")) {
                ListItem newSchool = schoolPickList.getListItems().get(schoolSpinner.getSelectedItemPosition());
                // Test for Please select
                if (newSchool.getItemOrder() == -1) {
                    TextView errorText = (TextView) schoolSpinner.getSelectedView();
                    errorText.setError("anything here, just to add the icon");
                    errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                    focusView = schoolSpinner;
                    success = false;
                } else {
                    editDocument.setSchoolID(newSchool.getListItemID());
                    // PickList contained
                    editDocument.setSchool(localDB.getListItem(editDocument.getSchoolID()));
                    // Build 181
                    // Build 233 Moved FSM to Case document
                    //editDocument.setFreeSchoolMeals(freeSchoolMeals.isChecked());
                    // Build 183
                    editDocument.setSpecialNeeds(specialNeeds.isChecked());
                }
            } else {
                editDocument.setSchoolID(null);
                // PickList contained
                editDocument.setSchool(null);
            }

            // Name
            String sName = nameView.getText().toString().trim();
            if (TextUtils.isEmpty(sName)) {
                nameView.setError(getString(R.string.error_field_required));
                focusView = nameView;
                success = false;
            } else {
                editDocument.setContactName(sName);
            }

            //Address/Postcode
            if (!newContactType.getItemValue().equals("Agency Contact") &&
                    !newContactType.getItemValue().equals("School Contact")) {
                // Address
                String sAddress = addressView.getText().toString().trim();
                if (!TextUtils.isEmpty(sAddress)) {
                    editDocument.setContactAddress(sAddress);
                } else {
                    editDocument.setContactAddress("");
                }

                // Postcode
                String sPostcode = postcodeView.getText().toString().trim();
                if (!TextUtils.isEmpty(sPostcode)) {
                    editDocument.setContactPostcode(sPostcode);
                } else {
                    editDocument.setContactPostcode("");
                }
            }

            // ContactNumber
            String sContactNumber = contactNumberView.getText().toString().trim();
            if (!TextUtils.isEmpty(sContactNumber)) {
                editDocument.setContactContactNumber(contactNumberView.getText().toString().trim());
            } else {
                editDocument.setContactContactNumber("");
            }

            // Email
            String sEmail = emailAddressView.getText().toString().trim().toLowerCase();
            if (!TextUtils.isEmpty(sEmail)) {
                if (!isEmailValid(sEmail)) {
                    emailAddressView.setError(getString(R.string.error_invalid_email));
                    focusView = emailAddressView;
                    success = false;
                } else {
                    editDocument.setContactEmailAddress(sEmail);
                }
            } else {
                editDocument.setContactEmailAddress("");
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
                    editDocument.setStartDate(dStartDate);
                }
            }
            // EndDate
            String sEndDate = endDateView.getText().toString();
            if (TextUtils.isEmpty(sEndDate)) {
                editDocument.setEndDate(new Date(Long.MIN_VALUE));
            } else {
                Date dEndDate = CRISUtil.parseDate(sEndDate);
                if (dEndDate == null) {
                    endDateView.setError(getString(R.string.error_invalid_date));
                    focusView = endDateView;
                    success = false;
                } else {
                    editDocument.setEndDate(dEndDate);
                }
            }

            // StartDate/EndDate consistency check
            if (editDocument.getEndDate().getTime() != Long.MIN_VALUE) {
                if (editDocument.getStartDate() != null && editDocument.getEndDate() != null) {
                    if (!editDocument.getStartDate().before(editDocument.getEndDate())) {
                        endDateView.setError(getString(R.string.error_invalid_date_order));
                        focusView = endDateView;
                        success = false;
                    }
                }
            }

            // AdditionalInformation
            String sAdditionalInformation = additionalInformationView.getText().toString().trim();
            if (!TextUtils.isEmpty(sAdditionalInformation)) {
                editDocument.setContactAdditionalInformation(additionalInformationView.getText().toString().trim());
            }

            //Relationship
            ListItem newRelationship = relationshipPickList.getListItems().get(relationshipSpinner.getSelectedItemPosition());
            // Test for Please select
            if (newRelationship.getItemOrder() == -1) {
                TextView errorText = (TextView) relationshipSpinner.getSelectedView();
                errorText.setError("anything here, just to add the icon");
                errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                focusView = relationshipSpinner;
                success = false;
            } else {
                editDocument.setRelationshipTypeID(newRelationship.getListItemID());
                // PickList contained
                editDocument.setRelationshipType(localDB.getListItem(editDocument.getRelationshipTypeID()));
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

    private String getHintText() {
        return "Longpress the Address field to use the client's address.";
    }
}
