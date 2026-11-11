document.addEventListener("DOMContentLoaded", () => {

    const logoutButton =
        document.getElementById("logoutButton");


    if (!logoutButton) {
        return;
    }


    logoutButton.addEventListener(
        "click",
        async () => {

            logoutButton.disabled = true;


            try {

                const response =
                    await fetch(
                        "/cf_automation/users/logout",
                        {
                            method: "POST",
                            credentials: "same-origin"
                        }
                    );


                if (
                    !response.ok &&
                    response.status !== 204
                ) {

                    logoutButton.disabled = false;

                    return;
                }


                window.location.href =
                    "/automation/login";


            } catch (error) {

                logoutButton.disabled = false;

                console.error(
                    "Logout failed.",
                    error
                );
            }
        }
    );

});