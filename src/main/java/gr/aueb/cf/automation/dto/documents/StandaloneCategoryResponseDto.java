package gr.aueb.cf.automation.dto.documents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandaloneCategoryResponseDto {

    private String result;

    private Long categoryId;

    private String categoryName;
}