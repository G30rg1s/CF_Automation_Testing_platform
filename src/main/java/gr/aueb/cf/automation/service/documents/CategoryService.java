package gr.aueb.cf.automation.service.documents;

import gr.aueb.cf.automation.dto.documents.CategoryOptionDto;
import gr.aueb.cf.automation.dto.documents.DocumentCategoryAssignmentResponseDto;
import gr.aueb.cf.automation.dto.documents.StandaloneCategoryResponseDto;
import gr.aueb.cf.automation.entity.documents.CategoryEntity;
import gr.aueb.cf.automation.entity.documents.DocumentEntity;
import gr.aueb.cf.automation.exception.exceptions.CategoryInUseException;
import gr.aueb.cf.automation.exception.exceptions.CategoryNotFoundException;
import gr.aueb.cf.automation.exception.exceptions.DefaultCategoryProtectedException;
import gr.aueb.cf.automation.repository.documents.CategoryRepository;
import gr.aueb.cf.automation.repository.documents.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;


    public CategoryService(CategoryRepository categoryRepository, DocumentRepository documentRepository) {
        this.categoryRepository = categoryRepository;
        this.documentRepository = documentRepository;
    }


    /* ========================================
       GET ALL CATEGORIES
       ======================================== */

    @Transactional(readOnly = true)
    public List<CategoryOptionDto> getAll() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(category -> new CategoryOptionDto(
                        category.getId(),
                        category.getName()
                ))
                .toList();
    }


    /* ========================================
       ASSIGN CATEGORY TO DOCUMENT
       ======================================== */

    @Transactional
    public DocumentCategoryAssignmentResponseDto assignToDocument(Long documentId, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }

        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        String normalizedName = categoryName.trim();

        CategoryEntity category = categoryRepository
                .findByNameIgnoreCaseAndAccent(normalizedName)
                .orElse(null);

        String categoryResult;

        if (category == null) {
            category = new CategoryEntity();
            category.setName(normalizedName);
            category = categoryRepository.save(category);

            categoryResult = "CATEGORY_CREATED";

        } else {
            categoryResult = "CATEGORY_ALREADY_EXISTED";
        }

        document.setCategory(category);
        documentRepository.save(document);

        return new DocumentCategoryAssignmentResponseDto(
                categoryResult,
                category.getId(),
                category.getName(),
                document.getId(),
                document.getTitle()
        );
    }


    /* ========================================
       CREATE STANDALONE CATEGORY
       ======================================== */

    @Transactional
    public StandaloneCategoryResponseDto createStandalone(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }

        String normalizedName = categoryName.trim();

        CategoryEntity category = categoryRepository
                .findByNameIgnoreCaseAndAccent(normalizedName)
                .orElse(null);

        String result;

        if (category == null) {
            category = new CategoryEntity();
            category.setName(normalizedName);
            category = categoryRepository.save(category);

            result = "CATEGORY_CREATED";

        } else {
            result = "CATEGORY_ALREADY_EXISTED";
        }

        return new StandaloneCategoryResponseDto(
                result,
                category.getId(),
                category.getName()
        );
    }


    /* ========================================
       DELETE CATEGORY
       ======================================== */

    @Transactional
    public boolean deleteCategory(Long categoryId) {
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        CategoryEntity defaultCategory = categoryRepository
                .findByNameIgnoreCaseAndAccent("Διάφορα")
                .orElse(null);

        if (defaultCategory != null && defaultCategory.getId().equals(categoryId)) {
            throw new DefaultCategoryProtectedException(
                    category.getId(),
                    category.getName()
            );
        }

        boolean hasDocuments = documentRepository.existsByCategory_Id(categoryId);

        if (hasDocuments) {
            throw new CategoryInUseException(
                    category.getId(),
                    category.getName()
            );
        }

        categoryRepository.delete(category);

        return true;
    }
}