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
    params.append("pageSize", 10);
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
            `<a href="movieList.html?startsWith=${i}&pageNumber=0&pageSize=10">${i}</a> `
        );
    }

    for (let i = 65; i <= 90; i++) {
        let ch = String.fromCharCode(i);
        titleBrowseList.append(
            `<a href="movieList.html?startsWith=${ch}&pageNumber=0&pageSize=10">${ch}</a> `
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
            `<a href="movieList.html?genre=${encodeURIComponent(genreName)}&pageNumber=0&pageSize=10">${genreName}</a> `
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



let autocompleteCache = {};

/*
 * Called when autocomplete library needs suggestions.
 */
function handleLookup(query, doneCallback) {
    console.log("autocomplete initiated");
    console.log("query = " + query);

    query = query.trim();

    // Requirement: do not search when only 1 or 2 characters
    if (query.length < 3) {
        doneCallback({ suggestions: [] });
        return;
    }

    // Requirement: check frontend cache first
    if (autocompleteCache[query] !== undefined) {
        console.log("using cached autocomplete result");
        doneCallback({ suggestions: autocompleteCache[query] });
        return;
    }

    console.log("sending AJAX request to autocomplete servlet");

    jQuery.ajax({
        method: "GET",
        url: "api/autocomplete",
        data: {
            query: query
        },
        success: function(data) {
            handleLookupAjaxSuccess(data, query, doneCallback);
        },
        error: function(errorData) {
            console.log("lookup ajax error");
            console.log(errorData);
            doneCallback({ suggestions: [] });
        }
    });
}

/*
 * Handles autocomplete AJAX success.
 */
function handleLookupAjaxSuccess(data, query, doneCallback) {
    console.log("lookup ajax successful");
    console.log(data);

    // If servlet returns JSON with contentType application/json,
    // jQuery may already parse it into an object.
    let jsonData = data;

    if (typeof data === "string") {
        jsonData = JSON.parse(data);
    }

    // Cache the result
    autocompleteCache[query] = jsonData;

    // Save to sessionStorage too, so cache survives page refresh in same tab
    sessionStorage.setItem(
        "autocomplete_" + query,
        JSON.stringify(jsonData)
    );

    doneCallback({
        suggestions: jsonData
    });
}

/*
 * When user selects one suggestion.
 */
function handleSelectSuggestion(suggestion) {
    console.log(
        "selected movie: " +
        suggestion["value"] +
        " with ID " +
        suggestion["data"]["movie_id"]
    );

    window.location.href =
        "single-movie.html?id=" + suggestion["data"]["movie_id"];
}

/*
 * Normal full-text search if no suggestion is selected.
 */
function handleNormalSearch(query) {
    query = query.trim();

    if (query.length === 0) {
        return;
    }

    window.location.href =
        "movieList.html?query=" +
        encodeURIComponent(query) +
        "&mode=fulltext&pageNumber=0&pageSize=10";
}

$(document).ready(function() {
    $("#fulltext_query").autocomplete({
        lookup: function(query, doneCallback) {
            query = query.trim();

            // Also check sessionStorage cache here
            let cached = sessionStorage.getItem("autocomplete_" + query);

            if (cached !== null) {
                console.log("using sessionStorage cache");
                doneCallback({
                    suggestions: JSON.parse(cached)
                });
                return;
            }

            handleLookup(query, doneCallback);
        },

        onSelect: function(suggestion) {
            handleSelectSuggestion(suggestion);
        },

        deferRequestBy: 300,

        // Requirement: only autocomplete after 3 characters
        minChars: 3,

        // Requirement: suggestion list no more than 10
        lookupLimit: 10
    });

    $("#fulltext_search_form").submit(function(event) {
        event.preventDefault();
        handleNormalSearch($("#fulltext_query").val());
    });

    $("#fulltext_query").keypress(function(event) {
        if (event.keyCode === 13) {
            event.preventDefault();
            handleNormalSearch($("#fulltext_query").val());
        }
    });
});