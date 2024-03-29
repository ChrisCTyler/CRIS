package solutions.cris.read;
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
// Build 200 Use the androidX Fragment class
//import android.app.Fragment;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Agency;
import solutions.cris.object.Client;
import solutions.cris.object.Contact;
import solutions.cris.object.School;
import solutions.cris.object.User;

public class ReadContact extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private Contact editDocument;
    private View parent;
    private Client client;

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


        TextView footer = getActivity().findViewById(R.id.footer);
        // Hide the FAB
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
        client = ((ListActivity) getActivity()).getClient();
        editDocument = (Contact) ((ListActivity) getActivity()).getDocument();
        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        // Swipe to be added to read in ListDocumentsFragment when adapter moved
        // do ListActivity. Until then, don't show message in footer
        footer.setText("");
        //footer.setText(R.string.action_swipe_left_for_unread);
        toolbar.setTitle(getString(R.string.app_name) + " - Contact");

        LocalDB localDB = LocalDB.getInstance();

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
            cancelReason.setText(String.format("Reason: %s",editDocument.getCancellationReason()));
        }

        Spinner contactTypeSpinner = parent.findViewById(R.id.contact_type_spinner);
        TextView contactTypeTextView = parent.findViewById(R.id.contact_type_read_text);
        Spinner agencySpinner = parent.findViewById(R.id.agency_spinner);
        TextView agencyTextView = parent.findViewById(R.id.agency_read_text);
        LinearLayout agencyLayout = parent.findViewById(R.id.agency_layout);
        Spinner schoolSpinner = parent.findViewById(R.id.school_spinner);
        TextView schoolTextView = parent.findViewById(R.id.school_read_text);
        LinearLayout schoolLayout = parent.findViewById(R.id.school_layout);
        TextView backgroundInformationView = parent.findViewById(R.id.background_information);
        LinearLayout backgroundLayout = parent.findViewById(R.id.background_layout);
        LinearLayout addressLayout = parent.findViewById(R.id.address_layout);
        EditText nameView = parent.findViewById(R.id.name);
        EditText addressView = parent.findViewById(R.id.address);
        EditText postcodeView = parent.findViewById(R.id.postcode);
        final EditText contactNumberView = parent.findViewById(R.id.contact_number);
        final EditText emailAddressView = parent.findViewById(R.id.email_address);
        EditText startDateView = parent.findViewById(R.id.start_date);
        EditText endDateView = parent.findViewById(R.id.end_date);
        EditText additionalInformationView = parent.findViewById(R.id.additional_information);
        Spinner relationshipSpinner = parent.findViewById(R.id.relationship_spinner);
        TextView relationshipTextView = parent.findViewById(R.id.relationship_read_text);
        // Build 181 FSM
        LinearLayout freeSchoolMealsLayoutView = parent.findViewById(R.id.free_school_meals_layout);
        TextView freeSchoolMealsTextView = parent.findViewById(R.id.free_school_meals_text);
        CheckBox freeSchoolMeals = parent.findViewById(R.id.free_school_meals);
        // Build 183 - SEND
        LinearLayout specialNeedsLayoutView = parent.findViewById(R.id.special_needs_layout);
        TextView specialNeedsTextView = parent.findViewById(R.id.special_needs_text);
        CheckBox specialNeeds = parent.findViewById(R.id.special_needs);

        TextView hintTextView = parent.findViewById(R.id.hint_text);
        ImageView hintIconView = parent.findViewById(R.id.hint_open_close);
        Button cancelButton = parent.findViewById(R.id.cancel_button);
        Button saveButton = parent.findViewById(R.id.save_button);

        contactTypeSpinner.setVisibility(View.GONE);
        contactTypeTextView.setVisibility(View.VISIBLE);
        contactTypeTextView.setFocusable(false);

        agencySpinner.setVisibility(View.GONE);
        agencyTextView.setVisibility(View.VISIBLE);
        agencyTextView.setFocusable(false);
        schoolSpinner.setVisibility(View.GONE);
        schoolTextView.setVisibility(View.VISIBLE);
        schoolTextView.setFocusable(false);

        // Build 181
        freeSchoolMealsLayoutView.setVisibility(View.GONE);
        freeSchoolMeals.setVisibility(View.GONE);
        // Build 233
        freeSchoolMealsTextView.setVisibility(View.VISIBLE);
        freeSchoolMealsTextView.setFocusable(false);
        // Build 183
        specialNeedsLayoutView.setVisibility(View.GONE);
        specialNeeds.setVisibility(View.GONE);
        specialNeedsTextView.setVisibility(View.VISIBLE);
        specialNeedsTextView.setFocusable(false);

        nameView.setInputType(InputType.TYPE_NULL);
        nameView.setFocusable(false);
        addressView.setInputType(InputType.TYPE_NULL);
        //addressView.setMaxLines(4);
        addressView.setSingleLine(false);
        addressView.setFocusable(false);
        postcodeView.setInputType(InputType.TYPE_NULL);
        postcodeView.setFocusable(false);
        contactNumberView.setInputType(InputType.TYPE_NULL);
        contactNumberView.setFocusable(false);
        emailAddressView.setInputType(InputType.TYPE_NULL);
        emailAddressView.setFocusable(false);
        startDateView.setInputType(InputType.TYPE_NULL);
        startDateView.setFocusable(false);
        endDateView.setInputType(InputType.TYPE_NULL);
        endDateView.setFocusable(false);
        additionalInformationView.setInputType(InputType.TYPE_NULL);
        //additionalInformationView.setMaxLines(4);
        additionalInformationView.setSingleLine(false);
        additionalInformationView.setFocusable(false);
        relationshipSpinner.setVisibility(View.GONE);
        relationshipTextView.setVisibility(View.VISIBLE);
        relationshipTextView.setFocusable(false);
        hintTextView.setVisibility(View.GONE);
        hintIconView.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);

        contactTypeTextView.setText(editDocument.getContactType().getItemValue());
        switch (editDocument.getContactType().getItemValue()) {
            case "Agency Contact":
                schoolLayout.setVisibility(View.GONE);
                agencyLayout.setVisibility(View.VISIBLE);
                backgroundLayout.setVisibility(View.VISIBLE);
                Agency agency = (Agency) localDB.getListItem(editDocument.getAgencyID());
                if (agency != null) {
                    backgroundInformationView.setText(agency.textSummary());
                } else {
                    backgroundInformationView.setText("");
                }
                addressLayout.setVisibility(View.GONE);
                agencyTextView.setText(editDocument.getAgency().getItemValue());
                break;
            case "School Contact":
                schoolLayout.setVisibility(View.VISIBLE);
                agencyLayout.setVisibility(View.GONE);
                backgroundLayout.setVisibility(View.VISIBLE);
                School school = (School) localDB.getListItem(editDocument.getSchoolID());
                if (school != null) {
                    backgroundInformationView.setText(school.textSummary());
                } else {
                    backgroundInformationView.setText("");
                }
                addressLayout.setVisibility(View.GONE);
                schoolTextView.setText(editDocument.getSchool().getItemValue());
                // Build 181
                // Build 233 FSM Moved to Case Document
                /*
                freeSchoolMealsLayoutView.setVisibility(View.VISIBLE);
                freeSchoolMealsTextView.setVisibility(View.VISIBLE);
                if (editDocument.isFreeSchoolMeals()) {
                    freeSchoolMealsTextView.setText("Yes");
                } else {
                    freeSchoolMealsTextView.setText("No");
                }
                 */
                // Build 183
                specialNeedsLayoutView.setVisibility(View.VISIBLE);
                specialNeedsTextView.setVisibility(View.VISIBLE);
                if (editDocument.isSpecialNeeds()) {
                    specialNeedsTextView.setText("Yes");
                } else {
                    specialNeedsTextView.setText("No");
                }
                break;
            default:
                schoolLayout.setVisibility(View.GONE);
                // Build 181
                freeSchoolMealsLayoutView.setVisibility(View.GONE);
                // Build 183
                specialNeedsLayoutView.setVisibility(View.GONE);
                agencyLayout.setVisibility(View.GONE);
                backgroundLayout.setVisibility(View.GONE);
                addressLayout.setVisibility(View.VISIBLE);
                addressView.setText(editDocument.getContactAddress());
                postcodeView.setText(editDocument.getContactPostcode());
        }
        nameView.setText(editDocument.getContactName());
        contactNumberView.setText(editDocument.getContactContactNumber());
        emailAddressView.setText(editDocument.getContactEmailAddress());
        startDateView.setText(sDate.format(editDocument.getStartDate()));
        if (editDocument.getEndDate().getTime() != Long.MIN_VALUE) {
            endDateView.setText(sDate.format(editDocument.getEndDate()));
        }
        additionalInformationView.setText(editDocument.getContactAdditionalInformation());
        relationshipTextView.setText(editDocument.getRelationshipType().getItemValue());

        // Add email Intent
        emailAddressView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] addresses = new String[1];
                addresses[0] = emailAddressView.getText().toString();
                composeEmail(addresses);

            }
        });

        // Add Contact Number Intent
        contactNumberView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialPhoneNumber(contactNumberView.getText().toString());
            }
        });
    }

    private void composeEmail(String[] addresses){
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, "CRIS");
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
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //SHARE
        MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        createShareActionProvider(shareOption);
    }

    // SHARE MENU ITEM (Both methods are required)
    private void createShareActionProvider(MenuItem menuItem) {
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String summary = String.format("%s\n\n%s", client.textSummary(), editDocument.textSummary());
        shareIntent.putExtra(Intent.EXTRA_TEXT, summary);
        shareActionProvider.setShareIntent(shareIntent);
    }

}

