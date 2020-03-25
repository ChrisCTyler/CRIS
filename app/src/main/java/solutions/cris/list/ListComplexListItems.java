package solutions.cris.list;
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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.UUID;

import solutions.cris.CRISActivity;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.EditAgency;
import solutions.cris.edit.EditGroup;
import solutions.cris.edit.EditNoteType;
import solutions.cris.edit.EditRole;
import solutions.cris.edit.EditSchool;
import solutions.cris.edit.EditTransportOrganisation;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Agency;
import solutions.cris.object.Group;
import solutions.cris.object.ListItem;
import solutions.cris.object.NoteType;
import solutions.cris.object.Role;
import solutions.cris.object.School;
import solutions.cris.object.TransportOrganisation;
import solutions.cris.object.User;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.utils.ExceptionHandler;
import solutions.cris.utils.LocalSettings;
import solutions.cris.utils.SwipeDetector;

public class ListComplexListItems extends CRISActivity {

    private User currentUser;
    private ListView listView;
    public static ArrayList<ListItem> items;
    private String listType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CurrentUser always exists so if this check fails then exception in child
        // // has rendered system inconsistent so exit and let Main start from scratch()
        currentUser = User.getCurrentUser();
        if (currentUser == null) {
            finish();
        } else {
            // Add the global uncaught exception handler
            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
            setContentView(R.layout.activity_list_with_action);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            // ListType is passed as an extra
            listType = getIntent().getStringExtra(Main.EXTRA_LIST_TYPE);
            toolbar.setTitle(getString(R.string.app_name) + " - " + listType);
            setSupportActionBar(toolbar);

            // Initialise the list view
            this.listView = (ListView) findViewById(R.id.list_view);
            final SwipeDetector swipeDetector = new SwipeDetector();
            this.listView.setOnTouchListener(swipeDetector);
            this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (swipeDetector.swipeDetected()){
                        displayListItemHistory(position, swipeDetector.getAction());
                    } else {
                    doListItem(position);
                    }
                }
            });

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        doNewAction();
                    }
                });
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        // Load the set of List Items
        LocalDB localDB = LocalDB.getInstance();
        items = localDB.getAllListItems(listType, true);
        ListItemAdapter adapter = new ListItemAdapter(this, items);
        this.listView.setAdapter(adapter);
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

    private void doNewAction(){
        Intent intent;
        switch (listType) {
            case "AGENCY":
                Agency newAgency = new Agency(currentUser, "", 0);
                intent = new Intent(this, EditAgency.class);
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, true);
                intent.putExtra(Main.EXTRA_AGENCY, newAgency);
                startActivity(intent);
                break;
            case "NOTE_TYPE":
                NoteType newNoteType = new NoteType(currentUser, "", 0);
                intent = new Intent(this, EditNoteType.class);
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, true);
                intent.putExtra(Main.EXTRA_NOTE_TYPE, newNoteType);
                startActivity(intent);
                break;
            case "GROUP":
                Group newGroup = new Group(currentUser, "", 0);
                intent = new Intent(this, EditGroup.class);
                // User is serializable so can pass as extra to EditUser Activity
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, true);
                intent.putExtra(Main.EXTRA_GROUP, newGroup);
                startActivity(intent);
                break;
            case "ROLE":
                Role newRole = new Role(currentUser, "", 0);
                intent = new Intent(this, EditRole.class);
                // User is serializable so can pass as extra to EditUser Activity
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, true);
                intent.putExtra(Main.EXTRA_ROLE, newRole);
                startActivity(intent);
                break;
            case "SCHOOL":
                School newSchool = new School(currentUser, "", 0);
                intent = new Intent(this, EditSchool.class);
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, true);
                intent.putExtra(Main.EXTRA_SCHOOL, newSchool);
                startActivity(intent);
                break;
            case "TRANSPORT_ORGANISATION":
                TransportOrganisation newTransportOrganisation = new TransportOrganisation(currentUser, "", 0);
                intent = new Intent(this, EditTransportOrganisation.class);
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, true);
                intent.putExtra(Main.EXTRA_TRANSPORT_ORGANISATION, newTransportOrganisation);
                startActivity(intent);
                break;
            default:
                throw new CRISException("Unexpected listType: " + listType);
        }
    }

    private void doListItem(int position) {
        switch (listType) {
            case "AGENCY":
                Intent intent = new Intent(this, EditAgency.class);
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, false);
                intent.putExtra(Main.EXTRA_LIST_POSITION, position);
                startActivity(intent);
                break;
            case "NOTE_TYPE":
                intent = new Intent(this, EditNoteType.class);
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, false);
                intent.putExtra(Main.EXTRA_LIST_POSITION, position);
                startActivity(intent);
                break;
            case "GROUP":
                intent = new Intent(this, EditGroup.class);
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, false);
                intent.putExtra(Main.EXTRA_LIST_POSITION, position);
                startActivity(intent);
                break;
            case "ROLE":
                intent = new Intent(this, EditRole.class);
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, false);
                intent.putExtra(Main.EXTRA_LIST_POSITION, position);
                startActivity(intent);
                break;
            case "SCHOOL":
                intent = new Intent(this, EditSchool.class);
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, false);
                intent.putExtra(Main.EXTRA_LIST_POSITION, position);
                startActivity(intent);
                break;
            case "TRANSPORT_ORGANISATION":
                intent = new Intent(this, EditTransportOrganisation.class);
                intent.putExtra(Main.EXTRA_IS_NEW_MODE, false);
                intent.putExtra(Main.EXTRA_LIST_POSITION, position);
                startActivity(intent);
                break;
            default:
                throw new CRISException("Unexpected listType: " + listType);
        }

    }

    private void displayListItemHistory(int position, SwipeDetector.Action action){
        ListItem item = items.get(position);
        // Call here to ensure instance exists in getChanges/textSummary
        LocalSettings.getInstance(this);
        //Loop through all instances of the document gathering data
        String history = "";
        LocalDB localDB = LocalDB.getInstance();
        ArrayList<UUID> recordIDs = localDB.getRecordIDs(item);
        switch (listType) {
            case "AGENCY":
                for (int i=0; i < recordIDs.size(); i++){
                    boolean isEarliest = (i == recordIDs.size()-1);
                    history += localDB.getListItemMetaData(recordIDs.get(i), isEarliest, action);
                    if (!isEarliest){
                        history += Agency.getChanges(localDB, recordIDs.get(i+1), recordIDs.get(i), action);
                    }
                }
                history += String.format("\nThe current List Item contents are:\n\n%s\n",
                    ((Agency) item).textSummary());
                break;
            case "NOTE_TYPE":
                for (int i=0; i < recordIDs.size(); i++){
                    boolean isEarliest = (i == recordIDs.size()-1);
                    history += localDB.getListItemMetaData(recordIDs.get(i), isEarliest, action);
                    if (!isEarliest){
                        history += NoteType.getChanges(localDB, recordIDs.get(i+1), recordIDs.get(i), action);
                    }
                }
                history += String.format("\nThe current List Item contents are:\n\n%s\n",
                        ((NoteType) item).textSummary());
                break;
            case "GROUP":
                for (int i=0; i < recordIDs.size(); i++){
                    boolean isEarliest = (i == recordIDs.size()-1);
                    history += localDB.getListItemMetaData(recordIDs.get(i), isEarliest, action);
                    if (!isEarliest){
                        history += Group.getChanges(localDB, recordIDs.get(i+1), recordIDs.get(i), action);
                    }
                }
                history += String.format("\nThe current List Item contents are:\n\n%s\n",
                        ((Group) item).textSummary());
                break;
            case "ROLE":
                for (int i=0; i < recordIDs.size(); i++){
                    boolean isEarliest = (i == recordIDs.size()-1);
                    history += localDB.getListItemMetaData(recordIDs.get(i), isEarliest, action);
                    if (!isEarliest){
                        history += Role.getChanges(localDB, recordIDs.get(i+1), recordIDs.get(i), action);
                    }
                }
                history += String.format("\nThe current List Item contents are:\n\n%s\n",
                        ((Role) item).textSummary());
                break;
            case "SCHOOL":
                for (int i=0; i < recordIDs.size(); i++){
                    boolean isEarliest = (i == recordIDs.size()-1);
                    history += localDB.getListItemMetaData(recordIDs.get(i), isEarliest, action);
                    if (!isEarliest){
                        history += School.getChanges(localDB, recordIDs.get(i+1), recordIDs.get(i), action);
                    }
                }
                history += String.format("\nThe current List Item contents are:\n\n%s\n",
                        ((School) item).textSummary());
                break;
            case "TRANSPORT_ORGANISATION":
                for (int i=0; i < recordIDs.size(); i++){
                    boolean isEarliest = (i == recordIDs.size()-1);
                    history += localDB.getListItemMetaData(recordIDs.get(i), isEarliest, action);
                    if (!isEarliest){
                        history += TransportOrganisation.getChanges(localDB, recordIDs.get(i+1), recordIDs.get(i), action);
                    }
                }
                history += String.format("\nThe current List Item contents are:\n\n%s\n",
                        ((TransportOrganisation) item).textSummary());
                break;
            default:
                throw new CRISException("Unexpected listType: " + listType);
        }

        Intent intent = new Intent(this, AlertAndContinue.class);
        intent.putExtra("title", String.format("Change History - %s", item.getListType().toString()));
        intent.putExtra("message", history);
        startActivity(intent);
    }

    private class ListItemAdapter extends ArrayAdapter<ListItem> {

        // Constructor
        ListItemAdapter(Context context, ArrayList<ListItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_list_item, parent, false);
            }
            ImageView viewItemIcon = (ImageView) convertView.findViewById(R.id.item_icon);
            TextView viewItemMainText = (TextView) convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = (TextView) convertView.findViewById(R.id.item_additional_text);
            TextView viewItemTitle = (TextView) convertView.findViewById(R.id.item_title);

            // Display the title only version
            viewItemMainText.setVisibility(View.GONE);
            viewItemAdditionalText.setVisibility(View.GONE);
            viewItemTitle.setVisibility(View.VISIBLE);
            // Display the list name
            final ListItem item = items.get(position);
            viewItemTitle.setText(item.getItemValue());
            if (item.isDefault()) {
                viewItemIcon.setImageDrawable(getDrawable(R.drawable.ic_list_item_default));
            } else if (item.isDisplayed()) {
                viewItemIcon.setImageDrawable(getDrawable(R.drawable.ic_list_item));
            } else {
                viewItemIcon.setImageDrawable(getDrawable(R.drawable.ic_list_item_grey));
            }
            return convertView;
        }
    }

}
