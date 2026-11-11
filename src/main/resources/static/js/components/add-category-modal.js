document.addEventListener("DOMContentLoaded", () => {

    /* ========================================
       ADD CATEGORY MODAL
       ======================================== */

    const addCategoryModal =
        document.getElementById("addCategoryModal");

    const openCategoryModalButton =
        document.getElementById("openCategoryModalButton");

    const closeCategoryModalButton =
        document.getElementById("closeCategoryModalButton");

    const cancelCategoryButton =
        document.getElementById("cancelCategoryButton");

    const addCategoryForm =
        document.getElementById("addCategoryForm");

    const categoryNameInput =
        document.getElementById("categoryName");

    const categoryNameError =
        document.getElementById("categoryNameError");

    const categoryModalMessage =
        document.getElementById("categoryModalMessage");

    const createCategoryButton =
        document.getElementById("createCategoryButton");


    /* ========================================
       CATEGORY ALREADY EXISTS MODAL
       ======================================== */

    const categoryAlreadyExistsModal =
        document.getElementById("categoryAlreadyExistsModal");

    const existingCategoryName =
        document.getElementById("existingCategoryName");

    const closeCategoryAlreadyExistsButton =
        document.getElementById("closeCategoryAlreadyExistsButton");


    if (!addCategoryModal || !addCategoryForm) {
        return;
    }


    /* ========================================
       CLEAR FORM MESSAGES
       ======================================== */

    function clearMessages() {

        categoryNameError.textContent = "";

        categoryNameInput.classList.remove(
            "input-error"
        );

        categoryModalMessage.textContent = "";

        categoryModalMessage.classList.remove(
            "success",
            "error"
        );
    }


    /* ========================================
       OPEN ADD CATEGORY MODAL
       ======================================== */

    function openAddCategoryModal() {

        clearMessages();

        addCategoryForm.reset();

        addCategoryModal.classList.add(
            "active"
        );

        categoryNameInput.focus();
    }


    /* ========================================
       CLOSE ADD CATEGORY MODAL
       ======================================== */

    function closeAddCategoryModal() {

        addCategoryModal.classList.remove(
            "active"
        );

        clearMessages();

        addCategoryForm.reset();
    }


    /* ========================================
       OPEN ALREADY EXISTS MODAL
       ======================================== */

    function openAlreadyExistsModal(categoryName) {

        existingCategoryName.textContent =
            categoryName;

        categoryAlreadyExistsModal.classList.add(
            "active"
        );
    }


    /* ========================================
       CLOSE ALREADY EXISTS MODAL
       ======================================== */

    function closeAlreadyExistsModal() {

        categoryAlreadyExistsModal.classList.remove(
            "active"
        );

        existingCategoryName.textContent = "";
    }


    /* ========================================
       VALIDATION
       ======================================== */

    function showCategoryNameError(message) {

        categoryNameError.textContent =
            message;

        categoryNameInput.classList.add(
            "input-error"
        );
    }


    function validateCategoryName() {

        const categoryName =
            categoryNameInput.value.trim();


        if (categoryName === "") {

            showCategoryNameError(
                "Category name is required."
            );

            return false;
        }


        if (categoryName.length > 100) {

            showCategoryNameError(
                "Category name must not exceed 100 characters."
            );

            return false;
        }


        return true;
    }


    /* ========================================
       OPEN ADD MODAL
       ======================================== */

    if (openCategoryModalButton) {

        openCategoryModalButton.addEventListener(
            "click",
            openAddCategoryModal
        );
    }


    /* ========================================
       CLOSE ADD MODAL
       ======================================== */

    closeCategoryModalButton.addEventListener(
        "click",
        closeAddCategoryModal
    );


    cancelCategoryButton.addEventListener(
        "click",
        closeAddCategoryModal
    );


    /* ========================================
       INPUT CHANGE
       ======================================== */

    categoryNameInput.addEventListener(
        "input",
        () => {

            categoryNameError.textContent = "";

            categoryNameInput.classList.remove(
                "input-error"
            );

            categoryModalMessage.textContent = "";

            categoryModalMessage.classList.remove(
                "success",
                "error"
            );
        }
    );


    /* ========================================
       CLICK OUTSIDE ADD MODAL
       ======================================== */

    addCategoryModal.addEventListener(
        "click",
        (event) => {

            if (event.target === addCategoryModal) {

                closeAddCategoryModal();
            }
        }
    );


    /* ========================================
       ALREADY EXISTS MODAL EVENTS
       ======================================== */

    if (
        categoryAlreadyExistsModal &&
        closeCategoryAlreadyExistsButton
    ) {

        closeCategoryAlreadyExistsButton.addEventListener(
            "click",
            closeAlreadyExistsModal
        );


        categoryAlreadyExistsModal.addEventListener(
            "click",
            (event) => {

                if (
                    event.target ===
                    categoryAlreadyExistsModal
                ) {

                    closeAlreadyExistsModal();
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

            if (event.key !== "Escape") {
                return;
            }


            if (
                categoryAlreadyExistsModal &&
                categoryAlreadyExistsModal.classList.contains(
                    "active"
                )
            ) {

                closeAlreadyExistsModal();

                return;
            }


            if (
                addCategoryModal.classList.contains(
                    "active"
                )
            ) {

                closeAddCategoryModal();
            }
        }
    );


    /* ========================================
       CREATE CATEGORY
       ======================================== */

    addCategoryForm.addEventListener(
        "submit",
        async (event) => {

            event.preventDefault();

            clearMessages();


            if (!validateCategoryName()) {
                return;
            }


            const requestBody = {

                name:
                    categoryNameInput.value.trim()
            };


            createCategoryButton.disabled =
                true;


            try {

                const response = await fetch(
                    "/cf_automation/categories",
                    {
                        method: "POST",

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


                if (
                    response.status === 401 ||
                    response.status === 403
                ) {

                    window.location.href =
                        "/automation/unauthorized";

                    return;
                }


                if (!response.ok) {

                    try {

                        const errorData =
                            await response.json();


                        categoryModalMessage.textContent =
                            errorData.message ||
                            "Unable to create category.";

                    } catch {

                        categoryModalMessage.textContent =
                            "Unable to create category.";
                    }


                    categoryModalMessage.classList.add(
                        "error"
                    );

                    return;
                }


                const data =
                    await response.json();


                /* ========================================
                   CATEGORY ALREADY EXISTS
                   ======================================== */

                if (
                    data.result ===
                    "CATEGORY_ALREADY_EXISTED"
                ) {

                    closeAddCategoryModal();

                    openAlreadyExistsModal(
                        data.categoryName
                    );

                    return;
                }


                /* ========================================
                   CATEGORY CREATED
                   ======================================== */

                if (
                    data.result ===
                    "CATEGORY_CREATED"
                ) {

                    categoryModalMessage.textContent =
                        `Category "${data.categoryName}" created successfully.`;

                    categoryModalMessage.classList.add(
                        "success"
                    );


                    if (
                        typeof loadCategories ===
                        "function"
                    ) {

                        await loadCategories();
                    }


                    setTimeout(
                        () => {

                            closeAddCategoryModal();

                        },
                        700
                    );
                }


            } catch (error) {

                categoryModalMessage.textContent =
                    "Unable to connect to the server.";

                categoryModalMessage.classList.add(
                    "error"
                );

            } finally {

                createCategoryButton.disabled =
                    false;
            }
        }
    );

});