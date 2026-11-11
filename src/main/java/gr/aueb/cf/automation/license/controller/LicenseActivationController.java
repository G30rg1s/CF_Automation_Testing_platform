package gr.aueb.cf.automation.license.controller;

import gr.aueb.cf.automation.license.model.LicenseState;
import gr.aueb.cf.automation.license.service.LicenseStateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LicenseActivationController {

    private final LicenseStateService licenseStateService;


    public LicenseActivationController(
            LicenseStateService licenseStateService
    ) {

        this.licenseStateService =
                licenseStateService;
    }


    /* ========================================
       ACTIVATION PAGE
       ======================================== */

    @GetMapping("/license/activate")
    public String activationPage(
            Model model
    ) {

        LicenseState state =
                licenseStateService
                        .getCurrentState();


        model.addAttribute(
                "licenseState",
                state
        );


        return "html/licence/license-activate";
    }


    /* ========================================
       ACTIVATE
       ======================================== */

    @PostMapping("/license/activate")
    public String activate(
            @RequestParam("licenseId")
            String licenseId,

            @RequestParam("activationCode")
            String activationCode,

            RedirectAttributes redirectAttributes
    ) {

        LicenseState state =
                licenseStateService.activate(
                        licenseId,
                        activationCode
                );


        if (state.accessAllowed()) {

            redirectAttributes
                    .addFlashAttribute(
                            "successMessage",
                            "License activated successfully."
                    );


            return "redirect:/automation/homepage";
        }


        redirectAttributes
                .addFlashAttribute(
                        "errorMessage",
                        state.message()
                );


        return "redirect:/license/activate";
    }


    /* ========================================
       RETRY VALIDATION
       ======================================== */

    @PostMapping("/license/retry")
    public String retry(
            RedirectAttributes redirectAttributes
    ) {

        LicenseState state =
                licenseStateService.refresh();


        if (state.accessAllowed()) {

            return "redirect:/automation/homepage";
        }


        redirectAttributes
                .addFlashAttribute(
                        "errorMessage",
                        state.message()
                );


        return "redirect:/license/activate";
    }
}