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
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import solutions.cris.CRISActivity;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Note;
import solutions.cris.object.Sync;
import solutions.cris.object.User;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.utils.CRISMenuItem;
import solutions.cris.utils.ExceptionHandler;

public class ListSysAdmin extends CRISActivity {

    private ArrayList<CRISMenuItem> menuItems;
    private User currentUser;
    SysAdminAdapter adapter;
    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK);

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
            setContentView(R.layout.activity_list);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setTitle(getString(R.string.app_name) + " - System Administration");
            setSupportActionBar(toolbar);

            // Initialise the list of Static Menu Items
            menuItems = new ArrayList<>();
            menuItems.add(new CRISMenuItem("List Manager", "", R.drawable.ic_list, null));
            menuItems.add(new CRISMenuItem("System Errors", "", R.drawable.ic_system_error, null));
            // Build 128 Duplicate Text Message Fix
            menuItems.add(new CRISMenuItem("Remove Duplicate Notes", "", R.drawable.ic_system_error, null));

            // Setup the List view listener
            ListView listView = (ListView) findViewById(R.id.list_view);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String title = ((CRISMenuItem) view.getTag()).getTitle();
                    switch (title) {
                        case "List Manager":
                            doListLists();
                            break;
                        case "System Errors":
                            doListErrors();
                            break;
                        case "Remove Duplicate Notes":
                            doDuplicateNotes();
                            break;
                        default:
                            throw new CRISException("Invalid main Menu Option: " + title);
                    }
                }
            });


            // Create the Main menu
            adapter = new SysAdminAdapter(this, menuItems);
            // Display in the List View
            listView.setAdapter(adapter);

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

    private void doListErrors() {
        Intent intent = new Intent(this, ListErrors.class);
        startActivity(intent);
    }

    private void doDuplicateNotes(){
        SimpleDateFormat sDateTime = new SimpleDateFormat("dd MMM HH:mm", Locale.UK);
        LocalDB localDB = LocalDB.getInstance();
        int count = 0;
        int duplicates = 0;
        String noteList = "";
        String content = "";
        String lastContent = "**Rubbish**";
        String lastClient = "";
        ArrayList<Note> notes = localDB.getAllBroadcastNotes();
        for (Note note:notes){
            if(note.getNoteType().getItemValue().equals("Text Message") ) {
                if (!note.getClientID().toString().equals(lastClient)){
                    lastContent = "**Rubbish**";
                }
                lastClient = note.getClientID().toString();
                count++;
                content = note.getContent();
                if (content.equals(lastContent)){
                    localDB.remove(note);
                    duplicates++;
                }
                lastContent = content;
            }
        }
        noteList += String.format("\nNotes Found = %d, Duplicates removed = %d",count,duplicates);
        Intent intent = new Intent(this, AlertAndContinue.class);
        intent.putExtra("title", String.format("Duplicate Notes"));
        intent.putExtra("message", noteList);
        startActivity(intent);
    }

   private void doListLists() {
        Intent intent = new Intent(this, ListListTypes.class);
        startActivity(intent);
    }
    private class SysAdminAdapter extends ArrayAdapter<CRISMenuItem> {

        SysAdminAdapter(Context context, List<CRISMenuItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_list_item, parent, false);
            }

            ImageView viewItemIcon = (ImageView) convertView.findViewById(R.id.item_icon);
            TextView viewItemDate = (TextView) convertView.findViewById(R.id.item_date);
            TextView viewItemMainText = (TextView) convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = (TextView) convertView.findViewById(R.id.item_additional_text);
            TextView viewItemTitle = (TextView) convertView.findViewById(R.id.item_title);
            ProgressBar syncProgress = (ProgressBar) convertView.findViewById(R.id.sync_progress);

            final CRISMenuItem menuItem = menuItems.get(position);
            convertView.setTag(menuItem);

            if (menuItem.getSummary().length() == 0){
                viewItemTitle.setVisibility(View.VISIBLE);
                viewItemTitle.setText(menuItem.getTitle());
                viewItemMainText.setVisibility(View.GONE);
                viewItemAdditionalText.setVisibility(View.GONE);
            } else {
                viewItemTitle.setVisibility(View.GONE);
                viewItemMainText.setVisibility(View.VISIBLE);
                viewItemAdditionalText.setVisibility(View.VISIBLE);
                viewItemMainText.setText(menuItem.getTitle());
                viewItemAdditionalText.setText(menuItem.getSummary());
            }
            if (menuItem.getIcon() == 0){
                syncProgress.setVisibility(View.VISIBLE);
                viewItemIcon.setVisibility(View.GONE);
            }
            else {
                syncProgress.setVisibility(View.GONE);
                viewItemIcon.setVisibility(View.VISIBLE);
                viewItemIcon.setImageDrawable(getDrawable(menuItem.getIcon()));
            }
            if (menuItem.getDisplayDate() != null) {
                viewItemDate.setText(sDate.format(menuItem.getDisplayDate()));
            }
            return convertView;
        }
    }

}
