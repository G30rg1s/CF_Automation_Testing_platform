package gr.aueb.cf.automation.service;

import gr.aueb.cf.automation.dto.DashboardStatsDto;
import gr.aueb.cf.automation.repository.documents.CategoryRepository;
import gr.aueb.cf.automation.repository.documents.DocumentRepository;
import org.springframework.stereotype.Service;


@Service
public class DashboardService {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;


    public DashboardService(DocumentRepository documentRepository, CategoryRepository categoryRepository) {
        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
    }


    /* ========================================
       GET DASHBOARD STATS
       ======================================== */

    public DashboardStatsDto getStats() {
        long totalDocuments = documentRepository.count();
        long totalCategories = categoryRepository.count();

        return new DashboardStatsDto(
                totalDocuments,
                totalCategories
        );
    }
}