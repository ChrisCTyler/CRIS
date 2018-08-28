package solutions.cris.utils;


import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import solutions.cris.db.LocalDB;
import solutions.cris.object.SystemError;
import solutions.cris.object.User;

/**
 * Copyright CRIS.Solutions 12/10/2016.
 */

public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {
    private final Activity myContext;

    public ExceptionHandler(AppCompatActivity context) {
        myContext = context;
    }

    public ExceptionHandler() {
        myContext = null;
    }


    public void uncaughtException(Thread thread, Throwable exception) {

        // Instantiate a System Error with all available information
        SystemError systemError;
        User currentUser = User.getCurrentUser();
        if (currentUser == null){
            systemError = new SystemError(new User(User.unknownUser), (Exception) exception);
        }
        else {
            // Create a SysteError with the Unknown User
            systemError = new SystemError(currentUser, (Exception) exception);
        }

        String title = "System Error";
        String message = "An unrecoverable system error has occurred.\n\n";

        // Try to open the database
        try {
            LocalDB localDB = LocalDB.getInstance();
            localDB.save(systemError);
            message += "The details have been saved and will be sent to your System " +
                    "Administrator when you next sync with the server. It is very likely " +
                    "that your last action will have failed and will need to be repeated.\n\n";
            // Remove the following line in production copy
            //message += systemError.getTextSummary();
            message += systemError.getExceptionMessage();
        }
        catch (Exception ex){
            // Worst case scenario
            // Unable to save error to database so display for user
            message += "Please copy and paste the following information into an email " +
                    "and send to your system administrator\n\n";
            message += systemError.getTextSummary();
        }

        if (myContext != null) {
            Intent intent = new Intent(myContext, AlertAndContinue.class);
            intent.putExtra("title", title);
            intent.putExtra("message", message);
            myContext.startActivity(intent);
        }

        //android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
}
