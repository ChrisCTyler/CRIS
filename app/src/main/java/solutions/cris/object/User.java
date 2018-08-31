package solutions.cris.object;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import solutions.cris.crypto.PasswordEncryptionService;
import solutions.cris.db.LocalDB;

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
public class User extends CrisObject implements Serializable {

    // Fixed UID for this class 12345 nnn vv (nnn = class code, vv = version)
    // Note: version should only be incremented if the class is changed in such
    // a way that older versions cannot be deserialised.
    private static final long serialVersionUID = CrisObject.SVUID_USER;
    public static final UUID unknownUser = UUID.fromString("86dab870-9449-11e6-ae22-56b6b6499611");
    public static final UUID firstTimeUser = UUID.fromString("b319fdf6-e779-4abb-b123-1ba5e681abd7");

    private static volatile User currentUser;

    // Constructor for new Users
    public User(User currentUser) {
        super(currentUser);
        userID = UUID.randomUUID();
        startDate = new Date(Long.MIN_VALUE);
        endDate = new Date(Long.MIN_VALUE);
        // New user so set password expired
        passwordExpiryDate = new Date(Long.MIN_VALUE);
    }

    // Constructor for the Unknown/FirstTime User
    public User(UUID userID) {
        super(userID);
        this.userID = userID;
        startDate = new Date(Long.MIN_VALUE);
        endDate = new Date(Long.MIN_VALUE);
        // First time user should not have to set a new password on first login
        passwordExpiryDate = User.addMonths(new Date(),4);
        if (userID == User.unknownUser) {
            this.firstName = "Unknown";
            this.lastName = "User";
            this.emailAddress = "Unknown";
            this.contactNumber = "Unknown";
        }
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized void setCurrentUser(User newCurrentUser) {
        currentUser = newCurrentUser;
    }

    // Useful function
    public static Date addMonths(Date date, int increment){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, increment);
        return cal.getTime();
    }

    //UserID
    private UUID userID;

    public UUID getUserID() {
        return userID;
    }

    //EmailAddress
    private String emailAddress;

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    //FirstName
    private String firstName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    //LastName
    private String lastName;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    //FullName
    public String getFullName() {
        // For partial User created for ListUsers only the FirstName is used
        String localFullName = firstName;
        if (lastName != null) {
            localFullName += " " + lastName;
        }
        return localFullName;
    }

    //StartDate
    private Date startDate;

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    //EndDate
    private Date endDate;

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    //ContactNumber
    private String contactNumber;

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    //PasswordSalt
    private byte[] passwordSalt;
    //PasswordHash
    private byte[] encryptedPassword;

    public void setNewPassword(String newPassword) {
        PasswordEncryptionService pes = new PasswordEncryptionService();
        try {
            if (passwordSalt == null) {
                passwordSalt = pes.generateSalt();
            }
            encryptedPassword = pes.getEncryptedPassword(newPassword, passwordSalt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            // Don't set the password, ignore since exceptions will not occur!
        }
    }

    public boolean authenticatePassword(String passwordAttempt) {
        boolean success = false;
        // User cannot login on or after their End date
        if (endDate.getTime() == Long.MIN_VALUE || endDate.after(new Date())) {
            PasswordEncryptionService pes = new PasswordEncryptionService();
            try {
                if (encryptedPassword != null) {
                    success = pes.authenticate(passwordAttempt, encryptedPassword, passwordSalt);
                }
            } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
                // validation fails, ignore since exceptions will not occur!
            }
        }
        return success;
    }

    // RoleID
    private UUID roleID;

    public UUID getRoleID() {
        return roleID;
    }

    public void setRoleID(UUID roleID) {
        this.roleID = roleID;
    }

    // Role
    private Role role;

    public Role getRole() {
        if (roleID != null && role == null) {
            LocalDB localDB = LocalDB.getInstance();
            role = (Role) localDB.getListItem(roleID);
            if (role == null) {
                // Only occurrence should be loading new tablet when this is the first sync and no roles exist
                role = new Role(new User(User.unknownUser), Role.noPrivilegeID);
            }
        }
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public static Comparator<User> comparator = new Comparator<User>() {
        @Override
        public int compare(User o1, User o2) {
            return o1.getFullName().compareTo(o2.getFullName());
        }
    };

    private Date passwordExpiryDate;

    public Date getPasswordExpiryDate() {
        return passwordExpiryDate;
    }

    public void setPasswordExpiryDate(Date passwordExpiryDate) {
        this.passwordExpiryDate = passwordExpiryDate;
    }

    public void clear(){
        setRole(null);
    }

    public void save(boolean isNewMode){
        LocalDB localDB = LocalDB.getInstance();
        Role role = getRole();
        clear();

        localDB.save(this, isNewMode, User.getCurrentUser());

        setRole(role);
    }

    public String textSummary() {
        SimpleDateFormat sDate = new SimpleDateFormat("dd MMM yyyy", Locale.UK);
        String summary = "Email Address:\n" + getEmailAddress() + "\n\n";
        summary += "Name:\n" + getFirstName() + " " + getLastName() + "\n\n";
        summary += "Start Date:\n";
        if (startDate.getTime() != Long.MIN_VALUE) {
            summary += sDate.format(getStartDate());
        }
        summary += "\n\n";
        summary += "End date:\n";
        if (endDate.getTime() != Long.MIN_VALUE) {
            summary += sDate.format(getEndDate());
        }
        summary += "\n\n";
        return summary;
    }
}
