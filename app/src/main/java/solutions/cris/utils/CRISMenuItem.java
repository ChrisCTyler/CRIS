package solutions.cris.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import solutions.cris.object.CrisObject;
import solutions.cris.object.Document;

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

public class CRISMenuItem implements Serializable {

    private static final long serialVersionUID = CrisObject.SVUID_CRIS_MENU_ITEM;

    public CRISMenuItem(String title, String summary, int icon, Date displayDate) {
        newCRISMenuItem(title, summary, icon, displayDate, title);
    }

    public CRISMenuItem(String title, String summary, int icon, Date displayDate, String sortValue) {
        newCRISMenuItem(title, summary, icon, displayDate, sortValue);
    }

    private void newCRISMenuItem(String title, String summary, int icon, Date displayDate, String sortValue){
        this.title = title;
        this.summary = summary;
        this.icon = icon;
        this.displayDate = displayDate;
        switch (title){
            case "Sync":
                sortString = "01Sync";
                break;
            case "My Clients":
                sortString = "04 My Clients";
                break;
            case "My Sessions":
                sortString = "05My Sessions";
                break;
            case "All Clients":
                sortString = "06All Clients";
                break;
            case "Users":
                sortString = "07Users";
                break;
            case "Library":
                sortString = "08Library";
                break;
            case "System Administration":
                sortString = "09System Administration";
                break;
            default:
                if (title.startsWith("CRIS")){
                    sortString = "99CRIS";
                } else if (title.startsWith("Library")){
                    sortString = "02" + sortValue;
                } else {
                    sortString = "033" + sortValue;
                }
        }
    }

    //Title
    private String title;
    public String getTitle() {return title;}
    public void setTitle(String title){this.title = title;}

    // Summary
    private String summary;
    public String getSummary() {return summary;}
    public void setSummary(String summary){this.summary = summary;}

    // Icon
    private int icon;
    public int getIcon() {return icon;}
    public void setIcon(int icon){this.icon = icon;}

    // DisplayDate
    private Date displayDate;
    public Date getDisplayDate() {return displayDate;}
    public void setDisplayDate(Date displayDate){this.displayDate = displayDate;}

    //DocumentList
    private ArrayList<Document> documentList;
    public ArrayList<Document> getDocumentList() {return documentList;}
    public void setDocumentList(ArrayList<Document> documentList) {this.documentList = documentList;}

    // SortString
    private String sortString;
    public String getSortString() {return sortString;}

    public void setSortString(String sortString) {
        this.sortString = sortString;
    }

    public static Comparator<CRISMenuItem> comparatorAZ = new Comparator<CRISMenuItem>() {
        @Override
        public int compare(CRISMenuItem o1, CRISMenuItem o2) {
            return (o1.getSortString().compareTo(o2.getSortString()));
        }
    };
}
