
package gr.aueb.cf.automation.dto.documents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseRequestDto {

    private Long documentId;
    private String filePath;
    private String databaseTarget;
}
