let login_form = $("#login_form");

/**
 * Handle the data returned by LoginServlet
 * @param resultDataString jsonObject
 */
function handleLoginResult(resultDataJson) {


    console.log("handle login response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);

    // If login succeeds, it will redirect the user to movieList.html
    if (resultDataJson["status"] === "success") {
        window.location.replace("main.html");
    } else {
        // If login fails, the web page will display 
        // error messages on <div> with id "login_error_message"
        console.log("show error message");
        console.log(resultDataJson["message"]);
        $("#login_error_message").text(resultDataJson["message"]);
        if (typeof grecaptcha !== "undefined") {
            grecaptcha.reset();
        }
    }
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitLoginForm(formSubmitEvent) {
    console.log("submit login form");
    /**
     * When users click the submit button, the browser will not direct
     * users to the url defined in HTML form. Instead, it will call this
     * event handler when the event is triggered.
     */
    formSubmitEvent.preventDefault();
    let recaptchaResponse = grecaptcha.getResponse();
    if(recaptchaResponse.length === 0){
        let errInfoHtmlBody = $("#recaptchaErrInfo");
        $("#recaptchaWrapper").css("border", "1px solid red");
        $("#recaptchaErrorMessage").show();
        return;
    }
    $.ajax(
        "api/boot/login", {
            method: "POST",
            // Serialize the login form to the data sent by POST request
            data: login_form.serialize(),
            success: handleLoginResult,
            error: function (xhr) {
                let message = "Login failed";

                if (xhr.responseText) {
                    try {
                        let responseJson = JSON.parse(xhr.responseText);
                        message = responseJson["message"] || message;
                    } catch (e) {
                        message = xhr.responseText;
                    }
                }

                $("#login_error_message").text(message);

                if (typeof grecaptcha !== "undefined") {
                    grecaptcha.reset();
                }
            }
        }
    );
}

// Bind the submit action of the form to a handler function
login_form.submit(submitLoginForm);

