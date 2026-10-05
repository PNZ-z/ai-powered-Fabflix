console.log("dashboard.js loaded");

/**
 * Handle Add Star response from servlet.
 */
function handleAddStarResult(resultData) {
    console.log("handle add star response");
    console.log(resultData);

    let messageDiv = $("#add_star_message");

    if (resultData["status"] === "success") {
        messageDiv
            .removeClass("text-danger")
            .addClass("text-success")
            .text(resultData["message"]);

        $("#add_star_form")[0].reset();
    } else {
        messageDiv
            .removeClass("text-success")
            .addClass("text-danger")
            .text(resultData["message"]);
    }
}

/**
 * Submit Add Star form.
 */
function submitAddStarForm(formSubmitEvent) {
    console.log("submit add star form");

    formSubmitEvent.preventDefault();

    $.ajax({
        dataType: "json",
        method: "POST",
        url: "api/boot/dashboard",
        data: $("#add_star_form").serialize() + "&action=add-star",
        success: handleAddStarResult,
        error: function (xhr, status, error) {
            console.log("add star ajax error");
            console.log("status code:", xhr.status);
            console.log("response text:", xhr.responseText);
            console.log("error:", error);

            $("#add_star_message")
                .removeClass("text-success")
                .addClass("text-danger")
                .text("Error: failed to send add star request.");
        }
    });
}

/**
 * Handle Metadata response from servlet.
 *
 * Expected JSON format:
 * {
 *   "status": "success",
 *   "tables": [
 *      {
 *          "table_name": "movies",
 *          "columns": [
 *              {"column_name": "id", "column_type": "varchar(10)"},
 *              {"column_name": "title", "column_type": "varchar(100)"}
 *          ]
 *      }
 *   ]
 * }
 */
function handleMetadataResult(resultData) {
    console.log("handle metadata response");
    console.log(resultData);

    let metadataDiv = $("#metadata_result");
    metadataDiv.empty();

    if (resultData["status"] !== "success") {
        metadataDiv
            .removeClass("text-success")
            .addClass("text-danger")
            .text(resultData["message"]);
        return;
    }

    let tables = resultData["tables"];

    if (tables.length === 0) {
        metadataDiv.text("No metadata found.");
        return;
    }

    for (let i = 0; i < tables.length; i++) {
        let table = tables[i];

        let tableCard = $("<div>").addClass("card mb-3");
        let tableHeader = $("<div>")
            .addClass("card-header font-weight-bold")
            .text(table["table_name"]);

        let tableBody = $("<div>").addClass("card-body");

        let columnTable = $("<table>").addClass("table table-sm table-bordered");
        let tableHead = $("<thead>").append(
            $("<tr>")
                .append($("<th>").text("Column Name"))
                .append($("<th>").text("Column Type"))
        );

        let tableBodyRows = $("<tbody>");

        let columns = table["columns"];
        for (let j = 0; j < columns.length; j++) {
            let column = columns[j];

            let row = $("<tr>")
                .append($("<td>").text(column["column_name"]))
                .append($("<td>").text(column["column_type"]));

            tableBodyRows.append(row);
        }

        columnTable.append(tableHead);
        columnTable.append(tableBodyRows);
        tableBody.append(columnTable);

        tableCard.append(tableHeader);
        tableCard.append(tableBody);

        metadataDiv.append(tableCard);
    }
}

/**
 * Request metadata from servlet.
 */
function requestMetadata() {
    console.log("request metadata");

    $.ajax({
        dataType: "json",
        method: "GET",
        url: "api/boot/dashboard?action=metadata",
        success: handleMetadataResult,
        error: function () {
            $("#metadata_result")
                .removeClass("text-success")
                .addClass("text-danger")
                .text("Error: failed to load database metadata.");
        }
    });
}

/**
 * Handle Add Movie response from servlet.
 */
function handleAddMovieResult(resultData) {
    console.log("handle add movie response");
    console.log(resultData);

    let messageDiv = $("#add_movie_message");

    if (resultData["status"] === "success") {
        messageDiv
            .removeClass("text-danger")
            .addClass("text-success")
            .text(resultData["message"]);

        $("#add_movie_form")[0].reset();
    } else {
        messageDiv
            .removeClass("text-success")
            .addClass("text-danger")
            .text(resultData["message"]);
    }
}

/**
 * Submit Add Movie form.
 */
function submitAddMovieForm(formSubmitEvent) {
    console.log("submit add movie form");

    formSubmitEvent.preventDefault();

    $.ajax({
        dataType: "json",
        method: "POST",
        url: "api/boot/dashboard",
        data: $("#add_movie_form").serialize() + "&action=add-movie",
        success: handleAddMovieResult,
        error: function (xhr, status, error) {
            console.log("add movie ajax error");
            console.log("status code:", xhr.status);
            console.log("response text:", xhr.responseText);
            console.log("error:", error);

            $("#add_movie_message")
                .removeClass("text-success")
                .addClass("text-danger")
                .text("Error: failed to send add movie request.");
        }
    });
}

// Register event handlers
$("#add_star_form").submit(submitAddStarForm);
$("#metadata_button").click(requestMetadata);
$("#add_movie_form").submit(submitAddMovieForm);
