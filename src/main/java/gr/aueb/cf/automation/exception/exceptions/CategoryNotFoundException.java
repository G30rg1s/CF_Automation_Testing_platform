package gr.aueb.cf.automation.exception.exceptions;

public class CategoryNotFoundException extends RuntimeException {

    private final Long categoryId;

    public CategoryNotFoundException(Long categoryId) {

        super(
                "Category not found: " + categoryId
        );

        this.categoryId = categoryId;
    }

    public Long getCategoryId() {
        return categoryId;
    }
}