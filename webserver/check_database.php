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
        $output['error_message'] = "PHP:Organisation not found.";
    }
    else {
        $result = mysqli_query($con,"SELECT * FROM Version ORDER BY VersionNumber DESC LIMIT 1");
        $row = mysqli_fetch_array($result);
        if($result == true) {
            $output['result'] = "SUCCESS";
            $output['organisation'] = $row[0];
            $output['version'] = $row[1];
        }
        else {
             $output['result'] = "FAILURE";
             $output['error_message'] = "PHP:Table Version is empty";
        }
        mysql_close($con);
    }
}
catch (Exception $e){
    $output['result'] = "FAILURE";
    $output['error_message'] = "EXCEPTION - PHP:" . $ex->getMessage();
}

echo json_encode($output,  JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_NUMERIC_CHECK);
?>

