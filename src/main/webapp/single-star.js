/**
 * This file follows frontend and backend separation.
 *
 * It does two things:
 * 1. Reads the star id from the page URL.
 * 2. Calls the backend API and fills the HTML page with star data.
 */


/**
 * Handles the JSON data returned by the backend and populates the page.
 * @param resultData JSON object for one star
 */
function handleSingleStarResult(resultData) {
    console.log("handleSingleStarResult: populating single star page");

    if (resultData["errorMessage"]) {
        jQuery("#star_name").text("Error");
        jQuery("#star_birth_year").text("");
        jQuery("#star_movies").text(resultData["errorMessage"]);
        return;
    }

    jQuery("#star_name").text(resultData["star_name"]);
    jQuery("#star_birth_year").text(resultData["star_birth_year"]);

    let starMoviesElement = jQuery("#star_movies");
    starMoviesElement.empty();

    let moviesArray = resultData["star_movies"];
    for (let i = 0; i < moviesArray.length; i++) {
        let currentMovie = moviesArray[i];

        starMoviesElement.append(
            '<a href="single-movie.html?id=' + currentMovie["movie_id"] + '">' +
            currentMovie["movie_title"] +
            '</a>'
        );

        if (i < moviesArray.length - 1) {
            starMoviesElement.append(", ");
        }
    }
}


/**
 * Read star id from the URL.
 * Example: single-star.html?id=nm1234567
 */
let urlParams = new URLSearchParams(window.location.search);
let starId = urlParams.get("id");


/**
 * Call backend API.
 */
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/single-star?id=" + starId,
    success: (resultData) => handleSingleStarResult(resultData),
    error: function(xhr, status, error) {
        console.error("Error fetching single star:", error);
        console.error("Response text:", xhr.responseText);
    }
});