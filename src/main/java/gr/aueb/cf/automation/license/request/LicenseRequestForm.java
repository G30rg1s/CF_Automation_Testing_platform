package gr.aueb.cf.automation.license.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LicenseRequestForm(

        @NotNull
        LicenseRequestType requestType,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Size(max = 30)
        String applicantType,

        @Size(max = 150)
        String teamName,

        @NotBlank
        @Email
        @Size(max = 200)
        String email,

        @Size(max = 50)
        String phone,

        @Size(max = 1000)
        String description,

        @NotBlank
        @Size(max = 2000)
        String reason
) {
}