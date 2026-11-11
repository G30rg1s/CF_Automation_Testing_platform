package gr.aueb.cf.automation.exception.exceptions;

public class CategoryInUseException extends RuntimeException {

    private final Long categoryId;
    private final String categoryName;

    public CategoryInUseException(
            Long categoryId,
            String categoryName
    ) {
        super(
                "Category cannot be deleted because it is assigned to one or more documents."
        );

        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
}