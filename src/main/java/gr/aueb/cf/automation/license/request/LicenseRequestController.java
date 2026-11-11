
package gr.aueb.cf.automation.license.request;

import gr.aueb.cf.automation.license.model.LicenseState;
import gr.aueb.cf.automation.license.service.LicenseStateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

//@Controller
public class LicenseRequestController {

    private static final Logger logger =
            LoggerFactory.getLogger(LicenseRequestController.class);

    private final LicenseRequestService licenseRequestService;
    private final LicenseRequestEmailService licenseRequestEmailService;
    private final LicenseStateService licenseStateService;

    public LicenseRequestController(
            LicenseRequestService licenseRequestService,
            LicenseRequestEmailService licenseRequestEmailService,
            LicenseStateService licenseStateService
    ) {
        this.licenseRequestService = licenseRequestService;
        this.licenseRequestEmailService = licenseRequestEmailService;
        this.licenseStateService = licenseStateService;
    }

    // ========================================
    // REQUEST PAGE
    // ========================================

    @GetMapping("/license/request")
    public String requestPage(Model model) {

        if (!model.containsAttribute("licenseRequestForm")) {

            model.addAttribute(
                    "licenseRequestForm",
                    new LicenseRequestForm(
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    )
            );
        }

        addLicenseState(model);

        return "html/licence/license-request";
    }

    // ========================================
    // SUBMIT REQUEST
    // ========================================

    @PostMapping("/license/request")
    public String submitRequest(
            @Valid
            @ModelAttribute("licenseRequestForm")
            LicenseRequestForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        if (bindingResult.hasErrors()) {

            addLicenseState(model);

            return "html/licence/license-request";
        }

        try {

            // Create request payload.
            LicenseRequest request =
                    licenseRequestService.createRequest(form);

            // Send JSON to License Admin using HTTP.
            // This is NOT an email.
            // send() throws an exception for non-2xx responses.
            licenseRequestEmailService.send(request);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your licence request has been submitted successfully."
            );

            return "redirect:/license/request";

        } catch (Exception exception) {

            logger.error(
                    "Failed to submit licence request to License Admin",
                    exception
            );

            model.addAttribute(
                    "errorMessage",
                    "Your request could not be sent. Please try again later."
            );

            addLicenseState(model);

            return "html/licence/license-request";
        }
    }

    // ========================================
    // LICENSE STATE
    // ========================================

    private void addLicenseState(Model model) {

        LicenseState state =
                licenseStateService.getCurrentState();

        model.addAttribute("licenseState", state);
    }
}
