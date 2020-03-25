package solutions.cris.read;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Client;
import solutions.cris.object.MyWeek;
import solutions.cris.object.User;

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

public class ReadMyWeek extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);

    private MyWeek editDocument;
    private View parent;
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
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        client = ((ListActivity) getActivity()).getClient();
        editDocument = (MyWeek) ((ListActivity) getActivity()).getDocument();
        FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        // Swipe to be added to read in ListDocumentsFragment when adapter moved
        // do ListActivity. Until then, don't show message in footer
        footer.setText("");
        //footer.setText(R.string.action_swipe_left_for_unread);
        toolbar.setTitle(getString(R.string.app_name) + " - MyWeek");
        // Hide the FAB
        fab.setVisibility(View.GONE);

        LocalDB localDB = LocalDB.getInstance();

        // CANCEL BOX
        if (editDocument.getCancelledFlag()) {
            LinearLayout cancelBoxView = (LinearLayout) parent.findViewById(R.id.cancel_box_layout);
            cancelBoxView.setVisibility(View.VISIBLE);
            TextView cancelBy = (TextView) parent.findViewById(R.id.cancel_by);
            String byText = "by ";
            User cancelUser = localDB.getUser(editDocument.getCancelledByID());
            byText += cancelUser.getFullName() + " on ";
            byText += sDate.format(editDocument.getCancellationDate());
            cancelBy.setText(byText);
            TextView cancelReason = (TextView) parent.findViewById(R.id.cancel_reason);
            cancelReason.setText(String.format("Reason: %s",editDocument.getCancellationReason()));
        }

        EditText referenceDateView = (EditText) parent.findViewById(R.id.reference_date);
        ImageView schoolSad = (ImageView) parent.findViewById(R.id.school_1);
        TextView schoolScore = (TextView) parent.findViewById(R.id.school_score);
        ImageView school2 = (ImageView) parent.findViewById(R.id.school_2);
        ImageView school3 = (ImageView) parent.findViewById(R.id.school_3);
        ImageView school4 = (ImageView) parent.findViewById(R.id.school_4);
        ImageView school5 = (ImageView) parent.findViewById(R.id.school_5);
        TextView schoolTitle = (TextView) parent.findViewById(R.id.school_title);
        ImageView schoolHappy = (ImageView) parent.findViewById(R.id.school_6);
        ImageView friendSad = (ImageView) parent.findViewById(R.id.friend_1);
        TextView friendScore = (TextView) parent.findViewById(R.id.friend_score);
        ImageView friend2 = (ImageView) parent.findViewById(R.id.friend_2);
        ImageView friend3 = (ImageView) parent.findViewById(R.id.friend_3);
        ImageView friend4 = (ImageView) parent.findViewById(R.id.friend_4);
        ImageView friend5 = (ImageView) parent.findViewById(R.id.friend_5);
        TextView friendTitle = (TextView) parent.findViewById(R.id.friend_title);
        ImageView friendHappy = (ImageView) parent.findViewById(R.id.friend_6);
        ImageView homeSad = (ImageView) parent.findViewById(R.id.home_1);
        TextView homeScore = (TextView) parent.findViewById(R.id.home_score);
        ImageView home2 = (ImageView) parent.findViewById(R.id.home_2);
        ImageView home3 = (ImageView) parent.findViewById(R.id.home_3);
        ImageView home4 = (ImageView) parent.findViewById(R.id.home_4);
        ImageView home5 = (ImageView) parent.findViewById(R.id.home_5);
        TextView homeTitle = (TextView) parent.findViewById(R.id.home_title);
        ImageView homeHappy = (ImageView) parent.findViewById(R.id.home_6);
        EditText noteView = (EditText) parent.findViewById(R.id.note);
        Button cancelButton = (Button) parent.findViewById(R.id.cancel_button);
        Button saveButton = (Button) parent.findViewById(R.id.save_button);

        referenceDateView.setInputType(InputType.TYPE_NULL);
        referenceDateView.setFocusable(false);
        noteView.setInputType(InputType.TYPE_NULL);
        noteView.setFocusable(false);
        cancelButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);

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
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //SHARE
        MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        createShareActionProvider(shareOption);
    }

    // SHARE MENU ITEM (Both methods are required)
    private void createShareActionProvider(MenuItem menuItem) {
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String summary = String.format("%s\n\n%s", client.textSummary(), editDocument.textSummary());
        shareIntent.putExtra(Intent.EXTRA_TEXT, summary);
        shareActionProvider.setShareIntent(shareIntent);
    }

}