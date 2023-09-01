package solutions.cris.utils;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Collections;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.list.ListClientsFragment;
import solutions.cris.object.ListType;
import solutions.cris.object.Role;
import solutions.cris.object.User;

public class BatchInviteDialogFragment extends DialogFragment {

    public BatchInviteDialogFragment(LocalDB localDB, LocalSettings localSettings) {
        super();
        this.localDB = localDB;
        this.localSettings = localSettings;
    }

    //private enum SelectOptions {JUST_SAVE, GROUPS, KEYWORKERS, COMMISSIONERS, SCHOOLS, AGENCIES};
    //private SelectOptions selectedOption = SelectOptions.JUST_SAVE;

    private LocalDB localDB;
    private LocalSettings localSettings;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Create the list of options
        String[] options = {
            String.format("Invite all clients from one or more %ss",localSettings.Group),
            String.format("Invite all clients of one or more %ss",localSettings.Keyworker),
            String.format("Invite all clients of one or more %ss",localSettings.Commisioner),
            "Invite all clients from one or more schools",
            "Invite all clients from one or more agencies"};
        // Build the Alert
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Batch Invite Wizard (select an option)")
                .setItems(options, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                PickListDialogFragment pickListDialog;
                switch (which) {
                    case 0:
                        final PickList groups = new PickList(localDB, ListType.GROUP, 0);
                        // Build 200 - Replaced single selection with checkbox selection for picklists
                        pickListDialog = new PickListDialogFragment(localSettings.Group, groups, ListActivity.SelectMode.GROUPS);
                        pickListDialog.show(getParentFragmentManager(), null);
                        break;

                    case 1:
                        // Get a list of keyworkers
                        ArrayList<User> users = localDB.getAllUsers();
                        ArrayList<User> keyworkerList = new ArrayList<>();
                        for (User user : users) {
                            if (user.getRole().hasPrivilege(Role.PRIVILEGE_USER_IS_KEYWORKER)) {
                                keyworkerList.add(user);
                            }
                        }
                        Collections.sort(keyworkerList, User.comparator);
                        final PickList keyworkers = new PickList(keyworkerList, 0);
                        // Build 200 - Replaced single selection with checkbox selection for picklists
                        pickListDialog = new PickListDialogFragment(localSettings.Keyworker, keyworkers, ListActivity.SelectMode.KEYWORKERS);
                        pickListDialog.show(getParentFragmentManager(), null);
                        break;

                    case 2:
                        final PickList commissioners = new PickList(localDB, ListType.COMMISSIONER, 0);
                        // Build 200 - Replaced single selection with checkbox selection for picklists
                        pickListDialog = new PickListDialogFragment(localSettings.Commisioner, commissioners, ListActivity.SelectMode.COMMISSIONERS);
                        pickListDialog.show(getParentFragmentManager(), null);
                        break;

                    case 3:
                        final PickList schools = new PickList(localDB, ListType.SCHOOL, 0);
                        // Build 200 - Replaced single selection with checkbox selection for picklists
                        pickListDialog = new PickListDialogFragment("School", schools, ListActivity.SelectMode.SCHOOLS);
                        pickListDialog.show(getParentFragmentManager(), null);
                        break;

                    case 4:
                        final PickList agencies = new PickList(localDB, ListType.AGENCY, 0);
                        // Build 200 - Replaced single selection with checkbox selection for picklists
                        pickListDialog = new PickListDialogFragment("Agencie", agencies, ListActivity.SelectMode.AGENCIES);
                        pickListDialog.show(getParentFragmentManager(), null);
                        break;
                }
            }
        });
        return builder.create();
    }
}
