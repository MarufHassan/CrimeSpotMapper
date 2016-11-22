<?php 

// array for JSON response
$response = array();

// include db connect class
require_once ('/home/a5345170/public_html/db_connect.php');
 
// connecting to db
$db = new DB_CONNECT();

if (isset($_GET["latitude"]) && isset($_GET["longitude"])) {
	
	$latitude = $_GET['latitude'];
	$longitude = $_GET['longitude'];

	$result = mysql_query("SELECT * FROM `crimeinfo` WHERE latitude = $latitude AND longitude = $longitude;");

	if (!empty($result)) {
		
		 if (mysql_num_rows($result) > 0) {
		 	$result = mysql_fetch_array($result);

            $spots = array();
            $spots["latitude"] = $result["latitude"];
            $spots["longitude"] = $result["longitude"];
            $spots["name"] = $result["name"];
            $spots["type"] = $result["type"];
            $spots["description"] = $result["description"];
            $spots["icon"] = $result["icon"];
            $spots["locality"] = $result["locality"];
            $spots["date"] = $result["date"];
            $spots["time"] = $result["time"];

            // success
            $response["success"] = 1;
            $response["spots"] = array();
            array_push($response["spots"], $spots);
            echo json_encode($response);
		 } else {
		    $response["success"] = 0;
            $response["message"] = "No spot found";
 
            // echo no users JSON
            echo json_encode($response);
		 }


	} else {
        // no spot found
        $response["success"] = 0;
        $response["message"] = "No spot found";
 
        // echo no users JSON
        echo json_encode($response);
    }

} else {
	// required field is missing
    $response["success"] = 0;
    $response["message"] = "Required field(s) is missing";
 
    // echoing JSON response
    echo json_encode($response);
}

?>