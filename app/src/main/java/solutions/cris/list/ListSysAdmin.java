package solutions.cris.list;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import solutions.cris.Login;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.User;
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