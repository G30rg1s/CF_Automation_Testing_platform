
package gr.aueb.cf.automation.dto.documents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedDocumentResultDto {

    private Long documentId;
    private String htmlContent;
    private String databaseTarget;
}
