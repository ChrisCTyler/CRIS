<?php

try {

    $JSONData = $_POST['JSONData'];

    //Decode JSON into an Array
    $data = json_decode($JSONData, true);

    $connection = $data["connection"];
    $user = $connection["user"];
    $password = $connection["password"];
    $database = $connection["database"];

    $con=mysqli_connect("localhost","$user","$password","$database");
    if (mysqli_connect_errno($con)) {
        $output['result'] = "FAILURE";
        $output['error_message'] = "PHP:Connection error (" . $database . "): " .  mysqli_error($con);
    }
    else {
        $result = mysqli_query($con,"SELECT SyncDate FROM Sync ORDER BY SyncDate DESC LIMIT 1");
        $row = mysqli_fetch_array($result);
        $dataOut = $row[0];
        if($result == true) {
            $output['result'] = "SUCCESS";
            $output['sync_date'] = $dataOut;
        }
        else {
             $output['result'] = "FAILURE";
             $output['error_message'] = "PHP:Table Sync is empty";
        }
        mysqli_close($con);
    }  
}
catch (Exception $e){
    $output['result'] = "FAILURE";
    $output['error_message'] = "EXCEPTION - PHP:" . $ex->getMessage();
}

echo json_encode($output,  JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_NUMERIC_CHECK);
?>

