<?php

echo "Starting test...\n";

$con=mysqli_connect("localhost","crissolu_623User","ukvKM7;WXDuU","crissolu_gvwovhma");
if (mysqli_connect_errno($con)) {
    echo 'Failed to connect';
}
else {
   if (! mysqli_begin_transaction($con, MYSQLI_TRANS_START_READ_WRITE)) {
          echo 'mysqli_begin_transaction (read write) failed';
    } else {
        mysqli_rollback($con);
        echo 'mysqli_begin_transaction Succeeded';
    }
}

echo '...Test complete';
?>

