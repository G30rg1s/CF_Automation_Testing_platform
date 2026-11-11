package gr.aueb.cf.automation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {

    private long totalDocuments;

    private long totalCategories;
}