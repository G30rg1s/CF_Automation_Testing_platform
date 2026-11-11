package gr.aueb.cf.automation.dto.documents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCategoryAssignmentResponseDto {

    private String categoryResult;

    private Long categoryId;

    private String categoryName;

    private Long documentId;

    private String documentTitle;
}