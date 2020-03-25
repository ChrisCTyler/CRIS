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
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import solutions.cris.CRISActivity;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.User;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.utils.ExceptionHandler;

public class ListListItems extends CRISActivity {

    private ListView listView;
    public static ArrayList<ListItem> listItems;
    private LocalDB localDB;
    private User currentUser;
    private String listType;
    private TextView hintTextView;
    private boolean hintTextDisplayed = true;

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
            setContentView(R.layout.activity_list_list_items);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            listType = getIntent().getStringExtra(Main.EXTRA_LIST_TYPE);
            toolbar.setTitle(getString(R.string.app_name) + " - " + listType);
            setSupportActionBar(toolbar);

            // Set up the hint text
            hintTextView = (TextView) findViewById(R.id.hint_text);
            hintTextView.setText(getHelpText());
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

            //listView = (ListView) findViewById(R.id.list_list_item_view);
            listView = (ListView) findViewById(R.id.list_list_item_view);
            //listView.setItemsCanFocus(true);

            localDB = LocalDB.getInstance();
            // Load the ListItems from the database
            listItems = localDB.getAllListItems(listType, true);
            // Add a potential new item to the bottom
            if (listItems.size() == 0) {
                ListItem newListItem = new ListItem(currentUser, ListType.valueOf(listType), "Enter New Value", -1);
                listItems.add(newListItem);
            } else {
                // Sort the list by itemOrder
                Collections.sort(listItems, ListItem.comparator);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
/*
        if (!hintTextDisplayed) {
            hintTextView.setHeight(50);
        }
        else {
            hintTextView.setHeight(hintTextHeight);
        }
*/
        ListItemAdapter adapter = new ListItemAdapter(this, listItems);
        this.listView.setAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Need to save new items and updates
        for (int position = 0; position < listItems.size(); position++) {
            ListItem listItem = listItems.get(position);
            if (listItem.hasBeenModified() || listItem.getItemOrder() != position) {
                listItem.setHasBeenModified(false);
                // Trim the item value
                listItem.setItemValue(listItem.getItemValue().trim());
                // Not interested in blank values or the spurious Please select
                if (listItem.getItemValue().length() > 0 &&
                        !listItem.getItemValue().toLowerCase().equals("enter new value")) {
                    try {
                        if (listItem.getItemOrder() == -1) {
                            // New List Item
                            listItem.setItemOrder(position);
                            localDB.save(listItem, true, currentUser);
                        } else {
                            // Existing list item (position may have changed)
                            listItem.setItemOrder(position);
                            localDB.save(listItem, false, currentUser);
                        }
                    } catch (SQLiteConstraintException ex) {
                        Intent intent = new Intent(this, AlertAndContinue.class);
                        intent.putExtra("title", "Identical Values Not Allowed");
                        intent.putExtra("message", "You have tried to save two " +
                                "options with the same value. The The duplicate " +
                                "value will not be saved.\n\n" + ex.getMessage());
                        startActivity(intent);
                    }
                }
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list_list_items, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_save:
                // onDestroy() does the save
                finish();
                return true;

            case R.id.action_cancel:
                finish();
                return true;

            case R.id.action_sort_az:
                Collections.sort(listItems, ListItem.comparatorAZ);
                for (ListItem listItem : listItems) {
                    listItem.setHasBeenModified(true);
                }
                onResume();
                return true;

            case R.id.action_sort_za:
                Collections.sort(listItems, ListItem.comparatorZA);
                for (ListItem listItem : listItems) {
                    listItem.setHasBeenModified(true);
                }
                onResume();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
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

    private class ListItemAdapter extends ArrayAdapter<ListItem> {

        private int selectedItem = -1;

        // Constructor
        ListItemAdapter(Context context, ArrayList<ListItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            final Drawable iconStarGrey = getDrawable(R.drawable.ic_star_grey);
            final Drawable iconStarBlue = getDrawable(R.drawable.ic_star_blue);

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = getLayoutInflater().inflate(R.layout.list_item_editor, parent, false);
                holder.addItemView = (ImageView) convertView.findViewById(R.id.action_add);
                holder.cutPasteView = (ImageView) convertView.findViewById(R.id.action_cut_paste);
                holder.defaultIconView = (ImageView) convertView.findViewById(R.id.default_icon);
                holder.selectIconView = (ImageView) convertView.findViewById(R.id.select_icon);
                holder.textView = (TextView) convertView.findViewById(R.id.item_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final ListItem listItem = listItems.get(position);

            holder.addItemView.setId(position);
            holder.cutPasteView.setId(position);
            if (selectedItem == -1) {
                holder.cutPasteView.setImageDrawable(getDrawable(R.drawable.ic_action_cut));
            } else if (selectedItem == position) {
                holder.cutPasteView.setImageDrawable(getDrawable(R.drawable.ic_action_paste_red));
            } else {
                holder.cutPasteView.setImageDrawable(getDrawable(R.drawable.ic_action_paste));
            }

            holder.defaultIconView.setId(position);
            holder.defaultIconView.setImageDrawable(iconStarGrey);
            if (listItem.isDefault()) {
                holder.defaultIconView.setImageDrawable(iconStarBlue);
            }
            holder.selectIconView.setId(position);
            if (listItem.getItemOrder() == -1) {
                // New item so can be deleted
                holder.selectIconView.setImageDrawable(getDrawable(R.drawable.ic_action_bin));
            } else if (listItem.isDisplayed()) {
                //holder.selectIconView.setImageDrawable(getDrawable(R.mipmap.ic_tick));
                holder.selectIconView.setImageDrawable(getDrawable(R.drawable.ic_action_tick));
            } else {
                holder.selectIconView.setImageDrawable(getDrawable(R.drawable.ic_action_cross));
            }
            // Embed listItem so that it's ItemValue can be changed
            holder.textView.setTag(listItem);
            String textValue = listItem.getItemValue();
            holder.textView.setText(textValue);

            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Popup a dialog
                    final ListItem listItem = (ListItem) v.getTag();
                    final EditText editText = new EditText(v.getContext());

                    String currentText = listItem.getItemValue();
                    if (currentText.length() > 0 && currentText.compareTo("Enter New Value") != 0) {
                        editText.setText(currentText);
                    }
                    editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);



                    new AlertDialog.Builder(v.getContext())
                            // Build 139 - Restored Edit Text (only works without parameters
                            .setView(editText)
                            //.setView(editText, 50, 10, 50, 10)
                            .setTitle("Enter a value (must be unique)")
                            .setMessage(currentText)
                            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    listItem.setItemValue(editText.getText().toString());
                                    listItem.setHasBeenModified(true);
                                    notifyDataSetChanged();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })

                            .show();

                }
            });

            holder.defaultIconView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int position = v.getId();
                    //final ImageView defaultIconView = (ImageView) v;
                    if (selectedItem == -1) {
                        ListItem listItem = listItems.get(position);
                        if (listItem.isDefault()) {
                            listItem.setIsDefault(false);
                        } else {
                            for (ListItem item : listItems) {
                                if (item.isDefault()) {
                                    item.setIsDefault(false);
                                    item.setHasBeenModified(true);
                                }
                            }
                            listItem.setIsDefault(true);
                        }
                        listItem.setHasBeenModified(true);
                        notifyDataSetChanged();
                    }
                }
            });

            holder.selectIconView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int position = v.getId();
                    //final ImageView defaultIconView = (ImageView) v;
                    if (selectedItem == -1) {
                        ListItem listItem = listItems.get(position);
                        if (listItem.getItemOrder() == -1) {
                            // List item is new so can be removed
                            listItems.remove(position);
                        } else if (listItem.isDisplayed()) {
                            listItem.setIsDisplayed(false);
                        } else {
                            listItem.setIsDisplayed(true);
                        }
                        listItem.setHasBeenModified(true);
                        notifyDataSetChanged();
                    }
                }
            });

            holder.addItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Add a new item before the first item
                    final int position = v.getId();
                    if (selectedItem == -1) {
                        // Order is set to -1 to indicate that this is a new item
                        ListItem newListItem = new ListItem(currentUser, ListType.valueOf(listType), "Enter New Value", -1);
                        listItems.add(position + 1, newListItem);
                        notifyDataSetChanged();
                    }
                }
            });

            holder.cutPasteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int position = v.getId();
                    if (selectedItem == -1) {
                        selectedItem = position;
                    } else {
                        // Insert the selected item above this item
                        ListItem item = listItems.get(selectedItem);
                        listItems.remove(selectedItem);
                        listItems.add(position, item);
                        selectedItem = -1;
                    }
                    notifyDataSetChanged();
                }
            });

            return convertView;
        }

        class ViewHolder {
            ImageView addItemView;
            ImageView cutPasteView;
            ImageView defaultIconView;
            ImageView selectIconView;
            //EditText textView;
            TextView textView;
        }
    }

    private String getHelpText() {
        return "The list items are shown in the order that they " +
                "will appear in pick-lists. Values can be moved around using the cut " +
                "(scissors) and paste option, or sorted via the menu.\n\n" +
                "New values can be inserted following any of the existing values using " +
                "the 'Add Item' option on the left.\n\n" +
                "One of the values may be set as the pick-list default, using the star. " +
                "If all stars are grey, the pick-list will default to 'Please select'.\n\n " +
                "Existing values may be renamed but this will cause every existing use in the system " +
                "to be renamed which is likely to be inappropriate, except for spelling errors " +
                "or to clarify a value. It is usually better to remove the existing value " +
                "from future pick-lists, using the tick/cross icon on the right of the display " +
                "and create one or more new values as replacements.";
    }
}
