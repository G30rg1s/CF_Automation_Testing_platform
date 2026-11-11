
package gr.aueb.cf.automation.dto.documents;

import gr.aueb.cf.automation.entity.documents.DocumentStatus;
import gr.aueb.cf.automation.entity.documents.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDetailsDto {

    private Long id;

    private String title;

    private String htmlContent;

    private DocumentStatus status;

    private String errorMessage;

    private Long categoryId;

    private String categoryName;

    private DocumentType documentType;
}
