package solutions.cris.read;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import solutions.cris.R;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Client;

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

public class ReadClient extends Fragment {
    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private Client editClient;
    private View parent;

    EditText emailView;
    EditText contactNumberView;
    EditText contactNumber2View;

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

        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        // Hide the FAB (if ListClientHeader)
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
        editClient = (Client) ((ListActivity) getActivity()).getDocument();
        // Swipe to be added to read in ListDocumentsFragment when adapter moved
        // do ListActivity. Until then, don't show message in footer
        footer.setText("");
        //footer.setText(R.string.action_swipe_left_for_unread);
        toolbar.setTitle(getString(R.string.app_name) + " - Client");

        // Set up the form.
        EditText firstNamesView = (EditText) parent.findViewById(R.id.first_names);
        EditText lastNameView = (EditText) parent.findViewById(R.id.last_name);
        EditText dateOfBirthView = (EditText) parent.findViewById(R.id.date_of_birth);
        EditText addressView = (EditText) parent.findViewById(R.id.address);
        EditText postcodeView = (EditText) parent.findViewById(R.id.postcode);
        contactNumberView = (EditText) parent.findViewById(R.id.contact_number);
        contactNumber2View = (EditText) parent.findViewById(R.id.contact_number2);
        emailView = (EditText) parent.findViewById(R.id.email);
        Spinner genderView = (Spinner) parent.findViewById(R.id.gender_spinner);
        TextView genderTextView = (TextView) parent.findViewById(R.id.gender_read_text);
        Spinner ethnicityView = (Spinner) parent.findViewById(R.id.ethnicity_spinner);
        TextView ethnicityTextView = (TextView) parent.findViewById(R.id.ethnicity_read_text);
        Button cancelButton = (Button) parent.findViewById(R.id.cancel_button);
        Button saveButton = (Button) parent.findViewById(R.id.save_button);

        // Set the fields to non-editable
        firstNamesView.setInputType(InputType.TYPE_NULL);
        firstNamesView.setFocusable(false);
        lastNameView.setInputType(InputType.TYPE_NULL);
        lastNameView.setFocusable(false);
        dateOfBirthView.setInputType(InputType.TYPE_NULL);
        dateOfBirthView.setFocusable(false);
        addressView.setInputType(InputType.TYPE_NULL);
        addressView.setMaxLines(4);
        addressView.setSingleLine(false);
        addressView.setFocusable(false);
        postcodeView.setInputType(InputType.TYPE_NULL);
        postcodeView.setFocusable(false);
        contactNumberView.setInputType(InputType.TYPE_NULL);
        contactNumberView.setFocusable(false);
        contactNumber2View.setInputType(InputType.TYPE_NULL);
        contactNumber2View.setFocusable(false);
        emailView.setInputType(InputType.TYPE_NULL);
        emailView.setFocusable(false);
        genderView.setVisibility(View.GONE);
        genderTextView.setVisibility(View.VISIBLE);
        genderTextView.setFocusable(false);
        ethnicityView.setVisibility(View.GONE);
        ethnicityTextView.setVisibility(View.VISIBLE);
        ethnicityTextView.setFocusable(false);
        cancelButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);

        // Load initial values
        firstNamesView.setText(editClient.getFirstNames(), null);
        lastNameView.setText(editClient.getLastName(), null);
        Date dateOfBirth = editClient.getDateOfBirth();
        dateOfBirthView.setText(sDate.format(dateOfBirth.getTime()));
        addressView.setText(editClient.getAddress(), null);
        postcodeView.setText(editClient.getPostcode(), null);
        contactNumberView.setText(editClient.getContactNumber());
        if (editClient.getContactNumber2() != null && !editClient.getContactNumber2().isEmpty()){
            contactNumber2View.setText(editClient.getContactNumber2());
        }
        emailView.setText(editClient.getEmailAddress(), null);
        genderTextView.setText(editClient.getGender().getItemValue());
        ethnicityTextView.setText(editClient.getEthnicity().getItemValue());

        // Add email Intent
        emailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] addresses = new String[1];
                addresses[0] = emailView.getText().toString();
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

        // Add Contact Number Intent
        contactNumber2View.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialPhoneNumber(contactNumber2View.getText().toString());
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
        shareIntent.putExtra(Intent.EXTRA_TEXT, editClient.textSummary());
        shareActionProvider.setShareIntent(shareIntent);
    }

}
