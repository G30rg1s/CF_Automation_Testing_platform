
package gr.aueb.cf.automation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/automation")
public class PageController {

    /* ========================================
       HOMEPAGE
       ======================================== */

    @GetMapping("/homepage")
    public String home(
            @CookieValue(
                    name = "database_target",
                    required = false
            )
            String databaseTarget,

            Model model
    ) {

        model.addAttribute(
                "selectedDatabaseMode",
                databaseTarget
        );

        model.addAttribute(
                "selectedDatabaseModeLabel",
                databaseModeLabel(databaseTarget)
        );

        return "html/homepage";
    }

    /* ========================================
   APP LOGS
   INDEPENDENT BROWSER WINDOW
   ======================================== */

    @GetMapping("/app-logs")
    public String appLogsPage() {
        return "html/app-logs";
    }

    /* ========================================
       REGISTER
       ======================================== */

    @GetMapping("/register")
    public String register() {
        return "html/registration";
    }

    /* ========================================
       LOGIN
       ======================================== */

    @GetMapping("/login")
    public String login() {
        return "html/login";
    }

    /* ========================================
       DASHBOARD
       ======================================== */

    @GetMapping("/dashboard")
    public String dashboard() {
        return "html/dashboard";
    }

    /* ========================================
       PROFILE
       ======================================== */

    @GetMapping("/profile")
    public String profile() {
        return "html/profile";
    }

    /* ========================================
       UPLOAD DOCUMENT
       ======================================== */

    @GetMapping("/upload")
    public String uploadDocument() {
        return "html/upload-document";
    }

    /* ========================================
       CATEGORY DOCUMENTS
       ======================================== */

    @GetMapping("/categories/{categoryId}")
    public String categoryDocuments(
            @PathVariable Long categoryId
    ) {
        return "html/category-documents";
    }

    /* ========================================
       DOCUMENT DETAILS
       ======================================== */

    @GetMapping("/documents/{documentId}")
    public String documentDetails(
            @PathVariable Long documentId
    ) {
        return "html/document-details";
    }

    /* ========================================
       MANAGE DATABASE
       ======================================== */

    @GetMapping("/manage-database")
    public String manageDatabase() {
        return "html/manage-database";
    }

    /* ========================================
       SQL QUERY CONSOLE
       INDEPENDENT BROWSER WINDOW
       ======================================== */

    @GetMapping("/sql-console")
    public String sqlConsole(
            @CookieValue(
                    name = "database_target",
                    required = false
            )
            String databaseTarget,

            Model model
    ) {

        model.addAttribute(
                "selectedDatabaseMode",
                databaseTarget
        );

        model.addAttribute(
                "selectedDatabaseModeLabel",
                databaseModeLabel(databaseTarget)
        );

        return "html/sql-console";
    }

    /* ========================================
       UNAUTHORIZED
       ======================================== */

    @GetMapping("/unauthorized")
    public String unauthorized() {
        return "html/unauthorized";
    }

    /* ========================================
       DATABASE MODE LABEL
       ======================================== */

    private String databaseModeLabel(
            String databaseTarget
    ) {

        if (
                databaseTarget == null ||
                        databaseTarget.isBlank()
        ) {
            return "Select Mode";
        }

        return switch (
                databaseTarget
                        .trim()
                        .toLowerCase()
                ) {

            case "production" ->
                    "Production";

            case "frontendtesting" ->
                    "Frontend Testing";

            case "apitesting" ->
                    "API Testing";

            default ->
                    "Select Mode";
        };
    }
}
