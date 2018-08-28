package solutions.cris.read;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
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
import java.util.Date;
import java.util.Locale;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.list.ListClientHeader;
import solutions.cris.list.ListSessionClients;
import solutions.cris.object.Agency;
import solutions.cris.object.Client;
import solutions.cris.object.Contact;
import solutions.cris.object.MyWeek;
import solutions.cris.object.School;
import solutions.cris.object.Transport;
import solutions.cris.object.TransportOrganisation;
import solutions.cris.object.User;

public class ReadTransport extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);
    private static final SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);

    private Transport editDocument;
    private View parent;
    private Client client;

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
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        client = ((ListActivity) getActivity()).getClient();
        editDocument = (Transport) ((ListActivity) getActivity()).getDocument();
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        // Swipe to be added to read in ListDocumentsFragment when adapter moved
        // do ListActivity. Until then, don't show message in footer
        footer.setText("");
        //footer.setText(R.string.action_swipe_left_for_unread);

        // Hide the FAB
        fab.setVisibility(View.GONE);

        toolbar.setTitle(getString(R.string.app_name) + " - Transport");

        LocalDB localDB = LocalDB.getInstance();

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

        Spinner transportSpinner = (Spinner) parent.findViewById(R.id.transport_spinner);
        TextView transportTextView = (TextView) parent.findViewById(R.id.transport_read_text);
        final TextView transportContactNumberView = (TextView) parent.findViewById(R.id.transport_contact_number);
        final TextView transportEmailView = (TextView) parent.findViewById(R.id.transport_email);
        TextView backgroundInformationView = (TextView) parent.findViewById(R.id.background_information);
        CheckBox transportBooked = (CheckBox) parent.findViewById(R.id.booked);
        CheckBox outboundRequired = (CheckBox) parent.findViewById(R.id.outbound_required);
        TextView outboundTextView = (TextView) parent.findViewById(R.id.outbound_text);
        LinearLayout outboundDateView = (LinearLayout) parent.findViewById(R.id.outbound_datetime_layout);
        CheckBox outboundUsed = (CheckBox) parent.findViewById(R.id.outbound_used);
        CheckBox returnRequired = (CheckBox) parent.findViewById(R.id.return_required);
        TextView returnTextView = (TextView) parent.findViewById(R.id.return_text);
        LinearLayout returnDateView = (LinearLayout) parent.findViewById(R.id.return_datetime_layout);
        CheckBox returnUsed = (CheckBox) parent.findViewById(R.id.return_used);
        EditText fromAddressView = (EditText) parent.findViewById(R.id.from_address);
        EditText fromPostcodeView = (EditText) parent.findViewById(R.id.from_postcode);
        EditText toAddressView = (EditText) parent.findViewById(R.id.to_address);
        EditText toPostcodeView = (EditText) parent.findViewById(R.id.to_postcode);
        EditText additionalInformationView = (EditText) parent.findViewById(R.id.additional_information);
        LinearLayout toHint = (LinearLayout) parent.findViewById(R.id.to_hint);
        LinearLayout fromHint = (LinearLayout) parent.findViewById(R.id.from_hint);
        Button cancelButton = (Button) parent.findViewById(R.id.cancel_button);
        Button saveButton = (Button) parent.findViewById(R.id.save_button);

        transportSpinner.setVisibility(View.GONE);
        transportTextView.setVisibility(View.VISIBLE);
        transportTextView.setFocusable(false);
        transportBooked.setVisibility(View.GONE);
        outboundRequired.setVisibility(View.GONE);
        outboundUsed.setVisibility(View.GONE);
        outboundTextView.setVisibility(View.VISIBLE);
        outboundTextView.setFocusable(false);

        returnRequired.setVisibility(View.GONE);
        returnUsed.setVisibility(View.GONE);
        returnTextView.setVisibility(View.VISIBLE);
        returnTextView.setFocusable(false);

        fromHint.setVisibility(View.GONE);
        fromAddressView.setInputType(InputType.TYPE_NULL);
        fromAddressView.setSingleLine(false);
        fromAddressView.setFocusable(false);
        fromPostcodeView.setInputType(InputType.TYPE_NULL);
        fromPostcodeView.setFocusable(false);
        toHint.setVisibility(View.GONE);
        toAddressView.setInputType(InputType.TYPE_NULL);
        toAddressView.setSingleLine(false);
        toAddressView.setFocusable(false);
        toPostcodeView.setInputType(InputType.TYPE_NULL);
        toPostcodeView.setFocusable(false);


        outboundDateView.setVisibility(View.GONE);
        returnDateView.setVisibility(View.GONE);
        additionalInformationView.setInputType(InputType.TYPE_NULL);
        additionalInformationView.setSingleLine(false);
        additionalInformationView.setFocusable(false);

        cancelButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);

        String transportText = editDocument.getTransportOrganisation().getItemValue();
        if (editDocument.isBooked()) {
            transportText += " - Booked";
        } else {
            transportText += " - NOT YET BOOKED";
        }
        transportTextView.setText(transportText);
        TransportOrganisation transportOrganisation =editDocument.getTransportOrganisation();
        if (transportOrganisation != null) {
            transportContactNumberView.setText(
                    String.format("tel: %s", transportOrganisation.getContactNumber()));
            transportEmailView.setText(
                    String.format("email: %s, (Press to send email)", transportOrganisation.getEmailAddress()));
            backgroundInformationView.setText(transportOrganisation.getAdditionalInformation());
        } else {
            transportContactNumberView.setText("");
            transportEmailView.setText("");
            backgroundInformationView.setText("");
        }

        if (editDocument.isRequiredOutbound()) {
            String outboundText = "";
            if (editDocument.getOutboundDate().getTime() != Long.MIN_VALUE) {
                outboundText = sDateTime.format(editDocument.getOutboundDate());
            } else {
                outboundText = "Date and time not known.";
            }
            if (editDocument.getOutboundDate().before(new Date())) {
                if (editDocument.isUsedOutbound()) {
                    outboundText += (" - Transport Used");
                } else {
                    outboundText += (" - TRANSPORT NOT USED");
                }
            }
            outboundTextView.setText(outboundText);
        } else {
            outboundTextView.setText("Not Required");
        }

        if (editDocument.isRequiredReturn()) {
            String returnText = "";
            if (editDocument.getReturnDate().getTime() != Long.MIN_VALUE) {
                returnText = sDateTime.format(editDocument.getReturnDate());
            } else {
                returnText = "Date and time not known.";
            }
            if (editDocument.getReturnDate().before(new Date())) {
                if (editDocument.isUsedReturn()) {
                    returnText += (" - Transport Used");
                } else {
                    returnText += (" - TRANSPORT NOT USED");
                }
            }
            returnTextView.setText(returnText);
        } else {
            returnTextView.setText("Not Required");
        }

        fromAddressView.setText(editDocument.getFromAddress());
        fromPostcodeView.setText(editDocument.getFromPostcode());
        toAddressView.setText(editDocument.getToAddress());
        toPostcodeView.setText(editDocument.getToPostcode());
        additionalInformationView.setText(editDocument.getAdditionalInformation());

        // Add email Intent
        transportEmailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TransportOrganisation transportOrganisation = editDocument.getTransportOrganisation();
                String[] addresses = new String[1];
                addresses[0] = transportOrganisation.getEmailAddress();
                composeEmail(addresses);

            }
        });

        // Add Contact Number Intent
        transportContactNumberView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TransportOrganisation transportOrganisation = editDocument.getTransportOrganisation();
                dialPhoneNumber(transportOrganisation.getEmailAddress());
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
        String summary = String.format("%s\n\n%s", client.shortTextSummary(), editDocument.textSummary());
        shareIntent.putExtra(Intent.EXTRA_TEXT, summary);
        shareActionProvider.setShareIntent(shareIntent);
    }

}

