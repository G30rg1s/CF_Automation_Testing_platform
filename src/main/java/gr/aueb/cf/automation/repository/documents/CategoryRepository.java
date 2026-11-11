package gr.aueb.cf.automation.repository.documents;

import gr.aueb.cf.automation.entity.documents.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    @Query(
            value = """
                    SELECT *
                    FROM categories
                    WHERE name COLLATE utf8mb4_0900_ai_ci = :name
                    LIMIT 1
                    """,
            nativeQuery = true
    )

    Optional<CategoryEntity> findByNameIgnoreCaseAndAccent(
            @Param("name") String name
    );

    List<CategoryEntity> findAllByOrderByNameAsc();
}