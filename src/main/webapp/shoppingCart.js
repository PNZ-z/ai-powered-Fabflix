function handleCartResult(resultData) {
    let cartTableBody = jQuery("#cart_table_body");
    cartTableBody.empty();

    let totalPrice = 0;

    for (let i = 0; i < resultData.length; i++) {
        let row = resultData[i];

        let movieId = row["movie_id"];
        let title = row["movie_title"];
        let quantity = row["quantity"];
        let price = row["price"];
        let itemTotal = quantity * price;

        totalPrice += itemTotal;

        let rowHTML = "";
        rowHTML += "<tr>";
        rowHTML += "<td>" + title + "</td>";
        rowHTML += "<td>" + quantity + "</td>";
        rowHTML += "<td>$" + price.toFixed(2) + "</td>";
        rowHTML += "<td>$" + itemTotal.toFixed(2) + "</td>";
        rowHTML += "<td>";
        rowHTML += "<button onclick=\"updateCart('" + movieId + "', 'increase')\">+</button>";
        rowHTML += "<button onclick=\"updateCart('" + movieId + "', 'decrease')\">-</button>";
        rowHTML += "<button onclick=\"updateCart('" + movieId + "', 'delete')\">Delete</button>";
        rowHTML += "</td>";
        rowHTML += "</tr>";

        cartTableBody.append(rowHTML);
    }

    jQuery("#cart_total_price").text("Total Price: $" + totalPrice.toFixed(2));
}

function updateCart(movieId, action) {
    jQuery.ajax({
        type: "POST",
        url: "api/boot/cart",
        data: {
            movieId: movieId,
            action: action
        },
        success: function () {
            loadCart();
        }
    });
}

function loadCart() {
    jQuery.ajax({
        type: "GET",
        url: "api/boot/cart",
        success: handleCartResult
    });
}

loadCart();
