package solutions.cris;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import solutions.cris.object.User;
import solutions.cris.utils.CRISDeviceAdmin;
import solutions.cris.utils.ExceptionHandler;

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

public class CRISActivity extends AppCompatActivity {

    static final int RESULT_ENABLE = 1;
    DevicePolicyManager deviceManager;
    ComponentName compName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        // Set up the Device Policy Manager
        deviceManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, CRISDeviceAdmin.class);
    }

    // MENU BLOCK
    private static final int MENU_LOCK = Menu.FIRST;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.menu_main, menu);
        // LOCK
        MenuItem lockOption = menu.add(0, MENU_LOCK, 1, R.string.action_lock);
        lockOption.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_action_lock));
        lockOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_LOCK:
                if (deviceManager.isAdminActive(compName)) {
                    // Return to Home Screen before locking it so that unlock isn't forced
                    // straight back into CRIS. (Only lock following MyWeek edit does this)
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.addCategory(Intent.CATEGORY_HOME);
                    startActivity(i);
                    deviceManager.lockNow();
                } else {
                    // Policy manager not enabled so logout as the lock mechanism
                    User currentUser = User.getCurrentUser();
                    Intent intent = new Intent(this, Login.class);
                    intent.putExtra(Main.EXTRA_EMAIL_ADDRESS, currentUser.getEmailAddress());
                    startActivity(intent);
                }


                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_ENABLE:
                if (resultCode == AppCompatActivity.RESULT_OK  &&
                        deviceManager.isAdminActive(compName)) {
                        Intent i = new Intent(Intent.ACTION_MAIN);
                        i.addCategory(Intent.CATEGORY_HOME);
                        startActivity(i);
                        deviceManager.lockNow();
                } else {
                    User currentUser = User.getCurrentUser();
                    Intent intent = new Intent(this, Login.class);
                    intent.putExtra(Main.EXTRA_EMAIL_ADDRESS, currentUser.getEmailAddress());
                    startActivity(intent);
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public DevicePolicyManager getDeviceManager() {
        return deviceManager;
    }

    public ComponentName getCompName() {
        return compName;
    }
}
