package solutions.cris.crypto;


import android.util.Base64;

import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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

public class AESEncryption {

    private static volatile AESEncryption instance;

    private Key localKey;
    private Key webKey;
    private Key oldLocalKey;

    public static final String LOCAL_CIPHER = "solutions.cris.LocalCipher";
    public static final String WEB_CIPHER = "solutions.cris.WebCipher";
    public static final String NO_ENCRYPTION = "solutoins.cris.NoEncryption";
    public static final String OLD_CIPHER = "solutions.cris.OldCipher";

    private AESEncryption(String organisation, String email) {
        // Save the old local key, if there is one (Change User)
        if (instance != null) {
            this.oldLocalKey = instance.localKey;
        }
        // Build the keys
        setLocalKey(organisation, email);
        setWebKey(organisation);
    }

    public static synchronized AESEncryption setInstance(String organisation, String email) {
        instance = new AESEncryption(organisation, email);
        return instance;
    }

    public static synchronized AESEncryption getInstance() {
        if (instance == null) {
            throw new CRISException("Encryption not initialised");
        }
        return instance;
    }

    public static String getDatabaseName(String organisation) {
        // Hash the result
        byte[] hash;
        try {
            String algorithm = "PBKDF2WithHmacSHA1";
            String saltString = "wkdmbpul";
            byte[] salt = saltString.getBytes();
            KeySpec spec = new PBEKeySpec(organisation.toCharArray(), salt, 1000, 160);
            SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);
            hash = f.generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new CRISException("Encryption error: " + ex.getMessage());
        }
        String longName = Base64.encodeToString(hash, Base64.DEFAULT);
        StringBuilder nameBuf = new StringBuilder();
        for (int i = 0; i < longName.length(); i++) {
            if (Character.isLetter(longName.charAt(i))) {
                nameBuf.append(longName.charAt(i));
            }
        }
        String name = nameBuf.toString().toLowerCase();
        if (name.length() < 8) {
            throw new CRISException("Organisation name generates dbname of less than 8 characters");
        }
        name = name.substring(0, 8);
        return "crissolu_" + name;
    }

    public byte[] encrypt(String mode, byte[] plainText) {
        byte[] output;
        /*
        // Build 120 4 May 2019 - Removed local encryption
        // 1. Modify Encrypt and Decrypt to simply return the sent parameter for mode=LOCAL_CIPHER
        // 2. The mechanism for re-encrypting will then automatically handle existing databases
        // It was designed for change of user but the check in Login.attemptLogin() will work
        // because the attempt to get the user record will throw an exception bexause of the new
        // decrypt functionality
        */
        if (mode.equals(LOCAL_CIPHER)){
            output = plainText;
        } else {

            try {
                //Generate random IV of 128-bit (AES block size)
                SecureRandom rnd = new SecureRandom();
                byte[] IV = new byte[128 / 8];
                rnd.nextBytes(IV);
                IvParameterSpec IVSpec = new IvParameterSpec(IV);
                //Create the cipher object to perform AES operations.
                Cipher AESCipher = Cipher.getInstance("AES/CFB/NoPadding");
                //Initialize the Cipher with the key and initialization vector.
                // Build 189 - Remove warnings
                //if (mode.equals(LOCAL_CIPHER)) {
                //    AESCipher.init(Cipher.ENCRYPT_MODE, localKey, IVSpec);
                //    //throw new CRISException("Code should not get here");
                //} else {
                //    AESCipher.init(Cipher.ENCRYPT_MODE, webKey, IVSpec);
                //}

                AESCipher.init(Cipher.ENCRYPT_MODE, webKey, IVSpec);
                //Encrypt the plaintext data
                byte[] ciphertext = AESCipher.doFinal(plainText);
                // Prepend the IV to the ciphertext message so that it's available for decryption
                output = new byte[ciphertext.length + (128 / 8)];
                //Copy the respective parts into the array.
                System.arraycopy(IV, 0, output, 0, IV.length);
                System.arraycopy(ciphertext, 0, output, IV.length, ciphertext.length);
            } catch (Exception ex) {
                throw new CRISException("Encryption error: " + ex.getMessage());
            }
        }
        return output;
    }

    public byte[] decrypt(String mode, byte[] encryptedText) {
        byte[] plaintext;

        /*
        // Build 120 4 May 2019 - Removed local encryption
        // 1. Modify Encrypt and Decrypt to simply return the sent parameter for mode=LOCAL_CIPHER
        // 2. The mechanism for re-encrypting will then automatically handle existing databases
        // It was designed for change of user but the check in Login.attemptLogin() will work
        // because the attempt to get the user record will throw an exception bexause of the new
        // decrypt functionality
        */
        if (mode.equals(LOCAL_CIPHER)){
            plaintext = encryptedText;
        } else {

            //Extract the IV from the encryption output
            byte[] IV = new byte[128 / 8];
            byte[] ciphertext = new byte[encryptedText.length - (128 / 8)];
            try {
                System.arraycopy(encryptedText, 0, IV, 0, IV.length);
                System.arraycopy(encryptedText, IV.length, ciphertext, 0, ciphertext.length);
                //Create the cipher object to perform AES operations.
                //Specify Advanced Encryption Standard - Cipher Feedback Mode - No Padding
                Cipher AESCipher = Cipher.getInstance("AES/CFB/NoPadding");
                //Create the IvParameterSpec object from the raw IV
                IvParameterSpec IVSpec = new IvParameterSpec(IV);
                //Initialize the Cipher with the key and initialization vector.
                // Build 189 - Remove warnings
                //switch (mode) {

                    //case LOCAL_CIPHER:
                    //   // AESCipher.init(Cipher.DECRYPT_MODE, localKey, IVSpec);
                    //   //break;
                    //   throw new CRISException("Code should not get here");
                //    case OLD_CIPHER:
                //        AESCipher.init(Cipher.DECRYPT_MODE, oldLocalKey, IVSpec);
                //        break;
                //    default:
                //        AESCipher.init(Cipher.DECRYPT_MODE, webKey, IVSpec);
                //}
                if (mode.equals(OLD_CIPHER)){
                    AESCipher.init(Cipher.DECRYPT_MODE, oldLocalKey, IVSpec);
                } else {
                    AESCipher.init(Cipher.DECRYPT_MODE, webKey, IVSpec);
                }
                //Decrypts the ciphertext data
                plaintext = AESCipher.doFinal(ciphertext);
            } catch (Exception ex) {
                throw new CRISException("Encryption error: " + ex.getMessage());
            }
        }
        return plaintext;
    }

    private void setLocalKey(String organisation, String emailAddress) {
        // Interleave email address and organisation (repeat smaller string until larger, interleave to 2x larger string)
        int keyLength;
        if (emailAddress.length() > organisation.length()) {
            keyLength = emailAddress.length();
            while (emailAddress.length() > organisation.length()) {
                organisation += organisation;
            }
        } else {
            keyLength = organisation.length();
            while (organisation.length() > emailAddress.length()) {
                emailAddress += emailAddress;
            }
        }
        StringBuilder keyString = new StringBuilder();
        for (int i = 0; i < keyLength; i++) {
            keyString.append(organisation.charAt(i));
            keyString.append(emailAddress.charAt(i));
        }
        // Hash the result
        byte[] hash;
        try {
            String algorithm = "PBKDF2WithHmacSHA1";
            String saltString = "airlvwls";
            byte[] salt = saltString.getBytes();
            KeySpec spec = new PBEKeySpec(keyString.toString().toCharArray(), salt, 1000, 160);
            SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);
            hash = f.generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new CRISException("Encryption error: " + ex.getMessage());
        }
        String key = Base64.encodeToString(hash, Base64.DEFAULT);
        key = key.substring(0, 16);
        localKey = new SecretKeySpec(key.getBytes(), "AES");
    }

    private void setWebKey(String organisation) {
        // Interleave cris.solutions and organisation (repeat smaller string until larger, interleave to 2x larger string)
        String crisSolutions = "CRIS.solutions";
        int keyLength;
        if (crisSolutions.length() > organisation.length()) {
            keyLength = crisSolutions.length();
            while (crisSolutions.length() > organisation.length()) {
                organisation += organisation;
            }
        } else {
            keyLength = organisation.length();
            while (organisation.length() > crisSolutions.length()) {
                crisSolutions += crisSolutions;
            }
        }
        StringBuilder keyString = new StringBuilder();
        for (int i = keyLength - 1; i >= 0; i--) {
            keyString.append(organisation.charAt(i));
            keyString.append(crisSolutions.charAt(i));
        }
        // Hash the result
        byte[] hash;
        try {
            String algorithm = "PBKDF2WithHmacSHA1";
            String saltString = "airlvwls";
            byte[] salt = saltString.getBytes();
            KeySpec spec = new PBEKeySpec(keyString.toString().toCharArray(), salt, 1000, 160);
            SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);
            hash = f.generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new CRISException("Encryption error: " + ex.getMessage());
        }
        String key = Base64.encodeToString(hash, Base64.DEFAULT);
        key = key.substring(0, 16);
        webKey = new SecretKeySpec(key.getBytes(), "AES");
    }
}

