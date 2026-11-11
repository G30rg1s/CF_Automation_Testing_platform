package gr.aueb.cf.automation.controller.documents;

import gr.aueb.cf.automation.dto.documents.*;
import gr.aueb.cf.automation.service.documents.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cf_automation/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(
            CategoryService categoryService
    ) {
        this.categoryService = categoryService;
    }


    @GetMapping
    public ResponseEntity<List<CategoryOptionDto>> getAll() {

        return ResponseEntity.ok(
                categoryService.getAll()
        );
    }


    @PostMapping
    public ResponseEntity<StandaloneCategoryResponseDto> create(
            @Valid @RequestBody StandaloneCategoryCreateDto request
    ) {

        return ResponseEntity.ok(
                categoryService.createStandalone(
                        request.getName()
                )
        );
    }

    @PatchMapping("/documents/{documentId}/category")
    public ResponseEntity<DocumentCategoryAssignmentResponseDto> assignCategory(
            @PathVariable Long documentId,
            @Valid @RequestBody CategoryCreateDto request
    ) {

        return ResponseEntity.ok(
                categoryService.assignToDocument(
                        documentId,
                        request.getName()
                )
        );
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long categoryId
    ) {

        boolean deleted =
                categoryService.deleteCategory(categoryId);

        if (!deleted) {
            return ResponseEntity
                    .status(409)
                    .build();
        }

        return ResponseEntity
                .noContent()
                .build();
    }
}