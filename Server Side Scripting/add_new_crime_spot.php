<?php 
	
// array for JSON response
$response = array();

// check for required fields
if (isset($_POST['latitude']) && isset($_POST['longitude']) 
	&& isset($_POST['name']) && isset($_POST['type']) 
	&& isset($_POST['description']) && isset($_POST['icon']) 
	&& isset($_POST['date']) && isset($_POST['time']) && isset($_POST['locality'])) {
	
	$latitude = $_POST['latitude'];
	$longitude = $_POST['longitude'];
	$name = $_POST['name'];
	$type = $_POST['type'];
	$description = $_POST['description'];
	$icon = $_POST['icon'];
	$locality = $_POST['locality'];
	$date = $_POST['date'];
	$time = $_POST['time'];

	// include db connect class
	require_once ('/home/a5345170/public_html/db_connect.php');

	// connecting to db
	$db = new DB_CONNECT();

	// mysql inserting a new row
	$result = mysql_query("INSERT INTO `crimeinfo` (`latitude`, `longitude`, `name`, `type`, `description`, `locality`, `icon`, `date`, `time`) VALUES ($latitude, $longitude, '$name', '$type', '$description', '$locality', '$icon', '$date', '$time');");

	if ($result) {
        // successfully inserted into database
        $response["success"] = 1;
        $response["message"] = "Information successfully added";
 
        // echoing JSON response
        echo json_encode($response);
    } else {
        // failed to insert row
        $response["success"] = 0;
        $response["message"] = "Oops! An error occurred.";
 
        // echoing JSON response
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