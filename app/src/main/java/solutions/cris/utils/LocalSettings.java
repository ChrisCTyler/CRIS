package solutions.cris.utils;

import android.content.Context;
import android.content.SharedPreferences;

import solutions.cris.R;
import solutions.cris.exceptions.CRISException;

/**
 * Copyright CRIS.Solutions 02/12/2016.
 */

public class LocalSettings {

    public static volatile LocalSettings instance = null;

    public String Group = "Group";
    public String Keyworker = "Keyworker";
    public String Tier = "Tier";
    public String Coworker1 = "Co-worker";
    public String Coworker2 = "Co-worker";
    public String Commisioner = "Commissioner";
    public String SessionCoordinator = "Session Coordinator";
    public String OtherStaff = "Other Staff";

    public int RedThreshold = 7;
    public int AmberThreshold = 28;
    public int GreenThreshold = 90;

    private LocalSettings(Context context){
        // Get the basic parameters
        SharedPreferences prefs = context.getSharedPreferences(
                context.getString(R.string.shared_preference_file), Context.MODE_PRIVATE);
        String organisation = prefs.getString(context.getString(R.string.pref_organisation), "");

        switch (organisation){
            case "Cheshire Young Carers":
                Keyworker = "Link Worker";
                SessionCoordinator = "Session Lead";
                OtherStaff = "Other Staff/Volunteer";
                RedThreshold = 14;
                AmberThreshold = 28;
                GreenThreshold = 42;
                break;

            case "Test Organisation":
                Keyworker = "Link Worker";
                SessionCoordinator = "Session Manager";
                break;
        }
    }

    public static LocalSettings getInstance(Context context){
        if (instance == null){
            instance = new LocalSettings(context);
        }
        return instance;
    }

    public static LocalSettings getInstance(){
        if (instance == null){
            throw new CRISException("LocalSettings.getInstance() called when instance was null.");
        }
        return instance;
    }
}
