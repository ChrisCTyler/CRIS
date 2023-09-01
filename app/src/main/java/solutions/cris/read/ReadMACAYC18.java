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
import solutions.cris.object.MACAYC18;
import solutions.cris.object.User;

//        CRIS - Client Record Information System
//        Copyright (C) 2022  Chris Tyler, CRIS.Solutions
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

public class ReadMACAYC18 extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private MACAYC18 editDocument;
    private View parent;
    private Client client;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.edit_maca__yc18, container, false);
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
        TextView footer = getActivity().findViewById(R.id.footer);
        editDocument = (MACAYC18) ((ListActivity) getActivity()).getDocument();
        client = ((ListActivity) getActivity()).getClient();
        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        // Swipe to be added to read in ListDocumentsFragment when adapter moved
        // do ListActivity. Until then, don't show message in footer
        footer.setText("");
        //footer.setText(R.string.action_swipe_left_for_unread);
        toolbar.setTitle(getString(R.string.app_name) + " - MACA-YC18");

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
            cancelReason.setText(String.format("Reason: %s", editDocument.getCancellationReason()));
        }

        // Link the controls
        EditText referenceDateView = parent.findViewById(R.id.reference_date);
        referenceDateView.setInputType(InputType.TYPE_NULL);
        referenceDateView.setFocusable(false);
        TextView provideHelpToTextView = parent.findViewById(R.id.provide_help_to_text);
        CheckBox provideHelpToMother = parent.findViewById(R.id.pht_mother);
        CheckBox provideHelpToStepMother = parent.findViewById(R.id.pht_stepmother);
        CheckBox provideHelpToFather = parent.findViewById(R.id.pht_father);
        CheckBox provideHelpToStepFather = parent.findViewById(R.id.pht_stepfather);
        EditText provideHelpToBrothersView = parent.findViewById(R.id.pht_brothers);
        TextView provideHelpToBrothersLabel = parent.findViewById(R.id.pht_brothers_label);
        EditText provideHelpToSistersView = parent.findViewById(R.id.pht_sisters);
        TextView provideHelpToSistersLabel = parent.findViewById(R.id.pht_sisters_label);
        EditText provideHelpToGrandparentsView = parent.findViewById(R.id.pht_grandparents);
        TextView provideHelpToGrandparentsLabel = parent.findViewById(R.id.pht_grandparents_label);
        EditText provideHelpToOtherRelativeView = parent.findViewById(R.id.pht_other_relative);
        CheckBox provideHelpToFamilyFriend = parent.findViewById(R.id.pht_family_friend);
        EditText provideHelpToOtherView = parent.findViewById(R.id.pht_other);
        EditText reasonNeedHelpView = parent.findViewById(R.id.reason_need_help);
        TextView caringTextView = parent.findViewById(R.id.caring_text);
        CheckBox caringSubstanceMisuse = parent.findViewById(R.id.caring_substance_misuse);
        CheckBox caringPhysicalDisability = parent.findViewById(R.id.caring_physical_disability);
        CheckBox caringLearningDisability = parent.findViewById(R.id.caring_learning_disability);
        CheckBox caringLifeLimiting = parent.findViewById(R.id.caring_life_limiting);
        CheckBox caringMentalHealth = parent.findViewById(R.id.caring_mental_health);
        CheckBox caringLGBT = parent.findViewById(R.id.caring_LGBT);
        CheckBox caringTraveller = parent.findViewById(R.id.caring_traveller);
        CheckBox caringRural = parent.findViewById(R.id.caring_rural);
        CheckBox caringEthnicMinority = parent.findViewById(R.id.caring_ethnic_minority);
        TextView caringErrorText = parent.findViewById(R.id.caring_error_text);
        Spinner cleanOwnBedroom01Spinner = parent.findViewById(R.id.clean_own_bedroom_01_spinner);
        TextView cleanOwnBedroom01TextView = parent.findViewById(R.id.clean_own_bedroom_01_read_text);
        Spinner cleanOtherRooms02Spinner = parent.findViewById(R.id.clean_other_rooms_02_spinner);
        TextView cleanOtherRooms02TextView = parent.findViewById(R.id.clean_other_rooms_02_read_text);
        Spinner washDishes03Spinner = parent.findViewById(R.id.wash_dishes_03_spinner);
        TextView washDishes03TextView = parent.findViewById(R.id.wash_dishes_03_read_text);
        Spinner decorateRoom04Spinner = parent.findViewById(R.id.decorate_room_04_spinner);
        TextView decorateRoom04TextView = parent.findViewById(R.id.decorate_room_04_read_text);
        Spinner responsibilityForShopping05Spinner = parent.findViewById(R.id.responsibility_for_shopping_05_spinner);
        TextView responsibilityForShopping05TextView = parent.findViewById(R.id.responsibility_for_shopping_05_read_text);
        Spinner carryingHeavyThings06Spinner = parent.findViewById(R.id.carrying_heavy_things_06_spinner);
        TextView carryingHeavyThings06TextView = parent.findViewById(R.id.carrying_heavy_things_06_read_text);
        Spinner billsBankingBenefits07Spinner = parent.findViewById(R.id.bills_banking_benefits_07_spinner);
        TextView billsBankingBenefits07TextView = parent.findViewById(R.id.bills_banking_benefits_07_read_text);
        Spinner workPartTime08Spinner = parent.findViewById(R.id.work_part_time_08_spinner);
        TextView workPartTime08TextView = parent.findViewById(R.id.work_part_time_08_read_text);
        Spinner interpretSignOther09Spinner = parent.findViewById(R.id.interpret_sign_other_09_spinner);
        TextView interpretSignOther09TextView = parent.findViewById(R.id.interpret_sign_other_09_read_text);
        Spinner helpDressUndress10Spinner = parent.findViewById(R.id.help_dress_undress_10_spinner);
        TextView helpDressUndress10TextView = parent.findViewById(R.id.help_dress_undress_10_read_text);
        Spinner helpWash11Spinner = parent.findViewById(R.id.help_wash_11_spinner);
        TextView helpWash11TextView = parent.findViewById(R.id.help_wash_11_read_text);
        Spinner helpBathShower12Spinner = parent.findViewById(R.id.help_bath_shower_12_spinner);
        TextView helpBathShower12TextView = parent.findViewById(R.id.help_bath_shower_12_read_text);
        Spinner keepPersonCompany13Spinner = parent.findViewById(R.id.keep_person_company_13_spinner);
        TextView keepPersonCompany13TextView = parent.findViewById(R.id.keep_person_company_13_read_text);
        Spinner makeSureAlright14Spinner = parent.findViewById(R.id.make_sure_alright_14_spinner);
        TextView makeSureAlright14TextView = parent.findViewById(R.id.make_sure_alright_14_read_text);
        Spinner takeOut15Spinner = parent.findViewById(R.id.take_out_15_spinner);
        TextView takeOut15TextView = parent.findViewById(R.id.take_out_15_read_text);
        Spinner takeSiblingsToSchool16Spinner = parent.findViewById(R.id.take_siblings_to_school_16_spinner);
        TextView takeSiblingsToSchool16TextView = parent.findViewById(R.id.take_siblings_to_school_16_read_text);
        Spinner lookAfterSiblingsWithAdult17Spinner = parent.findViewById(R.id.look_after_siblings_with_adult_17_spinner);
        TextView lookAfterSiblingsWithAdult17TextView = parent.findViewById(R.id.look_after_siblings_with_adult_17_read_text);
        Spinner lookAfterSiblingsOnOwn18Spinner = parent.findViewById(R.id.look_after_siblings_on_own_18_spinner);
        TextView lookAfterSiblingsOnOwn18STextView = parent.findViewById(R.id.look_after_siblings_on_own_18_read_text);
        EditText scoreView = parent.findViewById(R.id.score);

        // Show/hide the controls
        scoreView.setInputType(InputType.TYPE_NULL);
        scoreView.setFocusable(false);
        TextView hintTextView = parent.findViewById(R.id.hint_text);
        ImageView hintIconView = parent.findViewById(R.id.hint_open_close);
        Button cancelButton = parent.findViewById(R.id.cancel_button);
        Button saveButton = parent.findViewById(R.id.save_button);
        provideHelpToTextView.setVisibility(View.VISIBLE);
        provideHelpToTextView.setFocusable(false);
        provideHelpToMother.setVisibility(View.GONE);
        provideHelpToStepMother.setVisibility(View.GONE);
        provideHelpToFather.setVisibility(View.GONE);
        provideHelpToStepFather.setVisibility(View.GONE);
        provideHelpToBrothersView.setVisibility(View.GONE);
        provideHelpToBrothersLabel.setVisibility(View.GONE);
        provideHelpToSistersView.setVisibility(View.GONE);
        provideHelpToSistersLabel.setVisibility(View.GONE);
        provideHelpToGrandparentsView.setVisibility(View.GONE);
        provideHelpToGrandparentsLabel.setVisibility(View.GONE);
        provideHelpToOtherRelativeView.setVisibility(View.GONE);
        provideHelpToFamilyFriend.setVisibility(View.GONE);
        provideHelpToOtherView.setVisibility(View.GONE);
        reasonNeedHelpView.setInputType(InputType.TYPE_NULL);
        reasonNeedHelpView.setSingleLine(false);
        reasonNeedHelpView.setFocusable(false);
        caringTextView.setVisibility(View.VISIBLE);
        caringTextView.setFocusable(false);
        caringSubstanceMisuse.setVisibility(View.GONE);
        caringPhysicalDisability.setVisibility(View.GONE);
        caringLearningDisability.setVisibility(View.GONE);
        caringLifeLimiting.setVisibility(View.GONE);
        caringMentalHealth.setVisibility(View.GONE);
        caringLGBT.setVisibility(View.GONE);
        caringTraveller.setVisibility(View.GONE);
        caringRural.setVisibility(View.GONE);
        caringEthnicMinority.setVisibility(View.GONE);
        caringErrorText.setVisibility(View.GONE);
        cleanOwnBedroom01Spinner.setVisibility(View.GONE);
        cleanOwnBedroom01TextView.setVisibility(View.VISIBLE);
        cleanOwnBedroom01TextView.setFocusable(false);
        cleanOtherRooms02Spinner.setVisibility(View.GONE);
        cleanOtherRooms02TextView.setVisibility(View.VISIBLE);
        cleanOtherRooms02TextView.setFocusable(false);
        washDishes03Spinner.setVisibility(View.GONE);
        washDishes03TextView.setVisibility(View.VISIBLE);
        washDishes03TextView.setFocusable(false);
        decorateRoom04Spinner.setVisibility(View.GONE);
        decorateRoom04TextView.setVisibility(View.VISIBLE);
        decorateRoom04TextView.setFocusable(false);
        responsibilityForShopping05Spinner.setVisibility(View.GONE);
        responsibilityForShopping05TextView.setVisibility(View.VISIBLE);
        responsibilityForShopping05TextView.setFocusable(false);
        carryingHeavyThings06Spinner.setVisibility(View.GONE);
        carryingHeavyThings06TextView.setVisibility(View.VISIBLE);
        carryingHeavyThings06TextView.setFocusable(false);
        billsBankingBenefits07Spinner.setVisibility(View.GONE);
        billsBankingBenefits07TextView.setVisibility(View.VISIBLE);
        billsBankingBenefits07TextView.setFocusable(false);
        workPartTime08Spinner.setVisibility(View.GONE);
        workPartTime08TextView.setVisibility(View.VISIBLE);
        workPartTime08TextView.setFocusable(false);
        interpretSignOther09Spinner.setVisibility(View.GONE);
        interpretSignOther09TextView.setVisibility(View.VISIBLE);
        interpretSignOther09TextView.setFocusable(false);
        helpDressUndress10Spinner.setVisibility(View.GONE);
        helpDressUndress10TextView.setVisibility(View.VISIBLE);
        helpDressUndress10TextView.setFocusable(false);
        helpWash11Spinner.setVisibility(View.GONE);
        helpWash11TextView.setVisibility(View.VISIBLE);
        helpWash11TextView.setFocusable(false);
        helpBathShower12Spinner.setVisibility(View.GONE);
        helpBathShower12TextView.setVisibility(View.VISIBLE);
        helpBathShower12TextView.setFocusable(false);
        keepPersonCompany13Spinner.setVisibility(View.GONE);
        keepPersonCompany13TextView.setVisibility(View.VISIBLE);
        keepPersonCompany13TextView.setFocusable(false);
        makeSureAlright14Spinner.setVisibility(View.GONE);
        makeSureAlright14TextView.setVisibility(View.VISIBLE);
        makeSureAlright14TextView.setFocusable(false);
        takeOut15Spinner.setVisibility(View.GONE);
        takeOut15TextView.setVisibility(View.VISIBLE);
        takeOut15TextView.setFocusable(false);
        takeSiblingsToSchool16Spinner.setVisibility(View.GONE);
        takeSiblingsToSchool16TextView.setVisibility(View.VISIBLE);
        takeSiblingsToSchool16TextView.setFocusable(false);
        lookAfterSiblingsWithAdult17Spinner.setVisibility(View.GONE);
        lookAfterSiblingsWithAdult17TextView.setVisibility(View.VISIBLE);
        lookAfterSiblingsWithAdult17TextView.setFocusable(false);
        lookAfterSiblingsOnOwn18Spinner.setVisibility(View.GONE);
        lookAfterSiblingsOnOwn18STextView.setVisibility(View.VISIBLE);
        lookAfterSiblingsOnOwn18STextView.setFocusable(false);
        hintTextView.setVisibility(View.GONE);
        hintIconView.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);

        // Load the data values
        referenceDateView.setText(sDate.format(editDocument.getReferenceDate()));
        String provideHelpTo = "";
        if (editDocument.isProvideHelpToMother()) {
            provideHelpTo += String.format("%s\n", getString(R.string.prompt_maca_pht_mother));
        }
        if (editDocument.isProvideHelpToStepMother()) {
            provideHelpTo += String.format("%s\n", getString(R.string.prompt_maca_pht_stepmother));
        }
        if (editDocument.isProvideHelpToFather()) {
            provideHelpTo += String.format("%s\n", getString(R.string.prompt_maca_pht_father));
        }
        if (editDocument.isProvideHelpToStepFather()) {
            provideHelpTo += String.format("%s\n", getString(R.string.prompt_maca_pht_stepfather));
        }
        if (editDocument.getProvideHelpToBrothers() > 0) {
            provideHelpTo += String.format(Locale.UK, "%s: %d\n",
                    getString(R.string.prompt_maca_pht_brothers),
                    editDocument.getProvideHelpToBrothers()
            );
        }
        if (editDocument.getProvideHelpToSisters() > 0) {
            provideHelpTo += String.format(Locale.UK, "%s: %d\n",
                    getString(R.string.prompt_maca_pht_sisters),
                    editDocument.getProvideHelpToSisters()
            );
        }
        if (editDocument.getProvideHelpToGrandparents() > 0) {
            provideHelpTo += String.format(Locale.UK, "%s: %d\n",
                    getString(R.string.prompt_maca_pht_grandparents),
                    editDocument.getProvideHelpToGrandparents()
            );
        }
        if (editDocument.getProvideHelpToOtherRelative().length() > 0) {
            provideHelpTo += String.format("%s\n", editDocument.getProvideHelpToOtherRelative());
        }
        if (editDocument.isProvideHelpToFamilyFriend()) {
            provideHelpTo += String.format("%s\n", getString(R.string.prompt_maca_pht_family_friend));
        }
        if (editDocument.getProvideHelpToOther().length() > 0) {
            provideHelpTo += String.format("%s\n", editDocument.getProvideHelpToOther());
        }
        // Either remove the final carriage return or add a space to ensure field prompt is minimised
        if (provideHelpTo.isEmpty()) provideHelpTo = " ";
        else provideHelpTo = provideHelpTo.substring(0, provideHelpTo.length() - 1);
        provideHelpToTextView.setText(provideHelpTo);
        reasonNeedHelpView.setText(editDocument.getReasonNeedHelp());
        String caring = "";
        if (editDocument.isCaringSubstanceMisuse()) {
            caring += String.format("%s\n", getString(R.string.prompt_maca_caring_substance_misuse));
        }
        if (editDocument.isCaringPhysicalDisability()) {
            caring += String.format("%s\n", getString(R.string.prompt_maca_caring_physical_disability));
        }
        if (editDocument.isCaringLearningDisability()) {
            caring += String.format("%s\n", getString(R.string.prompt_maca_caring_learning_disability));
        }
        if (editDocument.isCaringLifeLimiting()) {
            caring += String.format("%s\n", getString(R.string.prompt_maca_caring_life_limiting));
        }
        if (editDocument.isCaringMentalHealth()) {
            caring += String.format("%s\n", getString(R.string.prompt_maca_caring_mental_health));
        }
        if (editDocument.isCaringLGBT()) {
            caring += String.format("%s\n", getString(R.string.prompt_maca_caring_LGBT));
        }
        if (editDocument.isCaringTraveller()) {
            caring += String.format("%s\n", getString(R.string.prompt_maca_caring_traveller));
        }
        if (editDocument.isCaringRural()) {
            caring += String.format("%s\n", getString(R.string.prompt_maca_caring_rural));
        }
        if (editDocument.isCaringEthnicMinority()) {
            caring += String.format("%s\n", getString(R.string.prompt_maca_caring_ethnic_minority));
        }
        // Either remove the final carriage return or add a space to ensure field prompt is minimised
        if (caring.isEmpty()) caring = " ";
        else caring = caring.substring(0, caring.length() - 1);
        caringTextView.setText(caring);
        cleanOwnBedroom01TextView.setText(editDocument.getCleanOwnBedroom01());
        cleanOtherRooms02TextView.setText(editDocument.getCleanOtherRooms02());
        washDishes03TextView.setText(editDocument.getWashDishes03());
        decorateRoom04TextView.setText(editDocument.getDecorateRoom04());
        responsibilityForShopping05TextView.setText(editDocument.getResponsibilityForShopping05());
        carryingHeavyThings06TextView.setText(editDocument.getCarryingHeavyThings06());
        billsBankingBenefits07TextView.setText(editDocument.getBillsBankingBenefits07());
        workPartTime08TextView.setText(editDocument.getWorkPartTime08());
        interpretSignOther09TextView.setText(editDocument.getInterpretSignOther09());
        helpDressUndress10TextView.setText(editDocument.getHelpDressUndress10());
        helpWash11TextView.setText(editDocument.getHelpWash11());
        helpBathShower12TextView.setText(editDocument.getHelpBathShower12());
        keepPersonCompany13TextView.setText(editDocument.getKeepPersonCompany13());
        makeSureAlright14TextView.setText(editDocument.getMakeSureAlright14());
        takeOut15TextView.setText(editDocument.getTakeOut15());
        takeSiblingsToSchool16TextView.setText(editDocument.getTakeSiblingsToSchool16());
        lookAfterSiblingsWithAdult17TextView.setText(editDocument.getLookAfterSiblingsWithAdult17());
        lookAfterSiblingsOnOwn18STextView.setText(editDocument.getLookAfterSiblingsOnOwn18());
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
