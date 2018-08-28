package solutions.cris.read;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
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
import java.util.Locale;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.list.ListClientHeader;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.User;
import solutions.cris.utils.LocalSettings;

/**
 * Copyright CRIS.Solutions 16/12/2016.
 */

public class ReadCase extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private Case editDocument;
    private View parent;
    private Client client;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.edit_case, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Hide the FAB
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        client = ((ListActivity) getActivity()).getClient();
        editDocument = (Case) ((ListActivity) getActivity()).getDocument();
        // Swipe to be added to read in ListDocumentsFragment when adapter moved
        // do ListActivity. Until then, don't show message in footer
        footer.setText("");
        //footer.setText(R.string.action_swipe_left_for_unread);
        toolbar.setTitle(getString(R.string.app_name) + " - Case");

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
            cancelReason.setText(String.format("Reason: %s",editDocument.getCancellationReason()));
        }

        Spinner caseTypeSpinner = (Spinner) parent.findViewById(R.id.case_type_spinner);
        TextView caseTypeTextView = (TextView) parent.findViewById(R.id.case_type_read_text);
        EditText referenceDateView = (EditText) parent.findViewById(R.id.reference_date);
        ImageView clientRedIcon = (ImageView) parent.findViewById(R.id.client_red_icon);
        ImageView clientAmberIcon = (ImageView) parent.findViewById(R.id.client_amber_icon);
        ImageView clientGreenIcon = (ImageView) parent.findViewById(R.id.client_green_icon);
        EditText caseSummaryView = (EditText) parent.findViewById(R.id.case_summary);
        EditText overdueThresholdView = (EditText) parent.findViewById(R.id.overdue_threshold);
        Spinner tierSpinner = (Spinner) parent.findViewById(R.id.tier_spinner);
        TextView tierTextView = (TextView) parent.findViewById(R.id.tier_read_text);
        Spinner groupSpinner = (Spinner) parent.findViewById(R.id.group_spinner);
        TextView groupTextView = (TextView) parent.findViewById(R.id.group_read_text);
        Spinner keyworkerSpinner = (Spinner) parent.findViewById(R.id.keyworker_spinner);
        TextView keyworkerTextView = (TextView) parent.findViewById(R.id.keyworker_read_text);
        Spinner coworker1Spinner = (Spinner) parent.findViewById(R.id.coworker1_spinner);
        TextView coworker1TextView = (TextView) parent.findViewById(R.id.coworker1_read_text);
        Spinner coworker2Spinner = (Spinner) parent.findViewById(R.id.coworker2_spinner);
        TextView coworker2TextView = (TextView) parent.findViewById(R.id.coworker2_read_text);
        Spinner commissionerSpinner = (Spinner) parent.findViewById(R.id.comissioner_spinner);
        TextView commissionerTextView = (TextView) parent.findViewById(R.id.comissioner_read_text);
        Spinner transportRequiredSpinner = (Spinner) parent.findViewById(R.id.transport_required_spinner);
        TextView transportRequiredTextView = (TextView) parent.findViewById(R.id.transport_required_read_text);
        EditText specialInstructionsView = (EditText) parent.findViewById(R.id.specialInstructions);
        TextView hintTextView = (TextView) parent.findViewById(R.id.hint_text);
        ImageView hintIconView = (ImageView) parent.findViewById(R.id.hint_open_close);
        Button cancelButton = (Button) parent.findViewById(R.id.cancel_button);
        Button saveButton = (Button) parent.findViewById(R.id.save_button);
        // Build 105
        CheckBox photographyConsentCheckbox = (CheckBox) parent.findViewById(R.id.photography_consent_flag);
        TextView photographyConsentReadText = (TextView) parent.findViewById(R.id.photography_consent_read_text);
        CheckBox doNotInviteCheckbox = (CheckBox) parent.findViewById(R.id.do_not_invite_flag);
        TextView doNotInviteReadText = (TextView) parent.findViewById(R.id.do_not_invite_read_text);

        caseTypeSpinner.setVisibility(View.GONE);
        caseTypeTextView.setVisibility(View.VISIBLE);
        caseTypeTextView.setFocusable(false);
        referenceDateView.setInputType(InputType.TYPE_NULL);
        referenceDateView.setFocusable(false);
        caseSummaryView.setVisibility(View.VISIBLE);
        caseSummaryView.setFocusable(false);
        photographyConsentCheckbox.setVisibility(View.GONE);
        photographyConsentReadText.setVisibility(View.VISIBLE);
        overdueThresholdView.setInputType(InputType.TYPE_NULL);
        overdueThresholdView.setFocusable(false);
        tierSpinner.setVisibility(View.GONE);
        tierTextView.setVisibility(View.VISIBLE);
        tierTextView.setFocusable(false);
        groupSpinner.setVisibility(View.GONE);
        groupTextView.setVisibility(View.VISIBLE);
        groupTextView.setFocusable(false);
        doNotInviteCheckbox.setVisibility(View.GONE);
        doNotInviteReadText.setVisibility(View.VISIBLE);
        keyworkerSpinner.setVisibility(View.GONE);
        keyworkerTextView.setVisibility(View.VISIBLE);
        keyworkerTextView.setFocusable(false);
        coworker1Spinner.setVisibility(View.GONE);
        coworker1TextView.setVisibility(View.VISIBLE);
        coworker1TextView.setFocusable(false);
        coworker2Spinner.setVisibility(View.GONE);
        coworker2TextView.setVisibility(View.VISIBLE);
        coworker2TextView.setFocusable(false);
        commissionerSpinner.setVisibility(View.GONE);
        commissionerTextView.setVisibility(View.VISIBLE);
        commissionerTextView.setFocusable(false);
        transportRequiredSpinner.setVisibility(View.GONE);
        transportRequiredTextView.setVisibility(View.VISIBLE);
        transportRequiredTextView.setFocusable(false);
        specialInstructionsView.setVisibility(View.VISIBLE);
        specialInstructionsView.setFocusable(false);
        hintTextView.setVisibility(View.GONE);
        hintIconView.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);

        // Set the 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance(getActivity());
        TextView tierLabel = (TextView) parent.findViewById(R.id.tier_label_text);
        tierLabel.setText(localSettings.Tier);
        TextView groupLabel = (TextView) parent.findViewById(R.id.group_label_text);
        groupLabel.setText(localSettings.Group);
        TextView keyworkerLabel = (TextView) parent.findViewById(R.id.keyworker_label_text);
        keyworkerLabel.setText(localSettings.Keyworker);
        TextView coworker1Label = (TextView) parent.findViewById(R.id.coworker1_label_text);
        coworker1Label.setText(localSettings.Coworker1);
        TextView coworker2Label = (TextView) parent.findViewById(R.id.coworker2_label_text);
        coworker2Label.setText(localSettings.Coworker2);
        TextView commissionerLabel = (TextView) parent.findViewById(R.id.commissioner_label_text);
        commissionerLabel.setText(localSettings.Commisioner);


        caseTypeTextView.setText(editDocument.getCaseType());
        referenceDateView.setText(sDate.format(editDocument.getReferenceDate()));
        if (editDocument.getCaseSummary().toString().isEmpty()){
            caseSummaryView.setText(" ");
        } else {
            caseSummaryView.setText(editDocument.getCaseSummary().toString());
        }
        if (editDocument.isPhotographyConsentFlag()) {
            photographyConsentReadText.setText("Photography/Media consent obtained");
        } else {
            photographyConsentReadText.setText("Photography/Media consent NOT OBTAINED");
        }
        overdueThresholdView.setText(String.format(Locale.UK, "%d", editDocument.getOverdueThreshold()));
        if (editDocument.getTier() != null) {
            tierTextView.setText(editDocument.getTier().getItemValue());
        }
        if (editDocument.getGroup() != null) {
            groupTextView.setText(editDocument.getGroup().getItemValue());
        }
        if (editDocument.isDoNotInviteFlag()){
            doNotInviteReadText.setText(" (Do not invite to Sessions)");
        }
        if (editDocument.getKeyWorker() != null) {
            keyworkerTextView.setText(editDocument.getKeyWorker().getFullName());
        }
        if (editDocument.getCoWorker1() != null) {
            coworker1TextView.setText(editDocument.getCoWorker1().getFullName());
        }
        if (editDocument.getCoWorker2() != null) {
            coworker2TextView.setText(editDocument.getCoWorker2().getFullName());
        }
        if (editDocument.getCommissioner() != null) {
            commissionerTextView.setText(editDocument.getCommissioner().getItemValue());
        }
        clientRedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red_grey));
        clientAmberIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber_grey));
        clientGreenIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green_grey));
        switch (editDocument.getClientStatus()) {
            case Case.RED:
                clientRedIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_red));
                break;
            case Case.AMBER:
                clientAmberIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_amber));
                break;
            case Case.GREEN:
                clientGreenIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_client_green));
        }
        transportRequiredTextView.setText(editDocument.getTransportRequired());
        specialInstructionsView.setText(editDocument.getTransportSpecialInstructions());
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

