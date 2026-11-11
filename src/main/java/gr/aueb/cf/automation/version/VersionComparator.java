
package gr.aueb.cf.automation.version;

import org.springframework.stereotype.Component;

@Component
public class VersionComparator {

    public boolean isNewerVersion(
            String currentVersion,
            String remoteVersion
    ) {
        if (currentVersion == null || remoteVersion == null) {
            return false;
        }

        if (currentVersion.isBlank() || remoteVersion.isBlank()) {
            return false;
        }

        if ("dev".equalsIgnoreCase(currentVersion)) {
            return false;
        }

        int[] current = parseVersion(currentVersion);
        int[] remote = parseVersion(remoteVersion);

        for (int i = 0; i < Math.max(current.length, remote.length); i++) {

            int currentPart = i < current.length ? current[i] : 0;
            int remotePart = i < remote.length ? remote[i] : 0;

            if (remotePart > currentPart) {
                return true;
            }

            if (remotePart < currentPart) {
                return false;
            }
        }

        return false;
    }

    private int[] parseVersion(String version) {
        String[] parts = version.trim().split("\\.");

        int[] numbers = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            numbers[i] = Integer.parseInt(parts[i]);
        }

        return numbers;
    }
}
