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
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.ArrayList;

import solutions.cris.CRISActivity;
import solutions.cris.Login;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListComplexListItems;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.NoteType;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;

public class EditNoteType extends CRISActivity {

    private NoteType editNoteType;
    private LocalDB localDB;
    private User currentUser;
    private boolean newMode;
    private int noteIcon = NoteType.ICON_COLOUR_BLUE;
    // UI references.
    private EditText noteTypeNameView;
    private CheckBox isDisplayed;
    private CheckBox isDefault;
    private ImageView noteRedIcon;
    private ImageView noteAmberIcon;
    private ImageView noteGreenIcon;
    private ImageView noteBlueIcon;
    private CheckBox setSupervisorToFollowView;
    private EditText noteTypeTemplateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add the global uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        // CurrentUser always exists so if this check fails then exception in child
        // // has rendered system inconsistent so exit and let Main start from scratch()
        currentUser = User.getCurrentUser();
        if (currentUser == null) {
            finish();
        } else {
            localDB = LocalDB.getInstance();
            setContentView(R.layout.activity_edit_note_type);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            // Get the parameter passed with the Intent
            newMode = getIntent().getBooleanExtra(Main.EXTRA_IS_NEW_MODE, false);
            if (newMode) {
                editNoteType = (NoteType) getIntent().getSerializableExtra(Main.EXTRA_NOTE_TYPE);
                toolbar.setTitle(getString(R.string.app_name) + " - New Note Type");
            } else {
                int listPos = getIntent().getIntExtra(Main.EXTRA_LIST_POSITION, 0);
                editNoteType = (NoteType) ListComplexListItems.items.get(listPos);
                toolbar.setTitle(getString(R.string.app_name) + " - Edit Note Type");
            }
            setSupportActionBar(toolbar);

            // Set up the form.
            noteTypeNameView = (EditText) findViewById(R.id.note_type_name);
            isDisplayed = (CheckBox) findViewById(R.id.is_displayed);
            isDefault = (CheckBox) findViewById(R.id.is_default);
            noteRedIcon = (ImageView) findViewById(R.id.note_red_icon);
            noteAmberIcon = (ImageView) findViewById(R.id.note_amber_icon);
            noteGreenIcon = (ImageView) findViewById(R.id.note_green_icon);
            noteBlueIcon = (ImageView) findViewById(R.id.note_blue_icon);
            setSupervisorToFollowView = (CheckBox) findViewById(R.id.note_type_supervisor_set_to_follow) ;
            noteTypeTemplateView = (EditText) findViewById(R.id.note_type_template) ;

            // Initialise the status icons
            noteRedIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    noteIcon = NoteType.ICON_COLOUR_RED;
                    noteRedIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_red));
                    noteAmberIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_amber_grey));
                    noteGreenIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_green_grey));
                    noteBlueIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_blue_grey));
                }
            });
            // Initialise the status icons
            noteAmberIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    noteIcon = NoteType.ICON_COLOUR_AMBER;
                    noteRedIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_red_grey));
                    noteAmberIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_amber));
                    noteGreenIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_green_grey));
                    noteBlueIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_blue_grey));
                }
            });
            // Initialise the status icons
            noteGreenIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    noteIcon = NoteType.ICON_COLOUR_GREEN;
                    noteRedIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_red_grey));
                    noteAmberIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_amber_grey));
                    noteGreenIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_green));
                    noteBlueIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_blue_grey));
                }
            });
            // Initialise the status icons
            noteBlueIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    noteIcon = NoteType.ICON_COLOUR_BLUE;
                    noteRedIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_red_grey));
                    noteAmberIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_amber_grey));
                    noteGreenIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_green_grey));
                    noteBlueIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_blue));
                }
            });


            // Cancel Button
            Button cancelButton = (Button) findViewById(R.id.cancel_button);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
            // Save Button
            Button saveButton = (Button) findViewById(R.id.save_button);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (validate()) {
                        if (save()) {
                            finish();
                        }
                    }
                }
            });
        }
        // Load initial values
        isDisplayed.setChecked(editNoteType.isDisplayed());
        isDefault.setChecked(editNoteType.isDefault());
        if (editNoteType.getItemValue() != null) {
            noteTypeNameView.setText(editNoteType.getItemValue(), null);
            setSupervisorToFollowView.setChecked(editNoteType.isSupervisorSetToFollow());
            noteTypeTemplateView.setText(editNoteType.getTemplate(), null);
            noteRedIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_red_grey));
            noteAmberIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_amber_grey));
            noteGreenIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_green_grey));
            noteBlueIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_blue_grey));
            noteIcon = editNoteType.getNoteIcon();
            switch (noteIcon) {
                case NoteType.ICON_COLOUR_RED:
                    noteRedIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_red));
                    break;
                case NoteType.ICON_COLOUR_AMBER:
                    noteAmberIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_amber));
                    break;
                case NoteType.ICON_COLOUR_GREEN:
                    noteGreenIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_green));
                    break;
                default:
                    noteBlueIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_note_blue));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_empty, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    // Validate the document
    private boolean validate() {
        boolean success = true;

        // Clear any existing errors
        noteTypeNameView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        // Role Name
        String sNoteTypeName = noteTypeNameView.getText().toString().trim();
        if (TextUtils.isEmpty(sNoteTypeName)) {
            noteTypeNameView.setError(getString(R.string.error_field_required));
            focusView = noteTypeNameView;
            success = false;
        } else {
            editNoteType.setItemValue(sNoteTypeName);
        }

        editNoteType.setIsDisplayed(isDisplayed.isChecked());
        editNoteType.setIsDefault(isDefault.isChecked());

        editNoteType.setNoteIcon(noteIcon);
        editNoteType.setSupervisorSetToFollow(setSupervisorToFollowView.isChecked());
        editNoteType.setTemplate(noteTypeTemplateView.getText().toString());

        if (!success) {
            focusView.requestFocus();
            focusView.requestFocusFromTouch();
        }
        return success;
    }

    // Save the document
    private boolean save() {
        boolean success = true;
        try {
            if (newMode) {
                // Append the new user to the list of users
                ListComplexListItems.items.add(editNoteType);
            }
            editNoteType.save(newMode);
        } catch (SQLiteConstraintException ex) {
            // ItemValue was not unique
            noteTypeNameView.setError(getString(R.string.error_value_not_unique));
            noteTypeNameView.requestFocus();
            success = false;
        }
        // If this item has been set to the Default, remove any other default
        if (editNoteType.isDefault()){
            LocalDB localDB = LocalDB.getInstance();
            ArrayList<ListItem> items = localDB.getAllListItems(ListType.NOTE_TYPE.toString(), true);
            for (ListItem item:items){
                if (item.isDefault() && !item.getItemValue().equals(editNoteType.getItemValue())) {
                    item.setIsDefault(false);
                    item.save(false);
                }
            }
        }
        return success;
    }
}
