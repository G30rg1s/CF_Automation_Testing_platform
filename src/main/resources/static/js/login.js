const loginForm = document.getElementById("loginForm");
const loginMessage = document.getElementById("loginMessage");

const homeRedirects = document.querySelectorAll(".home-redirect");
const registerRedirects = document.querySelectorAll(".register-redirect");

const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");

const emailError = document.getElementById("emailError");
const passwordError = document.getElementById("passwordError");


homeRedirects.forEach(button => {

    button.addEventListener("click", () => {
        window.location.href = "/automation/homepage";
    });

});


registerRedirects.forEach(button => {

    button.addEventListener("click", () => {
        window.location.href = "/automation/register";
    });

});


function showFieldError(input, errorElement, message) {

    errorElement.textContent = message;
    errorElement.classList.add("active");

    input.classList.add("input-error");
}


function clearFieldError(input, errorElement) {

    errorElement.textContent = "";
    errorElement.classList.remove("active");

    input.classList.remove("input-error");
}


function validateLoginForm() {

    let isValid = true;

    clearFieldError(emailInput, emailError);
    clearFieldError(passwordInput, passwordError);


    const email = emailInput.value.trim();
    const password = passwordInput.value;


    if (email === "") {

        showFieldError(
            emailInput,
            emailError,
            "Email is required."
        );

        isValid = false;

    } else if (emailInput.validity.typeMismatch) {

        showFieldError(
            emailInput,
            emailError,
            "Please enter a valid email address, for example name@example.com."
        );

        isValid = false;
    }


    if (password === "") {

        showFieldError(
            passwordInput,
            passwordError,
            "Password is required."
        );

        isValid = false;
    }


    return isValid;
}


emailInput.addEventListener("input", () => {

    clearFieldError(
        emailInput,
        emailError
    );

    loginMessage.textContent = "";
});


passwordInput.addEventListener("input", () => {

    clearFieldError(
        passwordInput,
        passwordError
    );

    loginMessage.textContent = "";
});


loginForm.addEventListener("submit", async (event) => {

    event.preventDefault();

    loginMessage.textContent = "";


    if (!validateLoginForm()) {
        return;
    }


    const requestBody = {
        email: emailInput.value.trim(),
        password: passwordInput.value
    };


    try {

        const response = await fetch(
            "/cf_automation/users/login",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                credentials: "same-origin",

                body: JSON.stringify(requestBody)
            }
        );


        if (!response.ok) {

            if (response.status === 401) {

                loginMessage.textContent =
                    "Invalid email or password.";

                return;
            }


            try {

                const errorData = await response.json();

                loginMessage.textContent =
                    errorData.message || "Login failed.";

            } catch {

                loginMessage.textContent =
                    "Login failed.";
            }

            return;
        }


        loginMessage.textContent =
            "Login successful.";


        setTimeout(() => {

            window.location.href =
                "/automation/dashboard";

        }, 700);


    } catch (error) {

        loginMessage.textContent =
            "Unable to connect to the server.";
    }
});