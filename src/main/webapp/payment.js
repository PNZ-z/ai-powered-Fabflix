function handleCartResult(resultData) {
    let totalPrice = 0;

    for (let i = 0; i < resultData.length; i++) {
        let row = resultData[i];

        let quantity = row["quantity"];
        let price = row["price"];

        totalPrice += quantity * price;
    }

    jQuery("#payment_total_price").text("Total Price: $" + totalPrice.toFixed(2));
}

function loadCartTotal() {
    jQuery.ajax({
        type: "GET",
        url: "api/boot/cart",
        success: handleCartResult,
        error: function () {
            jQuery("#payment_total_price").text("Failed to load total price.");
        }
    });
}

function submitPaymentForm(formSubmitEvent) {
    formSubmitEvent.preventDefault();

    jQuery.ajax({
        type: "POST",
        url: "api/boot/payment",
        data: jQuery("#payment_form").serialize(),
        success: function (resultData) {
            if (resultData["status"] === "success") {
                window.location.href = "confirmation.html";
            } else {
                jQuery("#payment_error_message").text(resultData["message"]);
            }
        },
        error: function () {
            jQuery("#payment_error_message").text("Payment failed. Please try again.");
        }
    });
}

jQuery("#payment_form").submit(submitPaymentForm);

loadCartTotal();
