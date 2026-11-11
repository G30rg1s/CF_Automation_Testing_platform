package gr.aueb.cf.automation.dto.documents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExistingDocumentDto {

    private Long documentId;
    private String title;

}