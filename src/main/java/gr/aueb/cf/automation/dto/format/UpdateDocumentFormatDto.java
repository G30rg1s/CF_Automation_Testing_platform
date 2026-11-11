package gr.aueb.cf.automation.dto.format;

import gr.aueb.cf.automation.entity.format.CodeLanguage;
import gr.aueb.cf.automation.entity.format.DocumentFormatType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentFormatDto {

    private Integer paragraphIndex;
    private DocumentFormatType formatType;
    private CodeLanguage codeLanguage;
}