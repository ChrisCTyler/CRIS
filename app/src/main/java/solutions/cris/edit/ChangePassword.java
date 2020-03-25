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

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Date;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.object.User;
import solutions.cris.utils.ExceptionHandler;

public class ChangePassword extends AppCompatActivity {

    private LocalDB localDB;
    private User currentUser;
    private boolean passwordExpiredMode;
    String password;

    private EditText passwordView;
    private EditText passwordCheckView;
    private TextView hintTextView;
    private boolean hintTextDisplayed = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add the global uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        passwordExpiredMode = getIntent().getBooleanExtra(Main.EXTRA_PASSWORD_EXPIRED, false);

        currentUser = User.getCurrentUser();
        if (currentUser == null) {
            finish();
        } else {
            localDB = LocalDB.getInstance();
            // Re-load the local current user from the database in case activity is called twice
            // with the same current user (would cause a constraint exception)
            currentUser = localDB.getUser(currentUser.getUserID());
            setContentView(R.layout.activity_change_password);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setTitle(getString(R.string.app_name) + " - Change Password");
            setSupportActionBar(toolbar);

            // Set up the form.
            passwordView = (EditText) findViewById(R.id.password);
            passwordCheckView = (EditText) findViewById(R.id.password_check);
            hintTextView = (TextView) findViewById(R.id.hint_text);
            hintTextView.setText(getHintText());
            hintTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleHint();
                }
            });

            // Save Button
            final Button button = (Button) findViewById(R.id.set_new_password);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    button.setEnabled(false);
                    if (validate()) {
                        if (save()) {
                            finish();
                        }
                    }
                    button.setEnabled(true);
                }
            });
        }

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

    // Validate the document
    private boolean validate() {
        boolean success = true;

        // Clear any existing errors
        passwordView.setError(null);
        passwordCheckView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        password = passwordView.getText().toString().trim();

        String allAlphanumericRegex = "^[a-zA-Z0-9]+$";
        if (password.length() < 8) {
            passwordView.setError(getString(R.string.error_password_too_short));
            focusView = passwordView;
            success = false;
        } else if (password.equals(password.toLowerCase())) {
            passwordView.setError(getString(R.string.error_password_no_caps));
            focusView = passwordView;
            success = false;
        } else if (password.matches(allAlphanumericRegex)) {
            passwordView.setError(getString(R.string.error_password_all_letters));
            focusView = passwordView;
            success = false;
        }
        // PasswordCheck
        String passwordCheck = passwordCheckView.getText().toString().trim();
        if (TextUtils.isEmpty((passwordCheck)) | !password.equals(passwordCheck)) {
            passwordCheckView.setError(getString(R.string.error_incorrect_password_check));
            focusView = passwordCheckView;
            success = false;
        } else if (currentUser.authenticatePassword(password)) {
            // New password must be the same as the old password
            passwordCheckView.setError(getString(R.string.error_new_password_is_same));
            focusView = passwordCheckView;
            success = false;
        }

        if (!success) {
            focusView.requestFocus();
        }
        return success;
    }

    @Override
    public void onBackPressed() {
        // Back not allowed if password expired
        if (!passwordExpiredMode) {
            finish();
        }
    }

    // Save the document
    private boolean save() {
        // Set the expiry date to 4 months from now
        Date newExpiryDate = User.addMonths(new Date(), 4);
        currentUser.setPasswordExpiryDate(newExpiryDate);
        currentUser.setNewPassword(password);
        localDB.save(currentUser, false, currentUser);
        // Reload the currentuser to update the expiry date
        User.setCurrentUser(currentUser);
        passwordExpiredMode = false;
        return true;
    }

    private String getHintText() {
        String passwordRules = "";
        if (passwordExpiredMode) {
            passwordRules = "Your password has expired\n\n";
        }
        passwordRules += "The information stored in the CRIS Care Record System is " +
                "confidential and it is important that a secure password is used. " +
                "Please use a different password to any you use in other " +
                "application so that if those applications are compromised, it does " +
                "not affect the security of this system.\n\n" +
                "The password you choose needs to be relatively simple to remember " +
                "and easy to type since you will need to type the password frequently when unlocking " +
                "the system. However, the password must not be easy to guess.\n\n" +
                "The password must be at least 8 characters long including at least one capital " +
                "letter and one non-alphanumeric character (one that is neither a number or a " +
                "letter). To make the password more secure, include a capital letter " +
                "somewhere in the middle of the password, not the first character and " +
                "to make the password easier to type, use a non-alphanumeric character " +
                "which is available on the main keyboard, not the symbol keyboard.";


        return passwordRules;
    }

}
