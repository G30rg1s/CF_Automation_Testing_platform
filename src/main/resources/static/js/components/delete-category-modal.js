document.addEventListener("DOMContentLoaded", () => {

    const deleteCategoryModal =
        document.getElementById("deleteCategoryModal");

    const deleteCategoryName =
        document.getElementById("deleteCategoryName");

    const deleteCategoryMessage =
        document.getElementById("deleteCategoryMessage");

    const cancelDeleteCategoryButton =
        document.getElementById("cancelDeleteCategoryButton");

    const confirmDeleteCategoryButton =
        document.getElementById("confirmDeleteCategoryButton");


    if (!deleteCategoryModal) {
        return;
    }


    let selectedCategoryId = null;
    let selectedCategoryName = null;


    /* ========================================
       OPEN MODAL
       ======================================== */

    function openDeleteCategoryModal(
        categoryId,
        categoryName
    ) {

        selectedCategoryId =
            categoryId;

        selectedCategoryName =
            categoryName;


        deleteCategoryName.textContent =
            categoryName;


        deleteCategoryMessage.textContent =
            "";

        deleteCategoryMessage.classList.remove(
            "success",
            "error"
        );


        deleteCategoryModal.classList.add(
            "active"
        );
    }


    /* ========================================
       CLOSE MODAL
       ======================================== */

    function closeDeleteCategoryModal() {

        deleteCategoryModal.classList.remove(
            "active"
        );


        deleteCategoryName.textContent =
            "";

        deleteCategoryMessage.textContent =
            "";

        deleteCategoryMessage.classList.remove(
            "success",
            "error"
        );


        selectedCategoryId = null;
        selectedCategoryName = null;
    }


    /* ========================================
       LISTEN FOR DYNAMIC DELETE BUTTONS
       ======================================== */

    document.addEventListener(
        "click",
        (event) => {

            const deleteButton =
                event.target.closest(
                    ".delete-category-button"
                );


            if (!deleteButton) {
                return;
            }


            event.stopPropagation();


            const categoryId =
                deleteButton.dataset.categoryId;

            const categoryName =
                deleteButton.dataset.categoryName;


            openDeleteCategoryModal(
                categoryId,
                categoryName
            );
        }
    );


    /* ========================================
       CANCEL
       ======================================== */

    cancelDeleteCategoryButton.addEventListener(
        "click",
        closeDeleteCategoryModal
    );


    /* ========================================
       CLICK OUTSIDE MODAL
       ======================================== */

    deleteCategoryModal.addEventListener(
        "click",
        (event) => {

            if (
                event.target ===
                deleteCategoryModal
            ) {

                closeDeleteCategoryModal();
            }
        }
    );


    /* ========================================
       ESCAPE
       ======================================== */

    document.addEventListener(
        "keydown",
        (event) => {

            if (
                event.key === "Escape" &&
                deleteCategoryModal.classList.contains(
                    "active"
                )
            ) {

                closeDeleteCategoryModal();
            }
        }
    );


    /* ========================================
       CONFIRM DELETE
       ======================================== */

    confirmDeleteCategoryButton.addEventListener(
        "click",
        async () => {

            if (selectedCategoryId === null) {
                return;
            }


            confirmDeleteCategoryButton.disabled =
                true;


            deleteCategoryMessage.textContent =
                "";

            deleteCategoryMessage.classList.remove(
                "success",
                "error"
            );


            try {

                const response = await fetch(
                    `/cf_automation/categories/${selectedCategoryId}`,
                    {
                        method: "DELETE",
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


                /*
                 * DELETE SUCCESS
                 */
                if (response.ok) {

                    if (
                        typeof loadCategories ===
                        "function"
                    ) {

                        await loadCategories();
                    }


                    closeDeleteCategoryModal();

                    return;
                }


                /*
                 * DELETE FAILED
                 */
                let errorData = null;


                try {

                    errorData =
                        await response.json();

                } catch {
                    // Response had no JSON body
                }


                if (
                    errorData &&
                    errorData.message
                ) {

                    deleteCategoryMessage.textContent =
                        errorData.message;

                } else {

                    deleteCategoryMessage.textContent =
                        "Unable to delete category.";
                }


                deleteCategoryMessage.classList.add(
                    "error"
                );


            } catch (error) {

                deleteCategoryMessage.textContent =
                    "Unable to connect to the server.";

                deleteCategoryMessage.classList.add(
                    "error"
                );

            } finally {

                confirmDeleteCategoryButton.disabled =
                    false;
            }
        }
    );

});