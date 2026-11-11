package gr.aueb.cf.automation.database;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class DatabaseModeModelAdvice {

    private static final String DATABASE_COOKIE =
            "database_target";


    /* ========================================
       SELECTED DATABASE MODE
       ======================================== */

    @ModelAttribute("selectedDatabaseMode")
    public String selectedDatabaseMode(
            HttpServletRequest request
    ) {

        String cookieValue =
                getDatabaseCookieValue(
                        request
                );


        if (cookieValue == null) {

            return null;
        }


        return switch (
                cookieValue.toLowerCase()
                ) {

            case "production" ->
                    "production";

            case "frontendtesting" ->
                    "frontendtesting";

            case "apitesting" ->
                    "apitesting";

            default ->
                    null;
        };
    }


    /* ========================================
       SELECTED DATABASE LABEL
       ======================================== */

    @ModelAttribute("selectedDatabaseModeLabel")
    public String selectedDatabaseModeLabel(
            HttpServletRequest request
    ) {

        String cookieValue =
                getDatabaseCookieValue(
                        request
                );


        if (cookieValue == null) {

            return "Select Mode";
        }


        return switch (
                cookieValue.toLowerCase()
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


    /* ========================================
       GET DATABASE COOKIE
       ======================================== */

    private String getDatabaseCookieValue(
            HttpServletRequest request
    ) {

        Cookie[] cookies =
                request.getCookies();


        if (cookies == null) {

            return null;
        }


        for (Cookie cookie : cookies) {

            if (DATABASE_COOKIE.equals(
                    cookie.getName()
            )) {

                return cookie.getValue();
            }
        }


        return null;
    }
}