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
        rowHTML += "<td>";
        // + resultData[i]["movie_genres"] + "</td>";
        let genres = resultData[i]["movie_genres"].split(',');
        for (let y=0; y<genres.length; y++){
            let genre = genres[y];
            let trimmedGenre = genre.trim();
            rowHTML += '<a href="movieList.html?genre=' + trimmedGenre + '">';
            if(y !== genres.length-1){
                rowHTML += trimmedGenre + "</a>" + ',';
            }
            else{
                rowHTML += trimmedGenre + "</a>";
            }
        }

        rowHTML += ""

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

$(document).ready(function() {
    // 1. Get current parameters
    let params = new URLSearchParams(window.location.search);

    // 2. Determine current page (default to 0 if not present)
    let currentPage = parseInt(params.get("pageNumber")) || 0;

    // 3. Update the Page Info span
    // $("#page_info").text("Page " + (currentPage + 1));

    // 4. Next Button Click
    $("#next_button").click(function() {
        params.set("pageNumber", currentPage + 1);
        window.location.search = params.toString(); // This reloads the page with new params
    });

    // 5. Previous Button Click
    $("#prev_button").click(function() {

        if (currentPage > 0) {
            params.set("pageNumber", currentPage - 1);
            window.location.search = params.toString();
        }
    });

    // Disable Prev button if on page 1
    if (currentPage === 0) {
        $("#prev_button").prop("disabled", true);
    }
});