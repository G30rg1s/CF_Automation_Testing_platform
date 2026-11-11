package gr.aueb.cf.automation.config;

import gr.aueb.cf.automation.entity.documents.CategoryEntity;
import gr.aueb.cf.automation.repository.documents.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(2)
public class CategoryInitializer implements CommandLineRunner {

    private static final String DEFAULT_CATEGORY = "Διάφορα";

    private final CategoryRepository categoryRepository;

    public CategoryInitializer(
            CategoryRepository categoryRepository
    ) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {

        boolean exists = categoryRepository
                .findByNameIgnoreCaseAndAccent(DEFAULT_CATEGORY)
                .isPresent();

        if (exists) {
            return;
        }

        CategoryEntity category =
                new CategoryEntity();

        category.setName(DEFAULT_CATEGORY);

        categoryRepository.save(category);
    }
}