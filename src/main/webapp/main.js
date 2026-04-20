let search_form = $("#search_form");

/**
 * Submit the search form content and redirect to movieList.html
 */
function submitSearchForm(formSubmitEvent) {
    console.log("Submit Search form triggered!");
    formSubmitEvent.preventDefault();

    let title = $("#movieTitle").val();
    let year = $("#movieYear").val();
    let director = $("#movieDirector").val();
    let star = $("#movieStar").val();

    let params = new URLSearchParams();
    if (title) params.append("title", title);
    if (year) params.append("year", year);
    if (director) params.append("director", director);
    if (star) params.append("star", star);
    params.append("pageNumber", 0);

    let url = "movieList.html" + (params.toString() ? "?" + params.toString() : "");

    console.log("Redirecting to: " + url);
    window.location.replace(url);
}

/**
 * Fill title browse links: 0-9 and A-Z
 */
function renderTitleBrowseLinks() {
    let titleBrowseList = $("#title_browse_list");
    titleBrowseList.empty();

    for (let i = 0; i <= 9; i++) {
        titleBrowseList.append(
            `<a href="movieList.html?startsWith=${i}&pageNumber=0">${i}</a> `
        );
    }

    for (let i = 65; i <= 90; i++) {
        let ch = String.fromCharCode(i);
        titleBrowseList.append(
            `<a href="movieList.html?startsWith=${ch}&pageNumber=0">${ch}</a> `
        );
    }
}

/**
 * Handle genre data returned by servlet
 */
function handleGenreResult(resultData) {
    console.log("Genre data received:");
    console.log(resultData);

    let genreBrowseList = $("#genre_browse_list");
    genreBrowseList.empty();

    for (let i = 0; i < resultData.length; i++) {
        let genreName = resultData[i]["name"];
        genreBrowseList.append(
            `<a href="movieList.html?genre=${encodeURIComponent(genreName)}&pageNumber=0">${genreName}</a> `
        );
    }
}

/**
 * Load genres from backend
 */
function loadGenres() {
    $.ajax("api/main", {
        method: "GET",
        dataType: "json",
        success: handleGenreResult,
        error: function(xhr, status, error) {
            console.log("Failed to load genres");
            console.log(status);
            console.log(error);
            console.log(xhr.responseText);
        }
    });
}

/**
 * Show the correct browse section depending on radio selection
 */
function updateBrowseSection() {
    let selectedType = $('input[name="browseType"]:checked').val();

    if (selectedType === "genre") {
        $("#genre_browse_section").show();
        $("#title_browse_section").hide();
    } else if (selectedType === "title") {
        $("#genre_browse_section").hide();
        $("#title_browse_section").show();
    }
}

$(document).ready(function() {
    console.log("DOM is ready, binding the form...");
    let search_form = $("#search_form");

    if (search_form.length === 0) {
        console.error("Bug: Could not find #search_form in the HTML!");
    }

    search_form.submit(submitSearchForm);

    $('input[name="browseType"]').on("change", function() {
        updateBrowseSection();
    });

    renderTitleBrowseLinks();
    loadGenres();
    updateBrowseSection();
});