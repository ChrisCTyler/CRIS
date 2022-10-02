package solutions.cris.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import java.util.Date;

import static android.content.Context.ACCOUNT_SERVICE;

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

public class SyncManager {

    // The authority for the sync adapter's content provider
    private static final String AUTHORITY = "solutions.cris.sync.provider";
    private static final String ACCOUNT_TYPE = "cris.solutions";
    private static final String ACCOUNT = "default_account";
    static final String SYNC_ACTIVITY_ID = "solutions.cris.SyncActivityID";

    private static final long SYNC_INTERVAL_IN_MINUTES = 30L;

    public static final String SYNC_ACTION = "solutions.cris.SyncAction";
    public static final String SYNC_STATUS = "solutions.cris.SyncStatus";
    public static final String SYNC_EXCEPTION_MESSAGE = "solutions.cris.SyncExceptionMessage";
    public static final String SYNC_ORGANISATION = "solutions.cris.SyncOrgansiation";
    // Build 181 Resync Options
    public static final String SYNC_RECHECK = "solutions.cris.SyncRecheck";
    public static final String SYNC_RECHECK_DATE = "solutions.cris.SyncRecheckDate";
    public static final String SYNC_PARTIAL_RECHECK_DATE = "solutions.cris.SyncPartialRecheckDate";

    private static volatile SyncManager syncManager;

    // The stub account to use for theSync Adapter
    private Account syncAccount;

    private SyncManager(Context context){
        syncAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        // This may fail because we are using a stub authenticator but it doesn't seem to matter
        accountManager.addAccountExplicitly(syncAccount, null, null);
        // Set the automatic sync in Android Accounts Settings
        ContentResolver.setSyncAutomatically(syncAccount, AUTHORITY, true);
    }

    /*
    public Account getSyncAccount() {
        return syncAccount;
    }
    */

    public void requestManualSync(){
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(SYNC_RECHECK, false);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        //Build 181 - Now to indicate that this is not a re-sync
        Date timeNow = new Date();
        settingsBundle.putLong(SYNC_RECHECK_DATE, timeNow.getTime());
        ContentResolver.requestSync(syncAccount, AUTHORITY, settingsBundle);
    }

    // Build 181 - Requestor for ReSync (has non-zero duration)
    public void requestReSync(long recheckDate) {
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(SYNC_RECHECK, true);
        settingsBundle.putLong(SYNC_RECHECK_DATE, recheckDate);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(syncAccount, AUTHORITY, settingsBundle);
    }

    public void addPeriodicSync(){
        Bundle settingsBundle = new Bundle();
        // It would appear that either the Cancel or the Remove causes problems with the ASUS
        // tablets making them intermittantly unable to resolve host names
        // Build 181 - Restore removePeriodicSync to ensure only one runs once - problem with inability to
        // resolve host names seems to still happen intermittantly anyway.
        ContentResolver.cancelSync(syncAccount, AUTHORITY);
        ContentResolver.removePeriodicSync(syncAccount, AUTHORITY, settingsBundle);
        ContentResolver.addPeriodicSync(syncAccount,AUTHORITY,settingsBundle,SYNC_INTERVAL_IN_MINUTES * 60);
    }

    public static synchronized SyncManager getSyncManager(Context context) {
        if (syncManager == null){
            syncManager = new SyncManager(context);
        }
        return syncManager;
    }


}
