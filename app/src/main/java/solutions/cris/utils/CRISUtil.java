package solutions.cris.utils;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import solutions.cris.crypto.AESEncryption;
import solutions.cris.exceptions.CRISException;
import solutions.cris.exceptions.CRISParseDateException;
import solutions.cris.exceptions.CRISParseTimeException;

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

public class
CRISUtil {

    public static Date parseDate(String sDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.UK);
        Date outputDate;
        try {
            outputDate = sdf.parse(sDate);
        } catch (ParseException ex) {
            outputDate = null;
        }
        if (outputDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(outputDate);
            int year = cal.get(Calendar.YEAR);
            if (year < 50) {
                cal.add(Calendar.YEAR, 2000);
                outputDate = cal.getTime();
            } else if (year < 100) {
                cal.add(Calendar.YEAR, 1900);
                outputDate = cal.getTime();
            } else if (year < 1900) {
                outputDate = null;
            }
        }
        return outputDate;
    }

    public static Date parseTime(String sTime){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.UK);
        Date outputDate;
        try {
            outputDate = sdf.parse(sTime);
        } catch (ParseException ex) {
            outputDate = null;
        }
        return outputDate;
    }

    public static Date parseDateTime(String sDate, String sTime) throws CRISParseDateException, CRISParseTimeException{
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.UK);
        sdf.setLenient(false);
        Date outputDate;
        try {
            outputDate = sdf.parse(String.format("%s %s", sDate, sTime));
        } catch (ParseException ex) {
            // Need to establish where error occurred
            ParseException pEx = (ParseException) ex;
            if (pEx.getErrorOffset() > sDate.length()){
                throw new CRISParseTimeException();
            } else {
                throw new CRISParseDateException();
            }
        }
        if (outputDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(outputDate);
            int year = cal.get(Calendar.YEAR);
            if (year < 50) {
                cal.add(Calendar.YEAR, 2000);
                outputDate = cal.getTime();
            } else if (year < 100) {
                cal.add(Calendar.YEAR, 1900);
                outputDate = cal.getTime();
            } else if (year < 1900) {
                outputDate = null;
            }
        }
        return outputDate;
    }

    public static Date midnightEarlier(Date date){
        Calendar later = Calendar.getInstance();
        later.setTime(date);
        // Revert to midnight
        later.set(Calendar.HOUR_OF_DAY, 0);
        later.set(Calendar.MINUTE, 0);
        later.set(Calendar.SECOND, 0);
        return later.getTime();
    }

    public static Date midnightLater(Date date){
        Calendar later = Calendar.getInstance();
        later.setTime(date);
        // Revert to midnight
        later.set(Calendar.HOUR_OF_DAY, 23);
        later.set(Calendar.MINUTE, 59);
        later.set(Calendar.SECOND, 59);
        return later.getTime();
    }

    public static void saveOrg(Context context, String organisation, String email){
        // Check all details are present
        if (organisation.isEmpty()){
            throw new CRISException("Attempt to save organisation details with no organisation.");
        }
        if (email.isEmpty()){
            throw new CRISException("Attempt to save organisation details with no email.");
        }
        // Use the dbname as the filename (properly formed)
        String fileName = String.format("org_%s", AESEncryption.getDatabaseName(organisation));
        // Create the content
        String content = String.format("%s\n%s",organisation,email);
        // Save the file
        FileOutputStream fos;
        try {
            fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();

        }
        catch (FileNotFoundException ex) {
            throw new CRISException("Unable to open org file: " + fileName);
        }
        catch (IOException ex){
            throw new CRISException("Unable to write to org file: " + fileName);
        }

    }

    public static void invalidateOrg(Context context, String dbName){
        // Use the dbname as the filename (properly formed)
        String fileName = String.format("org_%s", dbName);
        FileOutputStream fos;
        String content = "\nINVALID";
        try {
            fos = context.openFileOutput(fileName, Context.MODE_APPEND);
            fos.write(content.getBytes());
            fos.close();

        }
        catch (FileNotFoundException ex) {
            throw new CRISException("Unable to open org file: " + fileName);
        }
        catch (IOException ex){
            throw new CRISException("Unable to write to org file: " + fileName);
        }

    }

    public static String getOrgEmail(Context context, String organisation, boolean allowInvalid) {
        String email = "";
        if (organisation.isEmpty()){
            throw new CRISException("Attempt to get organisation details with no organisation.");
        }
        // Use the dbname as the filename (properly formed)
        String fileName = String.format("org_%s", AESEncryption.getDatabaseName(organisation));
        // Save the file
        FileInputStream fis;
        byte[] buf;
        try {
            fis = context.openFileInput(fileName);
            buf = new byte[fis.available()];
            int len = fis.read(buf);
            fis.close();
            if (len == 0){
                throw new CRISException("Empty org file found: " + organisation);
            }
            String sBuf = new String(buf);
            String[] lines = sBuf.split("\n");
            if (!lines[0].equals(organisation)){
                throw new CRISException("Invalid org file content:" + sBuf);
            }
            // If the file is invalidated (change user) there will be a 3rd line
            if (lines.length == 2 || allowInvalid) {
                email = lines[1];
            }
        }
        catch (FileNotFoundException ex) {
            // No successful login or user removed.
            return "";
        }
        catch (IOException ex){
            throw new CRISException("Unable to read org file: " + fileName);
        }
        return email;
    }
}
