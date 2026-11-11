const loginButtons = document.querySelectorAll(".login-redirect");
const registerButtons = document.querySelectorAll(".register-redirect");

loginButtons.forEach(button => {
    button.addEventListener("click", () => {
        window.location.href = "/automation/login";
    });
});

registerButtons.forEach(button => {
    button.addEventListener("click", () => {
        window.location.href = "/automation/register";
    });
});