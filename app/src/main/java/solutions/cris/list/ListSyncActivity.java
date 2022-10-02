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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import solutions.cris.CRISActivity;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.object.SyncActivity;
import solutions.cris.object.User;
import solutions.cris.utils.AlertAndContinue;
import solutions.cris.utils.ExceptionHandler;

public class ListSyncActivity extends CRISActivity {

    private ListView listView;
    public static List<SyncActivity> syncActivities;
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
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            this.listView = findViewById(R.id.list_view);

            this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    doReadError(position);
                }
            });

            LocalDB localDB = LocalDB.getInstance();
            // Load the documents for special client (ClientID=DOCUMENT) from the database
            // Build 158 - Reduce sync activity list to last 100
            //syncActivities = localDB.getAllSyncResults(currentUser);
            syncActivities = localDB.getRecentSyncResults(currentUser, "100");
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        SyncActivityAdapter adapter = new SyncActivityAdapter(this, syncActivities);
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

    private void doReadError(int position){
        SyncActivity syncActivity = syncActivities.get(position);
        Intent intent = new Intent(this, AlertAndContinue.class);
        intent.putExtra("title", "Sync Result");
        intent.putExtra("message", syncActivity.getTextSummary());
        startActivity(intent);
    }

    private class SyncActivityAdapter extends ArrayAdapter<SyncActivity> {

        // Constructor
        SyncActivityAdapter(Context context, List<SyncActivity> objects) {
            super(context, 0, objects);
        }

        @Override
        public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_list_item, parent, false);
            }

            ImageView viewItemIcon = convertView.findViewById(R.id.item_icon);
            TextView viewItemDate = convertView.findViewById(R.id.item_date);
            TextView viewItemMainText = convertView.findViewById(R.id.item_main_text);
            TextView viewItemAdditionalText = convertView.findViewById(R.id.item_additional_text);

            final SyncActivity syncActivity = syncActivities.get(position);

            if (syncActivity.getResult().equals("SUCCESS")){
                viewItemIcon.setImageDrawable(getDrawable(R.drawable.ic_sync_activity_green));
            }
            else {
                viewItemIcon.setImageDrawable(getDrawable(R.drawable.ic_sync_activity_red));
            }
            SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.UK);
            viewItemDate.setText(sDate.format(syncActivity.getCreationDate()));
            viewItemAdditionalText.setText((syncActivity.getSummary()));
            viewItemMainText.setText(syncActivity.getResult());
            return convertView;
        }
    }

}
