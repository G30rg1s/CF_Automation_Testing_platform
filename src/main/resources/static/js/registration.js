const registerForm = document.getElementById("registerForm");
const registerMessage = document.getElementById("registerMessage");

const homeRedirect = document.getElementById("homeRedirect");
const loginRedirects = document.querySelectorAll(".login-redirect");

const successModal = document.getElementById("registrationSuccessModal");

const firstNameInput = document.getElementById("firstName");
const lastNameInput = document.getElementById("lastName");
const emailInput = document.getElementById("email");
const phoneInput = document.getElementById("phone");
const fatherNameInput = document.getElementById("fatherName");
const motherNameInput = document.getElementById("motherName");
const ageInput = document.getElementById("age");
const passwordInput = document.getElementById("password");

const firstNameError = document.getElementById("firstNameError");
const lastNameError = document.getElementById("lastNameError");
const emailError = document.getElementById("emailError");
const phoneError = document.getElementById("phoneError");
const fatherNameError = document.getElementById("fatherNameError");
const motherNameError = document.getElementById("motherNameError");
const ageError = document.getElementById("ageError");
const passwordError = document.getElementById("passwordError");


homeRedirect.addEventListener("click", () => {
    window.location.href = "/automation/homepage";
});


loginRedirects.forEach(button => {

    button.addEventListener("click", () => {
        window.location.href = "/automation/login";
    });

});

successModal.addEventListener("click", (event) => {

    if (event.target.id === "continueToLogin") {
        window.location.href = "/automation/login";
    }

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


function clearAllErrors() {

    clearFieldError(firstNameInput, firstNameError);
    clearFieldError(lastNameInput, lastNameError);
    clearFieldError(emailInput, emailError);
    clearFieldError(phoneInput, phoneError);
    clearFieldError(fatherNameInput, fatherNameError);
    clearFieldError(motherNameInput, motherNameError);
    clearFieldError(ageInput, ageError);
    clearFieldError(passwordInput, passwordError);

    registerMessage.textContent = "";
}


function validateRegistrationForm() {

    let isValid = true;

    clearAllErrors();

    const firstName = firstNameInput.value.trim();
    const lastName = lastNameInput.value.trim();
    const email = emailInput.value.trim();
    const phone = phoneInput.value.trim();
    const fatherName = fatherNameInput.value.trim();
    const motherName = motherNameInput.value.trim();
    const age = Number(ageInput.value);
    const password = passwordInput.value;


    if (firstName === "") {

        showFieldError(
            firstNameInput,
            firstNameError,
            "First name is required."
        );

        isValid = false;
    }


    if (lastName === "") {

        showFieldError(
            lastNameInput,
            lastNameError,
            "Last name is required."
        );

        isValid = false;
    }


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


    if (phone === "") {

        showFieldError(
            phoneInput,
            phoneError,
            "Phone number is required."
        );

        isValid = false;

    } else if (!/^69\d{8}$/.test(phone)) {

        showFieldError(
            phoneInput,
            phoneError,
            "Phone number must contain exactly 10 digits and start with 69."
        );

        isValid = false;
    }


    if (fatherName === "") {

        showFieldError(
            fatherNameInput,
            fatherNameError,
            "Father's name is required."
        );

        isValid = false;
    }


    if (motherName === "") {

        showFieldError(
            motherNameInput,
            motherNameError,
            "Mother's name is required."
        );

        isValid = false;
    }


    if (ageInput.value.trim() === "") {

        showFieldError(
            ageInput,
            ageError,
            "Age is required."
        );

        isValid = false;

    } else if (age < 1 || age > 120) {

        showFieldError(
            ageInput,
            ageError,
            "Age must be between 1 and 120."
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

    } else if (password.length < 8) {

        showFieldError(
            passwordInput,
            passwordError,
            "Password must be at least 8 characters long."
        );

        isValid = false;

    } else if (password.length > 72) {

        showFieldError(
            passwordInput,
            passwordError,
            "Password must not exceed 72 characters."
        );

        isValid = false;

    } else if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {

        showFieldError(
            passwordInput,
            passwordError,
            "Password must contain at least one letter and one number."
        );

        isValid = false;
    }


    return isValid;
}


firstNameInput.addEventListener("input", () => {
    clearFieldError(firstNameInput, firstNameError);
});

lastNameInput.addEventListener("input", () => {
    clearFieldError(lastNameInput, lastNameError);
});

emailInput.addEventListener("input", () => {
    clearFieldError(emailInput, emailError);
});

phoneInput.addEventListener("input", () => {
    clearFieldError(phoneInput, phoneError);
});

fatherNameInput.addEventListener("input", () => {
    clearFieldError(fatherNameInput, fatherNameError);
});

motherNameInput.addEventListener("input", () => {
    clearFieldError(motherNameInput, motherNameError);
});

ageInput.addEventListener("input", () => {
    clearFieldError(ageInput, ageError);
});

passwordInput.addEventListener("input", () => {
    clearFieldError(passwordInput, passwordError);
});


registerForm.addEventListener("submit", async (event) => {

    event.preventDefault();

    registerMessage.textContent = "";


    if (!validateRegistrationForm()) {
        return;
    }


    const requestBody = {
        firstName: firstNameInput.value.trim(),
        lastName: lastNameInput.value.trim(),
        email: emailInput.value.trim(),
        phone: phoneInput.value.trim(),
        fatherName: fatherNameInput.value.trim(),
        motherName: motherNameInput.value.trim(),
        age: Number(ageInput.value),
        password: passwordInput.value
    };


    try {

        const response = await fetch(
            "/cf_automation/users/register",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify(requestBody)
            }
        );


        if (!response.ok) {

            const errorData = await response.json();

            registerMessage.textContent =
                errorData.message || "Registration failed.";

            return;
        }


        registerMessage.textContent = "";

        registerForm.reset();

        successModal.classList.add("active");


    } catch (error) {

        registerMessage.textContent =
            "Unable to connect to the server.";
    }
});