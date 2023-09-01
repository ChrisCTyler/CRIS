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

import android.app.DatePickerDialog;
// Build 200 Use the androidX Fragment class
//import android.app.Fragment;
//import android.app.FragmentManager;
import androidx.fragment.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import solutions.cris.CRISActivity;
import solutions.cris.Login;
import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Client;
import solutions.cris.object.Document;
import solutions.cris.object.MyWeek;
import solutions.cris.object.User;
import solutions.cris.utils.CRISUtil;

public class EditMyWeek extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private MyWeek editDocument;

    private EditText referenceDateView;
    private ImageView schoolSad;
    private TextView schoolScore;
    private ImageView school2;
    private ImageView school3;
    private ImageView school4;
    private ImageView school5;
    private TextView schoolTitle;
    private ImageView schoolHappy;
    private ImageView friendSad;
    private TextView friendScore;
    private ImageView friend2;
    private ImageView friend3;
    private ImageView friend4;
    private ImageView friend5;
    private TextView friendTitle;
    private ImageView friendHappy;
    private ImageView homeSad;
    private TextView homeScore;
    private ImageView home2;
    private ImageView home3;
    private ImageView home4;
    private ImageView home5;
    private TextView homeTitle;
    private ImageView homeHappy;
    private EditText noteView;

    private LocalDB localDB;
    private View parent;
    private boolean isNewMode = false;
    private User currentUser;
    private Client client;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.edit_my_week, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        currentUser = ((ListActivity) getActivity()).getCurrentUser();
        client = ((ListActivity) getActivity()).getClient();
        editDocument = (MyWeek) ((ListActivity) getActivity()).getDocument();
        if (((ListActivity) getActivity()).getMode() == Document.Mode.NEW) {
            isNewMode = true;
        }
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);

        if (isNewMode) {
            toolbar.setTitle(getString(R.string.app_name) + " - New MyWeek");
        } else {
            toolbar.setTitle(getString(R.string.app_name) + " - Edit MyWeek");
        }

        // Hide the FAB
        fab.setVisibility(View.GONE);

        // Clear the footer
        footer.setText("");

        localDB = LocalDB.getInstance();

        // CANCEL BOX
        if (editDocument.getCancelledFlag()) {
            LinearLayout cancelBoxView = (LinearLayout) parent.findViewById(R.id.cancel_box_layout);
            cancelBoxView.setVisibility(View.VISIBLE);
            TextView cancelBy = (TextView) parent.findViewById(R.id.cancel_by);
            String byText = "by ";
            // Build 150 - Allow for earlier bug which didn't set the cancelled by used
            if (editDocument.getCancelledByID() == null){
                byText += "Unknown User" + " on ";
            } else {
                User cancelUser = localDB.getUser(editDocument.getCancelledByID());
                byText += cancelUser.getFullName() + " on ";
            }
            byText += sDate.format(editDocument.getCancellationDate());
            cancelBy.setText(byText);
            TextView cancelReason = (TextView) parent.findViewById(R.id.cancel_reason);
            cancelReason.setText(String.format("Reason: %s", editDocument.getCancellationReason()));
        }


        referenceDateView = (EditText) parent.findViewById(R.id.reference_date);
        schoolSad = (ImageView) parent.findViewById(R.id.school_1);
        schoolScore = (TextView) parent.findViewById(R.id.school_score);
        school2 = (ImageView) parent.findViewById(R.id.school_2);
        school3 = (ImageView) parent.findViewById(R.id.school_3);
        school4 = (ImageView) parent.findViewById(R.id.school_4);
        school5 = (ImageView) parent.findViewById(R.id.school_5);
        schoolTitle = (TextView) parent.findViewById(R.id.school_title);
        schoolHappy = (ImageView) parent.findViewById(R.id.school_6);
        friendSad = (ImageView) parent.findViewById(R.id.friend_1);
        friendScore = (TextView) parent.findViewById(R.id.friend_score);
        friend2 = (ImageView) parent.findViewById(R.id.friend_2);
        friend3 = (ImageView) parent.findViewById(R.id.friend_3);
        friend4 = (ImageView) parent.findViewById(R.id.friend_4);
        friend5 = (ImageView) parent.findViewById(R.id.friend_5);
        friendTitle = (TextView) parent.findViewById(R.id.friend_title);
        friendHappy = (ImageView) parent.findViewById(R.id.friend_6);
        homeSad = (ImageView) parent.findViewById(R.id.home_1);
        homeScore = (TextView) parent.findViewById(R.id.home_score);
        home2 = (ImageView) parent.findViewById(R.id.home_2);
        home3 = (ImageView) parent.findViewById(R.id.home_3);
        home4 = (ImageView) parent.findViewById(R.id.home_4);
        home5 = (ImageView) parent.findViewById(R.id.home_5);
        homeTitle = (TextView) parent.findViewById(R.id.home_title);
        homeHappy = (ImageView) parent.findViewById(R.id.home_6);
        noteView = (EditText) parent.findViewById(R.id.note);

        referenceDateView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                referenceDatePicker();
                return true;
            }
        });

        // School Title depends on client's age
        if (client != null) {
            // Calculate client's age
            Calendar dob = Calendar.getInstance();
            dob.setTime(client.getDateOfBirth());
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());
            int age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (now.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            if (age < 16) {
                schoolTitle.setText("School*");
            }
        }


        schoolSad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSchoolScore((ImageView) view, 1);
            }
        });
        school2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSchoolScore((ImageView) view, 2);
            }
        });
        school3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSchoolScore((ImageView) view, 3);
            }
        });
        school4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSchoolScore((ImageView) view, 4);
            }
        });
        school5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSchoolScore((ImageView) view, 5);
            }
        });
        schoolHappy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSchoolScore((ImageView) view, 6);
            }
        });

        friendSad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateFriendScore((ImageView) view, 1);
            }
        });
        friend2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateFriendScore((ImageView) view, 2);
            }
        });
        friend3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateFriendScore((ImageView) view, 3);
            }
        });
        friend4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateFriendScore((ImageView) view, 4);
            }
        });
        friend5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateFriendScore((ImageView) view, 5);
            }
        });
        friendHappy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateFriendScore((ImageView) view, 6);
            }
        });

        homeSad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateHomeScore((ImageView) view, 1);
            }
        });
        home2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateHomeScore((ImageView) view, 2);
            }
        });
        home3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateHomeScore((ImageView) view, 3);
            }
        });
        home4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateHomeScore((ImageView) view, 4);
            }
        });
        home5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateHomeScore((ImageView) view, 5);
            }
        });
        homeHappy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateHomeScore((ImageView) view, 6);
            }
        });

        // Cancel Button
        Button cancelButton = (Button) parent.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Cancel so no need to update list of documents
                ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                // Build 200 Use the androidX Fragment class
                //FragmentManager fragmentManager = getFragmentManager();
                //fragmentManager.popBackStack();
                getParentFragmentManager().popBackStack();
                ComponentName compName = ((CRISActivity) getActivity()).getCompName();
                DevicePolicyManager deviceManager = ((CRISActivity) getActivity()).getDeviceManager();
                boolean active = deviceManager.isAdminActive(compName);
                if (active) {
                    deviceManager.lockNow();
                } else {
                    Intent intent = new Intent(parent.getContext(), Login.class);
                    intent.putExtra(Main.EXTRA_EMAIL_ADDRESS, currentUser.getEmailAddress());
                    startActivity(intent);
                }
            }
        });
        // Save Button
        Button saveButton = (Button) parent.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {
                    editDocument.save(isNewMode);
                    // Build 200 Use the androidX Fragment class
                    //FragmentManager fragmentManager = getFragmentManager();
                    //fragmentManager.popBackStack();
                    getParentFragmentManager().popBackStack();
                    ComponentName compName = ((CRISActivity) getActivity()).getCompName();
                    DevicePolicyManager deviceManager = ((CRISActivity) getActivity()).getDeviceManager();
                    boolean active = deviceManager.isAdminActive(compName);
                    if (active) {
                        deviceManager.lockNow();
                    } else {
                        Intent intent = new Intent(parent.getContext(), Login.class);
                        intent.putExtra(Main.EXTRA_EMAIL_ADDRESS, currentUser.getEmailAddress());
                        startActivity(intent);
                    }
                }
            }
        });

        // Set Defaults
        if (isNewMode) {
            // ReferenceDate is the Session Date so is set in the calling activity, ListSessionClientsFragment.
            //editDocument.setReferenceDate(new Date());
        }
        referenceDateView.setText(sDate.format(editDocument.getReferenceDate()));
        schoolScore.setText(String.format(Locale.UK, "%d", editDocument.getSchoolScore()));
        switch (editDocument.getSchoolScore()) {
            case 1:
                schoolSad.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.sad_face_dark));
                break;
            case 2:
                school2.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 3:
                school3.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 4:
                school4.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 5:
                school5.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 6:
                schoolHappy.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.happy_face_dark));
                break;
        }
        friendScore.setText(String.format(Locale.UK, "%d", editDocument.getFriendshipScore()));
        switch (editDocument.getFriendshipScore()) {
            case 1:
                friendSad.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.sad_face_dark));
                break;
            case 2:
                friend2.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 3:
                friend3.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 4:
                friend4.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 5:
                friend5.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 6:
                friendHappy.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.happy_face_dark));
                break;
        }
        homeScore.setText(String.format(Locale.UK, "%d", editDocument.getHomeScore()));
        switch (editDocument.getHomeScore()) {
            case 1:
                homeSad.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.sad_face_dark));
                break;
            case 2:
                home2.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 3:
                home3.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 4:
                home4.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 5:
                home5.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
                break;
            case 6:
                homeHappy.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.happy_face_dark));
                break;
        }
        noteView.setText(editDocument.getNote());
    }

    // MENU BLOCK
    private static final int MENU_CANCEL_DOCUMENT = Menu.FIRST + 1;
    private static final int MENU_UNCANCEL_DOCUMENT = Menu.FIRST + 2;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Initialise the Cancellation menu option
        if (editDocument.getCancelledFlag()) {
            MenuItem cancelOption = menu.add(0, MENU_UNCANCEL_DOCUMENT, 2, "Remove Cancellation");
            cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        } else {
            MenuItem cancelOption = menu.add(0, MENU_CANCEL_DOCUMENT, 3, "Cancel Document");
            cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        // Share option only exists if called from ListClientHeader
        if (shareOption != null) {
            shareOption.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case MENU_CANCEL_DOCUMENT:
                cancelDocument(true);
                return true;

            case MENU_UNCANCEL_DOCUMENT:
                cancelDocument(false);
                return true;

            default:
                return false;
        }
    }

    private void cancelDocument(boolean cancelType) {
        if (cancelType) {
            // Get the reason and then call the validate/save sequence.
            final EditText editText = new EditText(getActivity());
            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            new AlertDialog.Builder(getActivity())
                    .setView(editText)
                    .setTitle("Cancel Document")
                    .setMessage("Documents may not be removed, but cancelling them " +
                            "will remove them from view unless the user explicitly requests " +
                            "them. Please specify a cancellation reason")
                    .setPositiveButton("CancelDocument", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (editText.getText().length() > 0) {
                                editDocument.setCancellationDate(new Date());
                                editDocument.setCancellationReason(editText.getText().toString());
                                // Build 150 - Set the CancelledByID
                                //currentUser.getUserID();
                                editDocument.setCancelledByID(currentUser.getUserID());
                                editDocument.setCancelledFlag(true);
                                if (validate()) {
                                    editDocument.save(isNewMode);
                                    // Build 200 Use the androidX Fragment class
                                    //FragmentManager fragmentManager = getFragmentManager();
                                    //fragmentManager.popBackStack();
                                    getParentFragmentManager().popBackStack();

                                }
                            }
                        }
                    })
                    .setNegativeButton("DoNotCancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        } else {  // Uncancel the Document
            editDocument.setCancelledFlag(false);
            editDocument.setCancellationReason("");
            editDocument.setCancellationDate(new Date(Long.MIN_VALUE));
            editDocument.setCancelledByID(null);
            if (validate()) {
                editDocument.save(isNewMode);
                // Build 200 Use the androidX Fragment class
                //FragmentManager fragmentManager = getFragmentManager();
                //fragmentManager.popBackStack();
                getParentFragmentManager().popBackStack();
            }
        }
    }

    private void updateSchoolScore(ImageView view, int score) {
        schoolScore.setText(String.format(Locale.UK, "%d", score));
        editDocument.setSchoolScore(score);
        // Clear existing selection
        schoolSad.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.sad_face));
        school2.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        school3.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        school4.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        school5.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        schoolHappy.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.happy_face));
        switch (score) {
            case 1:
                schoolSad.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.sad_face_dark));
                break;
            case 6:
                schoolHappy.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.happy_face_dark));
                break;
            default:
                view.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
        }
    }

    private void updateFriendScore(ImageView view, int score) {
        friendScore.setText(String.format(Locale.UK, "%d", score));
        editDocument.setFriendshipScore(score);
        // Clear existing selection
        friendSad.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.sad_face));
        friend2.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        friend3.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        friend4.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        friend5.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        friendHappy.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.happy_face));
        switch (score) {
            case 1:
                friendSad.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.sad_face_dark));
                break;
            case 6:
                friendHappy.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.happy_face_dark));
                break;
            default:
                view.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
        }
    }

    private void updateHomeScore(ImageView view, int score) {
        homeScore.setText(String.format(Locale.UK, "%d", score));
        editDocument.setHomeScore(score);
        // Clear existing selection
        homeSad.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.sad_face));
        home2.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        home3.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        home4.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        home5.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar));
        homeHappy.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.happy_face));
        switch (score) {
            case 1:
                homeSad.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.sad_face_dark));
                break;
            case 6:
                homeHappy.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.happy_face_dark));
                break;
            default:
                view.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.my_week_bar_selected));
        }
    }

    private void referenceDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog referenceDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                referenceDateView.setText(sDate.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        referenceDatePickerDialog.show();
    }

    // Validate the document
    private boolean validate() {
        boolean success = true;

        // Clear any existing errors

        referenceDateView.setError(null);

        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

        // ReferenceDate
        String sReferenceDate = referenceDateView.getText().toString();
        if (TextUtils.isEmpty(sReferenceDate)) {
            referenceDateView.setError(getString(R.string.error_field_required));
            focusView = referenceDateView;
            success = false;
        } else {
            Date dReferenceDate = CRISUtil.parseDate(sReferenceDate);
            if (dReferenceDate == null) {
                referenceDateView.setError(getString(R.string.error_invalid_date));
                focusView = referenceDateView;
                success = false;
            } else {
                // Date must not be in the future
                Date today = new Date();
                if (today.before(dReferenceDate)) {
                    referenceDateView.setError(getString(R.string.error_date_in_future));
                    focusView = referenceDateView;
                    success = false;
                } else {
                    editDocument.setReferenceDate(dReferenceDate);
                }
            }
        }

        // SchoolScore
        if (editDocument.getSchoolScore() == 0) {
            schoolTitle.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.red));
            focusView = null;
            success = false;
        }

        // FriendScore
        if (editDocument.getFriendshipScore() == 0) {
            friendTitle.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.red));
            focusView = null;
            success = false;
        }


        // HomeScore
        if (editDocument.getHomeScore() == 0) {
            homeTitle.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.red));
            focusView = null;
            success = false;
        }


        // Note
        editDocument.setNote(noteView.getText().toString());

        if (focusView != null) {
            focusView.requestFocus();
            focusView.requestFocusFromTouch();
        }
        return success;
    }

}
