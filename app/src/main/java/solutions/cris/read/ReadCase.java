package solutions.cris.read;

// Build 200 Use the androidX Fragment class
//import android.app.Fragment;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.content.ContextCompat;
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
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Case;
import solutions.cris.object.Client;
import solutions.cris.object.Document;
import solutions.cris.object.User;
import solutions.cris.utils.LocalSettings;

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

public class ReadCase extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    public static final String PLAN_FINANCE_HINT_DISPLAYED = "solutions.cris.PlanFinanceHintDisplayed";

    private Case editDocument;
    private View parent;
    private Client client;
    private LocalDB localDB;
    // Build 232 Add Plan/FinSupp Hint
    private TextView planFinanceHintTextView;
    private boolean planFinanceHintTextDisplayed = true;
    // Build 232
    Button reviewButton;
    private TextView lastReviewedBy;
    private TextView lastReviewDateView;

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

        localDB = LocalDB.getInstance();

        // Set up the Plan/Finance hint text
        planFinanceHintTextView = getActivity().findViewById(R.id.plan_finance_hint_text);
        planFinanceHintTextView.setText(getPlanFinanceHintText());
        // Restore value of hintDisplayed (Set to opposite, toggle to come
        if (savedInstanceState != null) {
            planFinanceHintTextDisplayed = !savedInstanceState.getBoolean(PLAN_FINANCE_HINT_DISPLAYED);
        }
        togglePlanFinanceHint();
        planFinanceHintTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlanFinanceHint();
            }
        });

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
        // Build 139 - Second Group
        Spinner group2Spinner = (Spinner) parent.findViewById(R.id.group2_spinner);
        TextView group2TextView = (TextView) parent.findViewById(R.id.group2_read_text);
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
        // Build 139 - Second Group
        CheckBox doNotInvite2Checkbox = (CheckBox) parent.findViewById(R.id.do_not_invite2_flag);
        TextView doNotInvite2ReadText = (TextView) parent.findViewById(R.id.do_not_invite2_read_text);
        // Build 232
        TextView planFinanceView = (TextView) parent.findViewById(R.id.plan_finance_text);
        CheckBox pfPupilPremium = (CheckBox) parent.findViewById(R.id.pf_pupil_premium);
        CheckBox pfFreeSchoolMeals = (CheckBox) parent.findViewById(R.id.pf_free_school_meals);
        CheckBox pfChildProtectionPlan = (CheckBox) parent.findViewById(R.id.pf_child_protection_plan);
        CheckBox pfChildInNeedPlan = (CheckBox) parent.findViewById(R.id.pf_child_in_need_plan);
        CheckBox pfTafEarlyHelpPlan = (CheckBox) parent.findViewById(R.id.pf_taf_early_help_plan);
        CheckBox pfSocialServicesRecommendation = (CheckBox) parent.findViewById(R.id.pf_social_services_recommendation);
        CheckBox pfOtherPlanFinancialSupport = (CheckBox) parent.findViewById(R.id.pf_other_plan_financial_support);
        lastReviewedBy = (TextView) parent.findViewById(R.id.last_reviewed_by_read_text);
        lastReviewDateView = (TextView) parent.findViewById(R.id.last_review_date_text);
        reviewButton = (Button) parent.findViewById(R.id.review_button);
        reviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (client.getCurrentCase() == null ||
                        !client.getCurrentCaseID().equals(editDocument.getDocumentID())) {
                    reviewUnavailablePopup();
                } else {
                    reviewPopup();
                }
            }
        });
        if (client.getCurrentCase() == null ||
                !client.getCurrentCaseID().equals(editDocument.getDocumentID())) {
            reviewButton.setTextColor(ContextCompat.getColor(getActivity(), R.color.grey));
        }
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
        // Build 139 - Second Group
        group2Spinner.setVisibility(View.GONE);
        group2TextView.setVisibility(View.VISIBLE);
        group2TextView.setFocusable(false);
        doNotInvite2Checkbox.setVisibility(View.GONE);
        doNotInvite2ReadText.setVisibility(View.VISIBLE);
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
        // Build 232
        planFinanceView.setVisibility(View.VISIBLE);
        pfPupilPremium.setVisibility(View.GONE);
        pfFreeSchoolMeals.setVisibility(View.GONE);
        pfChildProtectionPlan.setVisibility(View.GONE);
        pfChildInNeedPlan.setVisibility(View.GONE);
        pfTafEarlyHelpPlan.setVisibility(View.GONE);
        pfSocialServicesRecommendation.setVisibility(View.GONE);
        pfOtherPlanFinancialSupport.setVisibility(View.GONE);

        // Set the 'local' labels
        LocalSettings localSettings = LocalSettings.getInstance(getActivity());
        TextView tierLabel = (TextView) parent.findViewById(R.id.tier_label_text);
        tierLabel.setText(localSettings.Tier);
        TextView groupLabel = (TextView) parent.findViewById(R.id.group_label_text);
        groupLabel.setText(localSettings.Group);
        // Build 139 - Second Group
        TextView group2Label = (TextView) parent.findViewById(R.id.group2_label_text);
        group2Label.setText(localSettings.Group + "2");
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
        if (editDocument.getCaseSummary().toString().isEmpty()) {
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
        if (editDocument.isDoNotInviteFlag()) {
            doNotInviteReadText.setText(" (Do not invite to Sessions)");
        }
        // Build 139 - Second Group
        if (editDocument.getGroup2() != null) {
            group2TextView.setText(editDocument.getGroup2().getItemValue());
        }
        if (editDocument.isDoNotInvite2Flag()) {
            doNotInvite2ReadText.setText(" (Do not invite to Sessions)");
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
        // Build 232
        String planFinance = "";
        if (editDocument.isPupilPremium())
            planFinance += String.format("%s\n", getString(R.string.prompt_pf_pupil_premium));
        if (editDocument.isFreeSchoolMeals())
            planFinance += String.format("%s\n", getString(R.string.prompt_pf_free_school_meals));
        if (editDocument.isChildProtectionPlan())
            planFinance += String.format("%s\n", getString(R.string.prompt_pf_child_protection_plan));
        if (editDocument.isChildInNeedPlan())
            planFinance += String.format("%s\n", getString(R.string.prompt_pf_child_in_need_plan));
        if (editDocument.isTafEarlyHelpPlan())
            planFinance += String.format("%s\n", getString(R.string.prompt_pf_taf_early_help_plan));
        if (editDocument.isSocialServicesRecommendation())
            planFinance += String.format("%s\n", getString(R.string.prompt_pf_social_services_recommendation));
        if (editDocument.isOtherPlanFinancialSupport())
            planFinance += String.format("%s\n", getString(R.string.prompt_pf_other_plan_financial_support));
        // Either remove the final carriage return or add a 'none' message
        if (planFinance.isEmpty()) planFinance = "No Plans or Financial Support recorded";
        else planFinance = planFinance.substring(0, planFinance.length() - 1);
        planFinanceView.setText(planFinance);
        User currentUser = User.getCurrentUser();
        lastReviewedBy.setText(editDocument.getLastReviewedBy().getFullName());
        lastReviewDateView.setText(String.format("%s, ", sDate.format(editDocument.getlastReviewDate())));
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

    private void reviewPopup() {
        String reviewMessage = "If nothing has changed since the last review, click Save to " +
                "record that you have carried out this review and set the last review date to " +
                "today.\n\n" +
                "If anything has changed, please create a Case Update document and make the " +
                "necessary changes. In this case, the reviewer and review date will be set " +
                "automatically.";
        new AlertDialog.Builder(getActivity())
                .setTitle("Review of Plans and Financial Support")
                .setMessage(reviewMessage)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editDocument.setLastReviewDate(new Date());
                        User currentUser = User.getCurrentUser();
                        editDocument.setLastReviewedByID(currentUser.getUserID());
                        // Must also set lastReviewdBy so that save restores the right one
                        editDocument.setLastReviewedBy(currentUser);
                        editDocument.save(false);
                        // Update the view
                        lastReviewDateView.setText(String.format("%s, ", sDate.format(editDocument.getlastReviewDate())));
                        lastReviewedBy.setText(editDocument.getLastReviewedBy().getFullName());
                        reviewButton.setTextColor(ContextCompat.getColor(getActivity(), R.color.grey));
                        reviewButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Do nothing
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do Nothing
                    }
                })
                .show();
    }

    private void reviewUnavailablePopup() {
        String reviewMessage = "This is not the most recent Case document. Please carry out " +
                "the review from that document which shows the current information.\n\n" +
                "The easiest way to find the most recent Case document is to use the 'Select " +
                "Documents by Type' option from the menu (three dots) and just show the Case " +
                "documents";
        new AlertDialog.Builder(getActivity())
                .setTitle("Review of Plans and Financial Support")
                .setMessage(reviewMessage)
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do Nothing
                    }
                })
                .show();
    }

    // Build 232
    private void togglePlanFinanceHint() {
        if (planFinanceHintTextDisplayed) {
            planFinanceHintTextView.setMaxLines(2);
            planFinanceHintTextDisplayed = false;
        } else {
            planFinanceHintTextDisplayed = true;
            planFinanceHintTextView.setMaxLines(planFinanceHintTextView.getLineCount());
        }
    }

    private String getPlanFinanceHintText() {
        return "Press for Review instructions. \n\n" +
                "If the client's Plans and Financial Support are still the same, click the " +
                "button to update the reviewed by and date fields.\n\n" +
                "If the information needs to be changed, create a new Case document, set it to " +
                "Case Update and make the changes. The reviewed by and date fields will be set " +
                "automatically.\n\n";

    }


}

