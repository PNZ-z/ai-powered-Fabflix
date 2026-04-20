/**
 * This file follows frontend and backend separation.
 *
 * Before this .js is loaded, the HTML skeleton is already created.
 *
 * This .js does two things:
 * 1. Sends a request to the backend API to get movie list JSON data.
 * 2. Populates the movie table in movieList.html.
 */


/**
 * Handles the data returned by the API and populates the movie list table.
 * @param resultData JSON array of movie objects
 */
function handleMovieListResult(resultData) {
    console.log("handleMovieListResult: populating movie list table from resultData");

    // Find the empty table body by id "movie_list_table_body"
    let movieListTableBodyElement = jQuery("#movie_list_table_body");

    // Clear existing rows first
    movieListTableBodyElement.empty();

    // Iterate through resultData, no more than 20 entries
    for (let i = 0; i < Math.min(20, resultData.length); i++) {
        let rowHTML = "";
        rowHTML += "<tr>";

        // Movie title with hyperlink to single-movie.html
        rowHTML += "<td>";
        rowHTML += '<a href="single-movie.html?id=' + resultData[i]["movie_id"] + '">';
        rowHTML += resultData[i]["movie_title"];
        rowHTML += "</a>";
        rowHTML += "</td>";

        rowHTML += "<td>" + resultData[i]["movie_year"] + "</td>";
        rowHTML += "<td>" + resultData[i]["movie_director"] + "</td>";
        rowHTML += "<td>" + resultData[i]["movie_genres"] + "</td>";

        // Stars with hyperlinks to single-star.html
        rowHTML += "<td>";
        let starsArray = resultData[i]["movie_stars"];

        for (let j = 0; j < starsArray.length; j++) {
            let currentStar = starsArray[j];

            rowHTML += '<a href="single-star.html?id=' + currentStar["star_id"] + '">';
            rowHTML += currentStar["star_name"];
            rowHTML += "</a>";

            if (j < starsArray.length - 1) {
                rowHTML += ", ";
            }
        }
        rowHTML += "</td>";

        rowHTML += "<td>" + resultData[i]["movie_rating"] + "</td>";
        rowHTML += "</tr>";

        // Append the row to the table body
        movieListTableBodyElement.append(rowHTML);
    }
}


/**
 * Once this .js is loaded, the following AJAX request is executed by the browser.
 */
let params = new URLSearchParams(window.location.search);
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/movie-list",
    data: {
        title: params.get("title"),
        year: params.get("year"),
        director: params.get("director"),
        star: params.get("star"),
        genre: params.get("genre"),
        startsWith: params.get("startsWith"),
        pageNumber: params.get("pageNumber")
    },
    success: (resultData) => handleMovieListResult(resultData),
    error: function(xhr, status, error) {
        console.error("Error fetching movie list:", error);
        console.error("Response text:", xhr.responseText);
    }
});
// $(document).ready(function() {
//     // 1. Get the parameters from the browser's address bar
//     let urlParams = new URLSearchParams(window.location.search);
//
//     // 2. Build the data object explicitly
//     let requestData = {};
//     if (urlParams.get("title")) requestData["title"] = urlParams.get("title");
//     if (urlParams.get("year")) requestData["year"] = urlParams.get("year");
//     if (urlParams.get("director")) requestData["director"] = urlParams.get("director");
//     if (urlParams.get("star")) requestData["star"] = urlParams.get("star");
//     if (urlParams.get("pageNumber")) requestData["pageNumber"] = urlParams.get("pageNumber");
//
//     console.log("Attempting to send data:", requestData);
//
//     // 3. The AJAX call
//     jQuery.ajax({
//         dataType: "json",
//         method: "GET",
//         url: "api/movie-list",
//         data: requestData, // This MUST be here for the Payload to appear
//         success: (resultData) => {
//             console.log("Success! Data received:", resultData);
//             handleMovieListResult(resultData);
//         },
//         error: (error) => {
//             console.log("AJAX Error:", error);
//         }
//     });
// });