
package gr.aueb.cf.parser.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseFailedDto {

    private Long documentId;
    private String errorMessage;
    private String databaseTarget;
}
