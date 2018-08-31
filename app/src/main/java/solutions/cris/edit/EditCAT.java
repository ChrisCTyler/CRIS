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
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.list.ListClientHeader;
import solutions.cris.object.Client;
import solutions.cris.object.CriteriaAssessmentTool;
import solutions.cris.object.Document;
import solutions.cris.object.User;

public class EditCAT extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private Client client;
    private CriteriaAssessmentTool editDocument;

    private EditText referenceDateView;
    private Spinner homeSituationSpinner;
    private Spinner childStatusSpinner;
    private Spinner typeOfSupportSpinner;
    private EditText personCaredForParentView;
    private EditText personCaredForSiblingView;
    private EditText personCaredForOtherView;
    private CheckBox tocDomestic1;
    private CheckBox tocDomestic2;
    private CheckBox tocPersonal;
    private CheckBox tocEmotional;
    private CheckBox tocSupervising;
    private EditText typeofConditionMentalHealthView;
    private EditText typeofConditionSubstanceMisuseView;
    private EditText typeofConditionAlcoholMisuseView;
    private EditText typeofConditionLearningDisabilityView;
    private EditText typeofConditionIllHealthView;
    private EditText typeofConditionPhysicalDisabilityView;
    private EditText typeofConditionAutismView;
    private EditText typeofConditionTerminalIllnessView;
    private Spinner frequencyOfCareSpinner;
    private Spinner frequencyOfSocialisingSpinner;
    private EditText scoreView;

    private LocalDB localDB;
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
        parent = inflater.inflate(R.layout.edit_cat, container, false);
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
            toolbar.setTitle(getString(R.string.app_name) + " - New Criteria Assessment Tool");
        } else {
            toolbar.setTitle(getString(R.string.app_name) + " - Edit Criteria Assessment Tool");
        }
        // Hide the FAB
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        fab.setVisibility(View.GONE);

        // Clear the footer
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        footer.setText("");

        // Set up the hint text
        hintTextView = (TextView) getActivity().findViewById(R.id.hint_text);
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
        client = ((ListActivity) getActivity()).getClient();
        editDocument = (CriteriaAssessmentTool) ((ListActivity) getActivity()).getDocument();
        localDB = LocalDB.getInstance();

        // Temp update of misssing values
        if (editDocument.getFrequencyOfCare() == null) {
            editDocument.setFrequencyOfCare("Please select");
        }
        if (editDocument.getFrequencyOfSocialising() == null) {
            editDocument.setFrequencyOfSocialising("Please select");
        }

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

        referenceDateView = (EditText) parent.findViewById(R.id.reference_date);
        referenceDateView.setInputType(InputType.TYPE_NULL);
        referenceDateView.setFocusable(false);
        homeSituationSpinner = (Spinner) parent.findViewById(R.id.home_situation_spinner);
        childStatusSpinner = (Spinner) parent.findViewById(R.id.child_status_spinner);
        typeOfSupportSpinner = (Spinner) parent.findViewById(R.id.type_of_support_spinner);
        personCaredForParentView = (EditText) parent.findViewById(R.id.person_cared_for_parents);
        personCaredForSiblingView = (EditText) parent.findViewById(R.id.person_cared_for_siblings);
        personCaredForOtherView = (EditText) parent.findViewById(R.id.person_cared_for_others);
        tocDomestic1 = (CheckBox) parent.findViewById(R.id.toc_domestic1);
        tocDomestic2 = (CheckBox) parent.findViewById(R.id.toc_domestic2);
        tocPersonal = (CheckBox) parent.findViewById(R.id.toc_personal);
        tocEmotional = (CheckBox) parent.findViewById(R.id.toc_emotional);
        tocSupervising = (CheckBox) parent.findViewById(R.id.toc_supervising);
        typeofConditionMentalHealthView = (EditText) parent.findViewById(R.id.type_of_condition_mental_health);
        typeofConditionSubstanceMisuseView = (EditText) parent.findViewById(R.id.type_of_condition_substance_misuse);
        typeofConditionAlcoholMisuseView = (EditText) parent.findViewById(R.id.type_of_condition_alcohol_misuse);
        typeofConditionLearningDisabilityView = (EditText) parent.findViewById(R.id.type_of_condition_learning_disability);
        typeofConditionIllHealthView = (EditText) parent.findViewById(R.id.type_of_condition_ill_health);
        typeofConditionPhysicalDisabilityView = (EditText) parent.findViewById(R.id.type_of_condition_physical_disability);
        typeofConditionAutismView = (EditText) parent.findViewById(R.id.type_of_condition_autism);
        typeofConditionTerminalIllnessView = (EditText) parent.findViewById(R.id.type_of_condition_terminal_illness);
        frequencyOfCareSpinner = (Spinner) parent.findViewById(R.id.frequency_of_care_spinner);
        frequencyOfSocialisingSpinner = (Spinner) parent.findViewById(R.id.frequency_of_socialising_spinner);
        scoreView = (EditText) parent.findViewById(R.id.score);
        scoreView.setInputType(InputType.TYPE_NULL);
        scoreView.setFocusable(false);

        // Initialise the Home Situation Spinner
        final ArrayAdapter<String> homeSituationAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, CriteriaAssessmentTool.homeSituationValues);
        homeSituationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        homeSituationSpinner.setAdapter(homeSituationAdapter);
        homeSituationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setHomeSituation(homeSituationSpinner.getSelectedItem().toString());
                homeSituationSpinner.requestFocus();
                homeSituationSpinner.requestFocusFromTouch();
                checkScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setHomeSituation(homeSituationSpinner.getSelectedItem().toString());
                homeSituationSpinner.requestFocus();
                homeSituationSpinner.requestFocusFromTouch();
                checkScore();
            }
        });

        // Initialise the Child Status Spinner
        final ArrayAdapter<String> childStatusAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, CriteriaAssessmentTool.childStatusValues);
        childStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        childStatusSpinner.setAdapter(childStatusAdapter);
        childStatusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setChildStatus(childStatusSpinner.getSelectedItem().toString());
                childStatusSpinner.requestFocus();
                childStatusSpinner.requestFocusFromTouch();
                checkScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setChildStatus(childStatusSpinner.getSelectedItem().toString());
                childStatusSpinner.requestFocus();
                childStatusSpinner.requestFocusFromTouch();
                checkScore();
            }
        });

        // Initialise the Type of Support Spinner
        final ArrayAdapter<String> typeOfSupportAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, CriteriaAssessmentTool.typeOfSupportValues);
        typeOfSupportAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeOfSupportSpinner.setAdapter(typeOfSupportAdapter);
        typeOfSupportSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setTypeOfSupport(typeOfSupportSpinner.getSelectedItem().toString());
                typeOfSupportSpinner.requestFocus();
                typeOfSupportSpinner.requestFocusFromTouch();
                checkScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setTypeOfSupport(typeOfSupportSpinner.getSelectedItem().toString());
                typeOfSupportSpinner.requestFocus();
                typeOfSupportSpinner.requestFocusFromTouch();
                checkScore();
            }
        });

        personCaredForParentView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(personCaredForParentView.getText().toString());
                    if (newValue == -1) {
                        personCaredForParentView.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        personCaredForParentView.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getPersonCaredForParent() != newValue) {
                        editDocument.setPersonCaredForParent(newValue);
                        checkScore();
                    }
                }
            }
        });

        personCaredForSiblingView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(personCaredForSiblingView.getText().toString());
                    if (newValue == -1) {
                        personCaredForSiblingView.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        personCaredForSiblingView.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getPersonCaredForSibling() != newValue) {
                        editDocument.setPersonCaredForSibling(newValue);
                        checkScore();
                    }
                }
            }
        });

        personCaredForOtherView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(personCaredForOtherView.getText().toString());
                    if (newValue == -1) {
                        personCaredForOtherView.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        personCaredForOtherView.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getPersonCaredForOther() != newValue) {
                        editDocument.setPersonCaredForOther(newValue);
                        checkScore();
                    }
                }
            }
        });

        tocDomestic1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setTypeOfCareDomestic1(b);
                // Request focus to ensure end focus called on edit text fields to register updates
                compoundButton.requestFocus();
                compoundButton.requestFocusFromTouch();
                checkScore();
            }
        });

        tocDomestic2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setTypeOfCareDomestic2(b);
                // Request focus to ensure end focus called on edit text fields to register updates
                compoundButton.requestFocus();
                compoundButton.requestFocusFromTouch();
                checkScore();
            }
        });

        tocPersonal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setTypeOfCarePersonal(b);
                // Request focus to ensure end focus called on edit text fields to register updates
                compoundButton.requestFocus();
                compoundButton.requestFocusFromTouch();
                checkScore();
            }
        });

        tocEmotional.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setTypeOfCareEmotional(b);
                // Request focus to ensure end focus called on edit text fields to register updates
                compoundButton.requestFocus();
                compoundButton.requestFocusFromTouch();
                checkScore();
            }
        });

        tocSupervising.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editDocument.setTypeOfCareSupervising(b);
                // Request focus to ensure end focus called on edit text fields to register updates
                compoundButton.requestFocus();
                compoundButton.requestFocusFromTouch();
                checkScore();
            }
        });

        typeofConditionMentalHealthView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(typeofConditionMentalHealthView.getText().toString());
                    if (newValue == -1) {
                        typeofConditionMentalHealthView.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        typeofConditionMentalHealthView.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getTypeOfConditionMentalHealth() != newValue) {
                        editDocument.setTypeOfConditionMentalHealth(newValue);
                        checkScore();
                    }
                }
            }
        });
        typeofConditionSubstanceMisuseView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(typeofConditionSubstanceMisuseView.getText().toString());
                    if (newValue == -1) {
                        typeofConditionSubstanceMisuseView.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        typeofConditionSubstanceMisuseView.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getTypeOfConditionSubstanceMisuse() != newValue) {
                        editDocument.setTypeOfConditionSubstanceMisuse(newValue);
                        checkScore();
                    }
                }
            }
        });
        typeofConditionAlcoholMisuseView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(typeofConditionAlcoholMisuseView.getText().toString());
                    if (newValue == -1) {
                        typeofConditionAlcoholMisuseView.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        typeofConditionAlcoholMisuseView.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getTypeOfConditionAlcoholMisuse() != newValue) {
                        editDocument.setTypeOfConditionAlcoholMisuse(newValue);
                        checkScore();
                    }
                }
            }
        });
        typeofConditionLearningDisabilityView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(typeofConditionLearningDisabilityView.getText().toString());
                    if (newValue == -1) {
                        typeofConditionLearningDisabilityView.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        typeofConditionLearningDisabilityView.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getTypeOfConditionLearningDisability() != newValue) {
                        editDocument.setTypeOfConditionLearningDisability(newValue);
                        checkScore();
                    }
                }
            }
        });
        typeofConditionIllHealthView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(typeofConditionIllHealthView.getText().toString());
                    if (newValue == -1) {
                        typeofConditionIllHealthView.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        typeofConditionIllHealthView.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getTypeOfConditionIllHealth() != newValue) {
                        editDocument.setTypeOfConditionIllHealth(newValue);
                        checkScore();
                    }
                }
            }
        });
        typeofConditionPhysicalDisabilityView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(typeofConditionPhysicalDisabilityView.getText().toString());
                    if (newValue == -1) {
                        typeofConditionPhysicalDisabilityView.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        typeofConditionPhysicalDisabilityView.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getTypeOfConditionPhysicalDisability() != newValue) {
                        editDocument.setTypeOfConditionPhysicalDisability(newValue);
                        checkScore();
                    }
                }
            }
        });
        typeofConditionAutismView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(typeofConditionAutismView.getText().toString());
                    if (newValue == -1) {
                        typeofConditionAutismView.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        typeofConditionAutismView.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getTypeOfConditionAutism() != newValue) {
                        editDocument.setTypeOfConditionAutism(newValue);
                        checkScore();
                    }
                }
            }
        });
        typeofConditionTerminalIllnessView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    int newValue = getInt(typeofConditionTerminalIllnessView.getText().toString());
                    if (newValue == -1) {
                        typeofConditionTerminalIllnessView.setError(getString(R.string.error_number_0_to_5));
                    } else if (newValue == -2) {
                        typeofConditionTerminalIllnessView.setError(getString(R.string.error_invalid_integer));
                    } else if (editDocument.getTypeOfConditionTerminalIllness() != newValue) {
                        editDocument.setTypeOfConditionTerminalIllness(newValue);
                        checkScore();
                    }
                }
            }
        });

        // Initialise the Frequency of Care Spinner
        final ArrayAdapter<String> frequencyOfCareAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, CriteriaAssessmentTool.frequencyOfCareValues);
        frequencyOfCareAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencyOfCareSpinner.setAdapter(frequencyOfCareAdapter);
        frequencyOfCareSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setFrequencyOfCare(frequencyOfCareSpinner.getSelectedItem().toString());
                frequencyOfCareSpinner.requestFocus();
                frequencyOfCareSpinner.requestFocusFromTouch();
                checkScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setFrequencyOfCare(frequencyOfCareSpinner.getSelectedItem().toString());
                frequencyOfCareSpinner.requestFocus();
                frequencyOfCareSpinner.requestFocusFromTouch();
                checkScore();
            }
        });

        // Initialise the Frequency Of Socialising Spinner
        final ArrayAdapter<String> frequencyOfSocialisingAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, CriteriaAssessmentTool.frequencyOfSocialisingValues);
        frequencyOfSocialisingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencyOfSocialisingSpinner.setAdapter(frequencyOfSocialisingAdapter);
        frequencyOfSocialisingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDocument.setFrequencyOfSocialising(frequencyOfSocialisingSpinner.getSelectedItem().toString());
                frequencyOfSocialisingSpinner.requestFocus();
                frequencyOfSocialisingSpinner.requestFocusFromTouch();
                checkScore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                editDocument.setFrequencyOfSocialising(frequencyOfSocialisingSpinner.getSelectedItem().toString());
                frequencyOfSocialisingSpinner.requestFocus();
                frequencyOfSocialisingSpinner.requestFocusFromTouch();
                checkScore();
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

        // Set Defaults
        Date today = new Date();


        if (isNewMode) {
            referenceDateView.setText(sDate.format(editDocument.getCreationDate()));
            scoreView.setText(String.format(Locale.UK, "%d", editDocument.getScore()));
            personCaredForParentView.setText("0");
            personCaredForSiblingView.setText("0");
            personCaredForOtherView.setText("0");
            typeofConditionMentalHealthView.setText("0");
            typeofConditionSubstanceMisuseView.setText("0");
            typeofConditionAlcoholMisuseView.setText("0");
            typeofConditionLearningDisabilityView.setText("0");
            typeofConditionIllHealthView.setText("0");
            typeofConditionPhysicalDisabilityView.setText("0");
            typeofConditionAutismView.setText("0");
            typeofConditionTerminalIllnessView.setText("0");

        } else {
            // Edit Mode
            referenceDateView.setText(sDate.format(editDocument.getReferenceDate()));
            homeSituationSpinner.setSelection(homeSituationAdapter.getPosition(editDocument.getHomeSituation()));
            childStatusSpinner.setSelection(childStatusAdapter.getPosition(editDocument.getChildStatus()));
            typeOfSupportSpinner.setSelection(typeOfSupportAdapter.getPosition(editDocument.getTypeOfSupport()));
            personCaredForParentView.setText(String.format(Locale.UK, "%d", editDocument.getPersonCaredForParent()));
            personCaredForSiblingView.setText(String.format(Locale.UK, "%d", editDocument.getPersonCaredForSibling()));
            personCaredForOtherView.setText(String.format(Locale.UK, "%d", editDocument.getPersonCaredForOther()));
            tocDomestic1.setChecked(editDocument.getTypeOfCareDomestic1());
            tocDomestic2.setChecked(editDocument.getTypeOfCareDomestic2());
            tocPersonal.setChecked(editDocument.getTypeOfCarePersonal());
            tocEmotional.setChecked(editDocument.getTypeOfCareEmotional());
            tocSupervising.setChecked(editDocument.getTypeOfCareSupervising());
            typeofConditionMentalHealthView.setText(String.format(Locale.UK, "%d", editDocument.getTypeOfConditionMentalHealth()));
            typeofConditionSubstanceMisuseView.setText(String.format(Locale.UK, "%d", editDocument.getTypeOfConditionSubstanceMisuse()));
            typeofConditionAlcoholMisuseView.setText(String.format(Locale.UK, "%d", editDocument.getTypeOfConditionAlcoholMisuse()));
            typeofConditionLearningDisabilityView.setText(String.format(Locale.UK, "%d", editDocument.getTypeOfConditionLearningDisability()));
            typeofConditionIllHealthView.setText(String.format(Locale.UK, "%d", editDocument.getTypeOfConditionIllHealth()));
            typeofConditionPhysicalDisabilityView.setText(String.format(Locale.UK, "%d", editDocument.getTypeOfConditionPhysicalDisability()));
            typeofConditionAutismView.setText(String.format(Locale.UK, "%d", editDocument.getTypeOfConditionAutism()));
            typeofConditionTerminalIllnessView.setText(String.format(Locale.UK, "%d", editDocument.getTypeOfConditionTerminalIllness()));
            frequencyOfCareSpinner.setSelection(frequencyOfCareAdapter.getPosition(editDocument.getFrequencyOfCare()));
            frequencyOfSocialisingSpinner.setSelection(frequencyOfSocialisingAdapter.getPosition(editDocument.getFrequencyOfSocialising()));
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
        int value = -1;
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

    private void checkScore() {
        if (!editDocument.getHomeSituation().equals("Please select") &&
                !editDocument.getChildStatus().equals("Please select") &&
                !editDocument.getTypeOfSupport().equals("Please select") &&
                !editDocument.getFrequencyOfCare().equals("Please select") &&
                !editDocument.getFrequencyOfSocialising().equals("Please select")) {
            editDocument.setScore(client);
            scoreView.setText(String.format(Locale.UK, "%d", editDocument.getScore()));
        } else {
            scoreView.setText("0");
        }

    }

    // Validate the document
    private boolean validate() {
        boolean success = true;

        personCaredForParentView.setError(null);
        personCaredForSiblingView.setError(null);
        personCaredForOtherView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        //Home Situation
        if (editDocument.getHomeSituation().equals("Please select")) {
            TextView errorText = (TextView) homeSituationSpinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = homeSituationSpinner;
            success = false;
        }

        //Child Status
        if (editDocument.getChildStatus().equals("Please select")) {
            TextView errorText = (TextView) childStatusSpinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = childStatusSpinner;
            success = false;
        }

        // PersonCareForParent
        String sPersonCareForParent = personCaredForParentView.getText().toString().trim();
        if (TextUtils.isEmpty(sPersonCareForParent)) {
            personCaredForParentView.setError(getString(R.string.error_field_required));
            focusView = personCaredForParentView;
            success = false;
        } else {
            int personCaredForParent;
            try {
                personCaredForParent = Integer.parseInt(sPersonCareForParent);
                if (personCaredForParent < 0 || personCaredForParent > 5) {
                    personCaredForParentView.setError(getString(R.string.error_number_0_to_5));
                    focusView = personCaredForParentView;
                    success = false;
                } else {
                    editDocument.setPersonCaredForParent(personCaredForParent);
                }
            } catch (Exception ex) {
                personCaredForParentView.setError(getString(R.string.error_invalid_integer));
                focusView = personCaredForParentView;
                success = false;
            }
        }

        // PersonCareForSibling
        String sPersonCareForSibling = personCaredForSiblingView.getText().toString().trim();
        if (TextUtils.isEmpty(sPersonCareForSibling)) {
            personCaredForSiblingView.setError(getString(R.string.error_field_required));
            focusView = personCaredForSiblingView;
            success = false;
        } else {
            int personCaredForSibling;
            try {
                personCaredForSibling = Integer.parseInt(sPersonCareForSibling);
                if (personCaredForSibling < 0 || personCaredForSibling > 5) {
                    personCaredForSiblingView.setError(getString(R.string.error_number_0_to_5));
                    focusView = personCaredForSiblingView;
                    success = false;
                } else {
                    editDocument.setPersonCaredForSibling(personCaredForSibling);
                }
            } catch (Exception ex) {
                personCaredForSiblingView.setError(getString(R.string.error_invalid_integer));
                focusView = personCaredForSiblingView;
                success = false;
            }
        }

        // PersonCareForOther
        String sPersonCareForOther = personCaredForOtherView.getText().toString().trim();
        if (TextUtils.isEmpty(sPersonCareForOther)) {
            personCaredForOtherView.setError(getString(R.string.error_field_required));
            focusView = personCaredForOtherView;
            success = false;
        } else {
            int personCaredForOther;
            try {
                personCaredForOther = Integer.parseInt(sPersonCareForOther);
                if (personCaredForOther < 0 || personCaredForOther > 5) {
                    personCaredForOtherView.setError(getString(R.string.error_number_0_to_5));
                    focusView = personCaredForOtherView;
                    success = false;
                } else {
                    editDocument.setPersonCaredForOther(personCaredForOther);
                }
            } catch (Exception ex) {
                personCaredForOtherView.setError(getString(R.string.error_invalid_integer));
                focusView = personCaredForOtherView;
                success = false;
            }
        }

        //TypeOfSupport
        if (editDocument.getTypeOfSupport().equals("Please select")) {
            TextView errorText = (TextView) typeOfSupportSpinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = typeOfSupportSpinner;
            success = false;
        }

        // TypeOfConditionMentalHealth
        String sTypeOfConditionMentalHealth = typeofConditionMentalHealthView.getText().toString().trim();
        if (TextUtils.isEmpty(sTypeOfConditionMentalHealth)) {
            typeofConditionMentalHealthView.setError(getString(R.string.error_field_required));
            focusView = typeofConditionMentalHealthView;
            success = false;
        } else {
            int typeOfConditionMentalHealth;
            try {
                typeOfConditionMentalHealth = Integer.parseInt(sTypeOfConditionMentalHealth);
                if (typeOfConditionMentalHealth < 0 || typeOfConditionMentalHealth > 5) {
                    typeofConditionMentalHealthView.setError(getString(R.string.error_number_0_to_5));
                    focusView = typeofConditionMentalHealthView;
                    success = false;
                } else {
                    editDocument.setTypeOfConditionMentalHealth(typeOfConditionMentalHealth);
                }
            } catch (Exception ex) {
                typeofConditionMentalHealthView.setError(getString(R.string.error_invalid_integer));
                focusView = typeofConditionMentalHealthView;
                success = false;
            }
        }

        // TypeOfConditionSubstanceMisuse
        String sTypeOfConditionSubstanceMisuse = typeofConditionSubstanceMisuseView.getText().toString().trim();
        if (TextUtils.isEmpty(sTypeOfConditionSubstanceMisuse)) {
            typeofConditionSubstanceMisuseView.setError(getString(R.string.error_field_required));
            focusView = typeofConditionSubstanceMisuseView;
            success = false;
        } else {
            int typeOfConditionSubstanceMisuse;
            try {
                typeOfConditionSubstanceMisuse = Integer.parseInt(sTypeOfConditionSubstanceMisuse);
                if (typeOfConditionSubstanceMisuse < 0 || typeOfConditionSubstanceMisuse > 5) {
                    typeofConditionSubstanceMisuseView.setError(getString(R.string.error_number_0_to_5));
                    focusView = typeofConditionSubstanceMisuseView;
                    success = false;
                } else {
                    editDocument.setTypeOfConditionSubstanceMisuse(typeOfConditionSubstanceMisuse);
                }
            } catch (Exception ex) {
                typeofConditionSubstanceMisuseView.setError(getString(R.string.error_invalid_integer));
                focusView = typeofConditionSubstanceMisuseView;
                success = false;
            }
        }

        // TypeOfConditionAlcoholMisuse
        String sTypeOfConditionAlcoholMisuse = typeofConditionAlcoholMisuseView.getText().toString().trim();
        if (TextUtils.isEmpty(sTypeOfConditionAlcoholMisuse)) {
            typeofConditionAlcoholMisuseView.setError(getString(R.string.error_field_required));
            focusView = typeofConditionAlcoholMisuseView;
            success = false;
        } else {
            int typeOfConditionAlcoholMisuse;
            try {
                typeOfConditionAlcoholMisuse = Integer.parseInt(sTypeOfConditionAlcoholMisuse);
                if (typeOfConditionAlcoholMisuse < 0 || typeOfConditionAlcoholMisuse > 5) {
                    typeofConditionAlcoholMisuseView.setError(getString(R.string.error_number_0_to_5));
                    focusView = typeofConditionAlcoholMisuseView;
                    success = false;
                } else {
                    editDocument.setTypeOfConditionAlcoholMisuse(typeOfConditionAlcoholMisuse);
                }
            } catch (Exception ex) {
                typeofConditionAlcoholMisuseView.setError(getString(R.string.error_invalid_integer));
                focusView = typeofConditionAlcoholMisuseView;
                success = false;
            }
        }

        // TypeOfConditionLearningDisability
        String sTypeOfConditionLearningDisability = typeofConditionLearningDisabilityView.getText().toString().trim();
        if (TextUtils.isEmpty(sTypeOfConditionLearningDisability)) {
            typeofConditionLearningDisabilityView.setError(getString(R.string.error_field_required));
            focusView = typeofConditionLearningDisabilityView;
            success = false;
        } else {
            int typeOfConditionLearningDisability;
            try {
                typeOfConditionLearningDisability = Integer.parseInt(sTypeOfConditionLearningDisability);
                if (typeOfConditionLearningDisability < 0 || typeOfConditionLearningDisability > 5) {
                    typeofConditionLearningDisabilityView.setError(getString(R.string.error_number_0_to_5));
                    focusView = typeofConditionLearningDisabilityView;
                    success = false;
                } else {
                    editDocument.setTypeOfConditionLearningDisability(typeOfConditionLearningDisability);
                }
            } catch (Exception ex) {
                typeofConditionLearningDisabilityView.setError(getString(R.string.error_invalid_integer));
                focusView = typeofConditionLearningDisabilityView;
                success = false;
            }
        }

        // TypeOfConditionIllHealth
        String sTypeOfConditionIllHealth = typeofConditionIllHealthView.getText().toString().trim();
        if (TextUtils.isEmpty(sTypeOfConditionIllHealth)) {
            typeofConditionIllHealthView.setError(getString(R.string.error_field_required));
            focusView = typeofConditionIllHealthView;
            success = false;
        } else {
            int typeOfConditionIllHealth;
            try {
                typeOfConditionIllHealth = Integer.parseInt(sTypeOfConditionIllHealth);
                if (typeOfConditionIllHealth < 0 || typeOfConditionIllHealth > 5) {
                    typeofConditionIllHealthView.setError(getString(R.string.error_number_0_to_5));
                    focusView = typeofConditionIllHealthView;
                    success = false;
                } else {
                    editDocument.setTypeOfConditionIllHealth(typeOfConditionIllHealth);
                }
            } catch (Exception ex) {
                typeofConditionIllHealthView.setError(getString(R.string.error_invalid_integer));
                focusView = typeofConditionIllHealthView;
                success = false;
            }
        }

        // TypeOfConditionPhysicalDisability
        String sTypeOfConditionPhysicalDisability = typeofConditionPhysicalDisabilityView.getText().toString().trim();
        if (TextUtils.isEmpty(sTypeOfConditionPhysicalDisability)) {
            typeofConditionPhysicalDisabilityView.setError(getString(R.string.error_field_required));
            focusView = typeofConditionPhysicalDisabilityView;
            success = false;
        } else {
            int typeOfConditionPhysicalDisability;
            try {
                typeOfConditionPhysicalDisability = Integer.parseInt(sTypeOfConditionPhysicalDisability);
                if (typeOfConditionPhysicalDisability < 0 || typeOfConditionPhysicalDisability > 5) {
                    typeofConditionPhysicalDisabilityView.setError(getString(R.string.error_number_0_to_5));
                    focusView = typeofConditionPhysicalDisabilityView;
                    success = false;
                } else {
                    editDocument.setTypeOfConditionPhysicalDisability(typeOfConditionPhysicalDisability);
                }
            } catch (Exception ex) {
                typeofConditionPhysicalDisabilityView.setError(getString(R.string.error_invalid_integer));
                focusView = typeofConditionPhysicalDisabilityView;
                success = false;
            }
        }

        // TypeOfConditionAutism
        String sTypeOfConditionAutism = typeofConditionAutismView.getText().toString().trim();
        if (TextUtils.isEmpty(sTypeOfConditionAutism)) {
            typeofConditionAutismView.setError(getString(R.string.error_field_required));
            focusView = typeofConditionAutismView;
            success = false;
        } else {
            int typeOfConditionAutism;
            try {
                typeOfConditionAutism = Integer.parseInt(sTypeOfConditionAutism);
                if (typeOfConditionAutism < 0 || typeOfConditionAutism > 5) {
                    typeofConditionAutismView.setError(getString(R.string.error_number_0_to_5));
                    focusView = typeofConditionAutismView;
                    success = false;
                } else {
                    editDocument.setTypeOfConditionAutism(typeOfConditionAutism);
                }
            } catch (Exception ex) {
                typeofConditionAutismView.setError(getString(R.string.error_invalid_integer));
                focusView = typeofConditionAutismView;
                success = false;
            }
        }

        // TypeOfConditionTerminalIllness
        String sTypeOfConditionTerminalIllness = typeofConditionTerminalIllnessView.getText().toString().trim();
        if (TextUtils.isEmpty(sTypeOfConditionTerminalIllness)) {
            typeofConditionTerminalIllnessView.setError(getString(R.string.error_field_required));
            focusView = typeofConditionTerminalIllnessView;
            success = false;
        } else {
            int typeOfConditionTerminalIllness;
            try {
                typeOfConditionTerminalIllness = Integer.parseInt(sTypeOfConditionTerminalIllness);
                if (typeOfConditionTerminalIllness < 0 || typeOfConditionTerminalIllness > 5) {
                    typeofConditionTerminalIllnessView.setError(getString(R.string.error_number_0_to_5));
                    focusView = typeofConditionTerminalIllnessView;
                    success = false;
                } else {
                    editDocument.setTypeOfConditionTerminalIllness(typeOfConditionTerminalIllness);
                }
            } catch (Exception ex) {
                typeofConditionTerminalIllnessView.setError(getString(R.string.error_invalid_integer));
                focusView = typeofConditionTerminalIllnessView;
                success = false;
            }
        }

        //FrequencyOfCare
        if (editDocument.getFrequencyOfCare().equals("Please select")) {
            TextView errorText = (TextView) frequencyOfCareSpinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = frequencyOfCareSpinner;
            success = false;
        }

        //FrequencyOfSocialising
        if (editDocument.getFrequencyOfSocialising().equals("Please select")) {
            TextView errorText = (TextView) frequencyOfSocialisingSpinner.getSelectedView();
            errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
            focusView = frequencyOfSocialisingSpinner;
            success = false;
        }

        if (success) {
            // In case save is clicked whilst in an Edit Text so loss of focus is not triggered
            editDocument.setScore(client);
        } else {
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
