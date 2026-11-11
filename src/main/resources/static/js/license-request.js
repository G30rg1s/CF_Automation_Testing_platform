document.addEventListener("DOMContentLoaded", () => {

    const requestTypeSelect =
        document.getElementById("requestType");

    const applicantTypeSelect =
        document.getElementById("applicantType");

    const existingLicenseInformation =
        document.getElementById("existingLicenseInformation");

    const teamNameGroup =
        document.getElementById("teamNameGroup");

    const teamNameInput =
        document.getElementById("teamName");

    const reasonLabel =
        document.getElementById("reasonLabel");

    const descriptionLabel =
        document.getElementById("descriptionLabel");


    if (
        !requestTypeSelect ||
        !applicantTypeSelect
    ) {
        return;
    }


    function updateRequestTypeView() {

        const requestType =
            requestTypeSelect.value;


        if (
            requestType ===
            "EXISTING_LICENSE_PROBLEM"
        ) {

            if (existingLicenseInformation) {
                existingLicenseInformation.hidden = false;
            }

            if (reasonLabel) {
                reasonLabel.textContent =
                    "Please describe the problem with your existing licence.";
            }

            if (descriptionLabel) {
                descriptionLabel.innerHTML =
                    'Additional Information <span class="optional">Optional</span>';
            }

            return;
        }


        if (existingLicenseInformation) {
            existingLicenseInformation.hidden = true;
        }


        if (
            requestType ===
            "NEW_LICENSE"
        ) {

            if (reasonLabel) {
                reasonLabel.textContent =
                    "Why would you like to use Automation Platform?";
            }

            if (descriptionLabel) {
                descriptionLabel.innerHTML =
                    'Additional Information <span class="optional">Optional</span>';
            }
        }
    }


    function updateApplicantTypeView() {

        const applicantType =
            applicantTypeSelect.value;


        if (
            applicantType === "TEAM"
        ) {

            if (teamNameGroup) {
                teamNameGroup.hidden = false;
            }

            if (teamNameInput) {
                teamNameInput.required = true;
            }

            return;
        }


        if (teamNameGroup) {
            teamNameGroup.hidden = true;
        }

        if (teamNameInput) {

            teamNameInput.required = false;

            if (
                applicantType === "INDIVIDUAL"
            ) {
                teamNameInput.value = "";
            }
        }
    }


    requestTypeSelect.addEventListener(
        "change",
        updateRequestTypeView
    );


    applicantTypeSelect.addEventListener(
        "change",
        updateApplicantTypeView
    );


    updateRequestTypeView();
    updateApplicantTypeView();
});