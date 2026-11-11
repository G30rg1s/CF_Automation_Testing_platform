
package gr.aueb.cf.automation.license.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.aueb.cf.automation.license.model.LicenseState;
import gr.aueb.cf.automation.license.service.LicenseStateService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class LicenseGateFilter extends OncePerRequestFilter {

    private final LicenseStateService licenseStateService;
    private final ObjectMapper objectMapper;

    public LicenseGateFilter(
            LicenseStateService licenseStateService,
            ObjectMapper objectMapper
    ) {
        this.licenseStateService = licenseStateService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        /*
         * Public resources remain accessible
         * even when the application is not activated.
         */
        if (isPublicLicensePath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        LicenseState state =
                licenseStateService.getCurrentState();

        if (state.accessAllowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * API clients receive a JSON response
         * instead of an HTML redirect.
         */
        if (isApiRequest(request, path)) {
            writeLockedApiResponse(response, state);
            return;
        }

        response.sendRedirect(
                request.getContextPath() + "/license/activate"
        );
    }

    /* ========================================
       PUBLIC PATHS
       ======================================== */

    private boolean isPublicLicensePath(String path) {

        return path.equals("/license/activate")

                // License activation retry
                || path.equals("/license/retry")

                // License request form and submission
                || path.equals("/license/request")
                || path.equals("/license/request/")

                // Application update status
                || path.equals("/api/update/status")

                // Error page
                || path.equals("/error")

                // Static resources
                || path.equals("/favicon.ico")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/");
    }

    /* ========================================
       API DETECTION
       ======================================== */

    private boolean isApiRequest(
            HttpServletRequest request,
            String path
    ) {

        if (path.startsWith("/cf_automation")
                || path.startsWith("/api/")) {
            return true;
        }

        String accept = request.getHeader("Accept");

        return accept != null
                && accept.contains("application/json");
    }

    /* ========================================
       LOCKED API RESPONSE
       ======================================== */

    private void writeLockedApiResponse(
            HttpServletResponse response,
            LicenseState state
    ) throws IOException {

        response.setStatus(423);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "error", "LICENSE_LOCKED",
                "status", state.status().name(),
                "message", state.message()
        );

        objectMapper.writeValue(
                response.getWriter(),
                body
        );
    }
}
