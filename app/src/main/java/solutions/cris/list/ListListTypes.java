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
import android.widget.TextView;

import java.util.ArrayList;

import solutions.cris.CRISActivity;
import solutions.cris.Login;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.object.ListType;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;

public class ListListTypes extends CRISActivity {

    private ListView listView;
    public static ArrayList<String> lists;
    private User currentUser;

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
            setSupportActionBar(toolbar);

            this.listView = (ListView) findViewById(R.id.list_view);

            this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    doShowListItems(position);
                }
            });

            // Load the set of ListTypes
            lists = new ArrayList<>();
            for (ListType value : ListType.values()) {
                lists.add(value.toString());
            }
            // Sort the list alphabetically
            //Collections.sort(lists, SystemError.dateComparator);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ListAdapter adapter = new ListAdapter(this, lists);
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

    private void doShowListItems(int position) {
        String listType = lists.get(position);
        Intent intent;
        switch (listType){
            case "AGENCY":
            case "NOTE_TYPE":
            case "ROLE":
            case "SCHOOL":
            case "GROUP":
            case "TRANSPORT_ORGANISATION":
                intent = new Intent(this, ListComplexListItems.class);
                break;
            default:
                intent = new Intent(this, ListListItems.class);
        }

        intent.putExtra(Main.EXTRA_LIST_TYPE, listType);
        startActivity(intent);
    }

    private class ListAdapter extends ArrayAdapter<String> {

        // Constructor
        ListAdapter(Context context, ArrayList<String> objects) {
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
            final String list = lists.get(position);
            viewItemTitle.setText(list);
            viewItemIcon.setImageDrawable(getDrawable(R.drawable.ic_list));
            return convertView;
        }
    }

}
