
package gr.aueb.cf.automation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/cf_automation/app-logs")
public class AppLogsController {

    // ========================================
    // CONFIGURATION
    // ========================================

    private static final int MAX_LINES = 3000;

    private static final int MAX_BYTES = 1024 * 1024;

    private final Path automationLogPath;
    private final Path parserLogPath;

    public AppLogsController(

            @Value("${app.logs.automation-path:logs/automation.log}")
            String automationLogPath,

            @Value("${app.logs.parser-path:logs/parser.log}")
            String parserLogPath

    ) {

        this.automationLogPath =
                Path.of(automationLogPath)
                        .toAbsolutePath()
                        .normalize();

        this.parserLogPath =
                Path.of(parserLogPath)
                        .toAbsolutePath()
                        .normalize();
    }

    // ========================================
    // RESPONSE DTO
    // ========================================

    public record LogsResponse(
            List<String> lines
    ) {
    }

    // ========================================
    // AUTOMATION PLATFORM LOGS
    // ========================================

    @GetMapping("/automation")
    public LogsResponse automationLogs() {

        return new LogsResponse(
                readLatestLines(automationLogPath)
        );
    }

    // ========================================
    // PARSER SERVICE LOGS
    // ========================================

    @GetMapping("/parser")
    public LogsResponse parserLogs() {

        return new LogsResponse(
                readLatestLines(parserLogPath)
        );
    }

    // ========================================
    // READ LATEST LOG LINES
    // ========================================

    private List<String> readLatestLines(
            Path logPath
    ) {

        if (!Files.isRegularFile(logPath)) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Log file is not available."
            );
        }

        if (!Files.isReadable(logPath)) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Log file is not readable."
            );
        }

        try (
                RandomAccessFile file =
                        new RandomAccessFile(
                                logPath.toFile(),
                                "r"
                        )
        ) {

            long fileLength = file.length();

            if (fileLength == 0) {
                return List.of();
            }

            // Read only the last MAX_BYTES
            // to avoid loading huge log files.

            long startPosition = Math.max(
                    0,
                    fileLength - MAX_BYTES
            );

            int bytesToRead = Math.toIntExact(
                    fileLength - startPosition
            );

            byte[] buffer = new byte[bytesToRead];

            file.seek(startPosition);
            file.readFully(buffer);

            String content = new String(
                    buffer,
                    StandardCharsets.UTF_8
            );

            // If reading started in the middle
            // of a line, discard that incomplete line.

            if (startPosition > 0) {

                int firstNewline = content.indexOf('\n');

                if (firstNewline < 0) {
                    return List.of();
                }

                content = content.substring(
                        firstNewline + 1
                );
            }

            if (content.isEmpty()) {
                return List.of();
            }

            List<String> allLines =
                    Arrays.asList(
                            content.split(
                                    "\\R",
                                    -1
                            )
                    );

            // Remove the trailing empty entry
            // when the file ends with a newline.

            int end = allLines.size();

            if (
                    end > 0 &&
                            allLines.get(end - 1).isEmpty()
            ) {
                end--;
            }

            int start = Math.max(
                    0,
                    end - MAX_LINES
            );

            return List.copyOf(
                    allLines.subList(
                            start,
                            end
                    )
            );

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to read application logs."
            );
        }
    }
}
