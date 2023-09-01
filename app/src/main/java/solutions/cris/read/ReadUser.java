package solutions.cris.read;
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
import android.net.Uri;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

import solutions.cris.CRISActivity;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.list.ListUsers;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;
import solutions.cris.utils.OnSwipeTouchListener;

public class ReadUser extends CRISActivity {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private User editUser;
    private User currentUser;
    EditText emailView;
    EditText contactNumberView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add the global uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        // Preset sDate for use throughout the activity
        sDate.setLenient(false);
        // CurrentUser always exists so if this check fails then exception in child
        // has rendered system inconsistent so exit and let Main start from scratch()
        currentUser = User.getCurrentUser();
        if (currentUser == null) {
            finish();
        } else {
            setContentView(R.layout.activity_user);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            // Get the parameter passed with the Intent
                int listPos = getIntent().getIntExtra(Main.EXTRA_LIST_POSITION, 0);
                editUser = ListUsers.adapterList.get(listPos);
                toolbar.setTitle(getString(R.string.app_name) + " - User");
            setSupportActionBar(toolbar);

            // Set up the form.
            emailView = (EditText) findViewById(R.id.email);
            EditText firstNameView = (EditText) findViewById(R.id.first_name);
            EditText lastNameView = (EditText) findViewById(R.id.last_name);
            TextInputLayout startDateView = (TextInputLayout) findViewById(R.id.start_date_view);
            TextInputLayout endDateView = (TextInputLayout) findViewById(R.id.end_date_view);
            contactNumberView = (EditText) findViewById(R.id.contact_number);
            Spinner roleSpinnerView = (Spinner) findViewById(R.id.role_spinner);
            TextView roleTextView = (TextView) findViewById(R.id.role_read_text);
            EditText passwordView = (EditText) findViewById(R.id.password);
            EditText passwordCheckView = (EditText) findViewById(R.id.password_check);
            TextView hintTextView = (TextView) findViewById(R.id.hint_text);
            ImageView hintIconView = (ImageView) findViewById(R.id.hint_open_close);
            Button cancelButton = (Button) findViewById(R.id.cancel_button);
            Button saveButton = (Button) findViewById(R.id.save_button);

            // Set the fields to non-editable
            emailView.setInputType(InputType.TYPE_NULL);
            emailView.setFocusable(false);
            firstNameView.setInputType(InputType.TYPE_NULL);
            firstNameView.setFocusable(false);
            lastNameView.setInputType(InputType.TYPE_NULL);
            lastNameView.setFocusable(false);
            //startDateView.setInputType(InputType.TYPE_NULL);
            //startDateView.setFocusable(false);
            startDateView.setVisibility(View.GONE);
            //endDateView.setInputType(InputType.TYPE_NULL);
            //endDateView.setFocusable(false);
            endDateView.setVisibility(View.GONE);
            contactNumberView.setInputType(InputType.TYPE_NULL);
            contactNumberView.setFocusable(false);
            roleSpinnerView.setVisibility(View.GONE);
            roleTextView.setVisibility(View.VISIBLE);
            roleTextView.setFocusable(false);
            hintTextView.setVisibility(View.GONE);
            hintIconView.setVisibility(View.GONE);
            passwordView.setVisibility(View.GONE);
            passwordCheckView.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);

            // Load the content
            if (editUser.getEmailAddress() != null) {
                emailView.setText(editUser.getEmailAddress(), null);
                firstNameView.setText(editUser.getFirstName(), null);
                lastNameView.setText(editUser.getLastName(), null);
                /*
                Date startDate = editUser.getStartDate();
                startDateView.setText(sDate.format(startDate.getTime()));
                if (editUser.getEndDate().getTime() != Long.MIN_VALUE) {
                    Date endDate = editUser.getEndDate();
                    endDateView.setText(sDate.format(endDate.getTime()));
                } else {
                    // Needs a space to show completed field in read mode
                    endDateView.setText(" ");
                }
                */
                contactNumberView.setText(editUser.getContactNumber(), null);
                roleTextView.setText(editUser.getRole().getItemValue());
            }

            // Swipe Left and Right
            ScrollView scrollView = (ScrollView) findViewById(R.id.edit_user_view);
            scrollView.setOnTouchListener(new OnSwipeTouchListener(this) {
                @Override
                public void onSwipeRight() {
                    try {
                        ListUsers.swipeValue = -1;
                        finish();
                    }
                    catch (Exception ex){
                        // Activity not descended from ListUser so ignore
                    }
                }
                @Override
                public void onSwipeLeft() {
                    try {
                        ListUsers.swipeValue = 1;
                        finish();
                    }
                    catch (Exception ex){
                        // Activity not descended from ListUser so ignore
                    }
                }
            });

            // Add email Intent
            emailView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String[] addresses = new String[1];
                    addresses[0] = emailView.getText().toString();
                    composeEmail(addresses);

                }
            });

            // Add Contact Number Intent
            contactNumberView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialPhoneNumber(contactNumberView.getText().toString());
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_share, menu);
        super.onCreateOptionsMenu(menu);
        //SHARE
        MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        createShareActionProvider(shareOption);
        return true;
    }

    // SHARE MENU ITEM (Both methods are required)
    private void createShareActionProvider(MenuItem menuItem){
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, editUser.textSummary());
        shareActionProvider.setShareIntent(shareIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void composeEmail(String[] addresses){
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, "CRIS");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void dialPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

}
