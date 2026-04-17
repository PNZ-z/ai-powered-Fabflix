let search_form = $("#search_form");

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
/**
 * Submit the form content and redirect to movieList.html
 */
function submitSearchForm(formSubmitEvent) {
    console.log("Submit Search form triggered!");
    formSubmitEvent.preventDefault();

    let title = $("#movieTitle").val();
    let year = $("#movieYear").val();
    let director = $("#movieDirector").val();
    let star = $("#movieStar").val();

    let url = "movieList.html?" +
        "title=" + encodeURIComponent(title) +
        "&year=" + encodeURIComponent(year) +
        "&director=" + encodeURIComponent(director) +
        "&star=" + encodeURIComponent(star);

    console.log("Redirecting to: " + url);
    window.location.replace(url);
}

// THIS PART IS CRITICAL:
$(document).ready(function() {
    console.log("DOM is ready, binding the form...");
    let search_form = $("#search_form");

    if (search_form.length === 0) {
        console.error("Bug: Could not find #search_form in the HTML!");
    }

    search_form.submit(submitSearchForm);
});


