<?php

$output['result'] = "SUCCESS";

try {

    $JSONData = $_POST['JSONData'];

   //Decode JSON into an Array
    $data = json_decode($JSONData, true);

    $connection = $data["connection"];
    $user = $connection["user"];
    $password = $connection["password"];
    $database = $connection["database"];

    $con=mysqli_connect("localhost","$user","$password","$database");
    if (mysqli_connect_errno($con))
    {
        $output['result'] = "FAILURE";
        $output['error_message'] = "PHP:Connection error (" . $database . "): " .  mysqli_error($con);
    }
    else {
        if (! mysqli_begin_transaction($con, MYSQLI_TRANS_START_READ_WRITE)) {
              $output['result'] = "FAILURE";
              $output['error_message'] = "PHP:Failed to start transaction";
        } else {
            $stmt = mysqli_prepare($con, "REPLACE User (RecordID, UserID, HistoryDate, SyncID, CreationDate, CreatedByID, EmailAddress, Name, SerialisedObject) VALUES (?, ?, ?, ?, ?, ?, ?, ? ,?)");
            foreach($data as $label=>$value){
                switch ($label) {
                    case "connection":
                        break;
                    case "sync":
                        // Load the sync record
                        mysqli_stmt_close($stmt);
                        $stmt = mysqli_prepare($con, "INSERT Sync (SyncID, SyncDate, TableName) VALUES (?, ?, ?)");
                        mysqli_stmt_bind_param($stmt, "sis", $value["SyncID"] , $value["SyncDate"] , $value["TableName"]);
                        //if (!mysqli_query($con,'INSERT Sync (SyncID, SyncDate, TableName) VALUES ("' . $value["SyncID"] . '", "' . $value["SyncDate"] . '", "' .$value["TableName"] . '")')) {
                        if (!mysqli_stmt_execute($stmt)) {
                            $output['result'] = "FAILURE";
                            $output['error_message'] = "PHP:Sync: - " . mysqli_stmt_error($stmt);
                        }
                        break;
                    default:
                        // Load the ListItem
                        //Store record into MySQL DB
                        mysqli_stmt_bind_param($stmt, "ssisissss", $value["RecordID"],$value["UserID"],$value["HistoryDate"],$value["SyncID"],$value["CreationDate"],$value["CreatedByID"],$value["EmailAddress"],$value["Name"],base64_decode($value["SerialisedObject"]));
                        if (!mysqli_stmt_execute($stmt)) {
                            $output['result'] = "FAILURE";
                            $output['error_message'] = "PHP:User: (" . $label . ") - " . mysqli_stmt_error($stmt);
                        }
                }
            }

            mysqli_stmt_close($stmt);
            // Commit the transaction or rollback if failure
            if ($output['result'] == "SUCCESS") {
                mysqli_commit($con);
            }
            else {
                mysqli_rollback($con);
            }
            mysqli_close($con);
        }
    } 
}
catch (Exception $e){
    $output['result'] = "FAILURE";
    $output['error_message'] = "EXCEPTION - PHP:" . $ex->getMessage();
    try {
        mysqli_rollback($con);
    }
    catch (Exception $ex) {
    }
}

echo json_encode($output,  JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_NUMERIC_CHECK);
?> 