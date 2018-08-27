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
        foreach($data as $label=>$value){
            if ($label != "connection" ){
                $sqlCount++;
                // execute the sql
                if (!mysqli_query($con,$value)) {
                    $output['result'] = "FAILURE";
                    $output[$label] = "PHP:Failed to execute SQL(" . $value . "): " .  mysqli_error($con) . "\n";
                }
            } 
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

