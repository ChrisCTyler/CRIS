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
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
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
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISParseDateException;
import solutions.cris.exceptions.CRISParseTimeException;
import solutions.cris.list.ListActivity;
import solutions.cris.list.ListClientHeader;
import solutions.cris.list.ListSessionClients;
import solutions.cris.object.Client;
import solutions.cris.object.Document;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.Transport;
import solutions.cris.object.TransportOrganisation;
import solutions.cris.object.User;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.PickList;

public class EditTransport extends Fragment {

    public static final String TO_HINT_DISPLAYED = "solutions.cris.ToHintDisplayed";
    public static final String FROM_HINT_DISPLAYED = "solutions.cris.FromHintDisplayed";
    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);
    private static final SimpleDateFormat sTime = new SimpleDateFormat("HH:mm", Locale.UK);

    private Client client;
    private Transport editDocument;

    private Spinner transportSpinner;
    private TextView transportContactNumberView;
    private TextView transportEmailView;
    private TextView backgroundInformationView;
    private CheckBox transportBooked;
    private CheckBox outboundRequired;
    private LinearLayout outboundDateTimeLayout;
    private EditText outboundDateView;
    private EditText outboundTimeView;
    private CheckBox outboundUsed;
    private CheckBox returnRequired;
    private LinearLayout returnDateTimeLayout;
    private EditText returnDateView;
    private EditText returnTimeView;
    private CheckBox returnUsed;
    private EditText fromAddressView;
    private EditText fromPostcodeView;
    private EditText toAddressView;
    private EditText toPostcodeView;
    private EditText additionalInformationView;

    private TextView toHintTextView;
    private boolean toHintTextDisplayed = true;

    private TextView fromHintTextView;
    private boolean fromHintTextDisplayed = true;

    private LocalDB localDB;
    private User currentUser;
    private View parent;
    private boolean isNewMode = false;

    private PickList transportPickList;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.edit_transport, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        currentUser = ((ListActivity) getActivity()).getCurrentUser();
        editDocument = (Transport) ((ListActivity) getActivity()).getDocument();
        client = ((ListActivity) getActivity()).getClient();
        if (((ListActivity) getActivity()).getMode() == Document.Mode.NEW) {
            isNewMode = true;
        }

        if (isNewMode) {
            toolbar.setTitle(getString(R.string.app_name) + " - New Transport");
        } else {
            toolbar.setTitle(getString(R.string.app_name) + " - Edit Transport");
        }

        // Hide the FAB
        fab.setVisibility(View.GONE);

        // Clear the footer
        footer.setText("");

        // Get the document to be edited from the activity
        localDB = LocalDB.getInstance();

        // CANCEL BOX
        if (editDocument.getCancelledFlag()) {
            LinearLayout cancelBoxView = (LinearLayout) parent.findViewById(R.id.cancel_box_layout);
            cancelBoxView.setVisibility(View.VISIBLE);
            TextView cancelBy = (TextView) parent.findViewById(R.id.cancel_by);
            String byText = "by ";
            User cancelUser = localDB.getUser(editDocument.getCancelledByID());
            byText += cancelUser.getFullName() + " on ";
            byText += sDate.format(editDocument.getCancellationDate());
            cancelBy.setText(byText);
            TextView cancelReason = (TextView) parent.findViewById(R.id.cancel_reason);
            cancelReason.setText(String.format("Reason: %s", editDocument.getCancellationReason()));
        }

        transportSpinner = (Spinner) parent.findViewById(R.id.transport_spinner);
        transportContactNumberView = (TextView) parent.findViewById(R.id.transport_contact_number);
        transportEmailView = (TextView) parent.findViewById(R.id.transport_email);
        backgroundInformationView = (TextView) parent.findViewById(R.id.background_information);
        transportBooked = (CheckBox) parent.findViewById(R.id.booked);
        outboundRequired = (CheckBox) parent.findViewById(R.id.outbound_required);
        outboundDateTimeLayout = (LinearLayout) parent.findViewById(R.id.outbound_datetime_layout);
        outboundDateView = (EditText) parent.findViewById(R.id.outbound_date);
        outboundTimeView = (EditText) parent.findViewById(R.id.outbound_time);
        outboundUsed = (CheckBox) parent.findViewById(R.id.outbound_used);
        returnRequired = (CheckBox) parent.findViewById(R.id.return_required);
        returnDateTimeLayout = (LinearLayout) parent.findViewById(R.id.return_datetime_layout);
        returnDateView = (EditText) parent.findViewById(R.id.return_date);
        returnTimeView = (EditText) parent.findViewById(R.id.return_time);
        returnUsed = (CheckBox) parent.findViewById(R.id.return_used);
        fromAddressView = (EditText) parent.findViewById(R.id.from_address);
        fromPostcodeView = (EditText) parent.findViewById(R.id.from_postcode);
        toAddressView = (EditText) parent.findViewById(R.id.to_address);
        toPostcodeView = (EditText) parent.findViewById(R.id.to_postcode);
        additionalInformationView = (EditText) parent.findViewById(R.id.additional_information);

        // Set up the hint text
        toHintTextView = (TextView) parent.findViewById(R.id.to_hint_text);
        toHintTextView.setText("Longpress the To Address field to use the client's address as the address to return to.");
        // Restore value of hintDisplayed (Set to opposite, toggle to come
        if (savedInstanceState != null) {
            toHintTextDisplayed = !savedInstanceState.getBoolean(TO_HINT_DISPLAYED);
        }
        toggleToHint();
        toHintTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleToHint();
            }
        });

        fromHintTextView = (TextView) parent.findViewById(R.id.from_hint_text);
        fromHintTextView.setText("Longpress the From Address field to use the client's address as the address to start from.");
        // Restore value of hintDisplayed (Set to opposite, toggle to come
        if (savedInstanceState != null) {
            fromHintTextDisplayed = !savedInstanceState.getBoolean(FROM_HINT_DISPLAYED);
        }
        toggleFromHint();
        fromHintTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFromHint();
            }
        });

        // Initialise the Transport Spinner
        transportPickList = new PickList(localDB, ListType.TRANSPORT_ORGANISATION);
        ArrayAdapter<String> transportTypeAdapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, transportPickList.getOptions());
        transportTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transportSpinner.setAdapter(transportTypeAdapter);
        transportSpinner.setSelection(transportPickList.getDefaultPosition());
        transportSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ListItem selected = transportPickList.getListItems().get(i);
                TransportOrganisation transportOrganisation =
                        (TransportOrganisation) localDB.getListItem(selected.getListItemID());
                if (transportOrganisation != null) {
                    transportContactNumberView.setText(
                            String.format("tel: %s", transportOrganisation.getContactNumber()));
                    transportEmailView.setText(
                            String.format("email: %s, (Press to send email)", transportOrganisation.getEmailAddress()));
                    backgroundInformationView.setText(transportOrganisation.getAdditionalInformation());
                    editDocument.setTransportOrganisationID(transportOrganisation.getListItemID());
                    editDocument.setTransportOrganisation(transportOrganisation);
                } else {
                    transportContactNumberView.setText("");
                    transportEmailView.setText("");
                    backgroundInformationView.setText("");
                    editDocument.setTransportOrganisation(null);
                    editDocument.setTransportOrganisationID(null);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                backgroundInformationView.setText("");
            }
        });
        // Outbound Required
        outboundRequired.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    // Display the other outbound fields
                    outboundUsed.setVisibility(View.VISIBLE);
                    outboundDateTimeLayout.setVisibility(View.VISIBLE);
                } else {
                    // Hide the other outbound fields
                    outboundUsed.setVisibility(View.GONE);
                    outboundDateTimeLayout.setVisibility(View.GONE);
                }
            }
        });

        outboundDateView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                outboundDatePicker();
                return true;
            }
        });

        outboundTimeView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                outboundTimePicker();
                return true;
            }
        });

        // Return Required
        returnRequired.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    // Display the other outbound fields
                    returnUsed.setVisibility(View.VISIBLE);
                    returnDateTimeLayout.setVisibility(View.VISIBLE);
                } else {
                    // Hide the other outbound fields
                    returnUsed.setVisibility(View.GONE);
                    returnDateTimeLayout.setVisibility(View.GONE);
                }
            }
        });

        returnDateView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                returnDatePicker();
                return true;
            }
        });

        returnTimeView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                returnTimePicker();
                return true;
            }
        });

        fromAddressView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (client != null) {
                    fromAddressView.setText(client.getAddress());
                    fromAddressView.setSelection(client.getAddress().length());
                    fromPostcodeView.setText(client.getPostcode());
                }
                return true;
            }
        });

        toAddressView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (client != null) {
                    toAddressView.setText(client.getAddress());
                    toAddressView.setSelection(client.getAddress().length());
                    toPostcodeView.setText(client.getPostcode());
                }
                return true;
            }
        });

        // Cancel Button
        Button cancelButton = (Button) parent.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Cancel so no need to update list of documents
                ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.popBackStack();
            }
        });
        // Save Button
        Button saveButton = (Button) parent.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {
                    editDocument.save(isNewMode);
                        FragmentManager fragmentManager = getFragmentManager();
                        fragmentManager.popBackStack();
                }
            }
        });

        // Preset the editable fields (If called from Session Register, lots of information is already available)
        transportSpinner.setSelection(transportPickList.getPosition(editDocument.getTransportOrganisation()));
        transportBooked.setChecked(editDocument.isBooked());
        fromAddressView.setText(editDocument.getFromAddress());
        fromPostcodeView.setText(editDocument.getFromPostcode());
        toAddressView.setText(editDocument.getToAddress());
        toPostcodeView.setText(editDocument.getToPostcode());
        if (editDocument.getOutboundDate().getTime() != Long.MIN_VALUE) {
            outboundDateView.setText(sDate.format(editDocument.getOutboundDate()));
            outboundTimeView.setText(sTime.format(editDocument.getOutboundDate()));
        }
        if (editDocument.getReturnDate().getTime() != Long.MIN_VALUE) {
            returnDateView.setText(sDate.format(editDocument.getReturnDate()));
            returnTimeView.setText(sTime.format(editDocument.getReturnDate()));
        }
        additionalInformationView.setText(editDocument.getAdditionalInformation());
        // Outbound Required
        outboundRequired.setChecked(editDocument.isRequiredOutbound());
        outboundUsed.setChecked(editDocument.isUsedOutbound());
        if (outboundRequired.isChecked()) {
            // Display the other outbound fields
            outboundUsed.setVisibility(View.VISIBLE);
            outboundDateTimeLayout.setVisibility(View.VISIBLE);
        } else {
            // Hide the other outbound fields
            outboundUsed.setVisibility(View.GONE);
            outboundDateTimeLayout.setVisibility(View.GONE);
        }
        // Return Required
        returnRequired.setChecked(editDocument.isRequiredReturn());
        returnUsed.setChecked(editDocument.isUsedReturn());
        if (returnRequired.isChecked()) {
            // Display the other outbound fields
            returnUsed.setVisibility(View.VISIBLE);
            returnDateTimeLayout.setVisibility(View.VISIBLE);
        } else {
            // Hide the other outbound fields
            returnUsed.setVisibility(View.GONE);
            returnDateTimeLayout.setVisibility(View.GONE);
        }


        // Add email Intent
        transportEmailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TransportOrganisation transportOrganisation = editDocument.getTransportOrganisation();
                if (transportOrganisation != null && !transportOrganisation.getEmailAddress().isEmpty()) {
                    String[] addresses = new String[1];
                    addresses[0] = transportOrganisation.getEmailAddress();
                    composeEmail(addresses);
                }

            }
        });

        // Add Contact Number Intent
        transportContactNumberView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TransportOrganisation transportOrganisation = editDocument.getTransportOrganisation();
                if (transportOrganisation != null && !transportOrganisation.getContactNumber().isEmpty()) {
                    dialPhoneNumber(transportOrganisation.getContactNumber());
                }
            }
        });
    }

    private void composeEmail(String[] addresses) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, "CRIS");
        intent.putExtra(Intent.EXTRA_TEXT, String.format("%s\n\n%s", client.shortTextSummary(), editDocument.textSummary()));
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void dialPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
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
        savedInstanceState.putBoolean(TO_HINT_DISPLAYED, toHintTextDisplayed);
        savedInstanceState.putBoolean(FROM_HINT_DISPLAYED, toHintTextDisplayed);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private void toggleToHint() {
        if (toHintTextDisplayed) {
            toHintTextView.setMaxLines(2);
            toHintTextDisplayed = false;
        } else {
            toHintTextDisplayed = true;
            toHintTextView.setMaxLines(toHintTextView.getLineCount());
        }
    }

    private void toggleFromHint() {
        if (fromHintTextDisplayed) {
            fromHintTextView.setMaxLines(2);
            fromHintTextDisplayed = false;
        } else {
            fromHintTextDisplayed = true;
            fromHintTextView.setMaxLines(fromHintTextView.getLineCount());
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
                                editDocument.setCancelledByID(currentUser.getUserID());
                                editDocument.setCancelledFlag(true);
                                if (validate()) {
                                    editDocument.save(isNewMode);
                                        FragmentManager fragmentManager = getFragmentManager();
                                        fragmentManager.popBackStack();
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
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.popBackStack();
            }
        }
    }

    private void outboundDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog startDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                outboundDateView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        startDatePickerDialog.show();
    }

    private void outboundTimePicker() {
        Calendar newCalendar = Calendar.getInstance();
        TimePickerDialog outboundTimePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                Calendar newTime = Calendar.getInstance();
                newTime.set(Calendar.HOUR_OF_DAY, hour);
                newTime.set(Calendar.MINUTE, minute);
                outboundTimeView.setText(sTime.format(newTime.getTime()));
            }
        }, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), true);
        outboundTimePickerDialog.show();
    }

    private void returnDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog endDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                returnDateView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        endDatePickerDialog.show();
    }

    private void returnTimePicker() {
        Calendar newCalendar = Calendar.getInstance();
        TimePickerDialog returnTimePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                Calendar newTime = Calendar.getInstance();
                newTime.set(Calendar.HOUR_OF_DAY, hour);
                newTime.set(Calendar.MINUTE, minute);
                returnTimeView.setText(sTime.format(newTime.getTime()));
            }
        }, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), true);
        returnTimePickerDialog.show();
    }

    // Validate the document
    private boolean validate() {
        boolean success = true;

        // Clear any existing errors
        fromAddressView.setError(null);
        fromPostcodeView.setError(null);
        toAddressView.setError(null);
        toPostcodeView.setError(null);
        outboundDateView.setError(null);
        returnDateView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        //Transport Organisation

        ListItem newTransport = transportPickList.getListItems().get(transportSpinner.getSelectedItemPosition());
        // Test for Please select
        if (newTransport.getItemOrder() == -1) {
            TextView errorText = (TextView) transportSpinner.getSelectedView();
            errorText.setError("anything here, just to add the icon");
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = transportSpinner;
            success = false;
        }

        // Booked
        editDocument.setBooked(transportBooked.isChecked());

        // Consistency Check
        if (!outboundRequired.isChecked() && !returnRequired.isChecked()) {
            outboundRequired.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));
            returnRequired.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));
            focusView = outboundRequired;
        } else {

            // Outbound Required
            editDocument.setRequiredOutbound(outboundRequired.isChecked());
            if (outboundRequired.isChecked()) {
                editDocument.setUsedOutbound(outboundUsed.isChecked());
                // OutboundDate
                String sOutboundDate = outboundDateView.getText().toString();
                String sOutboundTime = outboundTimeView.getText().toString();
                if (TextUtils.isEmpty(sOutboundDate)) {
                    outboundDateView.setError(getString(R.string.error_field_required));
                    focusView = outboundDateView;
                    success = false;
                } else if (TextUtils.isEmpty(sOutboundTime)) {
                    outboundTimeView.setError(getString(R.string.error_field_required));
                    focusView = outboundTimeView;
                    success = false;
                } else {
                    try {
                        Date dOutboundDate = CRISUtil.parseDateTime(sOutboundDate, sOutboundTime);
                        editDocument.setOutboundDate(dOutboundDate);
                    } catch (CRISParseDateException ex) {
                        outboundDateView.setError(ex.getMessage());
                        focusView = outboundDateView;
                        success = false;
                    } catch (CRISParseTimeException ex) {
                        outboundTimeView.setError(ex.getMessage());
                        focusView = outboundTimeView;
                        success = false;
                    }
                }
            } else {
                editDocument.setOutboundDate(new Date(Long.MIN_VALUE));
                editDocument.setUsedOutbound(false);
            }

            // Return Required
            editDocument.setRequiredReturn(returnRequired.isChecked());
            if (returnRequired.isChecked()) {
                editDocument.setUsedReturn(returnUsed.isChecked());
                // ReturnDate
                String sReturnDate = returnDateView.getText().toString();
                String sReturnTime = returnTimeView.getText().toString();
                if (TextUtils.isEmpty(sReturnDate)) {
                    returnDateView.setError(getString(R.string.error_field_required));
                    focusView = returnDateView;
                    success = false;
                } else {
                    try {
                        Date dReturnDate = CRISUtil.parseDateTime(sReturnDate, sReturnTime);
                        editDocument.setReturnDate(dReturnDate);
                    } catch (CRISParseDateException ex) {
                        returnDateView.setError(ex.getMessage());
                        focusView = returnDateView;
                        success = false;
                    } catch (CRISParseTimeException ex) {
                        returnTimeView.setError(ex.getMessage());
                        focusView = returnTimeView;
                        success = false;
                    }
                }
            } else {
                editDocument.setReturnDate(new Date(Long.MIN_VALUE));
                editDocument.setUsedReturn(false);
            }

        }

        // FromAddress
        String sFromAddress = fromAddressView.getText().toString().trim();
        if (TextUtils.isEmpty(sFromAddress)) {
            fromAddressView.setError(getString(R.string.error_field_required));
            focusView = fromAddressView;
            success = false;
            editDocument.setFromAddress("");

        } else {
            editDocument.setFromAddress(sFromAddress);
        }

        // FromPostcode
        String sFromPostcode = fromPostcodeView.getText().toString().trim();
        if (TextUtils.isEmpty(sFromPostcode)) {
            fromPostcodeView.setError(getString(R.string.error_field_required));
            focusView = fromPostcodeView;
            success = false;
            editDocument.setFromPostcode("");
        } else {
            editDocument.setFromPostcode(sFromPostcode);

        }

        // ToAddress
        String sToAddress = toAddressView.getText().toString().trim();
        if (TextUtils.isEmpty(sToAddress)) {
            toAddressView.setError(getString(R.string.error_field_required));
            focusView = toAddressView;
            success = false;
            editDocument.setToAddress("");

        } else {
            editDocument.setToAddress(sToAddress);
        }


        // ToPostcode
        String sToPostcode = toPostcodeView.getText().toString().trim();
        if (TextUtils.isEmpty(sToPostcode)) {
            toPostcodeView.setError(getString(R.string.error_field_required));
            focusView = toPostcodeView;
            success = false;
            editDocument.setToPostcode("");
        } else {
            editDocument.setToPostcode(sToPostcode);

        }

        // StartDate/EndDate consistency check
        if (editDocument.getOutboundDate().getTime() != Long.MIN_VALUE &&
                editDocument.getReturnDate().getTime() != Long.MIN_VALUE) {
            if (editDocument.getOutboundDate() != null && editDocument.getReturnDate() != null) {
                if (!editDocument.getOutboundDate().before(editDocument.getReturnDate())) {
                    returnDateView.setError(getString(R.string.error_invalid_transport_date_order));
                    focusView = returnDateView;
                    success = false;
                }
            }
        }

        // AdditionalInformation
        editDocument.setAdditionalInformation(additionalInformationView.getText().toString().trim());

        if (!success) {
            focusView.requestFocus();
            focusView.requestFocusFromTouch();
        }
        return success;
    }



}

