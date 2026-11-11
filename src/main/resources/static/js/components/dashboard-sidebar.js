
const categoryList =
    document.getElementById("categoryList");

const categoriesToggleButton =
    document.getElementById("categoriesToggleButton");

const dashboardNavigation =
    document.getElementById("dashboardNavigation");

const profileNavigation =
    document.getElementById("profileNavigation");

const manageDatabaseNavigation =
    document.getElementById("manageDatabaseNavigation");

const appLogsNavigation =
    document.getElementById("appLogsNavigation");

const uploadNavigation =
    document.getElementById("uploadNavigation");


/* ========================================
   DASHBOARD NAVIGATION
   ======================================== */

if (dashboardNavigation) {
    dashboardNavigation.addEventListener("click", () => {
        window.location.href = "/automation/dashboard";
    });
}


/* ========================================
   PROFILE NAVIGATION
   ======================================== */

if (profileNavigation) {
    profileNavigation.addEventListener("click", () => {
        window.location.href = "/automation/profile";
    });
}


/* ========================================
   MANAGE DATABASE NAVIGATION
   ======================================== */

if (manageDatabaseNavigation) {
    manageDatabaseNavigation.addEventListener("click", () => {
        window.location.href = "/automation/manage-database";
    });
}


/* ========================================
   APP LOGS NAVIGATION
   ======================================== */

if (appLogsNavigation) {

    appLogsNavigation.addEventListener("click", () => {

        const popupWidth = 1200;
        const popupHeight = 800;

        const popupLeft = Math.max(
            0,
            Math.round(
                (window.screen.availWidth - popupWidth) / 2
            )
        );

        const popupTop = Math.max(
            0,
            Math.round(
                (window.screen.availHeight - popupHeight) / 2
            )
        );

        const popup = window.open(
            "/automation/app-logs",
            "codingFactoryAppLogs",
            [
                "popup=yes",
                "resizable=yes",
                "scrollbars=yes",
                `width=${popupWidth}`,
                `height=${popupHeight}`,
                `left=${popupLeft}`,
                `top=${popupTop}`
            ].join(",")
        );

        if (popup) {
            popup.focus();
        } else {
            window.location.href = "/automation/app-logs";
        }
    });
}


/* ========================================
   UPLOAD NAVIGATION
   ======================================== */

if (uploadNavigation) {
    uploadNavigation.addEventListener("click", () => {
        window.location.href = "/automation/upload";
    });
}


/* ========================================
   ACTIVE NAVIGATION
   ======================================== */

const navigationButtons = [
    dashboardNavigation,
    profileNavigation,
    manageDatabaseNavigation,
    appLogsNavigation,
    uploadNavigation
];

navigationButtons.forEach(button => {
    if (button) {
        button.classList.remove("active");
    }
});

const currentPath = window.location.pathname;

if (
    currentPath === "/automation/dashboard" &&
    dashboardNavigation
) {
    dashboardNavigation.classList.add("active");
} else if (
    currentPath === "/automation/profile" &&
    profileNavigation
) {
    profileNavigation.classList.add("active");
} else if (
    currentPath === "/automation/manage-database" &&
    manageDatabaseNavigation
) {
    manageDatabaseNavigation.classList.add("active");
} else if (
    currentPath === "/automation/app-logs" &&
    appLogsNavigation
) {
    appLogsNavigation.classList.add("active");
} else if (
    currentPath === "/automation/upload" &&
    uploadNavigation
) {
    uploadNavigation.classList.add("active");
}


/* ========================================
   OPEN / CLOSE CATEGORIES
   ======================================== */

if (categoriesToggleButton && categoryList) {

    categoriesToggleButton.addEventListener(
        "click",
        () => {

            const isCollapsed =
                categoryList.classList.toggle("collapsed");

            categoriesToggleButton.classList.toggle(
                "collapsed",
                isCollapsed
            );

            categoriesToggleButton.setAttribute(
                "aria-expanded",
                String(!isCollapsed)
            );
        }
    );
}


/* ========================================
   CHECK DEFAULT CATEGORY
   ======================================== */

function isDefaultCategory(categoryName) {

    return categoryName
        .trim()
        .localeCompare(
            "Διάφορα",
            "el",
            {
                sensitivity: "base"
            }
        ) === 0;
}


/* ========================================
   CHECK GREEK CATEGORY
   ======================================== */

function isGreekCategory(categoryName) {

    const greekRegex =
        /[Α-Ωα-ωΆΈΉΊΌΎΏάέήίόύώϊϋΐΰ]/;

    return greekRegex.test(categoryName);
}


/* ========================================
   SORT CATEGORIES
   ======================================== */

function sortCategories(categories) {

    categories.sort(
        (categoryA, categoryB) => {

            const nameA = categoryA.name.trim();
            const nameB = categoryB.name.trim();

            /* ΔΙΑΦΟΡΑ ALWAYS FIRST */

            const aIsDefault =
                isDefaultCategory(nameA);

            const bIsDefault =
                isDefaultCategory(nameB);

            if (aIsDefault && !bIsDefault) {
                return -1;
            }

            if (!aIsDefault && bIsDefault) {
                return 1;
            }

            /* GREEK BEFORE ENGLISH */

            const aIsGreek =
                isGreekCategory(nameA);

            const bIsGreek =
                isGreekCategory(nameB);

            if (aIsGreek && !bIsGreek) {
                return -1;
            }

            if (!aIsGreek && bIsGreek) {
                return 1;
            }

            /* GREEK ALPHABETICAL ORDER */

            if (aIsGreek && bIsGreek) {
                return nameA.localeCompare(
                    nameB,
                    "el",
                    {
                        sensitivity: "base"
                    }
                );
            }

            /* ENGLISH ALPHABETICAL ORDER */

            return nameA.localeCompare(
                nameB,
                "en",
                {
                    sensitivity: "base"
                }
            );
        }
    );
}


/* ========================================
   LOAD CATEGORIES FROM BACKEND
   ======================================== */

async function loadCategories() {

    if (!categoryList) {
        return;
    }

    try {

        const response = await fetch(
            "/cf_automation/categories",
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

            categoryList.textContent =
                "Unable to load categories.";

            return;
        }

        const categories = await response.json();

        /* SORT BEFORE DISPLAY */

        sortCategories(categories);

        categoryList.innerHTML = "";

        if (categories.length === 0) {

            categoryList.textContent =
                "No categories available.";

            return;
        }

        categories.forEach(category => {

            /* CATEGORY ROW */

            const categoryRow =
                document.createElement("div");

            categoryRow.classList.add("category-row");

            categoryRow.dataset.categoryId =
                category.id;

            /* CATEGORY BUTTON */

            const categoryButton =
                document.createElement("button");

            categoryButton.type = "button";

            categoryButton.classList.add(
                "category-item"
            );

            categoryButton.textContent =
                category.name;

            categoryButton.dataset.categoryId =
                category.id;

            categoryButton.dataset.categoryName =
                category.name;

            categoryButton.setAttribute(
                "data-testid",
                `category-${category.id}`
            );

            /* OPEN CATEGORY PAGE */

            categoryButton.addEventListener(
                "click",
                () => {
                    window.location.href =
                        `/automation/categories/${category.id}`;
                }
            );

            categoryRow.appendChild(categoryButton);

            /* DELETE BUTTON:
               ΔΙΑΦΟΡΑ HAS NO DELETE BUTTON */

            if (!isDefaultCategory(category.name)) {

                const deleteButton =
                    document.createElement("button");

                deleteButton.type = "button";

                deleteButton.classList.add(
                    "delete-category-button"
                );

                deleteButton.textContent = "−";

                deleteButton.dataset.categoryId =
                    category.id;

                deleteButton.dataset.categoryName =
                    category.name;

                deleteButton.setAttribute(
                    "data-testid",
                    `delete-category-${category.id}`
                );

                deleteButton.setAttribute(
                    "aria-label",
                    `Delete category ${category.name}`
                );

                categoryRow.appendChild(deleteButton);
            }

            /* ADD ROW TO SIDEBAR */

            categoryList.appendChild(categoryRow);
        });

    } catch (error) {

        categoryList.textContent =
            "Unable to load categories.";

        console.error(
            "Unable to load categories.",
            error
        );
    }
}


/* ========================================
   INITIAL LOAD
   ======================================== */

loadCategories();
