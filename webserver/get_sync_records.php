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
        $query = "SELECT * FROM Sync WHERE SyncDate > " . $sync["SyncDate"] . " ORDER BY SyncDate";
        //$query = "SELECT * FROM Sync";
        //$stmt = mysqli_stmt_prepare($con, $query);
        //mysqli_stmt_bind_param($stmt, "i", $sync["SyncDate"]);
        //mysqli_stmt_execute($stmt);
        //$result = mysqli_stmt_get_result($stmt);
        $result = mysqli_query($con, $query);

           while ($row = mysqli_fetch_assoc($result)){
                $count++;
                $output['row_' . $count] = $row;
            }

        mysqli_stmt_close($stmt);
        mysqli_close($con);
    }
}
catch (Exception $e){
    $output['result'] = "FAILURE";
    $output['error_message'] = "EXCEPTION - PHP:" . $ex->getMessage();
}

echo json_encode($output,  JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_NUMERIC_CHECK);
?>

