package solutions.cris.object;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;
import solutions.cris.utils.CRISUtil;
import solutions.cris.utils.LocalSettings;
import solutions.cris.utils.SwipeDetector;

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

public class NoteType extends ListItem implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_NOTE_TYPE;

    public static final UUID responseNoteTypeID = UUID.fromString("14ad4cb6-7b3c-490c-ba19-315315f73a2f");

    public static final int ICON_COLOUR_UNKNOWN = 0;
    public static final int ICON_COLOUR_RED = 1;
    public static final int ICON_COLOUR_AMBER = 2;
    public static final int ICON_COLOUR_GREEN = 3;
    public static final int ICON_COLOUR_BLUE = 4;
    public static final int ICON_COLOUR_RESPONSE_RED = 5;
    public static final int ICON_COLOUR_RESPONSE_AMBER = 6;
    public static final int ICON_COLOUR_RESPONSE_GREEN = 7;
    public static final int ICON_COLOUR_RESPONSE_BLUE = 8;

    public NoteType(User currentUser, String itemValue, int itemOrder) {
        super(currentUser, ListType.NOTE_TYPE, itemValue, itemOrder);
        template = "";
        supervisorSetToFollow = false;
        // Build 128 Note Icon needs to be one of the above static ints
        //noteIcon = R.drawable.ic_note_blue;
        noteIcon = ICON_COLOUR_BLUE;
    }

    // Constructor for the response NoteType
    public NoteType(UUID noteTypeID) {
        super(new User(User.unknownUser), ListType.NOTE_TYPE, "Response", -1);
        if (!noteTypeID.equals(responseNoteTypeID)) {
            throw new CRISException("Invalid attempt to create Response NoteType with invalid UUID");
        }
        template = "";
        supervisorSetToFollow = false;
    }

    private String template;

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    private boolean supervisorSetToFollow;

    public boolean isSupervisorSetToFollow() {
        return supervisorSetToFollow;
    }

    public void setSupervisorSetToFollow(boolean supervisorSetToFollow) {
        this.supervisorSetToFollow = supervisorSetToFollow;
    }

    private int noteIcon;

    public int getNoteIcon() {
        return noteIcon;
    }

    public void setNoteIcon(int noteIcon) {
        this.noteIcon = noteIcon;
    }

    public void save(boolean isNewMode) {
        LocalDB localDB = LocalDB.getInstance();
        localDB.save(this, isNewMode, User.getCurrentUser());
    }

    public String textSummary() {
        String summary = super.textSummary();
        summary += String.format("Template: %s\n", getTemplate());
        if (isSupervisorSetToFollow()) {
            summary += "Supervisor set to Follow: Yes\n";
        } else {
            summary += "Supervisor set to Follow: No\n";
        }
        return summary;
    }

    public static String getChanges(LocalDB localDB, UUID previousRecordID, UUID thisRecordID, SwipeDetector.Action action) {
        NoteType previousItem = (NoteType) localDB.getListItemByRecordID(previousRecordID);
        NoteType thisItem = (NoteType) localDB.getListItemByRecordID(thisRecordID);
        String changes = ListItem.getChanges(previousItem, thisItem);
        changes += CRISUtil.getChanges(previousItem.getTemplate(), thisItem.getTemplate(), "Template");
        changes += CRISUtil.getChanges(previousItem.isSupervisorSetToFollow(), thisItem.isSupervisorSetToFollow(), "Supervisor set to Follow");
        if (previousItem.getNoteIcon() != thisItem.getNoteIcon()){
            changes += "Note Icon changed.";
        }
        if (changes.length() == 0) {
            changes = "No changes found.\n";
        }
        changes += "-------------------------------------------------------------\n";
        return changes;
    }
}
