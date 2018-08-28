package solutions.cris.edit;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.list.ListActivity;
import solutions.cris.object.Client;
import solutions.cris.object.Document;
import solutions.cris.object.ListItem;
import solutions.cris.object.ListType;
import solutions.cris.object.Note;
import solutions.cris.object.NoteType;
import solutions.cris.object.Role;
import solutions.cris.object.User;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.PickList;

/**
 * Copyright CRIS.Solutions 21/12/2016.
 */

public class EditNote extends Fragment {

    private static final SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
    private static final SimpleDateFormat sDateEdit = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);
    private static final SimpleDateFormat sDateTime = new SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.UK);

    private Toolbar toolbar;
    private Client client;
    private Note editDocument;
    private LocalDB localDB;
    private View parent;
    private Note.Mode mode;
    private User author;
    private Date responseCreationDate;
    private User currentUser;

    private FloatingActionButton fab;
    private Spinner noteTypeSpinner;
    private TextView noteTypeTextView;
    private EditText creationDateView;
    private EditText authorView;
    private CheckBox stickyFlagView;
    private EditText stickyDateView;
    private EditText contentView;
    private EditText responseView;

    MenuItem cancelOption = null;

    private Button cancelButton;
    private Button saveButton;

    private TextView stickyHintTextView;
    private TextView contentHintTextView;
    private LinearLayout hintBox;
    private boolean stickyHintTextDisplayed = true;
    private boolean contentHintTextDisplayed = true;
    private PickList noteTypePickList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure onCreateOptionsMenu is called
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        parent = inflater.inflate(R.layout.edit_note, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toolbar = ((ListActivity) getActivity()).getToolbar();
        TextView footer = (TextView) getActivity().findViewById(R.id.footer);
        currentUser = ((ListActivity) getActivity()).getCurrentUser();
        editDocument = (Note) ((ListActivity) getActivity()).getDocument();
        fab = ((ListActivity) getActivity()).getFab();
        mode = ((ListActivity) getActivity()).getMode();
        client = ((ListActivity) getActivity()).getClient();
        // Swipe to be added to read in ListDocumentsFragment when adapter moved
        // do ListActivity. Until then, don't show message in footer
        footer.setText("");
        //footer.setText(R.string.action_swipe_left_for_unread);

        // Set up the hint text
        stickyHintTextView = (TextView) parent.findViewById(R.id.hint_text);
        hintBox = (LinearLayout) parent.findViewById(R.id.hint_box);
        stickyHintTextView.setText(getHintText());
        stickyHintTextDisplayed = toggleHint(stickyHintTextView, stickyHintTextDisplayed);
        stickyHintTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stickyHintTextDisplayed = toggleHint(stickyHintTextView, stickyHintTextDisplayed);
            }
        });

        // ContentHintText
        contentHintTextView = (TextView) parent.findViewById(R.id.content_hint_text);
        contentHintTextView.setText(getContentHintText());
        contentHintTextDisplayed = toggleHint(contentHintTextView, contentHintTextDisplayed);
        contentHintTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contentHintTextDisplayed = toggleHint(contentHintTextView, contentHintTextDisplayed);
            }
        });


        // Get the document to be edited from the activity
        localDB = LocalDB.getInstance();

        noteTypeSpinner = (Spinner) parent.findViewById(R.id.note_type_spinner);
        noteTypeTextView = (TextView) parent.findViewById(R.id.note_type_read_text);
        authorView = (EditText) parent.findViewById(R.id.text_author);
        authorView.setInputType(InputType.TYPE_NULL);
        authorView.setFocusable(false);
        creationDateView = (EditText) parent.findViewById(R.id.text_creation_date);
        creationDateView.setInputType(InputType.TYPE_NULL);
        creationDateView.setFocusable(false);
        stickyFlagView = (CheckBox) parent.findViewById(R.id.sticky_flag);
        stickyDateView = (EditText) parent.findViewById(R.id.sticky_date);
        contentView = (EditText) parent.findViewById(R.id.note_content);
        responseView = (EditText) parent.findViewById(R.id.note_response);
        cancelButton = (Button) parent.findViewById(R.id.cancel_button);
        saveButton = (Button) parent.findViewById(R.id.save_button);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mode == Document.Mode.NEW) {
            // Set data values
            responseView.setVisibility(View.GONE);
        } else {
            noteTypeSpinner.setVisibility(View.GONE);
            noteTypeTextView.setVisibility(View.VISIBLE);
            noteTypeTextView.setFocusable(false);
            contentView.setInputType(InputType.TYPE_NULL);
            contentView.setSingleLine(false);
            contentView.setFocusable(false);
            String content = editDocument.getContent();
            if (editDocument.getResponseContent() != null) {
                content += editDocument.getResponseContent();
            }
            contentView.setText(content);
        }
        switch (mode) {
            case NEW:
                toolbar.setTitle(getString(R.string.app_name) + " - New Note");
                doShowFab();
                hintBox.setVisibility(View.VISIBLE);
                // Initialise the NodeType Spinner
                noteTypePickList = new PickList(localDB, ListType.NOTE_TYPE);
                ArrayAdapter<String> noteTypeAdapter = new
                        ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, noteTypePickList.getOptions());
                noteTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                noteTypeSpinner.setAdapter(noteTypeAdapter);
                noteTypeSpinner.setSelection(noteTypePickList.getDefaultPosition());
                stickyFlagView.setChecked(false);
                stickyDateView.setOnLongClickListener(new View.OnLongClickListener() {
                    public boolean onLongClick(View view) {
                        stickyDatePicker();
                        return true;
                    }
                });

                // Load the appropriate template if the NoteType changes
                noteTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position != 0) {
                            NoteType noteType = (NoteType) noteTypePickList.getListItems().get(position);
                            if (!noteType.getTemplate().isEmpty()) {
                                String newContent = "";
                                newContent += contentView.getText().toString().trim();
                                if (!newContent.isEmpty()) {
                                    newContent += "\n";
                                }
                                newContent += noteType.getTemplate();
                                contentView.setText(newContent);
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                author = User.getCurrentUser();
                authorView.setText(author.getFullName());
                creationDateView.setText(sDateTime.format(editDocument.getCreationDate()));
                responseView.setVisibility(View.GONE);
                contentView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        insertHashtag();
                        return false;
                    }
                });
                // Cancel Button
                cancelButton.setVisibility(View.VISIBLE);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FragmentManager fragmentManager = getFragmentManager();
                        fragmentManager.popBackStack();
                    }
                });
                // Save Button
                saveButton.setVisibility(View.VISIBLE);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (validate()) {
                            editDocument.save(true, author);
                                FragmentManager fragmentManager = getFragmentManager();
                                fragmentManager.popBackStack();
                        }
                    }
                });
                if (cancelOption != null) {
                    cancelOption.setVisible(false);
                }
                break;
            case EDIT:
                toolbar.setTitle(getString(R.string.app_name) + " - Edit Note");
                doShowFab();
                doCancelBox();
                noteTypeTextView.setText(editDocument.getNoteType().getItemValue());
                hintBox.setVisibility(View.VISIBLE);
                author = localDB.getUser(editDocument.getCreatedByID());
                authorView.setText(author.getFullName());
                if (editDocument.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                    creationDateView.setText(sDateTime.format(editDocument.getCreationDate()));
                } else {
                    creationDateView.setText(sDateTime.format(editDocument.getReferenceDate()));
                }
                stickyFlagView.setChecked(editDocument.isStickyFlag());
                if (editDocument.getStickyDate().getTime() != Long.MIN_VALUE) {
                    stickyDateView.setText(sDateEdit.format(editDocument.getStickyDate()));
                }
                stickyDateView.setOnLongClickListener(new View.OnLongClickListener() {
                    public boolean onLongClick(View view) {
                        stickyDatePicker();
                        return true;
                    }
                });
                responseView.setVisibility(View.GONE);
                // Cancel Button
                cancelButton.setVisibility(View.VISIBLE);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Cancel so no need to update list of documents
                        ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                        FragmentManager fragmentManager = getFragmentManager();
                        fragmentManager.popBackStack();
                    }
                });
                // Save Button
                saveButton.setVisibility(View.VISIBLE);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (validateEdit()) {
                            editDocument.save(false, author);
                                FragmentManager fragmentManager = getFragmentManager();
                                fragmentManager.popBackStack();
                        }
                    }
                });
                if (cancelOption != null) {
                    cancelOption.setVisible(false);
                }
                break;
            case READ:
                toolbar.setTitle(getString(R.string.app_name) + " - Note");
                doShowFab();
                doCancelBox();
                hintBox.setVisibility(View.GONE);
                author = localDB.getUser(editDocument.getCreatedByID());
                authorView.setText(author.getFullName());
                if (editDocument.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                    creationDateView.setText(sDateTime.format(editDocument.getCreationDate()));
                } else {
                    creationDateView.setText(sDateTime.format(editDocument.getReferenceDate()));
                }
                stickyFlagView.setVisibility(View.GONE);
                stickyDateView.setInputType(InputType.TYPE_NULL);
                stickyDateView.setFocusable(false);
                noteTypeTextView.setText(editDocument.getNoteType().getItemValue());
                // Use Stickydate field to display different types of stickiness
                String stickyText;
                if (editDocument.isStickyFlag()) {
                    stickyText = "(Note is sticky.)";
                } else {
                    stickyText = "(Note is not sticky.)";
                }

                if (editDocument.getStickyDate().getTime() != Long.MIN_VALUE) {
                    stickyText = sDate.format(editDocument.getStickyDate());
                }
                stickyDateView.setText(stickyText);
                responseView.setVisibility(View.GONE);
                cancelButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);
                if (cancelOption != null) {
                    cancelOption.setVisible(false);
                }
                break;
            case RESPONSE:
                toolbar.setTitle(getString(R.string.app_name) + " - New Response");
                doShowFab();
                hintBox.setVisibility(View.VISIBLE);
                author = User.getCurrentUser();
                authorView.setText(author.getFullName());
                creationDateView.setText(sDateTime.format(responseCreationDate));
                stickyFlagView.setVisibility(View.GONE);
                stickyDateView.setInputType(InputType.TYPE_NULL);
                stickyDateView.setFocusable(false);
                noteTypeTextView.setText(editDocument.getNoteType().getItemValue());
                // Use Stickydate field to display different types of stickiness
                if (editDocument.isStickyFlag()) {
                    stickyText = "(Note is sticky.)";
                } else {
                    stickyText = "(Note is not sticky.)";
                }

                if (editDocument.getStickyDate().getTime() != Long.MIN_VALUE) {
                    stickyText = sDate.format(editDocument.getStickyDate());
                }
                stickyDateView.setText(stickyText);
                responseView.setVisibility(View.VISIBLE);
                responseView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        insertHashtag();
                        return false;
                    }
                });
                // Cancel Button
                cancelButton.setVisibility(View.VISIBLE);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Cancel so no need to update list of documents
                        ((ListActivity) getActivity()).setMode(Document.Mode.READ);
                        FragmentManager fragmentManager = getFragmentManager();
                        fragmentManager.popBackStack();
                    }
                });
                // Save Button
                saveButton.setVisibility(View.VISIBLE);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (saveResponse()) {
                            // Although this starts as a READ, the new response note makes in a NEW
                            ((ListActivity) getActivity()).setMode(Document.Mode.NEW);
                            FragmentManager fragmentManager = getFragmentManager();
                            fragmentManager.popBackStack();
                        }
                    }
                });
                if (cancelOption != null) {
                    cancelOption.setVisible(false);
                }
                responseView.requestFocus();
                //InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);
                //mgr.showSoftInput(responseView, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // MENU BLOCK
    private static final int MENU_CANCEL_DOCUMENT = Menu.FIRST + 1;
    private static final int MENU_UNCANCEL_DOCUMENT = Menu.FIRST + 2;
    private static final int MENU_HASHTAG = Menu.FIRST +3;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem shareOption = menu.findItem(R.id.menu_item_share);
        switch (mode) {
            case EDIT:
                // V1.1 Only allow cancellation if the initial note
                if (!editDocument.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                    // Initialise the Cancellation menu option
                    if (editDocument.getCancelledFlag()) {
                        cancelOption = menu.add(0, MENU_UNCANCEL_DOCUMENT, 2, "Remove Cancellation");
                        cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    } else {
                        cancelOption = menu.add(0, MENU_CANCEL_DOCUMENT, 2, "Cancel Document");
                        cancelOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    }
                }
                shareOption.setVisible(false);
                break;
            case NEW:
            case RESPONSE:
                MenuItem hashtagOption = menu.add(0, MENU_HASHTAG, 3, "Insert Hashtag");
                hashtagOption.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                shareOption.setVisible(false);
                break;
            case READ:
                shareOption.setVisible(true);
                createShareActionProvider(shareOption);
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

            case MENU_HASHTAG:
                insertHashtag();
                return true;

            default:
                return false;
        }
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

    private void insertHashtag() {

        final PickList hashTags = new PickList(localDB, ListType.HASHTAG, 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Append to note:");
        ArrayList<String> itemList = hashTags.getOptions();
        String[] items = itemList.toArray(new String[itemList.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Field to insert hashtag depends on  mode
                EditText editView;
                if (mode == Document.Mode.RESPONSE){
                    editView = responseView;
                } else {
                    editView = contentView;
                }
                String hashTag = hashTags.getOptions().get(which);
                // Insert the hastag at the current cursor position
                int cursorPos = editView.getSelectionStart();
                String content = editView.getText().toString();
                String newContent = String.format("%s %s %s",
                        content.substring(0,cursorPos),
                        hashTag,
                        content.substring(cursorPos));
                editView.setText(newContent);
                editView.setSelection(cursorPos + hashTag.length() + 2);
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean toggleHint(TextView view, boolean current) {
        if (current) {
            view.setMaxLines(2);
            return false;
        } else {

            view.setMaxLines(view.getLineCount());
            return true;
        }
    }


    private void doShowFab() {
        // Fab is displayed in Edit and Read modes for non-response documents
        // Initially, hide the FAB
        fab.setVisibility(View.GONE);
        // Now check whether to display
        switch (mode) {
            case NEW:
            case RESPONSE:
                fab.setVisibility(View.GONE);
                break;
            case EDIT:
            case READ:
                if (editDocument.getNoteTypeID().equals(NoteType.responseNoteTypeID)) {
                    fab.setVisibility(View.GONE);
                } else {
                    // Check access
                    if (currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_ALL_DOCUMENTS)  ||
                            currentUser.getRole().hasPrivilege(Role.PRIVILEGE_WRITE_NOTES) ||
                            currentUser.getRole().hasPrivilege(Role.PRIVILEGE_CREATE_NOTES)) {
                        fab.setVisibility(View.VISIBLE);
                        fab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mode = Document.Mode.RESPONSE;
                                responseCreationDate = new Date();
                                onResume();
                            }
                        });
                    } else {
                        fab.setVisibility(View.GONE);
                    }
                }
        }
    }

    private void doCancelBox() {
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
                                editDocument.setCancelledByID(((ListActivity) getActivity()).getCurrentUser().getUserID());
                                editDocument.setCancelledFlag(true);
                                if (validate()) {
                                    editDocument.save(mode == Document.Mode.NEW, author);
                                        FragmentManager fragmentManager = getFragmentManager();
                                        fragmentManager.popBackStack();
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
                editDocument.save(mode == Document.Mode.NEW, author);
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.popBackStack();
            }
        }
    }

    private void stickyDatePicker() {
        Calendar newCalendar = Calendar.getInstance();
        DatePickerDialog startDatePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                stickyDateView.setText(sDateEdit.format(newDate.getTime()));
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        startDatePickerDialog.show();
    }

    // Validate the document
    private boolean validate() {
        boolean success = true;
        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;

            // Clear any existing errors
            stickyDateView.setError(null);

            //NoteType
            ListItem newNoteType = noteTypePickList.getListItems().get(noteTypeSpinner.getSelectedItemPosition());
            // Test for Please select
            if (newNoteType.getItemOrder() == -1) {
                TextView errorText = (TextView) noteTypeSpinner.getSelectedView();
                //errorText.setError("anything here, just to add the icon");
                errorText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));//just to highlight that this is an error
                focusView = noteTypeSpinner;
                success = false;
            } else {
                editDocument.setNoteTypeID(newNoteType.getListItemID());
                // PickList contained
                editDocument.setNoteType(localDB.getListItem(editDocument.getNoteTypeID()));
            }

            // StickyFlag
            editDocument.setStickyFlag(stickyFlagView.isChecked());

            // StickyDate
            String sStickyDate = stickyDateView.getText().toString().trim();
            if (TextUtils.isEmpty(sStickyDate)) {
                editDocument.setStickyDate(new Date(Long.MIN_VALUE));
            } else {
                Date dStickyDate = CRISUtil.parseDate(sStickyDate);
                if (dStickyDate == null) {
                    stickyDateView.setError(getString(R.string.error_invalid_date));
                    focusView = stickyDateView;
                    success = false;
                } else {
                    editDocument.setStickyDate(dStickyDate);
                }
            }

            // Content
            editDocument.setContent(contentView.getText().toString().trim());

        if (!success) {
            focusView.requestFocus();
            focusView.requestFocusFromTouch();
        }
        return success;
    }

    // Validate the document
    private boolean validateEdit() {
        boolean success = true;
        // Holds most recent view to fail validation. The validation
        // should check the fields in the displayed order
        View focusView = null;
            // Clear any existing errors
            stickyDateView.setError(null);
            // StickyFlag
            editDocument.setStickyFlag(stickyFlagView.isChecked());
            // StickyDate
            String sStickyDate = stickyDateView.getText().toString().trim();
            if (TextUtils.isEmpty(sStickyDate)) {
                editDocument.setStickyDate(new Date(Long.MIN_VALUE));
            } else {
                Date dStickyDate = CRISUtil.parseDate(sStickyDate);
                if (dStickyDate == null) {
                    stickyDateView.setError(getString(R.string.error_invalid_date));
                    focusView = stickyDateView;
                    success = false;
                } else {
                    editDocument.setStickyDate(dStickyDate);
                }
            }

        if (!success) {
            focusView.requestFocus();
            focusView.requestFocusFromTouch();
        }
        return success;
    }


    private boolean saveResponse() {
        Note newNote = new Note(currentUser, client.getClientID());
        newNote.setNoteTypeID(NoteType.responseNoteTypeID);
        // All responses have an identical reference date to the initial note
        newNote.setReferenceDate(editDocument.getReferenceDate());
        newNote.setContent(responseView.getText().toString().trim());
        String[] lines = newNote.getContent().split("\n");
        newNote.setSummary(String.format("%s - %s ",
                author.getFullName(),
                lines[0]));
        localDB.save(newNote, true, User.getCurrentUser());
        NoteType responseNoteType = new NoteType(NoteType.responseNoteTypeID);
        newNote.setNoteType(responseNoteType);
        return true;
    }

    private String getHintText() {
        return "Ticking the 'sticky' option will display the note at the " +
                "top of the list of documents, regardless of its date. This may be useful " +
                "for notes which need to be especially prominent, such as contact " +
                "instructions or allergies. However, it is important not to over-use this " +
                "facility since, if there are too many sticky notes, the effect will be " +
                "lost. If a Sticky Date is entered (instead of checking the sticky box) " +
                "the note will be 'sticky' until the date specified and then revert to its " +
                "normal position in the document list. This may be useful for notes which " +
                "need to be prominent for a short while (until everyone has seen them).";
    }

    private String getContentHintText() {
        return "Hashtags (the hash sign '#' followed by a word or sequence of words) may " +
                "be included in the note to enable particular notes to be searched for or " +
                "to indicate that a note has a particular purpose. For instance #Signposting " +
                "should be included in any note which relates to discussions with a client " +
                "about other services which may be available to him/her. \n\n" +
                "A number of pre-defined hastags will exist, which may be included using the " +
                "menu option 'Include Hashtag' or by long-pressing the note. However, you can " +
                "also add hashtags of your own simply by typing them directly. If you think " +
                "a hashtag would be useful to others, please ask your CRIS Administrator to add " +
                "the hashtag to the pre-defined list.";

    }

}
