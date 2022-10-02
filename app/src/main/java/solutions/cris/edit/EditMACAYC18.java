package solutions.cris.edit;
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

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Document;
import solutions.cris.object.MACAYC18;
import solutions.cris.object.User;
import solutions.cris.utils.CRISUtil;

public class EditMACAYC18 extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private MACAYC18 editDocument;

    private EditText referenceDateView;
    private CheckBox provideHelpToMother;
    private CheckBox provideHelpToStepMother;
    private CheckBox provideHelpToFather;
    private CheckBox provideHelpToStepFather;
    private EditText provideHelpToBrothers;
    private EditText provideHelpToSisters;
    private EditText provideHelpToGrandparents;
    private EditText provideHelpToOtherRelative;
    private CheckBox provideHelpToFamilyFriend;
    private EditText provideHelpToOther;
    private EditText reasonNeedHelpView;
    private CheckBox caringSubstanceMisuse;
    private CheckBox caringPhysicalDisability;
    private CheckBox caringLearningDisability;
    private CheckBox caringLifeLimiting;
    private CheckBox caringMentalHealth;
    private CheckBox caringLGBT;
    private CheckBox caringTraveller;
    private CheckBox caringRural;
    private CheckBox caringEthnicMinority;
    private TextView caringErrorText;
    private Spinner cleanOwnBedroom01Spinner;
    private Spinner cleanOtherRooms02Spinner;
    private Spinner washDishes03Spinner;
    private Spinner decorateRoom04Spinner;
    private Spinner responsibilityForShopping05Spinner;
    private Spinner carryingHeavyThings06Spinner;
    private Spinner billsBankingBenefits07Spinner;
    private Spinner workPartTime08Spinner;
    private Spinner interpretSignOther09Spinner;
    private Spinner helpDressUndress10Spinner;
    private Spinner helpWash11Spinner;
    private Spinner helpBathShower12Spinner;
    private Spinner keepPersonCompany13Spinner;
    private Spinner makeSureAlright14Spinner;
    private Spinner takeOut15Spinner;
    private Spinner takeSiblingsToSchool16Spinner;
    private Spinner lookAfterSiblingsWithAdult17Spinner;
    private Spinner lookAfterSiblingsOnOwn18Spinner;
    private EditText scoreView;

    private View parent;
    private boolean isNewMode = false;

    private TextView hintTextView;
    private boolean hintTextDisplayed = true;

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

        if (((ListActivity) getActivity()).getMode() == Document.Mode.NEW) {
            isNewMode = true;
        }

        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        if (isNewMode) {
            toolbar.setTitle(getString(R.string.app_name) + " - New MACA-YC18");
        } else {
            toolbar.setTitle(getString(R.string.app_name) + " - Edit MACA-YC18");
        }
        // Hide the FAB
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        fab.setVisibility(View.GONE);

        // Clear the footer
        TextView footer = getActivity().findViewById(R.id.footer);
        footer.setText("");

        // Set up the hint text
        hintTextView = getActivity().findViewById(R.id.hint_text);
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


        // Get the document to be edited from the activity
        editDocument = (MACAYC18) ((ListActivity) getActivity()).getDocument();
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

        referenceDateView = parent.findViewById(R.id.reference_date);


        // Need to hook up brother/sister checkbox etc
        provideHelpToMother = parent.findViewById(R.id.pht_mother);
        provideHelpToStepMother = parent.findViewById(R.id.pht_stepmother);
        provideHelpToFather = parent.findViewById(R.id.pht_father);
        provideHelpToStepFather = parent.findViewById(R.id.pht_stepfather);
        provideHelpToBrothers = parent.findViewById(R.id.pht_brothers);
        provideHelpToSisters = parent.findViewById(R.id.pht_sisters);
        provideHelpToGrandparents = parent.findViewById(R.id.pht_grandparents);
        provideHelpToOtherRelative = parent.findViewById(R.id.pht_other_relative);
        provideHelpToFamilyFriend = parent.findViewById(R.id.pht_family_friend);
        provideHelpToOther = parent.findViewById(R.id.pht_other);
        reasonNeedHelpView = parent.findViewById(R.id.reason_need_help);
        caringSubstanceMisuse = parent.findViewById(R.id.caring_substance_misuse);
        caringPhysicalDisability = parent.findViewById(R.id.caring_physical_disability);
        caringLearningDisability = parent.findViewById(R.id.caring_learning_disability);
        caringLifeLimiting = parent.findViewById(R.id.caring_life_limiting);
        caringMentalHealth = parent.findViewById(R.id.caring_mental_health);
        caringLGBT = parent.findViewById(R.id.caring_LGBT);
        caringTraveller = parent.findViewById(R.id.caring_traveller);
        caringRural = parent.findViewById(R.id.caring_rural);
        caringEthnicMinority = parent.findViewById(R.id.caring_ethnic_minority);
        caringErrorText = parent.findViewById(R.id.caring_error_text);
        cleanOwnBedroom01Spinner = parent.findViewById(R.id.clean_own_bedroom_01_spinner);
        cleanOtherRooms02Spinner = parent.findViewById(R.id.clean_other_rooms_02_spinner);
        washDishes03Spinner = parent.findViewById(R.id.wash_dishes_03_spinner);
        decorateRoom04Spinner = parent.findViewById(R.id.decorate_room_04_spinner);
        responsibilityForShopping05Spinner = parent.findViewById(R.id.responsibility_for_shopping_05_spinner);
        carryingHeavyThings06Spinner = parent.findViewById(R.id.carrying_heavy_things_06_spinner);
        billsBankingBenefits07Spinner = parent.findViewById(R.id.bills_banking_benefits_07_spinner);
        workPartTime08Spinner = parent.findViewById(R.id.work_part_time_08_spinner);
        interpretSignOther09Spinner = parent.findViewById(R.id.interpret_sign_other_09_spinner);
        helpDressUndress10Spinner = parent.findViewById(R.id.help_dress_undress_10_spinner);
        helpWash11Spinner = parent.findViewById(R.id.help_wash_11_spinner);
        helpBathShower12Spinner = parent.findViewById(R.id.help_bath_shower_12_spinner);
        keepPersonCompany13Spinner = parent.findViewById(R.id.keep_person_company_13_spinner);
        makeSureAlright14Spinner = parent.findViewById(R.id.make_sure_alright_14_spinner);
        takeOut15Spinner = parent.findViewById(R.id.take_out_15_spinner);
        takeSiblingsToSchool16Spinner = parent.findViewById(R.id.take_siblings_to_school_16_spinner);
        lookAfterSiblingsWithAdult17Spinner = parent.findViewById(R.id.look_after_siblings_with_adult_17_spinner);
        lookAfterSiblingsOnOwn18Spinner = parent.findViewById(R.id.look_after_siblings_on_own_18_spinner);
        scoreView = parent.findViewById(R.id.score);
        scoreView.setInputType(InputType.TYPE_NULL);
        scoreView.setFocusable(false);

        // Hide the (no selection) error text
        caringErrorText.setVisibility(View.GONE);

        referenceDateView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                referenceDatePicker();
                return true;
            }
        });

        // Initialise the cleanOwnBedroom01 Spinner
        final ArrayAdapter<String> cleanOwnBedroom01Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        cleanOwnBedroom01Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cleanOwnBedroom01Spinner.setAdapter(cleanOwnBedroom01Adapter);
        cleanOwnBedroom01Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setCleanOwnBedroom01(cleanOwnBedroom01Spinner.getSelectedItem().toString());
                cleanOwnBedroom01Spinner.requestFocus();
                cleanOwnBedroom01Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setCleanOwnBedroom01(cleanOwnBedroom01Spinner.getSelectedItem().toString());
                cleanOwnBedroom01Spinner.requestFocus();
                cleanOwnBedroom01Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the cleanOtherRooms02 Spinner
        final ArrayAdapter<String> cleanOtherRooms02Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        cleanOtherRooms02Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cleanOtherRooms02Spinner.setAdapter(cleanOtherRooms02Adapter);
        cleanOtherRooms02Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setCleanOtherRooms02(cleanOtherRooms02Spinner.getSelectedItem().toString());
                cleanOtherRooms02Spinner.requestFocus();
                cleanOtherRooms02Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setCleanOtherRooms02(cleanOtherRooms02Spinner.getSelectedItem().toString());
                cleanOtherRooms02Spinner.requestFocus();
                cleanOtherRooms02Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the washDishes03 Spinner
        final ArrayAdapter<String> washDishes03Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        washDishes03Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        washDishes03Spinner.setAdapter(washDishes03Adapter);
        washDishes03Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setWashDishes03(washDishes03Spinner.getSelectedItem().toString());
                washDishes03Spinner.requestFocus();
                washDishes03Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setWashDishes03(washDishes03Spinner.getSelectedItem().toString());
                washDishes03Spinner.requestFocus();
                washDishes03Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the decorateRoom04 Spinner
        final ArrayAdapter<String> decorateRoom04Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        decorateRoom04Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        decorateRoom04Spinner.setAdapter(decorateRoom04Adapter);
        decorateRoom04Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setDecorateRoom04(decorateRoom04Spinner.getSelectedItem().toString());
                decorateRoom04Spinner.requestFocus();
                decorateRoom04Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setDecorateRoom04(decorateRoom04Spinner.getSelectedItem().toString());
                decorateRoom04Spinner.requestFocus();
                decorateRoom04Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the responsibilityForShopping05 Spinner
        final ArrayAdapter<String> responsibilityForShopping05Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        responsibilityForShopping05Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        responsibilityForShopping05Spinner.setAdapter(responsibilityForShopping05Adapter);
        responsibilityForShopping05Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setResponsibilityForShopping05(responsibilityForShopping05Spinner.getSelectedItem().toString());
                responsibilityForShopping05Spinner.requestFocus();
                responsibilityForShopping05Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setResponsibilityForShopping05(responsibilityForShopping05Spinner.getSelectedItem().toString());
                responsibilityForShopping05Spinner.requestFocus();
                responsibilityForShopping05Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the carryingHeavyThings06 Spinner
        final ArrayAdapter<String> carryingHeavyThings06Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        carryingHeavyThings06Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        carryingHeavyThings06Spinner.setAdapter(carryingHeavyThings06Adapter);
        carryingHeavyThings06Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setCarryingHeavyThings06(carryingHeavyThings06Spinner.getSelectedItem().toString());
                carryingHeavyThings06Spinner.requestFocus();
                carryingHeavyThings06Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setCarryingHeavyThings06(carryingHeavyThings06Spinner.getSelectedItem().toString());
                carryingHeavyThings06Spinner.requestFocus();
                carryingHeavyThings06Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the billsBankingBenefits07 Spinner
        final ArrayAdapter<String> billsBankingBenefits07Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        billsBankingBenefits07Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        billsBankingBenefits07Spinner.setAdapter(billsBankingBenefits07Adapter);
        billsBankingBenefits07Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setBillsBankingBenefits07(billsBankingBenefits07Spinner.getSelectedItem().toString());
                billsBankingBenefits07Spinner.requestFocus();
                billsBankingBenefits07Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setBillsBankingBenefits07(billsBankingBenefits07Spinner.getSelectedItem().toString());
                billsBankingBenefits07Spinner.requestFocus();
                billsBankingBenefits07Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the workPartTime08 Spinner
        final ArrayAdapter<String> workPartTime08Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        workPartTime08Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        workPartTime08Spinner.setAdapter(workPartTime08Adapter);
        workPartTime08Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setWorkPartTime08(workPartTime08Spinner.getSelectedItem().toString());
                workPartTime08Spinner.requestFocus();
                workPartTime08Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setWorkPartTime08(workPartTime08Spinner.getSelectedItem().toString());
                workPartTime08Spinner.requestFocus();
                workPartTime08Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the interpretSignOther09 Spinner
        final ArrayAdapter<String> interpretSignOther09Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        interpretSignOther09Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        interpretSignOther09Spinner.setAdapter(interpretSignOther09Adapter);
        interpretSignOther09Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setInterpretSignOther09(interpretSignOther09Spinner.getSelectedItem().toString());
                interpretSignOther09Spinner.requestFocus();
                interpretSignOther09Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setInterpretSignOther09(interpretSignOther09Spinner.getSelectedItem().toString());
                interpretSignOther09Spinner.requestFocus();
                interpretSignOther09Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the helpDressUndress10 Spinner
        final ArrayAdapter<String> helpDressUndress10Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        helpDressUndress10Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        helpDressUndress10Spinner.setAdapter(helpDressUndress10Adapter);
        helpDressUndress10Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setHelpDressUndress10(helpDressUndress10Spinner.getSelectedItem().toString());
                helpDressUndress10Spinner.requestFocus();
                helpDressUndress10Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setHelpDressUndress10(helpDressUndress10Spinner.getSelectedItem().toString());
                helpDressUndress10Spinner.requestFocus();
                helpDressUndress10Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the helpWash11 Spinner
        final ArrayAdapter<String> helpWash11Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        helpWash11Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        helpWash11Spinner.setAdapter(helpWash11Adapter);
        helpWash11Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setHelpWash11(helpWash11Spinner.getSelectedItem().toString());
                helpWash11Spinner.requestFocus();
                helpWash11Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setHelpWash11(helpWash11Spinner.getSelectedItem().toString());
                helpWash11Spinner.requestFocus();
                helpWash11Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the helpBathShower12 Spinner
        final ArrayAdapter<String> helpBathShower12Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        helpBathShower12Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        helpBathShower12Spinner.setAdapter(helpBathShower12Adapter);
        helpBathShower12Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setHelpBathShower12(helpBathShower12Spinner.getSelectedItem().toString());
                helpBathShower12Spinner.requestFocus();
                helpBathShower12Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setHelpBathShower12(helpBathShower12Spinner.getSelectedItem().toString());
                helpBathShower12Spinner.requestFocus();
                helpBathShower12Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the keepPersonCompany13 Spinner
        final ArrayAdapter<String> keepPersonCompany13Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        keepPersonCompany13Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keepPersonCompany13Spinner.setAdapter(keepPersonCompany13Adapter);
        keepPersonCompany13Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setKeepPersonCompany13(keepPersonCompany13Spinner.getSelectedItem().toString());
                keepPersonCompany13Spinner.requestFocus();
                keepPersonCompany13Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setKeepPersonCompany13(keepPersonCompany13Spinner.getSelectedItem().toString());
                keepPersonCompany13Spinner.requestFocus();
                keepPersonCompany13Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the makeSureAlright14 Spinner
        final ArrayAdapter<String> makeSureAlright14Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        makeSureAlright14Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        makeSureAlright14Spinner.setAdapter(makeSureAlright14Adapter);
        makeSureAlright14Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setMakeSureAlright14(makeSureAlright14Spinner.getSelectedItem().toString());
                makeSureAlright14Spinner.requestFocus();
                makeSureAlright14Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setMakeSureAlright14(makeSureAlright14Spinner.getSelectedItem().toString());
                makeSureAlright14Spinner.requestFocus();
                makeSureAlright14Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the takeOut15 Spinner
        final ArrayAdapter<String> takeOut15Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        takeOut15Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        takeOut15Spinner.setAdapter(takeOut15Adapter);
        takeOut15Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setTakeOut15(takeOut15Spinner.getSelectedItem().toString());
                takeOut15Spinner.requestFocus();
                takeOut15Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setTakeOut15(takeOut15Spinner.getSelectedItem().toString());
                takeOut15Spinner.requestFocus();
                takeOut15Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the takeSiblingsToSchool16 Spinner
        final ArrayAdapter<String> takeSiblingsToSchool16Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        takeSiblingsToSchool16Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        takeSiblingsToSchool16Spinner.setAdapter(takeSiblingsToSchool16Adapter);
        takeSiblingsToSchool16Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setTakeSiblingsToSchool16(takeSiblingsToSchool16Spinner.getSelectedItem().toString());
                takeSiblingsToSchool16Spinner.requestFocus();
                takeSiblingsToSchool16Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setTakeSiblingsToSchool16(takeSiblingsToSchool16Spinner.getSelectedItem().toString());
                takeSiblingsToSchool16Spinner.requestFocus();
                takeSiblingsToSchool16Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the lookAfterSiblingsWithAdult17 Spinner
        final ArrayAdapter<String> lookAfterSiblingsWithAdult17Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        lookAfterSiblingsWithAdult17Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lookAfterSiblingsWithAdult17Spinner.setAdapter(lookAfterSiblingsWithAdult17Adapter);
        lookAfterSiblingsWithAdult17Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setLookAfterSiblingsWithAdult17(lookAfterSiblingsWithAdult17Spinner.getSelectedItem().toString());
                lookAfterSiblingsWithAdult17Spinner.requestFocus();
                lookAfterSiblingsWithAdult17Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setLookAfterSiblingsWithAdult17(lookAfterSiblingsWithAdult17Spinner.getSelectedItem().toString());
                lookAfterSiblingsWithAdult17Spinner.requestFocus();
                lookAfterSiblingsWithAdult17Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        // Initialise the lookAfterSiblingsOnOwn18 Spinner
        final ArrayAdapter<String> lookAfterSiblingsOnOwn18Adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MACAYC18.responseValues);
        lookAfterSiblingsOnOwn18Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lookAfterSiblingsOnOwn18Spinner.setAdapter(lookAfterSiblingsOnOwn18Adapter);
        lookAfterSiblingsOnOwn18Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setLookAfterSiblingsOnOwn18(lookAfterSiblingsOnOwn18Spinner.getSelectedItem().toString());
                lookAfterSiblingsOnOwn18Spinner.requestFocus();
                lookAfterSiblingsOnOwn18Spinner.requestFocusFromTouch();
                showScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setLookAfterSiblingsOnOwn18(lookAfterSiblingsOnOwn18Spinner.getSelectedItem().toString());
                lookAfterSiblingsOnOwn18Spinner.requestFocus();
                lookAfterSiblingsOnOwn18Spinner.requestFocusFromTouch();
                showScore();
            }
        });

        provideHelpToBrothers.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(provideHelpToBrothers.getText().toString());
                    if (newValue == -1) {
                        provideHelpToBrothers.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        provideHelpToBrothers.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getProvideHelpToBrothers() != newValue) {
                        editDocument.setProvideHelpToBrothers(newValue);
                    }
                }
            }
        });

        // Build 139 - Added Grandparents to People Cared For
        provideHelpToSisters.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(provideHelpToSisters.getText().toString());
                    if (newValue == -1) {
                        provideHelpToSisters.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        provideHelpToSisters.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getProvideHelpToSisters() != newValue) {
                        editDocument.setProvideHelpToSisters(newValue);
                    }
                }
            }
        });

        provideHelpToGrandparents.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(provideHelpToGrandparents.getText().toString());
                    if (newValue == -1) {
                        provideHelpToGrandparents.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        provideHelpToGrandparents.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getProvideHelpToGrandparents() != newValue) {
                        editDocument.setProvideHelpToGrandparents(newValue);
                    }
                }
            }
        });

        provideHelpToOtherRelative.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    editDocument.setProvideHelpToOtherRelative(provideHelpToOtherRelative.getText().toString());
                }
            }
        });

        provideHelpToOther.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    editDocument.setProvideHelpToOther(provideHelpToOther.getText().toString());
                }
            }
        });

        provideHelpToMother.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setProvideHelpToMother(b);
            }
        });

        provideHelpToStepMother.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setProvideHelpToStepMother(b);
            }
        });

        provideHelpToFather.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setProvideHelpToFather(b);
            }
        });

        provideHelpToStepFather.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setProvideHelpToStepFather(b);
            }
        });


        provideHelpToFamilyFriend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setProvideHelpToFamilyFriend(b);
            }
        });


        caringSubstanceMisuse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setCaringSubstanceMisuse(b);
            }
        });

        caringPhysicalDisability.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setCaringPhysicalDisability(b);
            }
        });

        caringLearningDisability.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setCaringLearningDisability(b);
            }
        });

        caringLifeLimiting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setCaringLifeLimiting(b);
            }
        });

        caringMentalHealth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setCaringMentalHealth(b);
            }
        });

        caringLGBT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setCaringLGBT(b);
            }
        });

        caringTraveller.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setCaringTraveller(b);
            }
        });

        caringRural.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setCaringRural(b);
            }
        });

        caringEthnicMinority.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setCaringEthnicMinority(b);
            }
        });


        // Cancel Button
        Button cancelButton = parent.findViewById(R.id.cancel_button);
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
        Button saveButton = parent.findViewById(R.id.save_button);
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

        if (isNewMode) {
            referenceDateView.setText(sDate.format(editDocument.getCreationDate()));
            scoreView.setText("0");
            provideHelpToBrothers.setText("0");
            provideHelpToSisters.setText("0");
            provideHelpToGrandparents.setText("0");

        } else {
            // Edit Mode
            referenceDateView.setText(sDate.format(editDocument.getReferenceDate()));
            provideHelpToMother.setChecked(editDocument.isProvideHelpToMother());
            provideHelpToStepMother.setChecked(editDocument.isProvideHelpToStepMother());
            provideHelpToFather.setChecked(editDocument.isProvideHelpToFather());
            provideHelpToStepFather.setChecked(editDocument.isProvideHelpToStepFather());
            provideHelpToBrothers.setText(String.format(Locale.UK, "%d", editDocument.getProvideHelpToBrothers()));
            provideHelpToSisters.setText(String.format(Locale.UK, "%d", editDocument.getProvideHelpToSisters()));
            provideHelpToGrandparents.setText(String.format(Locale.UK, "%d", editDocument.getProvideHelpToGrandparents()));
            provideHelpToOtherRelative.setText(String.format(Locale.UK, "%s", editDocument.getProvideHelpToOtherRelative()));
            provideHelpToFamilyFriend.setChecked(editDocument.isProvideHelpToFamilyFriend());
            provideHelpToOther.setText(String.format(Locale.UK, "%s", editDocument.getProvideHelpToOther()));
            reasonNeedHelpView.setText(String.format(Locale.UK, "%s", editDocument.getReasonNeedHelp()));
            caringSubstanceMisuse.setChecked(editDocument.isCaringSubstanceMisuse());
            caringPhysicalDisability.setChecked(editDocument.isCaringPhysicalDisability());
            caringLearningDisability.setChecked(editDocument.isCaringLearningDisability());
            caringLifeLimiting.setChecked(editDocument.isCaringLifeLimiting());
            caringMentalHealth.setChecked(editDocument.isCaringMentalHealth());
            caringLGBT.setChecked(editDocument.isCaringLGBT());
            caringTraveller.setChecked(editDocument.isCaringTraveller());
            caringRural.setChecked(editDocument.isCaringRural());
            caringEthnicMinority.setChecked(editDocument.isCaringEthnicMinority());
            cleanOwnBedroom01Spinner.setSelection(cleanOwnBedroom01Adapter.getPosition(editDocument.getCleanOwnBedroom01()));
            cleanOtherRooms02Spinner.setSelection(cleanOtherRooms02Adapter.getPosition(editDocument.getCleanOtherRooms02()));
            washDishes03Spinner.setSelection(washDishes03Adapter.getPosition(editDocument.getWashDishes03()));
            decorateRoom04Spinner.setSelection(decorateRoom04Adapter.getPosition(editDocument.getDecorateRoom04()));
            responsibilityForShopping05Spinner.setSelection(responsibilityForShopping05Adapter.getPosition(editDocument.getResponsibilityForShopping05()));
            carryingHeavyThings06Spinner.setSelection(carryingHeavyThings06Adapter.getPosition(editDocument.getCarryingHeavyThings06()));
            billsBankingBenefits07Spinner.setSelection(billsBankingBenefits07Adapter.getPosition(editDocument.getBillsBankingBenefits07()));
            workPartTime08Spinner.setSelection(workPartTime08Adapter.getPosition(editDocument.getWorkPartTime08()));
            interpretSignOther09Spinner.setSelection(interpretSignOther09Adapter.getPosition(editDocument.getInterpretSignOther09()));
            helpDressUndress10Spinner.setSelection(helpDressUndress10Adapter.getPosition(editDocument.getHelpDressUndress10()));
            helpWash11Spinner.setSelection(helpWash11Adapter.getPosition(editDocument.getHelpWash11()));
            helpBathShower12Spinner.setSelection(helpBathShower12Adapter.getPosition(editDocument.getHelpBathShower12()));
            keepPersonCompany13Spinner.setSelection(keepPersonCompany13Adapter.getPosition(editDocument.getKeepPersonCompany13()));
            makeSureAlright14Spinner.setSelection(makeSureAlright14Adapter.getPosition(editDocument.getMakeSureAlright14()));
            takeOut15Spinner.setSelection(takeOut15Adapter.getPosition(editDocument.getTakeOut15()));
            takeSiblingsToSchool16Spinner.setSelection(takeSiblingsToSchool16Adapter.getPosition(editDocument.getTakeSiblingsToSchool16()));
            lookAfterSiblingsWithAdult17Spinner.setSelection(lookAfterSiblingsWithAdult17Adapter.getPosition(editDocument.getLookAfterSiblingsWithAdult17()));
            lookAfterSiblingsOnOwn18Spinner.setSelection(lookAfterSiblingsOnOwn18Adapter.getPosition(editDocument.getLookAfterSiblingsOnOwn18()));
            scoreView.setText(String.format(Locale.UK, "%d", editDocument.getScore()));
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

    private int getInt(String sValue) {
        int value;
        if (sValue.isEmpty()) {
            value = -2;
        } else {
            try {
                value = Integer.parseInt(sValue);
                if (value < 0 || value > 5) {
                    value = -1;
                }
            } catch (Exception ex) {
                value = -2;
            }
        }
        return value;
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

    private void showScore() {
        scoreView.setText(String.format(Locale.UK, "%d", editDocument.getScore()));
    }

    private void referenceDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog referenceDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                referenceDateView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        referenceDatePickerDialog.show();
    }

    // Validate the document
    private boolean validate() {
        boolean success = true;

        referenceDateView.setError(null);
        provideHelpToBrothers.setError(null);
        provideHelpToSisters.setError(null);
        provideHelpToGrandparents.setError(null);

        caringErrorText.setVisibility(View.GONE);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        // ReferenceDate
        String sReferenceDate = referenceDateView.getText().toString();
        if (TextUtils.isEmpty(sReferenceDate)) {
            referenceDateView.setError(getString(R.string.error_field_required));
            focusView = referenceDateView;
            success = false;
        } else {
            Date dReferenceDate = CRISUtil.parseDate(sReferenceDate);
            if (dReferenceDate == null) {
                referenceDateView.setError(getString(R.string.error_invalid_date));
                focusView = referenceDateView;
                success = false;
            } else {
                editDocument.setReferenceDate(dReferenceDate);
            }
        }

        String sProvideHelpToBrothers = provideHelpToBrothers.getText().toString().trim();

        int provideHelpToBrothersCount;
        try {
            provideHelpToBrothersCount = Integer.parseInt(sProvideHelpToBrothers);
            if (provideHelpToBrothersCount < 0 || provideHelpToBrothersCount > 5) {
                provideHelpToBrothers.setError(getString(R.string.error_number_0_to_5));
                focusView = provideHelpToBrothers;
                success = false;
            } else {
                editDocument.setProvideHelpToBrothers(provideHelpToBrothersCount);
            }
        } catch (Exception ex) {
            provideHelpToBrothers.setError(getString(R.string.error_invalid_integer));
            focusView = provideHelpToBrothers;
            success = false;
        }


        String sProvideHelpToSisters = provideHelpToSisters.getText().toString().trim();

        int provideHelpToSistersCount;
        try {
            provideHelpToSistersCount = Integer.parseInt(sProvideHelpToSisters);
            if (provideHelpToSistersCount < 0 || provideHelpToSistersCount > 5) {
                provideHelpToSisters.setError(getString(R.string.error_number_0_to_5));
                focusView = provideHelpToSisters;
                success = false;
            } else {
                editDocument.setProvideHelpToSisters(provideHelpToSistersCount);
            }
        } catch (Exception ex) {
            provideHelpToSisters.setError(getString(R.string.error_invalid_integer));
            focusView = provideHelpToSisters;
            success = false;
        }


        String sProvideHelpToGrandparents = provideHelpToGrandparents.getText().toString().trim();

        int provideHelpToGrandparentsCount;
        try {
            provideHelpToGrandparentsCount = Integer.parseInt(sProvideHelpToGrandparents);
            if (provideHelpToGrandparentsCount < 0 || provideHelpToGrandparentsCount > 5) {
                provideHelpToGrandparents.setError(getString(R.string.error_number_0_to_5));
                focusView = provideHelpToGrandparents;
                success = false;
            } else {
                editDocument.setProvideHelpToGrandparents(provideHelpToGrandparentsCount);
            }
        } catch (Exception ex) {
            provideHelpToGrandparents.setError(getString(R.string.error_invalid_integer));
            focusView = provideHelpToGrandparents;
            success = false;
        }


        String sProvideHelpToOtherRelative = provideHelpToOtherRelative.getText().toString().trim();

        editDocument.setProvideHelpToOtherRelative(sProvideHelpToOtherRelative);


        String sProvideHelpToOther = provideHelpToOther.getText().toString().trim();
        editDocument.setProvideHelpToOther(sProvideHelpToOther);


        String sReasonNeedHelp = reasonNeedHelpView.getText().toString().trim();
        if (TextUtils.isEmpty(sReasonNeedHelp)) {
            reasonNeedHelpView.setError(getString(R.string.error_field_required));
            focusView = reasonNeedHelpView;
            success = false;
        } else {
            editDocument.setReasonNeedHelp(sReasonNeedHelp);
        }

        if (!caringSubstanceMisuse.isChecked() &&
                !caringPhysicalDisability.isChecked() &&
                !caringLearningDisability.isChecked() &&
                !caringLifeLimiting.isChecked() &&
                !caringMentalHealth.isChecked() &&
                !caringLGBT.isChecked() &&
                !caringTraveller.isChecked() &&
                !caringRural.isChecked() &&
                !caringEthnicMinority.isChecked()) {
            caringErrorText.setVisibility(View.VISIBLE);
            focusView = caringEthnicMinority;
            success = false;
        }

        // Check the question responses
        if (editDocument.getCleanOwnBedroom01().equals("Please select")) {
            TextView errorText = (TextView) cleanOwnBedroom01Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = cleanOwnBedroom01Spinner;
            success = false;
        }

        if (editDocument.getCleanOtherRooms02().equals("Please select")) {
            TextView errorText = (TextView) cleanOtherRooms02Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = cleanOtherRooms02Spinner;
            success = false;
        }

        if (editDocument.getWashDishes03().equals("Please select")) {
            TextView errorText = (TextView) washDishes03Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = washDishes03Spinner;
            success = false;
        }

        if (editDocument.getDecorateRoom04().equals("Please select")) {
            TextView errorText = (TextView) decorateRoom04Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = decorateRoom04Spinner;
            success = false;
        }

        if (editDocument.getResponsibilityForShopping05().equals("Please select")) {
            TextView errorText = (TextView) responsibilityForShopping05Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = responsibilityForShopping05Spinner;
            success = false;
        }

        if (editDocument.getCarryingHeavyThings06().equals("Please select")) {
            TextView errorText = (TextView) carryingHeavyThings06Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = carryingHeavyThings06Spinner;
            success = false;
        }

        if (editDocument.getBillsBankingBenefits07().equals("Please select")) {
            TextView errorText = (TextView) billsBankingBenefits07Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = billsBankingBenefits07Spinner;
            success = false;
        }

        if (editDocument.getWorkPartTime08().equals("Please select")) {
            TextView errorText = (TextView) workPartTime08Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = workPartTime08Spinner;
            success = false;
        }

        if (editDocument.getInterpretSignOther09().equals("Please select")) {
            TextView errorText = (TextView) interpretSignOther09Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = interpretSignOther09Spinner;
            success = false;
        }

        if (editDocument.getHelpDressUndress10().equals("Please select")) {
            TextView errorText = (TextView) helpDressUndress10Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = helpDressUndress10Spinner;
            success = false;
        }

        if (editDocument.getHelpWash11().equals("Please select")) {
            TextView errorText = (TextView) helpWash11Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = helpWash11Spinner;
            success = false;
        }

        if (editDocument.getHelpBathShower12().equals("Please select")) {
            TextView errorText = (TextView) helpBathShower12Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = helpBathShower12Spinner;
            success = false;
        }

        if (editDocument.getKeepPersonCompany13().equals("Please select")) {
            TextView errorText = (TextView) keepPersonCompany13Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = keepPersonCompany13Spinner;
            success = false;
        }

        if (editDocument.getMakeSureAlright14().equals("Please select")) {
            TextView errorText = (TextView) makeSureAlright14Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = makeSureAlright14Spinner;
            success = false;
        }

        if (editDocument.getTakeOut15().equals("Please select")) {
            TextView errorText = (TextView) takeOut15Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = takeOut15Spinner;
            success = false;
        }

        if (editDocument.getTakeSiblingsToSchool16().equals("Please select")) {
            TextView errorText = (TextView) takeSiblingsToSchool16Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = takeSiblingsToSchool16Spinner;
            success = false;
        }

        if (editDocument.getLookAfterSiblingsWithAdult17().equals("Please select")) {
            TextView errorText = (TextView) lookAfterSiblingsWithAdult17Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = lookAfterSiblingsWithAdult17Spinner;
            success = false;
        }

        if (editDocument.getLookAfterSiblingsOnOwn18().equals("Please select")) {
            TextView errorText = (TextView) lookAfterSiblingsOnOwn18Spinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = lookAfterSiblingsOnOwn18Spinner;
            success = false;
        }


        if (!success) {
            focusView.requestFocus();
            focusView.requestFocusFromTouch();
        }
        return success;
    }


    private String getHintText() {
        return "Once all fields have been completed, the score will be displayed. " +
                "If you wish to add some further text, please create a separate note " +
                "document headed: 'CAT - Further Information'. (Using a note document " +
                "ensures that the further information provided will be found using the " +
                "search facility.)";

    }

}
