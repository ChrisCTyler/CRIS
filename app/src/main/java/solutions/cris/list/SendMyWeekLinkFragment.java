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
import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.Main;
import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.edit.EditNote;
import solutions.cris.exceptions.CRISException;
import solutions.cris.object.Client;
import solutions.cris.object.ClientSession;
import solutions.cris.object.Document;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.Note;
import solutions.cris.object.NoteType;
import solutions.cris.object.Session;
import solutions.cris.object.User;


public class SendMyWeekLinkFragment extends Fragment {

    // Build 139 - Send texts in batches to overcome a network provide limit
    // Build 147 - Reduced batch size to hopefully fix send problems
    //public static final int SMS_BATCH_LIMIT = 25;
    public static final int SMS_BATCH_LIMIT = 5;
    public int smsBatchCount = 0;

    public enum SendStatuses {SUCCESS, FAIL, NOT_SENT}
    public final static String[] messageModes = {"Text", "Email", "NoAction"};

    private ArrayList<SendMyWeekClientSessionEntry> sendMyWeekLinkEntryList;
    private ListView listView;
    private View parent;
    private Session session;
    private LocalDB localDB;
    private User currentUser;
    //private EditText messageView;
    private Button sendButton;
    private String buttonText;
    private CheckBox createNoteView;
    // Email added for build 119
    ListItem emailListItem;
    ListItem textListItem;
    ListItem phoneListItem;
    Note phoneNote = null;

    private SendMyWeekLinkAdapter sendMyWeekLinkAdapter;
    SmsManager smsManager;
    ArrayList<String> messageList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        //parent = inflater.inflate(R.layout.layout_list, container, false);
        parent =  inflater.inflate(R.layout.layout_send_myweek_link, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Toolbar toolbar = ((ListActivity) getActivity()).getToolbar();
        toolbar.setTitle(getString(R.string.app_name) + " - Send MyWeek Link");
        final FloatingActionButton fab = ((ListActivity) getActivity()).getFab();
        fab.setVisibility(View.INVISIBLE);
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        footer.setText("");
        localDB = LocalDB.getInstance();
        currentUser = User.getCurrentUser();
        // Initialise the list view
        listView = (ListView) parent.findViewById(R.id.list_view);
        //messageView = (EditText) parent.findViewById(R.id.message_content);
        createNoteView = (CheckBox) parent.findViewById(R.id.create_note_flag);
        session = ((ListSessionClients) getActivity()).getSession();

        // Load the list of Broadcast entries from the client list in the parent
        if (sendMyWeekLinkEntryList == null) {
            buttonText = "Send Texts";
            // Build 137
            smsBatchCount = 1;

            sendMyWeekLinkEntryList = new ArrayList<>();
            for (Client client : ((ListActivity) getActivity()).getBroadcastClientList()) {
                SendMyWeekClientSessionEntry entry = new SendMyWeekClientSessionEntry(client);
                // If client has mobile number the text else email
                if (!client.getContactNumber().startsWith("0")) {
                    entry.setMessageMode("Email");
                } else if (client.getContactNumber().startsWith("07")) {
                    entry.setMessageMode("Text");
                } else {
                    entry.setMessageMode("Email");
                }
                sendMyWeekLinkEntryList.add(entry);
            }
        }

        // Add the Text Message, Email and Phone Message Note Types, if necessary
        checkNoteTypesExist();

        // Button
        sendButton = (Button) parent.findViewById(R.id.send_button);
        sendButton.setText(buttonText);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Button sendButton = (Button) view;
                if (sendButton.getText().toString().startsWith("Send Texts")) {
                    sendButton.setEnabled(false);
                    trySendTexts();
                } else if (sendButton.getText().equals("Send Emails")) {
                    sendButton.setEnabled(false);
                    sendEmails();
                }else {
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.popBackStack();
                }
            }
        });
    }

    public void onResume() {
        super.onResume();
        sendButton.setText(buttonText);
        // Create the adapter
        sendMyWeekLinkAdapter = new SendMyWeekLinkAdapter(getActivity(), sendMyWeekLinkEntryList);
        this.listView.setAdapter(sendMyWeekLinkAdapter);

    }
    // MENU BLOCK
    //private static final int MENU_EXPORT = Menu.FIRST + 1;

    private MenuItem searchItem;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // No menu options
     }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                throw new CRISException("Unexpected menu option:" + item.getItemId());
        }
    }

    private void checkNoteTypesExist(){
        // Check for Text Message, Email and Phone Message Note Types and create if missing
        ArrayList<ListItem> noteTypes = localDB.getAllListItems(ListType.NOTE_TYPE.toString(), true);
        int newItemOrder = noteTypes.size();
        emailListItem = localDB.getListItem("Email", ListType.NOTE_TYPE);
        if (emailListItem == null){
            emailListItem = new NoteType(User.getCurrentUser(),"Email",newItemOrder++);
            emailListItem.save(true);
        }
        textListItem = localDB.getListItem("Text Message", ListType.NOTE_TYPE);
        if (textListItem == null){
            textListItem = new NoteType(User.getCurrentUser(),"Text Message",newItemOrder++);
            textListItem.save(true);
        }
    }

    private void doNoPrivilege() {
        new AlertDialog.Builder(getActivity())
                .setTitle("No Privilege")
                .setMessage("Unfortunately, this option is not available.")
                .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void trySendTexts(){
        // First we need to check whether the user has granted the SMS permission
        if (ContextCompat.checkSelfPermission(((ListActivity)getActivity()), Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            sendButton.setEnabled(true);
            ActivityCompat.requestPermissions(((ListActivity)getActivity()),
                    new String[]{Manifest.permission.SEND_SMS},
                    Main.REQUEST_PERMISSION_SEND_SMS);
        } else if (ContextCompat.checkSelfPermission(((ListActivity)getActivity()), Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            sendButton.setEnabled(true);
            ActivityCompat.requestPermissions(((ListActivity)getActivity()),
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    Main.REQUEST_PERMISSION_SEND_SMS);
        } else {
            sendTexts();
        }
    }
    // onRequestPermissionsResult receiver is in ListActivity and simply acknowledges
    // the setting of the permission. User needs to click the button again

    public static String getMyWeekLinkMessage(String organisation, String dbName,
                                              Client client, UUID sessionID){

        dbName = dbName.substring(9);
        // client name and clientid are combined along with a very poor (but adequate
        // in the circumstances) bit of encryption
        String firstName = client.getFirstNames();
        String firstNameLength = String.format("%2s",firstName.length()).replace(' ', '0');;
        String lastName = client.getLastName();
        String lastNameLength = String.format("%2s",lastName.length()).replace(' ', '0');;
        String fullName = firstNameLength + firstName + lastNameLength + lastName;
        while (fullName.length() < 36){
            fullName += fullName;
        }
        if (fullName.length() > 36){
            fullName = fullName.substring(0,36);
        }
        byte[] byteFullName = fullName.getBytes();
        for (int i=0; i<byteFullName.length; i++){
            byteFullName[i] = (byte)(byteFullName[i]+1);
        }
        byte[] byteClientID = client.getClientID().toString().getBytes();
        byte[] byteClientParam = new byte[72];
        for (int i=0; i<36; i++){
            byteClientParam[i*2] = byteClientID[i];
            byteClientParam[i*2+1] = byteFullName[i];
        }
        String clientParam = new String(byteClientParam);

        String message = organisation + " would like to hear how young carers are. ";
        message += "Please click on the link below for young carers to fill in their ";
        message += "My Week form:\n\n";
        message += "https://cris.solutions/myweek/myweek/";
        message += "?cid=" + clientParam;
        message += "&sid=" + sessionID.toString();
        message += "&did=" + dbName;
        message += " ";

        return message;
    }

    public void sendTexts() {
        SmsManager smsManager = SmsManager.getDefault();

        // Test for SMS capability on device
        boolean deviceHasSMS = false;
        // Build 137 - Some network providers have set a limit on the number of texts which
        // can be sent in one go. To work around this, they will be sent in batches of 20
        Integer textsSent = 0;

        Bundle configValues = smsManager.getCarrierConfigValues();
        if (configValues != null){
            String test = configValues.toString();
            deviceHasSMS = true;
        }

        // The organisation and dbName are needed for the message
        SharedPreferences prefs = ((ListActivity)getActivity()).getSharedPreferences(
                getString(R.string.shared_preference_file), Context.MODE_PRIVATE);
        String organisation = "CRIS";
        if (prefs.contains(getString(R.string.pref_organisation))) {
            organisation = prefs.getString(getString(R.string.pref_organisation), "");
        }
        String dbName = localDB.getDatabaseName();

        // Loop through the broadcast list looking for text mode clients
        for (Integer count = 0;count<sendMyWeekLinkEntryList.size();count++){
            SendMyWeekClientSessionEntry entry = sendMyWeekLinkEntryList.get(count);
            // Build 137 - Could be 2nd batch so check sent flag
            //if (entry.getMessageMode().equals("Text")){
            if (entry.getMessageMode().equals("Text") &&
                    entry.getSendStatus().equals(SendStatuses.NOT_SENT) &&
                    textsSent < SMS_BATCH_LIMIT){
                textsSent++;
                // Clear the send result
                entry.setSendResult("");
                if (deviceHasSMS) {
                    try {
                        // Message is specific to the client so build it here
                        String message = SendMyWeekLinkFragment.getMyWeekLinkMessage(organisation,
                                dbName, entry.getClient(),
                                session.getDocumentID());
                        ArrayList<String> dividedMessage = smsManager.divideMessage(message);
                        String action = String.format("%d", count);
                        PendingIntent piSend = PendingIntent.getBroadcast(((ListActivity)getContext()), 0, new Intent(action), 0);
                        ArrayList<PendingIntent> piSendArray = new ArrayList<>();
                        for (Integer i = 0;i<dividedMessage.size();i++){
                            piSendArray.add(piSend);
                        }
                        ((ListActivity)getActivity()).registerReceiver(new BroadcastReceiver() {

                            @Override
                            public void onReceive(Context context, Intent intent) {
                                // Check the result
                                String message = "";
                                switch (getResultCode()) {
                                    case Activity.RESULT_OK:
                                        message = "";
                                        break;
                                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                        message = "Unknown Error";
                                        break;
                                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                                        message = "No Service";
                                        break;
                                    case SmsManager.RESULT_ERROR_NULL_PDU:
                                        message = "Null PDU";
                                        break;
                                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                                        message = "Radio off";
                                        break;
                                    case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:
                                        message = "Over Limit";
                                        break;
                                    case SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED:
                                        message = "Not allowed";
                                        break;
                                    case SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED:
                                        message = "Not allowed";
                                        break;
                                    default:
                                        message = String.format("Error: %d",getResultCode());
                                }
                                // Find the entry associated with this send
                                Integer entryCount = Integer.parseInt(intent.getAction());
                                SendMyWeekClientSessionEntry entry = sendMyWeekLinkEntryList.get(entryCount);
                                if (entry.getSendResult().length() == 0) {
                                    entry.setSendResult(message);
                                    if (getResultCode() == Activity.RESULT_OK){
                                        entry.setSendStatus(SendStatuses.SUCCESS);
                                        // Add note
                                        if (createNoteView.isChecked() && !entry.isNoteAdded()){
                                            Note newNote = new Note(currentUser, entry.getClient().getClientID());
                                            String broadcastMessage = message;
                                            newNote.setContent("Link to MyWeek sent by Text");
                                            newNote.setNoteType(textListItem);
                                            newNote.setNoteTypeID(textListItem.getListItemID());
                                            newNote.save(true,User.getCurrentUser());
                                            // Build 128 Set isNoteAdded flag to prevent multiples
                                            entry.setNoteAdded(true);
                                        }
                                    } else {
                                        entry.setSendStatus(SendStatuses.FAIL);
                                        entry.setMessageMode("Email");
                                    }
                                    sendMyWeekLinkAdapter.notifyDataSetChanged();
                                }
                            }
                        }, new IntentFilter(action));
                        smsManager.sendMultipartTextMessage(entry.getClient().getContactNumber(),
                                null,
                                dividedMessage,
                                piSendArray,
                                null);
                    } catch (Exception e) {
                        // SMS Send failed so switch to email and put a cross
                        entry.setSendResult("Exception");
                        entry.setMessageMode("Email");
                        entry.setSendStatus(SendStatuses.FAIL);
                    }
                }
                else {
                    // SMS Send failed so switch to email and put a cross
                    entry.setSendResult("No SMS");
                    entry.setMessageMode("Email");
                    entry.setSendStatus(SendStatuses.FAIL);
                }
            }
        }
        // Update the view
        sendMyWeekLinkAdapter.notifyDataSetChanged();
        if (textsSent == SMS_BATCH_LIMIT){
            smsBatchCount++;
            sendButton.setText(String.format("Send Texts (%d)", smsBatchCount));
        } else {
            sendButton.setText("Send Emails");
        }
        sendButton.setEnabled(true);
    }

    private void sendEmails(){

        // The organisation and dbName are needed for the message
        SharedPreferences prefs = ((ListActivity)getActivity()).getSharedPreferences(
                getString(R.string.shared_preference_file), Context.MODE_PRIVATE);
        String organisation = "CRIS";
        if (prefs.contains(getString(R.string.pref_organisation))) {
            organisation = prefs.getString(getString(R.string.pref_organisation), "");
        }
        String dbName = localDB.getDatabaseName();
        String subject = String.format("MyWeek Link from %s",organisation);


        // Loop through the broadcast list looking for email mode clients
        for (SendMyWeekClientSessionEntry entry : sendMyWeekLinkEntryList){
            if (entry.getMessageMode().equals("Email")){
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("text/plain");
                // Formulate the message
                String message = SendMyWeekLinkFragment.getMyWeekLinkMessage(organisation,
                        dbName, entry.getClient(),
                        session.getDocumentID());


                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                emailIntent.putExtra(Intent.EXTRA_TEXT, message);
                String[] emails = {entry.getClient().getEmailAddress()};
                emailIntent.putExtra(Intent.EXTRA_EMAIL, emails);
                entry.setSendStatus(SendStatuses.SUCCESS);
                // Clear the send result
                entry.setSendResult("");
                startActivity(emailIntent);

                // Add notes
                if (createNoteView.isChecked() && !entry.isNoteAdded()){
                    Note newNote = new Note(currentUser, entry.getClient().getClientID());
                    newNote.setContent("Link to MyWeek sent by Email");
                    newNote.setNoteType(emailListItem);
                    newNote.setNoteTypeID(emailListItem.getListItemID());
                    newNote.save(true,User.getCurrentUser());
                    // Build 128 Set isNoteAdded flag to prevent multiples
                    entry.setNoteAdded(true);
                }
                // Update the view
                sendMyWeekLinkAdapter.notifyDataSetChanged();

            }
        }
        sendButton.setEnabled(true);
        buttonText = "Finish";
        sendButton.setText(buttonText);
    }

    private class SendMyWeekClientSessionEntry {

        SendMyWeekClientSessionEntry(Client client) {
            this.client = client;
            this.sendStatus = SendStatuses.NOT_SENT;
            this.sendResult = "";
            this.noteAdded = false;
        }

        private Client client;
        Client getClient() {
            return client;
        }

        private SendStatuses sendStatus;
        SendStatuses getSendStatus() {return sendStatus;}
        void setSendStatus(SendStatuses sendStatus){this.sendStatus = sendStatus;}

        private String messageMode;
        String getMessageMode() {return messageMode;}
        void setMessageMode(String messageMode) {this.messageMode = messageMode;}

        private String sendResult;
        public String getSendResult() {
            return sendResult;
        }
        public void setSendResult(String sendResult) {
            this.sendResult = sendResult;
        }

        private boolean noteAdded;
        public boolean isNoteAdded() {
            return noteAdded;
        }
        public void setNoteAdded(boolean noteAdded) {
            this.noteAdded = noteAdded;
        }
    }

    private class SendMyWeekLinkAdapter extends ArrayAdapter<SendMyWeekClientSessionEntry> {

        // Constructor
        SendMyWeekLinkAdapter(Context context, List<SendMyWeekClientSessionEntry> objects) {
            super(context, 0, objects);
        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {

            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.layout_broadcast_item, parent, false);
            }
            final SendMyWeekClientSessionEntry sendMyWeekClientSessionEntry =
                    sendMyWeekLinkEntryList.get(position);

            final Client client = sendMyWeekClientSessionEntry.getClient();

            // Display the client's name and additional information
            TextView viewItemMainText = (TextView) convertView.findViewById(R.id.item_main_text);
            TextView viewItemPhoneText = (TextView) convertView.findViewById(R.id.item_phone);
            TextView viewItemEmailText = (TextView) convertView.findViewById(R.id.item_email);
            viewItemMainText.setText(client.getFullName());
            viewItemPhoneText.setText(client.getContactNumber());
            viewItemEmailText.setText(client.getEmailAddress());

            ImageView sentIcon = (ImageView) convertView.findViewById(R.id.sent_icon);
            TextView viewItemSendResult = (TextView) convertView.findViewById(R.id.item_send_result);
            viewItemSendResult.setText(sendMyWeekClientSessionEntry.getSendResult());

            Spinner messageTypeSpinner = (Spinner) convertView.findViewById(R.id.message_type_spinner);
            // Initialise the case Type Spinner
            final ArrayAdapter<String> messageTypeAdapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_item,messageModes);
            messageTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            messageTypeSpinner.setAdapter(messageTypeAdapter);
            messageTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selected = messageTypeAdapter.getItem(position);
                    if (selected != null) {
                        // Clear the send result
                        //broadcastEntry.setSendStatus(SendStatuses.NOT_SENT);
                        //broadcastEntry.setSendResult("");
                        switch (selected) {
                            case "Text":
                                sendMyWeekClientSessionEntry.setMessageMode("Text");
                                break;
                            case "Email":
                                sendMyWeekClientSessionEntry.setMessageMode("Email");
                                break;
                            case "NoAction":
                                sendMyWeekClientSessionEntry.setMessageMode("NoAction");
                                break;
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            messageTypeSpinner.setSelection(messageTypeAdapter.getPosition(sendMyWeekClientSessionEntry.messageMode));

            // Message Sent Icon
            switch (sendMyWeekClientSessionEntry.getSendStatus()) {
                case SUCCESS:    // Message Sent
                    sentIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_tick));
                    break;
                case FAIL:
                    sentIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_cross));
                    break;
                case NOT_SENT:
                    sentIcon.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_tick_grey));
                    break;
            }

            return convertView;
        }
    }

}
