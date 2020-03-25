package solutions.cris.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import solutions.cris.object.Client;
import solutions.cris.object.CrisObject;

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

public class CRISKPIItem implements Serializable {

    private static final long serialVersionUID = CrisObject.SVUID_CRIS_KPI_ITEM ;

    public enum kpiItemType  {MONTH, YEAR};

    public CRISKPIItem(String title, Date slotStart, Date slotEnd, kpiItemType type){
        this.title = title;
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
        this.type = type;
        this.kpiMap = new HashMap<>();
        this.clientArea = new HashMap<>();
    }

    //Title
    private String title;
    public String getTitle() {return title;}
    public void setTitle(String title){this.title = title;}

    // Type
    private kpiItemType type;

    public kpiItemType getType() {
        return type;
    }

    // slotStart
    private Date slotStart;

    public Date getSlotStart() {
        return slotStart;
    }

    private Date slotEnd;

    public Date getSlotEnd() {
        return slotEnd;
    }

    //KPIMap - Set of integer arrays mapped to Areas (plus a total)
    private HashMap<UUID, int[]> kpiMap;

    public HashMap<UUID, int[]> getKpiMap() {
        return kpiMap;
    }

    public void setKpiMap(HashMap<UUID, int[]> kpiMap) {
        this.kpiMap = kpiMap;
    }

    private HashMap<UUID, UUID> clientArea;

    public HashMap<UUID, UUID> getClientArea() {
        return clientArea;
    }

    public void setClientArea(HashMap<UUID, UUID> clientArea) {
        this.clientArea = clientArea;
    }
}
