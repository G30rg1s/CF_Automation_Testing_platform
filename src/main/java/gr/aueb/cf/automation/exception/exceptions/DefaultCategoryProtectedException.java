package gr.aueb.cf.automation.exception.exceptions;

public class DefaultCategoryProtectedException extends RuntimeException {

    private final Long categoryId;
    private final String categoryName;

    public DefaultCategoryProtectedException(
            Long categoryId,
            String categoryName
    ) {
        super(
                "The default category cannot be deleted."
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