/**
 * This file follows frontend and backend separation.
 *
 * It does two things:
 * 1. Reads the movie id from the page URL.
 * 2. Calls the backend API and fills the HTML page with movie data.
 */


/**
 * Handles the JSON data returned by the backend and populates the page.
 * @param resultData JSON object for one movie
 */
function handleSingleMovieResult(resultData) {
    console.log("handleSingleMovieResult: populating single movie page");

    if (resultData["errorMessage"]) {
        jQuery("#movie_title").text("Error");
        jQuery("#movie_year").text("");
        jQuery("#movie_director").text(resultData["errorMessage"]);
        jQuery("#movie_genres").text("");
        jQuery("#movie_stars").text("");
        jQuery("#movie_rating").text("");
        return;
    }

    jQuery("#movie_title").text(resultData["movie_title"]);
    jQuery("#movie_year").text(resultData["movie_year"]);
    jQuery("#movie_director").text(resultData["movie_director"]);

    let movieGenresElement = jQuery("#movie_genres");
    let genreText = resultData["movie_genres"];

    if (genreText == null || genreText.trim() === "") {
        movieGenresElement.append("N/A");
    }
    else{
        const genres = resultData["movie_genres"].split(',');
        for(let i=0; i< genres.length; i++){
            let trimmedGenre = genres[i].trim();
            movieGenresElement.append(
                '<a href="movieList.html?genre=' + encodeURIComponent(trimmedGenre)
                + "&pageSize=10" + "&pageNumber=0" + '">' + trimmedGenre
            )
            if(i !== genres.length-1){
                movieGenresElement.append('</a>, ');
            }
            else{
                movieGenresElement.append('</a>');
            }

        }
    }

    jQuery("#movie_rating").text(resultData["movie_rating"]);

    let movieStarsElement = jQuery("#movie_stars");
    movieStarsElement.empty();

    jQuery("#add_to_cart_button").click(function () {
        addToCart(movieId);
    });

    let starsArray = resultData["movie_stars"];
    for (let i = 0; i < starsArray.length; i++) {
        let currentStar = starsArray[i];
        let starName = currentStar["star_name"];
        let starId = currentStar["star_id"];
        if (!starId || starName.trim() === "N/A") {
            movieStarsElement.append("N/A");
        } else {
            movieStarsElement.append(
                '<a href="single-star.html?id=' + starId + '">' +
                starName +
                '</a>'
            );

        }

        if (i < starsArray.length - 1) {
            movieStarsElement.append(", ");
        }
    }
}

function addToCart(movieId) {
    jQuery.ajax({
        type: "POST",
        url: "api/boot/cart",
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
 * Read movie id from the URL.
 * Example: single-movie.html?id=tt1234567
 */
let urlParams = new URLSearchParams(window.location.search);
let movieId = urlParams.get("id");


/**
 * Call backend API.
 */
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/boot/single-movie?id=" + movieId,
    success: (resultData) => handleSingleMovieResult(resultData),
    error: function(xhr, status, error) {
        console.error("Error fetching single movie:", error);
        console.error("Response text:", xhr.responseText);
    }
});
