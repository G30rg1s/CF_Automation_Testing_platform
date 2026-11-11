
package gr.aueb.cf.parser.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseRequestDto {

    @NotNull(message = "Document ID is required")
    private Long documentId;

    private String filePath;

    @NotNull(message = "Database target is required")
    private String databaseTarget;
}
