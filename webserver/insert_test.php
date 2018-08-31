<?php
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
//
// 30th Aug 2018
// Build 107 - Rewritten to use PDO rather than mysqli due to website hosting problems

$output['result'] = "SUCCESS";

try {

    $JSONData = $_POST['JSONData'];

   //Decode JSON into an Array
    $data = json_decode($JSONData, true);

    $connection = $data["connection"];
    $user = $connection["user"];
    $password = $connection["password"];
    $database = $connection["database"];

    $mysqli = new mysqli("localhost","$user","$password","$database");
    if ($mysqli->connect_error) {
        $output['result'] = "FAILURE";
        $output['error_message'] = "PHP:Connection error (" . $database . "): " .  $mysqli->error;
    }
    else {
        if (!$mysqli->begin_transaction(MYSQLI_TRANS_START_READ_WRITE)) {
              $output['result'] = "FAILURE";
              $output['error_message'] = "PHP:Failed to start transaction - " .  $mysqli->error;
         }
         else {
              foreach($data as $label=>$value){
                switch ($label) {
                    case "connection":
                        break;
                    case "sync":
                        // Load the sync record
                        $stmt = $mysqli->prepare("INSERT Sync (SyncID, SyncDate, TableName) VALUES (?, ?, ?)");
                        $SyncID = "TestIDValue";
                        $CreationDate = 42;
                        $TableName = "SystemError";
                        if (!$stmt->bind_param("sis", $SyncID , $CreationDate , $TableName)){
                                $output['result'] = "FAILURE";
                                $output['error_message'] = "PHP:Bind Failure: - " . $mysqli->error;
                        }
                        else {
                            if (!$stmt->execute()) {
                                $output['result'] = "FAILURE";
                                $output['error_message'] = "PHP:UploadTest: - " . $stmt->error;
                            }
                        }
                        $stmt->close();
                        break;
                    default:

                            $output['result'] = "FAILURE";
                            $output['error_message'] = "PHP:Unexpected Label: (" . $label . ")";
                }
            }

            
            // Commit the transaction or rollback if failure
            if ($output['result'] == "SUCCESS") {
                $mysqli->commit();
            }
            else {
                $mysqli->rollback();
            }            
         }
         $mysqli->close();
    }
   
}
catch (Exception $e){
    $output['result'] = "FAILURE";
    $output['error_message'] = "EXCEPTION - PHP:" . $ex->getMessage();
    try {
        $mysqli->rollback();
    }
    catch (Exception $ex) {
    }

}

echo json_encode($output,  JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_NUMERIC_CHECK);
?> 