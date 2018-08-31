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

$file = 'pdo_get_records.php';
$output['result'] = "SUCCESS";

try {

    $JSONData = $_POST['JSONData'];

    //Decode JSON into an Array
    $data = json_decode($JSONData, true);

    $connection = $data["connection"];
    $database = $connection["database"];
    $dsn = "mysql:dbname=$database;host=localhost";
    $user = $connection['user'];
    $password = $connection['password'];
    $dbh = new PDO($dsn, $user, $password);

    $sync = $data["sync"];
    $count = 0;

    $sql = "SELECT * FROM " . $sync["TableName"] . " WHERE SyncID = '" . $sync["SyncID"] . "'";
    $sth = $dbh->prepare($sql);
    if (!$sth->execute()){
        throw new Exception("Failed to execute SQL(" . $sql . "): " . $sth->errorInfo()[2]);
    }
    // Fetch rows
    while ($row = $sth->fetch(PDO::FETCH_ASSOC)){
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
catch (Exception $e){
    $output['result'] = "FAILURE";
    $output['error_message'] = "PHP $file failed: " . $e->getMessage();
    if ($dbh){
        $dbhError = $dbh->errorInfo()[2];
        if (strlen($dbhError)>0) {
            $output['error_message'] .= " {$dbhError}";
        }
    }
}

echo json_encode($output,  JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_NUMERIC_CHECK);
?>
