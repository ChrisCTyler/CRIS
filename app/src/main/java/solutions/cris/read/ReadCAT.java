package solutions.cris.read;

// Build 200 Use the androidX Fragment class
//import android.app.Fragment;
import androidx.fragment.app.Fragment;
import android.content.Intent;
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
import solutions.cris.object.Client;
import solutions.cris.object.CriteriaAssessmentTool;
import solutions.cris.object.User;

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

public class ReadCAT extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private CriteriaAssessmentTool editDocument;
    private View parent;
    private Client client;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.edit_cat, container, false);
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
        editDocument = (CriteriaAssessmentTool) ((ListActivity) getActivity()).getDocument();
        client = ((ListActivity) getActivity()).getClient();
        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        // Swipe to be added to read in ListDocumentsFragment when adapter moved
        // do ListActivity. Until then, don't show message in footer
        footer.setText("");
        //footer.setText(R.string.action_swipe_left_for_unread);
        toolbar.setTitle(getString(R.string.app_name) + " - Criteria Assessment Tool");

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

        EditText referenceDateView = (EditText) parent.findViewById(R.id.reference_date);
        referenceDateView.setInputType(InputType.TYPE_NULL);
        referenceDateView.setFocusable(false);
        Spinner homeSituationSpinner = (Spinner) parent.findViewById(R.id.home_situation_spinner);
        TextView homeSituationTextView = (TextView) parent.findViewById(R.id.home_situation_read_text);
        Spinner childStatusSpinner = (Spinner) parent.findViewById(R.id.child_status_spinner);
        TextView childStatusTextView = (TextView) parent.findViewById(R.id.child_status_read_text);
        Spinner typeOfSupportSpinner = (Spinner) parent.findViewById(R.id.type_of_support_spinner);
        TextView typeOfSupportTextView = (TextView) parent.findViewById(R.id.type_of_support_read_text);
        TextView personCaredForTextView = (TextView) parent.findViewById(R.id.person_cared_for_text);
        EditText personCaredForParentView = (EditText) parent.findViewById(R.id.person_cared_for_parents);
        TextView personCaredForParentLabel = (TextView) parent.findViewById(R.id.person_cared_for_parents_label);
        // Build 139 - Added Grandparents to People Cared For
        EditText personCaredForGrandparentView = (EditText) parent.findViewById(R.id.person_cared_for_grandparents);
        TextView personCaredForGrandparentLabel = (TextView) parent.findViewById(R.id.person_cared_for_grandparents_label);
        EditText personCaredForSiblingView = (EditText) parent.findViewById(R.id.person_cared_for_siblings);
        TextView personCaredForSiblingLabel = (TextView) parent.findViewById(R.id.person_cared_for_siblings_label);
        EditText personCaredForOtherView = (EditText) parent.findViewById(R.id.person_cared_for_others);
        TextView personCaredForOtherLabel = (TextView) parent.findViewById(R.id.person_cared_for_others_label);
        TextView typeOfCareView = (TextView) parent.findViewById(R.id.type_of_care_text) ;
        CheckBox tocDomestic1 = (CheckBox) parent.findViewById(R.id.toc_domestic1);
        CheckBox tocDomestic2 = (CheckBox) parent.findViewById(R.id.toc_domestic2);
        CheckBox tocPersonal = (CheckBox) parent.findViewById(R.id.toc_personal);
        CheckBox tocEmotional = (CheckBox) parent.findViewById(R.id.toc_emotional);
        CheckBox tocSupervising = (CheckBox) parent.findViewById(R.id.toc_supervising);
        TextView typeOfConditionTextView = (TextView) parent.findViewById(R.id.type_of_condition_text);
        EditText typeOfConditionMentalHealthView = (EditText) parent.findViewById(R.id.type_of_condition_mental_health);
        TextView typeOfConditionMentalHealthLabel = (TextView) parent.findViewById(R.id.type_of_condition_mental_health_label);
        EditText typeOfConditionSubstanceMisuseView = (EditText) parent.findViewById(R.id.type_of_condition_substance_misuse);
        TextView typeOfConditionSubstanceMisuseLabel = (TextView) parent.findViewById(R.id.type_of_condition_substance_misuse_label);
        EditText typeOfConditionAlcoholMisuseView = (EditText) parent.findViewById(R.id.type_of_condition_alcohol_misuse);
        TextView typeOfConditionAlcoholMisuseLabel = (TextView) parent.findViewById(R.id.type_of_condition_alcohol_misuse_label);
        EditText typeOfConditionLearningDisabilityView = (EditText) parent.findViewById(R.id.type_of_condition_learning_disability);
        TextView typeOfConditionLearningDisabilityLabel = (TextView) parent.findViewById(R.id.type_of_condition_learning_disability_label);
        EditText typeOfConditionIllHealthView = (EditText) parent.findViewById(R.id.type_of_condition_ill_health);
        TextView typeOfConditionIllHealthLabel = (TextView) parent.findViewById(R.id.type_of_condition_ill_health_label);
        EditText typeOfConditionPhysicalDisabilityView = (EditText) parent.findViewById(R.id.type_of_condition_physical_disability);
        TextView typeOfConditionPhysicalDisabilityLabel = (TextView) parent.findViewById(R.id.type_of_condition_physical_disability_label);
        EditText typeOfConditionAutismView = (EditText) parent.findViewById(R.id.type_of_condition_autism);
        TextView typeOfConditionAutismLabel = (TextView) parent.findViewById(R.id.type_of_condition_autism_label);
        EditText typeOfConditionTerminalIllnessView = (EditText) parent.findViewById(R.id.type_of_condition_terminal_illness);
        TextView typeOfConditionTerminalIllnessLabel = (TextView) parent.findViewById(R.id.type_of_condition_terminal_illness_label);
        Spinner frequencyOfCareSpinner = (Spinner) parent.findViewById(R.id.frequency_of_care_spinner);
        TextView frequencyOfCareTextView = (TextView) parent.findViewById(R.id.frequency_of_care_read_text);
        Spinner frequencyOfSocialisingSpinner = (Spinner) parent.findViewById(R.id.frequency_of_socialising_spinner);
        TextView frequencyOfSocialisingTextView = (TextView) parent.findViewById(R.id.frequency_of_socialising_read_text);
        EditText scoreView = (EditText) parent.findViewById(R.id.score);
        scoreView.setInputType(InputType.TYPE_NULL);
        scoreView.setFocusable(false);
        TextView hintTextView = (TextView) parent.findViewById(R.id.hint_text);
        ImageView hintIconView = (ImageView) parent.findViewById(R.id.hint_open_close);
        Button cancelButton = (Button) parent.findViewById(R.id.cancel_button);
        Button saveButton = (Button) parent.findViewById(R.id.save_button);

        homeSituationSpinner.setVisibility(View.GONE);
        homeSituationTextView.setVisibility(View.VISIBLE);
        homeSituationTextView.setFocusable(false);
        childStatusSpinner.setVisibility(View.GONE);
        childStatusTextView.setVisibility(View.VISIBLE);
        childStatusTextView.setFocusable(false);
        personCaredForTextView.setVisibility(View.VISIBLE);
        personCaredForTextView.setFocusable(false);
        personCaredForParentView.setVisibility(View.GONE);
        // Build 139 - Added Grandparents to People Cared For
        personCaredForGrandparentView.setVisibility(View.GONE);
        personCaredForSiblingView.setVisibility(View.GONE);
        personCaredForOtherView.setVisibility(View.GONE);
        personCaredForParentLabel.setVisibility(View.GONE);
        // Build 139 - Added Grandparents to People Cared For
        personCaredForGrandparentLabel.setVisibility(View.GONE);
        personCaredForSiblingLabel.setVisibility(View.GONE);
        personCaredForOtherLabel.setVisibility(View.GONE);
        typeOfCareView.setVisibility(View.VISIBLE);
        typeOfSupportSpinner.setVisibility(View.GONE);
        typeOfSupportTextView.setVisibility(View.VISIBLE);
        typeOfSupportTextView.setFocusable(false);
        tocDomestic1.setVisibility(View.GONE);
        tocDomestic2.setVisibility(View.GONE);
        tocPersonal.setVisibility(View.GONE);
        tocEmotional.setVisibility(View.GONE);
        tocSupervising.setVisibility(View.GONE);
        typeOfConditionTextView.setVisibility(View.VISIBLE);
        typeOfConditionTextView.setFocusable(false);
        typeOfConditionMentalHealthView.setVisibility(View.GONE);
        typeOfConditionMentalHealthLabel.setVisibility(View.GONE);
        typeOfConditionSubstanceMisuseView.setVisibility(View.GONE);
        typeOfConditionSubstanceMisuseLabel.setVisibility(View.GONE);
        typeOfConditionAlcoholMisuseView.setVisibility(View.GONE);
        typeOfConditionAlcoholMisuseLabel.setVisibility(View.GONE);
        typeOfConditionLearningDisabilityView.setVisibility(View.GONE);
        typeOfConditionLearningDisabilityLabel.setVisibility(View.GONE);
        typeOfConditionIllHealthView.setVisibility(View.GONE);
        typeOfConditionIllHealthLabel.setVisibility(View.GONE);
        typeOfConditionPhysicalDisabilityView.setVisibility(View.GONE);
        typeOfConditionPhysicalDisabilityLabel.setVisibility(View.GONE);
        typeOfConditionAutismView.setVisibility(View.GONE);
        typeOfConditionAutismLabel.setVisibility(View.GONE);
        typeOfConditionTerminalIllnessView.setVisibility(View.GONE);
        typeOfConditionTerminalIllnessLabel.setVisibility(View.GONE);
        frequencyOfCareSpinner.setVisibility(View.GONE);
        frequencyOfCareTextView.setVisibility(View.VISIBLE);
        frequencyOfCareTextView.setFocusable(false);
        frequencyOfSocialisingSpinner.setVisibility(View.GONE);
        frequencyOfSocialisingTextView.setVisibility(View.VISIBLE);
        frequencyOfSocialisingTextView.setFocusable(false);
        hintTextView.setVisibility(View.GONE);
        hintIconView.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);

        // Load the data values
        referenceDateView.setText(sDate.format(editDocument.getReferenceDate()));
        homeSituationTextView.setText(editDocument.getHomeSituation());
        childStatusTextView.setText(editDocument.getChildStatus());
        typeOfSupportTextView.setText(editDocument.getTypeOfSupport());
        String personCaredFor = "";
        if (editDocument.getPersonCaredForParent() > 1) {
            personCaredFor += String.format(Locale.UK, "%d parents cared for.\n", editDocument.getPersonCaredForParent());
        } else if (editDocument.getPersonCaredForParent() > 0) {
            personCaredFor += String.format(Locale.UK, "%d parent cared for.\n", editDocument.getPersonCaredForParent());
        }
        // Build 139 - Added Grandparents to People Cared For
        if (editDocument.getPersonCaredForGrandparent() > 1) {
            personCaredFor += String.format(Locale.UK, "%d grandparents cared for.\n", editDocument.getPersonCaredForGrandparent());
        } else if (editDocument.getPersonCaredForGrandparent() > 0) {
            personCaredFor += String.format(Locale.UK, "%d grandparent cared for.\n", editDocument.getPersonCaredForGrandparent());
        }
        if (editDocument.getPersonCaredForSibling() > 1) {
            personCaredFor += String.format(Locale.UK, "%d siblings cared for.\n", editDocument.getPersonCaredForSibling());
        } else if (editDocument.getPersonCaredForSibling() > 0) {
            personCaredFor += String.format(Locale.UK, "%d sibling cared for.\n", editDocument.getPersonCaredForSibling());
        }
        if (editDocument.getPersonCaredForOther() > 1) {
            personCaredFor += String.format(Locale.UK, "%d others cared for.\n", editDocument.getPersonCaredForOther());
        } else if (editDocument.getPersonCaredForOther() > 0) {
            personCaredFor += String.format(Locale.UK, "%d other cared for.\n", editDocument.getPersonCaredForOther());
        }
        // Either remove the final carriage return or add a space to ensure field prompt is minimised
        if (personCaredFor.isEmpty()) personCaredFor = " ";
        else personCaredFor = personCaredFor.substring(0,personCaredFor.length()-1);
        personCaredForTextView.setText(personCaredFor);
        String typeOfCare = "";
        if (editDocument.getTypeOfCareDomestic1())
            typeOfCare += String.format("%s\n", getString(R.string.prompt_toc_domestic1));
        if (editDocument.getTypeOfCareDomestic2())
            typeOfCare += String.format("%s\n", getString(R.string.prompt_toc_domestic2));
        if (editDocument.getTypeOfCarePersonal())
            typeOfCare += String.format("%s\n", getString(R.string.prompt_toc_personal));
        if (editDocument.getTypeOfCareEmotional())
            typeOfCare += String.format("%s\n", getString(R.string.prompt_toc_emotional));
        if (editDocument.getTypeOfCareSupervising())
            typeOfCare += String.format("%s\n", getString(R.string.prompt_toc_supervising));
        // Either remove the final carriage return or add a space to ensure field prompt is minimised
        if (typeOfCare.isEmpty()) typeOfCare = " ";
        else typeOfCare = typeOfCare.substring(0,typeOfCare.length()-1);
        String typeOfCondition = "";
        if (editDocument.getTypeOfConditionMentalHealth() > 1) {
            typeOfCondition += String.format(Locale.UK, "Supporting %d people with %s.\n",
                    editDocument.getTypeOfConditionMentalHealth(), "Mental Health Issues");
        } else if (editDocument.getTypeOfConditionMentalHealth() > 0) {
            typeOfCondition += String.format(Locale.UK, "Supporting 1 person with %s.\n", "Mental Health Issues");
        }
        if (editDocument.getTypeOfConditionSubstanceMisuse() > 1) {
            typeOfCondition += String.format(Locale.UK, "Supporting %d people with %s.\n",
                    editDocument.getTypeOfConditionSubstanceMisuse(), "Substance Misuse Issues");
        } else if (editDocument.getTypeOfConditionSubstanceMisuse() > 0) {
            typeOfCondition += String.format(Locale.UK, "Supporting 1 person with %s.\n", "Substance Misuse Issues");
        }
        if (editDocument.getTypeOfConditionAlcoholMisuse() > 1) {
            typeOfCondition += String.format(Locale.UK, "Supporting %d people with %s.\n",
                    editDocument.getTypeOfConditionAlcoholMisuse(), "Alcohol Misuse Issues");
        } else if (editDocument.getTypeOfConditionAlcoholMisuse() > 0) {
            typeOfCondition += String.format(Locale.UK, "Supporting 1 person with %s.\n", "Alcohol Misuse Issues");
        }
        if (editDocument.getTypeOfConditionLearningDisability() > 1) {
            typeOfCondition += String.format(Locale.UK, "Supporting %d people with %s.\n",
                    editDocument.getTypeOfConditionLearningDisability(), "Learning Disabilities");
        } else if (editDocument.getTypeOfConditionLearningDisability() > 0) {
            typeOfCondition += String.format(Locale.UK, "Supporting 1 person with %s.\n", "Lerning Disability");
        }
        if (editDocument.getTypeOfConditionIllHealth() > 1) {
            typeOfCondition += String.format(Locale.UK, "Supporting %d people with %s.\n",
                    editDocument.getTypeOfConditionIllHealth(), "Physical Ill Health");
        } else if (editDocument.getTypeOfConditionIllHealth() > 0) {
            typeOfCondition += String.format(Locale.UK, "Supporting 1 person with %s.\n", "Physical Ill Health");
        }
        if (editDocument.getTypeOfConditionPhysicalDisability() > 1) {
            typeOfCondition += String.format(Locale.UK, "Supporting %d people with %s.\n",
                    editDocument.getTypeOfConditionPhysicalDisability(), "Physical Disabilities");
        } else if (editDocument.getTypeOfConditionPhysicalDisability() > 0) {
            typeOfCondition += String.format(Locale.UK, "Supporting 1 person with %s.\n", "a Physical Disability");
        }
        if (editDocument.getTypeOfConditionAutism() > 1) {
            typeOfCondition += String.format(Locale.UK, "Supporting %d people with %s.\n",
                    editDocument.getTypeOfConditionAutism(), "Autistic Spectrum Disorder");
        } else if (editDocument.getTypeOfConditionAutism() > 0) {
            typeOfCondition += String.format(Locale.UK, "Supporting 1 person with %s.\n", "Autistic Spectrum Disorder");
        }
        if (editDocument.getTypeOfConditionTerminalIllness() > 1) {
            typeOfCondition += String.format(Locale.UK, "Supporting %d people with %s.\n",
                    editDocument.getTypeOfConditionTerminalIllness(), "Terminal Illnesses");
        } else if (editDocument.getTypeOfConditionTerminalIllness() > 0) {
            typeOfCondition += String.format(Locale.UK, "Supporting 1 person with %s.\n", "a Terminal Illness");
        }
        // Either remove the final carriage return or add a space to ensure field prompt is minimised
        if (typeOfCondition.isEmpty()) typeOfCondition = " ";
        else typeOfCondition = typeOfCondition.substring(0,typeOfCondition.length()-1);
        typeOfConditionTextView.setText(typeOfCondition);
        typeOfCareView.setText(typeOfCare);
        tocDomestic2.setChecked(editDocument.getTypeOfCareDomestic2());
        tocPersonal.setChecked(editDocument.getTypeOfCarePersonal());
        tocEmotional.setChecked(editDocument.getTypeOfCareEmotional());
        tocSupervising.setChecked(editDocument.getTypeOfCareSupervising());
        frequencyOfCareTextView.setText(editDocument.getFrequencyOfCare());
        frequencyOfSocialisingTextView.setText(editDocument.getFrequencyOfSocialising());
        scoreView.setText(String.format(Locale.UK, "%d", editDocument.getScore()));

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