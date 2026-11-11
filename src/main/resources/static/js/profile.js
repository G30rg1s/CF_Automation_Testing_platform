document.addEventListener("DOMContentLoaded", () => {

    /* ========================================
       PROFILE ELEMENTS
       ======================================== */

    const profileFullName =
        document.getElementById("profileFullName");

    const profileEmailHeader =
        document.getElementById("profileEmailHeader");

    const profileFirstName =
        document.getElementById("profileFirstName");

    const profileLastName =
        document.getElementById("profileLastName");

    const profileEmail =
        document.getElementById("profileEmail");

    const profilePhone =
        document.getElementById("profilePhone");

    const profileFatherName =
        document.getElementById("profileFatherName");

    const profileMotherName =
        document.getElementById("profileMotherName");

    const profileAge =
        document.getElementById("profileAge");

    const profileMessage =
        document.getElementById("profileMessage");

    const updateProfileButton =
        document.getElementById("updateProfileButton");


    /* ========================================
       UPDATE PROFILE MODAL ELEMENTS
       ======================================== */

    const updateProfileModal =
        document.getElementById("updateProfileModal");

    const closeUpdateProfileModalButton =
        document.getElementById(
            "closeUpdateProfileModalButton"
        );

    const cancelUpdateProfileButton =
        document.getElementById(
            "cancelUpdateProfileButton"
        );

    const updateProfileForm =
        document.getElementById("updateProfileForm");

    const updateFirstName =
        document.getElementById("updateFirstName");

    const updateLastName =
        document.getElementById("updateLastName");

    const updateEmail =
        document.getElementById("updateEmail");

    const updatePhone =
        document.getElementById("updatePhone");

    const updateFatherName =
        document.getElementById("updateFatherName");

    const updateMotherName =
        document.getElementById("updateMotherName");

    const updateAge =
        document.getElementById("updateAge");

    const updateEmailError =
        document.getElementById("updateEmailError");

    const updatePhoneError =
        document.getElementById("updatePhoneError");

    const updateProfileMessage =
        document.getElementById("updateProfileMessage");

    const saveProfileButton =
        document.getElementById("saveProfileButton");


    /* ========================================
       CURRENT USER
       ======================================== */

    let currentUser = null;


    /* ========================================
       SIDEBAR ACTIVE ITEM
       ======================================== */

    const dashboardNav =
        document.getElementById("dashboardNavigation");

    const profileNav =
        document.getElementById("profileNavigation");


    if (dashboardNav) {

        dashboardNav.classList.remove(
            "active"
        );
    }


    if (profileNav) {

        profileNav.classList.add(
            "active"
        );
    }


    /* ========================================
       DISPLAY USER DATA
       ======================================== */

    function displayUser(user) {

        profileFullName.textContent =
            `${user.firstName ?? ""} ${user.lastName ?? ""}`
                .trim() || "-";


        profileEmailHeader.textContent =
            user.email ?? "-";


        profileFirstName.textContent =
            user.firstName ?? "-";

        profileLastName.textContent =
            user.lastName ?? "-";

        profileEmail.textContent =
            user.email ?? "-";

        profilePhone.textContent =
            user.phone ?? "-";

        profileFatherName.textContent =
            user.fatherName ?? "-";

        profileMotherName.textContent =
            user.motherName ?? "-";

        profileAge.textContent =
            user.age ?? "-";
    }


    /* ========================================
       CLEAR UPDATE VALIDATION ERRORS
       ======================================== */

    function clearUpdateValidationErrors() {

        updateEmailError.textContent =
            "";

        updatePhoneError.textContent =
            "";


        updateEmail.classList.remove(
            "input-error"
        );

        updatePhone.classList.remove(
            "input-error"
        );
    }


    /* ========================================
       EMAIL ERROR
       ======================================== */

    function showEmailError(message) {

        updateEmailError.textContent =
            message;

        updateEmail.classList.add(
            "input-error"
        );
    }


    /* ========================================
       PHONE ERROR
       ======================================== */

    function showPhoneError(message) {

        updatePhoneError.textContent =
            message;

        updatePhone.classList.add(
            "input-error"
        );
    }


    /* ========================================
       VALIDATE UPDATE PROFILE
       ======================================== */

    function validateUpdateProfile() {

        clearUpdateValidationErrors();


        let isValid =
            true;


        const email =
            updateEmail.value.trim();

        const phone =
            updatePhone.value.trim();


        /* ========================================
           EMAIL VALIDATION
           ======================================== */

        const emailRegex =
            /^[^\s@]+@[^\s@]+\.[^\s@]+$/;


        if (!email) {

            showEmailError(
                "Email is required."
            );

            isValid =
                false;

        } else if (email.length > 254) {

            showEmailError(
                "Email must not exceed 254 characters."
            );

            isValid =
                false;

        } else if (!emailRegex.test(email)) {

            showEmailError(
                "Email must be a valid email address."
            );

            isValid =
                false;
        }


        /* ========================================
           PHONE VALIDATION
           ======================================== */

        if (!phone) {

            showPhoneError(
                "Phone number is required."
            );

            isValid =
                false;

        } else if (!/^[0-9]{10}$/.test(phone)) {

            showPhoneError(
                "Phone number must contain exactly 10 digits."
            );

            isValid =
                false;

        } else if (!/^69/.test(phone)) {

            showPhoneError(
                "Phone number must start with 69."
            );

            isValid =
                false;
        }


        return isValid;
    }


    /* ========================================
       LOAD PROFILE
       ======================================== */

    async function loadProfile() {

        try {

            const response = await fetch(
                "/cf_automation/users/me",
                {
                    method: "GET",
                    credentials: "same-origin"
                }
            );


            if (
                response.status === 401 ||
                response.status === 403
            ) {

                window.location.href =
                    "/automation/unauthorized";

                return;
            }


            if (!response.ok) {

                profileMessage.textContent =
                    "Unable to load profile.";

                profileMessage.classList.add(
                    "error"
                );

                return;
            }


            const user =
                await response.json();


            currentUser =
                user;


            displayUser(user);


        } catch (error) {

            profileMessage.textContent =
                "Unable to connect to the server.";

            profileMessage.classList.add(
                "error"
            );


            console.error(
                "Unable to load profile.",
                error
            );
        }
    }


    /* ========================================
       OPEN UPDATE PROFILE MODAL
       ======================================== */

    function openUpdateProfileModal() {

        if (
            !currentUser ||
            !updateProfileModal
        ) {
            return;
        }


        updateFirstName.value =
            currentUser.firstName ?? "";

        updateLastName.value =
            currentUser.lastName ?? "";

        updateEmail.value =
            currentUser.email ?? "";

        updatePhone.value =
            currentUser.phone ?? "";

        updateFatherName.value =
            currentUser.fatherName ?? "";

        updateMotherName.value =
            currentUser.motherName ?? "";

        updateAge.value =
            currentUser.age ?? "";


        clearUpdateValidationErrors();


        updateProfileMessage.textContent =
            "";

        updateProfileMessage.classList.remove(
            "success",
            "error"
        );


        updateProfileModal.classList.add(
            "active"
        );


        updateEmail.focus();
    }


    /* ========================================
       CLOSE UPDATE PROFILE MODAL
       ======================================== */

    function closeUpdateProfileModal() {

        if (!updateProfileModal) {
            return;
        }


        updateProfileModal.classList.remove(
            "active"
        );


        clearUpdateValidationErrors();


        updateProfileMessage.textContent =
            "";

        updateProfileMessage.classList.remove(
            "success",
            "error"
        );
    }


    /* ========================================
       CLEAR EMAIL ERROR WHILE TYPING
       ======================================== */

    updateEmail.addEventListener(
        "input",
        () => {

            updateEmailError.textContent =
                "";

            updateEmail.classList.remove(
                "input-error"
            );
        }
    );


    /* ========================================
       CLEAR PHONE ERROR WHILE TYPING
       ======================================== */

    updatePhone.addEventListener(
        "input",
        () => {

            updatePhoneError.textContent =
                "";

            updatePhone.classList.remove(
                "input-error"
            );
        }
    );


    /* ========================================
       UPDATE PROFILE BUTTON
       ======================================== */

    if (updateProfileButton) {

        updateProfileButton.addEventListener(
            "click",
            openUpdateProfileModal
        );
    }


    /* ========================================
       CLOSE BUTTON
       ======================================== */

    if (closeUpdateProfileModalButton) {

        closeUpdateProfileModalButton.addEventListener(
            "click",
            closeUpdateProfileModal
        );
    }


    /* ========================================
       CANCEL BUTTON
       ======================================== */

    if (cancelUpdateProfileButton) {

        cancelUpdateProfileButton.addEventListener(
            "click",
            closeUpdateProfileModal
        );
    }


    /* ========================================
       CLICK OUTSIDE MODAL
       ======================================== */

    if (updateProfileModal) {

        updateProfileModal.addEventListener(
            "click",
            (event) => {

                if (
                    event.target ===
                    updateProfileModal
                ) {

                    closeUpdateProfileModal();
                }
            }
        );
    }


    /* ========================================
       ESCAPE KEY
       ======================================== */

    document.addEventListener(
        "keydown",
        (event) => {

            if (
                event.key === "Escape" &&
                updateProfileModal &&
                updateProfileModal.classList.contains(
                    "active"
                )
            ) {

                closeUpdateProfileModal();
            }
        }
    );


    /* ========================================
       SAVE PROFILE CHANGES
       ======================================== */

    if (updateProfileForm) {

        updateProfileForm.addEventListener(
            "submit",
            async (event) => {

                event.preventDefault();


                /* ========================================
                   CLEAR GENERAL MESSAGE
                   ======================================== */

                updateProfileMessage.textContent =
                    "";

                updateProfileMessage.classList.remove(
                    "success",
                    "error"
                );


                /* ========================================
                   FRONTEND VALIDATION

                   IMPORTANT:
                   If validation fails, execution stops
                   here BEFORE the fetch request.
                   ======================================== */

                const formIsValid =
                    validateUpdateProfile();


                if (!formIsValid) {

                    return;
                }


                /* ========================================
                   REQUEST BODY
                   ======================================== */

                const requestBody = {

                    firstName:
                        updateFirstName.value.trim(),

                    lastName:
                        updateLastName.value.trim(),

                    email:
                        updateEmail.value.trim(),

                    phone:
                        updatePhone.value.trim(),

                    fatherName:
                        updateFatherName.value.trim(),

                    motherName:
                        updateMotherName.value.trim(),

                    age:
                        Number(updateAge.value)
                };


                saveProfileButton.disabled =
                    true;


                try {

                    /* ========================================
                       BACKEND CALL

                       Reaches here ONLY if frontend
                       validation has passed.
                       ======================================== */

                    const response = await fetch(
                        "/cf_automation/users/update",
                        {
                            method: "PUT",

                            headers: {
                                "Content-Type":
                                    "application/json"
                            },

                            credentials:
                                "same-origin",

                            body:
                                JSON.stringify(
                                    requestBody
                                )
                        }
                    );


                    /* ========================================
                       UNAUTHORIZED
                       ======================================== */

                    if (
                        response.status === 401 ||
                        response.status === 403
                    ) {

                        window.location.href =
                            "/automation/unauthorized";

                        return;
                    }


                    /* ========================================
                       BACKEND ERROR
                       ======================================== */

                    if (!response.ok) {

                        try {

                            const errorData =
                                await response.json();


                            updateProfileMessage.textContent =
                                errorData.message ||
                                "Unable to update profile.";

                        } catch {

                            updateProfileMessage.textContent =
                                "Unable to update profile.";
                        }


                        updateProfileMessage.classList.add(
                            "error"
                        );

                        return;
                    }


                    /* ========================================
                       SUCCESS RESPONSE
                       ======================================== */

                    const updatedUser =
                        await response.json();


                    /* ========================================
                       UPDATE CURRENT USER
                       ======================================== */

                    currentUser =
                        updatedUser;


                    /* ========================================
                       UPDATE PROFILE PAGE
                       ======================================== */

                    displayUser(
                        updatedUser
                    );


                    /* ========================================
                       SUCCESS MESSAGE
                       ======================================== */

                    updateProfileMessage.textContent =
                        "Profile updated successfully.";

                    updateProfileMessage.classList.add(
                        "success"
                    );


                    setTimeout(
                        () => {

                            closeUpdateProfileModal();

                        },
                        700
                    );


                } catch (error) {

                    updateProfileMessage.textContent =
                        "Unable to connect to the server.";

                    updateProfileMessage.classList.add(
                        "error"
                    );


                    console.error(
                        "Unable to update profile.",
                        error
                    );

                } finally {

                    saveProfileButton.disabled =
                        false;
                }
            }
        );
    }


    /* ========================================
       INITIAL LOAD
       ======================================== */

    loadProfile();

});