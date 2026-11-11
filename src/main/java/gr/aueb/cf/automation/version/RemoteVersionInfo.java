package gr.aueb.cf.automation.version;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RemoteVersionInfo(
        String version,

        @JsonProperty("gvm_filename")
        String gvmFilename,

        @JsonProperty("gvm_url")
        String gvmUrl
) {
}