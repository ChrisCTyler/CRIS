package solutions.cris.sync;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import solutions.cris.db.LocalDB;
import solutions.cris.exceptions.CRISException;

/**
 * Copyright CRIS Solutions 01/02/2017.
 */

public class WebConnection {


    public WebConnection(LocalDB localDB) {
        inputJSON = new JSONObject();
        JSONObject connection = new JSONObject();
        try {
            connection.put("user", "crissolu_623User");
            connection.put("password", "ukvKM7;WXDuU");
            connection.put("database", localDB.getDatabaseName());
            inputJSON.put("connection", connection);
        } catch (JSONException ex) {
            throw new CRISException("Error creating JSON object: " + ex.getMessage());
        }
    }

    private JSONObject inputJSON;

    public JSONObject getInputJSON() {
        return inputJSON;
    }

    // Post seems to be intermittent on ASUS tablets (not on Samsung) so make up to 5
    // attempts to post, returning the first one that succeeds
    public JSONObject post(String webFileName) {
        final int MAX_ATTEMPTS = 5;
        int attempt = 0;
        while (attempt <= MAX_ATTEMPTS) {
            try {
                attempt++;
                return attemptPost(webFileName);
            } catch (Exception ex) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw ex;
                }
            }
        }
        throw new CRISException("WebConnection.post reached unreachable code!");
    }

    private JSONObject attemptPost(String webFileName) {
        //System.setProperty("http.keepAlive", "false");
        String postOutput;
        String link = "https://cris.solutions/" + webFileName;
        HttpsURLConnection connection = null;
        DataOutputStream wr = null;
        String debug = "start";
        try {
            String data = URLEncoder.encode("JSONData", "UTF-8") + "=" + URLEncoder.encode(inputJSON.toString(), "UTF-8");
            debug += "-data encoded";
            URL url = new URL(link);
            debug += "-url instantiated";
            connection = (HttpsURLConnection) url.openConnection();
            debug += "-connection opened";
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", Integer.toString(data.length()));
            //connection.setUseCaches(false);
            //connection.setRequestProperty ( " Connection" , "close ");
            connection.setFixedLengthStreamingMode(data.length());
            connection.setDoOutput(true);
            debug += "-do output true";
            connection.setDoInput(true);
            debug += "-do input true";
            wr = new DataOutputStream(connection.getOutputStream());
            debug += "-established writer";
            wr.writeBytes(data);
            wr.flush();
            wr.close();
            debug += "-data flushed and closed";
            postOutput = readStream(connection.getInputStream());
            debug += "-inStream read";
        } catch (java.net.MalformedURLException ex) {
            throw new CRISException("Invalid Url: " + link);
        } catch (Exception ex) {
            String errorStream;
            try {
                if (connection == null) {
                    errorStream = "Connection was null";
                } else if (connection.getErrorStream() == null) {
                    errorStream = "ErrorStream was null";
                } else {
                    errorStream = readStream(connection.getErrorStream());
                }
            } catch (IOException iEx) {
                errorStream = iEx.getMessage();
            }
            String responseCode;
            try {
                if (connection == null) {
                    responseCode = "Connection was null";
                } else {
                    responseCode = String.format(Locale.UK, "%d", connection.getResponseCode());
                }
            } catch (IOException iEx) {
                responseCode = iEx.getMessage();
            }
            //throw new CRISException("Error in postJSON: " + ex.getMessage());
            throw new CRISException(String.format(
                    "Error in postJson: %s\n\nLink: %s\nResponseCode: %s, ErrorStream: %s\nDebug: %s",
                    ex.getMessage(), link, responseCode, errorStream, debug));
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
                if (wr != null) {
                    wr.close();
                }
            } catch (Exception ex) {
                // Close was not necessary
            }
        }
        // Partially process the result which will be JSON to check for global errors
        JSONObject jsonOutput;
        if (!postOutput.isEmpty()) {
            try {
                jsonOutput = new JSONObject(postOutput);
                String result = jsonOutput.getString("result");
                switch (result) {
                    case "SUCCESS":
                        // Nothing more to do
                        break;
                    case "FAILURE":
                        String errorMessage = jsonOutput.getString("error_message");
                        if (errorMessage.startsWith("Connection error")) {
                            throw new CRISException("No database found: " + errorMessage);
                        }
                        // Else let the calling method deal with the problem
                        break;
                    default:
                        throw new CRISException("Unexpected response from " + webFileName + ": " + result);
                }
            } catch (JSONException ex) {
                throw new CRISException("Error parsing JSON data: " + ex.getMessage());
            }
        } else {
            throw new CRISException("Call to " + webFileName + " returned null");
        }
        return jsonOutput;
    }

    private String readStream(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }
}
