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

    $sync = $data["sync"];
    $count = 0;

    $con=mysqli_connect("localhost","$user","$password","$database");
    if (mysqli_connect_errno($con)) {
        $output['result'] = "FAILURE";
        $output['error_message'] = "PHP:Connection error (" . $database . "): " .  mysqli_error($con);
    }
    else {
        $query = "SELECT * FROM " . $sync["TableName"] . " WHERE SyncID = '" . $sync["SyncID"] . "'";
        //$query = 'SELECT * FROM ' . $sync["TableName"] ;
        $result = mysqli_query($con, $query);
        if ($result == false) {
             $output['result'] = "FAILURE";
             $output['error_message'] = "PHP:" . mysqli_error($con) . "(" . $query . ")";
        } 
        else {
             while ($row = mysqli_fetch_assoc($result)){
               foreach($row as $label=>$value){
                   if ($label == 'SerialisedObject'){
                       $row["SerialisedObject"] = base64_encode($value);
                   } else if ($label == 'Content'){
                       $row["Content"] = base64_encode($value);
                   }
               }
               $count++;
               $output['row_' . $count] = $row;
            }
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

