function handleEmployeeLoginResult(resultData) {
    console.log("handle employee login response");
    console.log(resultData);

    if (resultData["status"] === "success") {
        window.location.replace("dashboard.html");
    } else {
        $("#employee_login_error_message").text(resultData["message"]);
    }
}

function submitEmployeeLoginForm(formSubmitEvent) {
    console.log("submit employee login form");

    formSubmitEvent.preventDefault();

    // Check reCAPTCHA before submitting
    let recaptchaResponse = grecaptcha.getResponse();

    if (recaptchaResponse.length === 0) {
        $("#recaptchaErrorMessage").show();
        return;
    } else {
        $("#recaptchaErrorMessage").hide();
    }

    $.ajax({
        dataType: "json",
        method: "POST",
        url: "api/employee-login",
        data: $("#employee_login_form").serialize(),
        success: handleEmployeeLoginResult
    });
}

$("#employee_login_form").submit(submitEmployeeLoginForm);