package solutions.cris.object;

import android.os.Build;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Locale;

import solutions.cris.BuildConfig;

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

public class SystemError extends CrisObject implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_SYSTEM_ERROR;

    public SystemError(User user, Exception exception) {
        super(user);

        versionCode = BuildConfig.VERSION_CODE;
        versionName = BuildConfig.VERSION_NAME;

        buildBrand = Build.BRAND;
        buildDevice = Build.DEVICE;
        buildModel = Build.MODEL;
        buildProduct = Build.PRODUCT;
        buildSerial = Build.SERIAL;

        userName = user.getFirstName() + " " + user.getLastName();
        userEmailAddress = user.getEmailAddress();
        userContactNumber = user.getContactNumber();

        exceptionName = exception.toString();
        exceptionMessage = exception.getMessage();
        StringWriter sbStackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(sbStackTrace));
        stackTrace = ("************ STACK TRACE ************\n\n" + sbStackTrace.toString());
    }

    public  SystemError(User user, String exceptionMessage){
        super(user);

        versionCode = BuildConfig.VERSION_CODE;
        versionName = BuildConfig.VERSION_NAME;

        buildBrand = Build.BRAND;
        buildDevice = Build.DEVICE;
        buildModel = Build.MODEL;
        buildProduct = Build.PRODUCT;
        buildSerial = Build.SERIAL;

        userName = user.getFirstName() + " " + user.getLastName();
        userEmailAddress = user.getEmailAddress();
        userContactNumber = user.getContactNumber();

        exceptionName = exceptionMessage;
        this.exceptionMessage = exceptionMessage;
    }

    //UserName
    private String userName;
    public String getUserName()  {return userName;}

    //UserEmailAddress
    private String userEmailAddress;
    //public String getUserEmailAddress()  {return userEmailAddress;}

    //UserContactNumber
    private String userContactNumber;
    //public String getUserContactNumber()  {return userContactNumber;}

    //VersionCode
    private int versionCode;
    //public int getVersionCode()  {return versionCode;}

    //VersionName
    private String versionName;
    //public String getVersionName()  {return versionName;}

    // BuildBrand
    private String buildBrand;
    //public String getBuildBrand()  {return buildBrand;}
    // BuildDevice
    private String buildDevice;
    //public String getBuildDevice()  {return buildDevice;}
    // BuildModel
    private String buildModel;
    //public String getBuildModel()  {return buildModel;}
    // BuildProduct
    private String buildProduct;
    //public String getBuildProduct()  {return buildProduct;}
    //BuildSerial
    private String buildSerial;
    //public String getBuildSerial()  {return buildSerial;}

    // ExceptionName
    private String exceptionName;
    //public String getExceptionName()  {return exceptionName;}
    // ExceptionMessage
    private String exceptionMessage;
    public String getExceptionMessage()  {
        if (exceptionMessage == null){
            return "Exception message was null";
        } else {
            return exceptionMessage;
        }
    }
    // StackTrace
    private String stackTrace;
    //public String getStackTrace()  {return stackTrace;}

    public String getTextSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.UK);
        String text = "System Error: " + exceptionName + "\n\n";
        text += getExceptionMessage() + "\n\n";
        text += "Date: " + sDate.format(getCreationDate()) + "\n";
        text += "User: " + userName + "\n";
        text += "Email Address: " + userEmailAddress + "\n";
        text += "Contact Number: " + userContactNumber + "\n";
        text += String.format("CRIS Version: %s (Build %s)\n",versionName, versionCode);
        text += "CRIS Version: " + versionName + "\n";
        text += "Brand: " + buildBrand + "\n";
        text += "Device: " + buildDevice + "\n";
        text += "Model: " + buildModel + "\n";
        text += "Product: " + buildProduct + "\n";
        text += "Serial: " + buildSerial + "\n\n";
        if (stackTrace != null){
            text += stackTrace + "\n";
        }

        return text;
    }

    public static Comparator<SystemError> comparatorAZ = new Comparator<SystemError>() {
        @Override
        public int compare(SystemError o1, SystemError o2) {
            int compare = o1.exceptionName.compareTo(o2.exceptionName);
            if (compare == 0){
                if (o1.getCreationDate().after(o2.getCreationDate())) {compare = 0;}
                else {compare = 1;}
            }
            return compare;
        }
    };

}
