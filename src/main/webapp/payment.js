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

// The checkout token doubles as the idempotency key. It is fetched once when
// the page loads and reused for every submission, so a repeated submission
// cannot place a second order.
let checkoutToken = null;

function loadCheckoutToken() {
    jQuery.ajax({
        type: "GET",
        url: "api/boot/checkout/token",
        success: function (tokenData) {
            checkoutToken = tokenData["token"];
        },
        error: function () {
            checkoutToken = null;
        }
    });
}

function submitPaymentForm(formSubmitEvent) {
    formSubmitEvent.preventDefault();

    if (!checkoutToken) {
        jQuery("#payment_error_message").text("Unable to start checkout. Please try again.");
        return;
    }

    let paymentData = jQuery("#payment_form").serialize()
        + "&checkoutToken=" + encodeURIComponent(checkoutToken);

    jQuery.ajax({
        type: "POST",
        url: "api/boot/payment",
        data: paymentData,
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
loadCheckoutToken();
