<?php 

// array for JSON response
$response = array();


// include db connect class
require_once ('/home/a5345170/public_html/db_connect.php');
 
// connecting to db
$db = new DB_CONNECT();


$result = mysql_query("SELECT * FROM  `crimeinfo`;") or die(mysql_error());

if (mysql_num_rows($result) > 0) {
	$response["spots"] = array();

	while ($row = mysql_fetch_array($result)) {
		$spot = array();

		$spot["latitude"] = $row["latitude"];
		$spot["longitude"] = $row["longitude"];
		$spot["name"] = $row["name"];
		$spot["type"] = $row["type"];
		$spot["description"] = $row["description"];
		$spot["icon"] = $row["icon"];
		$spot["locality"] = $row["locality"];
		$spot["date"] = $row["date"];
		$spot["time"] = $row["time"];

        array_push($response["spots"], $spot);
	}
	// success
    $response["success"] = 1;
 
    // echoing JSON response
    echo json_encode($response);

} else {
    // no products found
    $response["success"] = 0;
    $response["message"] = "No spots found";
 
    // echo no users JSON
    echo json_encode($response);
}

?>
