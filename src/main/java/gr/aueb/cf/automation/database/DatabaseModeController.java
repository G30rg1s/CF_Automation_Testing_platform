package gr.aueb.cf.automation.database;

import gr.aueb.cf.automation.queryrunner.DatabaseTarget;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DatabaseModeController {

    private static final String DATABASE_COOKIE =
            "database_target";

    private static final String ACCESS_TOKEN_COOKIE =
            "access_token";


    /* ========================================
       CHANGE DATABASE MODE
       ======================================== */

    @PostMapping("/automation/database-mode")
    public String changeDatabaseMode(
            @RequestParam("mode") String mode,
            HttpServletResponse response
    ) {

        DatabaseTarget databaseTarget =
                parseDatabaseTarget(mode);


        /* ========================================
           DATABASE MODE COOKIE
           ======================================== */

        ResponseCookie databaseCookie =
                ResponseCookie
                        .from(
                                DATABASE_COOKIE,
                                toCookieValue(databaseTarget)
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .build();


        response.addHeader(
                HttpHeaders.SET_COOKIE,
                databaseCookie.toString()
        );


        /* ========================================
           CLEAR CURRENT LOGIN
           ======================================== */

        ResponseCookie clearAccessTokenCookie =
                ResponseCookie
                        .from(
                                ACCESS_TOKEN_COOKIE,
                                ""
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();


        response.addHeader(
                HttpHeaders.SET_COOKIE,
                clearAccessTokenCookie.toString()
        );


        /* ========================================
           REDIRECT TO HOMEPAGE
           ======================================== */

        return "redirect:/automation/homepage";
    }


    /* ========================================
       PARSE MODE
       ======================================== */

    private DatabaseTarget parseDatabaseTarget(
            String mode
    ) {

        if (
                mode == null ||
                        mode.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Database mode is required."
            );
        }


        return switch (
                mode.trim()
                        .toLowerCase()
                ) {

            case "production" ->
                    DatabaseTarget.PRODUCTION;

            case "frontendtesting" ->
                    DatabaseTarget.FRONTEND_TESTING;

            case "apitesting" ->
                    DatabaseTarget.API_TESTING;

            default ->
                    throw new IllegalArgumentException(
                            "Unknown database mode: "
                                    + mode
                    );
        };
    }


    /* ========================================
       COOKIE VALUE
       ======================================== */

    private String toCookieValue(
            DatabaseTarget databaseTarget
    ) {

        return switch (databaseTarget) {

            case PRODUCTION ->
                    "production";

            case FRONTEND_TESTING ->
                    "frontendtesting";

            case API_TESTING ->
                    "apitesting";
        };
    }
}