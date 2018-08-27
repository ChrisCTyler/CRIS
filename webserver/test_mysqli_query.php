<?php


echo "Starting test...\n";

$con=mysqli_connect("localhost","crissolu_623User","ukvKM7;WXDuU","crissolu_gvwovhma");
if (mysqli_connect_errno($con)) {
    echo 'Failed to connect';
}
else {
    $result = mysqli_query($con,"SELECT * FROM Version ORDER BY VersionNumber DESC LIMIT 1");
    $row = mysqli_fetch_array($result);
    if($result == true) {
        echo $row[0] . ' - ' . $row[1];
    }
    else {
         echo 'Version table is empty';
    }
    mysql_close($con);
}

echo '...Test complete';
?>

