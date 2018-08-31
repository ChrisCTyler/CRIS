package solutions.cris.object;

import java.io.Serializable;
import java.util.UUID;

import solutions.cris.R;
import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;

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
        noteIcon = R.drawable.ic_note_blue;

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
    public int getNoteIcon() {return noteIcon;}
    public void setNoteIcon(int noteIcon) {this.noteIcon = noteIcon;}

    public void save(boolean isNewMode){
        LocalDB localDB = LocalDB.getInstance();
        localDB.save(this, isNewMode, User.getCurrentUser());
    }
}
