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
    $("#loading").hide();
    $("#movie_list_table").show();
    // Find the empty table body by id "movie_list_table_body"
    let movieListTableBodyElement = jQuery("#movie_list_table_body");

    // Clear existing rows first
    movieListTableBodyElement.empty();

    let pageSize = parseInt(params.get("pageSize")) || 10;

    if(resultData.length > pageSize){
        $("#next_button").prop("disabled", false);
        console.log("next_button set to clickable");
    }
    else{
        $("#next_button").prop("disabled", true);
        console.log("next_button set to NOT clickable");
    }
    // Iterate through resultData, no more than 20 entries
    for (let i = 0; i < Math.min(pageSize, resultData.length); i++) {
        let movieId = resultData[i]["movie_id"];
        let rowHTML = "";
        rowHTML += "<tr>";

        // Movie title with hyperlink to single-movie.html
        rowHTML += "<td>";
        rowHTML += '<a href="single-movie.html?id=' + movieId + '">';
        rowHTML += resultData[i]["movie_title"];
        rowHTML += "</a>";
        rowHTML += "</td>";

        rowHTML += "<td>" + resultData[i]["movie_year"] + "</td>";
        rowHTML += "<td>" + resultData[i]["movie_director"] + "</td>";
        rowHTML += "<td>";
        // + resultData[i]["movie_genres"] + "</td>";
        let genreText = resultData[i]["movie_genres"];

        if (genreText == null || genreText.trim() === "") {
            rowHTML += "N/A";
        } else {
            let genres = genreText.split(',');

            for (let y = 0; y < genres.length; y++) {
                let trimmedGenre = genres[y].trim();
                if (trimmedGenre === "") continue;

                let genreUrl = "movieList.html?genre=" + encodeURIComponent(trimmedGenre)
                    + "&pageSize=" + pageSize
                    + "&pageNumber=0";

                rowHTML += '<a href="' + genreUrl + '">';

                if (y !== genres.length - 1) {
                    rowHTML += trimmedGenre + "</a>" + ", ";
                } else {
                    rowHTML += trimmedGenre + "</a>";
                }
            }
        }


        rowHTML += ""

        // Stars with hyperlinks to single-star.html
        rowHTML += "<td>";
        let starsArray = resultData[i]["movie_stars"];

        for (let j = 0; j < starsArray.length; j++) {
            let currentStar = starsArray[j];

            if (currentStar["star_name"] === "N/A" || currentStar["star_id"] === "") {
                rowHTML += "N/A";
            } else {
                rowHTML += '<a href="single-star.html?id=' + currentStar["star_id"] + '">';
                rowHTML += currentStar["star_name"];
                rowHTML += "</a>";
            }

            if (j < starsArray.length - 1) {
                rowHTML += ", ";
            }
        }
        rowHTML += "</td>";
        let rating = resultData[i]["movie_rating"];
        if(rating == null){
            rowHTML += "<td>N/A</td>";
        }
        else{
            rowHTML += "<td>" + resultData[i]["movie_rating"] + "</td>";
        }
        rowHTML += "<td>";
        rowHTML += "<button onclick=\"addToCart('" + movieId + "')\" class=\"btn btn-outline-primary\">Add to Cart</button>";
        rowHTML += "</td>";
        rowHTML += "</tr>";

        // Append the row to the table body
        movieListTableBodyElement.append(rowHTML);
    }
}

function addToCart(movieId) {
    jQuery.ajax({
        type: "POST",
        url: "api/cart",
        data: {
            movieId: movieId,
            action: "add"
        },
        success: function () {
            alert("Added to shopping cart!");
        },
        error: function () {
            alert("Failed to add movie to cart.");
        }
    });
}


/**
 * Once this .js is loaded, the following AJAX request is executed by the browser.
 */
let params = new URLSearchParams(window.location.search);
let mode = params.get("mode");
let apiUrl = "api/movie-list";
if (mode === "fulltext") {
    apiUrl = "api/full-text-search";
}
$("#loading").show();
$("#movie_list_table").hide();
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: apiUrl,
    data: {
        // New full-text search parameter
        query: params.get("query"),
        mode: params.get("mode"),
        // Old advanced search / browse parameters
        title: params.get("title"),
        year: params.get("year"),
        director: params.get("director"),
        star: params.get("star"),
        genre: params.get("genre"),
        startsWith: params.get("startsWith"),
        // Shared pagination / sorting parameters
        pageNumber: params.get("pageNumber"),
        pageSize: params.get("pageSize"),
        sort1: params.get("sort1"),
        dir1: params.get("dir1"),
        sort2: params.get("sort2"),
        dir2: params.get("dir2")
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
    const displayNumberDropdown = $("#display_number");
    let currentPageSize = params.get("pageSize") || "10";
    displayNumberDropdown.val(currentPageSize);

    const sort1 = $("#sort1");
    const sort2 = $("#sort2");
    sort1.val(params.get("sort1") || "title");
    sort2.val(params.get("sort2") || "rating");
    const dir1 = $("#dir1");
    const dir2 = $("#dir2");
    dir1.val(params.get("dir1") || "ASC");
    dir2.val(params.get("dir2") || "DESC");
    sort1.on("change", function() {
        if ($(this).val() === "title") {
            sort2.val("rating");
        } else {
            sort2.val("title");
        }
    });
    sort2.on("change", function() {
        if ($(this).val() === "title") {
            sort1.val("rating");
        } else {
            sort1.val("title");
        }
    });
    $("#apply_filter_btn").on("click", function() {
        let params = new URLSearchParams(window.location.search);
        params.set("sort1", sort1.val());
        params.set("dir1", $("#dir1").val());
        params.set("sort2", sort2.val());
        params.set("dir2", $("#dir2").val());

        window.location.search = params.toString();
    });
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
    displayNumberDropdown.val(currentPageSize);
    displayNumberDropdown.on("change", function(){
        let params = new URLSearchParams(window.location.search);

        let newLimit = $(this).val();

        params.set("pageSize", newLimit);
        // params.set("pageNumber", "0"); // Always reset to page 1 when changing result count

        window.location.search = params.toString();
    });
    // Disable Prev button if on page 1
    if (currentPage === 0) {
        $("#prev_button").prop("disabled", true);
    }


});